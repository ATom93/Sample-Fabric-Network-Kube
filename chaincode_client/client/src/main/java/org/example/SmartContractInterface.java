package org.example;

public interface SmartContractInterface {

    String CreateAsset(String ID, String eventID, String eventType, String eventDate, String payload);

    String AddAssetEvent(String ID, String eventID, String eventType, String eventDate, String eventPayload);

    String ReadAsset(String ID);

}
