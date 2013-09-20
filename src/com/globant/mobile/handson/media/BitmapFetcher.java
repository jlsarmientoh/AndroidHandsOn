package com.globant.mobile.handson.media;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.globant.mobile.handson.BuildConfig;
import com.globant.mobile.handson.util.DiskLruCache;


public class BitmapFetcher extends BitmapDecoder {

    private static final String TAG = "BimapFetcher";
    private static final int BITMAP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String BITMAP_CACHE_DIR = "bitmap";
    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private DiskLruCache mBitmapDiskCache;
    private File mBitmapCacheDir;
    private boolean mBitmapDiskCacheStarting = true;
    private final Object mBitmapDiskCacheLock = new Object();
    private static final int DISK_CACHE_INDEX = 0;
    

    /**
     * Initialize providing a target image width and height for the processing images.
     *
     * @param context
     * @param imageWidth
     * @param imageHeight
     */
    public BitmapFetcher(Context context, int imageWidth, int imageHeight) {
        super(context, imageWidth, imageHeight);
        init(context);
    }

    /**
     * Initialize providing a single target image size (used for both width and height);
     *
     * @param context
     * @param imageSize
     */
    public BitmapFetcher(Context context, int imageSize) {
        super(context, imageSize);
        init(context);
    }

    private void init(Context context) {
        checkConnection(context);        
        mBitmapCacheDir = BitmapCache.getDiskCacheDir(context, BITMAP_CACHE_DIR);
    }

    @Override
    protected void initDiskCacheInternal() {
        super.initDiskCacheInternal();
        initBitmapDiskCache();
    }

    private void initBitmapDiskCache() {
        if (!mBitmapCacheDir.exists()) {
            mBitmapCacheDir.mkdirs();
        }
        synchronized (mBitmapDiskCacheLock) {
            if (BitmapCache.getUsableSpace(mBitmapCacheDir) > BITMAP_CACHE_SIZE) {
                try {
                    mBitmapDiskCache = DiskLruCache.open(mBitmapCacheDir, 1, 1, BITMAP_CACHE_SIZE);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Disk cache initialized");
                    }
                } catch (IOException e) {
                    mBitmapDiskCache = null;
                }
            }
            mBitmapDiskCacheStarting = false;
            mBitmapDiskCacheLock.notifyAll();
        }
    }

    @Override
    protected void clearCacheInternal() {
        super.clearCacheInternal();
        synchronized (mBitmapDiskCacheLock) {
            if (mBitmapDiskCache != null && !mBitmapDiskCache.isClosed()) {
                try {
                    mBitmapDiskCache.delete();
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "HTTP cache cleared");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "clearCacheInternal - " + e);
                }
                mBitmapDiskCache = null;
                mBitmapDiskCacheStarting = true;
                initBitmapDiskCache();
            }
        }
    }

    @Override
    protected void flushCacheInternal() {
        super.flushCacheInternal();
        synchronized (mBitmapDiskCacheLock) {
            if (mBitmapDiskCache != null) {
                try {
                    mBitmapDiskCache.flush();
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "HTTP cache flushed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }

    @Override
    protected void closeCacheInternal() {
        super.closeCacheInternal();
        synchronized (mBitmapDiskCacheLock) {
            if (mBitmapDiskCache != null) {
                try {
                    if (!mBitmapDiskCache.isClosed()) {
                        mBitmapDiskCache.close();
                        mBitmapDiskCache = null;
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "HTTP cache closed");
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "closeCacheInternal - " + e);
                }
            }
        }
    }

    /**
    * Simple network connection check.
    *
    * @param context
    */
    private void checkConnection(Context context) {
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            //Toast.makeText(context, R.string.no_network_connection_toast, Toast.LENGTH_LONG).show();
            Log.e(TAG, "checkConnection - no connection found");
        }
    }

    /**
     * The main process method, which will be called by the ImageWorker in the AsyncTask background
     * thread.
     *
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized bitmap
     */
    private Bitmap processBitmap(String data) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - " + data);
        }

        final String key = BitmapCache.hashKeyForDisk(data);
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        DiskLruCache.Snapshot snapshot;
        synchronized (mBitmapDiskCacheLock) {
            // Wait for disk cache to initialize
            while (mBitmapDiskCacheStarting) {
                try {
                    mBitmapDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }

            if (mBitmapDiskCache != null) {
                try {
                    snapshot = mBitmapDiskCache.get(key);
                    if (snapshot == null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "processBitmap, not found in http cache, downloading...");
                        }
                        DiskLruCache.Editor editor = mBitmapDiskCache.edit(key);
                        if (editor != null) {
                            if (downloadUrlToStream(data,
                                    editor.newOutputStream(DISK_CACHE_INDEX))) {
                                editor.commit();
                            } else {
                                editor.abort();
                            }
                        }
                        snapshot = mBitmapDiskCache.get(key);
                    }
                    if (snapshot != null) {
                        fileInputStream =
                                (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
                        fileDescriptor = fileInputStream.getFD();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "processBitmap - " + e);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "processBitmap - " + e);
                } finally {
                    if (fileDescriptor == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {}
                    }
                }
            }
        }

        Bitmap bitmap = null;
        if (fileDescriptor != null) {
            bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor, mImageWidth,
                    mImageHeight, getImageCache());
            //Face Detection
            /*int width = bitmap.getWidth();
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
            float confidence = 0.0f;
            
            if(facesFound > 0){
            	for(int i = 0; i < facesFound; i++){
            		faces[i].getMidPoint(midPoint);
            		eyeDistance = faces[i].eyesDistance();
            		confidence = faces[i].confidence();
            		
            		canvas.drawRect((int)midPoint.x - eyeDistance, 
            				(int)midPoint.y - eyeDistance,
            				(int)midPoint.x + eyeDistance,
            				(int)midPoint.y + eyeDistance, drawPaint);
            	}
            	
            	bitmap = bitmap565;
            }*/
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {}
        }
        return bitmap;
    }

    @Override
    protected Bitmap processBitmap(Object data) {
        return processBitmap(String.valueOf(data));
    }

    /**
     * Download a bitmap from a URL and write the content to an output stream.
     *
     * @param urlString The URL to fetch
     * @return true if successful, false otherwise
     */
    public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {        
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final File file = new File(urlString);           
            
            in = new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {}
        }
        return false;
    }
}
