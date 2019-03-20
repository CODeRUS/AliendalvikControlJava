package org.coderus.aliendalvikcontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private Intent receivedIntent = null;

    @Override
    protected void onStart() {
        super.onStart();

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receivedIntent = getIntent();
        Log.w(TAG, receivedIntent.toString());

        String command = receivedIntent.getStringExtra("command");
        Log.w(TAG, "Command: " + command);

        final Context context = getApplicationContext();

        // clean intent
        receivedIntent.removeExtra("command");
        receivedIntent.setComponent(null);

        JSONObject json = new JSONObject();
        try {
            json.put("command", command);
            switch (command) {
                case "sharing":
                    final Uri fileUrl = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                    String fileName = fileUrl == null ? new String() : fileUrl.toString();
                    String data = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
                    if (data == null) {
                        data = new String();
                    }
                    final String mimeType = receivedIntent.getType();

                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(receivedIntent, 0);
                    JSONArray array = new JSONArray();
                    for (ResolveInfo resolveInfo : resolveInfoList) {
                        final String packageName = resolveInfo.activityInfo.packageName;
                        switch (packageName) {
                            case "com.android.bluetooth":
                            case "com.myriadgroup.nativeapp.email":
                            case "com.myriadgroup.nativeapp.messages":
                                continue;
                            default:
                                break;

                        }


                        JSONObject resolveObject = new JSONObject();
                        resolveObject.put("packageName", packageName);

                        final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                        resolveObject.put("launcherClass", launchIntent.getComponent().getClassName());

                        resolveObject.put("mimeType", mimeType);
                        resolveObject.put("fileName", fileName);
                        resolveObject.put("data", data);

                        final String label = resolveInfo.loadLabel(pm).toString();
                        Log.w(TAG, "Resolved application: " + packageName + " label: " + label);
                        resolveObject.put("prettyName", label);

                        final String className = resolveInfo.activityInfo.name;
                        resolveObject.put("className", className);

                        Drawable icon = resolveInfo.loadIcon(pm);
                        final Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        final Canvas canvas = new Canvas(bmp);
                        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        icon.draw(canvas);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        final String iconString = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
                        resolveObject.put("icon", iconString);

                        array.put(resolveObject);
                    }
                    json.put("candidates", array);

                    break;
                case "deviceInfo":
                    JSONObject deviceProperties = new JSONObject();

                    Resources resources = context.getResources();
                    final int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                    int navbarHeight = 0;
                    if (resourceId > 0) {
                        navbarHeight = resources.getDimensionPixelSize(resourceId);
                    }

                    deviceProperties.put("navbarHeight", navbarHeight);

                    json.put("deviceProperties", deviceProperties);
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String result = json.toString();
        Log.w(TAG, "Result: " + result);
        String ret = Native.reply(result);
        Log.w(TAG, "Native result: " + ret);

        finish();
    }
}

class Native {
    static {
        System.loadLibrary("native-lib");
    }
    static native String reply(String data);
}
