minikube start --mount-string="${PWD}/crypto_material:/crypto_material" --mount

./script/startCouchDBDatabase.sh

./script/startNet.sh ./crypto_material $(minikube ip)

./script/storePeerCert.sh admin:password@$(minikube ip) certificates

./script/peerJoinChannel.sh

eval $(minikube -p minikube docker-env)

./script/chaincodePackagingInstallation.sh