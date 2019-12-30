package pro.butovanton.coordinator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
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
    private long maxsdeltatime = 300;

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

    private SharedPreferences msharedPreferences;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) //только для Lolli
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("DEBUG","service oncreate");
        msharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        msharedPreferences.registerOnSharedPreferenceChangeListener(mListener);
        sendNotif(1);
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
                    Log.d("DEBUG", "time: " + event.getEventTime() + ", touch. x=" + event.getX() + ", y=" + event.getY());
                    wm.removeView(detectorMax);
                    //Screenshot----------------------------
                    // create virtual display depending on device width / height
                    startProjection();
                    sendNotif(1);
                    //--------------------------------------
                    Toast toast = Toast.makeText(getApplicationContext(), "x=" + event.getX() + " y=" + event.getY(), Toast.LENGTH_SHORT);
                    toast.show();
                return false;
            }
        });
        detector = new View(this);
        detector.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("DEBUG", "time: "+event.getEventTime()+", touch. x=" + event.getX() + ", y=" + event.getY());
                if (event.getEventTime()-touchtimepass>=mindeltatime && event.getEventTime()-touchtimepass<=maxsdeltatime){
                    touchtimepass = 0;
                    sendNotif(2);
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
                    String filePatch = msharedPreferences.getString("storedirectory","")+"/screenshot" + ".jpg";
                    fos = new FileOutputStream(filePatch);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    IMAGES_PRODUCED++;
                    MediaScannerConnection.scanFile(getBaseContext(), new String[] {filePatch}, null, null);
                    Log.e("DEBUG", "captured image: " + IMAGES_PRODUCED);
                    Log.d("DEBUG", "Rotation = " + mDisplay.getRotation());
                    ExifInterface exif = new ExifInterface(filePatch);
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(mDisplay.getRotation()));
                    exif.saveAttributes();

                   // String localUri = "/data/data/pro.butovanton.coordinator/files/coninfo/screenshot.jpg"; //тут уже как хотите так и формируйте путь, хоть через Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + имя файла
                 //   File file = new File(filePatch);
                 //   Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
                 //   Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                 //   openFileIntent.setDataAndTypeAndNormalize(contentUri, "image/*");
                 //   openFileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                 //   openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 //   startActivity(openFileIntent);
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

    public void sendNotif(int Type) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "7")
                //     .setPriority(NotificationCompat.PRIORITY_HIGH)
                //  .setTimeoutAfter(1)
                .setShowWhen(false);
        // .setLights(10000, 1000,1000)
        builder.setContentTitle(getString(R.string.text_notif));
        switch (Type) {
            case 1:
                builder.setSmallIcon(R.drawable.ic_notif);
                break;
            case 2:
                builder.setSmallIcon(R.drawable.ic_notif2);
                break;
        }
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

    public SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            maxsdeltatime = msharedPreferences.getLong("maxsdeltatime",300);
            Log.d("DEBUG", "A preference has been changed, maxdelta= "+maxsdeltatime);
        }
    };


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
        msharedPreferences.unregisterOnSharedPreferenceChangeListener(mListener);
    }

}


