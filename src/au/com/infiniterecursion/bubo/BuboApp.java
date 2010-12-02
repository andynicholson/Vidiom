package au.com.infiniterecursion.bubo;

/*
 * Main Bubo Application 
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
import au.com.infiniterecursion.bubo.facebook.SessionStore;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

public class BuboApp extends Application {
	
	private static final String FACEBOOK_APP_ID = "175287182490445";
	private static final String FACEBOOK_APP_KEY = "2841286527d46765c6823bc9b08fdad9";
	private static final String FACEBOOK_APP_SECRET = "ed97e81ca8b2327aaaa9291979f0e8cb";
	
	private static String TAG ="RoboticEye-MainApp";
		
	//Facebook associated objects
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;


	/*
	 * On application startup, get the home position from the preferences.
	 * 
	 * (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	public void onCreate() {
    	super.onCreate();
    	Log.i(TAG, "*** onCreate called ***");
		
		
		 //Facebook init
        mFacebook = new Facebook(FACEBOOK_APP_ID);
        mAsyncRunner = new AsyncFacebookRunner(mFacebook);
		
		SessionStore.restore(mFacebook, this);
		
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
    
   public static String getFacebookAPIkey () {
	   return FACEBOOK_APP_KEY;
   }

   public static String getFacebookAPIsecret() {
	// 
	return FACEBOOK_APP_SECRET;
   }


}
