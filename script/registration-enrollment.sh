printf "\ndownload file binari di Hyperledger Fabric\n"
curl -sSL https://raw.githubusercontent.com/hyperledger/fabric/master/scripts/bootstrap.sh > ./bootstrap.sh | chmod 777 ./bootstrap.sh
./bootstrap.sh -s -d

# $1 corrisponde all'IP del cluster dato in input allo script
#creazione identità amministratore della Certification Authority
printf "\nADMIN CA"
export FABRIC_CA_HOME=$2/rootca-admin
./bin/fabric-ca-client enroll -u http://admin:adminpw@$1:30754 


printf "\nregistrazione identità presso la CA"
printf "\nREGISTRATION orderer1-org0-com CA\n"
./bin/fabric-ca-client register --id.name orderer1-org0-com --id.secret secret --id.type orderer -u http://$1:30754

printf "\nREGISTRATION orderer2-org0-com CA\n"
./bin/fabric-ca-client register --id.name orderer2-org0-com --id.secret secret --id.type orderer -u http://$1:30754


printf "\nREGISTRATION peer1-org1-com CA\n"
./bin/fabric-ca-client register --id.name peer1-org1-com --id.secret secret --id.type peer -u http://$1:30754

printf "\nREGISTRATION peer1-org2-com CA\n"
./bin/fabric-ca-client register --id.name peer1-org2-com --id.secret secret --id.type peer -u http://$1:30754


printf "\nREGISTRATION admin-org0-com CA\n"
./bin/fabric-ca-client register --id.name admin-org0-com --id.secret secret --id.type admin -u http://$1:30754

printf "\nREGISTRATION admin-org1-com CA\n"
./bin/fabric-ca-client register --id.name admin-org1-com --id.secret secret --id.type admin -u http://$1:30754

printf "\nREGISTRATION  admin-org2-com CA\n"
./bin/fabric-ca-client register --id.name admin-org2-com --id.secret secret --id.type admin -u http://$1:30754


printf "\nenrollment identità su CA e generazione materiale crittografico in percorso in FABRIC_CA_HOME"
printf "\nENROLLMENT admin-org0-com CA\n"
export FABRIC_CA_HOME=$2/ordererOrgs/org0/orderer1/msp/user/
./bin/fabric-ca-client enroll --csr.hosts "admin-org0-com" -u http://admin-org0-com:secret@$1:30754 -M admin 

printf "\nENROLLMENT admin-org1-com CA\n"
export FABRIC_CA_HOME=$2/peerOrgs/org1/peer1/msp/user/
./bin/fabric-ca-client enroll --csr.hosts "admin-org1-com" -u http://admin-org1-com:secret@$1:30754 -M admin

printf "\nENROLLMENT admin-org2-com CA\n"
export FABRIC_CA_HOME=$2/peerOrgs/org2/peer1/msp/user/
./bin/fabric-ca-client enroll --csr.hosts "admin-org2-com" -u http://admin-org2-com:secret@$1:30754 -M admin


printf "\nENROLLMENT orderer1-org0-com CA\n"
export FABRIC_CA_HOME=$2/ordererOrgs/org0/orderer1/
./bin/fabric-ca-client enroll --csr.hosts "orderer1-org0-com" -u http://orderer1-org0-com:secret@$1:30754

printf "\nENROLLMENT orderer2-org0-com CA\n"
export FABRIC_CA_HOME=$2/ordererOrgs/org0/orderer2/
./bin/fabric-ca-client enroll --csr.hosts "orderer2-org0-com" -u http://orderer2-org0-com:secret@$1:30754


printf "\nENROLLMENT peer1-org1-com CA\n"
export FABRIC_CA_HOME=$2/peerOrgs/org1/peer1/
./bin/fabric-ca-client enroll --csr.hosts "peer1-org1-com" -u http://peer1-org1-com:secret@$1:30754 

printf "\nENROLLMENT peer1-org2-com CA\n"
export FABRIC_CA_HOME=$2/peerOrgs/org2/peer1/
./bin/fabric-ca-client enroll --csr.hosts "peer1-org2-com" -u http://peer1-org2-com:secret@$1:30754


printf "\ncreazione identità amministratore della Certification Authority TLS"
printf "\nADMIN TLSCA\n"
export FABRIC_CA_HOME=$2/roottlsca-admin
./bin/fabric-ca-client enroll -u http://admin:adminpw@$1:30854


printf "\nregistrazione identità presso la CA TLS"
printf "\nREGISTRATION orderer1-org0-com TLSCA\n"
./bin/fabric-ca-client register --id.name orderer1-org0-com --id.secret secret --id.type orderer -u http://$1:30854

printf "\nREGISTRATION orderer2-org0-com TLSCA\n"
./bin/fabric-ca-client register --id.name orderer2-org0-com --id.secret secret --id.type orderer -u http://$1:30854


printf "\nREGISTRATION peer1-org1-com TLSCA\n"
./bin/fabric-ca-client register --id.name peer1-org1-com --id.secret secret --id.type peer -u http://$1:30854

printf "\nREGISTRATION peer1-org2-com TLSCA\n"
./bin/fabric-ca-client register --id.name peer1-org2-com --id.secret secret --id.type peer -u http://$1:30854


printf "\nenrollment identità su CA TLS e generazione materiale crittografico in percorso in FABRIC_CA_HOME"
printf "\n ENROLLMENT orderer1-org0-com TLSCA\n"
export FABRIC_CA_HOME=$2/ordererOrgs/org0/orderer1/
./bin/fabric-ca-client enroll --csr.hosts "orderer1-org0-com" -u http://orderer1-org0-com:secret@$1:30854 -M tls

printf "\nENROLLMENT orderer2-org0-com TLSCA\n"
export FABRIC_CA_HOME=$2/ordererOrgs/org0/orderer2/
./bin/fabric-ca-client enroll --csr.hosts "orderer2-org0-com" -u http://orderer2-org0-com:secret@$1:30854 -M tls


printf "\nENROLLMENT peer1-org1-com TLSCA\n"
export FABRIC_CA_HOME=$2/peerOrgs/org1/peer1/
./bin/fabric-ca-client enroll --csr.hosts "peer1-org1-com" -u http://peer1-org1-com:secret@$1:30854 -M tls 

printf "\nENROLLMENT peer1-org2-com TLSCA\n"
export FABRIC_CA_HOME=$2/peerOrgs/org2/peer1/
./bin/fabric-ca-client enroll --csr.hosts "peer1-org2-com" -u http://peer1-org2-com:secret@$1:30854 -M tls