package com.globant.mobile.handson.media.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.globant.mobile.handson.ImageDetailFragment;
import com.globant.mobile.handson.R;
import com.globant.mobile.handson.media.FaceDetection;

public class MustacheWorker {
	private static final String TAG = "MustacheWorker";
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;
	private static final String ALBUM_NAME = "HandsOn";
	
	private FaceDetection mFaceDetection;
	private AssetManager mManager;	
	private ProgressDialog mProgressDialog;
	private File storageDir;
	private SaveMustacheBitmapTask task;
	//Listener
	private WorkerListener<String> mListener;
	

	public MustacheWorker(Context context) {
		mManager = context.getAssets();				
		mFaceDetection = new FaceDetection();
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage(context.getText(R.string.savingMessage));
	}

	public void setWorkerListener(WorkerListener<String> mListener) {
		this.mListener = mListener;
	}
	
	public void unRegisterListener(){
		this.mListener = null;
	}

	public void saveMustachedPicture(Object targePicturepath){
		task = new SaveMustacheBitmapTask();
		
		mProgressDialog.setProgress(0);
		mProgressDialog.show();
		
		task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, targePicturepath);
	}
	
	public String getResult(){
		try {
			return task.get();
		} catch (InterruptedException e) {
			e.printStackTrace();			
		} catch (ExecutionException e) {			
			e.printStackTrace();
		}
		return null;
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
			imageFileName = "IMG_MUSTACHE_" + timeStamp;
			image = new File(storageDir.getPath() + File.separator + imageFileName + ".jpg");
		}else if(type == MEDIA_TYPE_VIDEO){
			//Set the storage directory
			storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), ALBUM_NAME);
			initStorageDir();
			//Create and image file name
			imageFileName = "VID_MUSTACHE_" + timeStamp;
			image = new File(storageDir.getPath() + File.separator + imageFileName + ".mp4");
		}
		
		return image;
	}
	
	private String savePicture(Bitmap mustachedBitmap){
		File pictureFile = null;
		try {
			pictureFile = createImageFile(MEDIA_TYPE_IMAGE);
			
			FileOutputStream fos = new FileOutputStream(pictureFile);
			mustachedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();
			return pictureFile.getAbsolutePath();
		} catch(FileNotFoundException e){
			Log.d(TAG, "File not found: " + e.getMessage());
			return null;
		} catch (IOException e) {
			Log.d(TAG, "Error creating media file, check storage permissions");			
			return null;
		} catch(Exception e){
			Log.d(TAG, "Erroe accesing file: " + e.getMessage());
			return null;
		}
	}
	
	private class SaveMustacheBitmapTask extends AsyncTask<Object, Integer, String>{

		@Override
		protected String doInBackground(Object... params) {
			
			Object bitmapPath = params[0];
			Log.d("SaveMustacheBitmapTask", "doInBackground: " + bitmapPath.toString());
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			publishProgress(10);
			Bitmap baseBitmap = BitmapFactory.decodeFile((String)bitmapPath);
			Log.d("SaveMustacheBitmapTask", "doInBackground: Put mustache" + bitmapPath.toString());
			publishProgress(60);
			Bitmap mustachedBitmap = mFaceDetection.putMustache(baseBitmap, mManager);
			Log.d("SaveMustacheBitmapTask", "doInBackground: Save mustache" + bitmapPath.toString());
			publishProgress(80);
			return savePicture(mustachedBitmap);
		}
		
		protected void onProgressUpdate(Integer... progress){
			mProgressDialog.setProgress(progress[0]);
			mProgressDialog.show();
		}
		
		@Override
		protected void onPostExecute(String result){
			Log.d("SaveMustacheBitmapTask", "onPostExecute: " + result);			
			mListener.onTaskCompleted(result);
			mProgressDialog.hide();
		}
	}

}
