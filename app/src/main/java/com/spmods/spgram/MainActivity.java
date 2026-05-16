package com.spmods.spgram;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SPGram";
    private static String libraryError = null;

    static {
        try {
            System.loadLibrary("tdjni");
            Log.d(TAG, "tdjni loaded OK");
        } catch (Throwable t) {
            libraryError = "loadLibrary FAILED:\n" + t.toString();
            Log.e(TAG, libraryError);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (libraryError != null) {
            new AlertDialog.Builder(this)
                .setTitle("Library Error")
                .setMessage(libraryError)
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        try {
            initTelegram();
        } catch (Throwable t) {
            new AlertDialog.Builder(this)
                .setTitle("Init Error")
                .setMessage(t.toString())
                .setPositiveButton("OK", null)
                .show();
        }
    }

    private void initTelegram() {
        org.drinkless.tdlib.Client client = org.drinkless.tdlib.Client.create(object -> {
            if (object instanceof org.drinkless.tdlib.TdApi.UpdateAuthorizationState) {
                Log.d(TAG, "Auth state: " + object);
            }
        }, null, null);

        org.drinkless.tdlib.TdApi.SetTdlibParameters params = new org.drinkless.tdlib.TdApi.SetTdlibParameters();
        params.apiId = BuildVars.API_ID;
        params.apiHash = BuildVars.API_HASH;
        params.systemLanguageCode = "en";
        params.deviceModel = "Android";
        params.applicationVersion = BuildVars.VERSION;
        params.databaseDirectory = getFilesDir().getAbsolutePath();

        client.send(params, result -> Log.d(TAG, "TDLib params set: " + result));
    }
}
