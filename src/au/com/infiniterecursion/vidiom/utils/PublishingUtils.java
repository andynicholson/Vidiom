package au.com.infiniterecursion.vidiom.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import au.com.infiniterecursion.vidiom.R;
import au.com.infiniterecursion.vidiom.activity.VidiomActivity;
import au.com.infiniterecursion.vidiom.sslemail.SSLEmailSender;
import au.com.infiniterecursion.vidiom.utils.GoogleAuthoriser.AuthorizationListener;

import com.facebook.android.Facebook;
import com.facebook.android.Util;

/*
 * Vidiom Library Activity 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public class PublishingUtils {

	private static final String TAG = "RoboticEye-PublishingUtils";

	private File folder;
	private Resources res;
	private DBUtils dbutils;

	private static final String INITIAL_UPLOAD_URL = "http://uploads.gdata.youtube.com/resumable/feeds/api/users/default/uploads";
	private static final String DEFAULT_VIDEO_CATEGORY = "News";
	private static final String DEFAULT_VIDEO_TAGS = "mobile";
	private static final String YOUTUBE_PLAYER_URL = "http://www.youtube.com/watch?feature=player_profilepage&v=";

	private static final int MAX_RETRIES = 5;
	private static final int BACKOFF = 4; // base of exponential backoff

	private String clientLoginToken = null;
	private String youTubeName = null;

	private double currentFileSize = 0;
	private double totalBytesUploaded = 0;
	private int numberOfRetries = 0;
	private String tags = null;
	private GoogleAuthoriser authorizer;

	public PublishingUtils(Resources res, DBUtils dbutils) {

		this.res = res;
		this.dbutils = dbutils;
		folder = new File(Environment.getExternalStorageDirectory()
				+ res.getString(R.string.rootSDcardFolder));

	}

	public static String showDate(long timemillis) {

		if (timemillis <= 0)
			return "N/A";

		Calendar cal;
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss");
		cal = Calendar.getInstance();
		cal.setTimeInMillis(timemillis);
		return sdf.format(cal.getTime());

	}

	/*
	 * 
	 * Methods for publishing the video
	 */

	// videoUploadToFacebook modified from
	// http://code.google.com/p/stickman-android/source/browse/trunk/src/org/hackday/stickman/upload/FacebookHelper.java?spec=svn31&r=31
	// http://www.apache.org/licenses/LICENSE-2.0
	// 
	// Changed by Andy Nicholson
	//
	// Facebook uploading
	//
	public Thread videoUploadToFacebook(final Activity activity,
			final Handler handler, final Facebook mFacebook, final String path,
			final String title, final String description,
			final String emailAddress, final long sdrecord_id) {

		// Make the progress bar view visible.
		((VidiomActivity) activity).startedUploading();
		final Resources res = activity.getResources();

		Thread t = new Thread(new Runnable() {
			public void run() {
				// Do background task.
				// Track errors
				boolean failed = false;

				Log.i(TAG, "Upload starting");
				// Initialising REST API video.upload parameters
				Bundle params = new Bundle();

				params.putString("method", "facebook.video.upload");
				params.putString("format", "json");
				params.putString("title", title);
				params.putString("description", description);
				params.putString("call_id", String.valueOf(System
						.currentTimeMillis()));
				params.putString("v", "1.0");
				params.putString("oauth_token", mFacebook.getAccessToken());

				// Reading input file
				try {
					File videoFile = new File(path);
					byte[] data = null;
					try {

						// XXX
						// SPLIT THIS INTO 5K chunks!!
						// XXX

						data = new byte[(int) videoFile.length()];
					} catch (OutOfMemoryError e) {
						failed = true;
					}
					if (data != null) {
						InputStream is = new FileInputStream(videoFile);
						is.read(data);
						params.putByteArray(videoFile.getName(), data);
					}
				} catch (Exception ex) {
					Log.e(TAG, "Cannot read video file :", ex);
				}

				// Sending POST request to Facebook
				String response = null;
				String url = "https://api-video.facebook.com/restserver.php";

				try {
					if (!failed) {
						response = Util.openUrl(url, "POST", params);
					}
					// SessionEvents.onUploadComplete(response);
				} catch (FileNotFoundException e) {
					// SessionEvents.onFileNotFoundException(e);
					failed = true;
					e.printStackTrace();
				} catch (MalformedURLException e) {
					// SessionEvents.onMalformedURLException(e);
					failed = true;
					e.printStackTrace();
				} catch (IOException e) {
					// SessionEvents.onIOException(e);
					failed = true;
					e.printStackTrace();
				} catch (OutOfMemoryError e) {
					failed = true;
					e.printStackTrace();
				}

				if (failed) {
					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Indicate back to calling activity the result!
							// update uploadInProgress state also.

							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_facebook_failed_));
						}
					}, 0);

					return;
				}

				Log.i(TAG, "Uploading to facebook complete. Response is "
						+ response);

				// response is JSON
				JSONObject fb_response = null;
				// decode, and grab URL
				try {
					fb_response = (JSONObject) new JSONTokener(response)
							.nextValue();
				} catch (JSONException e) {
					//
					e.printStackTrace();
					fb_response = null;
				}
				String hosted_url = "facebook.com";

				if (fb_response != null) {

					try {
						hosted_url = fb_response.getString("link");
						Log.i(TAG, "Facebook hosted url is : " + hosted_url);
					} catch (JSONException e) {
						//
						e.printStackTrace();
						hosted_url = null;
					}

					if (hosted_url != null) {
						// Log record of this URL in POSTs table
						dbutils.creatHostDetailRecordwithNewVideoUploaded(
								sdrecord_id, url, hosted_url, "");

						// Use the handler to execute a Runnable on the
						// main thread in order to have access to the
						// UI elements.
						handler.postDelayed(new Runnable() {

							public void run() {
								// Update UI

								// Indicate back to calling activity the result!
								// update uploadInProgress state also.

								((VidiomActivity) activity)
										.finishedUploading(true);
								((VidiomActivity) activity)
										.createNotification(res
												.getString(R.string.upload_to_facebook_succeeded_));

							}
						}, 0);
					}

				} else {

					// an error -- fb_response is NULL.

					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Indicate back to calling activity the result!
							// update uploadInProgress state also.

							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_facebook_failed_));
						}
					}, 0);

				}

				if (emailAddress != null && fb_response != null
						&& hosted_url != null) {

					// EmailSender through IR controlled gmail system.
					SSLEmailSender sender = new SSLEmailSender(
							activity
									.getString(R.string.automatic_email_username),
							activity
									.getString(R.string.automatic_email_password)); // consider
					// this
					// public
					// knowledge.
					try {
						sender
								.sendMail(
										activity
												.getString(R.string.vidiom_automatic_email), // subject.getText().toString(),
										activity
												.getString(R.string.url_of_hosted_video_is_)
												+ " " + hosted_url, // body.getText().toString(),
										activity
												.getString(R.string.automatic_email_from), // from.getText().toString(),
										emailAddress // to.getText().toString()
								);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage(), e);
					}
				}

			}
		});

		t.start();

		return t;

	}

	public Thread videoUploadToVideoBin(final Activity activity,
			final Handler handler, final String video_absolutepath,
			final String title, final String description,
			final String emailAddress, final long sdrecord_id) {

		Log.d(TAG, "doPOSTtoVideoBin starting");

		// Make the progress bar view visible.
		((VidiomActivity) activity).startedUploading();
		final Resources res = activity.getResources();

		Thread t = new Thread(new Runnable() {
			public void run() {
				// Do background task.

				boolean failed = false;

				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(
						CoreProtocolPNames.PROTOCOL_VERSION,
						HttpVersion.HTTP_1_1);

				URI url = null;
				try {
					url = new URI(res.getString(R.string.http_videobin_org_add));
				} catch (URISyntaxException e) {
					// Ours is a fixed URL, so not likely to get here.
					e.printStackTrace();
					return;

				}
				HttpPost post = new HttpPost(url);
				MultipartEntity entity = new MultipartEntity(
						HttpMultipartMode.BROWSER_COMPATIBLE);

				File file = new File(video_absolutepath);
				entity.addPart(res.getString(R.string.video_bin_API_videofile),
						new FileBody(file));

				try {
					entity.addPart(res.getString(R.string.video_bin_API_api),
							new StringBody("1", "text/plain", Charset
									.forName("UTF-8")));

					// title
					entity.addPart(res.getString(R.string.video_bin_API_title),
							new StringBody(title, "text/plain", Charset
									.forName("UTF-8")));

					// description
					entity.addPart(res
							.getString(R.string.video_bin_API_description),
							new StringBody(description, "text/plain", Charset
									.forName("UTF-8")));

				} catch (IllegalCharsetNameException e) {
					// error
					e.printStackTrace();
					failed = true;

				} catch (UnsupportedCharsetException e) {
					// error
					e.printStackTrace();
					return;
				} catch (UnsupportedEncodingException e) {
					// error
					e.printStackTrace();
					failed = true;
				}

				post.setEntity(entity);

				// Here we go!
				String response = null;
				try {
					response = EntityUtils.toString(client.execute(post)
							.getEntity(), "UTF-8");
				} catch (ParseException e) {
					// error
					e.printStackTrace();
					failed = true;
				} catch (ClientProtocolException e) {
					// error
					e.printStackTrace();
					failed = true;
				} catch (IOException e) {
					// error
					e.printStackTrace();
					failed = true;
				}

				client.getConnectionManager().shutdown();

				if (failed) {
					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Indicate back to calling activity the result!
							// update uploadInProgress state also.

							((VidiomActivity) activity)
									.finishedUploading(false);

							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_videobin_org_failed_));
						}
					}, 0);

					return;
				}

				Log.d(TAG, " video bin got back " + response);

				// XXX Convert to preference for auto-email on videobin post
				// XXX ADD EMAIL NOTIF to all other upload methods
				// stuck on YES here, if email is defined.

				if (emailAddress != null && response != null) {

					// EmailSender through IR controlled gmail system.
					SSLEmailSender sender = new SSLEmailSender(
							activity
									.getString(R.string.automatic_email_username),
							activity
									.getString(R.string.automatic_email_password)); // consider
					// this
					// public
					// knowledge.
					try {
						sender
								.sendMail(
										activity
												.getString(R.string.vidiom_automatic_email), // subject.getText().toString(),
										activity
												.getString(R.string.url_of_hosted_video_is_)
												+ " " + response, // body.getText().toString(),
										activity
												.getString(R.string.automatic_email_from), // from.getText().toString(),
										emailAddress // to.getText().toString()
								);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage(), e);
					}
				}

				// Log record of this URL in POSTs table
				dbutils.creatHostDetailRecordwithNewVideoUploaded(sdrecord_id,
						res.getString(R.string.http_videobin_org_add),
						response, "");

				// Use the handler to execute a Runnable on the
				// main thread in order to have access to the
				// UI elements.
				handler.postDelayed(new Runnable() {
					public void run() {
						// Update UI

						// Indicate back to calling activity the result!
						// update uploadInProgress state also.

						((VidiomActivity) activity).finishedUploading(true);
						((VidiomActivity) activity)
								.createNotification(res
										.getString(R.string.upload_to_videobin_org_succeeded_));

					}
				}, 0);
			}
		});

		t.start();

		return t;

	}

	public Thread videoUploadToFTPserver(final Activity activity,
			final Handler handler, final String latestVideoFile_filename,
			final String latestVideoFile_absolutepath,
			final String emailAddress, final long sdrecord_id) {

		Log.d(TAG, "doVideoFTP starting");

		// Make the progress bar view visible.
		((VidiomActivity) activity).startedUploading();

		final Resources res = activity.getResources();

		Thread t = new Thread(new Runnable() {
			public void run() {
				// Do background task.
				// FTP; connect preferences here!
				//
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(activity.getBaseContext());
				String ftpHostName = prefs.getString(
						"defaultFTPhostPreference", null);
				String ftpUsername = prefs.getString(
						"defaultFTPusernamePreference", null);
				String ftpPassword = prefs.getString(
						"defaultFTPpasswordPreference", null);

				// use name of local file.
				String ftpRemoteFtpFilename = latestVideoFile_filename;

				// FTP
				FTPClient ftpClient = new FTPClient();
				InetAddress uploadhost = null;
				try {

					uploadhost = InetAddress.getByName(ftpHostName);
				} catch (UnknownHostException e1) {
					// If DNS resolution fails then abort immediately - show
					// dialog to
					// inform user first.
					e1.printStackTrace();
					Log.e(TAG, " got exception resolving " + ftpHostName
							+ " - video uploading failed.");
					uploadhost = null;
				}

				if (uploadhost == null) {

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

							new AlertDialog.Builder(activity)
									.setMessage(R.string.cant_find_upload_host)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											})

									.setNegativeButton(
											R.string.cancel,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											}).show();

						}
					}, 0);

					return;
				}

				boolean connected = false;

				try {
					ftpClient.connect(uploadhost);
					connected = true;

				} catch (SocketException e) {
					e.printStackTrace();
					connected = false;

				} catch (UnknownHostException e) {
					//
					e.printStackTrace();
					connected = false;
				} catch (IOException e) {
					//
					e.printStackTrace();
					connected = false;
				}

				if (!connected) {

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

							new AlertDialog.Builder(activity)
									.setMessage(R.string.cant_login_upload_host)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											})

									.setNegativeButton(
											R.string.cancel,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											}).show();

						}
					}, 0);

					return;
				}

				boolean reply = false;
				try {

					reply = ftpClient.login(ftpUsername, ftpPassword);
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log
							.e(TAG,
									" got exception on ftp.login - video uploading failed.");
				}

				// check the reply code here
				// If we cant login, abort after showing user a dialog.
				if (!reply) {
					try {
						ftpClient.disconnect();
					} catch (IOException e) {
						//
						e.printStackTrace();
					}

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

							new AlertDialog.Builder(activity)
									.setMessage(R.string.cant_login_upload_host)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											}).show();
						}
					}, 0);

					return;
				}

				// Set File type to binary
				try {
					ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				} catch (IOException e) {
					//
					e.printStackTrace();
					// keep going?!
				}

				// BEYOND HERE DONT USE DIALOGS!

				// Construct the input stream to send to Ftp server, from the
				// local
				// video file on the sd card
				BufferedInputStream buffIn = null;
				File file = new File(latestVideoFile_absolutepath);

				try {
					buffIn = new BufferedInputStream(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					//
					e.printStackTrace();
					Log
							.e(TAG,
									" got exception on local video file - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

						}
					}, 0);

					// This is a bad error, lets abort.
					// user dialog ?! shouldnt happen, but still...
					return;
				}

				ftpClient.enterLocalPassiveMode();

				try {
					// UPLOAD THE LOCAL VIDEO FILE.
					ftpClient.storeFile(ftpRemoteFtpFilename, buffIn);
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log
							.e(TAG,
									" got exception on storeFile - video uploading failed.");

					// This is a bad error, lets abort.
					// user dialog ?! shouldnt happen, but still...
					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

						}
					}, 0);
					return;
				}
				try {
					buffIn.close();
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log
							.e(TAG,
									" got exception on buff.close - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

						}
					}, 0);
					return;
				}
				try {
					ftpClient.logout();
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log
							.e(TAG,
									" got exception on ftp logout - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

						}
					}, 0);
					return;
				}
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log
							.e(TAG,
									" got exception on ftp disconnect - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((VidiomActivity) activity)
									.finishedUploading(false);
							((VidiomActivity) activity)
									.createNotification(res
											.getString(R.string.upload_to_ftp_host_failed_));

						}
					}, 0);
					return;
				}

				if (emailAddress != null && ftpHostName != null) {

					// EmailSender through IR controlled gmail system.
					SSLEmailSender sender = new SSLEmailSender(
							activity
									.getString(R.string.automatic_email_username),
							activity
									.getString(R.string.automatic_email_password)); // consider
					// this
					// public
					// knowledge.
					try {
						sender
								.sendMail(
										activity
												.getString(R.string.vidiom_automatic_email), // subject.getText().toString(),
										activity
												.getString(R.string.url_of_hosted_video_is_)
												+ " " + ftpHostName, // body.getText().toString(),
										activity
												.getString(R.string.automatic_email_from), // from.getText().toString(),
										emailAddress // to.getText().toString()
								);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage(), e);
					}
				}

				// Log record of this URL in POSTs table
				dbutils.creatHostDetailRecordwithNewVideoUploaded(sdrecord_id,
						ftpHostName, ftpHostName, "");

				// Use the handler to execute a Runnable on the
				// main thread in order to have access to the
				// UI elements.
				handler.postDelayed(new Runnable() {
					public void run() {
						// Update UI

						// Indicate back to calling activity the result!
						// update uploadInProgress state also.

						((VidiomActivity) activity).finishedUploading(true);
						((VidiomActivity) activity)
								.createNotification(res
										.getString(R.string.upload_to_ftp_host_succeeded_));

					}
				}, 0);

			}
		});

		t.start();

		return t;
	}

	public void launchEmailIntentWithCurrentVideo(final Activity activity,
			final String latestVideoFile_absolutepath) {
		Log.d(TAG, "launchEmailIntentWithCurrentVideo starting");

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// XXX hardcoded video mimetype
		i.setType("video/mp4");
		i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"
				+ latestVideoFile_absolutepath));
		activity.startActivity(i);
	}

	public void launchVideoPlayer(final Activity activity, final String movieurl) {

		try {
			Intent tostart = new Intent(Intent.ACTION_VIEW);
			tostart.setDataAndType(Uri.parse(movieurl), "video/*");
			activity.startActivity(tostart);
		} catch (android.content.ActivityNotFoundException e) {
			Log.e(TAG, " Cant start activity to show video!");

			new AlertDialog.Builder(activity).setMessage(
					R.string.cant_show_video).setPositiveButton(R.string.yes,
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

			return;
		}

	}

	public File selectFilenameAndCreateFile(String filenameConventionPrefence) {
		// Video file name selection process
		String new_videofile_name = res
				.getString(R.string.defaultVideoFilenamePrefix);
		String file_ext_name = ".mp4";

		if (filenameConventionPrefence.compareTo(res
				.getString(R.string.filenameConventionDefaultPreference)) == 0) {
			// The default is by date
			SimpleDateFormat postFormater = new SimpleDateFormat(
					"yyyy-MM-dd-HH-mm-ss");
			Calendar cal = Calendar.getInstance();
			Date now = cal.getTime();
			String newDateStr = postFormater.format(now);

			new_videofile_name += newDateStr + file_ext_name;

		} else {
			// Sequentially

			// look into database for this number
			int next_number = dbutils.getNextFilenameNumberAndIncrement();

			// XXX deal with -1 error condition

			new_videofile_name += next_number + file_ext_name;

		}

		File tempFile = new File(folder.getAbsolutePath(), new_videofile_name);
		return tempFile;
	}

	public boolean deleteVideo(String movieuri) {
		Log.d(TAG, "deleteVideo with " + movieuri);

		File tempFile = new File(movieuri);

		return tempFile.delete();

	}

	// youtube upload code from
	// from
	// http://code.google.com/p/ytd-android/source/browse/trunk/src/com/google/ytd/SubmitActivity.java
	// http://www.apache.org/licenses/LICENSE-2.0
	// Copyright 2010 Google License Apache
	// 
	// Changed by Andy Nicholson

	public void asyncYouTubeUpload(final Activity activity, final File file,
			final Handler handler, final String emailAddress,
			final long sdrecord_id) {

		new Thread(new Runnable() {
			public void run() {
				Message msg = new Message();
				Bundle bundle = new Bundle();
				msg.setData(bundle);

				String videoId = null;
				int submitCount = 0;
				try {
					while (submitCount <= MAX_RETRIES && videoId == null) {
						try {
							submitCount++;
							videoId = startYouTubeUpload(activity, file,
									handler, emailAddress, sdrecord_id);
							assert videoId != null;
						} catch (Internal500ResumeException e500) { // TODO -
							// this
							// should
							// not
							// really
							// happen
							if (submitCount < MAX_RETRIES) {
								Log.w(TAG, e500.getMessage());
								Log.d(TAG, String.format("Upload retry :%d.",
										submitCount));
							} else {
								Log.d(TAG, "Giving up");
								Log.e(TAG, e500.getMessage());
								throw new IOException(e500.getMessage());
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "AsyncYouTubeUpload ERROR - finishing");
					return;
				} catch (YouTubeAccountException e) {
					e.printStackTrace();
					Log.e(TAG, "AsyncYouTubeUpload ERROR - finishing");
					return;
				} catch (SAXException e) {
					e.printStackTrace();
					Log.e(TAG, "AsyncYouTubeUpload ERROR - finishing");
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
					Log.e(TAG, "AsyncYouTubeUpload ERROR - finishing");
				}

			}
		}).start();
	}

	static class YouTubeAccountException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public YouTubeAccountException(String msg) {
			super(msg);
		}
	}

	private String startYouTubeUpload(final Activity activity, File file,
			final Handler handler, final String emailAddress,
			final long sdrecord_id) throws IOException,
			YouTubeAccountException, SAXException,
			ParserConfigurationException, Internal500ResumeException {

		if (this.clientLoginToken == null) {
			// The stored gmail account is not linked to YouTube
			throw new YouTubeAccountException(this.youTubeName
					+ " is not linked to a YouTube account.");
		}

		String[] strs = dbutils
				.getTitleAndDescriptionFromID(new String[] { Long
						.toString(sdrecord_id) });
		// add our branding to the description.
		String uploadUrl = uploadMetaData(activity, handler, file
				.getAbsolutePath(), strs[0], strs[1] + "\n"
				+ activity.getString(R.string.uploaded_by_), true);

		Log.d(TAG, "uploadUrl=" + uploadUrl + " youtube account name is "
				+ this.youTubeName);
		Log.d(TAG, String.format("Client token : %s ", this.clientLoginToken));

		this.currentFileSize = file.length();
		this.totalBytesUploaded = 0;
		this.numberOfRetries = 0;

		int uploadChunk = 1024 * 1024 * 3; // 3MB

		int start = 0;
		int end = -1;

		String videoId = null;
		double fileSize = this.currentFileSize;
		while (fileSize > 0) {
			if (fileSize - uploadChunk > 0) {
				end = start + uploadChunk - 1;
			} else {
				end = start + (int) fileSize - 1;
			}
			Log.d(TAG, String.format("start=%s end=%s total=%s", start, end,
					file.length()));
			try {
				videoId = gdataUpload(file, uploadUrl, start, end);
				fileSize -= uploadChunk;
				start = end + 1;
				this.numberOfRetries = 0; // clear this counter as we had a
				// successful upload
			} catch (IOException e) {
				Log.d(TAG, "Error during upload : " + e.getMessage());
				ResumeInfo resumeInfo = null;
				do {
					if (!shouldResume()) {
						Log.d(TAG, String.format("Giving up uploading '%s'.",
								uploadUrl));
						throw e;
					}
					try {
						resumeInfo = resumeFileUpload(uploadUrl);
					} catch (IOException re) {
						// ignore
						Log.d(TAG, String.format(
								"Failed retry attempt of : %s due to: '%s'.",
								uploadUrl, re.getMessage()));
					}
				} while (resumeInfo == null);
				Log.d(TAG, String.format("Resuming stalled upload to: %s.",
						uploadUrl));
				if (resumeInfo.videoId != null) { // upload actually complted
					// despite the exception
					videoId = resumeInfo.videoId;
					Log.d(TAG, String.format(
							"No need to resume video ID '%s'.", videoId));
					break;
				} else {
					int nextByteToUpload = resumeInfo.nextByteToUpload;
					Log.d(TAG, String.format("Next byte to upload is '%d'.",
							nextByteToUpload));
					this.totalBytesUploaded = nextByteToUpload; // possibly
					// rolling back
					// the
					// previously
					// saved value
					fileSize = this.currentFileSize - nextByteToUpload;
					start = nextByteToUpload;
				}
			}
		}

		if (videoId != null) {

			if (emailAddress != null) {

				// EmailSender through IR controlled mail system.
				SSLEmailSender sender = new SSLEmailSender(activity
						.getString(R.string.automatic_email_username), activity
						.getString(R.string.automatic_email_password)); // consider
				// this
				// public
				// knowledge.
				try {
					sender
							.sendMail(
									activity
											.getString(R.string.vidiom_automatic_email), // subject.getText().toString(),
									activity
											.getString(R.string.url_of_hosted_video_is_)
											+ " "
											+ YOUTUBE_PLAYER_URL
											+ videoId, // body.getText().toString(),
									activity
											.getString(R.string.automatic_email_from), // from.getText().toString(),
									emailAddress // to.getText().toString()
							);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}

			// Log record of this URL in POSTs table
			dbutils.creatHostDetailRecordwithNewVideoUploaded(sdrecord_id,
					uploadUrl, YOUTUBE_PLAYER_URL + videoId, "");

			// Use the handler to execute a Runnable on the
			// main thread in order to have access to the
			// UI elements.
			handler.postDelayed(new Runnable() {
				public void run() {
					// Update UI

					// Indicate back to calling activity the result!
					// update uploadInProgress state also.

					((VidiomActivity) activity).finishedUploading(true);
					((VidiomActivity) activity)
							.createNotification(res
									.getString(R.string.upload_to_youtube_host_succeeded_));

				}
			}, 0);

			return videoId;
		}

		return null;
	}

	private String uploadMetaData(final Activity activity,
			final Handler handler, String filePath, String title,
			String description, boolean retry) throws IOException {
		String uploadUrl = INITIAL_UPLOAD_URL;

		HttpURLConnection urlConnection = getGDataUrlConnection(uploadUrl);
		urlConnection.setRequestMethod("POST");
		urlConnection.setDoOutput(true);
		urlConnection
				.setRequestProperty("Content-Type", "application/atom+xml");
		// urlConnection.setRequestProperty("Content-Length", newValue);
		urlConnection.setRequestProperty("Slug", filePath);
		String atomData = null;

		String category = DEFAULT_VIDEO_CATEGORY;
		this.tags = DEFAULT_VIDEO_TAGS;

		String template = readFile(activity, R.raw.gdata).toString();

		// Workarounds for corner cases. Youtube doesnt like empty titles
		if (title == null || title.length() == 0) {
			title = "Untitled";
		}
		if (description == null || description.length() == 0) {
			description = "No description";
		}

		atomData = String.format(template, title, description, category,
				this.tags);

		OutputStreamWriter outStreamWriter = null;
		int responseCode = -1;

		try {
			outStreamWriter = new OutputStreamWriter(urlConnection
					.getOutputStream());
			outStreamWriter.write(atomData);
			outStreamWriter.close();

			/*
			 * urlConnection.connect(); InputStream is =
			 * urlConnection.getInputStream(); BufferedReader in = new
			 * BufferedReader(new InputStreamReader(is)); String inputLine;
			 * 
			 * while ((inputLine = in.readLine()) != null) {
			 * Log.d(TAG,inputLine); } in.close();
			 */

			responseCode = urlConnection.getResponseCode();

			// ERROR LOGGING
			InputStream is = urlConnection.getErrorStream();
			if (is != null) {
				Log.e(TAG, " Error stream from Youtube available!");
				BufferedReader in = new BufferedReader(
						new InputStreamReader(is));
				String inputLine;

				while ((inputLine = in.readLine()) != null) {
					Log.d(TAG, inputLine);
				}
				in.close();

				Map<String, List<String>> hfs = urlConnection.getHeaderFields();
				for (Entry<String, List<String>> hf : hfs.entrySet()) {
					Log.d(TAG, " entry : " + hf.getKey());
					List<String> vals = hf.getValue();
					for (String s : vals) {
						Log.d(TAG, "vals:" + s);
					}
				}
			}

		} catch (IOException e) {
			//
			// Catch IO Exceptions here, like UnknownHostException, so we can
			// detect network failures, and send a notification
			//
			Log.d(TAG, " Error occured in uploadMetaData! ");
			e.printStackTrace();
			responseCode = -1;
			outStreamWriter = null;

			// Use the handler to execute a Runnable on the
			// main thread in order to have access to the
			// UI elements.
			handler.postDelayed(new Runnable() {
				public void run() {
					// Update UI

					// Indicate back to calling activity the result!
					// update uploadInProgress state also.

					((VidiomActivity) activity).finishedUploading(false);
					((VidiomActivity) activity)
							.createNotification(res
									.getString(R.string.upload_to_youtube_host_failed_));

				}
			}, 0);

			// forward it on!
			throw e;
		}

		if (responseCode < 200 || responseCode >= 300) {
			// The response code is 40X
			if ((responseCode + "").startsWith("4") && retry) {
				Log.d(TAG, "retrying to fetch auth token for " + youTubeName);
				this.clientLoginToken = authorizer.getFreshAuthToken(
						youTubeName, clientLoginToken);
				// Try again with fresh token
				return uploadMetaData(activity, handler, filePath, title,
						description, false);
			} else {

				// Probably not authorised!

				// Need to setup a Youtube account.

				// Use the handler to execute a Runnable on the
				// main thread in order to have access to the
				// UI elements.
				handler.postDelayed(new Runnable() {
					public void run() {
						// Update UI

						// Indicate back to calling activity the result!
						// update uploadInProgress state also.

						((VidiomActivity) activity).finishedUploading(false);
						((VidiomActivity) activity)
								.createNotification(res
										.getString(R.string.upload_to_youtube_host_failed_));

					}
				}, 0);

				throw new IOException(String.format(
						"response code='%s' (code %d)" + " for %s",
						urlConnection.getResponseMessage(), responseCode,
						urlConnection.getURL()));

			}
		}

		return urlConnection.getHeaderField("Location");
	}

	private String gdataUpload(File file, String uploadUrl, int start, int end)
			throws IOException {
		int chunk = end - start + 1;
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		FileInputStream fileStream = new FileInputStream(file);

		HttpURLConnection urlConnection = getGDataUrlConnection(uploadUrl);
		// some mobile proxies do not support PUT, using X-HTTP-Method-Override
		// to get around this problem
		if (isFirstRequest()) {
			Log.d(TAG, String.format(
					"Uploaded %d bytes so far, using POST method.",
					(int) totalBytesUploaded));
			urlConnection.setRequestMethod("POST");
		} else {
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty("X-HTTP-Method-Override", "PUT");
			Log
					.d(
							TAG,
							String
									.format(
											"Uploaded %d bytes so far, using POST with X-HTTP-Method-Override PUT method.",
											(int) totalBytesUploaded));
		}
		urlConnection.setDoOutput(true);
		urlConnection.setFixedLengthStreamingMode(chunk);
		// /XXX hardcoded video mimetype
		urlConnection.setRequestProperty("Content-Type", "video/mp4");
		urlConnection.setRequestProperty("Content-Range", String.format(
				"bytes %d-%d/%d", start, end, file.length()));
		Log.d(TAG, urlConnection.getRequestProperty("Content-Range"));

		OutputStream outStreamWriter = urlConnection.getOutputStream();

		fileStream.skip(start);

		int bytesRead;
		int totalRead = 0;
		while ((bytesRead = fileStream.read(buffer, 0, bufferSize)) != -1) {
			outStreamWriter.write(buffer, 0, bytesRead);
			totalRead += bytesRead;
			this.totalBytesUploaded += bytesRead;

			// double percent = (totalBytesUploaded / currentFileSize) * 99;

			/*
			 * Log.d(TAG, String.format(
			 * "fileSize=%f totalBytesUploaded=%f percent=%f", currentFileSize,
			 * totalBytesUploaded, percent));
			 */

			if (totalRead == (end - start + 1)) {
				break;
			}
		}

		outStreamWriter.close();

		int responseCode = urlConnection.getResponseCode();

		Log.d(TAG, "responseCode=" + responseCode);
		Log.d(TAG, "responseMessage=" + urlConnection.getResponseMessage());

		try {
			if (responseCode == 201) {
				String videoId = parseVideoId(urlConnection.getInputStream());

				Log.i(TAG, "Youtube video submitted - new video id is "
						+ videoId);

				// 100% finished here.

				// dialog.setProgress(100);

				return videoId;
			} else if (responseCode == 200) {
				Set<String> keySet = urlConnection.getHeaderFields().keySet();
				String keys = urlConnection.getHeaderFields().keySet()
						.toString();
				Log.d(TAG, String.format("Headers keys %s.", keys));
				for (String key : keySet) {
					Log.d(TAG, String.format("Header key %s value %s.", key,
							urlConnection.getHeaderField(key)));
				}
				Log.w(TAG, "Received 200 response during resumable uploading");
				throw new IOException(
						String
								.format(
										"Unexpected response code : responseCode=%d responseMessage=%s",
										responseCode, urlConnection
												.getResponseMessage()));
			} else {
				if ((responseCode + "").startsWith("5")) {
					String error = String.format(
							"responseCode=%d responseMessage=%s", responseCode,
							urlConnection.getResponseMessage());
					Log.w(TAG, error);
					// TODO - this exception will trigger retry mechanism to
					// kick in
					// TODO - even though it should not, consider introducing a
					// new type so
					// TODO - resume does not kick in upon 5xx
					throw new IOException(error);
				} else if (responseCode == 308) {
					// OK, the chunk completed successfully
					Log.d(TAG, String.format(
							"responseCode=%d responseMessage=%s", responseCode,
							urlConnection.getResponseMessage()));
				} else {
					// TODO - this case is not handled properly yet
					Log
							.w(
									TAG,
									String
											.format(
													"Unexpected return code : %d %s while uploading :%s",
													responseCode,
													urlConnection
															.getResponseMessage(),
													uploadUrl));
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean isFirstRequest() {
		return totalBytesUploaded == 0;
	}

	private ResumeInfo resumeFileUpload(String uploadUrl) throws IOException,
			ParserConfigurationException, SAXException,
			Internal500ResumeException {
		HttpURLConnection urlConnection = getGDataUrlConnection(uploadUrl);
		urlConnection.setRequestProperty("Content-Range", "bytes */*");
		urlConnection.setRequestMethod("POST");
		urlConnection.setRequestProperty("X-HTTP-Method-Override", "PUT");
		urlConnection.setFixedLengthStreamingMode(0);

		HttpURLConnection.setFollowRedirects(false);

		urlConnection.connect();
		int responseCode = urlConnection.getResponseCode();

		if (responseCode >= 300 && responseCode < 400) {
			int nextByteToUpload;
			String range = urlConnection.getHeaderField("Range");
			if (range == null) {
				Log.d(TAG, String.format(
						"PUT to %s did not return 'Range' header.", uploadUrl));
				nextByteToUpload = 0;
			} else {
				Log.d(TAG, String.format("Range header is '%s'.", range));
				String[] parts = range.split("-");
				if (parts.length > 1) {
					nextByteToUpload = Integer.parseInt(parts[1]) + 1;
				} else {
					nextByteToUpload = 0;
				}
			}
			return new ResumeInfo(nextByteToUpload);
		} else if (responseCode >= 200 && responseCode < 300) {
			return new ResumeInfo(parseVideoId(urlConnection.getInputStream()));
		} else if (responseCode == 500) {
			// TODO this is a workaround for current problems with resuming
			// uploads while switching transport (Wifi->EDGE)
			throw new Internal500ResumeException(String
					.format("Unexpected response for PUT to %s: %s "
							+ "(code %d)", uploadUrl, urlConnection
							.getResponseMessage(), responseCode));
		} else {
			throw new IOException(String
					.format("Unexpected response for PUT to %s: %s "
							+ "(code %d)", uploadUrl, urlConnection
							.getResponseMessage(), responseCode));
		}
	}

	//
	// This process fetches a google-youtube auth token, linked from a google
	// email account
	// if successfully, it launches the async upload.
	// 

	private boolean shouldResume() {
		this.numberOfRetries++;
		if (this.numberOfRetries > MAX_RETRIES) {
			return false;
		}
		try {
			int sleepSeconds = (int) Math.pow(BACKOFF, this.numberOfRetries);
			Log.d(TAG, String.format("Zzzzz for : %d sec.", sleepSeconds));
			Thread.currentThread().sleep(sleepSeconds * 1000);
			Log.d(TAG, String.format("Zzzzz for : %d sec done.", sleepSeconds));
		} catch (InterruptedException se) {
			se.printStackTrace();
			return false;
		}
		return true;
	}

	private String parseVideoId(InputStream atomDataStream)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(atomDataStream);

		NodeList nodes = doc.getElementsByTagNameNS("*", "*");
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			String nodeName = node.getNodeName();
			String val_print;
			if (node.getFirstChild() != null) {
				val_print = node.getFirstChild().getNodeValue();
			} else {
				val_print = "null";
			}

			Log.d(TAG, " node name " + nodeName + " val : " + val_print);
			if (nodeName != null && nodeName.equals("yt:videoid")) {
				return node.getFirstChild().getNodeValue();
			}
		}
		return null;
	}

	private HttpURLConnection getGDataUrlConnection(String urlString)
			throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Authorization", String.format(
				"GoogleLogin auth=\"%s\"", clientLoginToken));
		connection.setRequestProperty("GData-Version", "2");
		connection.setRequestProperty("X-GData-Key", String.format("key=%s",
				res.getString(R.string.youtube_dev_key)));
		return connection;
	}

	public void getYouTubeAuthTokenWithPermissionAndUpload(
			final Activity activity, String accountName, final String path,
			final Handler handler, final String emailAddress,
			final long sdrecord_id) {

		// Make the progress bar view visible.
		((VidiomActivity) activity).startedUploading();

		this.youTubeName = accountName;

		this.authorizer = new GlsAuthorizer.GlsAuthorizerFactory()
				.getAuthorizer(activity, GlsAuthorizer.YOUTUBE_AUTH_TOKEN_TYPE);

		this.authorizer.fetchAuthToken(accountName, activity,
				new AuthorizationListener<String>() {
					public void onCanceled() {

						Log.d(TAG, " Cancelled in fetchAuthToken! ");

					}

					public void onError(Exception e) {

						Log.d(TAG, " Error in fetchAuthToken! ");

						// Use the handler to execute a Runnable on the
						// main thread in order to have access to the
						// UI elements.
						handler.postDelayed(new Runnable() {
							public void run() {
								// Update UI

								// Indicate back to calling activity the result!
								// update uploadInProgress state also.

								((VidiomActivity) activity)
										.finishedUploading(false);
								((VidiomActivity) activity)
										.createNotification(res
												.getString(R.string.upload_to_youtube_host_failed_));

							}
						}, 0);

					}

					public void onSuccess(String result) {
						PublishingUtils.this.clientLoginToken = result;
						File file = new File(path);
						// Launch Async YouTube video upload.
						asyncYouTubeUpload(activity, file, handler,
								emailAddress, sdrecord_id);
					}
				});
	}

	class ResumeInfo {
		int nextByteToUpload;
		String videoId;

		ResumeInfo(int nextByteToUpload) {
			this.nextByteToUpload = nextByteToUpload;
		}

		ResumeInfo(String videoId) {
			this.videoId = videoId;
		}
	}

	/**
	 * Need this for now to trigger entire upload transaction retry
	 */
	class Internal500ResumeException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Internal500ResumeException(String message) {
			super(message);
		}
	}

	public static CharSequence readFile(Activity activity, int id) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(activity
					.getResources().openRawResource(id)));
			String line;
			StringBuilder buffer = new StringBuilder();
			while ((line = in.readLine()) != null) {
				buffer.append(line).append('\n');
			}
			// Chomp the last newline
			buffer.deleteCharAt(buffer.length() - 1);
			return buffer;
		} catch (IOException e) {
			return "";
		} finally {
			closeStream(in);
		}
	}

	/**
	 * Closes the specified stream.
	 * 
	 * @param stream
	 *            The stream to close.
	 */
	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

}
