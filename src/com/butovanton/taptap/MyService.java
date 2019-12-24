package com.butovanton.taptap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MyService extends Service {


    public MyService() {
    }

    private WindowManager wm;
    private View detector;
    private View detectorMax;
    private long touchtimepass = 0;
    private long mindeltatime = 100;
    private long maxsdeltatime = 200;
    private Toast toast;

    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection = null;
    private ImageReader mImageReader;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    public static Intent ResulData;
    private MediaProjectionManager mProjectionManager = null;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) //только для Lolli
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("DEBUG","service oncreate");
        sendNotif();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0,
                0,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // display metrics
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDisplay = wm.getDefaultDisplay();
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;
        //detextorMax
        detectorMax = new View(this);
        detectorMax.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("DEBUG", "Обрабатываем координаты");
                Log.d("DEBUG", "time: "+event.getEventTime()+", touch. x=" + event.getX() + ", y=" + event.getY());
                wm.removeView(detectorMax);
                toast.cancel();
                //Screenshot----------------------------
                // create virtual display depending on device width / height
                startProjection();

                //--------------------------------------
                Toast toast2 = toast.makeText(getApplicationContext(),"x="+event.getX()+" y="+event.getY(),Toast.LENGTH_SHORT);
                toast2.show();
                return false;
            }
        });
        detector = new View(this);
        detector.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("DEBUG", "time: "+event.getEventTime()+", touch. x=" + event.getX() + ", y=" + event.getY());
                if (event.getEventTime()-touchtimepass>=mindeltatime && event.getEventTime()-touchtimepass<=maxsdeltatime){
                    toast = Toast.makeText(getApplicationContext(),"Укажите координаты",Toast.LENGTH_SHORT);
                    toast.show();
                    touchtimepass = 0;
                    Log.d("DEBUG", "give koordinate");
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            mWidth,
                            mHeight,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                            PixelFormat.TRANSLUCENT);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                    else params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                    wm.addView(detectorMax, params);
                } else
                    touchtimepass = event.getEventTime();
                return false;
            }
        });
        wm.addView(detector, params);

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mDensity = metrics.densityDpi;
        // get width and height
        mDisplay.getSize(size);
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 1);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        if (sMediaProjection == null) {
            IMAGES_PRODUCED = 0;
            // start capture reader
            sMediaProjection = mProjectionManager.getMediaProjection(-1, ResulData);
            mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, null);
        }
        Log.d("DEBUG","Start father");

    }

    private void stopProjection() {
        if (sMediaProjection != null) {
            sMediaProjection.stop();
        }
        sMediaProjection = null;

        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            stopProjection();
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null && IMAGES_PRODUCED == 0) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    // write bitmap to a file
                    fos = new FileOutputStream(getFilesDir() + "/coninfo/" + IMAGES_PRODUCED + ".jpg", true);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    IMAGES_PRODUCED++;
                    Log.e("DEBUG", "captured image: " + IMAGES_PRODUCED);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
            }
        }
    }

    public static void setResultData(Intent data){
    MyService.ResulData = data;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void sendNotif() {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "7")
                //     .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_notif)
                //  .setTimeoutAfter(1)
                .setShowWhen(false);
        // .setLights(10000, 1000,1000)
        builder.setContentTitle(getString(R.string.text_notif));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //  .setContentText("Much longer text that cannot fit one line...")
            // .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
            builder.setAutoCancel(true);
        }
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        startForeground(444, notification);
    }

    private  void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.text_notif);
            // String description = "Служба Важный звонок";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("7", name, importance);
            //channel.setDescription("???");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            channel.setShowBadge(false);
            //      channel.enableLights(true);
            //      channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
            Log.d("DEBUG","Создаем канал уведомлений");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DEBUG","service onstart");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DEBUG",  "service onDestroy");
        wm.removeView(detector);
        if (detectorMax != null) wm.removeView(detectorMax);

        if (sMediaProjection != null) {
            sMediaProjection.stop();
            sMediaProjection = null;
        }
    }

}


