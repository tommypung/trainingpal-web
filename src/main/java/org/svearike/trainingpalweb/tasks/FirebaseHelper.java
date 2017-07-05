package org.svearike.trainingpalweb.tasks;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper
{
	private static final Logger LOG = Logger.getLogger(FirebaseHelper.class.getName());
	private static final FirebaseHelper INSTANCE = new FirebaseHelper();
	private FirebaseHelper()
	{/*
		try {
			String authToken = "{\"web\":{\"client_id\":\"733491293394-bann7a0s4953oci6sobcaafnaccqrlom.apps.googleusercontent.com\",\"project_id\":\"trainingpal-web\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://accounts.google.com/o/oauth2/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"gpqirgYHjiZUl0ymdx1KHH5W\",\"redirect_uris\":[\"https://trainingpal-web.firebaseapp.com/__/auth/handler\"],\"javascript_origins\":[\"http://localhost\",\"http://localhost:5000\",\"https://trainingpal-web.firebaseapp.com\"]}}";
			FirebaseOptions options = new FirebaseOptions.Builder()
					.setCredential(FirebaseCredentials.applicationDefault())
//					.setCredential(FirebaseCredentials.fromRefreshToken(new ByteArrayInputStream(authToken.getBytes("UTF-8"))))
					.setDatabaseUrl("https://trainingpal-web.firebaseio.com/")
					.build();

			FirebaseApp.initializeApp(options);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Could not initialize firebase", e);
		}*/
	}

	public static FirebaseDatabase getDatabase()
	{
		return FirebaseDatabase.getInstance();
	}
}
