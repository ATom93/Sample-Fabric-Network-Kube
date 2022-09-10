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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
        //recupero certificato peer per TLS
        Path peerTLSCertPath = FileSystems.getDefault().getPath(
                "..", "..", "crypto_material", "peerOrgs", "org1", "peer1",
                "tls", "signcerts", "cert.pem");

        try {
            X509Certificate TLSCert =
                    //identityManager.getCertificate(peerTLSCertPath);
                    identityManager.getCertificateFromDatabase();
            this.gatewayManager = new GatewayManager(peerAddress, TLSCert);

            walletIdentityManager = new WalletIdentityManager(
                    user, dbAddress, walletName, mspId, encryptionPassword);
            Gateway.Builder gatewayBuilder = gatewayManager.getGatewayBuilder(
                    walletIdentityManager.getIdentity(),
                    walletIdentityManager.getIdentitySigner()
            );
            this.contract = new SmartContract(channelName, chaincodeName,
                    gatewayBuilder);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*

    public Mono<String> CreateAsset(CommonEvent event) {

            String id = ...;
            String eventId = ...;
            String eventType = event.getEventType();
            String eventDate = event.getEventDate();
            String eventPayload = event.getPayload();

            return Mono.just(
                        this.contract.CreateAsset(id, eventId, eventType, eventDate, eventPayload)
            )


     }

     public Mono<String> AddAssetEvent(CommonEvent event) {

            String id = ...;
            String eventId = ...;
            String eventType = event.getEventType();
            String eventDate = event.getEventDate();
            String eventPayload = event.getPayload();

            return Mono.just(
                        this.contract.AddAssetEvent(id, eventId, eventType, eventDate, eventPayload)
            )

    }

    */

    public static void main(String[] args)
            throws CommitException, GatewayException, InterruptedException,
            IOException, CertificateException, InvalidKeyException,
            NoSuchAlgorithmException, InvalidKeySpecException {

        String peerAddress = args[0];
        String dbAddress = args[1];
        String walletName = args[2];
        String userName = args[3];
        String encryptionPassword = args[4];
        String channelName = args[5];
        String chaincodeMethod = args[6];

        Client client = new Client(
                peerAddress,
                channelName,
                dbAddress,
                walletName,
                encryptionPassword,
                userName);

        switch (chaincodeMethod) {
            case "CreateAsset":
                client.contract.CreateAsset(
                        args[7], args[8], args[9], args[10], args[11]);
                //argomenti: ID_asset, type_asset, eventID, eventType, eventDate
                //client.gatewayManager.closeGRPCChannel();
                break;
            case "AddAssetEvent":
                client.contract.AddAssetEvent(
                        args[7], args[8], args[9], args[10]);
                //argomenti: ID_asset, eventID, eventType, eventDate
                //client.gatewayManager.closeGRPCChannel();
                break;
            case "ReadAsset":
                client.contract.ReadAsset(
                        args[7]);
                //argomenti: ID_asset
                //client.gatewayManager.closeGRPCChannel();
                break;
        }
    }

}