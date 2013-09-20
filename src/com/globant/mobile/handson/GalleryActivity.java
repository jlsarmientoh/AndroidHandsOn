package com.globant.mobile.handson;

import java.io.File;
import java.io.IOException;

import com.globant.mobile.handson.provider.Bitmaps;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class GalleryActivity extends FragmentActivity {
	
	private static final String TAG = "GalleryActivity";
	private static final String ALBUM_NAME = "HandsOn";
	private File storageDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }
        super.onCreate(savedInstanceState);

        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(android.R.id.content, new ImageGrid(), TAG);
            ft.commit();
        }
    }
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gallery, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@TargetApi(11)
    private void enableStrictMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                threadPolicyBuilder.penaltyFlashScreen();
                vmPolicyBuilder
                        .setClassInstanceLimit(GalleryActivity.class, 1)
                        .setClassInstanceLimit(GalleryActivity.class, 1);
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }
	
	private void initStorageDir() throws IOException{		
		String state = Environment.getExternalStorageState();
		storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM_NAME);
		//Checks if the external storage is mounted
		if(Environment.MEDIA_MOUNTED.equals(state)){
			if(!storageDir.exists()){
				if(!storageDir.mkdirs()){
					Log.d("HandsOn", "Failed to create directory");
				}
			} else{
				File[] files = storageDir.listFiles();
				if(files != null){
					Bitmaps.imageUrls = new String[files.length];
					Bitmaps.imageThumbUrls = new String[files.length];
					for(int i = 0; i < files.length; i++){
						Bitmaps.imageUrls[i] = files[i].getAbsolutePath();
						Bitmaps.imageThumbUrls[i] = files[i].getAbsolutePath();
					}
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
	public void onResume(){
		super.onResume();
		try {
			initStorageDir();
		} catch (IOException e) {
			if (BuildConfig.DEBUG) {
                Log.d(TAG, "External storage not initialized");
            }
			e.printStackTrace();
		}
	}
}
