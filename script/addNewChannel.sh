CHANNEL_NAME=$1

ID_POD_cli1=$(kubectl get pods -l io.kompose.service=cli-org1 -o=name)
ID_POD_cli2=$(kubectl get pods -l io.kompose.service=cli-org2 -o=name)

mkdir ./crypto_material/channel-artifacts/${CHANNEL_NAME}

printf "\nNEW CHANNEL GENESIS BLOCK\n"
./bin/configtxgen -profile genesis -outputBlock ./crypto_material/channel-artifacts/${CHANNEL_NAME}/${CHANNEL_NAME}_genesis.block -channelID $CHANNEL_NAME -configPath ./crypto_material/config_files
./bin/configtxgen -profile default -outputCreateChannelTx ./crypto_material/channel-artifacts/${CHANNEL_NAME}/$CHANNEL_NAME.tx -channelID $CHANNEL_NAME -configPath ./crypto_material/config_files

sleep 10s

printf "\nCHANNEL CREATION\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer channel create -c $CHANNEL_NAME -f ./channel-artifacts/$CHANNEL_NAME/$CHANNEL_NAME.tx --outputBlock ./channel-artifacts/$CHANNEL_NAME/$CHANNEL_NAME.block -o orderer1-org0-com:30750  --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/msp/tlscacerts/server.crt 

printf "\nPEER_ORG1 JOIN CHANNEL\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer channel join -b ./channel-artifacts/$CHANNEL_NAME/$CHANNEL_NAME.block -o orderer1-org0-com:30750 --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/msp/tlscacerts/server.crt

printf "\nPEER_ORG2 JOIN CHANNEL\n"
kubectl exec --stdin --tty $ID_POD_cli2 -- peer channel join -b ./channel-artifacts/$CHANNEL_NAME/$CHANNEL_NAME.block -o orderer1-org0-com:30750 --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/msp/tlscacerts/server.crt

printf "\nPEER ORG1 CHANNEL LIST\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer channel list
printf "\nPEER ORG2 CHANNEL LIST\n"
kubectl exec --stdin --tty $ID_POD_cli2 -- peer channel list

kubectl get pods 

#-------------------------------------------------------------------------------------------------------------------------------------------------------------

PACKAGE_ID_1=$(kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode queryinstalled | grep -oP 'Package ID: \K.*(?=,)')
PACKAGE_ID_2=$(kubectl exec --stdin --tty $ID_POD_cli2 -- peer lifecycle chaincode queryinstalled | grep -oP 'Package ID: \K.*(?=,)')

printf "\nCHAINCODE APPROVAL --> PEER ORG1\n"
#CLI_ORG1
kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode approveformyorg --package-id $PACKAGE_ID_1 --name basic -v 0 --sequence 1 --channelID $CHANNEL_NAME --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/peer1/tls/cacerts/cacert-30854.pem --waitForEvent 

printf "\nCHAINCODE APPROVAL --> PEER ORG2\n"
#CLI_ORG2
kubectl exec --stdin --tty $ID_POD_cli2 -- peer lifecycle chaincode approveformyorg --package-id $PACKAGE_ID_2 --name basic -v 0 --sequence 1 --channelID $CHANNEL_NAME --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org2/peer1/tls/cacerts/cacert-30854.pem --waitForEvent 

printf "\nCOMMIT ON CHANNEL\n"
#CLI_ORG1
kubectl exec --stdin --tty $ID_POD_cli1 -- peer lifecycle chaincode commit --name basic -v 0 --sequence 1 --channelID $CHANNEL_NAME --peerAddresses peer1-org1-com:30751 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/peer1/tls/cacerts/cacert-30854.pem --peerAddresses peer1-org2-com:30756 --tlsRootCertFiles /other_certs/Org2/tls/cacerts/cacert-30854.pem --tls --cafile /other_certs/Org0/tls/cacerts/cacert-30854.pem --waitForEvent