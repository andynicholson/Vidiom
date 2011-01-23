package au.com.infiniterecursion.vidiom.activity;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import twitter4j.http.AccessToken;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Inspiration - http://androidforums.com/introductions/218621-twitter4j-oauth-android-simple.html
 * 
 * @author andycat
 *
 */

public class TwitterOAuthActivity extends Activity  {
	
	private String TAG = "RoboticEye-TwitterOAuthActivity";
	private CommonsHttpOAuthConsumer httpOauthConsumer;
	private OAuthProvider httpOauthprovider;
	public final static String consumerKey = "AXKcekv78ff73UohyRd9ng";
	public final static String consumerSecret = "cUYWl7GLlNydB6YPKPr9zrGEfGpjKuU4L2KaR6JyU";
	private final String CALLBACKURL = "vidiom://TwitterOAuthActivityCallback";
	//private Twitter twitter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		doOauth();
	}
	
	/**
	 * Opens the browser using signpost jar with application specific
	 * consumerkey and consumerSecret.
	 */

	private void doOauth() {
		try {
			httpOauthConsumer = new CommonsHttpOAuthConsumer(consumerKey,
					consumerSecret);
			httpOauthprovider = new CommonsHttpOAuthProvider(
					"http://twitter.com/oauth/request_token",
					"http://twitter.com/oauth/access_token",
					"http://twitter.com/oauth/authorize");
			String authUrl = httpOauthprovider.retrieveRequestToken(
					httpOauthConsumer, CALLBACKURL);

			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse(authUrl)));
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, " doOauth errored " + e.getMessage());
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	/**
	 * After use authorizes this is the function where we get back callbac with
	 * user specific token and secret token. You might want to store this token
	 * for future use.
	 */

	@Override
	protected void onNewIntent(Intent intent) {

		super.onNewIntent(intent);
		Uri uri = intent.getData();
		
		if (uri != null) {
			Log.d(TAG, " onNewIntent got " + uri.toString());
		}
		
		if (uri != null && uri.toString().startsWith(CALLBACKURL)) {

			String verifier = uri
					.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

			try {
				// this will populate token and token_secret in consumer

				httpOauthprovider.retrieveAccessToken(httpOauthConsumer,
						verifier);
				
				AccessToken a = new AccessToken(httpOauthConsumer.getToken(),
						httpOauthConsumer.getTokenSecret());

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				Editor editor = prefs.edit();
				editor.putString("twitterToken", a.getToken());
				editor.putString("twitterTokenSecret", a.getTokenSecret());
				editor.commit();
				
				// initialize Twitter4J
				/*
				twitter = new TwitterFactory().getInstance();
				twitter.setOAuthConsumer(consumerKey, consumerSecret);
				twitter.setOAuthAccessToken(a);
				*/
				//we have finished here.
				finish();
				
			} catch (Exception e) {

				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}

		}
	}

	

}