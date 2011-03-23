package au.com.infiniterecursion.vidiom.facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.util.Log;

import com.facebook.android.FacebookError;
import com.facebook.android.AsyncFacebookRunner.RequestListener;

/**
 * Skeleton base class for RequestListeners, providing default error handling.
 * Applications should handle these error conditions.
 * 
 */
public abstract class BaseRequestListener implements RequestListener {

	private static String TAG = "RoboticEye-Facebook";

	public void onFacebookError(FacebookError e) {
		Log.e(TAG, e.getMessage());
		e.printStackTrace();
	}

	public void onFileNotFoundException(FileNotFoundException e) {
		Log.e(TAG, e.getMessage());
		e.printStackTrace();
	}

	public void onIOException(IOException e) {
		Log.e(TAG, e.getMessage());
		e.printStackTrace();
	}

	public void onMalformedURLException(MalformedURLException e) {
		Log.e(TAG, e.getMessage());
		e.printStackTrace();
	}

}
