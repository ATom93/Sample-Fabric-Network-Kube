package org.example;

import org.hyperledger.fabric.client.identity.*;

import java.io.IOException;
import java.io.Reader;
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

    /*
    private PrivateKey getPrivateKey(Path certificatePath) throws IOException, CertificateException, InvalidKeyException {
        PrivateKey privateKey = Identities.readPrivateKey(
                getReader(certificatePath)
        );
        return privateKey;
    }
    */

    /*
    public Identity getIdentity(Path certificatePath, String mspId) {
        X509Certificate certificate = null;
        Identity identity = null;
        try {
            certificate = getCertificate(certificatePath);
            identity = new X509Identity(mspId, certificate);
            return identity;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public Signer getSigner(Path privateKeyPath) {
        PrivateKey privateKey = null;
        Signer signer = null;
        try {
            privateKey = getPrivateKey(privateKeyPath);
            signer = Signers.newPrivateKeySigner(privateKey);
            return signer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
    */
}
