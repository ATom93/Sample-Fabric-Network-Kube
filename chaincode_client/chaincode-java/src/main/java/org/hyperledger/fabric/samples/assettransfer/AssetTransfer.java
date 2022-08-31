/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import org.hyperledger.fabric.contract.ClientIdentity;

import org.hyperledger.fabric.samples.assettransfer.Asset;
import org.hyperledger.fabric.samples.assettransfer.AssetClass;
import org.hyperledger.fabric.samples.assettransfer.AssetOrBuilder;

import com.owlike.genson.Genson;
import org.json.JSONObject;


@Contract()
@Default
public final class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        INVALID_DATA,
        NOT_ALLOWED,
        EXCEPTION;
    }

    private String[] states = {"IN_PREPARAZIONE","IN_CORSO", "IN_VALUTAZIONE"};

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
                functionName.equals("CreateAsset") ||
                functionName.equals("StateChange_InCorso") ||
                functionName.equals("Update_ImportoRDA") ||
                functionName.equals("ReadFullAsset")
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
                functionName.equals("ReadAssetInfo")
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
    public String CreateAsset(final Context ctx, final String gara) {
        ChaincodeStub stub = ctx.getStub();

        JSONObject info_gara = new JSONObject(gara);
        String ID = info_gara.getString("codice");
        String tipoAggiudicazione = info_gara.getString("tipo_aggiudicazione");
        String tipoOfferta = info_gara.getString("tipo_offerta");

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di esistenza
        if (AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s already exists", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        //controllo sui valori associati ai campi "tipoAggiudicazione" e "tipoOfferta"
        if ( !tipoAggiudicazione.equalsIgnoreCase("economicamente più vantaggiosa") &&
             !tipoAggiudicazione.equalsIgnoreCase("prezzo più basso") ) {
            String errorMessage = String.format("Tipo aggiudicazione non valido per l'asset con ID %s\nAsset non creato", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_DATA.toString());
        }
        if ( !tipoOfferta.equalsIgnoreCase("importo") &&
             !tipoOfferta.equalsIgnoreCase("sconto") ) {
            String errorMessage = String.format("Tipo offerta non valido per l'asset con ID %s\nAsset non creato", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_DATA.toString());
        }

        //creazione dell'asset della gara
        Asset asset = Asset.newBuilder()
                .setID(ID)
                .setStato(states[0])
                .setOggetto(info_gara.getString("oggetto"))
                .setDescrizione(info_gara.getString("descrizione"))
                .setImportoRDA(info_gara.getInt("importoRDA"))
                .setIvaRDA(info_gara.getInt("IVA_RDA"))
                .setTerminePresentazioni(info_gara.getString("termine_presentazione_offerte"))
                .setTipoAggiudicazione(tipoAggiudicazione)
                .setTipoOfferta(tipoOfferta)
                .setNumeroRilanci(info_gara.getString("numero_rilanci"))
                .setPunteggioTecnico(info_gara.optInt("punteggio_tecnico"))
                .setPunteggioEconomico(info_gara.optInt("punteggio_economico"))
                .setCalcoloPunteggioEconomico(info_gara.optString("calcolo_punteggio_economico"))
                .build();

        //inserimento dell'asset della gara nel ledger come array di byte
        stub.putState(ID, asset.toByteArray());
        stub.setEvent("Asset Created", asset.toByteArray());

        return asset.toString();
    }

    /**
     * Publish tender asset on the ledger.
     *
     * @param ctx the transaction context
     * @param ID asset identifier
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String StateChange_InCorso(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset newAsset = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        try {
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Asset asset = Asset.parseFrom(assetByte);

            //controllo sullo stato della gara: la gara non può passare nello stato "in corso" se il suo stato corrente non è "in preparazione"
            if ( !asset.getStato().equals(states[0]) ) {
                String errorMessage = String.format("Asset con ID %s in stato di valutazione, già in corso o terminato\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }

            long unixTime = Instant.now().getEpochSecond();
            //modifica dell'asset con il nuovo stato
            newAsset = Asset.newBuilder()
                    .setID(asset.getID())
                    .setStato(states[1])
                    .setOggetto(asset.getOggetto())
                    .setDescrizione(asset.getDescrizione())
                    .setImportoRDA(asset.getImportoRDA())
                    .setTerminePresentazioni(asset.getTerminePresentazioni())
                    .setTipoAggiudicazione(asset.getTipoAggiudicazione())
                    .setTipoOfferta(asset.getTipoOfferta())
                    .setIvaRDA(asset.getIvaRDA())
                    .setNumeroRilanci(asset.getNumeroRilanci())
                    .setPunteggioTecnico(asset.getPunteggioTecnico())
                    .setPunteggioEconomico(asset.getPunteggioEconomico())
                    .setCalcoloPunteggioEconomico(asset.getCalcoloPunteggioEconomico())
                    .setDataInvioRFx(unixTime).build();

            //inserimento asset nel ledger
            stub.putState(ID, newAsset.toByteArray());

        } catch (Exception e) {
            //il blocco catch deve lanciare un'eccezione quando è necessario 
            //far restituire un codice di errore per impedire la generazione della transazione
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }
        stub.setEvent("Asset Modified", newAsset.toByteArray());
        return newAsset.toString();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String Update_ImportoRDA(final Context ctx, final String ID, int newImporto) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset newAsset = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        try {
            //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
            Asset asset = Asset.parseFrom(assetByte);

            //controllo sullo stato della gara: le informazioni di gara non possono essere più modificati se la gara è in corso o in valutazione
            if ( !asset.getStato().equals(states[0]) ) {
                String errorMessage = String.format("Asset %s: modifica non consentita\nAsset non aggiornato", ID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage, AssetTransferErrors.NOT_ALLOWED.toString());
            }

            //modifica dell'asset con il nuovo importo RDA
            newAsset = Asset.newBuilder()
                    .setID(asset.getID())
                    .setStato(asset.getStato())
                    .setOggetto(asset.getOggetto())
                    .setDescrizione(asset.getDescrizione())
                    .setImportoRDA(newImporto)
                    .setTerminePresentazioni(asset.getTerminePresentazioni())
                    .setTipoAggiudicazione(asset.getTipoAggiudicazione())
                    .setTipoOfferta(asset.getTipoOfferta())
                    .setIvaRDA(asset.getIvaRDA())
                    .setNumeroRilanci(asset.getNumeroRilanci())
                    .setPunteggioTecnico(asset.getPunteggioTecnico())
                    .setPunteggioEconomico(asset.getPunteggioEconomico())
                    .setCalcoloPunteggioEconomico(asset.getCalcoloPunteggioEconomico())
                    .build();

            //inserimento asset nel ledger
            stub.putState(ID, newAsset.toByteArray());
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        stub.setEvent("Asset Modified", newAsset.toByteArray());
        return newAsset.toString();
    }

    /**
     * Retrieves an asset with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param ID the ID of the asset
     * @return the asset found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadFullAsset(final Context ctx, final String ID) {
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

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadAssetInfo(final Context ctx, final String ID) {
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
        String message = "";
        try {
            asset = Asset.parseFrom(assetByte);
            message = "ID:\t" + asset.getID() + "\n";
            message += "STATO:\t" + asset.getStato() + "\n";
            message += "DATA INVIO RFx\t" + asset.getDataInvioRFx() + "\n";
            message += "OGGETTO:\t" + asset.getOggetto()  + "\n";
            message += "DESCRIZIONE:\t" + asset.getDescrizione()  + "\n";
            message += "IMPORTO RDA:\t" + asset.getImportoRDA() + "\n";
            message += "TERMINE PRESENTAZIONI:\t" + asset.getTerminePresentazioni()  + "\n";
            message += "TIPO AGGIUDICAZIONE:\t" + asset.getTipoAggiudicazione()  + "\n";
            message += "TIPO OFFERTA:\t" + asset.getTipoOfferta()  + "\n";
            message += "IVA RDA:\t" + asset.getIvaRDA()  + "\n";
            message += "NUMERO RILANCI:\t" + asset.getNumeroRilanci()  + "\n";
            message += "PUNTEGGIO TECNICO:\t" + asset.getPunteggioTecnico()  + "\n";
            message += "PUNTEGGIO ECONOMICO:\t" + asset.getPunteggioEconomico()  + "\n";
            message += "CALCOLO PUNTEGGIO ECONOMICO:\t" + asset.getCalcoloPunteggioEconomico()  + "\n";
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            System.out.println(errorMessage);
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        return message;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertOffer(final Context ctx, final String ID, int importo) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset asset = null;
        Asset newAsset = null;
        ClientIdentity ci = null;

        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata
        try {
            asset = Asset.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: un'offerta non può essere presentata se la gara è in stato di preparazione o di valutazione
        String assetState = asset.getStato();
        if ( !assetState.equals(states[1]) ) {
            String errorMessage = "";
            if (assetState.equals(states[0])) {
                errorMessage = String.format("Asset %s: inserimento offerta non consentito. Gara in fase di preparazione, non ancora pubblicata.", ID);
            } else if (assetState.equals(states[2])) {
                errorMessage = String.format("Asset %s: inserimento offerta non consentito. Gara in fase di valutazione.", ID);
            }
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
        Asset.Builder newAssetBuilder = Asset.newBuilder()
                .setID(asset.getID())
                .setStato(asset.getStato())
                .setOggetto(asset.getOggetto())
                .setDescrizione(asset.getDescrizione())
                .setImportoRDA(asset.getImportoRDA())
                .setTerminePresentazioni(asset.getTerminePresentazioni())
                .setTipoAggiudicazione(asset.getTipoAggiudicazione())
                .setTipoOfferta(asset.getTipoOfferta())
                .setIvaRDA(asset.getIvaRDA())
                .setNumeroRilanci(asset.getNumeroRilanci())
                .setPunteggioTecnico(asset.getPunteggioTecnico())
                .setPunteggioEconomico(asset.getPunteggioEconomico())
                .setCalcoloPunteggioEconomico(asset.getCalcoloPunteggioEconomico());

        List<Offer> offers = asset.getOffersList();
        newAssetBuilder.addAllOffers(offers);

        newAssetBuilder.addOffers(
                Offer.newBuilder()
                    .setCreatedBy(ci.getId())
                    .setImporto(importo)
                );

        newAsset = newAssetBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newAsset.toByteArray());
        
        //return this.ReadSubmittedOffer(ctx, ID);

    } 

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadSubmittedOffer(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset asset = null;
        String userID;

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

        List<Offer> offers = asset.getOffersList();
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
            return submittedOffer.toString();
        }        
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InsertMessage(final Context ctx, final String ID, String message) {

        ChaincodeStub stub = ctx.getStub();
        byte[] assetByte = stub.getState(ID);
        Asset asset = null;
        Asset newAsset = null;
        ClientIdentity ci = null;
        JSONObject info_gara = null;


        //controllo esistenza gara con ID trasmesso come parametro
        //eccezione in caso di non esistenza
        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        //parsing dell'array di byte in formato ProtocolBuffer nella classe associata e del JSON del messaggio
        try {
            info_gara = new JSONObject(message);
            asset = Asset.parseFrom(assetByte);
        } catch (Exception e) {
            String errorMessage = "Parsing error";
            e.printStackTrace();
            throw new ChaincodeException(errorMessage, AssetTransferErrors.EXCEPTION.toString());
        }

        //controllo sullo stato della gara: un'offerta non può essere presentata se la gara è in stato di preparazione o di valutazione
        String assetState = asset.getStato();
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
        Asset.Builder newAssetBuilder = Asset.newBuilder()
                .setID(asset.getID())
                .setStato(asset.getStato())
                .setOggetto(asset.getOggetto())
                .setDescrizione(asset.getDescrizione())
                .setImportoRDA(asset.getImportoRDA())
                .setTerminePresentazioni(asset.getTerminePresentazioni())
                .setTipoAggiudicazione(asset.getTipoAggiudicazione())
                .setTipoOfferta(asset.getTipoOfferta())
                .setIvaRDA(asset.getIvaRDA())
                .setNumeroRilanci(asset.getNumeroRilanci())
                .setPunteggioTecnico(asset.getPunteggioTecnico())
                .setPunteggioEconomico(asset.getPunteggioEconomico())
                .setCalcoloPunteggioEconomico(asset.getCalcoloPunteggioEconomico());

        List<Offer> offers = asset.getOffersList();
        newAssetBuilder.addAllOffers(offers);

        List<Message> messages = asset.getMessageList();
        newAssetBuilder.addAllMessage(messages);
        newAssetBuilder.addMessage(
                Message.newBuilder()
                        .setSender(info_gara.getString("mittente"))
                        .setReceiver(info_gara.getString("destinatario"))
                        .setObject(info_gara.getString("oggetto"))
                        .setText(info_gara.getString("testo"))
                        .setTimestamp(info_gara.getString("timestamp"))
        );

        newAsset = newAssetBuilder.build();

        //inserimento asset nel ledger
        stub.putState(ID, newAsset.toByteArray());

        //return this.ReadSubmittedOffer(ctx, ID);

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


    /**
     * Retrieves all assets from the ledger.
     *
     * @param ctx the transaction context
     * @return array of assets found on the ledger
     */
    /*
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllAssets(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<Asset> queryResults = new ArrayList<Asset>();

        // To retrieve all assets from the ledger use getStateByRange with empty startKey & endKey.
        // Giving empty startKey & endKey is interpreted as all the keys from beginning to end.
        // As another example, if you use startKey = 'asset0', endKey = 'asset9' ,
        // then getStateByRange will retrieve asset with keys between asset0 (inclusive) and asset9 (exclusive) in lexical order.
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result: results) {
            Asset asset = genson.deserialize(result.getStringValue(), Asset.class);
            System.out.println(asset);
            queryResults.add(asset);
        }

        final String response = genson.serialize(queryResults);

        return response;
    }
    */

    /**
     * Deletes asset on the ledger.
     *
     * @param ctx the transaction context
     * @param ID the ID of the asset being deleted
     */
    /*
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteAsset(final Context ctx, final String ID) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, ID)) {
            String errorMessage = String.format("Asset %s does not exist", ID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        stub.delState(ID);
    }
    */
}