package org.example;

import org.hyperledger.fabric.client.*;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import reactor.core.publisher.Mono;

public class Client {

    private GatewayManager gatewayManager;
    private String chaincodeName = "basic";
    public SmartContract contract;

    public Client(
            String peerAddress, String channelName, /*String chaincodeName,*/
            String dbAddress, String walletName, String encryptionPassword, String userName/*, String mspId*/
    ) {
        String user = userName;
        String mspId = "org1";              //organization of the user who invokes the smart contract
        WalletIdentityManager walletIdentityManager = null;

        IdentityManager identityManager = new IdentityManager();

        try {
            X509Certificate TLSCert = identityManager.getCertificateFromDatabase(dbAddress);
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
    /**
     * Invokes the smart contract to create a new asset on the blockchain
     * registering also the related creation event
     *
     * @param event CommonEvent object
     * @return A Mono with the string returned from the chaincode invocation
     * @throws ParseException
     */
    /*
    public Mono<String> CreateAsset(CommonEvent event) throws ParseException {

            String tenantId = event.getTenantId();
            String eventType = event.getEventType();
            String microService = eventType.split("_")[0];
            String eventDate = event.getEventDate();
            String eventId = tenantId + "_" + eventDate + "_" + eventType;

            //JSONObject e JSONParser da org.json.simple
            String eventPayload = event.getPayload();
            JSONObject eventPayloadJSON = (JSONObject) new JSONParser().parse(eventPayload);
            if (eventPayloadJSON.containsKey("id")) {
                String eventObjectId = (String) eventPayloadJSON.get("id");
                String id = tenantId + "_" + microService + "_" + eventObjectId;

                return Mono.just(
                            this.contract.CreateAsset(id, eventId, eventType, eventDate, eventPayload)
                );

            } else {
                return Mono.just("No asset ID");
            }

     }

    /**
     * Registers the received event on an asset previously registered on the blockchain.
     *
     * @param event CommonEvent object
     * @return A Mono with the string returned from the chaincode invocation
     * @throws ParseException
     */
    /*
     public Mono<String> AddAssetEvent(CommonEvent event) throws ParseException {

            String tenantId = event.getTenantId();
            String eventType = event.getEventType();
            String microService = eventType.split("_")[0];
            String eventDate = event.getEventDate();
            String eventId = tenantId + "_" + eventDate + "_" + eventType;

            //JSONObject e JSONParser da org.json.simple
            String eventPayload = event.getPayload();
            JSONObject eventPayloadJSON = (JSONObject) new JSONParser().parse(eventPayload);
            if (eventPayloadJSON.containsKey("id")) {
                String eventObjectId = (String) eventPayloadJSON.get("id");
                String id = tenantId + "_" + microService + "_" + eventObjectId;

                return Mono.just(
                            this.contract.AddAssetEvent(id, eventId, eventType, eventDate, eventPayload)
                );

            } else {
                return Mono.just("No asset ID");
            }

    }
    */

    public static void main(String[] args) {

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

        String result = "";

        switch (chaincodeMethod) {
            case "CreateAsset":
                result = client.contract.CreateAsset(
                        args[7], args[8], args[9], args[10], args[11]);
                System.out.println(result);
                //argomenti: ID_asset, eventID, eventType, eventDate, payload
                //client.gatewayManager.closeGRPCChannel();
                break;
            case "AddAssetEvent":
                result = client.contract.AddAssetEvent(
                        args[7], args[8], args[9], args[10], args[11]);
                System.out.println(result);
                //argomenti: ID_asset, eventID, eventType, eventDate, payload
                //client.gatewayManager.closeGRPCChannel();
                break;
            case "ReadAsset":
                result = client.contract.ReadAsset(
                        args[7]);
                System.out.println(result);
                //argomenti: ID_asset
                //client.gatewayManager.closeGRPCChannel();
                break;
        }
    }

}