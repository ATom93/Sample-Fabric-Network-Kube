package org.example;

import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

import javax.net.ssl.SSLException;


public class Client {

    public static void main(String[] args) throws CommitException, GatewayException, InterruptedException, IOException, CertificateException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {

        //BlockchainManager.init();
        //BlockchainManager.addChannel(
        //        args[0]
        //);

        String peerAddress = args[0];
        String dbAddress = args[1];
        String walletName = args[2];
        String userName = args[3];
        String encryptionPassword = args[4];
        String channelName = args[5];
        String chaincodeName = args[6];
        String chaincodeMethod = args[7];
        String argument = args[8];

        IdentityManager identityManager = new IdentityManager();

        //recupero certificato e chiave peer per TLS
        Path peerTLSCertPath = FileSystems.getDefault().getPath(
                "..", "..", "crypto_material", "peerOrgs", "org1", "peer1", "tls", "signcerts", "cert.pem");
        X509Certificate TLSCert = identityManager.getCertificate(peerTLSCertPath);
        GatewayManager gatewayManager = new GatewayManager(peerAddress, TLSCert);

        String user = userName;
        String mspId = "org1";
        WalletIdentityManager walletIdentityManager;
        try {
            walletIdentityManager = new WalletIdentityManager(user, dbAddress, walletName, mspId, encryptionPassword);
            Gateway.Builder gatewayBuilder = gatewayManager.getGatewayBuilder(
                    walletIdentityManager.getIdentity(),
                    walletIdentityManager.getIdentitySigner()
            );

            SmartContract contract = new SmartContract(channelName, chaincodeName);

            switch (chaincodeMethod) {
                case "createAsset":
                    /*
                    {
                       "codice": "gara10",
                       "oggetto": "oggetto di gara",
                       "descrizione": "descrizione",
                       "importoRDA": "4200",
                       "IVA_RDA": "15",
                       "termine_presentazione_offerte": "12/12/2021"
                       "tipo_aggiudicazione": "economicamente più vantaggiosa",
                       "tipo_offerta": "importo",
                       "numero_rilanci": "2",
                       OPZIONALI:
                       "punteggio_tecnico": <punteggio_tecnico>,
                       "punteggio_economico": <punteggio_economico>,
                       "calcolo_punteggio_economico": <calcoloPunteggioEconomico>
                    }
                    */
                    contract.CreateAsset(gatewayBuilder, argument); //argomenti: jsonGara
                    break;
                case "ReadFullAsset":
                    contract.ReadFullAsset(gatewayBuilder, argument); //argomenti: idGara
                    break;
                case "ReadAssetInfo":
                    contract.ReadAssetInfo(gatewayBuilder, argument);
                    break;
                case "QueryTransaction":
                    contract.QueryTransaction(gatewayBuilder, argument);
                    break;
                case "StateChange_InCorso":
                    contract.StateChange_InCorso(gatewayBuilder, argument);  //argomenti: idGara
                    break;
                case "InsertOffer":
                    contract.InsertOffer(gatewayBuilder, argument, args[9]); //argomenti: idGara, importo
                    break;
                case "ReadSubmittedOffer":
                    contract.ReadSumbitterOffer(gatewayBuilder, argument);  //argomenti: idGara
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            gatewayManager.closeGRPCChannel();
        }

    }

}