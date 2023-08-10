# workshop-deployer

```
mvn package
docker build -f src/main/docker/Dockerfile.jvm -t quay.io/cloud-architecture-workshop/workshop-deployer:<tag> .
docker push quay.io/cloud-architecture-workshop/workshop-deployer:<tag>
```