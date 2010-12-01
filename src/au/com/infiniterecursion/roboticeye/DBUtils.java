package au.com.infiniterecursion.roboticeye;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/*
 * Database utilities 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class DBUtils {
	private static final String TAG = "RoboticEye-DBUtils";
	private DatabaseHelper dbHelper;
	public SQLiteDatabase generic_write_db = null;

	private Context context;

	DBUtils(Context c) {
		context = c;
	}

	private void getOpenHelper() {
		if (dbHelper == null) {
			dbHelper = new DatabaseHelper(context);
			Log.v(TAG, "getOpenHelper called and started a new one");
		}
	}

	public void genericWriteOpen() {
		if (dbHelper == null)
			getOpenHelper();

		if (generic_write_db == null) {
			generic_write_db = dbHelper.getWritableDatabase();
			generic_write_db.setLockingEnabled(true);
			Log.v(TAG, "genericWriteOpen allocated db ");
		}

	}

	public void close() {

		if (generic_write_db != null) {
			generic_write_db.close();
			generic_write_db = null;
		}

	}

	/*
	 * Returns the next filename number to use in naming the recorded videofile
	 * 
	 * -1 is error
	 */
	public int getNextFilenameNumberAndIncrement() {

		genericWriteOpen();

		Cursor next_filename_number_cursor = generic_write_db.query(
				DatabaseHelper.FILENAME_TABLE_NAME, null, null, null, null,
				null, null);

		if (next_filename_number_cursor.moveToFirst()) {
			int column_index = next_filename_number_cursor
					.getColumnIndexOrThrow(DatabaseHelper.FilenameDetails.NEXT_FILENAME_NUMBER);
			int next_number = next_filename_number_cursor.getInt(column_index);

			int next_number_in_db = next_number + 1;
			// Increment the number
			String incr_sql = "UPDATE " + DatabaseHelper.FILENAME_TABLE_NAME
					+ " SET "
					+ DatabaseHelper.FilenameDetails.NEXT_FILENAME_NUMBER
					+ " = " + next_number_in_db;
			generic_write_db.execSQL(incr_sql);

			next_filename_number_cursor.close();
			close();

			return next_number;
		}

		// ERROR
		return -1;

	}

	/*
	 * Returns the ID of the new record, or -1 if error
	 */
	public long createSDFileRecordwithNewVideoRecording(String filepath,
			String filename, int duration, String video_audio_codecstr) {
		genericWriteOpen();

		ContentValues vals = new ContentValues();
		vals.put(DatabaseHelper.SDFileRecord.FILENAME, filename);
		vals.put(DatabaseHelper.SDFileRecord.FILEPATH, filepath);
		vals.put(DatabaseHelper.SDFileRecord.LENGTH_SECS, duration);
		vals.put(DatabaseHelper.SDFileRecord.VIDEO_AUDIO_CODEC_STRING,
				video_audio_codecstr);
		vals.put(DatabaseHelper.SDFileRecord.CREATED_DATETIME,
				(Long) System.currentTimeMillis());

		long rez = generic_write_db.insert(
				DatabaseHelper.SDFILERECORD_TABLE_NAME,
				DatabaseHelper.SDFileRecord.FILENAME, vals);

		close();

		return rez;
	}
	
	/*
	 * Returns the ID of the new record, or -1 if error
	 */
	public long creatHostDetailRecordwithNewVideoUploaded(long sdrecord_id,
			String host_uri, String hosted_video_url, String params) {
		genericWriteOpen();

		ContentValues vals = new ContentValues();
		vals.put(DatabaseHelper.HostDetails.HOST_SDRECORD_ID, sdrecord_id);
		vals.put(DatabaseHelper.HostDetails.HOST_URI, host_uri);
		vals.put(DatabaseHelper.HostDetails.HOST_VIDEO_URL, hosted_video_url);
		vals.put(DatabaseHelper.HostDetails.HOST_PARAMS, params);

		long rez = generic_write_db.insert(
				DatabaseHelper.HOST_TABLE_NAME,
				DatabaseHelper.HostDetails.HOST_URI, vals);

		close();

		return rez;
	}
	
	
	/**
	 * Delete the db record, and all linked entries
	 * 
	 * @return -1 if error, else 0
	 */
	public long deleteSDFileRecord(long recordid) {
		
		if (recordid > 0) {
			genericWriteOpen();
			String args[] = new String[] { Long.toString(recordid) };
			long rez = generic_write_db.delete(DatabaseHelper.SDFILERECORD_TABLE_NAME, " " + DatabaseHelper.SDFileRecord._ID + " = ?", args);
			
			//XXX Delete host records also.
			
			close();
			return rez;
		}
		
		return -1;
		
	}
	
}
