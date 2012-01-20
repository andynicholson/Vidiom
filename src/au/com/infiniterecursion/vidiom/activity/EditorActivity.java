package au.com.infiniterecursion.vidiom.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.infiniterecursion.vidiom.R;


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
	int levelquantum = 3;
	int countlevel = 4;
	int start_selection = 0;
	int end_selection = (levelquantum * countlevel) - 1;

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

		thumber = new MediaMetadataRetriever();
		try {
			thumber.setDataSource(filepath);

		} catch (Exception e) {
			e.printStackTrace();
		}

		String duration = thumber
				.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		Log.d(TAG, "Filepath has duration " + duration);
		duration_millis = Long.parseLong(duration);

		grid_main.setSelected(true);
		// grid_main.setSelection(0);

		grid_main.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				int position_position = ((adapterVideoThumbs.getCount() -1) / 2) ;

				int dist_to_start = Math.abs(position - start_selection); 
				int dist_to_end = Math.abs(end_selection - position);
				
				if (dist_to_end > dist_to_start) {
					start_selection = position;
					adapterVideoThumbs.notifyDataSetChanged();
					return;
				}
				
				if (dist_to_end < dist_to_start) {
					end_selection = position;
					adapterVideoThumbs.notifyDataSetChanged();
					return;
				}
				
				// Is an start selection?
				if (position <= position_position) {
					start_selection = position;
					adapterVideoThumbs.notifyDataSetChanged();
					return;
				}

				// Is an end selection?
				if (position > position_position) {

					end_selection = position;
					adapterVideoThumbs.notifyDataSetChanged();
					return;
				}

			}
		});

	}

	public void resetSelections() {
		start_selection = 0;
		end_selection = adapterVideoThumbs.getCount() - 1;
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
		MenuItem menu_zoom_in = menu.add(0, MENU_ITEM_1, 0, R.string.menu_zoom);
		//
		// menu_zoom_in.setIcon(R.drawable.wizard48);

		MenuItem menu_zoom_out = menu.add(0, MENU_ITEM_2, 0,
				R.string.menu_unzoom);
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
			adapterVideoThumbs.zoom();
			resetSelections();
			adapterVideoThumbs.notifyDataSetChanged();
			break;

		//
		case MENU_ITEM_2:
			adapterVideoThumbs.unzoom();
			resetSelections();
			adapterVideoThumbs.notifyDataSetChanged();
			break;

		//
		case MENU_ITEM_3:

			break;

		}
		return true;
	}

	public class ImageAdapter extends BaseAdapter {
		Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			// ALWAYS a multiple of
			return levelquantum * countlevel;

		}

		public void reset_zoom() {
			countlevel = 4;

		}

		public void zoom() {
			countlevel++;
			// shouldn't be able to go above number of actual frames...
			//
			// XXX
		}

		public void unzoom() {
			if (countlevel > 0) {
				countlevel--;
			}

		}

		public View getView(int position, View convertView, ViewGroup parent) {
			//
			// TimeUs (in microseconds) is

			// duration_millis * 1000 * position/ getCount()
			long duration_micros = duration_millis * 1000;

			long timeUs = duration_micros * position / getCount();
			String time_seconds = String.valueOf(timeUs / 1000000.0);
			View v;

			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				v = li.inflate(R.layout.thumbnail_gridview, null);

			} else {
				v = convertView;

			}

			TextView tv = (TextView) v.findViewById(R.id.icon_text);
			tv.setText("Time " + time_seconds);
			ImageView iv = (ImageView) v.findViewById(R.id.icon_image);

			Log.v(TAG, " Thumb at " + timeUs);
			Bitmap outThumbnail = thumber.getFrameAtTime(timeUs);

			iv.setImageBitmap(outThumbnail);

			if (position == start_selection) {
				TextView tv2 = (TextView) v.findViewById(R.id.icon_text_two);
				tv2.setText("Start of Trim");
			}
			else if (position == end_selection) {
				TextView tv2 = (TextView) v.findViewById(R.id.icon_text_two);
				tv2.setText("End of Trim");
			} else {
				TextView tv2 = (TextView) v.findViewById(R.id.icon_text_two);
				tv2.setText("");
			}
			

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
	}
}
