package com.spmods.spgram;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.*;

public class CrashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String crashLog = getIntent().getStringExtra("crash_log");
        if (crashLog == null) crashLog = "No crash info available.";

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("SPGram Crashed");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        final ScrollView scroll = new ScrollView(this);
        final TextView logView = new TextView(this);
        logView.setText(crashLog);
        logView.setTextSize(11);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scroll.addView(logView);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        layout.addView(scroll, scrollParams);

        final String finalLog = crashLog;
        Button copyBtn = new Button(this);
        copyBtn.setText("Copy Log");
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("crash_log", finalLog));
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();
        });
        layout.addView(copyBtn);

        setContentView(layout);
    }
}
