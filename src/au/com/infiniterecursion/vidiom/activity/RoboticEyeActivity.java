package au.com.infiniterecursion.vidiom.activity;


public abstract interface RoboticEyeActivity  {
	
	public abstract boolean isUploading();
	
	public abstract void startedUploading();
	
	public abstract void finishedUploading(boolean success);
	
	public void createNotification(String notification_text);
}
