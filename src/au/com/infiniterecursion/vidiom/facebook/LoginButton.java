
/*
 *  Vidiom Facebook Login button 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 */

package au.com.infiniterecursion.vidiom.facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import au.com.infiniterecursion.vidiom.R;
import au.com.infiniterecursion.vidiom.activity.LibraryActivity;
import au.com.infiniterecursion.vidiom.facebook.SessionEvents.AuthListener;
import au.com.infiniterecursion.vidiom.facebook.SessionEvents.LogoutListener;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class LoginButton extends ImageButton {
    
    private Facebook mFb;
    private Handler mHandler;
    private SessionListener mSessionListener = new SessionListener();
    private String[] mPermissions;
    private static String TAG ="RoboticEye-Facebook";
    private LibraryActivity mAuth_activity;
    
    
    public LoginButton(Context context) {
        super(context);
        
        
    }
    
    public LoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public LoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void init(final Facebook fb, final String[] permissions, final Activity auth_activity) {
        mFb = fb;
        mPermissions = permissions;
        mHandler = new Handler();
        mAuth_activity = (LibraryActivity) auth_activity;
        
        setBackgroundColor(Color.TRANSPARENT);
        setAdjustViewBounds(true);
        setImageResource(fb.isSessionValid() ?
                         R.drawable.logout_button : 
                         R.drawable.login_button);
        drawableStateChanged();
        
     
      //initialise Session Events listeners
		SessionEvents.resetListeners();
		
        SessionEvents.addAuthListener(mSessionListener);
        SessionEvents.addLogoutListener(mSessionListener);
        
        setOnClickListener(new ButtonOnClickListener());
    }
    
    public void  logout() {
    	Log.d(TAG, "Logging out of Facebook!");
    	SessionEvents.onLogoutBegin();
        AsyncFacebookRunner asyncRunner = new AsyncFacebookRunner(mFb);
        asyncRunner.logout(getContext(), new LogoutRequestListener());
    }
    
    
    private final class ButtonOnClickListener implements OnClickListener {
        
        public void onClick(View arg0) {
            if (mFb.isSessionValid()) {
               logout();
            } else {
                mFb.authorize(mAuth_activity, mPermissions,
                        new LoginDialogListener());
            }
        }
    }

    private final class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
        	Log.d(TAG, "onComplete!");
            SessionEvents.onLoginSuccess();
        }

        public void onFacebookError(FacebookError error) {
        	Log.d(TAG, "onFacebookError!");
            SessionEvents.onLoginError(error.getMessage());
        }
        
        public void onError(DialogError error) {
        	Log.d(TAG, "onError!");
            SessionEvents.onLoginError(error.getMessage());
        }

        public void onCancel() {
        	Log.d(TAG, "onCancel!");
            SessionEvents.onLoginError("Action Canceled");
        }
    }
    
    private class LogoutRequestListener extends BaseRequestListener {
        public void onComplete(String response) {
            // callback should be run in the original thread, 
            // not the background thread
            mHandler.post(new Runnable() {
                public void run() {
                    SessionEvents.onLogoutFinish();
                    Log.d(TAG, "Logged out of Facebook!");
                }
            });
        }

		public void onComplete(String response, Object state) {
			// TODO Auto-generated method stub
			onComplete(response);
		}	

		public void onFacebookError(FacebookError e, Object state) {
			// TODO Auto-generated method stub
			e.printStackTrace();
		}

		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
			// TODO Auto-generated method stub
			e.printStackTrace();
		}

		public void onIOException(IOException e, Object state) {
			// TODO Auto-generated method stub
			e.printStackTrace();
		}

		public void onMalformedURLException(MalformedURLException e,
				Object state) {
			// TODO Auto-generated method stub
			e.printStackTrace();
		}
    }
    
    private class SessionListener implements AuthListener, LogoutListener {
        
        public void onAuthSucceed() {
            setImageResource(R.drawable.logout_button);
            SessionStore.save(mFb, getContext());
            Log.d(TAG, "Logged into FB successfully, saving session.");
            
            mAuth_activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(mAuth_activity, "Logged into Facebook successfully", Toast.LENGTH_LONG).show();
				}
				
			});
        	mAuth_activity.resetFacebookMenuButtons();
        }

        public void onAuthFail(String error) {
        	 Log.d(TAG, "onAuthFail : " + error);
        }
        
        public void onLogoutBegin() {           
        }
        
        public void onLogoutFinish() {
            SessionStore.clear(getContext());
            setImageResource(R.drawable.login_button);
            Log.d(TAG, "Logged out of FB successfully, clearing session.");
            mAuth_activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(mAuth_activity, "Logged out of Facebook successfully", Toast.LENGTH_LONG).show();
				}
				
			});
            mAuth_activity.resetFacebookMenuButtons();
        }
    }
    
}
