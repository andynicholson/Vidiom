package au.com.infiniterecursion.roboticeye;

import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LibraryActivity extends Activity {

	//Database
	DBUtils dbutils;
	private String[] lv_arr;

	 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dbutils = new DBUtils(getBaseContext());		
		dbutils.genericWriteOpen();
			
		Cursor c = dbutils.generic_write_db.query(DatabaseHelper.SDFILERECORD_TABLE_NAME, null, null, null, null, null, DatabaseHelper.SDFileRecord.DEFAULT_SORT_ORDER);
	
		if (c.moveToFirst())
	    {
	        ArrayList<String> strings = new ArrayList<String>();
	       do {                    
	          String mC = c.getString( c.getColumnIndexOrThrow(DatabaseHelper.SDFileRecord.FILENAME));
	          strings.add(mC);  

	       } while (c.moveToNext());
	       
	       lv_arr = strings.toArray(new String[strings.size()]);
	    }
		
		dbutils.close();
		
		ListView CView = new ListView(this);
	    CView.setAdapter(new ArrayAdapter<String>(this,
	            android.R.layout.simple_list_item_2, lv_arr));      
	    setContentView(CView);
	    
	}
}
