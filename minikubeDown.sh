minikube delete

rm -rf ./bin
rm -rf ./builders
rm -rf ./config
rm -rf ./bootstrap.sh

rm -rf ./crypto_material/ca
rm -rf ./crypto_material/channel-artifacts
rm -rf ./crypto_material/ordererOrgs
rm -rf ./crypto_material/peerOrgs
rm -rf ./crypto_material/rootca-admin
rm -rf ./crypto_material/roottlsca-admin
rm -rf ./crypto_material/tlsca
rm -rf ./crypto_material/other_certs

rm -rf ./crypto_material/ccaas/basic-org1.tgz
rm -rf ./crypto_material/ccaas/basic-org2.tgz
rm -rf ./crypto_material/ccaas/code.tar.gz