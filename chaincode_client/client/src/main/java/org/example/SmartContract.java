package org.example;

import org.hyperledger.fabric.client.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class SmartContract implements SmartContractInterface {

    String channelName;
    String chaincodeName;
    Contract contract;
    Gateway.Builder gatewayBuilder;

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

    @Override
    public void CreateAsset(
            String ID, String type, String eventID, String eventType, String eventDate) {

        Contract contract = GetContract(this.gatewayBuilder);

        try {

            System.out.println("\nsubmitting 'CreateAsset' Transaction...");
            byte[] result = contract.submitTransaction(
                    "CreateAsset", ID, eventType, eventID, eventType, eventDate
            );
            System.out.println(new String(result, StandardCharsets.UTF_8));

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
    };

    @Override
    public void AddAssetEvent(
            String ID, String eventID, String eventType, String eventDate) {

        Contract contract = GetContract(this.gatewayBuilder);

        try {

            System.out.println("\nsubmitting 'AddAssetEvent' Transaction...");
            byte[] result = contract.submitTransaction(
                    "AddAssetEvent", ID, eventID, eventType, eventDate
            );
            System.out.println(new String(result, StandardCharsets.UTF_8));

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
    };

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
