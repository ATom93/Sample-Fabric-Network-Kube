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
import org.json.JSONObject;


@Contract()
@Default
public final class GaraSmartContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        INVALID_DATA,
        NOT_ALLOWED,
        EXCEPTION;
    }

    private String[] states = {"IN_PREPARAZIONE","IN_CORSO", "IN_VALUTAZIONE","CHIUSA"};

    //handler eseguito prima dell'invocazione di ogni smart contract
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
                    String errorMessage = String.format("%s not allowed to invoke %s", clientRole/*mspId*/, functionName);
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
                    String errorMessage = String.format("%s not allowed to invoke %s", clientRole/*mspId*/, functionName);
                    System.out.println(errorMessage);
                    throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
                }
            }
        }
    }


    /**
     * Creates a new asset on the ledger.
     *
     * @param ctx the transaction context
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String CreateGara(final Context ctx, final String eventID, final String JSONGara) {
        ChaincodeStub stub = ctx.getStub();

        JSONObject info_gara = new JSONObject(JSONGara);
        String ID = info_gara.getString("codice");
        String tipoAggiudicazione = info_gara.getString("tipo_aggiudicazione");
        String tipoOfferta = info_gara.getString("tipo_offerta");
        String calcoloPunteggioEconomico = info_gara.optString("calcolo_punteggio_economico");
        int pesoPunteggioTecnico = info_gara.optInt("punteggio_tecnico");
        int pesoPunteggioEconomico = info_gara.optInt("punteggio_economico");
        JSONObject criteriTecnici = info_gara.optJSONObject("criteriTecnici");
        ClientIdentity ci = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di esistenza
        if (GaraExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s already exists", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        //controllo sui valori associati ai campi "tipoAggiudicazione" e "tipoOfferta"
        if ( !tipoAggiudicazione.equalsIgnoreCase("Offerta Economicamente Più Vantaggiosa") &&
             !tipoAggiudicazione.equalsIgnoreCase("Prezzo più basso") ) {
            String errorMessage = String.format("Tipo aggiudicazione non valido per l'asset con ID %s\nAsset non creato", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_DATA.toString());
        }
        if ( !tipoOfferta.equalsIgnoreCase("A importo") &&
             !tipoOfferta.equalsIgnoreCase("A percentuale di sconto") ) {
            String errorMessage = String.format("Tipo offerta non valido per l'asset con ID %s\nAsset non creato", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_DATA.toString());
        }
        if ( calcoloPunteggioEconomico != null &&
                !calcoloPunteggioEconomico.equalsIgnoreCase("Per singola posizione") &&
                !calcoloPunteggioEconomico.equalsIgnoreCase("Su totale offerta") ) {
            String errorMessage = String.format("Modalità di calcolo punteggio economico" +
                    " non valida per l'asset con ID %s\nAsset non creato", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_DATA.toString());
        }
        if (pesoPunteggioEconomico + pesoPunteggioTecnico != 100){
            String errorMessage = String.format("Pesi per il punteggio economico e per il punteggio tecnico" +
                    " non validi per l'asset con ID %s\nAsset non creato", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_DATA.toString());
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
                .setType("creazione gara")
                .setCreatedBy(ci.getId())
                .setTimestamp(Instant.now().getEpochSecond())
                .build();

        //creazione dell'asset relativo alla gara (contenuto nel messaggio PB "Gara")
        Asset asset = Asset.newBuilder()
                .setID(ID)
                .setCreatedBy(ci.getId())
                .setType("gara")
                .addEvents(event)
                .build();

        //creazione della gara
        Gara.Builder garaBuilder = Gara.newBuilder()
                //.setID(ID)
                .setStato(states[0])
                .setOggetto(info_gara.getString("oggetto"))
                .setDescrizione(info_gara.getString("descrizione"))
                .setImportoRDA(info_gara.getInt("importoRDA"))
                .setIvaRDA(info_gara.getInt("IVA_RDA"))
                .setTerminePresentazioni(info_gara.getString("termine_presentazione_offerte"))
                .setTipoAggiudicazione(tipoAggiudicazione)
                .setTipoOfferta(tipoOfferta)
                .setNumeroRilanci(info_gara.getString("numero_rilanci"))
                .setPesoPunteggioTecnico(pesoPunteggioTecnico)
                .setPesoPunteggioEconomico(pesoPunteggioEconomico)
                .setCalcoloPunteggioEconomico(calcoloPunteggioEconomico)
                .setAsset(asset);
                //.setCreatedBy(ci.getId());

        if (criteriTecnici != null) {
            TechnicalCriteria tc = TechnicalCriteria.newBuilder()
                    .setDescrizione(criteriTecnici.getString("descrizione"))
                    .setPunteggioMax(criteriTecnici.getInt("punteggioTecnicoMax"))
                    .build();
            garaBuilder.setCriteriTecnici(tc);
        }

        Gara gara = garaBuilder.build();

        //inserimento della gara nel ledger come array di byte
        stub.putState(ID, gara.toByteArray());
        stub.setEvent("Asset Created", gara.toByteArray());

        return gara.toString();
    }

    /*
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String Update_ImportoRDA(final Context ctx, final String ID, int newImporto) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        try {
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Gara gara = Gara.parseFrom(assetByte);

            //controllo sullo stato della gara: le informazioni di gara non possono essere più modificate
            //se la gara è in corso o in valutazione
            if ( !gara.getStato().equals(states[0]) ) {
                String errorMessage = String.format("Asset %s: modifica non consentita\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }

            //modifica della gara con il nuovo importo RDA
            newGara = gara.toBuilder()
                    .setImportoRDA(newImporto)
                    .build();

            //inserimento gara nel ledger
            stub.putState(ID, newGara.toByteArray());
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        stub.setEvent("Asset Modified", newGara.toByteArray());
        return newGara.toString();
    }
     */

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void UpdateField(final Context ctx, final String eventID, final String ID, String fieldName, String newValue) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            System.out.println("Parsing asset bytes");
            Gara gara = Gara.parseFrom(assetByte);
            Gara.Builder garaBuilder = gara.toBuilder();

            //controllo sullo stato della gara: le informazioni di gara non possono essere più modificate
            //se la gara è in corso o in valutazione
            if ( !gara.getStato().equals(states[0]) ) {
                String errorMessage = String.format("Asset %s: modifica non consentita\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }


            //modifica della gara
            Descriptors.FieldDescriptor fieldDescriptor = garaBuilder
                    .getDescriptorForType()
                    .findFieldByName(fieldName);
            garaBuilder.clearField(fieldDescriptor);
            try {
                System.out.println("Parsing newValue");
                int integerNewValue = Integer.parseInt(newValue);
                System.out.println("Setting newValue integer");
                garaBuilder.setField(fieldDescriptor, integerNewValue);
            } catch(NumberFormatException e) {
                System.out.println("Setting newValue");
                garaBuilder.setField(fieldDescriptor, newValue);
            }

            //registrazione dell'evento di modifica
            Asset asset = gara.getAsset().toBuilder()
                    .addEvents(Event.newBuilder()
                            .setEventId(eventID)
                            .setType("modifica gara")
                            .setCreatedBy(ci.getId())
                            .setTimestamp(Instant.now().getEpochSecond())
                            .build()).build();
            garaBuilder.setAsset(asset);

            //inserimento asset nel ledger
            stub.putState(ID, garaBuilder.build().toByteArray());
        } catch (InvalidProtocolBufferException e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //return newAsset.toString();
    }


    /**
     * Publish tender asset on the ledger.
     *
     * @param ctx the transaction context
     * @param ID asset identifier
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String StateChange_InCorso(final Context ctx, final String eventID, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Gara gara = Gara.parseFrom(assetByte);

            //controllo sullo stato della gara: la gara non può passare nello stato "in corso"
            //se il suo stato corrente non è "in preparazione"
            if ( !gara.getStato().equals(states[0]) ) {
                String errorMessage = String.format("Asset con ID %s in stato " +
                        gara.getStato() +
                        "\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }

            long unixTime = Instant.now().getEpochSecond();
            //modifica dell'asset con il nuovo stato
            Gara.Builder newGaraBuilder = gara.toBuilder()
                    .setStato(states[1])
                    .setDataInvioRFx(unixTime);

            //registrazione dell'evento di modifica
            Asset asset = gara.getAsset().toBuilder()
                    .addEvents(Event.newBuilder()
                            .setEventId(eventID)
                            .setType("modifica stato gara")
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setCreatedBy(ci.getId())
                            .build()).build();
            newGaraBuilder.setAsset(asset);
            newGara = newGaraBuilder.build();

            //inserimento asset nel ledger
            stub.putState(ID, newGara.toByteArray());

        } catch (Exception e) {
            //il blocco catch deve lanciare un'eccezione quando è necessario 
            //far restituire un codice di errore per impedire la generazione della transazione
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }
        stub.setEvent("Asset Modified", newGara.toByteArray());
        return newGara.toString();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertOffer(final Context ctx, final String eventID, final String ID, int importo) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara gara = null;
        Gara.Builder newGaraBuilder = null;
        ClientIdentity ci = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        try {
            gara = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: un'offerta non può essere presentata se la gara è in stato di preparazione o di valutazione
        String assetState = gara.getStato();
        if ( !assetState.equals(states[1]) ) {
            String errorMessage = "";
            errorMessage = String.format("Asset %s: inserimento offerta non consentito. Gara " +
                    assetState +
                    "", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
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

        //modifica dell'asset con l'inserimento dell'offerta
        newGaraBuilder = gara.toBuilder();

        newGaraBuilder.addOffers(
                Offer.newBuilder()
                        .setCreatedBy(ci.getId())
                        .setImporto(importo)
        );

        //registrazione dell'evento di modifica
        Asset asset = gara.getAsset().toBuilder()
                .addEvents(Event.newBuilder()
                        .setEventId(eventID)
                        .setType("inserimento offerta")
                        .setCreatedBy(ci.getId())
                        .setTimestamp(Instant.now().getEpochSecond())
                        .build())
                .build();
        newGaraBuilder.setAsset(asset);

        Gara newGara = newGaraBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newGara.toByteArray());

        //return this.ReadSubmittedOffer(ctx, ID);

    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertMessage(final Context ctx, final String eventID, final String ID, String message) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara gara = null;
        Gara.Builder newGaraBuilder = null;
        Gara newGara = null;
        ClientIdentity ci = null;
        JSONObject info_gara = null;


        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata e del JSON del messaggio
        try {
            info_gara = new JSONObject(message);
            gara = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: un messaggio non può essere inserito se la gara è in stato di preparazione
        String assetState = gara.getStato();
        if ( !assetState.equals(states[1]) ) {
            String errorMessage = String.format("Asset %s: inserimento offerta non consentito. Gara in fase di preparazione, non ancora pubblicata.", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
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

        //modifica dell'asset con l'inserimento del messaggio
        newGaraBuilder = gara.toBuilder();

        newGaraBuilder.addMessage(
                Message.newBuilder()
                        .setSender(info_gara.getString("mittente"))
                        .setReceiver(info_gara.getString("destinatario"))
                        .setObject(info_gara.getString("oggetto"))
                        .setText(info_gara.getString("testo"))
                        .setTimestamp(info_gara.getString("timestamp"))
        );

        //registrazione dell'evento
        Asset asset = gara.getAsset().toBuilder()
                .addEvents(Event.newBuilder()
                        .setEventId(eventID)
                        .setType("inserimento messaggio")
                        .setTimestamp(Instant.now().getEpochSecond())
                        .setCreatedBy(ci.getId())
                        .build())
                .build();
        newGaraBuilder.setAsset(asset);

        newGara = newGaraBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newGara.toByteArray());

        //return this.ReadSubmittedOffer(ctx, ID);

    }



    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String StateChange_InValutazione(final Context ctx, final String eventID, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Gara gara = Gara.parseFrom(assetByte);

            //controllo sullo stato della gara: la gara non può passare nello stato "in valutazione"
            // se il suo stato corrente non è "in corso"
            if ( !gara.getStato().equals(states[1]) ) {
                String errorMessage = String.format("Asset con ID %s nello stato " +
                        gara.getStato() +
                        "\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }

            //modifica dell'asset con il nuovo stato
            Gara.Builder newGaraBuilder = gara.toBuilder()
                    .setStato(states[2]);

            //registrazione dell'evento di modifica
            Asset asset = gara.getAsset().toBuilder()
                    .addEvents(Event.newBuilder()
                            .setEventId(eventID)
                            .setType("modifica stato gara da " + states[1] + " a " + states[2])
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setCreatedBy(ci.getId())
                            .build()).build();
            newGaraBuilder.setAsset(asset);
            newGara = newGaraBuilder.build();

            //inserimento asset nel ledger
            stub.putState(ID, newGara.toByteArray());

        } catch (Exception e) {
            //il blocco catch deve lanciare un'eccezione quando è necessario
            //far restituire un codice di errore per impedire la generazione della transazione
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }
        stub.setEvent("Asset Modified", newGara.toByteArray());
        return newGara.toString();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertAdministrativeEvaluation(
            final Context ctx,  String eventID, final String ID, String IDSupplier, String approval, String motivazione) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara gara = null;
        Gara.Builder newGaraBuilder = null;
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata e del JSON del messaggio
        try {
            gara = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: una valutazione non può essere inserita se la gara non è in stato di valutazione
        String assetState = gara.getStato();
        if ( !assetState.equals(states[2]) ) {
            String errorMessage = String.format("Asset %s: inserimento valutazione non consentito. Gara non in fase di valutazione.", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
        }

        //builder uguale all'asset originale
        newGaraBuilder = gara.toBuilder();

        //lista dei builder delle offerte nell'asset originale
        List<Offer.Builder> newAssetBuilder_OfferBuilderList = newGaraBuilder.getOffersBuilderList();
        List<Offer> offerList = new ArrayList<Offer>();

        //ciclo sui builder della lista dei builder delle offerte nell'asset originale
        for(Offer.Builder offer: newAssetBuilder_OfferBuilderList) {
            System.out.println("\nOfferta da:\t" + offer.getCreatedBy() + "\n");
            //selezione del builder scelto da modificare (creato dal supplier con l'ID passato)
            if (offer.getCreatedBy().contains(IDSupplier)) {
                //modifica del builder scelto con l'aggiunta del nuovo campo
                offer.setValutazioneAmministrativa(
                        Approval.newBuilder()
                                .setApprovato(approval)
                                .setMotivazione(motivazione)
                );
            }
            offerList.add(offer.build());
        }

        //cancellazione di tutte le offerte preesistenti
        newGaraBuilder.clearOffers();
        //aggiunta di tutte le offerte al builder copia dell'asset originale con l'offerta selezionata modificata
        newGaraBuilder.addAllOffers(
                offerList
        );

        //registrazione dell'evento di modifica
        Asset asset = gara.getAsset().toBuilder()
                .addEvents(Event.newBuilder()
                        .setEventId(eventID)
                        .setType("inserimento valutazione amministrativa")
                        .setTimestamp(Instant.now().getEpochSecond())
                        .setCreatedBy(ci.getId())
                        .build()).build();
        newGaraBuilder.setAsset(asset);

        newGara = newGaraBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newGara.toByteArray());
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertTechnicalEvaluation(final Context ctx, final String ID, String eventID, String IDSupplier, int punteggioTecnico) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara gara = null;
        Gara.Builder newGaraBuilder = null;
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata e del JSON del messaggio
        try {
            gara = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: una valutazione non può essere inserita se la gara non è in stato di valutazione
        String assetState = gara.getStato();
        if ( !assetState.equals(states[2]) ) {
            String errorMessage = String.format("Asset %s: inserimento valutazione non consentito. Gara non in fase di valutazione.", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
        }

        newGaraBuilder = gara.toBuilder();

        //lista dei builder delle offerte nell'asset originale
        List<Offer.Builder> newAssetBuilder_OfferBuilderList = newGaraBuilder.getOffersBuilderList();
        List<Offer> offerList = new ArrayList<Offer>();

        //ciclo sui builder della lista dei builder delle offerte nell'asset originale
        for(Offer.Builder offer: newAssetBuilder_OfferBuilderList) {
            System.out.println("\nOfferta da:\t" + offer.getCreatedBy() + "\n");
            //selezione del builder scelto da modificare (creato dal supplier con l'ID passato)
            if (offer.getCreatedBy().contains(IDSupplier)) {
                //modifica del builder scelto con l'aggiunta del nuovo campo
                offer.setValutazioneTecnica(
                        TechnicalEvaluation.newBuilder()
                                .setPunteggioTecnico(punteggioTecnico)
                );
            }
            offerList.add(offer.build());
        }

        //cancellazione di tutte le offerte preesistenti
        newGaraBuilder.clearOffers();
        //aggiunta di tutte le offerte al builder copia dell'asset originale con l'offerta selezionata modificata
        newGaraBuilder.addAllOffers(
                offerList
        );

        //registrazione dell'evento di modifica
        Asset asset = gara.getAsset().toBuilder()
                .addEvents(Event.newBuilder()
                        .setEventId(eventID)
                        .setType("inserimento valutazione tecnica")
                        .setTimestamp(Instant.now().getEpochSecond())
                        .setCreatedBy(ci.getId())
                        .build()).build();
        newGaraBuilder.setAsset(asset);

        newGara = newGaraBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newGara.toByteArray());
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertEconomicalEvaluation(final Context ctx, final String ID, String eventID, String IDSupplier, int punteggioEconomico) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara gara = null;
        Gara.Builder newGaraBuilder = null;
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata e del JSON del messaggio
        try {
            gara = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: una valutazione non può essere inserita se la gara non è in stato di valutazione
        String assetState = gara.getStato();
        if ( !assetState.equals(states[2]) ) {
            String errorMessage = String.format("Asset %s: inserimento valutazione non consentito. Gara non in fase di valutazione.", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
        }

        newGaraBuilder = gara.toBuilder();

        //lista dei builder delle offerte nell'asset originale
        List<Offer.Builder> newAssetBuilder_OfferBuilderList = newGaraBuilder.getOffersBuilderList();
        List<Offer> offerList = new ArrayList<Offer>();

        //ciclo sui builder della lista dei builder delle offerte nell'asset originale
        for(Offer.Builder offer: newAssetBuilder_OfferBuilderList) {
            System.out.println("\nOfferta da:\t" + offer.getCreatedBy() + "\n");
            //selezione del builder scelto da modificare (creato dal supplier con l'ID passato)
            if (offer.getCreatedBy().contains(IDSupplier)) {
                //modifica del builder scelto con l'aggiunta del nuovo campo
                offer.setValutazioneEconomica(
                        EconomicalEvaluation.newBuilder()
                                .setPunteggioEconomico(punteggioEconomico)
                );
            }
            offerList.add(offer.build());
        }

        //cancellazione di tutte le offerte preesistenti
        newGaraBuilder.clearOffers();
        //aggiunta di tutte le offerte al builder copia dell'asset originale con l'offerta selezionata modificata
        newGaraBuilder.addAllOffers(
                offerList
        );

        //registrazione dell'evento di modifica
        Asset asset = gara.getAsset().toBuilder()
                .addEvents(Event.newBuilder()
                        .setEventId(eventID)
                        .setType("inserimento valutazione economica")
                        .setTimestamp(Instant.now().getEpochSecond())
                        .setCreatedBy(ci.getId())
                        .build()).build();
        newGaraBuilder.setAsset(asset);


        newGara = newGaraBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newGara.toByteArray());
    }



    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String StateChange_Chiusura(final Context ctx, final String eventID, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Gara gara = Gara.parseFrom(assetByte);

            //controllo sullo stato della gara: la gara non può passare nello stato "chiusa"
            //se il suo stato corrente non è "in valutazione"
            if ( !gara.getStato().equals(states[2]) ) {
                String errorMessage = String.format("Asset con ID %s non in stato 'in valutazione'\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }

            //modifica dell'asset con il nuovo stato
            Gara.Builder newGaraBuilder = gara.toBuilder()
                    .setStato(states[3]);

            //registrazione dell'evento di modifica
            Asset asset = gara.getAsset().toBuilder()
                    .addEvents(Event.newBuilder()
                            .setEventId(eventID)
                            .setType("modifica stato gara da " + states[2] + " a " + states[3])
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setCreatedBy(ci.getId())
                            .build())
                    .build();
            newGaraBuilder.setAsset(asset);
            newGara = newGaraBuilder.build();

            //inserimento asset nel ledger
            stub.putState(ID, newGara.toByteArray());

        } catch (Exception e) {
            //il blocco catch deve lanciare un'eccezione quando è necessario
            //far restituire un codice di errore per impedire la generazione della transazione
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }
        stub.setEvent("Asset Modified", newGara.toByteArray());
        return newGara.toString();
    }



    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void AddGaraEvent(final Context ctx, final String ID, final String eventID, final String eventType) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara newGara = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
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
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Gara gara = Gara.parseFrom(assetByte);


            //registrazione dell'evento di modifica
            Asset asset = gara.getAsset().toBuilder()
                    .addEvents(Event.newBuilder()
                            .setEventId(eventID)
                            .setType(eventType)
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setCreatedBy(ci.getId())
                            .build())
                    .build();
            newGara = gara.toBuilder().setAsset(asset).build();

            //inserimento asset nel ledger
            stub.putState(ID, newGara.toByteArray());

        } catch (Exception e) {
            //il blocco catch deve lanciare un'eccezione quando è necessario
            //far restituire un codice di errore per impedire la generazione della transazione
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }
        stub.setEvent("Asset Modified", newGara.toByteArray());
    }


    /**
     * Retrieves an asset with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param ID the ID of the asset
     * @return the asset found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadGara(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara asset = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, 
                AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        try {
            asset = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        return asset.toString();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadInfoGara(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara asset = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage,
                    AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        String message = "";
        try {
            asset = Gara.parseFrom(assetByte);
            String stato = asset.getStato();
            message = "ID:\t" + asset.getAsset().getID() + "\n";
            message += "STATO:\t" + stato + "\n";
            if (!stato.equals(states[0])) {
                message += "DATA INVIO RFx\t" + asset.getDataInvioRFx() + "\n";
            }
            message += "OGGETTO:\t" + asset.getOggetto()  + "\n";
            message += "DESCRIZIONE:\t" + asset.getDescrizione()  + "\n";
            message += "IMPORTO RDA:\t" + asset.getImportoRDA() + "\n";
            message += "TERMINE PRESENTAZIONI:\t" + asset.getTerminePresentazioni()  + "\n";
            message += "TIPO AGGIUDICAZIONE:\t" + asset.getTipoAggiudicazione()  + "\n";
            message += "TIPO OFFERTA:\t" + asset.getTipoOfferta()  + "\n";
            message += "IVA RDA:\t" + asset.getIvaRDA()  + "\n";
            message += "NUMERO RILANCI:\t" + asset.getNumeroRilanci()  + "\n";
            message += (asset.hasPesoPunteggioTecnico()) ? "PESO PUNTEGGIO TECNICO:\t" + asset.getPesoPunteggioTecnico()  + "\n" : "PESO PUNTEGGIO TECNICO:\tN/A";
            message += (asset.hasPesoPunteggioEconomico()) ? "PESO PUNTEGGIO ECONOMICO:\t" + asset.getPesoPunteggioEconomico()  + "\n" : "PESO PUNTEGGIO ECONOMICO:\tN/A";
            message += (asset.hasCalcoloPunteggioEconomico()) ? "CALCOLO PUNTEGGIO ECONOMICO:\t" + asset.getCalcoloPunteggioEconomico()  + "\n" : "CALCOLO PUNTEGGIO ECONOMICO:\tN/A";
            if (asset.hasCriteriTecnici()) {
                TechnicalCriteria tc = asset.getCriteriTecnici();
                message += "DESCRIZIONE CRITERIO TECNICO:\t" + tc.getDescrizione() + "\n";
                message += "PUNTEGGIO MASSIMO CRITERIO TECNICO:\t" + tc.getPunteggioMax() + "\n";
            }

        } catch (Exception e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        return message;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadSubmittedOffer(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Gara gara = null;
        String userID;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!GaraExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage,
                    AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        try {
            gara = Gara.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing exception";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage,
                    AssetTransferErrors.EXCEPTION.toString());
        }

        //recupero ID dell'utente che ha invocato lo smart contract
        try {
            ClientIdentity ci = new ClientIdentity(stub);
            userID = ci.getId();
        } catch (Exception e) {
            String errorMessage = "ClientIdentity error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        List<Offer> offers = gara.getOffersList();
        Offer submittedOffer = null;

        //controllo dell'esistenza di un'offerta sottomessa dall'utente che ha invocato questo s.c.
        //recupero offerta dell'utente in caso di esistenza
        //eccezione in caso di assenza di un'offerta creata dall'utente richiedente per la gara
        boolean offerExists = false;
        for (Offer offer: offers) {
            if (offer.getCreatedBy().equals(userID)) {
                offerExists = true;
                submittedOffer = offer;
            }
        }
        if (offerExists == false) {
            return "Nessuna offerta presentata";
        } else {
            String message = "CREATED BY:\t" + submittedOffer.getCreatedBy() + "\n";
            message += "IMPORTO:\t" + submittedOffer.getImporto();
            if ( gara.getStato().equals(states[3]) ) {
                message += "VALUTAZIONE AMMINISTRATIVA:\t" + submittedOffer.getValutazioneAmministrativa() + "\n";
                message += "VALUTAZIONE TECNICA:\t" + submittedOffer.getValutazioneTecnica() + "\n";
                message += "VALUTAZIONE ECONOMICA:\t" + submittedOffer.getValutazioneEconomica() + "\n";
            }
            return message;
        }
    }



    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx the transaction context
     * @param ID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean GaraExists(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        boolean exists = true;

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        //se il parsing non va a buon fine (blocco catch) o se l'ID dell'asset parsato è diverso da quello passato
        //allora si assume che l'asset non esiste nel ledger 
        try {
            Gara asset = Gara.parseFrom(assetByte);
            exists = asset.getAsset().getID().equals(ID);
        } catch (Exception e) {
            exists = false;
        }

        return exists;
    }





    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String CreateAsset(final Context ctx, final String ID, String type, String eventID) {
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
                .setType("creazione asset")
                .setTimestamp(Instant.now().getEpochSecond())
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
    public String AddAssetEvent(final Context ctx, final String ID, final String eventID, final String eventType) {
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
                            .setTimestamp(Instant.now().getEpochSecond())
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