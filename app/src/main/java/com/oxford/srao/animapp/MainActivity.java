package com.oxford.srao.animapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import io.apptik.widget.MultiSlider;

import static android.content.ContentValues.TAG;
import static org.bytedeco.javacpp.opencv_core.CV_32SC4;
import static org.bytedeco.javacpp.opencv_core.inRange;
import static org.bytedeco.javacpp.opencv_imgproc.CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvMoments;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.drawContours;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.minEnclosingCircle;
import static org.bytedeco.javacpp.opencv_imgproc.moments;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class MainActivity extends AppCompatActivity {

    private static InputStream stream;
    Uri selectedFile;
    Mat grabbedMatFrame;
    Mat matFrame;
    ImageView img;
    TextView tvoutput;
    SeekBar seekBar;
    String fileDisplayName = "";
    int H_MIN;
    int S_MIN;
    int V_MIN;
    int H_MAX;
    int S_MAX;
    int V_MAX;
    Size newsize;
    Point startPt = new Point(0, 0);
    Point endPt = new Point(0, 0);
    float scaleFactor = 1;
    float resizeFactor = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize HSV settings from previous run
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        H_MIN = prefs.getInt("H_MIN", 0);
        S_MIN = prefs.getInt("S_MIN", 0);
        V_MIN = prefs.getInt("V_MIN", 0);
        H_MAX = prefs.getInt("H_MAX", 180);
        S_MAX = prefs.getInt("S_MAX", 255);
        V_MAX = prefs.getInt("V_MAX", 30);
        Log.i(TAG, "prefs: " + H_MIN + "," + H_MAX + "," + S_MIN + "," + S_MAX + "," + V_MIN + "," + V_MAX );

        // find views and initialize
        img = findViewById(R.id.image_view);
        final Switch switchCropVideo = findViewById(R.id.switchCropVideo);
        final NumberPicker npMeasurement = findViewById(R.id.npMeasurement);
        npMeasurement.setMinValue(0);
        npMeasurement.setMaxValue(50);
        npMeasurement.setWrapSelectorWheel(true);
        final Switch switchPlayVideo = findViewById(R.id.switchPlayVideo);
        tvoutput = findViewById(R.id.output);
        seekBar = findViewById(R.id.seekBar);

        // initialise options menu


        // Request read/write permissions from user
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // When "Open video" button is clicked, open file dialog
        findViewById(R.id.btnParseVideo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                seekBar.setProgress(0); // reset seekbar
                //performFileSearch();
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
            }
        });


        // seekbar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvoutput.setText(String.valueOf(progress));
                Log.i(TAG, "seekBar progress:" + String.valueOf(progress));
                try {
                    stream = getContentResolver().openInputStream(selectedFile);
                    //Toast.makeText(getApplicationContext(), selectedFile.toString(), Toast.LENGTH_SHORT).show();
                    grabFirstFrame(stream);
                } catch(Error e){
                    Log.i(TAG, "File seeking failed!" + e.toString());
                } catch(Exception e) {
                    //seekBar.setMax(progress - 1);
                    Log.i(TAG, "File seeking failed!" + e.toString());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // Process click-drag for rectangle selection inside imageview
        img.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch(action){
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "ACTION_DOWN- " + x + " : " + y);
                        startPt = projectXY((ImageView)v, grabbedMatFrame, x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        endPt = projectXY((ImageView)v, grabbedMatFrame, x, y);
                        //drawOnRectProjectedBitMap((ImageView)v, grabbedMatFrame, x, y);
                        drawRectangle(grabbedMatFrame);
                        //Log.i(TAG,"ACTION_MOVE- " + x + " : " + y);
                        //drawOnRectProjectedBitMap((ImageView)v, grabbedMatFrame, x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG,"ACTION_UP- " + x + " : " + y);
                        endPt = projectXY((ImageView)v, grabbedMatFrame, x, y);
                        //drawOnRectProjectedBitMap((ImageView)v, grabbedMatFrame, x, y);
                        drawRectangle(grabbedMatFrame);
                        break;
                }

                float screenDistance =  Math.abs(startPt.y() - endPt.y());
                if (screenDistance > 0) {
                    scaleFactor = npMeasurement.getValue()/screenDistance;
                }
                /*
                 * Return 'true' to indicate that the event have been consumed.
                 * If auto-generated 'false', your code can detect ACTION_DOWN only,
                 * cannot detect ACTION_MOVE and ACTION_UP.
                 */
                //updateImage(grabbedMatFrame);
                return true;
            }});

        // Go to next screen if Next is clicked; also send HSV values, crop and threshold settings to next activity
        findViewById(R.id.btnScreen1Next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayVideoImgActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                if (selectedFile == null) {
                    Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();
                } else {
                    intent.putExtra("uri", selectedFile.toString());
                    intent.putExtra("Hmin", H_MIN);
                    intent.putExtra("Smin", S_MIN);
                    intent.putExtra("Vmin", V_MIN);
                    intent.putExtra("Hmax", H_MAX);
                    intent.putExtra("Smax", S_MAX);
                    intent.putExtra("Vmax", V_MAX);
                    intent.putExtra("fileDisplayName", fileDisplayName);
                    Switch switchShowThreshold = (Switch) findViewById(R.id.switchShowThreshold);
                    intent.putExtra("isChecked", switchShowThreshold.isChecked());
                    if (switchCropVideo.isChecked()) {
                        intent.putExtra("startX", startPt.x());
                        intent.putExtra("startY", startPt.y());
                        intent.putExtra("endX", endPt.x());
                        intent.putExtra("endY", endPt.y());
                    }
                    intent.putExtra("scaleFactor", scaleFactor);
                    intent.putExtra("width", newsize.width());
                    intent.putExtra("height", newsize.height());
                    intent.putExtra("playVideo", switchPlayVideo.isChecked());
                    intent.putExtra("resizeFactor", resizeFactor);
                    // write config file in Downloads folder
                    String configText = H_MIN + "," + H_MAX + "," + S_MIN + "," + S_MAX + "," + V_MIN + "," + V_MAX;
                    writeTEXT(configText, fileDisplayName + ".settings.txt", MainActivity.this);
                    MainActivity.this.startActivity(intent);
                }
            }
        });

        // Go to GraphActivity if Analyse button is clicked; also send scale factor data to activity
        findViewById(R.id.btnAnalyse).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (selectedFile == null) {
                    Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, GraphActivity.class);
                    intent.putExtra("fileDisplayName", fileDisplayName);
                    intent.putExtra("framewidth", newsize.width());
                    intent.putExtra("frameheight", newsize.height());
                    intent.putExtra("scaleFactor", scaleFactor);
                    MainActivity.this.startActivity(intent);
                }
            }
        });

        // if Threshold switch is flipped, update imageview
        final Switch switchShowThreshold = (Switch) findViewById(R.id.switchShowThreshold);
        switchShowThreshold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (grabbedMatFrame == null) {
                    Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_SHORT).show();
                    switchShowThreshold.setChecked(false);
                } else {
                    updateImage(grabbedMatFrame);
                }
            }
        });


        // Initialize multisliders for HSV
        MultiSlider mrbHue = findViewById(R.id.mrbHue);
        MultiSlider mrbSat = findViewById(R.id.mrbSat);
        MultiSlider mrbVal = findViewById(R.id.mrbVal);

        mrbHue.setMax(179);
        mrbHue.setMin(0);
        mrbSat.setMax(255);
        mrbSat.setMin(0);
        mrbVal.setMax(255);
        mrbVal.setMin(0);

        // Initialize thumbs at previously saved locations (first remove all thumbs, then add them
        // at desired locations
        mrbHue.removeThumb(0);
        mrbHue.removeThumb(0);
        mrbHue.addThumbOnPos(0, H_MIN);
        mrbHue.addThumbOnPos(1, H_MAX);

        mrbSat.removeThumb(0);
        mrbSat.removeThumb(0);
        mrbSat.addThumbOnPos(0, S_MIN);
        mrbSat.addThumbOnPos(1, S_MAX);

        mrbVal.removeThumb(0);
        mrbVal.removeThumb(0);
        mrbVal.addThumbOnPos(0, V_MIN);
        mrbVal.addThumbOnPos(1, V_MAX);

        // if HSV multisliders change, update image with new HSV values
        mrbHue.setOnThumbValueChangeListener(new MultiSlider.OnThumbValueChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider,
                                       MultiSlider.Thumb thumb,
                                       int thumbIndex,
                                       int value)
            {
                if (thumbIndex == 0) {
                    H_MIN = value;
                    tvoutput.setText("H_MIN: " + value);
                } else {
                    H_MAX = value;
                    tvoutput.setText("H_MAX: " + value);
                }
                if (selectedFile == null) {
                    //Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();
                } else {
                    updateImage(grabbedMatFrame);
                }
            }
        });

        mrbSat.setOnThumbValueChangeListener(new MultiSlider.OnThumbValueChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider,
                                       MultiSlider.Thumb thumb,
                                       int thumbIndex,
                                       int value)
            {
                if (thumbIndex == 0) {
                    S_MIN = value;
                    tvoutput.setText("S_MIN: " + value);
                } else {
                    S_MAX = value;
                    tvoutput.setText("S_MAX: " + value);
                }
                if (selectedFile == null) {
                    //Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();
                } else {
                    updateImage(grabbedMatFrame);
                }
            }
        });

        mrbVal.setOnThumbValueChangeListener(new MultiSlider.OnThumbValueChangeListener() {
            @Override
            public void onValueChanged(MultiSlider multiSlider,
                                       MultiSlider.Thumb thumb,
                                       int thumbIndex,
                                       int value)
            {
                if (thumbIndex == 0) {
                    V_MIN = value;
                    tvoutput.setText("V_MIN: " + value);
                } else {
                    V_MAX = value;
                    tvoutput.setText("V_MAX: " + value);
                }
                if (selectedFile == null) {
                    //Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG).show();
                } else {
                    updateImage(grabbedMatFrame);
                }
            }
        });

        // If number picker is changed, set scale factor
        npMeasurement.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                float screenDistance =  Math.abs(startPt.y() - endPt.y());
                if (screenDistance > 0) {
                    scaleFactor = newVal/screenDistance;
                }
            }
        });

    }

    // result from file chooser
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int fileSize = 0;
        if(requestCode==123 && resultCode==RESULT_OK) {
            selectedFile = data.getData(); //The uri with the location of the file
            Cursor cursor = null;
            try { // get actual name of the video file
                cursor = getContentResolver().query(selectedFile, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    fileDisplayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    fileSize = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)))/(1024*1024);

                    Log.i(TAG, "display name: " + fileDisplayName + ":" + fileSize + "MB");
                    tvoutput.setText(fileDisplayName);
                }
            } catch(Exception e) {
                Log.i(TAG, "Error getting file info: " +  e.toString());
            } finally {
                cursor.close();
            }

            // User information if file size of video is > 80 MB
            if (fileSize > 80) {
                Log.i(TAG, "file is too big!");
                selectedFile = null; //reset this so that btnAnalyse.onClick does not look for uri that does not exist
                // setup the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Large file");
                builder.setMessage("This file is too big to load. File size can be reduced by turning off audio recording and recording at 640 x 480 resolution. File resolution can also be reduced or audio stripped using an external application like ffmpeg.");

                // add a button
                builder.setPositiveButton("OK", null);

                // create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
                return;
            }
            try {
                stream = getContentResolver().openInputStream(selectedFile);
                Toast.makeText(getApplicationContext(), selectedFile.toString(), Toast.LENGTH_SHORT).show();
                grabFirstFrame(stream);
            } catch(Error e){
                Log.i(TAG, "This file is too big!" + e.toString());
            } catch(Exception e) {
                Log.i(TAG, "Something went wrong with reading video file!" + e.toString());
            }

        }
    }
    public static InputStream getInStream() {
        return stream;
    }

    // fetch first frame of the video
    private void grabFirstFrame(InputStream stream) throws
            FrameGrabber.Exception,
            FrameRecorder.Exception,
            IOException, NullPointerException {
        Log.i(TAG, "stream:" + stream.toString());

        FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(stream);
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

        Frame frame;
        videoGrabber.start();
        int count = 0;
        int length_frames = videoGrabber.getLengthInVideoFrames();

        videoGrabber.setFrameNumber(seekBar.getProgress()); // set frame number to current seekbar location
        frame = videoGrabber.grabFrame();

        seekBar.setMax(length_frames - 2); // update seekbar max value to total num frames;


        while (count < length_frames) { // keep getting new frame until it contains data or until the end of the video
            frame = videoGrabber.grabFrame();
            if (matConverter.convert(frame.clone()) != null) {
                break;
            }
            count++;
        }

        Log.i(TAG, "frame length:" + length_frames + "current frame: " + count);
        if (frame == null) {
            Log.i(TAG, "frame is null for some reason!");
        }

        //grabbedMatFrame = matConverter.convert(frame);
        grabbedMatFrame = matConverter.convert(frame.clone());
        //Log.i(TAG, "grabbedmatframe:" + grabbedMatFrame.empty());
        if (grabbedMatFrame == null) {
            Log.i(TAG, "grabbedMatFrame is null for some reason!");
        }
        Log.i(TAG, "before printing frame");
        Log.i(TAG, "frame:" + grabbedMatFrame.toString());

        float aspectRatioInverse = (float) videoGrabber.getImageHeight()/videoGrabber.getImageWidth();
        float aspectRatio = (float) videoGrabber.getImageWidth()/videoGrabber.getImageHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels/3;
        int displayWidth = displayMetrics.widthPixels;
        if (aspectRatio*height > displayWidth) {
            newsize = new Size(displayWidth, (int) (aspectRatioInverse*displayWidth));
        } else {
            newsize = new Size((int) (aspectRatio*height), height);
        }
        Log.i(TAG, "newsize: " + newsize.height() + "," + newsize.width() + "aspect ratio: " + newsize.width()/newsize.height());
        resizeFactor = newsize.width()/videoGrabber.getImageWidth();

        resize(grabbedMatFrame, grabbedMatFrame, newsize);
        updateImage(grabbedMatFrame);
    }

    // Threshold frame with opencv and update imageview with bitmap
    private void updateImage(Mat originalMatFrame) throws NullPointerException{
        //todo: make this worker thread
        Mat matFrame = originalMatFrame.clone();
        long startRenderImage = System.nanoTime();
        Bitmap currentImage;
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();

        Mat matHSV = new Mat();
        cvtColor(matFrame, matHSV, COLOR_BGR2HSV);
        Mat destMat = new Mat();

        inRange(matHSV,
                new Mat(1, 1, CV_32SC4, new Scalar(H_MIN, S_MIN, V_MIN, 0)),
                new Mat(1, 1, CV_32SC4, new Scalar(H_MAX, S_MAX, V_MAX, 0)),
                destMat);

        MatVector contours = new MatVector();
        Mat bestContour = new Mat();
        findContours(destMat.clone(), contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        Scalar color = new Scalar(239, 117, 94, 5);
        double maxVal = 0;
        int maxValIdx = 0;

        if (!contours.empty()) { // draw circle only if at least one contour was found
            for (int i = 0; i < contours.size(); i++) {
                double eachContourArea = contourArea(contours.get(i));
                if (maxVal < eachContourArea) {
                    maxVal = eachContourArea;
                    maxValIdx = i;
                }
            }
            bestContour = contours.get(maxValIdx);
            Moments bestMoments = new Moments();
            try {
                bestMoments = moments(bestContour);

            } catch (NullPointerException e) {
                //
            }

            Log.i("moments", "" + bestMoments.m00());
            Point2f center = new Point2f();
            float[] radius = new float[1];
            try {
                minEnclosingCircle(bestContour, center, radius);
            } catch (NullPointerException e) {
                Log.i(TAG, "enclosing circle:" + e.toString());
            }

            Log.i("circle", "" + center.x() + "," + center.y() + "," + radius[0]);
            int intRadius = (int) radius[0];
            Point pointCenter = new Point(Math.round(center.x()), Math.round(center.y()));;
            circle(matFrame, pointCenter, intRadius, org.bytedeco.javacpp.helper.opencv_core.AbstractScalar.GREEN, 5, 8, 0);
        }


        Switch switchShowThreshold = (Switch) findViewById(R.id.switchShowThreshold);
        if (switchShowThreshold.isChecked()) {
            currentImage = bitmapConverter.convert(matConverter.convert(destMat));
        } else {
            currentImage = bitmapConverter.convert(matConverter.convert(matFrame));
        }

        long endRenderImage = System.nanoTime();
        final Float renderFPS = 1000000000.0f / (endRenderImage - startRenderImage + 1);
        img.setImageBitmap(currentImage);
        //Log.i(TAG, "frame" + frame.imageHeight + ":" + frame.imageWidth);
        Log.i(TAG, "matFrame" + matFrame.rows() + ":" + matFrame.cols());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission granted
                } else {

                    // permission denied
                    Toast.makeText(MainActivity.this, "Permission denied to write your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    // Make new point (called when imageview is touch-dragged
    private Point projectXY(ImageView iv, Mat mat, int x, int y){
        // handle if x or y outside ImageView
        if(x > iv.getWidth()){
            x = iv.getWidth();
        } else if (y > iv.getHeight()) {
            y = iv.getHeight();
        } else if (x < 0){
            x = 0;
        } else if (y < 0) {
            y = 0;
        }

        int projectedX = (int)((double)x * ((double)mat.cols()/(double)iv.getWidth()));
        int projectedY = (int)((double)y * ((double)mat.rows()/(double)iv.getHeight()));

        return new Point(projectedX, projectedY);
    }

    // Draw rectangle from start and end points of touch-drag on imageview
    private void drawRectangle(Mat originalMatFrame) {
        matFrame = originalMatFrame.clone();
        Bitmap currentImage;
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();

        try {
            Log.i(TAG, "points: " + startPt.x() + startPt.y() + endPt.x() + endPt.y());
            rectangle(matFrame, startPt, endPt, org.bytedeco.javacpp.helper.opencv_core.AbstractScalar.GREEN);

            currentImage = bitmapConverter.convert(matConverter.convert(matFrame));

            img.setImageBitmap(currentImage);
        } catch (NullPointerException e) {
            //Exit if the point is outside imageview
            return;
        }
    }

    // When activity is stopped, store HSV values in SharedPreferences
    @Override
    public void onStop(){
        super.onStop();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("H_MIN", H_MIN);
        editor.putInt("H_MAX", H_MAX);
        editor.putInt("S_MIN", S_MIN);
        editor.putInt("S_MAX", S_MAX);
        editor.putInt("V_MIN", V_MIN);
        editor.putInt("V_MAX", V_MAX);
        Log.i(TAG, "prefs: " + H_MIN + "," + H_MAX + "," + S_MIN + "," + S_MAX + "," + V_MIN + "," + V_MAX );
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.mnuHelp:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sraorao/AnimApp"));
                startActivity(browserIntent);
                break;
            case R.id.mnuLoadPresets:
                Intent intent = new Intent(MainActivity.this, PresetsActivity.class);
                MainActivity.this.startActivity(intent);
                break;
        }
        return true;
    }

    // Writes line to csv file
    private void writeTEXT(String data, String fileName, Context context) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);
            Log.i(TAG, "CONFIG filename: " + file.toString());
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch(IOException e) {
            Log.i(TAG, "CONFIG file Writing failed" + e.toString());

        }
    }
}

