package au.com.infiniterecursion.bubo.activity;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import au.com.infiniterecursion.bubo.BuboApp;
import au.com.infiniterecursion.bubo.R;
import au.com.infiniterecursion.bubo.facebook.LoginButton;
import au.com.infiniterecursion.bubo.utils.DBUtils;
import au.com.infiniterecursion.bubo.utils.DatabaseHelper;
import au.com.infiniterecursion.bubo.utils.PublishingUtils;

/*
 * RoboticEye Library Activity 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class LibraryActivity extends ListActivity implements RoboticEyeActivity {

	// Database
	DBUtils dbutils;
	private String[] video_absolutepath;
	private String[] video_filename;
	private Integer[] video_ids;
	
	
	private boolean videos_available;

	private static final int MENU_ITEM_1 = Menu.FIRST;
	private static final int MENU_ITEM_2 = MENU_ITEM_1 + 1;
	private static final int MENU_ITEM_3 = MENU_ITEM_2 + 1;
	private static final int MENU_ITEM_4 = MENU_ITEM_3 + 1;
	private static final int MENU_ITEM_5 = MENU_ITEM_4 + 1;
	private static final int MENU_ITEM_6 = MENU_ITEM_5 + 1;
	private static final int MENU_ITEM_7 = MENU_ITEM_6 + 1;
	private static final int MENU_ITEM_8 = MENU_ITEM_7 + 1;
	private static final int MENU_ITEM_9 = MENU_ITEM_8 + 1;
	private static final int MENU_ITEM_10 = MENU_ITEM_9 + 1;
	
	
	private LoginButton lb;
	private AlertDialog fb_dialog;
	
	private static final String TAG = "RoboticEye-Library";
	private static final int NOTIFICATION_ID = 2;
	private PublishingUtils pu;

	private Handler handler;
	private String emailPreference;
	private SimpleCursorAdapter listAdapter;
	private Cursor libraryCursor;
	
	private Thread thread_vb;
	private Thread thread_fb;
	private Thread thread_ftp;
	private Thread thread_youtube;
	
	private BuboApp mainapp;
	
	private String movieurl;
	private String moviefilename;
	private long sdrecord_id;
	
	private SharedPreferences prefs;
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		emailPreference = prefs.getString("emailPreference", null);

		Log.d(TAG, " onCreate ");
		dbutils = new DBUtils(getBaseContext());
		pu = new PublishingUtils(getResources(), dbutils);
		handler = new Handler();
		thread_vb = null;
		thread_fb = null;
		thread_ftp = null;
		thread_youtube = null;
	}

	public void onResume() {
		super.onResume();
		mainapp = (BuboApp) getApplication();
		Log.d(TAG, " onResume ");
		setContentView(R.layout.library_layout);
		
		makeCursorAndAdapter();

		registerForContextMenu(getListView());

		lb = new LoginButton(this);
		lb.init(mainapp.getFacebook(), BuboApp.FB_LOGIN_PERMISSIONS, this);
	}

	private void makeCursorAndAdapter() {
		dbutils.genericWriteOpen();

		// This query is for videofiles only, no joins.
		
		//libraryCursor = dbutils.generic_write_db.query(
		//		DatabaseHelper.SDFILERECORD_TABLE_NAME, null, null, null, null,
			//	null, DatabaseHelper.SDFileRecord.DEFAULT_SORT_ORDER);

		//SELECT 
		String join_sql = " SELECT a.filename as filename , a.filepath as filepath, a.length_secs as length_secs , a.created_datetime as created_datetime, a._id as _id , " 
						+ " b.host_uri as host_uri , b.host_video_url as host_video_url FROM " 
						+ " videofiles a LEFT OUTER JOIN hosts b ON "
						+ " a._id = b.sdrecord_id "
						+ " ORDER BY a.created_datetime DESC ";
		libraryCursor = dbutils.generic_write_db.rawQuery(join_sql, null);
		
		if (libraryCursor.moveToFirst()) {
			ArrayList<Integer> video_ids_al = new ArrayList<Integer>();
			ArrayList<String> video_paths_al = new ArrayList<String>();
			ArrayList<String> video_filenames_al = new ArrayList<String>();
			do {
				long video_id = libraryCursor
						.getLong(libraryCursor
								.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord._ID));
				video_ids_al.add((int) video_id);
				String video_path = libraryCursor
						.getString(libraryCursor
								.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.FILEPATH));
				video_paths_al.add(video_path);
				String video_filename = libraryCursor
				.getString(libraryCursor
						.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.FILENAME));
				video_filenames_al.add(video_filename);
		

			} while (libraryCursor.moveToNext());

			video_ids = video_ids_al.toArray(new Integer[video_ids_al.size()]);
			video_absolutepath = video_paths_al
					.toArray(new String[video_paths_al.size()]);
			video_filename = video_filenames_al.toArray(new String[video_filenames_al.size()]);
			videos_available = true;

		} else {

			videos_available = false;

		}
		

		//Make Cursor Adapter

		String[] from = new String[] { DatabaseHelper.SDFileRecord.FILENAME,
				DatabaseHelper.SDFileRecord.LENGTH_SECS,
				DatabaseHelper.SDFileRecord.CREATED_DATETIME, DatabaseHelper.HostDetails.HOST_VIDEO_URL };
		int[] to = new int[] { android.R.id.text1, android.R.id.text2,
				R.id.text3, R.id.text4 };
		listAdapter = new SimpleCursorAdapter(this,
				R.layout.library_list_item, libraryCursor, from, to);

		listAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				//Transform the text3 specifically, from time in millis to text repr.
				if (columnIndex == cursor
						.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.CREATED_DATETIME)) {
					long time_in_mills = cursor.getLong(cursor
							.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.CREATED_DATETIME));
					TextView datetime = (TextView) view
							.findViewById(R.id.text3);
					datetime.setText(PublishingUtils.showDate(time_in_mills));
					return true;
				}
				return false;
			}
		});

		setListAdapter(listAdapter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, " onDestroy ");
		libraryCursor.close();
		dbutils.close();
	}
	
	@Override
	public void onPause() {
		
		super.onDestroy();
		Log.d(TAG,"On pause");
		
		if (thread_vb != null) {
			Log.d(TAG,"Interrupting videobin thread");
			thread_vb.interrupt();
		}
		if (thread_fb != null) {
			Log.d(TAG,"Interrupting facebook thread");
			thread_fb.interrupt();
		}
		if (thread_ftp != null) {
			Log.d(TAG,"Interrupting ftp thread");
			thread_ftp.interrupt();
		}
		if (thread_youtube != null) {
			Log.d(TAG,"Interrupting youtube thread");
			thread_youtube.interrupt();
		}
		if (fb_dialog != null && fb_dialog.isShowing()) {
			Log.d(TAG, "Dismissing fb dialog");
			fb_dialog.dismiss();
			lb = null;
		}
		
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, final int position,
			long id) {
		super.onListItemClick(l, v, position, id);
		
		//play this selection.
		String movieurl = video_absolutepath[(int) position];
		Log.d(TAG, " operation on " + movieurl);
		
		pu.launchVideoPlayer(this, movieurl);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {

		menu.add(0, MENU_ITEM_1, 0, R.string.library_menu_play);

		menu.add(0, MENU_ITEM_8, 0, R.string.rename_video);
		
		menu.add(0, MENU_ITEM_9, 0, R.string.add_description);
		
		menu.add(0, MENU_ITEM_3, 0, R.string.menu_publish_to_videobin);
		
		menu.add(0, MENU_ITEM_5, 0, R.string.menu_publish_to_facebook);
		
		menu.add(0, MENU_ITEM_6, 0, R.string.menu_ftp);
		
		menu.add(0, MENU_ITEM_7, 0, R.string.menu_youtube);
		
		menu.add(0, MENU_ITEM_4, 0, R.string.menu_send_via_email);
		
		menu.add(0, MENU_ITEM_10, 0, R.string.menu_send_hosted_url_via_email);
		
		menu.add(0, MENU_ITEM_2, 0, R.string.library_menu_delete);
		

	}

	public boolean onContextItemSelected(MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Log.d(TAG, " got " + item.getItemId() + " at position " + info.position);
		
		
		if (!videos_available) {
			return true;
		}

		movieurl = video_absolutepath[info.position];
		sdrecord_id = video_ids[info.position];
		moviefilename = video_filename[info.position];
		Log.d(TAG, " operation on " + movieurl + " id " + sdrecord_id + " filename " + moviefilename);

		switch (item.getItemId()) {

		case MENU_ITEM_1:
			// play
			pu.launchVideoPlayer(this, movieurl);
			break;

		case MENU_ITEM_2:
			// delete

			//XXX ask if sure they want to delete ?
			
			// deleting files,
			if (!pu.deleteVideo(movieurl)) {
				Log.w(TAG, "Cant delete file " + movieurl);
				
			}
			// and removing DB records!
			if (dbutils.deleteSDFileRecord(sdrecord_id) == -1) {
				Log.w(TAG, "Cant delete record " + sdrecord_id);
			}
			
			reloadList();

			
			break;

		case MENU_ITEM_3:
			// publish to video bin
			// grab thread
			thread_vb = pu.videoUploadToVideoBin(this, handler, movieurl, emailPreference, sdrecord_id);
			break;

		case MENU_ITEM_4:
			// email
			pu.launchEmailIntentWithCurrentVideo(this, movieurl);
			break;

		case MENU_ITEM_5:
			// facebook
			if (fb_dialog != null ) {
				Log.d(TAG, "Dismissing fb dialog");
				fb_dialog.dismiss();
				lb = null;
				lb = new LoginButton(this);
				lb.init(mainapp.getFacebook(), BuboApp.FB_LOGIN_PERMISSIONS, this);
			}
			
			askFacebookLogin();
			break;
			
		case MENU_ITEM_6:
			// facebook
			thread_ftp = pu.videoUploadToFTPserver(this, handler, moviefilename, movieurl, sdrecord_id);
			
			break;
			
		case MENU_ITEM_7:
			// youtube
			// XXX ask for title and description
			String title="Experimental Bubo YouTube post";
			String description=getString(R.string.uploaded_by_bubo_your_wandering_eye_http_bubovideo_info_);
			// This launches the youtube upload process
			
			
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
				pu.getYouTubeAuthTokenWithPermissionAndUpload(this, possibleEmail, movieurl, handler, sdrecord_id);
			} else {
				
				//XXX throw up dialog
				
			}
			break;
		}

		return true;

	}

	private void reloadList() {
		//Refresh the list view
		runOnUiThread(new Runnable() {
		    public void run() {
		    	
		    	makeCursorAndAdapter();
				
				listAdapter.notifyDataSetChanged();

		    }
		});
	}


	public boolean isUploading() {
		return mainapp.isUploading() ;
	}


	public void startedUploading() {
		// Show notification of uploading
		Resources res = getResources();
		this.createNotification(res.getString(R.string.starting_upload) + " " + moviefilename);
		
		mainapp.setUploading();
	}


	public void finishedUploading(boolean success) {
		// Reload the gallery list
		reloadList();
		
		mainapp.setNotUploading();
	}
	
	public void createNotification(String notification_text) {
		Resources res = getResources();
		 CharSequence contentTitle = res.getString(R.string.bubo_notification_title);
		 CharSequence contentText = notification_text;

		 final Notification notifyDetails =
		        new Notification(R.drawable.icon, notification_text, System.currentTimeMillis());

		 Intent notifyIntent = new Intent(this, LibraryActivity.class);

		    PendingIntent intent =
		          PendingIntent.getActivity(this, 0,
		          notifyIntent,  PendingIntent.FLAG_UPDATE_CURRENT | Notification.FLAG_AUTO_CANCEL);

		    notifyDetails.setLatestEventInfo(this, contentTitle, contentText, intent);
		    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		    
		   mNotificationManager.notify(NOTIFICATION_ID, notifyDetails);
		
	}
	
	private void askFacebookLogin() {
		
		fb_dialog = new AlertDialog.Builder(this)
				.setMessage(R.string.request_facebook_login)
				//XXX Add in title and description text boxes
				.setView(lb)
				.setPositiveButton(R.string.videopost_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// If they succeeded in login, then the
								// session will be valid.
								
								if (mainapp.getFacebook()
										.isSessionValid()) {
									// we have a valid session.
	
									thread_fb = pu.videoUploadToFacebook(LibraryActivity.this, handler, mainapp.getFacebook(), movieurl, "Test Video", getString(R.string.uploaded_by_bubo_your_wandering_eye_http_bubovideo_info_), sdrecord_id);
										
									
								} 

							}
						})
				
				.setNegativeButton(R.string.videopost_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							

								// logout of facebook, if the session is
								// valid.
								if (mainapp.getFacebook().isSessionValid()) {
									lb.logout();
								}

							}
						})

				.show();
		
		
	}

}
