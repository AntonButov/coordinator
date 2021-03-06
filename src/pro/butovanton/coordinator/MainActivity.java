package pro.butovanton.coordinator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private final int REQUEST_CODE_MEDIAPROJECTION = 100;
    private final int OVERLAY_REQUEST = 101;
    private final int STORAGE_REQUEST = 102;

    private String STORE_DIRECTORY;
    private MediaProjectionManager mProjectionManager;
    public Intent mdata = null;
    private SeekBar mseekBar;

    private TextView textView, textViewSeekBar, textViewPatch;
    private Button mbuttonОк;

    File storeDirectory;
    private SharedPreferences msharedPreferences;
    /****************************************** Activity Lifecycle methods ************************/

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textViewSeekBar = findViewById(R.id.textseekBar);
        textViewPatch = findViewById(R.id.textViewPatch);
        mbuttonОк = findViewById(R.id.buttonOk);
        mbuttonОк.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // запись на диск.
             //   Intent intentService = new Intent(getApplicationContext(), MyService.class);
             //   startService(intentService);
                finish();
            }
        });
        mseekBar = findViewById(R.id.seekBar);
        mseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewSeekBar.setText("Время между тапами, мс. :"+mseekBar.getProgress());
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        PackageInfo packageInfo;

        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
            String ver = packageInfo.versionName ;
            textView.setText("Версия программы: " + ver);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        msharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        storeDirectory = Environment.getExternalStoragePublicDirectory("/Coninfo/");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case (OVERLAY_REQUEST): {

                    break;
                }
                case (REQUEST_CODE_MEDIAPROJECTION): {
                    MyService.setResultData(data);
                    mdata = data;
                    if (storeDirectory != null) { //File.c
                        STORE_DIRECTORY = storeDirectory.getPath();
                        SharedPreferences.Editor editor = msharedPreferences.edit();
                        editor.putString("storedirectory",STORE_DIRECTORY);
                        editor.commit();

                        if (!storeDirectory.exists()) {
                            boolean success = storeDirectory.mkdirs();

                            if (!success) {
                                Log.e("DEBUG", "failed to create file storage directory.");
                                return;
                            } else {
                                MediaScannerConnection.scanFile(this, new String[] {storeDirectory.toString()}, null, null);
                            }

                        }
                    } else {
                        Log.e("DEBUG", "failed to create file storage directory, getExternalFilesDir is null.");
                        return;
                    }
                    if (resultCode == RESULT_OK) {
                        Intent intentService = new Intent(this, MyService.class);
                        this.startService(intentService);
                        //finish();
                    } else mFinish();
                    break;
                }
            }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("DEBUG", "onResume");
        mseekBar.setProgress((int) msharedPreferences.getLong("maxsdeltatime",300));
        textViewSeekBar.setText("Время между тапами, мс. :"+mseekBar.getProgress());
        textViewPatch.setText(STORE_DIRECTORY);
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_REQUEST);
        }
        if (Settings.canDrawOverlays(this) && mdata == null &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case STORAGE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    mFinish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = msharedPreferences.edit();
        editor.putLong("maxsdeltatime", mseekBar.getProgress());
       // editor.putString("storedirectory",STORE_DIRECTORY);
        editor.commit();
    }


    private void mFinish() {
        Toast toast = Toast.makeText(getApplicationContext(), "Без этого разрешения программа не сможет работать.", Toast.LENGTH_LONG);
        toast.show();
        finish();
    }
}//+