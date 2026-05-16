package com.spmods.spgram;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class MainActivity extends AppCompatActivity {

    private Client client;
    private static final String TAG = "SPGram";
    private static String libraryError = null;

    static {
        try {
            System.loadLibrary("tdjni");
        } catch (Throwable t) {
            libraryError = t.toString() + "\n\n" + Log.getStackTraceString(t);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.init(this);
        setContentView(R.layout.activity_main);

        if (libraryError != null) {
            new AlertDialog.Builder(this)
                .setTitle("Library Load Error")
                .setMessage(libraryError)
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        initTelegram();
    }

    private void initTelegram() {
        client = Client.create(object -> {
            if (object instanceof TdApi.UpdateAuthorizationState) {
                handleAuth(((TdApi.UpdateAuthorizationState) object).authorizationState);
            }
        }, null, null);

        TdApi.SetTdlibParameters params = new TdApi.SetTdlibParameters();
        params.apiId = BuildVars.API_ID;
        params.apiHash = BuildVars.API_HASH;
        params.systemLanguageCode = "en";
        params.deviceModel = "Android";
        params.applicationVersion = BuildVars.VERSION;
        params.databaseDirectory = getFilesDir().getAbsolutePath();

        client.send(params, result -> Log.d(TAG, "TDLib ready"));
    }

    private void handleAuth(TdApi.AuthorizationState state) {
        if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            Log.d(TAG, "Waiting for phone number");
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            Log.d(TAG, "Waiting for OTP");
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            Log.d(TAG, "Logged in!");
        }
    }
}
