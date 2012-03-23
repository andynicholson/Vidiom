package au.com.infiniterecursion.vidiom.activity;

import java.io.File;
import java.util.concurrent.TimeUnit;

import uk.co.halfninja.videokit.Videokit;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.com.infiniterecursion.vidiom.utils.DBUtils;
import au.com.infiniterecursion.vidiom.utils.RangeSeekBar;
import au.com.infiniterecursion.vidiom.utils.RangeSeekBar.OnRangeSeekBarChangeListener;
import au.com.infiniterecursion.vidiompro.R;

/*
 * Vidiom Editor Activity 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2012
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class EditorActivity extends Activity {
	private static final String TAG = "RoboticEye-EditorActivity";

	private static final int MENU_ITEM_1 = 0;

	private static final int MENU_ITEM_2 = 1;

	private static final int MENU_ITEM_3 = 2;

	GridView grid_main;
	MediaMetadataRetriever thumber;
	String filepath;
	long duration_millis;
	ImageAdapter adapterVideoThumbs;

	int number_of_thumbnails = 6;

	int start_selection = 0;
	int end_selection = 100;

	private long duration_micros;

	Videokit vk = new Videokit();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gridview_editor);

		grid_main = (GridView) findViewById(R.id.GridView01);
		adapterVideoThumbs = new ImageAdapter(this);
		grid_main.setAdapter(adapterVideoThumbs);

		filepath = this.getIntent().getExtras()
				.getString(getString(R.string.EditorActivityFilenameKey));
		Log.d(TAG, " Editing " + filepath);

		final RangeSeekBar<Integer> seekBar = new RangeSeekBar<Integer>(0, 100,
				getBaseContext());
		seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
			@Override
			public void rangeSeekBarValuesChanged(Integer minValue,
					Integer maxValue) {
				// handle changed range values
				Log.i(TAG, "User selected new range values: MIN=" + minValue
						+ ", MAX=" + maxValue);

				start_selection = minValue;
				end_selection = maxValue;

				adapterVideoThumbs.notifyDataSetChanged();
			}
		});

		// add RangeSeekBar to pre-defined layout
		final LinearLayout layout = (LinearLayout) findViewById(R.id.editorLayout);
		layout.addView(seekBar);

		ViewTreeObserver vto = layout.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				// adjust size of editor to seek bar height.
				int height_editor = layout.getHeight() - seekBar.getHeight();
				Log.d(TAG, " editor height is " + grid_main.getHeight()
						+ " layout height " + layout.getHeight() + " seekBar "
						+ seekBar.getHeight());
				Log.d(TAG, " editor height becoming " + height_editor);

				grid_main.setLayoutParams(new LinearLayout.LayoutParams(
						grid_main.getWidth(), height_editor));

				// remove, so it works once only.
				ViewTreeObserver obs = layout.getViewTreeObserver();
				obs.removeGlobalOnLayoutListener(this);
			}
		});

		// API level 10 code here
		// protect from devices that dont support this !
		// Currently done by LibraryActivity not invoking us if we dont support
		// this level.

		thumber = new MediaMetadataRetriever();
		try {
			thumber.setDataSource(filepath);

		} catch (Exception e) {
			e.printStackTrace();
		}

		String duration = thumber
				.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		Log.d(TAG, "Filepath has duration " + duration);
		try {

			duration_millis = Long.parseLong(duration);
		} catch (NumberFormatException npe) {
			npe.printStackTrace();
			duration_millis = 0;
		}

		duration_micros = duration_millis * 1000;

		grid_main.setSelected(true);

		grid_main.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				// nothing.
			}
		});

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		Log.i(TAG, "OnPrepareOptionsMenu called");

		menu.clear();

		addConstantMenuItems(menu);

		return true;
	}

	private void addConstantMenuItems(Menu menu) {
		// ALWAYS ON menu items.
		// MenuItem menu_zoom_in = menu.add(0, MENU_ITEM_1, 0,
		// R.string.menu_zoom);
		//
		// menu_zoom_in.setIcon(R.drawable.wizard48);

		// MenuItem menu_zoom_out = menu.add(0, MENU_ITEM_2, 0,
		// R.string.menu_unzoom);
		//
		// menu_zoom_out.setIcon(R.drawable.wizard48);

		MenuItem menu_trim = menu.add(0, MENU_ITEM_3, 0, R.string.menu_trim);
		//
		// menu_trim.setIcon(R.drawable.wizard48);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuitem) {
		int menuNum = menuitem.getItemId();

		Log.d("MENU", "Option " + menuNum + " selected");

		switch (menuitem.getItemId()) {

		//
		case MENU_ITEM_1:

			break;

		//
		case MENU_ITEM_2:

			break;

		//
		case MENU_ITEM_3:

			trimVideo();

			break;

		}
		return true;
	}

	private void trimVideo() {
		// Lets trim the video

		Log.v(TAG, " Starting to trim video");

		Log.i("Test", "Let's set input to " + filepath);
		//
		// RENAME TO 3GP - ffmpeg wont use the amrnb codec inside MP4
		String output = filepath.replace(".mp4", "-transcoded.3gp");
		Log.i("Test", "Let's set output to " + output);

		// Starting position, offset in microseconds.
		long offset = (long) (start_selection / 100.0 * duration_micros);

		// TimeUnit.MICROSECONDS.toMinutes(offset), >= API 9
		long rounded_to_mins = (long) (TimeUnit.MICROSECONDS.toSeconds(offset) / 60.0);
		long rounded_to_mins_in_seconds = rounded_to_mins * 60;

		// NOTA BELLA: HOURS set to ZERO!
		String offsetstr = String.format(
				"00:%02d:%02d.%03d",
				rounded_to_mins,
				TimeUnit.MICROSECONDS.toSeconds(offset)
						- rounded_to_mins_in_seconds,
				TimeUnit.MICROSECONDS.toMillis(offset)
						- TimeUnit.SECONDS.toMillis(TimeUnit.MICROSECONDS
								.toSeconds(offset)));

		// Duration from starting position
		long duration = (long) ((end_selection - start_selection) / 100.0 * duration_micros);

		long rounded_to_mins_duration = (long) (TimeUnit.MICROSECONDS
				.toSeconds(duration) / 60.0); // TimeUnit.MICROSECONDS.toMinutes(duration);
												// >= API 9
		long rounded_to_mins_duration_in_seconds = rounded_to_mins_duration * 60;

		// NOTA BELLA : HOURS set to ZERO
		String durationstr = String.format(
				"00:%02d:%02d.%03d",
				rounded_to_mins_duration,
				TimeUnit.MICROSECONDS.toSeconds(duration)
						- rounded_to_mins_duration_in_seconds,
				TimeUnit.MICROSECONDS.toMillis(duration)
						- TimeUnit.SECONDS.toMillis(TimeUnit.MICROSECONDS
								.toSeconds(duration)));

		Log.d(TAG, " skip to " + offsetstr + " duration " + durationstr);

		// INVOKE FFMPEG ;-D
		vk.run(new String[] { "ffmpeg", "-i", filepath, "-ss", offsetstr, "-t",
				durationstr, "-vcodec", "copy", "-acodec", "copy", "-y", output });

		// Check file size
		File new_file = new File(output);
		// Check the new transcoded copy
		if (new_file.exists() && new_file.length() > 0) {

			DBUtils db_utils = new DBUtils(getBaseContext());
			// Add this to our library!
			long latestsdrecord_id = db_utils
					.createSDFileRecordwithNewVideoRecording(output,
							new_file.getName(), (int) (duration / 1000000),
							// XXX hardcoded vid & audio codecs
							"h263;amr-nb", "Untitled Copy",
							"Transcoded version");

			// If ID > 0, then new record in DB was successfully created
			if (latestsdrecord_id > 0) {

				Log.d(TAG,
						"Valid DB Record - made transcoded video file - sdrecord id  is "
								+ latestsdrecord_id);

				// Send the info to the inbuilt Android Media Scanner

				// Save the name and description of a video in a
				// ContentValues map.
				ContentValues values = new ContentValues(2);
				values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
				values.put(MediaStore.Video.Media.DATA, output);

				// Add a new record (identified by uri), but with the values
				// just set.
				Uri uri = getContentResolver().insert(
						MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
						uri));

				// Video recording finished dialog!
				new AlertDialog.Builder(this)
						.setMessage(
								getResources().getString(
										R.string.file_transcoded)
										+ " "
										+ new_file.getName()
										+ '\n'
										+ getResources().getString(
												R.string.transcoded_in_gallery))
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {

										// Finish at this point.
										finish();

									}
								}).show();
			}

		} else {

			// sorry!
			AlertDialog ffmpeg_zero_out = new AlertDialog.Builder(this)
					.setMessage("Sorry! Transcoding failed.")
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									// Finish at this point.
									finish();

								}
							}).show();

		}

	}

	public class ImageAdapter extends BaseAdapter {
		Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			//
			// TimeUs (in microseconds) is

			// duration_millis * 1000 * position/ getCount()

			// Based on a modified version of duration_micros
			// depending on start_selection, end_selection

			// offset = start_selection * duration_micros / 100
			// +
			// position * (end_start_selection-start_selection)/100 *
			// duration_micros / getCount()

			long offset = (long) (start_selection / 100.0 * duration_micros);
			long position_val = (long) (position
					* (end_selection - start_selection) / 100.0
					* duration_micros / getCount());
			// final timeUs for the wanted thumbnail
			long timeUs = offset + position_val;

			String time_seconds = String.valueOf(timeUs / 1000000.0);
			View v;

			Log.v(TAG, " position " + position + " : " + end_selection + " to "
					+ start_selection + " offset " + offset + " position_val "
					+ position_val);

			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				v = li.inflate(R.layout.thumbnail_gridview, null);

			} else {
				v = convertView;

			}

			// set total layout (row) width and height
			// v.setLayoutParams(new LinearLayout.LayoutParams(v.getWidth(),
			// 40));

			TextView tv = (TextView) v.findViewById(R.id.icon_text);
			tv.setText("Time " + time_seconds);
			ImageView iv = (ImageView) v.findViewById(R.id.icon_image);

			Log.v(TAG, " Thumb at " + timeUs);

			// API level 10 code here
			// protect from devices that dont support this !
			// Currently done by LibraryActivity not invoking us if we dont
			// support this level.
			Bitmap outThumbnail = null;
			try {
				outThumbnail = thumber.getFrameAtTime(timeUs,
						MediaMetadataRetriever.OPTION_NEXT_SYNC);
			} catch (NoSuchMethodError nsm) {
				// nsm.printStackTrace();
				// Log.e(TAG, " Probably have Android 2.2 ");
			}

			if (outThumbnail != null) {
				Log.v(TAG, " bitmap size is " + outThumbnail.getWidth() + "x"
						+ outThumbnail.getHeight());
				iv.setImageBitmap(outThumbnail);

			} else {
				iv.setImageResource(R.drawable.icon);
			}
			Log.v(TAG,
					" imageview size is " + iv.getWidth() + "x"
							+ iv.getHeight());

			/*
			 * if (position == start_selection) { TextView tv2 = (TextView)
			 * v.findViewById(R.id.icon_text_two); tv2.setText("Start of Trim");
			 * } else if (position == end_selection) { TextView tv2 = (TextView)
			 * v.findViewById(R.id.icon_text_two); tv2.setText("End of Trim"); }
			 * else { TextView tv2 = (TextView)
			 * v.findViewById(R.id.icon_text_two); tv2.setText(""); }
			 */

			return v;
		}

		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getCount() {
			// number of views ie thumbnails
			return number_of_thumbnails;
		}
	}
}
