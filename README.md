# workshop-deployer

```
mvn package

docker build -f src/main/docker/Dockerfile.jvm -t quay.io/cloud-architecture-workshop/workshop-deployer:ac549f2 .

docker push quay.io/cloud-architecture-workshop/workshop-deployer:ac549f2

```


Environment variables needed to run this locally. These are default values. 

export NAMESPACE=workshop-deployer
export ALLOWED_MODULES_COUNT=2
export BOOKBAG_NAMESPACE=bookbag
export OPENSHIFT_DOMAIN=apps.cluster-psl4g.psl4g.sandbox1250.opentlc.com
export USER_PASSWORD=openshift
export ARGO_NAMESPACE_PREFIX=globex-gitops
export TEST_USER=user2