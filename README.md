# workshop-deployer

```
mvn package
docker build -f src/main/docker/Dockerfile.jvm -t quay.io/cloud-architecture-workshop/workshop-deployer:<tag> .
docker push quay.io/cloud-architecture-workshop/workshop-deployer:<tag>
```


Environment variables needed to run this locally. These are default values. 

* export NAMESPACE=workshop-deployer
* export ALLOWED_MODULES_COUNT=2
* export BOOKBAG_NAMESPACE=bookbag
* export OPENSHIFT_DOMAIN=apps.cluster-2b2fp.2b2fp.sandbox2896.opentlc.com
* export USER_PASSWORD=openshift
* export ARGO_NAMESPACE_PREFIX=globex-gitops
* export TEST_USER=user1