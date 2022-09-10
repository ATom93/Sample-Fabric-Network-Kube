package org.example;

import org.apache.commons.io.IOUtils;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.hyperledger.fabric.client.identity.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class IdentityManager {

    private Reader getReader(Path path) throws IOException {
        Reader reader = Files.newBufferedReader(path);
        return reader;
    }

    public X509Certificate getCertificate(Path certificatePath) throws IOException, CertificateException {
        X509Certificate certificate = Identities.readX509Certificate(
                getReader(certificatePath)
        );
        return certificate;
    }

    public X509Certificate getCertificateFromDatabase() throws IOException, CertificateException, ParseException {
        String cert = getPeerCertificateFromDatabase();
        X509Certificate X509Ccertificate = Identities.readX509Certificate(cert);
        return X509Ccertificate;
    }

    private String getPeerCertificateFromDatabase() throws IOException, ParseException {
        HttpClient httpClient = new StdHttpClient
                .Builder()
                .url("http://admin:password@127.0.0.1:5984")
                .build();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        CouchDbConnector db = new StdCouchDbConnector("wallet", dbInstance);
        String id = "peerCert";
        InputStream doc = db.getAsStream(id);
        String output = IOUtils.toString(doc, StandardCharsets.UTF_8);
        doc.close();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(output);
        String certificate = (String) json.get("data");
        //certificate = certificate.substring(27,certificate.length()-25);
        System.out.println("\nCertificate:\n"+certificate+"\n");
        return certificate;
    }

}
