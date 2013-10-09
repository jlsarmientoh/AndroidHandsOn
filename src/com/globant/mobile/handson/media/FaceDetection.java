package com.globant.mobile.handson.media;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

public class FaceDetection {
	
	private static final String ASSET = "mustache_256.png";
	private static final String TAG = "FaceDetection";


	public static Bitmap detectFaces(Bitmap bitmap){
		//Face Detection
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        FaceDetector detector = new FaceDetector(width, height, 5);
        Face[] faces = new Face[5];
        
        Bitmap bitmap565 = Bitmap.createBitmap(width, height, Config.RGB_565);
        Paint ditherPaint = new Paint();
        Paint drawPaint = new Paint();
        
        ditherPaint.setDither(true);
        drawPaint.setColor(Color.YELLOW);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(2);
        
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap565);
        canvas.drawBitmap(bitmap, 0, 0, ditherPaint);
        
        int facesFound = detector.findFaces(bitmap565, faces);
        PointF midPoint = new PointF();
        float eyeDistance = 0.0f;        
        
        if(facesFound > 0){
        	for(int i = 0; i < facesFound; i++){
        		faces[i].getMidPoint(midPoint);
        		eyeDistance = faces[i].eyesDistance();
        		
        		canvas.drawRect((int)midPoint.x - eyeDistance, 
        				(int)midPoint.y - eyeDistance,
        				(int)midPoint.x + eyeDistance,
        				(int)midPoint.y + eyeDistance, drawPaint);
        	}
        	
        	//return the bitmap with the marked faces
        	return bitmap565;
        }else{
        	return bitmap;
        }
	}
	
	public Bitmap putMustache(Bitmap bitmap, AssetManager manager){
		//Face Detection
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        FaceDetector detector = new FaceDetector(width, height, 5);
        Face[] faces = new Face[5];
        
        Bitmap bitmap565 = Bitmap.createBitmap(width, height, Config.RGB_565);
        Paint ditherPaint = new Paint();
        Paint drawPaint = new Paint();
        
        ditherPaint.setDither(true);
        /*drawPaint.setColor(Color.YELLOW);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(2);*/
        
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap565);
        canvas.drawBitmap(bitmap, 0, 0, ditherPaint);
        
        int facesFound = detector.findFaces(bitmap565, faces);
        PointF midPoint = new PointF();
        float eyeDistance = 0.0f;        
        
        if(facesFound > 0){
        	for(int i = 0; i < facesFound; i++){
        		faces[i].getMidPoint(midPoint);
        		eyeDistance = faces[i].eyesDistance();        		
        		Bitmap mustache = loadMustacheBitmap(eyeDistance, manager);
        		
        		canvas.drawBitmap(mustache, 
        				(int)midPoint.x - (int)(mustache.getWidth() / 2), 
        				(int)midPoint.y + (int)(eyeDistance / 3), 
        				null);
        	}
        	
        	//return the bitmap with the marked faces
        	return bitmap565;
        }else{
        	return bitmap;
        }
	}
	
	private Bitmap loadMustacheBitmap(float eyeDistance, AssetManager manager){
		Bitmap mustache = null;		
		InputStream is = null;
		
		try{
			is = manager.open(ASSET);
			Bitmap assetMustache = BitmapFactory.decodeStream(is);
			//Scale the mustache from eye distance, it will have the same width
			int width = assetMustache.getWidth();
			int height = assetMustache.getHeight();
			float scaleWidth = eyeDistance / (float)width;
			float scaleHeight = eyeDistance / (float)height;
			//create a Matrix for the manipulation
			Matrix matrix = new Matrix();
			//Resize the bitmap
			matrix.postScale(scaleWidth, scaleHeight);
			//Recreate the new bitmap
			mustache = Bitmap.createBitmap(assetMustache, 0, 0, width, height, matrix, false);
		}catch(IOException e){
			Log.e(TAG, e.getMessage(), e);
		}finally{
			if(is != null){
				try{
					is.close();
				}catch(IOException e){
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}
		
		return mustache;
	}
}
