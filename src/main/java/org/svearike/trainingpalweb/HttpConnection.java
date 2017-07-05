package org.svearike.trainingpalweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.json.JSONException;

import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

/** Helper utility class for creating HttpConnections.
 *
 * @author Tommy Wallberg <tommy.wallberg@imodules.se>
 */
public abstract class HttpConnection
{
	/** Default Logger.
	 */
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(HttpConnection.class.getName());

	/** Wraps a Future<HTTPResponse> so we can retrieve a Future<String> instead.
	 */
	private static class FutureWrapper implements Future<String>
	{
		/** The original wrapped object.
		 */
		private final Future<HTTPResponse> w;

		/** Create a new wrapper for the provided Future.
		 *
		 * @param futureResponse - The object to wrap.
		 */
		FutureWrapper(Future<HTTPResponse> futureResponse) {
			this.w = futureResponse;
		}

		@Override public boolean cancel(boolean mayInterruptIfRunning) { return w.cancel(mayInterruptIfRunning); }
		@Override public boolean isCancelled()                         { return w.isCancelled(); }
		@Override public boolean isDone()                              { return w.isDone(); }
		@Override public String get() throws InterruptedException, ExecutionException
		{
			return parse(w.get());
		}

		@Override
		public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
		{
			return parse(w.get(timeout, unit));
		}

