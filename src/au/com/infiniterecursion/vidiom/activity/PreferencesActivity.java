package au.com.infiniterecursion.vidiom.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import au.com.infiniterecursion.vidiompro.R;
import au.com.infiniterecursion.vidiom.VidiomApp;

/*
 * Preferences 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class PreferencesActivity extends PreferenceActivity {

	private static final String TAG = "RoboticEye-PreferencesActivity";
	private VidiomApp mainapp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mainapp = (VidiomApp) getApplication();
		
		updateFolderLocation();
		
	}

	private void updateFolderLocation() {
		PreferenceScreen ps = getPreferenceScreen();
		//XXX use the key?
		Preference tp = ps.getPreference(0);
		tp.setSummary("Current location is " + mainapp.getCurrentPath());
		Log.d(TAG, " preferences onCreate");
	}
	
	public void onResume() {
		super.onResume();
		
		updateFolderLocation();
	}
}
