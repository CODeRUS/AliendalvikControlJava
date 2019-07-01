package org.coderus.aliendalvikcontrol;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
        final String fileScheme = fileUrl.getScheme();
        Log.w(TAG, fileScheme);
        String fileName = fileUrl == null ? new String() : fileUrl.toString();
        final String mimeType = receivedIntent.getType();
        String data = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
        final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (data == null) {
            data = "temp-share";
        }
        if (fileScheme.equals("content")) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUrl);

                File file = new File(getFilesDir(), data + "." + extension);
                OutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[4 * 1024]; // or other buffer size
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                output.flush();
                output.close();

                fileName = file.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
