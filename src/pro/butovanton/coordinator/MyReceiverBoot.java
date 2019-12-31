package pro.butovanton.coordinator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiverBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DEBUG","Устройство загрузилось");
        Intent intentboot = new Intent(context, MainActivity.class);
        intentboot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentboot);
    }
}
