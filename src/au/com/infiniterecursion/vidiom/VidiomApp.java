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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import android.app.Application;
import android.util.Log;
import au.com.infiniterecursion.vidiom.facebook.SessionStore;
import au.com.infiniterecursion.vidiom.utils.DBUtils;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

public class VidiomApp extends Application {

	private static final String FACEBOOK_APP_ID = "175287182490445";
	public static final String[] FB_LOGIN_PERMISSIONS = new String[] {
			"publish_stream", "read_stream", "video_upload" };
	private static String TAG = "VidiomTag-MainApp";

	// Facebook associated objects
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;

	private boolean isUploading;

	private String current_path;

	public boolean support_v9 = false;
	public boolean support_v10 = false;
	public boolean support_v11 = false;

	// Static variables

	// A Map of SDRecord id and to a set of flags indicating this record is
	// being uploaded by a thread (particular service id).
	private static Map<Long, HashSet<Integer>> in_progress_uploads;

	// Database
	private static DBUtils db_utils;

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
		Log.i(TAG, "Session for facebook restored . Is valid ?" + session_valid);

		isUploading = false;

		// API level checking.

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		// v9 support
		if (currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			support_v9 = true;
			Log.d(TAG, "Phone supports v9 API or greater");
		}
		// v10 and above.
		if (currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			// API 10 or above yes
			support_v10 = true;
			Log.d(TAG, "Phone supports v10 API or greater");

		}
		// API 11 or above
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			support_v11 = true;
			Log.d(TAG, "Phone supports v11 API or greater");
		}

		// Uploading tracking
		in_progress_uploads = new HashMap<Long, HashSet<Integer>>();

		db_utils = new DBUtils(getBaseContext());
	}

	public void onTerminate() {
		Log.i(TAG, "*** OnTerminate called ***");
		super.onTerminate();
	}

	public Facebook getFacebook() {
		return mFacebook;
	}

	public DBUtils getDBUtils() {
		return db_utils;
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

	public String getCurrentPath() {
		return current_path;
	}

	public void setCurrentPath(String path) {
		this.current_path = path;
	}

	// Methods to track uploading per ID , per video hosting service
	public void addSDFileRecordIDtoUploadingTrack(long sdfilerecord_id, int type) {

		Log.d(TAG, "addSDFileRecordIDtoUploadingTrack id " + sdfilerecord_id
				+ " type :" + type);

		HashSet<Integer> services_in_progress;
		if (in_progress_uploads.containsKey(sdfilerecord_id)) {
			services_in_progress = in_progress_uploads.get(sdfilerecord_id);
			services_in_progress.add(type);
		} else {
			services_in_progress = new HashSet<Integer>();
			services_in_progress.add(type);
			in_progress_uploads.put(sdfilerecord_id, services_in_progress);
		}

	}

	public boolean isSDFileRecordUploading(long sdfilerecord_id, int type) {
		HashSet<Integer> services_in_progress = in_progress_uploads
				.get(sdfilerecord_id);
		if (services_in_progress == null)
			return false;
		return services_in_progress.contains(type);

	}

	public HashSet<Integer> isSDFileRecordUploadingToAnyService(
			long sdfilerecord_id) {
		HashSet<Integer> services_in_progress = in_progress_uploads
				.get(sdfilerecord_id);
		return services_in_progress;

	}

	public void removeSDFileRecordIDfromUploadingTrack(long sdfilerecord_id,
			int type) {

		Log.d(TAG, "removeSDFileRecordIDfromUploadingTrack id "
				+ sdfilerecord_id + " type :" + type);

		HashSet<Integer> services_in_progress;
		if (in_progress_uploads.containsKey(sdfilerecord_id)) {
			services_in_progress = in_progress_uploads.get(sdfilerecord_id);
			services_in_progress.remove(type);
		} else {
			// nothing to do.
		}
	}

}
