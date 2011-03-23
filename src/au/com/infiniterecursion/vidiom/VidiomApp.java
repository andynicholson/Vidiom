package au.com.infiniterecursion.vidiom;

/*
 * Main Vidiom Application 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 */

import android.app.Application;
import android.util.Log;
import au.com.infiniterecursion.vidiom.facebook.SessionStore;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

public class VidiomApp extends Application {

	private static final String FACEBOOK_APP_ID = "175287182490445";
	public static final String[] FB_LOGIN_PERMISSIONS = new String[] {
			"publish_stream", "read_stream", "video_upload" };
	private static String TAG = "RoboticEye-MainApp";

	// Facebook associated objects
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;

	private boolean isUploading;

	/*
	 * On application startup, get the home position from the preferences.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onCreate()
	 */
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "*** onCreate called ***");

		// Facebook init
		mFacebook = new Facebook(FACEBOOK_APP_ID);
		mAsyncRunner = new AsyncFacebookRunner(mFacebook);

		boolean session_valid = SessionStore.restore(mFacebook, this);
		Log
				.i(TAG, "Session for facebook restored . Is valid ?"
						+ session_valid);

		isUploading = false;
	}

	public void onTerminate() {
		Log.i(TAG, "*** OnTerminate called ***");
		super.onTerminate();
	}

	public Facebook getFacebook() {
		return mFacebook;
	}

	public AsyncFacebookRunner getAsyncFacebookRunner() {
		return mAsyncRunner;

	}

	public boolean isUploading() {
		return isUploading;
	}

	public void setUploading() {
		isUploading = true;
	}

	public void setNotUploading() {
		isUploading = false;
	}
}
