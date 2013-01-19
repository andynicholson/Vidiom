package au.com.infiniterecursion.vidiom.activity;

/*
 * Vidiom  Activity interface 
 * 
 * AUTHORS:
 * 
 * Andy Nicholson
 * 
 * 2010
 * Copyright Infinite Recursion Pty Ltd.
 * http://www.infiniterecursion.com.au
 */

public abstract interface VidiomActivity {

	public abstract boolean isUploading();

	//Service code as defined by PublishingUtils public static final ints TYPE_*
	public abstract void startedUploading(int service_code);

	public abstract void finishedUploading(boolean success);

	public void createNotification(String notification_text);
}
