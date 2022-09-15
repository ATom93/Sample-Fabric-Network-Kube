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
		//System.out.println("CA ADDRESS:\t"+caAddress);
		String dbAddress = args[1];
		//System.out.println("DB ADDRESS:\t"+dbAddress);
		String walletName = args[2];
		//System.out.println("WALLET:\t"+walletName);
		enrollAdmin(caAddress, dbAddress, walletName);
	}

	/**
	 * Enrollment of administrator identity for user registration.
	 *
	 * @param caAddress Address of the Certification Authority (default port 30754, otherwise it must be included in the address)
	 * @param dbAddress Address (with port) of the DBMS (CouchDB) where to store the administrator certificate
	 * @param walletName Name of the database on CouchDB where to store certificates
	 * @throws Exception
	 */
	public static void enrollAdmin(String caAddress, String dbAddress, String walletName/*, String adminOrg*/) throws Exception {
		String adminOrg = "org1";

		String password = "adminpwd";
		String adminName = "admin";
		String adminSecret = "adminpw";

		Properties props = new Properties();
		props.put("allowAllHostNames", "true");

		HFCAClient caClient = null;
		if (caAddress.contains(":")) {
			caClient = HFCAClient.createNewInstance("http://" + caAddress, props);
		} else {
			caClient = HFCAClient.createNewInstance("http://" + caAddress + ":30754", props);
		}
		CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
		caClient.setCryptoSuite(cryptoSuite);

		//Create a wallet for managing identities
		Wallet wallet = Wallets.newCouchDBWallet(new URL(
				dbAddress
		), walletName);

		// Check to see if the admin user is already enrolled
		if (wallet.get(adminName, password) != null) {
			System.out.println("An identity for the admin user \"" + adminName + "\" already exists in the wallet");
			return;
		}

		// Enroll the admin user, and import the new identity into the wallet.
		final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
		enrollmentRequestTLS.addHost(caAddress);
		enrollmentRequestTLS.setProfile("tls");
		Enrollment enrollment = caClient.enroll(adminName, adminSecret, enrollmentRequestTLS);
		Identity user = Identities.newX509Identity(adminOrg, enrollment);
		wallet.put(adminName + "-" + adminOrg, user, password);
		System.out.println("Successfully enrolled user \"" + adminName + "\" and imported it into the wallet");
	}
}
