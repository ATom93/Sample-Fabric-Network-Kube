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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class IdentityManager {

    private Reader getReader(Path path) throws IOException {
        Reader reader = Files.newBufferedReader(path);
        return reader;
    }

    /**
     * Get a X509Certificate object from a certificate on file system
     *
     * @param certificatePath Path of the certificate on file system
     * @return X509Certificate object from the given certificate
     * @throws IOException
     * @throws CertificateException
     */
    public X509Certificate getCertificate(Path certificatePath) throws IOException, CertificateException {
        X509Certificate certificate = Identities.readX509Certificate(
                getReader(certificatePath)
        );
        return certificate;
    }

    /**
     * Get a X509Certificate object from a certificate on a database
     *
     * @param dbAddress Address of the DBMS with a database where a peer node certificate is stored
     * @return X509Certificate object from the given certificate
     * @throws IOException
     * @throws CertificateException
     */
    public X509Certificate getCertificateFromDatabase(String dbAddress) throws IOException, CertificateException, ParseException {
        String cert = getPeerCertificateFromDatabase(dbAddress);
        X509Certificate X509Ccertificate = Identities.readX509Certificate(cert);
        return X509Ccertificate;
    }

    /**
     * Retrieve the certificate of the peer node from
     * the document with id "peerCert" in the CouchDB database with name "certificates"
     */
    private String getPeerCertificateFromDatabase(String dbAddress) throws IOException, ParseException {
        String databaseName = "certificates";
        String id = "peerCert";

        HttpClient httpClient = new StdHttpClient
                .Builder()
                .url(dbAddress)
                .build();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        CouchDbConnector db = new StdCouchDbConnector(databaseName, dbInstance);
        InputStream doc = db.getAsStream(id);
        String output = IOUtils.toString(doc, StandardCharsets.UTF_8);
        doc.close();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(output);
        String certificate = (String) json.get("data");
        return certificate;
    }

}
