package org.example;

import org.hyperledger.fabric.client.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class SmartContract {

    String channelName;
    String chaincodeName;

    public SmartContract(String channelName, String chaincodeName) {
        this.chaincodeName = chaincodeName;
        this.channelName = channelName;
    }

    private Contract GetContract(Gateway.Builder builder) {
        Gateway gateway = builder.connect();

        Network network = gateway.getNetwork(channelName);

        Contract contract = network.getContract(chaincodeName);

        return contract;
    }

    public void CreateAsset(Gateway.Builder builder, String gara) {

        Contract contract = GetContract(builder);

        Proposal proposal = null;
        Transaction transaction = null;
        SubmittedTransaction submittedTransaction = null;
        try {
            System.out.println("\nbuilding 'CreateAsset' transaction proposal...");
            proposal = contract.newProposal("CreateAsset")
                    .addArguments(gara)
                    .build();
            System.out.println("\ngetting transaction endorsment...");
            transaction = proposal.endorse();
            System.out.println("\nsubmitting transaction...");
            submittedTransaction = transaction.submitAsync();

            System.out.println("\nSubmitted transaction id:\t"+ submittedTransaction.getTransactionId());

        } catch (SubmitException e) {
            throw new RuntimeException(e);
        } catch (EndorseException e) {
            throw new RuntimeException(e);
        }

    }

    public void QueryTransaction(Gateway.Builder builder, String txID) {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");

            ChaincodeEventsRequest request = network.newChaincodeEventsRequest("basic")
                    .startBlock(1)
                    .build();

            try (CloseableIterator<ChaincodeEvent> eventIter = request.getEvents(
                    CallOption.deadlineAfter(5, TimeUnit.SECONDS))) {
                while (eventIter.hasNext()) {
                    ChaincodeEvent event = eventIter.next();
                    String transactionID = event.getTransactionId();
                    //System.out.println("\n<-- Chaincode event: " + event.getEventName() + " - " + transactionID);
                    if (transactionID.equals(txID)) {
                        System.out.println("Transaction with ID\t"+ transactionID + " committed");
                        break;
                    }
                }
            } catch (GatewayRuntimeException e) {
                System.out.println("Transaction not committed");
            }
        }
    }

    public void ReadFullAsset(Gateway.Builder builder, String idGara) {
        try {

            Contract contract = GetContract(builder);

            byte[] evaluateResult = contract.evaluateTransaction("ReadFullAsset", idGara);
            System.out.println("Query result: \n" + new String(evaluateResult, StandardCharsets.UTF_8));

        } catch (EndorseException e) {
            e.printStackTrace();
        } catch (SubmitException e) {
            e.printStackTrace();
        } catch (CommitStatusException e) {
            e.printStackTrace();
        } catch (GatewayException e) {
            e.printStackTrace();
        }
    }

    public void ReadAssetInfo(Gateway.Builder builder, String idGara) {
        try {

            Contract contract = GetContract(builder);

            byte[] evaluateResult = contract.evaluateTransaction("ReadAssetInfo", idGara);
            System.out.println("Query result: \n" + new String(evaluateResult, StandardCharsets.UTF_8));

        } catch (EndorseException e) {
            e.printStackTrace();
        } catch (SubmitException e) {
            e.printStackTrace();
        } catch (CommitStatusException e) {
            e.printStackTrace();
        } catch (GatewayException e) {
            e.printStackTrace();
        }
    }

    public void StateChange_InCorso(Gateway.Builder builder, String idGara) {
        try {

            Contract contract = GetContract(builder);

            System.out.println("\nsubmitting 'StateChange_InCorso' Transaction...");
            byte[] result = contract.submitTransaction(
                    "StateChange_InCorso",idGara
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
    }

    public void InsertOffer(Gateway.Builder builder, String idGara, String importo) {
        try {

            Contract contract = GetContract(builder);

            System.out.println("\nsubmitting 'InsertOffer' Transaction...");
            byte[] result = contract.submitTransaction(
                    "InsertOffer",idGara, importo
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
    }

    public void ReadSumbitterOffer(Gateway.Builder builder, String idGara) {
        try {

            Contract contract = GetContract(builder);

            System.out.println("\nsubmitting 'ReadSubmittedOffer' Transaction...");
            byte[] result = contract.submitTransaction(
                    "ReadSubmittedOffer", idGara
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
    }

}
