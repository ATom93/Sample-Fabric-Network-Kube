#recupero ID dei POD delle CLI a partire dalla label del deployment relativo (in fabric-network.yaml)
ID_POD_cli1=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -l io.kompose.service=cli-org1)
ID_POD_cli2=$(kubectl get pods --no-headers -o custom-columns=":metadata.name" -l io.kompose.service=cli-org2)

sleep 5

#aggiunta del nodo peer dell'organizzazione org1 al canale "mychannel"
#   il canale "mychannel" è riferito utilizzando il blocco di genesi "genesis.block" 
#   creato in "startNet.sh" tramite "configtxgen" passando il nome nel flag -channelID
printf "\nPEER ORG1 JOIN CHANNEL\n"
kubectl exec --stdin --tty $ID_POD_cli1 -- peer channel join -b channel-artifacts/genesis.block -o orderer1-org0-com:30750 --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/msp/tlscacerts/server.crt

sleep 5

#aggiunta del nodo peer dell'organizzazione org2 al canale "mychannel"
#   il canale "mychannel" è riferito utilizzando il blocco di genesi "genesis.block"
#   creato in "startNet.sh" tramite "configtxgen" passando il nome nel flag -channelID
printf "\nPEER ORG2 JOIN CHANNEL\n"
kubectl exec --stdin --tty $ID_POD_cli2 -- peer channel join -b channel-artifacts/genesis.block -o orderer1-org0-com:30750 --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/org1/msp/tlscacerts/server.crt