minikube start --mount-string="${PWD}/crypto_material:/crypto_material" --mount

./script/startNet.sh ./crypto_material $(minikube ip)

./script/peerJoinChannel.sh

eval $(minikube -p minikube docker-env)

./script/chaincodePackagingInstallation.sh