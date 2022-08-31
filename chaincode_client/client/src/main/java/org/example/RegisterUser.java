/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.net.URL;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Set;

public class RegisterUser {

	public static void main(String[] args) throws Exception {
		// Create a CA client for interacting with the CA.
		String caAddress = args[0];
		String dbAddress = args[1];
		String walletName = args[2];
		String password = args[3];
		String enrollmentID = args[4];
		String role = args[5];
		registerUser(caAddress, dbAddress, walletName, password, enrollmentID, role);
	}

	public static void registerUser(String caAddress, String dbAddress, String walletName, String password, String enrollmentID, String role) throws Exception {
		String adminPassword = "adminpwd";

		Properties props = new Properties();

		props.put("pemFile",
				"../../crypto_material/peerOrgs/org1/msp/cacerts/server.crt");

		props.put("allowAllHostNames", "true");
		HFCAClient caClient = HFCAClient.createNewInstance("http://"+ caAddress + ":30754", props);
		CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
		caClient.setCryptoSuite(cryptoSuite);

		// Create a wallet for managing identities
		//Wallet wallet = Wallets.newFileSystemWallet(Paths.get("wallet"));
		Wallet wallet = Wallets.newCouchDBWallet(new URL(
				dbAddress/*"http://admin:password@127.0.0.1:5984"*/
		), walletName);

		// Check to see if we've already enrolled the user.
		if (wallet.get("appUser", adminPassword) != null) {
			System.out.println("An identity for the user \"appUser\" already exists in the wallet");
			return;
		}

		X509Identity adminIdentity = (X509Identity)wallet.get("admin", adminPassword);
		if (adminIdentity == null) {
			System.out.println("\"admin\" needs to be enrolled and added to the wallet first");
			return;
		}
		User admin = new User() {

			@Override
			public String getName() {
				return "admin";
			}

			@Override
			public Set<String> getRoles() {
				return null;
			}

			@Override
			public String getAccount() {
				return null;
			}

			@Override
			public String getAffiliation() {
				return "org1";
			}

			@Override
			public Enrollment getEnrollment() {
				return new Enrollment() {

					@Override
					public PrivateKey getKey() {
						return adminIdentity.getPrivateKey();
					}

					@Override
					public String getCert() {
						return Identities.toPemString(adminIdentity.getCertificate());
					}
				};
			}

			@Override
			public String getMspId() {
				return "org1";
			}

		};

		//Register the user, enroll the user, and import the new identity into the wallet.
		RegistrationRequest registrationRequest = new RegistrationRequest("appUser");
		registrationRequest.setEnrollmentID(enrollmentID);				//--id.name
		registrationRequest.setType("client");							//--id.type
		Attribute attribute = new Attribute("role", role);
		registrationRequest.addAttribute(attribute);					//--id.attrs 'role=<ruolo>'
		System.out.println("User Registration");
		String enrollmentSecret = caClient.register(registrationRequest, admin);
		System.out.println("User Enrollment");

		EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
		enrollmentRequest.addHost(enrollmentID);							//--csr.hosts
		enrollmentRequest.addAttrReq("role");							//--enrollment.attrs

		Enrollment enrollment = caClient.enroll(enrollmentID, enrollmentSecret, enrollmentRequest);

		Identity user = Identities.newX509Identity("org1", enrollment);
		wallet.put(enrollmentID, user, password);
		System.out.println("Successfully enrolled user \"appUser\" and imported it into the wallet");
	}

}
