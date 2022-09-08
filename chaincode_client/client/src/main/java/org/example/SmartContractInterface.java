package org.example;

public interface SmartContractInterface {

    String CreateAsset(String ID, String type, String eventID);

    String AddAssetEvent(String ID, String eventID, String eventType);



    void CreateGara(String eventID, String idGara);

    void UpdateField(String eventID, String idGara, String campo, String newValue);


    void StateChange_InCorso(String eventID, String idGara);

    void InsertOffer(String eventID, String idGara, String importo);

    void InsertMessage(String eventId, String idGara, String messaggio);


    void StateChange_InValutazione(String eventID, String idGara);

    void InsertAdministrativeEvaluation(
            String eventId, String ID, String IDSupplier, String approval, String motivazione);

    //void InsertTechnicalEvaluation(String ID, String IDSupplier, String punteggio);

    //void InsertEconomicalEvaluation(String ID, String IDSupplier, String punteggio);


    void StateChange_Chiusura(String eventID, String idGara);

    void ReadGara(String idGara);

    void ReadInfoGara(String idGara);

    void ReadSubmittedOffer(String idGara);

    void AddGaraEvent(String idGara, String eventID, String eventType);



    void QueryTransaction(String txID);

}
