package com.globant.mobile.handson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends BaseActivity {

	/**
	 * Static Attributes
	 */
	public static final String 	EXTRA_MESSAGE = "com.globant.mobile.handson.MESSAGE";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;
	private static final String ALBUM_NAME = "HandsOn";
	/**
	 * Private Attributes
	 */
	private File storageDir;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Show the Up button in the action bar.
		setupActionBar();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		//Handle presses on the action bar items
		switch(item.getItemId()){
			case R.id.action_settings:{
				//openSettings();
				return true;
			}
			case R.id.action_search:{
				//openSearch();
				return true;
			}
			default:{
				return super.onOptionsItemSelected(item);
			}
		}
	}
	
	/**
	 * Opens the Display Message activity
	 * @param view
	 */
	public void sendMessage(View view){
		/*String message = null;
		
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		
		EditText editText = (EditText)findViewById(R.id.edit_message);
		message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		
		startActivity(intent);*/
	}	
	/**
	 * Opens the Custom Camera activity 
	 * @param view
	 */
	public void dispatchTakePictureInternal(View view){
		Intent intent = new Intent(this, CameraActivity.class);
		startActivity(intent);
	}
	/**
	 * Opens the featured camera activity (External Activity)
	 * @param view
	 */
	public void dispatchTakePictureExternal(View view){
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);		
		try{
			intent.putExtra(MediaStore.EXTRA_OUTPUT, createImageFile(MEDIA_TYPE_IMAGE));		
			startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
		} catch(IOException ioex){
			Toast.makeText(this, "Error: " + ioex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	/**
	 * Opens the Gallery activity
	 * @param view
	 */
	public void dispatchGallery(View view){
		Intent intent = new Intent(this, GalleryActivity.class);
		startActivity(intent);
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
			if(resultCode == RESULT_OK){
				//Image captured and saved to storageDir + createImageFile() specified in the Intent
				Toast.makeText(this, "Image saved on:\n" + data.getData(), Toast.LENGTH_LONG).show();
			}
		}
		
		if(requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE){
			if(resultCode == RESULT_OK){
				//Image captured and saved to storageDir + createImageFile() specified in the Intent
				Toast.makeText(this, "Video saved on:\n" + data.getData(), Toast.LENGTH_LONG).show();
			}
		}
	}

}
