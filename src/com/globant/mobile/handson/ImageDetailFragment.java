package com.globant.mobile.handson;

import java.io.File;
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.globant.mobile.handson.media.task.BitmapWorker;
import com.globant.mobile.handson.media.task.MustacheWorker;
import com.globant.mobile.handson.media.task.WorkerListener;

public class ImageDetailFragment extends Fragment implements View.OnLongClickListener, WorkerListener<String> {

	private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private String mImageUrl;
    private ImageView mImageView;
    private BitmapFetcher mImageFetcher;
    private MustacheWorker mMustacheWorker;
    private ActionMode mActionMode;
    private boolean modified = false;
    private boolean share = false;

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
        
        mMustacheWorker = new MustacheWorker(this.getActivity());
        mMustacheWorker.setWorkerListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.fragment_image_detail, container, false);
        mImageView = (ImageView) v.findViewById(R.id.pictureFrame);
        mImageView.setDrawingCacheEnabled(true);        
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
            mImageFetcher.loadImage(mImageUrl, mImageView, "false", null);
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
    public void onPause(){    	
    	super.onPause();
    }

	@Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            BitmapWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
        if(mMustacheWorker != null){
        	mMustacheWorker.unRegisterListener();
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
						mImageFetcher.loadImage(mImageUrl, mImageView, "true", manager);
						modified = true;						
						mode.finish(); // Action picked, so close the CAB						
						return true;
					}
					case R.id.action_share:{
						if(modified){
							//Saves the modified picture before share it													
							mMustacheWorker.saveMustachedPicture(mImageUrl);							
							modified = false;
							share = true;
							//Sharing goes after the save mustache finishes see the onTaskCompleted method
						}else{
							getShareIntent(mImageUrl);
						}
						mode.finish();// Action picked, so close the CAB
						return true;
					}
					case R.id.action_save:{
						if(modified){							
							mMustacheWorker.saveMustachedPicture(mImageUrl);							
							modified = false;
							share = false;
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
    	shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(bitmapPath)));		
    	shareIntent.setType("image/jpeg");
    	
    	startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share)));
	}

	@Override
	public void onTaskCompleted(String result) {
		this.mImageUrl = result;
		if(share){
			getShareIntent(this.mImageUrl);
		}
	}
}
