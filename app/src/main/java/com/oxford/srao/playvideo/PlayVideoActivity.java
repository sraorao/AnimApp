package com.oxford.srao.playvideo;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PlayVideoActivity extends AppCompatActivity {
    public String selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);
        selectedFile = getIntent().getStringExtra("selectedFile");

    }

    public void onClickPlay(View v) {
        Toast.makeText(getApplicationContext(), selectedFile, Toast.LENGTH_LONG).show();
        TextView textv = (TextView) findViewById(R.id.textView);
        textv.setText(selectedFile.toString());
    }

}
