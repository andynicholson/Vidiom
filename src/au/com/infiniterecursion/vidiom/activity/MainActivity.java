package au.com.infiniterecursion.vidiom.activity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import au.com.infiniterecursion.vidiom.VidiomApp;
import au.com.infiniterecursion.vidiom.utils.DBUtils;
import au.com.infiniterecursion.vidiom.utils.PublishingUtils;
import au.com.infiniterecursion.vidiompro.R;

/*
 * Main Vidiom Activity 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback,
		VidiomActivity, MediaRecorder.OnInfoListener {

	private static final String TAG = "RoboticEye";

	// Menu ids
	private static final int MENU_ITEM_1 = Menu.FIRST;
	private static final int MENU_ITEM_2 = MENU_ITEM_1 + 1;
	private static final int MENU_ITEM_3 = MENU_ITEM_2 + 1;
	private static final int MENU_ITEM_4 = MENU_ITEM_3 + 1;
	private static final int MENU_ITEM_5 = MENU_ITEM_4 + 1;
	private static final int MENU_ITEM_6 = MENU_ITEM_5 + 1;
	private static final int MENU_ITEM_7 = MENU_ITEM_6 + 1;

	private static final int NOTIFICATION_ID = 1;

	// Camera objects
	//
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private boolean previewRunning;

	// Objects for recording
	private MediaRecorder mediaRecorder;
	// eg 40 seconds max video
	private int maxDurationInMs = 0;
	// eg 1MB limit
	private long maxFileSizeInBytes = 0;
	private final int videoFramesPerSecond = 25;

	// App state
	private boolean recordingInMotion;
	// Filenames (abs, relative) for latest recorded video file.
	private String latestVideoFile_absolutepath;
	private String latestVideoFile_filename;

	private long latestsdrecord_id;
	private long startTimeinMillis;
	private long endTimeinMillis;

	// Video files
	private File folder;

	private boolean canAccessSDCard = false;

	// Preferences
	private boolean autoEmailPreference;
	private boolean fTPPreference;
	private boolean videobinPreference;
	private boolean facebookPreference;
	private boolean youtubePreference;
	private boolean twitterPreference;

	private String emailPreference;
	private String filenameConventionPrefence;
	private String maxDurationPreference;
	private String maxFilesizePreference;

	// Message queue
	private Handler handler;
	// Database
	private DBUtils db_utils;
	// Publishing utilities
	private PublishingUtils pu;
	// Android preferences
	private SharedPreferences prefs;

	// Uploading threads
	private Thread threadVB;
	private Thread threadFB;
	private Thread threadFTP;

	private VidiomApp mainapp;

	// For naming videos after recording finishes.
	protected String title;
	protected String description;

	protected TextView statusIndicator;

	private Resources res;

	// UI threaded updater infrastructure
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);
	private int seconds_recording = 0;
	ScheduledFuture<?> ui_Incrementer_handler;

	final Runnable ui_Incrementer = new Runnable() {
		public void run() {
			seconds_recording++;
			Log.d(TAG, " Seconds recording are " + seconds_recording);

			int minutes = seconds_recording / 60;
			int seconds = seconds_recording % 60;
			final String time_format = String.format("%02d", minutes) + ":"
					+ String.format("%02d", seconds);
			handler.postDelayed(new Runnable() {
				public void run() {
					statusIndicator.setText("RECORDING " + time_format);
				}
			}, 0);

		}
	};

	private boolean showing_titledesc = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();

		Log.d(TAG, "On create");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		setContentView(R.layout.surface_layout);
		surfaceView = (SurfaceView) findViewById(R.id.surface_camera);

		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// Overlay , for Recording indications etc

		LayoutInflater overlayInflater = LayoutInflater.from(getBaseContext());
		View viewControl = overlayInflater.inflate(R.layout.camera_overlay,
				null);
		LayoutParams layoutParamsControl = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(viewControl, layoutParamsControl);

		statusIndicator = (TextView) findViewById(R.id.overlay);
		// Make the icon and the current text clickable to record/stop

		statusIndicator.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Toggle the status.
				if (recordingInMotion) {
					stopRecording();
				} else {
					// Dont start if we are still showing dialog from
					// stopRecording..
					if (!showing_titledesc) {
						tryToStartRecording();
					}
				}

			}
		});

		recordingInMotion = false;

		latestVideoFile_absolutepath = "";
		latestVideoFile_filename = "";
		startTimeinMillis = endTimeinMillis = 0;

		mainapp = (VidiomApp) getApplication();

		// Helper classes
		//

		handler = new Handler();
		db_utils = new DBUtils(getBaseContext());
		pu = new PublishingUtils(res, db_utils);
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// dump stats
		db_utils.getStats();

		// check our folder exists, and if not make it
		checkInstallDirandCreateIfMissing();

		// Initial install?
		checkIfFirstTimeRunAndWelcome();

		threadVB = null;
		threadFTP = null;

		// set path in mainapp
		mainapp.setCurrentPath(folder.getPath());

	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "On resume");
		loadPreferences();
	}

	@Override
	public void onPause() {

		super.onPause();
		Log.d(TAG, "On pause");
		if (mediaRecorder != null) {
			if (recordingInMotion) {
				stopRecording();
			}
			mediaRecorder.release();
		}

		if (threadVB != null) {
			Log.d(TAG, "Interrupting videobin thread");
			threadVB.interrupt();
		}
		if (threadFTP != null) {
			Log.d(TAG, "Interrupting FTP thread;");
			threadFTP.interrupt();
		}
		if (threadFB != null) {
			Log.d(TAG, "Interrupting facebook thread");
			threadFB.interrupt();
		}
	}

	public int getVersionCode() {
		int v = 0;
		try {
			v = getPackageManager().getPackageInfo(
					getApplicationInfo().packageName, 0).versionCode;
		} catch (NameNotFoundException e) {
			// Huh? Really?
		}
		return v;
	}

	public String getVersionName() {
		String name = null;
		try {
			name = getPackageManager().getPackageInfo(
					getApplicationInfo().packageName, 0).versionName;
		} catch (NameNotFoundException e) {
			// Huh? Really?
		}
		return name;
	}

	private void checkIfFirstTimeRunAndWelcome() {
		//
		boolean first_time = prefs.getBoolean("firstTimeRun", true);

		if (first_time) {

			Editor editor = prefs.edit();
			editor.putBoolean("firstTimeRun", false);
			editor.commit();

			String possibleEmail = null;
			// We need an email account to send emails for video publishing
			// notifications
			Account[] accounts = AccountManager.get(this).getAccountsByType(
					"com.google");
			for (Account account : accounts) {
				// TODO: Check possibleEmail against an email regex or treat
				// account.name as an email address only for certain
				// account.type values.
				possibleEmail = account.name;
				Log.d(TAG, "Could use : " + possibleEmail);
			}

			if (possibleEmail != null) {

				// Lets preset the emailPreference.
				editor.putString("emailPreference", possibleEmail);
				editor.commit();
			}

			// Welcome dialog!
			new AlertDialog.Builder(MainActivity.this)
					.setMessage(R.string.welcome)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									// show second dialog
									new AlertDialog.Builder(MainActivity.this)
											.setMessage(R.string.welcome2)
											.setPositiveButton(
													R.string.yes,
													new DialogInterface.OnClickListener() {
														public void onClick(
																DialogInterface dialog,
																int whichButton) {

															// show second
															// dialog

														}
													}).show();

								}
							}).show();

		}

	}

	private void loadPreferences() {

		autoEmailPreference = prefs.getBoolean("autoemailPreference", false);
		fTPPreference = prefs.getBoolean("ftpPreference", false);
		videobinPreference = prefs.getBoolean("videobinPreference", false);
		facebookPreference = prefs.getBoolean("facebookPreference", false);
		emailPreference = prefs.getString("emailPreference", null);
		youtubePreference = prefs.getBoolean("youtubePreference", false);
		twitterPreference = prefs.getBoolean("twitterPreference", false);

		// Filename style, duration, max filesize

		filenameConventionPrefence = prefs.getString(
				"filenameConventionPrefence",
				res.getString(R.string.filenameConventionDefaultPreference));
		maxDurationPreference = prefs.getString("maxDurationPreference",
				res.getString(R.string.maxDurationPreferenceDefault));
		maxFilesizePreference = prefs.getString("maxFilesizePreference",
				res.getString(R.string.maxFilesizePreferenceDefault));

		// set the videos folder, either the default or a set user preference.
		findVideosFolder();

		Log.d(TAG, "behaviour preferences are " + autoEmailPreference + ":"
				+ fTPPreference + ":" + videobinPreference + ":"
				+ emailPreference);

		Log.d(TAG, "video recording preferences are "
				+ filenameConventionPrefence + ":" + maxDurationPreference
				+ ":" + maxFilesizePreference);

	}

	private void checkInstallDirandCreateIfMissing() {
		// set the videos folder, either the default or a set user preference.
		findVideosFolder();

		// it is writeable?
		boolean success;

		if (!folder.exists()) {

			Log.d(TAG, " Folder doesnt exit ... attempting to make it");

			success = folder.mkdir();
			if (!success) {
				canAccessSDCard = false;

				new AlertDialog.Builder(this)
						.setMessage(R.string.sdcard_failed)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {

									}
								})

						.setNegativeButton(R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {

									}
								}).show();
			} else {
				Log.d(TAG, " Folder created...");
				canAccessSDCard = true;
			}
		} else {
			Log.d(TAG, " Folder already exists...");
			canAccessSDCard = true;
		}
	}

	private void findVideosFolder() {
		// android.os.Environment.getExternalStorageDirectory().getPath()

		// DEFAULT videos folder
		folder = new File(Environment.getExternalStorageDirectory()
				+ res.getString(R.string.rootSDcardFolder));
		Log.d(TAG, "Base Folder:" + folder.getAbsolutePath());
		// Check for a set preference, if there is one, use that instead.
		String customVideoFolder = prefs.getString(
				res.getString(R.string.customVideoFolderPreference), "");
		Log.d(TAG, "Custom Base Folder:" + customVideoFolder);

		if (customVideoFolder.length() > 0) {
			// Use the custom set path.
			folder = new File(customVideoFolder);
		}

		Log.d(TAG, "Using Folder:" + folder.getAbsolutePath());
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		Log.i(TAG, "OnPrepareOptionsMenu called");

		createConditionalMenu(menu);

		addConstantMenuItems(menu);

		return true;
	}

	private void createConditionalMenu(Menu menu) {
		menu.clear();

		Log.i(TAG, "createConditionalMenu called. recordingInMotion ? "
				+ recordingInMotion);
		// Conditionally on menu items.
		if (recordingInMotion) {
			menu.removeItem(MENU_ITEM_1);
			MenuItem menu_stop = menu.add(0, MENU_ITEM_2, 0,
					R.string.menu_stop_recording);
			menu_stop.setIcon(R.drawable.stop48);
		} else {
			if (canAccessSDCard) {
				MenuItem menu_start = menu.add(0, MENU_ITEM_1, 0,
						R.string.menu_start_recording);
				menu_start.setIcon(R.drawable.sun48);
				menu.removeItem(MENU_ITEM_2);
			}
		}

	}

	private void addConstantMenuItems(Menu menu) {
		// ALWAYS ON menu items.
		MenuItem menu_about = menu.add(0, MENU_ITEM_5, 0, R.string.menu_about);
		menu_about.setIcon(R.drawable.wizard48);
		MenuItem menu_prefs = menu.add(0, MENU_ITEM_6, 0,
				R.string.menu_preferences);
		menu_prefs.setIcon(R.drawable.options);
		MenuItem menu_library = menu.add(0, MENU_ITEM_7, 0,
				R.string.menu_library);
		menu_library.setIcon(R.drawable.business48);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuitem) {
		int menuNum = menuitem.getItemId();

		Log.d("MENU", "Option " + menuNum + " selected");

		switch (menuitem.getItemId()) {

		// Record
		case MENU_ITEM_1:

			tryToStartRecording();

			break;

		// Stop
		case MENU_ITEM_2:
			menuResponseForStopItem();
			break;

		case MENU_ITEM_5:
			// ABOUT
			String mesg = getString(R.string.about_this);
			// find&replace VERSION
			mesg = mesg.replace("VERSION", "version " + getVersionName());

			final SpannableString s = new SpannableString(mesg);
			Linkify.addLinks(s, Linkify.ALL);

			// Licenses
			String mesg2 = getString(R.string.third_party_licenses);

			final SpannableString s2 = new SpannableString(mesg2);
			Linkify.addLinks(s2, Linkify.ALL);

			AlertDialog about = new AlertDialog.Builder(this)
					.setMessage(s)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							})
					.setNegativeButton(R.string.licenses,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									AlertDialog licenses = new AlertDialog.Builder(
											MainActivity.this)
											.setMessage(s2)
											.setPositiveButton(
													R.string.yes,
													new DialogInterface.OnClickListener() {
														public void onClick(
																DialogInterface dialog,
																int whichButton) {

														}
													}).show();

									// makes links work
									((TextView) licenses
											.findViewById(android.R.id.message))
											.setMovementMethod(LinkMovementMethod
													.getInstance());

								}
							})
					.setNeutralButton(R.string.help,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// help
									AlertDialog help = new AlertDialog.Builder(
											MainActivity.this)
											.setMessage(
													res.getString(R.string.helpText))
											.setPositiveButton(
													R.string.yes,
													new DialogInterface.OnClickListener() {
														public void onClick(
																DialogInterface dialog,
																int whichButton) {

														}
													}).show();
								}
							}).show();

			// makes links work
			((TextView) about.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());

			break;

		case MENU_ITEM_6:

			// Preferences
			Intent intent = new Intent().setClass(this,
					PreferencesActivity.class);
			this.startActivityForResult(intent, 0);

			break;

		case MENU_ITEM_7:

			// Library menu option

			// Launch library activity, showing list of recorded videos
			// their properties, if they are still 'on disk'
			// how they were published, links to published sites
			// etc
			Intent intent2 = new Intent().setClass(this, LibraryActivity.class);
			this.startActivityForResult(intent2, 0);

			break;

		default:
			return super.onOptionsItemSelected(menuitem);
		}

		return true;
	}

	/*
	 * 
	 * Menu response methods
	 */

	private void menuResponseForStopItem() {
		Log.d(TAG, "State is recordingInMotion:" + recordingInMotion);

		if (recordingInMotion) {

			stopRecording();

		} else {
			//
			new AlertDialog.Builder(this)
					.setMessage(R.string.notrecording)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							})

					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();
		}
	}

	/*
	 * Camera methods
	 */

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "surfaceChanged");
		// Check camera isnt null for some reason
		if (camera == null) {
			Log.e(TAG, "surfaceChanged: camera is null!");
			return;
		}

		if (previewRunning) {
			camera.stopPreview();
		}
		// Set parameters
		Camera.Parameters p = camera.getParameters();
		Log.d(TAG, " format width height are : " + format + ":" + width + ":"
				+ height);

		// XXX should query supported capabilities first.

		// 320, 240 seems only possible resolution
		// and it seems the preview size must be the same as the video size
		//
		p.setPreviewSize(320, 240);
		// p.setPictureSize(320,240);
		p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
		p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		// Log.d(TAG, "Parameters are " + p.toString());

		try {
			camera.setParameters(p);
		} catch (RuntimeException e) {
			e.printStackTrace();
			Log.e(TAG, " setParameters failed! " + e.getMessage());
		}

		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
			previewRunning = true;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// Create camera object
		Log.d(TAG, "surfaceCreated!");
		try {
			camera = Camera.open();
		} catch (RuntimeException e) {
			e.printStackTrace();
			camera = null;
		}

		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			camera.setParameters(params);
		} else {
			Toast.makeText(getApplicationContext(), "Camera not available!",
					Toast.LENGTH_LONG).show();
			finish();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		//
		Log.d(TAG, "surfaceDestroyed!");
		previewRunning = false;
		if (camera != null) {
			camera.stopPreview();
			camera.release();
		} else {
			Log.e(TAG, "surfaceDestroyed: camera is null!");
		}
	}

	private void tryToStartRecording() {
		// If all the stars are aligned..
		if (canAccessSDCard && previewRunning && startRecording()) {

			recordingInMotion = true;

		} else {

			new AlertDialog.Builder(MainActivity.this)
					.setMessage(R.string.camera_failed)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();

			recordingInMotion = false;
		}
	}

	public boolean startRecording() {

		if (camera == null) {
			Log.e(TAG, "startRecording: camera is null!");
			return false;
		}

		try {
			camera.unlock();
		} catch (RuntimeException e) {
			e.printStackTrace();
			camera = null;
			Log.e(TAG, "startRecording: unlock failed!");
			return false;
		}

		statusIndicator.setText("RECORDING 00:00");

		mediaRecorder = new MediaRecorder();
		mediaRecorder.setOnInfoListener(this);

		mediaRecorder.setCamera(camera);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

		Log.d(TAG, " startRecording - preferences are " + maxDurationPreference
				+ ":" + filenameConventionPrefence + ":"
				+ maxFilesizePreference);

		Integer user_duration = 0;
		Integer user_filesize = 0;

		try {
			user_duration = Integer.parseInt(maxDurationPreference);
		} catch (NumberFormatException nfe) {
			// whatever
			user_duration = 0;
		}

		// preferences for user in seconds.
		maxDurationInMs = user_duration * 1000;
		mediaRecorder.setMaxDuration(maxDurationInMs);

		File tempFile = pu.selectFilenameAndCreateFile(
				filenameConventionPrefence, folder);
		latestVideoFile_filename = tempFile.getName();
		latestVideoFile_absolutepath = tempFile.getAbsolutePath();
		mediaRecorder.setOutputFile(tempFile.getAbsolutePath());
		Log.d(TAG, "Starting recording into " + tempFile.getAbsolutePath());

		mediaRecorder.setVideoFrameRate(videoFramesPerSecond);

		// mediaRecorder.setVideoSize(surfaceView.getWidth(),
		// surfaceView.getHeight())

		mediaRecorder.setVideoSize(320, 240);

		mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

		try {
			user_filesize = Integer.parseInt(maxFilesizePreference);
		} catch (NumberFormatException nfe) {
			// whatever
			user_filesize = 0;
		}

		// preferences for user in KB.
		maxFileSizeInBytes = user_filesize * 1024;
		try {
			mediaRecorder.setMaxFileSize(maxFileSizeInBytes);
		} catch (RuntimeException rte) {
			Log.e(TAG, "RuntimeException:" + rte.getMessage());
			rte.printStackTrace();
			// keep going -might still work??
		}

		try {
			// Setup media recorder
			//
			mediaRecorder.prepare();
			mediaRecorder.start();

			startTimeinMillis = System.currentTimeMillis();

			// start UI updater thread
			seconds_recording = 0;
			ui_Incrementer_handler = scheduler.scheduleAtFixedRate(
					ui_Incrementer, 1, 1, TimeUnit.SECONDS);

			return true;

		} catch (IllegalStateException e) {
			Log.e(TAG, "Illegal State Exception:" + e.getMessage());
			e.printStackTrace();

			startRecordingMediaRecorderExceptionHandler();

			return false;
		} catch (IOException e) {
			Log.e(TAG, "IOException:" + e.getMessage());
			e.printStackTrace();

			startRecordingMediaRecorderExceptionHandler();

			return false;
		} catch (RuntimeException re) {
			Log.e(TAG, "RuntimeException:" + re.getMessage());
			re.printStackTrace();

			startRecordingMediaRecorderExceptionHandler();

			return false;
		}

	}

	private void startRecordingMediaRecorderExceptionHandler() {
		// Reset the UI
		shutdownRecIncrUIThread();

		try {
			camera.lock();
		} catch (RuntimeException e) {
			e.printStackTrace();
			camera = null;
			Log.e(TAG, "startRecording: unlock failed!");

		}
	}

	public void stopRecording() {

		recordingInMotion = false;
		endTimeinMillis = System.currentTimeMillis();
		shutdownRecIncrUIThread();

		Boolean show_dialog = true;

		try {
			mediaRecorder.stop();
		} catch (RuntimeException e) {
			e.printStackTrace();
			Log.e(TAG, "stopRecording: stop failed!");
			show_dialog = false;
		}

		try {
			camera.lock();
		} catch (RuntimeException e) {
			e.printStackTrace();
			camera = null;
			Log.e(TAG, "stopRecording: unlock failed!");
			show_dialog = false;
		}

		Log.d(TAG, "Recording time of video is "
				+ ((endTimeinMillis - startTimeinMillis) / 1000)
				+ " seconds. filename " + latestVideoFile_filename + " : path "
				+ latestVideoFile_absolutepath);

		// ask for title and description after capture.
		title = null;
		description = null;

		// if mediarecorder and camera stop/locking worked.
		if (show_dialog) {
			showTitleDescriptionDialog();
		}
	}

	private void shutdownRecIncrUIThread() {
		// Shutdown the threads
		scheduler.schedule(new Runnable() {
			public void run() {
				// cancel UI updater
				ui_Incrementer_handler.cancel(true);
				// reset the indicator
				handler.postDelayed(new Runnable() {
					public void run() {
						statusIndicator.setText("STOPPED");
					}
				}, 500);

			}
		}, 0, TimeUnit.SECONDS);
	}

	private void showTitleDescriptionDialog() {
		// Launch Title/Description Edit View

		showing_titledesc = true;

		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final View title_descr = inflater
				.inflate(R.layout.title_and_desc, null);

		final EditText title_edittext = (EditText) title_descr
				.findViewById(R.id.EditTextTitle);
		final EditText desc_edittext = (EditText) title_descr
				.findViewById(R.id.EditTextDescr);

		final Dialog d = new Dialog(this);
		Window w = d.getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		d.setTitle(R.string.rename_video);
		// the edit layout defined in an xml file (in res/layout)
		d.setContentView(title_descr);
		// Cancel
		Button cbutton = (Button) d.findViewById(R.id.button2Cancel);
		cbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// delete the file
				pu.deleteVideo(latestVideoFile_absolutepath);

				showing_titledesc = false;
				d.dismiss();
			}
		});
		// Edit
		Button ebutton = (Button) d.findViewById(R.id.button1Edit);
		ebutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// save title and description to DB.
				title = title_edittext.getText().toString();
				description = desc_edittext.getText().toString();

				Log.d(TAG, "New title and description is " + title + ":"
						+ description);

				latestsdrecord_id = db_utils
						.createSDFileRecordwithNewVideoRecording(
								latestVideoFile_absolutepath,
								latestVideoFile_filename,
								(int) ((endTimeinMillis - startTimeinMillis) / 1000),
								// XXX hardcoded vid & audio codecs
								"h263;amr-nb", title, description);

				// If ID > 0, then new record in DB was successfully created
				if (latestsdrecord_id > 0) {

					Log.d(TAG,
							"Valid DB Record - can send video file - sdrecord id  is "
									+ latestsdrecord_id);

					// Send the info to the inbuilt Android Media Scanner

					// Save the name and description of a video in a
					// ContentValues map.
					ContentValues values = new ContentValues(2);
					values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
					values.put(MediaStore.Video.Media.DATA,
							latestVideoFile_absolutepath);

					// Add a new record (identified by uri), but with the values
					// just set.
					Uri uri = getContentResolver()
							.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
									values);

					sendBroadcast(new Intent(
							Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

					// launch auto complete actions - make sure its AFTER
					// latestsdrecord_id is set.
					doAutoCompletedRecordedActions();

					// Video recording finished dialog!
					new AlertDialog.Builder(MainActivity.this)
							.setMessage(
									res.getString(R.string.file_saved)
											+ " "
											+ latestVideoFile_filename
											+ '\n'
											+ res.getString(R.string.posts_in_gallery))
							.setPositiveButton(R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {

										}
									}).show();
				}

				showing_titledesc = false;
				d.dismiss();
			}
		});
		d.show();
	}

	public void onInfo(MediaRecorder mr, int what, int extra) {
		// called when an error occurs

		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
				|| what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

			Log.d(TAG, "We have reached the end limit");

			// Video recording finished dialog!
			new AlertDialog.Builder(MainActivity.this)
					.setMessage(res.getString(R.string.limits_reached))
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									stopRecording();

								}
							}).show();

		}

	}

	private void doAutoCompletedRecordedActions() {
		// Auto completion actions

		Log.d(TAG, "Doing auto completed recorded actions");
		String[] strs = db_utils
				.getTitleAndDescriptionFromID(new String[] { Long
						.toString(latestsdrecord_id) });

		if (videobinPreference) {

			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(MainActivity.this,
							R.string.auto_publishing_to_videobin_org,
							Toast.LENGTH_LONG).show();
				}
			});

			threadVB = pu.videoUploadToVideoBin(this, handler,
					latestVideoFile_absolutepath, strs[0], strs[1],
					emailPreference, latestsdrecord_id);
		}

		if (fTPPreference) {

			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(MainActivity.this,
							R.string.auto_publishing_to_ftp_server,
							Toast.LENGTH_LONG).show();
				}
			});

			threadFTP = pu.videoUploadToFTPserver(this, handler,
					latestVideoFile_filename, latestVideoFile_absolutepath,
					emailPreference, latestsdrecord_id);
		}

		// facebook auto publishing
		if (facebookPreference) {

			// get title and description for video upload to FB
			if (mainapp.getFacebook() != null
					&& mainapp.getFacebook().isSessionValid()) {

				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(MainActivity.this,
								R.string.auto_publishing_to_facebook_com,
								Toast.LENGTH_LONG).show();
					}
				});

				threadFB = pu.videoUploadToFacebook(this, handler,
						mainapp.getFacebook(), latestVideoFile_absolutepath,
						strs[0], strs[1], emailPreference, latestsdrecord_id);
			}
		}

		// youtube auto publishing
		if (youtubePreference) {

			String possibleEmail = null;
			// We need a linked google account for youtube.
			Account[] accounts = AccountManager.get(this).getAccountsByType(
					"com.google");
			for (Account account : accounts) {
				// TODO: Check possibleEmail against an email regex or treat
				// account.name as an email address only for certain
				// account.type values.
				possibleEmail = account.name;
				Log.d(TAG, "Could use : " + possibleEmail);
			}
			if (possibleEmail != null) {
				Log.d(TAG, "Using account name for youtube upload .. "
						+ possibleEmail);
				// This launches the youtube upload process

				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(MainActivity.this,
								R.string.auto_publishing_to_youtube_com,
								Toast.LENGTH_LONG).show();
					}
				});

				pu.getYouTubeAuthTokenWithPermissionAndUpload(this,
						possibleEmail, latestVideoFile_absolutepath, handler,
						emailPreference, latestsdrecord_id);
			}

		}

		// Leave as last
		if (autoEmailPreference) {
			pu.launchEmailIntentWithCurrentVideo(this,
					latestVideoFile_absolutepath);
		}

	}

	public boolean isUploading() {
		// are we?
		return mainapp.isUploading();
	}

	public void startedUploading() {
		this.createNotification(res.getString(R.string.starting_upload) + " "
				+ latestVideoFile_filename);
		// flip the switch
		mainapp.setUploading();
	}

	public void finishedUploading(boolean success) {
		// not uploading anymore.

		// Auto twittering.
		//
		if (twitterPreference && success) {

			new Thread(new Runnable() {
				public void run() {

					// Check there is a hosted URL for a start..
					String hosted_url_to_tweet = db_utils
							.getHostedURLFromID(new String[] { Long
									.toString(latestsdrecord_id) });
					Log.d(TAG, " checking " + hosted_url_to_tweet
							+ " in auto twitter publishing");

					if (hosted_url_to_tweet != null) {

						// Check there is a valid twitter OAuth tokens.
						String twitterToken = prefs.getString("twitterToken",
								null);
						String twitterTokenSecret = prefs.getString(
								"twitterTokenSecret", null);

						if (twitterToken != null && twitterTokenSecret != null) {

							// Ok, now we can tweet this URL
							AccessToken a = new AccessToken(twitterToken,
									twitterTokenSecret);
							Twitter twitter = new TwitterFactory()
									.getInstance();
							twitter.setOAuthConsumer(
									TwitterOAuthActivity.consumerKey,
									TwitterOAuthActivity.consumerSecret);
							twitter.setOAuthAccessToken(a);

							String status = "New video:" + hosted_url_to_tweet;
							try {
								twitter.updateStatus(status);
								runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(MainActivity.this,
												R.string.tweeted_ok,
												Toast.LENGTH_LONG).show();
									}
								});
							} catch (TwitterException e) {
								//
								e.printStackTrace();
								Log.e(TAG,
										"Auto twittering failed "
												+ e.getMessage());
							}
						}

					}

				}
			}).start();

		}

		mainapp.setNotUploading();
	}

	public void createNotification(String notification_text) {

		CharSequence contentTitle = res.getString(R.string.notification_title);
		CharSequence contentText = notification_text;

		final Notification notifyDetails = new Notification(R.drawable.icon,
				notification_text, System.currentTimeMillis());

		Intent notifyIntent = new Intent(this, MainActivity.class);

		PendingIntent intent = PendingIntent.getActivity(this, 0, notifyIntent,
				PendingIntent.FLAG_UPDATE_CURRENT
						| Notification.FLAG_AUTO_CANCEL);

		notifyDetails.setLatestEventInfo(this, contentTitle, contentText,
				intent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(NOTIFICATION_ID, notifyDetails);

	}

}
