package au.com.infiniterecursion.roboticeye;

import java.util.ArrayList;

import android.app.ListActivity;
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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


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

	//Database
	DBUtils dbutils;
	private String[] video_names;
	private String[] video_absolutepath;
	
	private boolean videos_available;
	
	private static final int MENU_ITEM_1 = Menu.FIRST;
	private static final int MENU_ITEM_2 = MENU_ITEM_1 + 1;
	private static final int MENU_ITEM_3 = MENU_ITEM_2 + 1;
	private static final int MENU_ITEM_4 = MENU_ITEM_3 + 1;
	
	private static final String TAG = "RoboticEye-Library";
	private PublishingUtils pu;
	
	private Handler handler;
	private String emailPreference;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences prefs = PreferenceManager
		.getDefaultSharedPreferences(getBaseContext());
		emailPreference = prefs.getString("emailPreference",null);
		
		pu = new PublishingUtils();
		dbutils = new DBUtils(getBaseContext());		
		handler = new Handler();
	}
	
	public void onResume() {
		super.onResume();
		
		
		dbutils.genericWriteOpen();
		
		Cursor c = dbutils.generic_write_db.query(DatabaseHelper.SDFILERECORD_TABLE_NAME, null, null, null, null, null, DatabaseHelper.SDFileRecord.DEFAULT_SORT_ORDER);
		startManagingCursor(c);
		
		
		Resources res = getResources();
		
		if (c.moveToFirst())
	    {
	        ArrayList<String> video_names_al = new ArrayList<String>();
	        ArrayList<String> video_paths_al = new ArrayList<String>();
	        
	       do {                    
	          String video_name = c.getString( c.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.FILENAME));
	          video_names_al.add(video_name); 
	          String video_path = c.getString( c.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.FILEPATH));
	          video_paths_al.add(video_path); 
	          

	       } while (c.moveToNext());
	       
	       video_names = video_names_al.toArray(new String[video_names_al.size()]);
	       video_absolutepath = video_paths_al.toArray(new String[video_paths_al.size()]);
	       videos_available = true;
	       
	    } else {
	    	String empty = res.getString(R.string.no_videos_in_library);
	    	video_names = new String[] { empty };
	    	videos_available = false;
	    	
	    }
		//c.close();
		dbutils.close();
		
		
		setContentView(R.layout.library_layout);
		
		/*
		setListAdapter(new ArrayAdapter<String>(this,
	            android.R.layout.simple_list_item_1, video_names)); 		    
	    */
		/*
		 ListAdapter adapter = new SimpleCursorAdapter(
                 this, // Context.
                 R.layout.library_list_item, 
                 c,                                              // Pass in the cursor to bind to.
                 new String[] {DatabaseHelper.SDFileRecord.FILENAME, DatabaseHelper.SDFileRecord.LENGTH_SECS, DatabaseHelper.SDFileRecord.CREATED_DATETIME},           // Array of cursor columns to bind to.
                 new int[] {android.R.id.text1, android.R.id.text2, R.id.text3});  // Parallel array of which template objects to bind to those columns.
	*/
		String[] from =  new String[] {DatabaseHelper.SDFileRecord.FILENAME, DatabaseHelper.SDFileRecord.LENGTH_SECS, DatabaseHelper.SDFileRecord.CREATED_DATETIME};
		int[] to = new int[] {android.R.id.text1, android.R.id.text2, R.id.text3};
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.library_list_item, c, from, to);
		
		
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
		    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		        if(columnIndex == cursor.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.CREATED_DATETIME)) {
		        		long time_in_mills = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.CREATED_DATETIME));
		        		TextView datetime = (TextView)view.findViewById(R.id.text3);
		        		datetime.setText(PublishingUtils.showDate(time_in_mills));
		                return true;
		        }
		        return false;
		    }
		});

		
		setListAdapter(adapter);
		
		registerForContextMenu(getListView());
	    
	    
		
	
		hideProgressIndicator();
		
	}
	
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		
		menu.add(0, MENU_ITEM_1, 0, R.string.library_menu_play);
		menu.add(0, MENU_ITEM_2, 0, R.string.library_menu_delete);
		menu.add(0, MENU_ITEM_3, 0, R.string.menu_publish_to_videobin);
		menu.add(0, MENU_ITEM_4, 0, R.string.menu_send_via_email);
			
	}
	
	public boolean  onContextItemSelected (MenuItem item) {
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		Log.d(TAG, " got " + item.getItemId() + " at position " + info.position);
		
		if (!videos_available) {
			return true;
		}
		
		String movieurl = video_absolutepath[info.position];
		Log.d(TAG, " operation on " + movieurl);
		
		
		switch(item.getItemId()) {
		
		case MENU_ITEM_1:
			//play
			pu.launchVideoPlayer(this, movieurl);				
			break;
		
		case MENU_ITEM_2:
			//delete
			
			//XXX deleting files, and removing DB records!
			
			break;

		case MENU_ITEM_3:
			//publish to video bin
			pu.doPOSTtoVideoBin(this, handler, movieurl, emailPreference);
			break;

		case MENU_ITEM_4:
			//email
			pu.launchEmailIntentWithCurrentVideo(this, movieurl);
			break;
			
		}
		
		return true;
		
		
	}

	public void showProgressIndicator() {
		// 
		findViewById(R.id.uploadprogresslibrary).setVisibility(View.VISIBLE);
	}

	public void hideProgressIndicator() {
		// 
		findViewById(R.id.uploadprogresslibrary).setVisibility(View.INVISIBLE);
	}
	
}
