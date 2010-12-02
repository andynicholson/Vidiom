package au.com.infiniterecursion.bubo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

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

import com.facebook.android.Facebook;
import com.facebook.android.Util;

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
import android.preference.PreferenceManager;
import android.util.Log;
import au.com.infiniterecursion.bubo.facebook.SessionEvents;

public class PublishingUtils {

	private static final String BUBO_INFINITERECURSION_COM_AU = "bubo@infiniterecursion.com.au";

	private static final String TAG = "RoboticEye-PublishingUtils";

	private File folder;
	private Resources res;
	private DBUtils dbutils;

	PublishingUtils(Resources res, DBUtils dbutils) {

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

	/*
	 * public Thread doPostToYouTube() {
	 * 
	 * new Thread(new Runnable() { public void run() { try { HttpTransport
	 * transport = setUpTransport(); ClientLogin login = new ClientLogin();
	 * login.applicationName = appName; login.authTokenType = "youtube";
	 * login.username = getUsername(); login.password = getPassword(); Response
	 * response = login.authenticate();
	 * response.setAuthorizationHeader(transport);
	 * 
	 * showState("Autorization passed");
	 * 
	 * Entry entry = new Entry(); entry.group = new MediaGroup();
	 * entry.group.title = getVideoTitle(); entry.group.description =
	 * mDescription.getText().toString(); entry.group.incomplete = new
	 * Incomplete(); entry.group.category = new MediaCategory();
	 * entry.group.category.Cat = "People\n";
	 * 
	 * XmlNamespaceDictionary namespaceDictionary = new
	 * XmlNamespaceDictionary(); namespaceDictionary.addNamespace("",
	 * "http://www.w3.org/2005/Atom"); namespaceDictionary.addNamespace("media",
	 * "http://search.yahoo.com/mrss/"); namespaceDictionary.addNamespace("gd",
	 * "http://schemas.google.com/g/2005");
	 * namespaceDictionary.addNamespace("yt",
	 * "http://gdata.youtube.com/schemas/2007");
	 * 
	 * AtomContent aContent = new AtomContent(); aContent.entry = entry;
	 * aContent.namespaceDictionary = namespaceDictionary;
	 * 
	 * InputStreamContent bContent = new InputStreamContent();
	 * bContent.setFileInput(new File(mPath)); bContent.type = "video/mp4";
	 * 
	 * MultipartRelatedContent multiContent = new MultipartRelatedContent();
	 * multiContent.parts.add(aContent); multiContent.parts.add(bContent);
	 * 
	 * HttpRequest request = transport.buildPostRequest(); request.setUrl(
	 * "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads");
	 * GoogleHeaders headers = (GoogleHeaders) request.headers;
	 * headers.setSlugFromFileName(mPath); request.content = multiContent;
	 * request.execute(); showResult("Success", "Video uploaded"); } catch
	 * (Exception e) { showResult("Error", e.getMessage()); } finally {
	 * handler.sendEmptyMessage(0); } }
	 * 
	 * private void showState(final String message) { handler.post(new
	 * Runnable() { public void run() { Toast.makeText(YoutubeActivity.this,
	 * message, Toast.LENGTH_SHORT).show(); } }); }
	 * 
	 * private void showResult(final String title, final String message) {
	 * handler.post(new Runnable() { public void run() { AlertDialog.Builder
	 * builder = new AlertDialog.Builder(YoutubeActivity.this) .setTitle(title)
	 * .setMessage(message) .setNegativeButton("Ok", new
	 * DialogInterface.OnClickListener() { public void onClick(DialogInterface
	 * dialog, int which) { YoutubeActivity.this.finish(); } }); builder.show();
	 * } }); }
	 * 
	 * }).start();
	 * 
	 * 
	 * 
	 * }
	 * 
	 * private HttpTransport setUpTransport() { HttpTransport transport =
	 * GoogleTransport.create(); GoogleHeaders headers = (GoogleHeaders)
	 * transport.defaultHeaders; headers.setApplicationName(appName);
	 * headers.setDeveloperId(devKey);
	 * 
	 * headers.gdataVersion = "2"; transport.addParser(new JsonCParser());
	 * return transport; }
	 */

	// XXX Make Threaded
	//
	public void videoUploadToFacebook(Facebook mFacebook, String path, String title,
			String description, long sdrecord_id) {
		Log.i(TAG, "Upload starting");
		// Initializing POST parameters
		Bundle params = new Bundle();
		params.putString("method", "facebook.video.upload");
		params.putString("format", "json");
		
		params.putString("title", title);
		params.putString("description", description);
	
		params.putString("call_id", String.valueOf(System.currentTimeMillis()));
		params.putString("v", "1.0");
		
		params.putString("oauth_token", mFacebook.getAccessToken());
	
		// Reading input file
		try {
			File videoFile = new File(path);
			byte[] data = new byte[(int) videoFile.length()];
			int len = data.length;

			InputStream is = new FileInputStream(videoFile);
			is.read(data);
			params.putByteArray(videoFile.getName(), data);
		} catch (Exception ex) {
			Log.e(TAG, "Cannot read file", ex);
		}

		// Sending POST request to Facebook
		String response = null;
		try {
			String url = "https://api-video.facebook.com/restserver.php";
			response = Util.openUrl(url, "POST", params);
			// SessionEvents.onUploadComplete(response);
		} catch (FileNotFoundException e) {
			// SessionEvents.onFileNotFoundException(e);
		} catch (MalformedURLException e) {
			// SessionEvents.onMalformedURLException(e);
		} catch (IOException e) {
			// SessionEvents.onIOException(e);
		}

		Log.i(TAG, "Uploading complete. Response is " + response);
		
		//response is JSON
		// decode, and grab URL
		
		// Log record of this URL in POSTs table
		dbutils.creatHostDetailRecordwithNewVideoUploaded(sdrecord_id,
				"facebook.com",
				response, "");
	}


	public Thread doPOSTtoVideoBin(final Activity activity,
			final Handler handler, final String video_absolutepath,
			final String emailAddress, final long sdrecord_id) {

		Log.d(TAG, "doPOSTtoVideoBin starting");

		// Make the progress bar view visible.
		((RoboticEyeActivity) activity).startedUploading();

		Thread t = new Thread(new Runnable() {
			public void run() {
				// Do background task.

				Resources res = activity.getResources();

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
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;

				}
				HttpPost post = new HttpPost(url);
				MultipartEntity entity = new MultipartEntity(
						HttpMultipartMode.BROWSER_COMPATIBLE);

				File file = new File(video_absolutepath);
				entity.addPart(res.getString(R.string.video_bin_API_videofile),
						new FileBody(file));

				try {
					entity.addPart(
							res.getString(R.string.video_bin_API_api),
							new StringBody("1", "text/plain", Charset
									.forName("UTF-8")));
				} catch (IllegalCharsetNameException e) {
					// error
					e.printStackTrace();
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;

				} catch (UnsupportedCharsetException e) {
					// error
					e.printStackTrace();
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;
				} catch (UnsupportedEncodingException e) {
					// error
					e.printStackTrace();
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;
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
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;
				} catch (ClientProtocolException e) {
					// error
					e.printStackTrace();
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;
				} catch (IOException e) {
					// error
					e.printStackTrace();
					((RoboticEyeActivity) activity).finishedUploading(false);
					return;
				}

				client.getConnectionManager().shutdown();

				Log.d(TAG, " got back " + response);

				// XXX Convert to preference for auto-email on videobin post
				// ?
				// stuck on YES here, if email is defined.

				if (false && emailAddress != null && response != null) {

					// EmailSender through IR controlled gmail system.
					// XX dont use gmail
					GMailEmailSender sender = new GMailEmailSender(
							BUBO_INFINITERECURSION_COM_AU, "bubo!!99"); // consider
																		// this
																		// public
																		// knowledge.
					try {
						sender.sendMail("Bubo automatic email.", // subject.getText().toString(),
								"URL of video is  " + response, // body.getText().toString(),
								BUBO_INFINITERECURSION_COM_AU, // from.getText().toString(),
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

						((RoboticEyeActivity) activity).finishedUploading(true);
						if (!activity.isFinishing()) {
							new AlertDialog.Builder(activity)
									.setMessage(R.string.video_bin_uploaded_ok)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											}).show();
						}

					}
				}, 0);
			}
		});

		t.start();

		return t;

	}

	public Thread doVideoFTP(final Activity activity, final Handler handler,
			final String latestVideoFile_filename,
			final String latestVideoFile_absolutepath, final long sdrecord_id) {

		Log.d(TAG, "doVideoFTP starting");

		// Make the progress bar view visible.
		((RoboticEyeActivity) activity).startedUploading();

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
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

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

				try {
					ftpClient.connect(uploadhost);
				} catch (SocketException e) {
					// These exceptions will be essentially caught by our check
					// of
					// ftpclient.login immediately below.
					// if you cant connect you wont be able to login.
					e.printStackTrace();
				} catch (UnknownHostException e) {
					//
					e.printStackTrace();
				} catch (IOException e) {
					//
					e.printStackTrace();
				}

				boolean reply = false;
				try {

					reply = ftpClient.login(ftpUsername, ftpPassword);
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log.e(TAG,
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
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

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
				}

				// Construct the input strteam to send to Ftp server, from the
				// local
				// video file on the sd card
				BufferedInputStream buffIn = null;
				File file = new File(latestVideoFile_absolutepath);

				try {
					buffIn = new BufferedInputStream(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					//
					e.printStackTrace();
					Log.e(TAG,
							" got exception on local video file - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

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
					Log.e(TAG,
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
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

						}
					}, 0);
					return;
				}
				try {
					buffIn.close();
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log.e(TAG,
							" got exception on buff.close - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

						}
					}, 0);
					return;
				}
				try {
					ftpClient.logout();
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log.e(TAG,
							" got exception on ftp logout - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

						}
					}, 0);
					return;
				}
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					//
					e.printStackTrace();
					Log.e(TAG,
							" got exception on ftp disconnect - video uploading failed.");

					// Use the handler to execute a Runnable on the
					// main thread in order to have access to the
					// UI elements.
					handler.postDelayed(new Runnable() {
						public void run() {
							// Update UI

							// Hide the progress bar
							((RoboticEyeActivity) activity)
									.finishedUploading(false);

						}
					}, 0);
					return;
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

						((RoboticEyeActivity) activity).finishedUploading(true);
						Log.d(TAG,
								"Activity isfinishing "
										+ activity.isFinishing());

						if (!activity.isFinishing()) {
							new AlertDialog.Builder(activity)
									.setMessage(R.string.video_bin_uploaded_ok)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {

												}
											}).show();
						}

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
		i.setType("video/mp4");
		i.putExtra(Intent.EXTRA_STREAM,
				Uri.parse("file://" + latestVideoFile_absolutepath));
		activity.startActivity(i);
	}

	public void launchVideoPlayer(final Activity activity, final String movieurl) {

		try {
			Intent tostart = new Intent(Intent.ACTION_VIEW);
			tostart.setDataAndType(Uri.parse(movieurl), "video/*");
			activity.startActivity(tostart);
		} catch (android.content.ActivityNotFoundException e) {
			Log.e(TAG, " Cant start activity to show video!");

			new AlertDialog.Builder(activity)
					.setMessage(R.string.cant_show_video)
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
}
