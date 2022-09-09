cd database
docker-compose up -d 
cd ..

minikube start --mount-string="${PWD}/crypto_material:/crypto_material" --mount

./script/startNet.sh ./crypto_material $(minikube ip)

curl -X PUT http://admin:password@127.0.0.1:5984/wallet
CERT=$(cat ./crypto_material/peerOrgs/org1/peer1/tls/signcerts/cert.pem | sed ':a;N;$!ba;s/\n/ /g' | sed 's/ //g' | sed 's/BEGINCERTIFICATE/BEGIN CERTIFICATE/g' | sed 's/ENDCERTIFICATE/END CERTIFICATE/g')
DATA='{"data":"'$(echo $CERT)'"}'
echo $DATA
curl -X PUT http://admin:password@127.0.0.1:5984/wallet/peerCert -d ''"$DATA"''

./script/peerJoinChannel.sh

eval $(minikube -p minikube docker-env)

./script/chaincodePackagingInstallation.sh