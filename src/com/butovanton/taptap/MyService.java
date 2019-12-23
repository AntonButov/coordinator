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

    private static Intent mResultData;
    private Display mDisplay;
    private int mWidth;
    private int mHeight;

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
    }

    public static void setResultData(Intent ResultData) {
        MyService.mResultData = ResultData;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startProjection() {
        Log.d("DEBUG", "startProgection");

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
    }

}


