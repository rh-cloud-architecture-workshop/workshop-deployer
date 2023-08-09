package com.redhat.cloudarchitectureworkshop;


import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/api")
public class WorkshopDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkshopDeployer.class);

    @Inject
    OpenShiftClient client;

    private JsonArray modules;

    private String namespace;

    private String allowedModulesCount;

    private String openShiftDomain;
    
    private String bookBagURL;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Loading configmaps...");
        namespace = System.getenv("NAMESPACE");
        allowedModulesCount = System.getenv("ALLOWED_MODULES_COUNT");
        bookBagURL = System.getenv("BOOKBAG_URL_ROOT");
        if (namespace == null || namespace.isBlank()) {
            throw new RuntimeException("Environment variable 'NAMESPACE' for namespace not set.");
        }
        if (allowedModulesCount == null || !allowedModulesCount.matches("-?\\d+")) {
            throw new RuntimeException("Environment variable 'ALLOWED_MODULES_COUNT' for namespace is either not set or is NaN.");
        }
        if (bookBagURL == null ) {
            throw new RuntimeException("Environment variable 'bookBagURL' for namespace is not set.");
        }
        String configmap = System.getenv().getOrDefault("CONFIGMAP_MODULES", "workshop-modules");
        ConfigMap cmModules = client.configMaps().inNamespace(namespace).withName(configmap).get();
        if (cmModules == null) {
            throw new RuntimeException("Configmap '" + configmap + "' not found in namespace '" + namespace + "'.");
        }
        String modulesKey = System.getenv().getOrDefault("CONFIGMAP_MODULES_KEY", "modules.json");
        String modulesStr = cmModules.getData().get(modulesKey);
        if (modulesStr == null || modulesStr.isEmpty()) {
            throw new RuntimeException("Entry '" + modulesKey + "' not found in ConfigMap '" + configmap + "'.");
        }
        modules = new JsonObject(modulesStr).getJsonArray("modules");

        modules.stream().forEach(o -> {
            JsonObject module = (JsonObject) o;
            ConfigMap cm = client.configMaps().inNamespace(namespace).withName(module.getString("configMap")).get();
            if (cm != null) {
                String application = cm.getData().get(cm.getMetadata().getName() + ".json");
                module.put("application", application);
                String applicationName = new JsonObject(application).getJsonObject("metadata").getString("name");
                module.put("applicationName", applicationName);
                LOGGER.info("Loaded application for " + applicationName);
            }
        });

        openShiftDomain = getOpenShiftDomain();
    }

    @GET
    @Path("/modules")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> workShopModules(@Context HttpHeaders headers) {
        String user = getUser(headers);
        return Uni.createFrom().voidItem().emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(v -> {
                    List<GenericKubernetesResource> applications = listApplicationsForUser(user);
                    JsonObject response = new JsonObject();
                    JsonArray modulesArray = new JsonArray();

                    modules.stream().forEach(o -> {
                        JsonObject jsonObject = (JsonObject) o;
                        JsonObject module = new JsonObject();
                        module.put("name", jsonObject.getString("name"));
                        module.put("description", jsonObject.getString("description"));
                        module.put("primaryTags", jsonObject.getJsonObject("tags").getJsonArray("primary"));
                        module.put("secondaryTags", jsonObject.getJsonObject("tags").getJsonArray("secondary"));
                        module.put("application", jsonObject.getString("applicationName"));

                        Optional<GenericKubernetesResource> application = applications.stream()
                                .filter(r -> r.getMetadata().getName().equals(jsonObject.getString("applicationName")))
                                .findFirst();
                        if (application.isPresent()) {
                            module.put("deployed", true);
                            if (application.get().getMetadata().getDeletionTimestamp() != null) {
                                module.put("deleting", true);
                            } else {
                                module.put("deleting", false);
                            }
                            module.put("status", application.get().get("status", "sync", "status"));
                            module.put("health", application.get().get("status", "health", "status"));
                        } else {
                            module.put("deployed", false);
                            module.put("deleting", false);
                            module.put("status", "");
                            module.put("health", "");
                        }
                        modulesArray.add(module);
                    });
                    response.put("modules", modulesArray);
                    return response.toString();
                }).onItem().transform(resp -> Response.ok(resp).build())
                .onFailure().recoverWithItem(throwable -> {
                    LOGGER.error("Exception while getting modules for user " + user, throwable);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/getGlobalConfig")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response>  getConfigDetails() {
        JsonObject module = new JsonObject();
        module.put("ALLOWED_MODULES_COUNT", allowedModulesCount);
        module.put("BOOKBAG_URL_ROOT", bookBagURL);
        
        
        return Uni.createFrom().voidItem().emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(catalogList -> module)
                .onItem().transform(catalogList -> {
                    if (catalogList == null) {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    } else {
                        return Response.ok(catalogList).build();
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOGGER.error("Exception while fetching category list", throwable);
                    return Response.serverError().build();
                });          
    }

    @POST
    @Path("/deploy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> deployApplication(String input, @Context HttpHeaders headers) {
        String user = getUser(headers);
        String application = new JsonObject(input).getString("application");
        return Uni.createFrom().voidItem().emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(v -> {
                    Optional<Object> module = modules.stream().filter(o -> {
                        JsonObject m = (JsonObject) o;
                        return m.getString("applicationName").equals(application);
                    }).findFirst();
                    if (module.isEmpty()) {
                        LOGGER.warn("Module for application '" + application + "' not found.");
                        return new JsonObject().put("status", "notchanged");
                    }
                    ResourceDefinitionContext context = new ResourceDefinitionContext.Builder()
                            .withGroup("argoproj.io")
                            .withVersion("v1alpha1")
                            .withKind("Application")
                            .withPlural("applications")
                            .withNamespaced(true)
                            .build();

                    GenericKubernetesResource resource = client.genericKubernetesResources(context)
                            .inNamespace("globex-gitops-" + user).withName(application).get();
                    if (resource != null) {
                        LOGGER.warn("Application '" + application + "' is already deployed for user '" + user + "'");
                        return new JsonObject().put("status", "notchanged");
                    }
                    LOGGER.info("Deploying application '" + application + "' for user '" + user + "'");
                    String applicationDef = ((JsonObject)module.get()).getString("application");

                    applicationDef = applicationDef.replaceAll("\\{\\{ __user }}", user)
                            .replaceAll("\\{\\{ r_openshift_subdomain }}", openShiftDomain);
                    InputStream inputStream = new ByteArrayInputStream(applicationDef.getBytes());
                    GenericKubernetesResource newResource = client.genericKubernetesResources(context)
                            .inNamespace("globex-gitops-" + user).load(inputStream).create();
                    return new JsonObject().put("status", "ok")
                            .put("application", new JsonObject().put("deployed", true).put("deleting", false)
                            .put("status", newResource.get("status", "sync", "status") == null ? "" : newResource.get("status", "sync", "status"))
                            .put("health", newResource.get("status", "health", "status") == null ? "" : newResource.get("status", "health", "status")));
                })
                .onItem().transform(resp -> Response.ok(resp.toString()).build())
                .onFailure().recoverWithItem(throwable -> {
                    LOGGER.error("Exception while getting modules for user " + user, throwable);
                    return Response.serverError().build();
                });
    }

    @POST
    @Path("/undeploy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> undeployApplication(String input, @Context HttpHeaders headers) {
        String user = getUser(headers);
        String application = new JsonObject(input).getString("application");
        return Uni.createFrom().voidItem().emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(v -> {
                    ResourceDefinitionContext context = new ResourceDefinitionContext.Builder()
                            .withGroup("argoproj.io")
                            .withVersion("v1alpha1")
                            .withKind("Application")
                            .withPlural("applications")
                            .withNamespaced(true)
                            .build();

                    GenericKubernetesResource resource = client.genericKubernetesResources(context)
                            .inNamespace("globex-gitops-" + user).withName(application).get();
                    if (resource == null) {
                        LOGGER.warn("Application '" + application + "' not found for user '" + user + "'");
                        return new JsonObject().put("status", "notchanged");
                    }
                    LOGGER.info("Undeploying application '" + application + "' for user '" + user + "'");
                    client.genericKubernetesResources(context)
                            .inNamespace("globex-gitops-" + user).withName(application).delete();
                    return new JsonObject().put("status", "ok")
                            .put("application", new JsonObject().put("deployed", true).put("deleting", true)
                            .put("status", "")
                            .put("health", ""));
                })
                .onItem().transform(resp -> Response.ok(resp.toString()).build())
                .onFailure().recoverWithItem(throwable -> {
                    LOGGER.error("Exception while getting modules for user " + user, throwable);
                    return Response.serverError().build();
                });
    }

    private String getUser(HttpHeaders headers) {
        List<String> userTokenHeader =  headers.getRequestHeader("X-Forwarded-User");
        if (userTokenHeader == null || userTokenHeader.isEmpty()) {
            LOGGER.warn("Header 'X-Forwarded-User' not present!");
            return System.getenv("TEST_USER");
        }
        return userTokenHeader.get(0);
    }
    private List<GenericKubernetesResource> listApplicationsForUser(String user) {
        try {
            //Application cr
            ResourceDefinitionContext context = new ResourceDefinitionContext.Builder()
                    .withGroup("argoproj.io")
                    .withVersion("v1alpha1")
                    .withKind("Application")
                    .withPlural("applications")
                    .withNamespaced(true)
                    .build();

            return client.genericKubernetesResources(context)
                    .inNamespace("globex-gitops-" + user)
                    .list().getItems();

        } catch (Exception e) {
            LOGGER.error("Exception while listing Applications for user " + user, e);
            return new ArrayList<>();
        }
    }

    private String getOpenShiftDomain() {
        List<Route> routes = client.routes().inNamespace(namespace).list().getItems();
        if (routes.isEmpty()) {
            throw new RuntimeException("No routes found in namespace " + namespace);
        }
        String host = routes.get(0).getSpec().getHost();
        Pattern pattern = Pattern.compile(".*\\.(apps\\..*)");
        Matcher matcher = pattern.matcher(host);
        if (matcher.find())
        {
            return matcher.group(1);
        } else {
            throw new RuntimeException("Can't extract domain form host " + host);
        }
    }
}
