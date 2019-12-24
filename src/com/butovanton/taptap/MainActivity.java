package com.butovanton.taptap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE = 100;
    private static String STORE_DIRECTORY;
    private MediaProjectionManager mProjectionManager;

    /****************************************** Activity Lifecycle methods ************************/
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
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
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
        Intent intentService = new Intent(this, MyService.class);
        this.startService(intentService);
        }
        Log.d("DEBUG","end chield");
    }

    @Override
    protected void onPause() {
        super.onPause();
      //  finish();
    }

}//+