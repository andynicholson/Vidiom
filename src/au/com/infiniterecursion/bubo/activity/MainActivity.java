package au.com.infiniterecursion.bubo.activity;

import java.io.File;
import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import au.com.infiniterecursion.bubo.BuboApp;
import au.com.infiniterecursion.bubo.R;
import au.com.infiniterecursion.bubo.utils.DBUtils;
import au.com.infiniterecursion.bubo.utils.PublishingUtils;


/*
 * Main RoboticEye Activity 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback, RoboticEyeActivity,
		MediaRecorder.OnInfoListener {

	private static final String TAG = "RoboticEye";

	//Menu ids
	private static final int MENU_ITEM_1 = Menu.FIRST;
	private static final int MENU_ITEM_2 = MENU_ITEM_1 + 1;
	private static final int MENU_ITEM_3 = MENU_ITEM_2 + 1;
	private static final int MENU_ITEM_4 = MENU_ITEM_3 + 1;
	private static final int MENU_ITEM_5 = MENU_ITEM_4 + 1;
	private static final int MENU_ITEM_6 = MENU_ITEM_5 + 1;
	private static final int MENU_ITEM_7 = MENU_ITEM_6 + 1;

	private static final int NOTIFICATION_ID = 1;

	
	
	//Camera objects
	//
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private boolean previewRunning;

	// Objects for recording
	private MediaRecorder mediaRecorder;
	// eg 40 seconds max video
	private int maxDurationInMs = 0;
	//eg 1MB limit
	private long maxFileSizeInBytes = 0;
	private final int videoFramesPerSecond = 25;

	//App state
	private boolean recordingInMotion;
	//Filenames (abs, relative) for latest recorded video file.
	private String latestVideoFile_absolutepath;
	private String latestVideoFile_filename;
	
	private long latestsdrecord_id;
	private long startTimeinMillis;
	private long endTimeinMillis;
	
	
	//Video files
	private File folder;
	
	private boolean canAccessSDCard = false;

	//Preferences
	private boolean autoEmailPreference;
	private boolean fTPPreference;
	private boolean videobinPreference;
	private boolean facebookPreference;
	private boolean youtubePreference;
	private String emailPreference;
	private String filenameConventionPrefence;
	private String maxDurationPreference;
	private String maxFilesizePreference;
	
	//Message queue
	private Handler handler;

	//Database
	private DBUtils db_utils;
	
	private PublishingUtils pu;

	private SharedPreferences prefs;

	//Uploading threads
	private Thread threadVB;
	private Thread threadFB;
	private Thread threadFTP;

	private BuboApp mainapp;

	//For naming videos after recording finishes.
	protected String title;
	protected String description;



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		Log.d(TAG,"On create");
		
		setContentView(R.layout.surface_layout);
		surfaceView = (SurfaceView) findViewById(R.id.surface_camera);

		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		recordingInMotion = false;
		
		latestVideoFile_absolutepath = "";
		latestVideoFile_filename = "";
		startTimeinMillis=endTimeinMillis=0;
	
		mainapp = (BuboApp) getApplication();
		
		//Helper classes
		//
	
		
		handler = new Handler();
		db_utils = new DBUtils(getBaseContext());
		pu=new PublishingUtils(getResources(), db_utils);
		prefs = PreferenceManager
		.getDefaultSharedPreferences(getBaseContext());
		
		//dump stats
		db_utils.getStats();
		
		// check our folder exists, and if not make it
		checkInstallDirandCreateIfMissing();
		
		// Initial install?
		checkIfFirstTimeRunAndWelcome();
		
		threadVB = null;
		threadFTP = null;
		
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG,"On resume");
		loadPreferences();
	}
	

	@Override
	public void onPause() {
		
		super.onDestroy();
		Log.d(TAG,"On pause");
		if (mediaRecorder != null) {
			if (recordingInMotion) {
				stopRecording();
			}
			mediaRecorder.release();
		}
		

		if (threadVB != null) {
			Log.d(TAG,"Interrupting videobin thread");
			threadVB.interrupt();
		}
		if (threadFTP != null) {
			Log.d(TAG,"Interrupting FTP thread;");
			threadFTP.interrupt();
		}
		if (threadFB != null) {
			Log.d(TAG,"Interrupting facebook thread");
			threadFB.interrupt();
		}
	}
	
	
	
	private void checkIfFirstTimeRunAndWelcome() {
		// 
		boolean first_time = prefs.getBoolean("firstTimeRun", true);
		
		if (first_time) {
			Editor editor = prefs.edit();
			editor.putBoolean("firstTimeRun", false);
			editor.commit();
			
			//Welcome dialog!
			new AlertDialog.Builder(MainActivity.this)
			.setMessage(R.string.welcome)
			.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

						}
					}).show();
			
			
		}
		
	}
	
	private void loadPreferences() {
		
		autoEmailPreference = prefs.getBoolean("autoemailPreference", false);
		fTPPreference = prefs.getBoolean("ftpPreference", false);
		videobinPreference = prefs.getBoolean("videobinPreference", false);
		facebookPreference = prefs.getBoolean("facebookPreference", false);
		emailPreference = prefs.getString("emailPreference",null);
		youtubePreference = prefs.getBoolean("youtubePreference", false);
		
		// Filename style, duration, max filesize
		Resources res = getResources();

		filenameConventionPrefence = prefs.getString("filenameConventionPrefence",res.getString(R.string.filenameConventionDefaultPreference));
		maxDurationPreference = prefs.getString("maxDurationPreference",res.getString(R.string.maxDurationPreferenceDefault));
		maxFilesizePreference = prefs.getString("maxFilesizePreference",res.getString(R.string.maxFilesizePreferenceDefault));
				
		Log.d(TAG,"behaviour preferences are " + autoEmailPreference+":"+fTPPreference+":"+videobinPreference+":"+emailPreference);
		
		Log.d(TAG,"video recording preferences are " + filenameConventionPrefence+":"+maxDurationPreference+":"+maxFilesizePreference);
	}

	private void checkInstallDirandCreateIfMissing() {
		// android.os.Environment.getExternalStorageDirectory().getPath()
		Resources res = getResources();
		folder = new File(Environment.getExternalStorageDirectory()
				+ res.getString(R.string.rootSDcardFolder));
		boolean success;
		Log.d(TAG, "Base Folder:" + folder.getAbsolutePath());
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

		Log.i(TAG, "createConditionalMenu called. recordingInMotion ? "  + recordingInMotion );
		// Conditionally on menu items.
		if (recordingInMotion) {
			menu.removeItem(MENU_ITEM_1);
			MenuItem menu_stop = menu.add(0, MENU_ITEM_2, 0,R.string.menu_stop_recording);
			menu_stop.setIcon(R.drawable.stop48);
		} else {
			if (canAccessSDCard) {
				MenuItem menu_start = menu.add(0, MENU_ITEM_1, 0, R.string.menu_start_recording);
				menu_start.setIcon(R.drawable.sun48);
				menu.removeItem(MENU_ITEM_2);
			}
		}

	}
	

	private void addConstantMenuItems(Menu menu) {
		// ALWAYS ON menu items.
		MenuItem menu_about = menu.add(0, MENU_ITEM_5, 0, R.string.menu_about);
		menu_about.setIcon(R.drawable.wizard48);
		MenuItem menu_prefs = menu.add(0, MENU_ITEM_6, 0, R.string.menu_preferences);
		menu_prefs.setIcon(R.drawable.options);
		MenuItem menu_library = menu.add(0, MENU_ITEM_7, 0, R.string.menu_library);
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
			new AlertDialog.Builder(this)
					.setMessage(R.string.about_this)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();

			break;

		case MENU_ITEM_6:

			// Preferences
			Intent intent = new Intent().setClass(this,
					PreferencesActivity.class);
			this.startActivityForResult(intent, 0);

			break;

		case MENU_ITEM_7:
			
			//Library menu option
			
			// Launch library activity, showing list of recorded videos
			// their properties, if they are still 'on disk'
			// how they were published, links to published sites 
			// etc
			Intent intent2 = new Intent().setClass(this,
					LibraryActivity.class);
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
		Log.d(TAG, "State is recordingInMotion:"+recordingInMotion);
		
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
	 * 
	 *
	 */
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//
		if (previewRunning) {
			camera.stopPreview();
		}
		Camera.Parameters p = camera.getParameters();

		Log.d(TAG, " format width height are : " + format + ":" + width + ":"
				+ height);

		// 320, 240 seems only possible resolution
		// and it seems the preview size must be the same as the video size
		//
		p.setPreviewSize(320, 240);
		// p.setPictureSize(320,240);
		p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

		p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		
		//Log.d(TAG, "Parameters are " + p.toString());

		camera.setParameters(p);

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
		//
		Log.d(TAG,"surfaceCreated!");
		camera = Camera.open();
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
		Log.d(TAG,"surfaceDestroyed!");
		camera.stopPreview();
		previewRunning = false;
		camera.release();
	}
	

	private void tryToStartRecording() {
		if (canAccessSDCard && startRecording()) {

			recordingInMotion = true;

		} else {

			new AlertDialog.Builder(MainActivity.this)
					.setMessage(R.string.camera_failed)
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

			recordingInMotion = false;
		}
	}

	public boolean startRecording() {
		
			camera.unlock();

			mediaRecorder = new MediaRecorder();
			mediaRecorder.setOnInfoListener(this);

			mediaRecorder.setCamera(camera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

			Log.d(TAG, " startRecording - preferences are " + maxDurationPreference + ":" + filenameConventionPrefence+":"+maxFilesizePreference);
			
			Integer user_duration = Integer.parseInt(maxDurationPreference);
			//preferences for user in seconds.
			maxDurationInMs = user_duration * 1000;
			mediaRecorder.setMaxDuration(maxDurationInMs);

			File tempFile = pu.selectFilenameAndCreateFile(filenameConventionPrefence);
			latestVideoFile_filename = tempFile.getName();
			latestVideoFile_absolutepath = tempFile.getAbsolutePath();
			mediaRecorder.setOutputFile(tempFile.getAbsolutePath());
			Log.d(TAG, "Starting recording into " + tempFile.getAbsolutePath());

			
			
			mediaRecorder.setVideoFrameRate(videoFramesPerSecond);

			// mediaRecorder.setVideoSize(surfaceView.getWidth(),
			// surfaceView.getHeight())

			mediaRecorder.setVideoSize(320, 240);

			mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

			Integer user_filesize = Integer.parseInt(maxFilesizePreference);
			//preferences for user in KB.
			maxFileSizeInBytes = user_filesize * 1024;
			mediaRecorder.setMaxFileSize(maxFileSizeInBytes);
	
		try {
			
			mediaRecorder.prepare();
			mediaRecorder.start();

			startTimeinMillis = System.currentTimeMillis();
			
			return true;
		} catch (IllegalStateException e) {
			Log.e(TAG, "Illegal State Exception" + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
			return false;
		}
	}



	public void stopRecording() {

		mediaRecorder.stop();
		camera.lock();
		recordingInMotion = false;
				
		endTimeinMillis = System.currentTimeMillis();
		
		Log.d(TAG, "Recording time of video is " + ((endTimeinMillis-startTimeinMillis)/1000) + " seconds. filename " + latestVideoFile_filename + " : path " + latestVideoFile_absolutepath);
		
		// ask for title and description after capture.
		title = null;
		description = null;
		LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	      
		final View title_descr = inflater.inflate(R.layout.title_and_desc, null);		
		final EditText title_edittext = (EditText) title_descr.findViewById(R.id.EditTextTitle);
		final EditText desc_edittext = (EditText) title_descr.findViewById(R.id.EditTextDescr);
		
		AlertDialog title_descr_dialog = new AlertDialog.Builder(this)
		.setMessage(R.string.rename_video)
		.setView(title_descr)
		.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						
						//title and description.
						MainActivity.this.title = title_edittext.getText().toString();
						MainActivity.this.description = desc_edittext.getText().toString() ;
						
						latestsdrecord_id = db_utils.createSDFileRecordwithNewVideoRecording(latestVideoFile_absolutepath, latestVideoFile_filename ,(int) ((endTimeinMillis-startTimeinMillis)/1000), "h263;samr", title, description);
						
						//If ID > 0, then new record in DB was successfully created
						if (latestsdrecord_id > 0) {
							
							Log.d(TAG, "Valid DB Record - can send video file - sdrecord id  is " + latestsdrecord_id);
							
							//launch auto complete actions - make sure its AFTER latestsdrecord_id is set.
							doAutoCompletedRecordedActions();
							
							Resources res = getResources();
							
							//Video recording finished dialog!
							new AlertDialog.Builder(MainActivity.this)
							.setMessage(res.getString(R.string.file_saved) + " " + latestVideoFile_filename + '\n' + res.getString(R.string.posts_in_gallery))
							.setPositiveButton(R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int whichButton) {

											
											
										}
									}).show();
						}
						
					}
				}).show();
		
	}
	

	public void onInfo(MediaRecorder mr, int what, int extra) {
		//
		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
				|| what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
			Log.d(TAG, "We have reached the end limit");
			
			stopRecording();
		}

	}
	
	private void doAutoCompletedRecordedActions() {
		// Auto completion actions

		Log.d(TAG, "Doing auto completed recorded actions");
		
		if (videobinPreference) {
			threadVB = pu.videoUploadToVideoBin(this, handler, latestVideoFile_absolutepath, emailPreference, latestsdrecord_id);
		}

		if (fTPPreference) {
			threadFTP = pu.videoUploadToFTPserver(this, handler, latestVideoFile_filename, latestVideoFile_absolutepath, latestsdrecord_id);
		}

		// facebook auto publishing
		if (facebookPreference) {
			//get title and description for video upload to FB
			String[] strs = db_utils.getTitleAndDescriptionFromID(new String[] { Long.toString(latestsdrecord_id)});
			threadFB = pu.videoUploadToFacebook(this, handler, mainapp.getFacebook(), latestVideoFile_absolutepath, strs[0], strs[1]+ "\n" + getString(R.string.uploaded_by_), latestsdrecord_id);
		}
		
		
		// youtube auto publishing
		if (youtubePreference) {
			
			
			String possibleEmail = null;
			//We need a linked google account for youtube.
			Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
			for (Account account : accounts) {
			  // TODO: Check possibleEmail against an email regex or treat
			  // account.name as an email address only for certain account.type values.
			  possibleEmail = account.name;
			  Log.d(TAG, "Could use : " + possibleEmail);
			}
			if (possibleEmail != null) {
				Log.d(TAG,"Using account name for youtube upload .. " + possibleEmail);
				// This launches the youtube upload process				
				pu.getYouTubeAuthTokenWithPermissionAndUpload(this, possibleEmail, latestVideoFile_absolutepath, handler, latestsdrecord_id);
			}
			
		}

		//XXX add in vimeo, when done.
				
		
		//Leave as last
		if (autoEmailPreference) {
			pu.launchEmailIntentWithCurrentVideo(this, latestVideoFile_absolutepath);
		}
		
		
	}


	public boolean isUploading() {
		// are we?
		return mainapp.isUploading();
	}


	public void startedUploading() {
		Resources res = getResources();
		this.createNotification(res.getString(R.string.starting_upload) + " " + latestVideoFile_filename);
		//flip the switch
		mainapp.setUploading();
	}


	public void finishedUploading(boolean success) {
		//not uploading.
		
		mainapp.setNotUploading();
	}

	public void createNotification(String notification_text) {
		Resources res = getResources();
		 CharSequence contentTitle = res.getString(R.string.bubo_notification_title);
		 CharSequence contentText = notification_text;

		 final Notification notifyDetails =
		        new Notification(R.drawable.icon, notification_text, System.currentTimeMillis());

		 Intent notifyIntent = new Intent(this, MainActivity.class);

		    PendingIntent intent =
		          PendingIntent.getActivity(this, 0,
		          notifyIntent,  PendingIntent.FLAG_UPDATE_CURRENT | Notification.FLAG_AUTO_CANCEL);

		    notifyDetails.setLatestEventInfo(this, contentTitle, contentText, intent);
		    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		    
		   mNotificationManager.notify(NOTIFICATION_ID, notifyDetails);
		
	}
	
}