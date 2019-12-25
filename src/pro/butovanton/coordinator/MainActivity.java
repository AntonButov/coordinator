package pro.butovanton.coordinator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private final int REQUEST_CODE_MEDIAPROJECTION = 100;
    private final int OVERLAY_REQUEST = 101;
    private String STORE_DIRECTORY;
    private MediaProjectionManager mProjectionManager;
    public Intent mdata = null;

    private TextView textView;
    private Button mbuttonОк;

    /****************************************** Activity Lifecycle methods ************************/
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        mbuttonОк = findViewById(R.id.buttonOk);
        mbuttonОк.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // запись на диск.
              //  Intent intentService = new Intent(getApplicationContext(), MyService.class);
             //   startService(intentService);
             //   finish();

                String localUri = "/data/data/pro.butovanton.coordinator/files/coninfo/screenshot.jpg"; //тут уже как хотите так и формируйте путь, хоть через Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + имя файла
                File file = new File(localUri);
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
                Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                openFileIntent.setDataAndTypeAndNormalize(contentUri, "image/*");
                openFileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(openFileIntent);
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
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case (OVERLAY_REQUEST): {

                    break;
                }
                case (REQUEST_CODE_MEDIAPROJECTION): {
                    MyService.setResultData(data);
                    mdata = data;
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
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST);
        }
        if (Settings.canDrawOverlays(this) && mdata == null)
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION);
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