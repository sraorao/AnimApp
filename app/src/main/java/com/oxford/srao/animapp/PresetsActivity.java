package com.oxford.srao.animapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class PresetsActivity extends AppCompatActivity {
    Uri selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presets);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NumberPicker npBins = findViewById(R.id.npBins);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
        int numBins = prefs.getInt("Bins", 10);
        Log.i(TAG, "Bin width:" + numBins);
        npBins.setMinValue(1);
        npBins.setMaxValue(50);
        npBins.setValue(numBins);
        npBins.setWrapSelectorWheel(true);

        findViewById(R.id.btnRed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("H_MIN", 158);
                editor.putInt("H_MAX", 180);
                editor.putInt("S_MIN", 67);
                editor.putInt("S_MAX", 255);
                editor.putInt("V_MIN", 90);
                editor.putInt("V_MAX", 255);
                editor.apply();
                PresetsActivity.this.startActivity(intent);
            }
        });
        findViewById(R.id.btnBlue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("H_MIN", 111);
                editor.putInt("H_MAX", 154);
                editor.putInt("S_MIN", 0);
                editor.putInt("S_MAX", 255);
                editor.putInt("V_MIN", 0);
                editor.putInt("V_MAX", 154);
                editor.apply();
                PresetsActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnGreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("H_MIN", 50);
                editor.putInt("H_MAX", 75);
                editor.putInt("S_MIN", 67);
                editor.putInt("S_MAX", 255);
                editor.putInt("V_MIN", 90);
                editor.putInt("V_MAX", 255);
                editor.apply();
                PresetsActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnBonW).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("H_MIN", 0);
                editor.putInt("H_MAX", 180);
                editor.putInt("S_MIN", 0);
                editor.putInt("S_MAX", 255);
                editor.putInt("V_MIN", 0);
                editor.putInt("V_MAX", 47);
                editor.apply();
                PresetsActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnWonB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("H_MIN", 0);
                editor.putInt("H_MAX", 180);
                editor.putInt("S_MIN", 0);
                editor.putInt("S_MAX", 255);
                editor.putInt("V_MIN", 90);
                editor.putInt("V_MAX", 255);
                editor.apply();
                PresetsActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnLarva).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("H_MIN", 0);
                editor.putInt("H_MAX", 61);
                editor.putInt("S_MIN", 0);
                editor.putInt("S_MAX", 255);
                editor.putInt("V_MIN", 204);
                editor.putInt("V_MAX", 255);
                editor.apply();
                PresetsActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnCustom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("text/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a config file"), 123);
            }
        });

        // If number picker is changed, set scale factor
        npBins.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("Bins", newVal);
                editor.apply();
            }
        });

    }

    // result from file chooser
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int fileSize = 0;
        String[] content = null;
        List<String[]> csvLine = new ArrayList<>();
        if(requestCode==123 && resultCode==RESULT_OK) {
            selectedFile = data.getData(); //The uri with the location of the file
            Cursor cursor = null;
            try { // get actual name of the video file
                cursor = getContentResolver().query(selectedFile, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String fileDisplayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                }
            } catch(Exception e) {
                Log.i(TAG, "Error getting file info: " +  e.toString());
            } finally {
                cursor.close();
            }


            try {
                InputStream stream = getContentResolver().openInputStream(selectedFile);
                if (!selectedFile.toString().endsWith(".settings.txt")) {
                    throw new FileNotFoundException();
                }
                Log.i(TAG, "selectedfile:" + selectedFile.toString());
                Toast.makeText(getApplicationContext(), "Setting custom HSV values", Toast.LENGTH_SHORT).show();
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                String line = "";
                while((line = br.readLine()) != null){
                    content = line.split(",");
                    Log.i(TAG, "CONFIG file:" + content[5].toString());
                    Intent intent = new Intent(PresetsActivity.this, MainActivity.class);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PresetsActivity.this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("H_MIN", Integer.valueOf(content[0]));
                    editor.putInt("H_MAX", Integer.valueOf(content[1]));
                    editor.putInt("S_MIN", Integer.valueOf(content[2]));
                    editor.putInt("S_MAX", Integer.valueOf(content[3]));
                    editor.putInt("V_MIN", Integer.valueOf(content[4]));
                    editor.putInt("V_MAX", Integer.valueOf(content[5]));
                    editor.apply();
                    PresetsActivity.this.startActivity(intent); //TODO: figure out how to exit this activity properly
                    //finish();
                }
                br.close();
            } catch(Error e){
                Log.i(TAG, "Couldn't read config file!" + e.toString());
            } catch(Exception e) {
                Toast.makeText(getApplicationContext(), "Config file must have the extension .settings.txt", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Couldn't read config file!" + e.toString());
            }

        }


    }

}
