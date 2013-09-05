package com.globant.mobile.handson.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.globant.mobile.handson.exception.CameraNotAvailableException;

public class CustomCamera extends SurfaceView implements SurfaceHolder.Callback{
	/**
	 * Private attributes
	 */
	private SurfaceHolder mHolder;
	private Camera mCamera;		
	private File storageDir;
	/**
	 * Static attributes
	 */
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;
	private static final String ALBUM_NAME = "HandsOn";

	public CustomCamera(Context context) {
		super(context);		
		
		if(checkCameraHardware(context)){
			try{
				initCameraInstance();
				
			} catch(CameraNotAvailableException cnae){
				Log.d("HandsOn", "Error getting Camera: " + cnae.getMessage());
			}
		}				
		/**
		 * Install a SurfaceHolder.Callback so we get notified when
		 * the underlying surface is created and destroyed.
		 */
		mHolder = getHolder();
		mHolder.addCallback(this);
		/**
		 * Setting required on Android versions prior to 3.0
		 */
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}


	/**
	 * Check if device has at least 1 camera
	 * @param context
	 * @return
	 */
	private boolean checkCameraHardware(Context context){
		if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){			
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Intantiates the camera
	 * @throws CameraNotAvailableException
	 */
	private void initCameraInstance() throws CameraNotAvailableException{
		mCamera = null;
		try{
			mCamera = Camera.open();
		} catch(Exception e){
			throw new CameraNotAvailableException("Camera is not available (in use or does not exist)");
		}		
	}


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		
		if(mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}
		
		//stop preview before making changes
		try{
			mCamera.stopPreview();
		} catch(Exception e){
			//Nothing here
		}
		
		try{
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		}catch(Exception e){
			Log.d("HandsOn", "Error starting camera preview: " + e.getMessage());
		}
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try{
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		}catch(IOException e){
			Log.d("HandsOn", "Error setting Camera preview: " + e.getMessage());
		}
	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//The camera is released in the activity or not?
	}
	
	public void releaseCamera(){
		mCamera.release();
	}
	
	public void takePicture(){
		mCamera.takePicture(null, null, mPicture);
	}
	
	/**
	 * Creates the file for the taken image
	 * @return Image File for storage
	 * @throws IOException
	 */
	@SuppressLint("SimpleDateFormat")
	private File createImageFile(int type) throws IOException{
		File image = null;
		//Get the time stamp for the file's name
		String imageFileName = null;
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		
		if(type == MEDIA_TYPE_IMAGE){
			//Set the storage directory
			storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM_NAME);
			initStorageDir();
			//Create and image file name
			imageFileName = "IMG_" + timeStamp;
			image = new File(storageDir.getPath() + File.separator + imageFileName + ".jpg");
		}else if(type == MEDIA_TYPE_VIDEO){
			//Set the storage directory
			storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), ALBUM_NAME);
			initStorageDir();
			//Create and image file name
			imageFileName = "VID_" + timeStamp;
			image = new File(storageDir.getPath() + File.separator + imageFileName + ".mp4");
		}
		
		return image;
	}
	
	private void initStorageDir() throws IOException{		
		String state = Environment.getExternalStorageState();
		//Checks if the external storage is mounted
		if(Environment.MEDIA_MOUNTED.equals(state)){
			if(!storageDir.exists()){
				if(!storageDir.mkdirs()){
					Log.d("HandsOn", "Failed to create directory");
				}
			}
		}else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
			//External storage mounted but read only, can't write on it
			throw new IOException("External memory in READ ONLY mode");
		}else{
			//External storage not available/mounted/etc
			throw new IOException("External memory not available");
		}
	}
	
	private PictureCallback mPicture = new PictureCallback(){

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			File pictureFile;
			try {
				pictureFile = createImageFile(MEDIA_TYPE_IMAGE);
			} catch (IOException e) {
				Log.d("HandsOn", "Error creating media file, check storage permissions");
				e.printStackTrace();
				return;
			}
			
			try{
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch(FileNotFoundException e){
				Log.d("HandsOn", "File not found: " + e.getMessage());
			} catch(Exception e){
				Log.d("HandsOn", "Erroe accesing file: " + e.getMessage());
			}
		}
	};
}
