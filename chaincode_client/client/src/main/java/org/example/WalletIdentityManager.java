package org.example;

import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.client.identity.X509Identity;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class WalletIdentityManager {

    String mspId;
    String dbAddress;
    String walletName;
    org.hyperledger.fabric.gateway.X509Identity oldIdentity;

    public WalletIdentityManager(String walletIdentityLabel, String dbAddress, String walletName, String mspId, String password) throws Exception {
        this.mspId = mspId;
        this.dbAddress = dbAddress;
        this.walletName = walletName;
        this.oldIdentity = getWalletIdentity(walletIdentityLabel, password);
    }

    private org.hyperledger.fabric.gateway.X509Identity getWalletIdentity(String label, String encryptionPassword) throws Exception {

        Wallet wallet = Wallets.newCouchDBWallet(new URL(
                dbAddress
        ), walletName);
        org.hyperledger.fabric.gateway.X509Identity identity = (org.hyperledger.fabric.gateway.X509Identity) wallet.get(label, encryptionPassword);

        if (identity == null) {
            throw new Exception("\nAn identity for the user \""+label+"\" doesn't exist in the wallet. \nIdentity enrollment and registration required.\n");
        } else {
            return identity;
        }
    }

    public X509Identity getIdentity() throws IOException {
        return new X509Identity(mspId, oldIdentity.getCertificate());
    }

    public Signer getIdentitySigner() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return Signers.newPrivateKeySigner(
                oldIdentity.getPrivateKey()
        );
    }

}
