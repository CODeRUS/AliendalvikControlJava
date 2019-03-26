package org.coderus.aliendalvikcontrol;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class SharingActivity extends Activity {
    private static final String TAG = "SharingActivity";

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

        final Uri fileUrl = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        String fileName = fileUrl == null ? new String() : fileUrl.toString();
        String data = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
        if (data == null) {
            data = new String();
        }
        final String mimeType = receivedIntent.getType();

        JSONObject json = new JSONObject();
        try {
            json.put("command", "share");
            JSONObject shareIntent = new JSONObject();
            shareIntent.put("mimeType", mimeType);
            shareIntent.put("data", data);
            shareIntent.put("fileName", fileName);
            json.put("shareIntent", shareIntent);
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