		/** Parse the HTTPResponse into a String.
		 *
		 * @param httpResponse - The response.
		 * @return A String or null if an error occurred.
		 */
		private String parse(HTTPResponse httpResponse)
		{
			try {
				return new String(httpResponse.getContent(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
	}

	/** Connects to a AppEngine backend asynchronously and retrieve the response as a String.
	 *
	 * @param url - The url to connect to.
	 * @param postData - The post data to send.
	 * @param headers - Any additional headers to send.
	 * @return A Future<String>
	 */
	public static Future<String> connectAndGetStringFromAppEngineAsync(URL url, String postData, Map<String, String> headers)
	{
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPRequest req = new HTTPRequest(url, HTTPMethod.POST, FetchOptions.Builder.doNotFollowRedirects());
		if (headers != null)
			for(Entry<String, String> e : headers.entrySet())
				req.addHeader(new HTTPHeader(e.getKey(), e.getValue()));

		try {
			req.setPayload(postData.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) { }
		Future<HTTPResponse> resp = service.fetchAsync(req);
		return new FutureWrapper(resp);
	}

	/** Generic function for fetching a String from an URL on AppEngine.
	 *
	 * This will parse the output into a String no matter if the status code is OK or ERROR.
	 *
	 * @param url - The URL to call.
	 * @param postData - The data to post.
	 * @param connectTimeout - Connect timeout given in milliseconds.
	 * @param readTimeout - Read timeout given in milliseconds.
	 * @return A String returned by the Server.
	 * @throws IOException - Self explanatory.
	 * @throws JSONException - Self explanatory.
	 */
	public static String connectAndGetStringFromAppEngine(URL url, String postData, int connectTimeout, int readTimeout) throws IOException, JSONException
	{
		byte[] postBytes = postData.getBytes("UTF-8");
		//		AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
		//		AppIdentityService.GetAccessTokenResult accessToken = appIdentity.getAccessToken(Arrays.asList("https://www.googleapis.com/auth/userinfo.email"));
		//		LOG.info("AccessToken being used: " + accessToken);
		//		LOG.info("Token: " + accessToken.getAccessToken());
		// The token asserts the identity reported by appIdentity.getServiceAccountName()
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Content-Type", "application/json");
		//		connection.addRequestProperty("Authorization", "OAuth " + accessToken.getAccessToken());
		connection.setRequestProperty("Content-Length", "" + Integer.toString(postBytes.length));
		connection.setRequestProperty("User-Agent", "Gumbler AppEngine / HttpURLConnection");
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		connection.setInstanceFollowRedirects(false);
		BufferedReader reader = null;
		StringBuilder results = new StringBuilder();
		try {
			OutputStream os = connection.getOutputStream();
			os.write(postBytes);
			os.flush();
			os.close();
			connection.getResponseCode();

			InputStream is = connection.getErrorStream();
			if (is == null)
				is = connection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

			String line;
			while ((line = reader.readLine()) != null) {
				results.append(line);
			}

			connection.disconnect();

			return results.toString();
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/** Generic function for fetching a String from an URL on AppEngine.
	 *
	 * This will parse the output into a String no matter if the status code is OK or ERROR.
	 *
	 * @param url - The URL to call.
	 * @param headers - Headers to add.
	 * @param postData - The data to post.
	 * @param connectTimeout - Connect timeout given in milliseconds.
	 * @param readTimeout - Read timeout given in milliseconds.
	 * @return A String returned by the Server.
	 * @throws IOException - Self explanatory.
	 * @throws JSONException - Self explanatory.
	 */
	public static String connectAndGetStringFromAppEngine(URL url, Map<String, String> headers, String postData, int connectTimeout, int readTimeout) throws IOException, JSONException
	{
		byte[] postBytes = postData.getBytes("UTF-8");
		//		AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
		//		AppIdentityService.GetAccessTokenResult accessToken = appIdentity.getAccessToken(Arrays.asList("https://www.googleapis.com/auth/userinfo.email"));
		//		LOG.info("AccessToken being used: " + accessToken);
		//		LOG.info("Token: " + accessToken.getAccessToken());
		// The token asserts the identity reported by appIdentity.getServiceAccountName()
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Content-Type", "application/json");
		//		connection.addRequestProperty("Authorization", "OAuth " + accessToken.getAccessToken());
		connection.setRequestProperty("Content-Length", "" + Integer.toString(postBytes.length));
		connection.setRequestProperty("User-Agent", "Gumbler AppEngine / HttpURLConnection");
		for(Entry<String, String> e : headers.entrySet())
			connection.setRequestProperty(e.getKey(), e.getValue());
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		connection.setInstanceFollowRedirects(false);
		BufferedReader reader = null;
		StringBuilder results = new StringBuilder();
		try {
			OutputStream os = connection.getOutputStream();
			os.write(postBytes);
			os.flush();
			os.close();
			connection.getResponseCode();

			InputStream is = connection.getErrorStream();
			if (is == null)
				is = connection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

			String line;
			while ((line = reader.readLine()) != null) {
				results.append(line);
			}

			connection.disconnect();

			return results.toString();
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/** Generic function for fetching a String from an URL.
	 *
	 * This will parse the output into a String no matter if the status code is OK or ERROR.
	 *
	 * @param url - The URL to call.
	 * @param headers - Optional headers to add - may be null.
	 * @param postData - The data to post.
	 * @param connectTimeout - Connect timeout given in milliseconds.
	 * @param readTimeout - Read timeout given in milliseconds.
	 * @return A String returned by the Server.
	 * @throws IOException - Self explanatory.
	 * @throws JSONException - Self explanatory.
	 */
	public static String connectAndGetString(URL url, Map<String, String> headers, String method, String postData, int connectTimeout, int readTimeout) throws IOException, JSONException
	{
		byte[] postBytes = null;
		if (postData != null)
			postBytes = postData.getBytes("UTF-8");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		if (postBytes == null)
			connection.setRequestMethod(method);
		else
		{
			connection.setRequestMethod(method);
			connection.setRequestProperty("Content-Type", "application/JSON");//; charset=UTF-8");
			connection.setRequestProperty("Content-Length", "" + Integer.toString(postBytes.length));
			connection.setRequestProperty("User-Agent", "Gumbler AppEngine / HttpURLConnection");
			connection.setDoOutput(true);
		}
		if (headers != null)
			for(Entry<String, String> e : headers.entrySet())
				connection.setRequestProperty(e.getKey(), e.getValue());
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);

		BufferedReader reader = null;
		StringBuilder results = new StringBuilder();
		try {
			if (postBytes != null)
			{
				OutputStream os = connection.getOutputStream();
				os.write(postBytes);
				os.flush();
				os.close();
			}
			connection.getResponseCode();

			InputStream is = connection.getErrorStream();
			if (is == null)
				is = connection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is));

			String line;
			while ((line = reader.readLine()) != null) {
				results.append(line);
			}

			connection.disconnect();

			return results.toString();
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/** Generic function for fetching a String from an URL.
	 *
	 * This will parse the output into a String no matter if the status code is OK or ERROR.
	 *
	 * @param url - The URL to call.
	 * @param postData - The data to post.
	 * @param connectTimeout - Connect timeout given in milliseconds.
	 * @param readTimeout - Read timeout given in milliseconds.
	 * @return A String returned by the Server.
	 * @throws IOException - Self explanatory.
	 * @throws JSONException - Self explanatory.
	 */
	public static String connectAndGetString(URL url, String method, String postData, int connectTimeout, int readTimeout) throws IOException, JSONException
	{
		return connectAndGetString(url, null, method, postData, connectTimeout, readTimeout);
	}

	/** Generic function for fetching a String from an URL.
	 *
	 * This will parse the output into a String no matter if the status code is OK or ERROR.
	 *
	 * @param url - The URL to call.
	 * @param connectTimeout - Connect timeout given in milliseconds.
	 * @param readTimeout - Read timeout given in milliseconds.
	 * @return A String returned by the Server.
	 * @throws IOException - Self explanatory.
	 * @throws JSONException - Self explanatory.
	 */
	public static String connectAndGetString(URL url, int connectTimeout, int readTimeout) throws IOException, JSONException
	{
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", "Gumbler AppEngine / HttpURLConnection");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);

		BufferedReader reader = null;
		StringBuilder results = new StringBuilder();
		try {
			connection.getResponseCode();

			InputStream is = connection.getErrorStream();
			if (is == null)
				is = connection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is));

			String line;
			while ((line = reader.readLine()) != null) {
				results.append(line);
			}

			connection.disconnect();

			return results.toString();
		} finally {
			if (reader != null)
				reader.close();
		}
	}

}
