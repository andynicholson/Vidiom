package au.com.infiniterecursion.roboticeye;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

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


public class MainActivity extends Activity implements SurfaceHolder.Callback, MediaRecorder.OnInfoListener {

	private static final String TAG = "RoboticEye";

	private static final int MENU_ITEM_1 = Menu.FIRST;
	private static final int MENU_ITEM_2 = MENU_ITEM_1+1;
	private static final int MENU_ITEM_3 = MENU_ITEM_2+1;
	private static final int MENU_ITEM_4 = MENU_ITEM_3+1;
	private static final int MENU_ITEM_5 = MENU_ITEM_4+1;
	
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private boolean previewRunning;

	// Objects for recording
	private MediaRecorder mediaRecorder;
	// 40 seconds max video
	private final int maxDurationInMs = 40 * 1000;
	// 1MB limit
	private final long maxFileSizeInBytes = 1000000;
	private final int videoFramesPerSecond = 25;

	private boolean recordingInMotion;
	private String latestTempVideoFile;
	private boolean canSendVideoFile;
	private boolean uploadedSuccessfully;
	
	private File folder;
	private String rootSDcardFolder = "/RoboticEye/";
	private boolean canAccessSDCard = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.main);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.surface_layout);
		surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
				
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		recordingInMotion = false;
		canSendVideoFile = false;
		latestTempVideoFile = "";
		uploadedSuccessfully = false;
		
		//check our folder exists, and if not make it
		
		//android.os.Environment.getExternalStorageDirectory().getPath()
		folder = new File(Environment.getExternalStorageDirectory() + rootSDcardFolder);
		boolean success;
		if (! folder.exists()) {
			
			Log.d(TAG, " Folder doesnt exit ... attempting to make it");
			
			success = folder.mkdir();
			if (! success) {
				canAccessSDCard = false;
				
				new AlertDialog.Builder(this)
        		.setMessage(R.string.sdcard_failed)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		
	                   }
	                })
	          
	            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		
	                    }
	                })
				.show();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		Log.i(TAG, "OnCreateOptionsMenu called");

		
		menu.add(0, MENU_ITEM_1, 0, "Record");
	
		menu.add(0, MENU_ITEM_5, 0, "About");
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		Log.i(TAG, "OnPrepareOptionsMenu called");
		
		menu.clear();
		
		if (recordingInMotion) {
			menu.removeItem(MENU_ITEM_1);
			menu.add(0, MENU_ITEM_2, 0, "Stop");
		} else {
			menu.add(0, MENU_ITEM_1, 0, "Record");
			menu.removeItem(MENU_ITEM_2);
		}
		
		if (canSendVideoFile) {
			menu.add(0, MENU_ITEM_3, 0, "Publish to videobin.org");
			menu.add(0, MENU_ITEM_4, 0, "Send via Email");
		} else {
			menu.removeItem(MENU_ITEM_3);
			menu.removeItem(MENU_ITEM_4);
		}
		
		menu.add(0, MENU_ITEM_5, 0, "About");
		
		return true;
	}
	
	@Override	
	public boolean onOptionsItemSelected(MenuItem menuitem) {
		int menuNum = menuitem.getItemId();
		

		Log.d("MENU", "Option " + menuNum + " selected");

		switch (menuitem.getItemId()) {

		// Record
		case MENU_ITEM_1:
			
			if (! uploadedSuccessfully && canSendVideoFile) {
				
				new AlertDialog.Builder(this)
        		.setMessage(R.string.this_will_wipe_existing_video)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                	
	                    	
	                    	tryToStartRecording();
	                		
	                   }

						
	                })
	          
	            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		
	                    }
	                })
				.show();
				
				
			} else {
				
				tryToStartRecording();
			}
			
			
			break;
			
		// Stop
		case MENU_ITEM_2:
			if (recordingInMotion)  { stopRecording(); } 
			else {
				//
				new AlertDialog.Builder(this)
        		.setMessage(R.string.notrecording)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                	
	                   }
	                })
	          
	            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                	
	                    }
	                })
				.show();
			}
			break;
			
		//Post to Video FTP service
		case MENU_ITEM_3:
			
			if (canSendVideoFile && ! recordingInMotion) {
				

				//doVideoFTP();
				uploadedSuccessfully = false;
				
				HttpClient client = new DefaultHttpClient();
				client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

				URI url = null;
				try {
					url = new URI("http://videobin.org/add");
				} catch (URISyntaxException e) {
					// 
					e.printStackTrace();
				}
				HttpPost        post   = new HttpPost( url );
				MultipartEntity entity = new MultipartEntity( HttpMultipartMode.BROWSER_COMPATIBLE );

				File file = new File(latestTempVideoFile);
				entity.addPart("videoFile", new FileBody(file));
				
				try {
					entity.addPart( "api", new StringBody( "1", "text/plain",Charset.forName( "UTF-8" )));
				} catch (IllegalCharsetNameException e) {
					// 
					e.printStackTrace();
				} catch (UnsupportedCharsetException e) {
					// 
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					//
					e.printStackTrace();
				}

				post.setEntity( entity );

				// Here we go!
				String response = null;
				try {
					response = EntityUtils.toString( client.execute( post ).getEntity(), "UTF-8" );
				} catch (ParseException e) {
					// 
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					// 
					e.printStackTrace();
				} catch (IOException e) {
					// 
					e.printStackTrace();
				}

				client.getConnectionManager().shutdown();
				
				Log.d(TAG, " got back " + response);
				uploadedSuccessfully = true;
				
				
			} else if (recordingInMotion) {
				
				new AlertDialog.Builder(this)
	    		.setMessage(R.string.stop_recording_to_send)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                	
	                   }
	                })
	          
				.show();
				
				
			} else if (! recordingInMotion) {
				

				new AlertDialog.Builder(this)
	    		.setMessage(R.string.haventrecorded)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		
	                   }
	                })
	          
				.show();
				
			}
			
			break;
			
			
		case MENU_ITEM_4:
			//Email
			if (canSendVideoFile && ! recordingInMotion) {
				
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.setType("video/mp4");
				i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + latestTempVideoFile));
				startActivity(i);
				
			} else if (recordingInMotion) {
				
				new AlertDialog.Builder(this)
	    		.setMessage(R.string.stop_recording_to_send)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                	
	                   }
	                })
	          
				.show();
				
				
			} else if (! recordingInMotion ) {
				
				new AlertDialog.Builder(this)
	    		.setMessage(R.string.haventrecorded)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		
	                   }
	                })
	          
				.show();
				
				
			}
			break;
			
		case MENU_ITEM_5:
			//ABOUT
			new AlertDialog.Builder(this)
    		.setMessage(R.string.about_this)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                		
                    	
                   }
                })
			.show();
			
		default:
			return super.onOptionsItemSelected(menuitem);
	    }
		
		return true;
	}

	
	private void tryToStartRecording() {
		if (canAccessSDCard && startRecording()) {
			
			recordingInMotion = true;
			
		} else {
			
			new AlertDialog.Builder(MainActivity.this)
    		.setMessage(R.string.camera_failed)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                	
                		
                   }
                })
          
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                		
                    }
                })
			.show();
			
			recordingInMotion = false;
		}
	}
	
	private void doVideoFTP() {
		
		uploadedSuccessfully = false;
		
		String ftpHostName = "engagemedia.org";
		String ftpUsername = "re";
		String ftpPassword = "rere!!";
		String ftpRemoteFtpFilename = "test2.mp4";
		
		//FTP
		FTPClient ftpClient = new FTPClient();
		InetAddress uploadhost = null;
		try {
			
			uploadhost = InetAddress.getByName(ftpHostName);
		} catch (UnknownHostException e1) {
			// If DNS resolution fails then abort immediately - show dialog to inform user first.
			e1.printStackTrace();
			Log.e(TAG, " got exception resolving " + ftpHostName + " - video uploading failed.");
			uploadhost = null;
		}
		
		if (uploadhost == null) {
			new AlertDialog.Builder(this)
			.setMessage(R.string.cant_find_upload_host)
		    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		        	
		           }
		        })
		  
		    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		        		
		            }
		        })
			.show();
			
			return;
		}
		
		try {
			ftpClient.connect(uploadhost);
		} catch (SocketException e) {
			// These exceptions will be essentially caught by our check of ftpclient.login immediately below.
			//if you cant connect you wont be able to login.
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
			Log.e(TAG, " got exception on ftp.login - video uploading failed.");
		}
		
		//check the reply code here
		// If we cant login, abort after showing user a dialog.
		if(!reply) {
			try {
				ftpClient.disconnect();
			} catch (IOException e) {
				//
				e.printStackTrace();
			}
			
			new AlertDialog.Builder(this)
			.setMessage(R.string.cant_login_upload_host)
		    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		        		
		           }
		        })
		  
		    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		        		
		            }
		        })
			.show();
			
			
			return;
		  }
		
		// Set File type to binary
		try {
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		} catch (IOException e) {
			// 
			e.printStackTrace();
		}
		
		//Construct the input strteam to send to Ftp server, from the local video file on the sd card
		BufferedInputStream buffIn=null;
		File file = new File(latestTempVideoFile);
		
		try {
			buffIn=new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			// 
			e.printStackTrace();
			Log.e(TAG, " got exception on local video file - video uploading failed.");
			
			//This is a bad error, lets abort.
			//XXX user dialog ?! shouldnt happen, but still...
			return;
		}
		
		ftpClient.enterLocalPassiveMode();
		
		try {
			//UPLOAD THE LOCAL VIDEO FILE.
			ftpClient.storeFile(ftpRemoteFtpFilename, buffIn);
		} catch (IOException e) {
			// 
			e.printStackTrace();
			Log.e(TAG, " got exception on storeFile - video uploading failed.");
			
			//XXX user dialog ?! shouldnt happen, but still...
			
			return;
		}
		try {
			buffIn.close();
		} catch (IOException e) {
			// 
			e.printStackTrace();
			Log.e(TAG, " got exception on buff.close - video uploading failed.");
			return;
		}
		try {
			ftpClient.logout();
		} catch (IOException e) {
			// 
			e.printStackTrace();
			Log.e(TAG, " got exception on ftp logout - video uploading failed.");
			return;
		}
		try {
			ftpClient.disconnect();
		} catch (IOException e) {
			//
			e.printStackTrace();
			Log.e(TAG, " got exception on ftp disconnect - video uploading failed.");
			return;
		}
		
		//If we get here, it all worked out.
		uploadedSuccessfully = true;
	}
	
	

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// 
		if (previewRunning) {
			camera.stopPreview();
		}
		Camera.Parameters p = camera.getParameters();
		
		Log.d(TAG, " format width height are : " + format + ":" + width+ ":"+height);
		
		//320, 240 seems only possible resolution
		// and it seems the preview size must be the same as the video size
		//
		p.setPreviewSize(320,240);
		//p.setPictureSize(320,240);
		p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
		
		
		Log.d(TAG, "Parameters are "+ p.toString());
		
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
		camera.stopPreview();
		previewRunning = false;
		camera.release();
	}
	
	
	public boolean startRecording(){
		try {
			canSendVideoFile = false;
			
			camera.unlock();

			mediaRecorder = new MediaRecorder();
			mediaRecorder.setOnInfoListener(this);
			
			mediaRecorder.setCamera(camera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
			
			mediaRecorder.setMaxDuration(maxDurationInMs);

			//Tempfile name
			String cacheFileName = "RoboticEyeTest2.mp4";
			File tempFile = new File(folder.getAbsolutePath(),cacheFileName);
			
			mediaRecorder.setOutputFile(tempFile.getAbsolutePath());
			
			Log.d(TAG,"Starting recording into " + tempFile.getAbsolutePath());
			
			
			latestTempVideoFile = tempFile.getAbsolutePath();
			
			mediaRecorder.setVideoFrameRate(videoFramesPerSecond);
			
			//mediaRecorder.setVideoSize(surfaceView.getWidth(), surfaceView.getHeight())
			
			mediaRecorder.setVideoSize(320,240);
			
			mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

			mediaRecorder.setMaxFileSize(maxFileSizeInBytes);

			
			
            mediaRecorder.prepare();
			mediaRecorder.start();

			return true;
		} catch (IllegalStateException e) {
			Log.e(TAG, "Illegal State Exception" + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public void stopRecording(){
		
		mediaRecorder.stop();
		camera.lock();
		recordingInMotion = false;
		canSendVideoFile = true;
	}

	public void onInfo(MediaRecorder mr, int what, int extra) {
		// 
		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
			Log.d(TAG, "We have reached the end limit");
			stopRecording();
		}
		
	}
	
	
}