package au.com.infiniterecursion.roboticeye;

public abstract interface RoboticEyeActivity  {
	
	public abstract boolean isUploading();
	
	public abstract void startedUploading();
	
	public abstract void finishedUploading(boolean success);
}
