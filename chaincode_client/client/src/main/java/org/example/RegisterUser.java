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

	/**
	 * Registration of a new user.
	 *
	 * @param caAddress Address of the Certification Authority (default port 30754, otherwise it must be included in the address)
	 * @param dbAddress Address (with port) of the DBMS (CouchDB) where to store user certificates
	 * @param walletName Name of the database on CouchDB where to store user certificates
	 * @param password Password for user private key encryption
	 * @param enrollmentID Identifier for the new user
	 * @param role Role of the new user
	 * @throws Exception
	 */
	public static void registerUser(String caAddress, String dbAddress, String walletName, String password,
									String enrollmentID/*, String userOrg*/, String role) throws Exception {
		String userOrg = "org1";

		String adminName = "admin-"+userOrg;
		String adminSecret = "adminpwd";

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

		// Create a wallet for managing identities
		Wallet wallet = Wallets.newCouchDBWallet(new URL(
				dbAddress
		), walletName);

		// Check to see if we've already enrolled the user.
		if (wallet.get(enrollmentID, password) != null) {
			System.out.println("An identity for the user \""+ enrollmentID + "\" already exists in the wallet");
			return;
		}

		X509Identity adminIdentity = (X509Identity)wallet.get(adminName, adminSecret);
		if (adminIdentity == null) {
			System.out.println("\"" + adminName + "\" needs to be enrolled and added to the wallet first");
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
				return userOrg;
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
				return userOrg;
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

		Identity user = Identities.newX509Identity(userOrg, enrollment);
		wallet.put(enrollmentID, user, password);
		System.out.println("Successfully enrolled user \"" + enrollmentID + "\" and imported it into the wallet");
	}

}
