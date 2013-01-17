package au.com.infiniterecursion.vidiom.activity;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import twitter4j.http.AccessToken;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import au.com.infiniterecursion.vidiompro.R;

/**
 * Inspiration -
 * http://androidforums.com/introductions/218621-twitter4j-oauth-android
 * -simple.html
 * 
 * @author andycat
 * 
 */

public class TwitterOAuthActivity extends Activity {

	private String TAG = "RoboticEye-TwitterOAuthActivity";
	private CommonsHttpOAuthConsumer httpOauthConsumer;
	private OAuthProvider httpOauthprovider;
	public final static String consumerKey = "AXKcekv78ff73UohyRd9ng";
	public final static String consumerSecret = "cUYWl7GLlNydB6YPKPr9zrGEfGpjKuU4L2KaR6JyU";
	private final String CALLBACKURL = "vidiom://TwitterOAuthActivityCallback";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.twitter_oauth_browser);

		new TwitterOauthAsyncTask().execute();
	}

	/**
	 * Opens the browser using signpost jar with application specific
	 * consumerkey and consumerSecret.
	 */

	private class TwitterOauthAsyncTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			String authUrl = null;
			Log.d(TAG, " TwitterOauthAsyncTask starting ");

			try {
				httpOauthConsumer = new CommonsHttpOAuthConsumer(consumerKey,
						consumerSecret);
				httpOauthprovider = new CommonsHttpOAuthProvider(
						"http://twitter.com/oauth/request_token",
						"http://twitter.com/oauth/access_token",
						"http://twitter.com/oauth/authorize");
				authUrl = httpOauthprovider.retrieveRequestToken(
						httpOauthConsumer, CALLBACKURL);

			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG, " TwitterOauthAsyncTask errored " + e.getMessage());
				authUrl = null;
				// Toast.makeText(this, e.getMessage(),
				// Toast.LENGTH_LONG).show();
				// finish();
			}

			return authUrl;

		}

		protected void onPostExecute(String authUrl) {
			WebView webv = (WebView) findViewById(R.id.webkitWebView1);
			webv.setWebViewClient(new TwitterOAuthWebViewClient());

			webv.loadUrl(authUrl);
		}

	}

	private class TwitterOAuthWebViewClient extends WebViewClient {
		public boolean shouldOverrideUrlLoading(WebView v, String url) {
			Log.d(TAG, "TwitterOAuthWebViewClient Url is " + url);

			if (url != null && url.startsWith(CALLBACKURL)) {

				new TwitterParseUriAsyncTask().execute(url);

				finish();

				return true;
			}

			return false;
		}

	}

	/**
	 * After user authorizes this is the function where we get called back, with
	 * user specific token and secret token. We store this token for future use.
	 */

	private class TwitterParseUriAsyncTask extends
			AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {

			Log.d(TAG, "parseUri: Url is " + params[0]);
			Uri uri = Uri.parse(params[0]);

			String verifier = uri
					.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

			try {
				// this will populate token and token_secret in consumer

				httpOauthprovider.retrieveAccessToken(httpOauthConsumer,
						verifier);

				AccessToken a = new AccessToken(httpOauthConsumer.getToken(),
						httpOauthConsumer.getTokenSecret());

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext());
				Editor editor = prefs.edit();
				editor.putString("twitterToken", a.getToken());
				editor.putString("twitterTokenSecret", a.getTokenSecret());
				editor.commit();

				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						Toast.makeText(TwitterOAuthActivity.this,
								R.string.twitter_authorisation_successfully,
								Toast.LENGTH_LONG).show();

					}
				});

			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG, "parseUri: Exception is " + e.getMessage());

			}
			return null;
		}

	}

}