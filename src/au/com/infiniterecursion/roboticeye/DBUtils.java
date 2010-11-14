package au.com.infiniterecursion.roboticeye;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBUtils {
	private static final String TAG = "RoboticEye-DBUtils";
	private DatabaseHelper dbHelper;
	public SQLiteDatabase generic_write_db = null;
	
	private Context context;
	
	DBUtils(Context c) {
		context=c;
	}
	
	private void getOpenHelper() {
		if (dbHelper == null) {
			dbHelper = new DatabaseHelper(context);
			Log.v(TAG, "***  OPENHELPER() called");
		}
	}

	
	public void genericWriteOpen() {
		if (dbHelper == null)
			getOpenHelper();

		if (generic_write_db == null) {
			generic_write_db = dbHelper.getWritableDatabase();

			generic_write_db.setLockingEnabled(true);
			Log.v(TAG, "***  WRITEOPEN() allocated db " 
					+ generic_write_db.hashCode() 
					);
		}

	}
	
	public void close() {
		
		if (generic_write_db != null) {
			generic_write_db.close();
		}
		
	}
}
