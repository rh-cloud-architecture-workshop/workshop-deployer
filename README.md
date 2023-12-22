# workshop-deployer

```
mvn package
docker build -f src/main/docker/Dockerfile.jvm -t quay.io/cloud-architecture-workshop/workshop-deployer:<tag> .
docker push quay.io/cloud-architecture-workshop/workshop-deployer:<tag>
```

Environment variables needed to run this locally

* export NAMESPACE=workshop-deployer
* export ALLOWED_MODULES_COUNT=2
* export BOOKBAG_NAMESPACE=bookbag
* export OPENSHIFT_DOMAIN=<url>
* export USER_PASSWORD=openshift