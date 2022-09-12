/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.net.URL;
import java.util.Properties;

public class EnrollAdmin {

	public static void main(String[] args) throws Exception {
		String caAddress = args[0];
		System.out.println("CA ADDRESS:\t"+caAddress);
		String dbAddress = args[1];
		System.out.println("DB ADDRESS:\t"+dbAddress);
		String walletName = args[2];
		System.out.println("WALLET:\t"+walletName);
		enrollAdmin(caAddress, dbAddress, walletName);
	}

	public static void enrollAdmin(String caAddress, String dbAddress, String walletName) throws Exception {
		String password = "adminpwd";

		String adminOrg = "org1";
		String adminName = "admin";
		String adminSecret = "adminpw";

		Properties props = new Properties();
		props.put("allowAllHostNames", "true");

		HFCAClient caClient = HFCAClient.createNewInstance("http://" + caAddress + ":30754", props);
		CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
		caClient.setCryptoSuite(cryptoSuite);

		// Create a wallet for managing identities
		//Wallet wallet = Wallets.newFileSystemWallet(Paths.get("wallet"));
		Wallet wallet = Wallets.newCouchDBWallet(new URL(
				dbAddress
		), walletName);

		// Check to see if we've already enrolled the admin user.
		if (wallet.get("admin", password) != null) {
			System.out.println("An identity for the admin user \"admin\" already exists in the wallet");
			return;
		}

		// Enroll the admin user, and import the new identity into the wallet.
		final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
		enrollmentRequestTLS.addHost(caAddress);
		enrollmentRequestTLS.setProfile("tls");
		Enrollment enrollment = caClient.enroll(adminName, adminSecret, enrollmentRequestTLS);
		Identity user = Identities.newX509Identity(adminOrg, enrollment);
		wallet.put(adminName, user, password);
		System.out.println("Successfully enrolled user \"admin\" and imported it into the wallet");
	}
}
