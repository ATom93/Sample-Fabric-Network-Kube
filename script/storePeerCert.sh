DB_ADDRESS=$1
DATABASE_NAME=$2

curl -X PUT http://$1:32764/$2

CERT=$(echo $(while read line; do echo "$line\n"; done < ./crypto_material/peerOrgs/org1/peer1/tls/signcerts/cert.pem) | sed "s/ //g" | sed 's/ENDCERTIFICATE/END CERTIFICATE/g' | sed 's/BEGINCERTIFICATE/BEGIN CERTIFICATE/g' | sed 's/-\n/-/g')
DATA='{"data":"'$(echo $CERT)'"}'

curl -X PUT http://$1:32764/$2/peerCert -d ''"$DATA"''