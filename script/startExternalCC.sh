#build dell'immagine docker del chaincode ("chaincode_client/chaincode_java/dockerfile")
./script/buildExternalCC.sh

#deploy su Kubernetes dei container per il chaincode a partire dall'immagine
kubectl run org1-peer1-basic --image=chaincode/basic --image-pull-policy=Never --env "CHAINCODE_SERVER_ADDRESS=0.0.0.0:30751" --env "CHAINCODE_AS_A_SERVICE_BUILDER_CONFIG=\"{\"peername\":\"org1-peer1\"}\"" --env "CORE_CHAINCODE_ID_NAME=$1" --port 30751
kubectl run org2-peer1-basic --image=chaincode/basic --image-pull-policy=Never --env "CHAINCODE_SERVER_ADDRESS=0.0.0.0:30751" --env "CHAINCODE_AS_A_SERVICE_BUILDER_CONFIG=\"{\"peername\":\"org2-peer1\"}\"" --env "CORE_CHAINCODE_ID_NAME=$2" --port 30751
kubectl apply -f configFiles_kube/cc-services.yaml

kubectl wait --for=condition=ready --all pod


#kubectl delete pod org1-peer1-basic org2-peer1-basic
#kubectl delete svc org1-peer1-basic org2-peer1-basic
#docker rmi chaincode/basic:latest