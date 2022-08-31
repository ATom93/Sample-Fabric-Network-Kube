#recupero ID dei POD delle CLI a partire dalla label del deployment relativo (in fabric-network.yaml)
ID_POD_cli1=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -l io.kompose.service=cli-org1)
ID_POD_cli2=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -l io.kompose.service=cli-org2)

#creazione archivi chaincode
printf "\nPACKAGING CHAINCODE\n"
tar cfz ./crypto_material/ccaas/code.tar.gz -C ./crypto_material/ccaas connection.json
tar cfz ./crypto_material/ccaas/basic-org1.tgz -C ./crypto_material/ccaas code.tar.gz metadata.json
tar cfz ./crypto_material/ccaas/basic-org2.tgz -C ./crypto_material/ccaas code.tar.gz metadata.json

#installazione chaincode sul peer dell'organizzazione org1
printf "\nINSTALLING CHAINCODE --> PEER ORG1\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode install /chaincode/basic-org1.tgz
PACKAGE_ID_1=$(kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode queryinstalled | grep -oP 'Package ID: \K.*(?=,)')

#installazione chaincode sul peer dell'organizzazione org2
printf "\nINSTALLING CHAINCODE --> PEER ORG2\n"
kubectl exec --stdin --tty $ID_POD_cli2 -- peer lifecycle chaincode install /chaincode/basic-org2.tgz
PACKAGE_ID_2=$(kubectl exec --stdin --tty $ID_POD_cli2 -- peer lifecycle chaincode queryinstalled | grep -oP 'Package ID: \K.*(?=,)')

sh ./script/startExternalCC.sh $PACKAGE_ID_1 $PACKAGE_ID_2

printf "\n\n"
kubectl get pods
printf "\n"
kubectl get services
printf "\n\n"

#approvazione definizione chaincode da parte del peer di org1
printf "\nCHAINCODE APPROVAL --> PEER ORG1\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode approveformyorg --package-id $PACKAGE_ID_1 --name basic -v 0 --sequence 1 --channelID mychannel --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/peer1/tls/cacerts/cacert-30854.pem --waitForEvent

#approvazione definizione chaincode da parte del peer di org2
printf "\nCHAINCODE APPROVAL --> PEER ORG2\n"
kubectl exec --stdin --tty $ID_POD_cli2 -- peer lifecycle chaincode approveformyorg --package-id $PACKAGE_ID_2 --name basic -v 0 --sequence 1 --channelID mychannel --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org2/peer1/tls/cacerts/cacert-30854.pem --waitForEvent

#commit del chaincode sul canale (effettuato dal peer dell'organizzazione org1)
printf "\nCOMMIT ON CHANNEL\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode commit --name basic -v 0 --sequence 1 --channelID mychannel --peerAddresses peer1-org1-com:30751 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/peer1/tls/cacerts/cacert-30854.pem --peerAddresses peer1-org2-com:30756 --tlsRootCertFiles /other_certs/Org2/tls/cacerts/cacert-30854.pem --tls --cafile /other_certs/Org0/tls/cacerts/cacert-30854.pem --waitForEvent