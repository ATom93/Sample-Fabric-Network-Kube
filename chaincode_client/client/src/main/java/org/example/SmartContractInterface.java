package org.example;

public interface SmartContractInterface {

    void CreateAsset(String ID, String type, String eventID, String eventType, String eventDate);

    void AddAssetEvent(String ID, String eventID, String eventType, String eventDate);

    String ReadAsset(String ID);

}
