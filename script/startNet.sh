CRYPTO_PATH=$1
CLUSTER_IP=$2

printf "\neliminazione eventuali cartelle di deployment precedenti\n"
rm -rf $CRYPTO_PATH/tlsca
rm -rf $CRYPTO_PATH/ca
rm -rf $CRYPTO_PATH/ordererOrgs $CRYPTO_PATH/peerOrgs $CRYPTO_PATH/rootca-admin $CRYPTO_PATH/roottlsca-admin $CRYPTO_PATH/channel-artifacts
rm -rf $CRYPTO_PATH/other_certs $CRYPTO_PATH/other_certs

sleep 3

mkdir $CRYPTO_PATH/tlsca $CRYPTO_PATH/ca
mkdir -p $CRYPTO_PATH/other_certs/Org2/tls/cacerts $CRYPTO_PATH/other_certs/Org0/tls/cacerts

printf "\ncreazione directory MSP per le organizzazioni a cui fanno capo i nodi orderer ("ordererOrgs") e i nodi peer ("peerOrgs")"
printf "\ncartella "ordererOrgs/org0" MSP dell'organizzazione org0 (con certificati delle due CA e dell'amministratore) e di ciascun orderer\n"
mkdir -p $CRYPTO_PATH/ordererOrgs/org0/msp/cacerts
mkdir -p $CRYPTO_PATH/ordererOrgs/org0/msp/tlscacerts
mkdir -p $CRYPTO_PATH/ordererOrgs/org0/msp/admincerts
mkdir -p $CRYPTO_PATH/ordererOrgs/org0/orderer1
mkdir -p $CRYPTO_PATH/ordererOrgs/org0/orderer2

printf "\ncartella "peerOrgs/org1" con MSP dell'organizzazione org1 e di ciascun peer\n"
mkdir -p $CRYPTO_PATH/peerOrgs/org1/msp/cacerts
mkdir -p $CRYPTO_PATH/peerOrgs/org1/msp/tlscacerts
mkdir -p $CRYPTO_PATH/peerOrgs/org1/msp/admincerts
mkdir -p $CRYPTO_PATH/peerOrgs/org1/peer1

printf "\ncartella "peerOrgs/org2" con MSP dell'organizzazione org2 e di ciascun peer\n"
mkdir -p $CRYPTO_PATH/peerOrgs/org2/msp/cacerts
mkdir -p $CRYPTO_PATH/peerOrgs/org2/msp/tlscacerts
mkdir -p $CRYPTO_PATH/peerOrgs/org2/msp/admincerts
mkdir -p $CRYPTO_PATH/peerOrgs/org2/peer1

printf "\ncartelle per certificati amministratori CA e TLSCA\n"
mkdir -p $CRYPTO_PATH/rootca-admin
mkdir -p $CRYPTO_PATH/roottlsca-admin

printf "\nimportazione del file config.yaml per la definizione dei NodeOU\n" 
#all'interno delle directory dell'MSP di ciascuna organizzazione
cp $CRYPTO_PATH/config_files/config.yaml $CRYPTO_PATH/ordererOrgs/org0/msp/
cp $CRYPTO_PATH/config_files/config.yaml $CRYPTO_PATH/peerOrgs/org1/msp/
cp $CRYPTO_PATH/config_files/config.yaml $CRYPTO_PATH/peerOrgs/org2/msp/


printf "\ndeploymnent dei container per le Certification Authorities\n"
kubectl apply -f configFiles_kube/fabric-ca.yaml
kubectl apply -f configFiles_kube/fabric-tlsca.yaml

sleep 50s

kubectl get pods

#esecuzione script per i passi di registrazione ed enrollment 
#di tutte le identità per ogni organizzazione (per generazione certificati e chiavi)
./script/registration-enrollment.sh $CLUSTER_IP $CRYPTO_PATH


