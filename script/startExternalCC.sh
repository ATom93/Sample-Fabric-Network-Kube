#build dell'immagine docker del chaincode ("chaincode_client/chaincode_java/dockerfile")
./script/buildExternalCC.sh

ID_POD_cli1=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -l io.kompose.service=cli-org1)
ID_POD_cli2=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -l io.kompose.service=cli-org2)
PACKAGE_ID_1=$(kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode queryinstalled | grep -oP 'Package ID: \K.*(?=,)')
PACKAGE_ID_2=$(kubectl exec --stdin --tty $ID_POD_cli2 -- peer lifecycle chaincode queryinstalled | grep -oP 'Package ID: \K.*(?=,)')

kubectl delete --ignore-not-found pod org1-peer1-basic org2-peer1-basic 
kubectl delete --ignore-not-found svc org1-peer1-basic org2-peer1-basic

#deploy su Kubernetes dei container per il chaincode a partire dall'immagine
kubectl run org1-peer1-basic --image=chaincode/basic --image-pull-policy=Never --env "CHAINCODE_SERVER_ADDRESS=0.0.0.0:30751" --env "CORE_PEER_ADDRESS=peer1-org1-com:30751" --env "CHAINCODE_AS_A_SERVICE_BUILDER_CONFIG=\"{\"peername\":\"org1-peer1\"}\"" --env "CORE_CHAINCODE_ID_NAME=$PACKAGE_ID_1" --port 30751
kubectl run org2-peer1-basic --image=chaincode/basic --image-pull-policy=Never --env "CHAINCODE_SERVER_ADDRESS=0.0.0.0:30751" --env "CORE_PEER_ADDRESS=peer1-org2-com:30756" --env "CHAINCODE_AS_A_SERVICE_BUILDER_CONFIG=\"{\"peername\":\"org2-peer1\"}\"" --env "CORE_CHAINCODE_ID_NAME=$PACKAGE_ID_2" --port 30751
kubectl apply -f configFiles_kube/cc-services.yaml

kubectl wait --for=condition=ready --all pod