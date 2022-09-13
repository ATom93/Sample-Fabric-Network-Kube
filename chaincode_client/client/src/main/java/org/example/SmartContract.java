package org.example;

import org.hyperledger.fabric.client.*;
import java.nio.charset.StandardCharsets;

public class SmartContract implements SmartContractInterface {

    String channelName;
    String chaincodeName;
    Gateway.Builder gatewayBuilder;

    /**
     * Initialize Smart Contract object
     *
     * @param channelName Name of the channel used to invoke Smart Contract methods
     * @param chaincodeName Name of the chaincode deployed on the channel
     * @param gatewayBuilder Gateway builder with configuration to connect to blockchain infrastructure
     */
    public SmartContract(String channelName, String chaincodeName, Gateway.Builder gatewayBuilder) {
        this.chaincodeName = chaincodeName;
        this.channelName = channelName;
        this.gatewayBuilder =  gatewayBuilder;
    }

    private Contract GetContract(Gateway.Builder builder) {
        Gateway gateway = builder.connect();

        Network network = gateway.getNetwork(channelName);

        Contract contract = network.getContract(chaincodeName);

        return contract;
    }

    /**
     * Create a new asset on the blockchain to keep track of
     *
     * @param ID Unique identifier of the asset
     * @param eventID Unique identifier of the event of creation of the asset (first event for the asset)
     * @param eventType Type of the event
     * @param eventDate Timestamp of the event
     * @param payload Payload of the received event
     * @return
     */
    @Override
    public String CreateAsset(
            String ID, String eventID, String eventType, String eventDate, String payload) {

        String output = "";
        Contract contract = GetContract(this.gatewayBuilder);

        try {

            System.out.println("\nsubmitting 'CreateAsset' Transaction...");
            byte[] result = contract.submitTransaction(
                    "CreateAsset", ID, eventID, eventType, eventDate, payload
            );
            output = new String(result, StandardCharsets.UTF_8);

        } catch (EndorseException e) {
            e.printStackTrace();
        } catch (CommitException e) {
            e.printStackTrace();
        } catch (SubmitException e) {
            e.printStackTrace();
        } catch (CommitStatusException e) {
            e.printStackTrace();
        } catch (GatewayException e) {
            e.printStackTrace();
        }
        return output;
    };

    /**
     * Add a new event for an asset already registered on the blockchain
     *
     * @param ID Unique identifier of the asset
     * @param eventID Unique identifier of the event
     * @param eventType Type of the event to be registered on the asset
     * @param eventDate Timestamp of the event
     * @param eventPayload Payload of the received event
     * @return
     */
    @Override
    public String AddAssetEvent(
            String ID, String eventID, String eventType, String eventDate, String eventPayload) {

        String output = "";
        Contract contract = GetContract(this.gatewayBuilder);

        try {

            System.out.println("\nsubmitting 'AddAssetEvent' Transaction...");
            byte[] result = contract.submitTransaction(
                    "AddAssetEvent", ID, eventID, eventType, eventDate
            );
            output = new String(result, StandardCharsets.UTF_8);

        } catch (EndorseException e) {
            e.printStackTrace();
        } catch (CommitException e) {
            e.printStackTrace();
        } catch (SubmitException e) {
            e.printStackTrace();
        } catch (CommitStatusException e) {
            e.printStackTrace();
        } catch (GatewayException e) {
            e.printStackTrace();
        }
        return output;
    };

    /**
     * Get a specific asset information
     *
     * @param ID Identifier of the asset to read from the blockchain
     * @return A string with asset information registered on the blockchain
     */
    @Override
    public String ReadAsset(String ID) {

        Contract contract = GetContract(this.gatewayBuilder);
        String output = "";

        try {

            System.out.println("\nsubmitting 'ReadAsset' Transaction...");
            byte[] result = contract.submitTransaction(
                    "ReadAsset", ID
            );
            System.out.println(new String(result, StandardCharsets.UTF_8));
            output = result.toString();

        } catch (EndorseException e) {
            e.printStackTrace();
        } catch (CommitException e) {
            e.printStackTrace();
        } catch (SubmitException e) {
            e.printStackTrace();
        } catch (CommitStatusException e) {
            e.printStackTrace();
        } catch (GatewayException e) {
            e.printStackTrace();
        }
        return output;
    };


}
