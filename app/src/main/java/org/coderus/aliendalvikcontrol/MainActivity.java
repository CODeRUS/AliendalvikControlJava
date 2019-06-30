package org.coderus.aliendalvikcontrol;
import org.coderus.aliendalvikcontrol.BuildConfig;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onStart() {
        super.onStart();

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent receivedIntent = getIntent();
        Log.w(TAG, receivedIntent.toString());

        String command = receivedIntent.getStringExtra("command");
        Log.w(TAG, "Command: " + command);

        // clean intent
        receivedIntent.removeExtra("command");
        receivedIntent.setComponent(null);

        JSONObject json = new JSONObject();
        try {
            json.put("command", command);
            switch (command) {
                case "sharing": {
                    json.put("candidates", getSharingCandidates(receivedIntent));
                    break;
                }
                case "deviceInfo": {
                    json.put("deviceProperties", getDeviceProperties());
                    break;
                }
                case "launchApp": {
                    json.put("launchParameters", getLaunchParameters(receivedIntent));
                    break;
                }
                case "selector": {
                    final Uri url = receivedIntent.getData();
                    json.put("candidates", getSelectorCandidates(url));
                    json.put("url", url.toString());
                    break;
                }
                case "uri": {
                    final Uri url = receivedIntent.getData();
                    json.put("default", getDefaultApplication(url));
                    break;
                }
                case "uptime": {
                    json.put("payload", receivedIntent.getStringExtra("payload"));
                    json.put("value", SystemClock.uptimeMillis());
                    break;
                }
                default: {
                    Log.w(TAG, "Unhandled command type (old helper version?): " + command);
                    break;
                }
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

    private JSONArray getSharingCandidates(Intent receivedIntent) throws JSONException {
        final Uri fileUrl = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        String fileName = fileUrl == null ? new String() : fileUrl.toString();
        String data = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
        if (data == null) {
            data = new String();
        }
        final String mimeType = receivedIntent.getType();

        PackageManager pm = getPackageManager();
        final List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(receivedIntent, 0);
        JSONArray array = new JSONArray();
        for (final ResolveInfo resolveInfo : resolveInfoList) {
            final String packageName = resolveInfo.activityInfo.packageName;
            switch (packageName) {
                case "org.coderus.aliendalvikcontrol":
                case "com.android.bluetooth":
                case "com.myriadgroup.nativeapp":
                case "com.myriadgroup.nativeapp.email":
                case "com.myriadgroup.nativeapp.messages":
                    continue;
                default:
                    break;

            }

            final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                continue;
            }

            JSONObject resolveObject = new JSONObject();
            resolveObject.put("packageName", packageName);

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
        return array;
    }

    private JSONObject getDeviceProperties() throws JSONException {
        JSONObject deviceProperties = new JSONObject();

        final Context context = getApplicationContext();
        Resources resources = context.getResources();
        final int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int navbarHeight = 0;
        if (resourceId > 0) {
            navbarHeight = resources.getDimensionPixelSize(resourceId);
        }

        deviceProperties.put("navbarHeight", navbarHeight);
        deviceProperties.put("api", Build.VERSION.SDK_INT);
        deviceProperties.put("versionCode", BuildConfig.VERSION_CODE);
        deviceProperties.put("versionName", BuildConfig.VERSION_NAME);

        return deviceProperties;
    }

    private JSONObject getLaunchParameters(Intent receivedIntent) throws JSONException {
        final String packageName = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
        if (packageName == null) {
            return new JSONObject();
        }
        JSONObject launchParameters = new JSONObject();
        PackageManager pm = getPackageManager();
        final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        launchParameters.put("packageName", packageName);
        launchParameters.put("className", launchIntent.getComponent().getClassName());

        return launchParameters;
    }

    private JSONArray getSelectorCandidates(Uri url) throws JSONException {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setData(url);
        PackageManager pm = getPackageManager();
        final List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(view, 0);
        JSONArray array = new JSONArray();
        for (final ResolveInfo resolveInfo : resolveInfoList) {
            final String packageName = resolveInfo.activityInfo.packageName;
//            switch (packageName) {
//                case "org.coderus.aliendalvikcontrol":
//                case "com.android.bluetooth":
//                case "com.myriadgroup.nativeapp":
//                case "com.myriadgroup.nativeapp.email":
//                case "com.myriadgroup.nativeapp.messages":
//                    continue;
//                default:
//                    break;
//
//            }

            JSONObject resolveObject = new JSONObject();
            resolveObject.put("packageName", packageName);

            Intent pkIntent = new Intent();
            pkIntent.setPackage(packageName);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(pkIntent, 0);
            for (ResolveInfo resolveInfoz : resolveInfos) {
                Log.w(TAG, "Resolved intent: " + resolveInfoz.activityInfo.name);
            }

            final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                resolveObject.put("launcherClass", new String());
            } else {
                resolveObject.put("launcherClass", launchIntent.getComponent().getClassName());
            }

            resolveObject.put("data", url.toString());

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
        return array;
    }

    private JSONObject getDefaultApplication(Uri url) throws JSONException {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setData(url);
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PackageManager pm = getPackageManager();
        final ResolveInfo resolveInfo = pm.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY);
        String packageName = resolveInfo.activityInfo.packageName;
        String className = resolveInfo.activityInfo.name;
        Log.w(TAG, "Resolved intent: " + className);
        if (className.equals("com.android.internal.app.ResolverActivity")) {
            final List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY);
            for (final ResolveInfo defaultResolveInfo : resolveInfoList) {
                final String resolvePackageName = defaultResolveInfo.activityInfo.packageName;
                switch (resolvePackageName) {
                    case "org.coderus.aliendalvikcontrol":
                    case "com.android.bluetooth":
                    case "com.myriadgroup.nativeapp":
                    case "com.myriadgroup.nativeapp.browser":
                    case "com.myriadgroup.nativeapp.email":
                    case "com.myriadgroup.nativeapp.messages":
                        continue;
                    default:
                        break;

                }
                packageName = resolvePackageName;
                className = defaultResolveInfo.activityInfo.name;
                Log.w(TAG, "First default intent: " + className);
                break;
            }
        }
        JSONObject defaultObject = new JSONObject();
        defaultObject.put("data", url.toString());
        defaultObject.put("className", className);
        defaultObject.put("packageName", packageName);
        final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            defaultObject.put("launcherClass", new String());
        } else {
            defaultObject.put("launcherClass", launchIntent.getComponent().getClassName());
        }
        return defaultObject;
    }
}
