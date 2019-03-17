package org.coderus.aliendalvikcontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private Intent receivedIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receivedIntent = getIntent();
        Log.w(TAG, receivedIntent.toString());

        String command = receivedIntent.getStringExtra("command");
        Log.w(TAG, "Command: " + command);

        // clean intent
        receivedIntent.removeExtra("command");
        receivedIntent.setComponent(null);

        String result = command;

        switch (command) {
            case "sharing":
                PackageManager pm = getPackageManager();
                List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(receivedIntent, 0);
                for (ResolveInfo resolveInfo : resolveInfoList) {
                    Log.w(TAG, resolveInfo.loadLabel(pm).toString());
                }
                if (resolveInfoList.isEmpty()) {
                    result = "empty";
                } else {
                    result = resolveInfoList.get(0).loadLabel(pm).toString();
                }
                break;
            default:
                break;
        }

        Log.w(TAG, "Result: " + result);
        String ret = Native.reply(result);
        Log.w(TAG, "Return: " + ret);

        finish();
    }
}

class Native {
    static {
        System.loadLibrary("native-lib");
    }
    static native String reply(String data);
}
