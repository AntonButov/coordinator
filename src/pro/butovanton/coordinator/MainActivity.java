package pro.butovanton.coordinator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private final int REQUEST_CODE_MEDIAPROJECTION = 100;
    private final int OVERLAY_REQUEST = 101;
    private String STORE_DIRECTORY;
    private MediaProjectionManager mProjectionManager;

    /****************************************** Activity Lifecycle methods ************************/
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start projection
        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
               // startProjection();
            }
        });

        // stop projection
        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
               // stopProjection();
            }
        });

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST);
        }
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case (OVERLAY_REQUEST): {

                    break;
                }
                case (REQUEST_CODE_MEDIAPROJECTION): {
                    MyService.setResultData(data);
                    File externalFilesDir = getExternalFilesDir(null);
                    if (externalFilesDir != null) {
                        STORE_DIRECTORY = getFilesDir() + "/coninfo/";
                        File storeDirectory = new File(STORE_DIRECTORY);
                        if (!storeDirectory.exists()) {
                            boolean success = storeDirectory.mkdirs();
                            if (!success) {
                                Log.e("DEBUG", "failed to create file storage directory.");
                                return;
                            }
                        }
                    } else {
                        Log.e("DEBUG", "failed to create file storage directory, getExternalFilesDir is null.");
                        return;
                    }
                    if (Settings.canDrawOverlays(this)) {
                        Intent intentService = new Intent(this, MyService.class);
                        this.startService(intentService);
                    }
                        else mFinish();
                    break;

                }
            }
        }else {
           mFinish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
      //  finish();
    }

    private void mFinish() {
        Toast toast = Toast.makeText(getApplicationContext(), "Без этого разрешения программа не сможет работать.", Toast.LENGTH_LONG);
        toast.show();
        finish();
    }
}//+