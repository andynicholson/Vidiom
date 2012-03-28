package au.com.infiniterecursion.vidiom.activity;

import java.util.ArrayList;

import android.content.res.Resources;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import au.com.infiniterecursion.vidiom.VidiomApp;
import au.com.infiniterecursion.vidiompro.R;

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
	private PreferenceScreen ps;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mainapp = (VidiomApp) getApplication();
		Log.d(TAG, " preferences onCreate");
		ps = getPreferenceScreen();
	}

	private void setupValidQualityPreferences() {
		ListPreference qualityPreferenceList = (ListPreference) ps
				.findPreference("recordingQualityPreference");
		CharSequence[] entries_values = qualityPreferenceList.getEntryValues();

		ArrayList<CharSequence> valid_entries_values = new ArrayList<CharSequence>();
		ArrayList<CharSequence> valid_entries = new ArrayList<CharSequence>();

		Resources res = getResources();

		//  Only for v11 support or higher, can we use hasProfile
		if (mainapp.support_v11) {

			for (CharSequence c : entries_values) {
				Log.d(TAG, " entry value is " + c);

				if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[0])) {
					// default quality
					Log.d(TAG, " testing default recording quality");
					// assume it is available
					valid_entries_values.add(c);

					valid_entries.add(res
							.getStringArray(R.array.recordingQualityTypes)[0]);

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[1])) {
					// Check if user wanted low or high quality recording.
					// First array entry is low -- check strings.xml
					Log.d(TAG, " testing low recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_LOW);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[1]);
					}

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[2])) {
					// High quality
					Log.d(TAG, " testing high recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_HIGH);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[2]);
					}

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[3])) {
					// QCIF quality
					Log.d(TAG, " testing QCIF recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_QCIF);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[3]);
					}

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[4])) {
					// CIF quality
					Log.d(TAG, " testing CIF recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_CIF);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[4]);
					}

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[5])) {
					// 480p quality
					Log.d(TAG, " testing 480p recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_480P);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[5]);
					}

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[6])) {
					// 720p quality
					Log.d(TAG, " testing 720p recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_720P);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[6]);
					}

				} else if (c.equals(res
						.getStringArray(R.array.recordingQualityTypeIds)[7])) {
					// 1080p quality
					Log.d(TAG, " testing 1080p recording quality");

					boolean hasIt = CamcorderProfile
							.hasProfile(CamcorderProfile.QUALITY_1080P);
					if (hasIt) {
						valid_entries_values.add(c);
						valid_entries
								.add(res.getStringArray(R.array.recordingQualityTypes)[7]);
					}
				}
			}
			
		} else {
			// if not v11 or higher, only use default, low and high.
			// Plus 480p
			
			//The descriptive strings
			//
			valid_entries.add(res
					.getStringArray(R.array.recordingQualityTypes)[0]);
			valid_entries.add(res
					.getStringArray(R.array.recordingQualityTypes)[1]);
			valid_entries.add(res
					.getStringArray(R.array.recordingQualityTypes)[2]);
			//Custom 720 x 480
			valid_entries.add(res
					.getStringArray(R.array.recordingQualityTypes)[8]);
			
			
			// The values
			valid_entries_values.add(res
					.getStringArray(R.array.recordingQualityTypeIds)[0]);
			valid_entries_values.add(res
					.getStringArray(R.array.recordingQualityTypeIds)[1]);
			valid_entries_values.add(res
					.getStringArray(R.array.recordingQualityTypeIds)[2]);
			//Custom 720 x 480
			valid_entries_values.add(res
					.getStringArray(R.array.recordingQualityTypeIds)[8]);
			
		}

		// Update the list preference.
		CharSequence[] ve = new CharSequence[valid_entries.size()];
		valid_entries.toArray(ve);
		qualityPreferenceList.setEntries(ve);
		CharSequence[] vev = new CharSequence[valid_entries_values.size()];
		valid_entries_values.toArray(vev);
		qualityPreferenceList.setEntryValues(vev);

	}

	private void updateFolderLocation() {
		// find the preference.
		String key = getResources().getString(R.string.defaultVideosFolder);
		Preference tp = ps.findPreference(key);
		tp.setSummary("Current location is " + mainapp.getCurrentPath());
	}

	public void onResume() {
		super.onResume();
		Log.d(TAG, " preferences onResume");
		updateFolderLocation();

		setupValidQualityPreferences();
	}
}
