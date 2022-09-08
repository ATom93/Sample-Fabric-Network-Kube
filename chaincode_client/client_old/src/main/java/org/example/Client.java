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

    private String dbAddress;
    private String walletName;
    private String encryptionPassword;
    private GatewayManager gatewayManager;
    private String channelName;
    private String chaincodeName = "basic";
    public SmartContract contract;


    public Client(
            String peerAddress, String channelName,
            String dbAddress, String walletName, String encryptionPassword, String userName
    ) {
        String user = userName;
        String mspId = "org1";
        WalletIdentityManager walletIdentityManager = null;

        this.dbAddress = dbAddress;
        this.walletName = walletName;
        this.encryptionPassword = encryptionPassword;
        this.channelName = channelName;

        IdentityManager identityManager = new IdentityManager();
        //recupero certificato e chiave peer per TLS
        Path peerTLSCertPath = FileSystems.getDefault().getPath(
                "..", "..", "crypto_material", "peerOrgs", "org1", "peer1",
                "tls", "signcerts", "cert.pem");

        try {
            X509Certificate TLSCert = identityManager.getCertificate(peerTLSCertPath);
            this.gatewayManager = new GatewayManager(peerAddress, TLSCert);

            walletIdentityManager = new WalletIdentityManager(
                    user, dbAddress, walletName, mspId, encryptionPassword);
            Gateway.Builder gatewayBuilder = gatewayManager.getGatewayBuilder(
                    walletIdentityManager.getIdentity(),
                    walletIdentityManager.getIdentitySigner()
            );
            this.contract = new SmartContract(channelName, chaincodeName, gatewayBuilder);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } /*finally {
            try {
                if (gatewayManager != null) {
                    gatewayManager.closeGRPCChannel();
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }*/
    }

    public static void main(String[] args)
            throws CommitException, GatewayException, InterruptedException, IOException, CertificateException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {

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
        //String chaincodeName = args[6];
        String chaincodeMethod = args[6];
        String argument = args[7];

        //System.out.println("ARGUMENT:\t"+argument+"\n");
        Client client = null;

        switch (chaincodeMethod) {
            case "CreateGara":
                /*
                    {
                       "codice": "gara10",
                       "oggetto": "oggetto di gara",
                       "descrizione": "descrizione",
                       "importoRDA": "4200",
                       "IVA_RDA": "15",
                       "termine_presentazione_offerte": "12/12/2021",
                       "tipo_aggiudicazione": "Offerta Economicamente Più Vantaggiosa",
                       "tipo_offerta": "A importo",
                       "numero_rilanci": "2",
                       "punteggio_tecnico": "20",
                       "punteggio_economico": "80",
                       "calcolo_punteggio_economico": "Per singola Posizione",
                       "criteriTecnici": {
                            "descrizione": "descrizione CT",
                            "punteggioTecnicoMax": "10"
                       }
                    }
                    */
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.CreateGara(argument, args[8]);
                //argomenti: eventType, jsonGara
                client.gatewayManager.closeGRPCChannel();
                break;
            case "ReadGara":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.ReadGara(argument);
                //argomenti: idGara
                client.gatewayManager.closeGRPCChannel();
                break;
            case "ReadInfoGara":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.ReadInfoGara(argument);
                client.gatewayManager.closeGRPCChannel();
                break;
            case "QueryTransaction":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.QueryTransaction(argument);
                client.gatewayManager.closeGRPCChannel();
                break;
            case "StateChange_InCorso":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.StateChange_InCorso(argument, args[8]);
                //argomenti: idEvento, idGara
                client.gatewayManager.closeGRPCChannel();
                break;
            case "InsertOffer":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.InsertOffer(argument, args[8], args[9]);
                //argomenti: idEvento, idGara, importo
                client.gatewayManager.closeGRPCChannel();
                break;
            case "StateChange_InValutazione":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.StateChange_InValutazione(argument, args[8]);
                //argomenti: idEvento, idGara
                client.gatewayManager.closeGRPCChannel();
                break;
            case "InsertAdministrativeEvaluation":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.InsertAdministrativeEvaluation(argument, args[8], args[9], args[10], args[11]);
                //argomenti: idEvento, idGara, id_fornitore, approvazione, motivazione
                client.gatewayManager.closeGRPCChannel();
                break;
            case "StateChange_Chiusura":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.StateChange_Chiusura(argument, args[8]);
                //argomenti: idEvento, idGara
                client.gatewayManager.closeGRPCChannel();
                break;
            case "ReadSubmittedOffer":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.ReadSubmittedOffer(argument);
                //argomenti: idGara
                client.gatewayManager.closeGRPCChannel();
                break;
            case "UpdateField":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.UpdateField(argument, args[8], args[9], args[10]);
                //argomenti: eventID, idGara, campo, nuovo valore
                client.gatewayManager.closeGRPCChannel();
                break;
            case "AddGaraEvent":
                client = new Client(
                        peerAddress,
                        channelName,
                        dbAddress,
                        walletName,
                        encryptionPassword,
                        userName);
                client.contract.AddGaraEvent(argument, args[8], args[9]);
                //argomenti: idGara, eventId, eventType
                client.gatewayManager.closeGRPCChannel();
                break;
        }
    }


}