printf '\nrinominazione dei file delle chiavi private con uno stesso nome "server.key"\n'
mv $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/keystore/* $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/keystore/server.key
mv $CRYPTO_PATH/ordererOrgs/org0/orderer1/tls/keystore/* $CRYPTO_PATH/ordererOrgs/org0/orderer1/tls/keystore/server.key   #no tls dir

mv $CRYPTO_PATH/ordererOrgs/org0/orderer2/msp/keystore/* $CRYPTO_PATH/ordererOrgs/org0/orderer2/msp/keystore/server.key
mv $CRYPTO_PATH/ordererOrgs/org0/orderer2/tls/keystore/* $CRYPTO_PATH/ordererOrgs/org0/orderer2/tls/keystore/server.key   #no tls dir

mv $CRYPTO_PATH/peerOrgs/org1/peer1/msp/keystore/* $CRYPTO_PATH/peerOrgs/org1/peer1/msp/keystore/server.key
mv $CRYPTO_PATH/peerOrgs/org1/peer1/tls/keystore/* $CRYPTO_PATH/peerOrgs/org1/peer1/tls/keystore/server.key  #no tls dir

mv $CRYPTO_PATH/peerOrgs/org2/peer1/msp/keystore/* $CRYPTO_PATH/peerOrgs/org2/peer1/msp/keystore/server.key
mv $CRYPTO_PATH/peerOrgs/org2/peer1/tls/keystore/* $CRYPTO_PATH/peerOrgs/org2/peer1/tls/keystore/server.key

printf "\nrinominazione dei file dei certificati\n"
mv $CRYPTO_PATH/peerOrgs/org1/peer1/tls/cacerts/*.pem $CRYPTO_PATH/peerOrgs/org1/peer1/tls/cacerts/cacert-30854.pem
mv $CRYPTO_PATH/peerOrgs/org2/peer1/tls/cacerts/*.pem $CRYPTO_PATH/peerOrgs/org2/peer1/tls/cacerts/cacert-30854.pem
mv $CRYPTO_PATH/ordererOrgs/org0/orderer1/tls/cacerts/*.pem $CRYPTO_PATH/ordererOrgs/org0/orderer1/tls/cacerts/cacert-30854.pem
mv $CRYPTO_PATH/ordererOrgs/org0/orderer2/tls/cacerts/*.pem $CRYPTO_PATH/ordererOrgs/org0/orderer2/tls/cacerts/cacert-30854.pem


printf "\ncopia dei certificati delle Certification Authorities nelle apposite cartelle di ogni organizzazione\n"
cp $CRYPTO_PATH/tlsca/server.crt $CRYPTO_PATH/ordererOrgs/org0/msp/tlscacerts
cp $CRYPTO_PATH/tlsca/server.crt $CRYPTO_PATH/peerOrgs/org1/msp/tlscacerts
cp $CRYPTO_PATH/tlsca/server.crt $CRYPTO_PATH/peerOrgs/org2/msp/tlscacerts

cp $CRYPTO_PATH/ca/server.crt $CRYPTO_PATH/ordererOrgs/org0/msp/cacerts
cp $CRYPTO_PATH/ca/server.crt $CRYPTO_PATH/peerOrgs/org1/msp/cacerts
cp $CRYPTO_PATH/ca/server.crt $CRYPTO_PATH/peerOrgs/org2/msp/cacerts

cp $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/ordererOrgs/org0/msp/admincerts/
cp $CRYPTO_PATH/peerOrgs/org1/peer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/peerOrgs/org1/msp/admincerts/
cp $CRYPTO_PATH/peerOrgs/org2/peer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/peerOrgs/org2/msp/admincerts/

mkdir $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/admincerts $CRYPTO_PATH/ordererOrgs/org0/orderer2/msp/admincerts $CRYPTO_PATH/peerOrgs/org1/peer1/msp/admincerts $CRYPTO_PATH/peerOrgs/org2/peer1/msp/admincerts
cp $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/admincerts/
cp $CRYPTO_PATH/ordererOrgs/org0/orderer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/ordererOrgs/org0/orderer2/msp/admincerts/

cp $CRYPTO_PATH/peerOrgs/org1/peer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/peerOrgs/org1/peer1/msp/admincerts/
cp $CRYPTO_PATH/peerOrgs/org2/peer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/peerOrgs/org2/peer1/msp/admincerts/

mkdir $CRYPTO_PATH/peerOrgs/org2/peer1/msp/user/admin/admincerts
cp $CRYPTO_PATH/peerOrgs/org2/peer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/peerOrgs/org2/peer1/msp/user/admin/admincerts/

mkdir $CRYPTO_PATH/peerOrgs/org1/peer1/msp/user/admin/admincerts
cp $CRYPTO_PATH/peerOrgs/org1/peer1/msp/user/admin/signcerts/cert.pem $CRYPTO_PATH/peerOrgs/org1/peer1/msp/user/admin/admincerts/


printf "\ngenerazione del blocco di genesi del canale\n"
mkdir $CRYPTO_PATH/channel-artifacts
printf "\nGENESIS BLOCK GENERATION\n"
./bin/configtxgen -profile genesis -outputBlock $CRYPTO_PATH/channel-artifacts/genesis.block -channelID mychannel -configPath $CRYPTO_PATH/config_files
printf "\nCHANNEL TRANSACTION GENERATION \n"
./bin/configtxgen -profile default -outputCreateChannelTx $CRYPTO_PATH/channel-artifacts/default.tx -channelID mychannel -configPath $CRYPTO_PATH/config_files

printf "\nCOPYING CRYPTO MATERIAL\n"
cp $CRYPTO_PATH/ordererOrgs/org0/orderer1/tls/cacerts/*-30854.pem $CRYPTO_PATH/other_certs/Org0/tls/cacerts/cacert-30854.pem
cp $CRYPTO_PATH/peerOrgs/org2/peer1/tls/cacerts/*-30854.pem $CRYPTO_PATH/other_certs/Org2/tls/cacerts/cacert-30854.pem


printf "\ndeployment dei container per gli orderer, i peer e le CLI amministratore per org1 e org2\n"
kubectl apply -f configFiles_kube/fabric-network.yaml

kubectl wait --for=condition=ready --all pod