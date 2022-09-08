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
    Gateway gateway;

    public SmartContract(String channelName, String chaincodeName, Gateway.Builder gatewayBuilder) {
        this.chaincodeName = chaincodeName;
        this.channelName = channelName;
        this.gateway =  gatewayBuilder.connect();
        this.contract = this.GetContract(gatewayBuilder);
    }

    private Contract GetContract(Gateway.Builder builder) {
        Gateway gateway = builder.connect();

        Network network = gateway.getNetwork(channelName);

        Contract contract = network.getContract(chaincodeName);

        return contract;
    }

    @Override
    public void CreateGara(String eventID, String gara) {

        Proposal proposal = null;
        Transaction transaction = null;
        SubmittedTransaction submittedTransaction = null;
        try {
            System.out.println("\nbuilding 'CreateGara' transaction proposal...");
            proposal = contract.newProposal("CreateGara")
                    .addArguments(eventID, gara)
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

    @Override
    public void UpdateField(String eventID, String gara, String campo, String newValue) {
        try {

            System.out.println("\nsubmitting 'StateChange_InCorso' Transaction...");
            byte[] result = contract.submitTransaction(
                    "UpdateField",eventID,gara,campo,newValue
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


    @Override
    public void StateChange_InCorso(String eventID, String idGara) {
        try {


            System.out.println("\nsubmitting 'StateChange_InCorso' Transaction...");
            byte[] result = contract.submitTransaction(
                    "StateChange_InCorso",eventID, idGara
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

    @Override
    public void StateChange_InValutazione(String eventID, String idGara) {
        try {

            System.out.println("\nsubmitting 'StateChange_InValutazione' Transaction...");
            byte[] result = contract.submitTransaction(
                    "StateChange_InValutazione",eventID,idGara
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

    @Override
    public void StateChange_Chiusura(String eventID, String idGara) {
        try {


            System.out.println("\nsubmitting 'StateChange_Chiusura' Transaction...");
            byte[] result = contract.submitTransaction(
                    "StateChange_Chiusura",eventID, idGara
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


    @Override
    public void ReadGara(String idGara) {
        try {


            byte[] evaluateResult = contract.evaluateTransaction("ReadGara", idGara);
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

    @Override
    public void ReadInfoGara(String idGara) {
        try {

            byte[] evaluateResult = contract.evaluateTransaction("ReadInfoGara", idGara);
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

    @Override
    public void ReadSubmittedOffer(String idGara) {
        try {

            byte[] evaluateResult = contract.evaluateTransaction("ReadSubmittedOffer", idGara);
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


    @Override
    public void InsertAdministrativeEvaluation(String eventId, String ID, String IDSupplier, String approval, String motivazione) {
        try {

            System.out.println("\nsubmitting 'InsertAdministrativeEvaluation' Transaction...");
            byte[] result = contract.submitTransaction(
                    "InsertAdministrativeEvaluation",eventId, ID,IDSupplier,approval,motivazione
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

    /*
    public void InsertTechnicalEvaluation() {
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

    public void InsertEconomicalEvaluation() {
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
    */

    @Override
    public void InsertOffer(String eventId, String idGara, String importo) {
        try {

            System.out.println("\nsubmitting 'InsertOffer' Transaction...");
            byte[] result = contract.submitTransaction(
                    "InsertOffer",eventId, idGara, importo
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

    @Override
    public void InsertMessage(String eventId, String idGara, String messaggio) {
        try {

            System.out.println("\nsubmitting 'InsertMessage' Transaction...");
            byte[] result = contract.submitTransaction(
                    "InsertMessage",eventId,idGara, messaggio
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

    @Override
    public void AddGaraEvent(String idGara, String eventID, String eventType) {
        try {

            System.out.println("\nsubmitting 'AddGaraEvent' Transaction...");
            byte[] result = contract.submitTransaction(
                    "AddGaraEvent",idGara, eventID, eventType
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



    @Override
    public String CreateAsset(String ID, String type, String eventID) {
        return "";
    };

    @Override
    public String AddAssetEvent(String ID, String eventID, String eventType) {
        return "";
    };


    @Override
    public void QueryTransaction(String txID) {
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
