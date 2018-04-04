package com.oxford.srao.playvideo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

//import com.nononsenseapps.filepicker.FilePickerActivity;
//import com.nononsenseapps.filepicker.Utils;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.swresample;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
    //public String selectedFile;
    private static InputStream stream;
    private static final int READ_REQUEST_CODE = 42;
    ImageView img;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.image_view);
        findViewById(R.id.btnParseVideo).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //performFileSearch();
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
            }
        });
    }

    // result from file chooser
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==123 && resultCode==RESULT_OK) {
            Uri selectedFile = data.getData(); //The uri with the location of the file
            try {
                stream = getContentResolver().openInputStream(selectedFile);
                Toast.makeText(getApplicationContext(), stream.toString(), Toast.LENGTH_LONG).show();
            } catch(Exception e){

            }
            // start new activity
            //Intent intent = new Intent(MainActivity.this, Main2Activity.class);
            //intent.putExtra("selectedFile", getPath(getApplicationContext(), selectedFile));
            //startActivity(intent);
            startVideoParsing(stream);
        }
    }
    public static InputStream getInStream() {
        return stream;
    }
/*
    public void performFileSearch() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("video/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                //Log.i(TAG, "Uri: " + uri.toString());
                String path = uri.getPath();
                Log.i(TAG, "Uri: " + path);
                //showImage(uri);
                startVideoParsing(uri.toString());
            }
        }
    }
*/

    private void startVideoParsing(final InputStream stream) {
        Toast.makeText(MainActivity.this,
                "playing..." + stream,
                Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doConvert(stream);
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void doConvert(InputStream stream) throws
            FrameGrabber.Exception,
            FrameRecorder.Exception,
            IOException {
        FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(stream);
        Frame frame;
        int count = 0;
        videoGrabber.start();
        AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();
        while (true) {
            long startRenderImage = System.nanoTime();
            frame = videoGrabber.grabFrame();
            if (frame == null) {
                break;
            }
            if (frame.image == null) {
                continue;
            }
            count++;
            final Bitmap currentImage = bitmapConverter.convert(frame);
//            final ArrayList<GestureBean> rst = Predictor.predict(currentImage, this);
            long endRenderImage = System.nanoTime();
            final Float renderFPS = 1000000000.0f / (endRenderImage - startRenderImage + 1);
            //final Handler handler = new Handler(); // newline
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    outputTv.setText(String.format("读取数据FPS：%f,结果:%d", renderFPS, rst == null ? 0 : rst.size()));
                    img.setImageBitmap(currentImage);
                    //handler.postDelayed(this, 1000); // newline
                }
            });
        }
    }
}

