package com.globant.mobile.handson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.globant.mobile.handson.media.BitmapFetcher;
import com.globant.mobile.handson.media.FaceDetection;
import com.globant.mobile.handson.media.task.BitmapWorker;

public class ImageDetailFragment extends Fragment implements View.OnLongClickListener {

	private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private String mImageUrl;
    private ImageView mImageView;
    private BitmapFetcher mImageFetcher;
    private ActionMode mActionMode;
    private FaceDetection mFaceDetection;
    private File storageDir;
    /**
	 * Static attributes
	 */
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;
	private static final String ALBUM_NAME = "HandsOn";

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageDetailFragment newInstance(String imageUrl) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {}

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(String)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_EXTRA) : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.fragment_image_detail, container, false);
        mImageView = (ImageView) v.findViewById(R.id.pictureFrame);
        mFaceDetection = new FaceDetection();
      //Setting the Context Menu for the GridVew id API level is lower than Honeycomb
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
        	registerForContextMenu(mImageView);
        }else{
        	mImageView.setOnLongClickListener(this);
        }
        
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (ImageDetailActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((ImageDetailActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(mImageUrl, mImageView);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (OnClickListener.class.isInstance(getActivity()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo){
    	super.onCreateContextMenu(menu, view, menuInfo);
    	
    	MenuInflater inflater = this.getActivity().getMenuInflater();
    	inflater.inflate(R.menu.image_detail, menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            BitmapWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onLongClick(View view) {
		//Setting the Context Action Bar if API Level is Honeycomb and higher
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
        	if(mActionMode != null){
        		return false;
        	}
        	
        	final AssetManager manager = this.getActivity().getAssets();
        	
        	ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
				
				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {					
					return false;
				}
				
				@Override
				public void onDestroyActionMode(ActionMode mode) {
					mActionMode = null;
					
				}
				
				@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					// Inflate a menu resource providing context menu items
			        MenuInflater inflater = mode.getMenuInflater();
			        inflater.inflate(R.menu.image_detail, menu);
			        
			        
			        return true;
				}
				
				@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					switch(item.getItemId()){
					case R.id.action_mustache:{						
						mode.finish(); // Action picked, so close the CAB
						Bitmap b = BitmapFactory.decodeFile(mImageUrl);
						Bitmap mustachedBitmap = mFaceDetection.putMustache(b, manager);
						mImageView.setImageBitmap(mustachedBitmap);
						File pictureFile;
						try {
							pictureFile = createImageFile(MEDIA_TYPE_IMAGE);
						} catch (IOException e) {
							Log.d("HandsOn", "Error creating media file, check storage permissions");
							e.printStackTrace();
							return false;
						}
						
						try{
							FileOutputStream fos = new FileOutputStream(pictureFile);
							mustachedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
							fos.close();
							mImageUrl = pictureFile.getAbsolutePath();
						} catch(FileNotFoundException e){
							Log.d("HandsOn", "File not found: " + e.getMessage());
						} catch(Exception e){
							Log.d("HandsOn", "Erroe accesing file: " + e.getMessage());
						}
						return true;
					}
					case R.id.action_share:{
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
					        getShareIntent(mImageUrl);
				        }
						mode.finish();// Action picked, so close the CAB
						return true;
					}
					default:
						return false;
					}
				}
			};
        	
        	mActionMode = this.getActivity().startActionMode(mActionModeCallback);
        	view.setSelected(true);        	        	
        	return true;
        }else{
        	return false;
        }
	}

	private void getShareIntent(String bitmapPath) {    	    	
    	//Create the intent
    	Intent shareIntent = new Intent();
    	shareIntent.setAction(Intent.ACTION_SEND);
    	shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(bitmapPath));		
    	shareIntent.setType("image/jpeg");
    	
    	startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share)));
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
	
}
