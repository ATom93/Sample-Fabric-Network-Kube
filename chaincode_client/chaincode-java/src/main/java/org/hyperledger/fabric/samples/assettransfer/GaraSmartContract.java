/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import org.hyperledger.fabric.contract.ClientIdentity;

import org.hyperledger.fabric.samples.assettransfer.Asset;
import org.hyperledger.fabric.samples.assettransfer.AssetClass;
import org.hyperledger.fabric.samples.assettransfer.AssetOrBuilder;

import com.owlike.genson.Genson;


@Contract()
@Default
public final class GaraSmartContract implements ContractInterface {

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        NOT_ALLOWED,
        EXCEPTION;
    }

    //handler eseguito prima dell'invocazione di ogni smart contract
    /*
    @Override
    public void beforeTransaction(Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        //recupero del nome dello smart contract invocato
        String functionName = stub.getFunction();

        ClientIdentity ci = null;
        String mspId;
        String clientRole = "";

        //recupero ID dell'utente che ha invocato lo smart contract
        try {
            ci = new ClientIdentity(stub);
            mspId = ci.getMSPID();
            clientRole = ci.getAttributeValue("role");
        } catch (Exception e) {
            String errorMessage = "ClientIdentity exception";
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
        }

        System.out.println("\nInvoked smart contract:\t"+functionName+
                            "\nrequestor ID:\t"+ci.getId()+
                            "\nrequestor MSP ID:\t"+mspId+
                            "\nrequestor role:\t"+clientRole+
                            "\n");

        //l'accesso ai metodi del chaincode è ristretto solo ad una sola organizzazione di clienti
        if (!mspId.equals("org1")) {
            String errorMessage = String.format("%s not allowed to invoke the chaincode", mspId, functionName);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
        } else {
            //a seconda del metodo richiamato dall'utente, 
            //esso può avere o meno accesso a quell'operazione a seconda del ruolo all'interno dell'organizzazione
            if (
                functionName.equals("CreateGara") ||
                functionName.equals("StateChange_InCorso") ||
                functionName.equals("StateChange_InValutazione") ||
                functionName.equals("StateChange_Chiusura") ||
                functionName.equals("UpdateField") ||
                functionName.equals("ReadGara") ||
                functionName.equals("InsertAdministrativeEvaluation") ||
                functionName.equals("InsertTechnicalEvaluation") ||
                functionName.equals("InsertEconomicalEvaluation")
            ) {
                if (
                    !clientRole.equals("buyer")
                ) {
                    String errorMessage = String.format("%s not allowed to invoke %s", clientRole, functionName);
                    System.out.println(errorMessage);
                    throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
                }
            } else if (
                functionName.equals("InsertOffer") ||
                functionName.equals("ReadSubmittedOffer") ||
                functionName.equals("ReadInfoGara")
            ) {
                if (
                    !clientRole.equals("supplier")
                ) {
                    String errorMessage = String.format("%s not allowed to invoke %s", clientRole, functionName);
                    System.out.println(errorMessage);
                    throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
                }
            }
        }
    }
     */

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String CreateAsset(
            final Context ctx, final String ID, String type,
            String eventID, String eventType, String eventDate) {
        ChaincodeStub stub = ctx.getStub();
        ClientIdentity ci = null;

        //controllo esistenza asset con ID trasmesso come parametro
        //eccezione in caso di esistenza
        if (AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s already exists", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        //recupero ID dell'utente che ha invocato lo smart contract
        try {
            ci = new ClientIdentity(stub);
        }
        catch (Exception e) {
            String errorMessage = "ClientIdentity error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //evento di creazione della gara
        Event event = Event.newBuilder()
                .setEventId(eventID)
                .setType(eventType)
                .setTimestamp(eventDate)
                .setCreatedBy(ci.getId())
                .build();

        //creazione dell'asset relativo alla gara (contenuto nel messaggio PB "Gara")
        Asset asset = Asset.newBuilder()
                .setID(ID)
                .setCreatedBy(ci.getId())
                .setType(type)
                .addEvents(event)
                .build();

        //inserimento della gara nel ledger come array di byte
        stub.putState(ID, asset.toByteArray());
        stub.setEvent("Asset Created", asset.toByteArray());

        return asset.toString();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String AddAssetEvent(
            final Context ctx, final String ID,
            final String eventID, final String eventType, String eventDate) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset asset = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        ClientIdentity ci = null;
        //recupero ID dell'utente che ha invocato lo smart contract
        try {
            ci = new ClientIdentity(stub);
        }
        catch (Exception e) {
            String errorMessage = "ClientIdentity error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        try {

            //registrazione dell'evento
            asset = Asset.parseFrom(assetByte).toBuilder()
                    .addEvents(Event.newBuilder()
                            .setEventId(eventID)
                            .setType(eventType)
                            .setTimestamp(eventDate)
                            .setCreatedBy(ci.getId())
                            .build())
                    .build();

            //inserimento asset nel ledger
            stub.putState(ID, asset.toByteArray());

        } catch (Exception e) {
            //il blocco catch deve lanciare un'eccezione quando è necessario
            //far restituire un codice di errore per impedire la generazione della transazione
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }
        stub.setEvent("Asset Modified", asset.toByteArray());
        return asset.toString();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadAsset(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset asset = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage,
                    AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        try {
            asset = Asset.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        return asset.toString();
    }

    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx the transaction context
     * @param ID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        boolean exists = true;

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        //se il parsing non va a buon fine (blocco catch) o se l'ID dell'asset parsato è diverso da quello passato
        //allora si assume che l'asset non esiste nel ledger
        try {
            Asset asset = Asset.parseFrom(assetByte);
            exists = asset.getID().equals(ID);
        } catch (Exception e) {
            exists = false;
        }

        return exists;
    }

}