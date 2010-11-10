package au.com.infiniterecursion.roboticeye;

import android.os.Bundle;
import android.preference.PreferenceActivity;


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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		/*
		EditTextPreference myEditTextPreference = null;
		myEditTextPreference = (EditTextPreference) findPreference("mobilepreference");
		EditText myEditText = (EditText)myEditTextPreference.getEditText(); 
		myEditText.setKeyListener(DigitsKeyListener.getInstance(false,true));
		*/
	}
}
