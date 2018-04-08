package com.oxford.srao.playvideo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

//import com.nononsenseapps.filepicker.FilePickerActivity;
//import com.nononsenseapps.filepicker.Utils;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_imgproc.*;
import org.bytedeco.javacpp.swresample;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static android.content.ContentValues.TAG;
import static org.bytedeco.javacpp.opencv_core.CV_32SC4;
import static org.bytedeco.javacpp.opencv_core.CV_64FC1;
import static org.bytedeco.javacpp.opencv_core.cvCreateMat;
import static org.bytedeco.javacpp.opencv_core.inRange;
import static org.bytedeco.javacpp.opencv_imgproc.CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvGetSpatialMoment;
import static org.bytedeco.javacpp.opencv_imgproc.cvMoments;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.drawContours;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.minEnclosingCircle;
import static org.bytedeco.javacpp.opencv_imgproc.moments;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class MainActivity extends Activity {
    private static InputStream stream;
    Uri selectedFile;
    Mat grabbedMatFrame;
    ImageView img;
    TextView tvoutput;
    int H_MIN = 0;
    int S_MIN = 0;
    int V_MIN = 0;
    int H_MAX = 180;
    int S_MAX = 255;
    int V_MAX = 30;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.image_view);
        //img.setBackgroundColor(Color.parseColor("blue"));
        tvoutput = findViewById(R.id.output);

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

        findViewById(R.id.btnScreen1Next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayVideoImgActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                if (selectedFile == null) {
                    Toast.makeText(MainActivity.this, "Please select a file!", Toast.LENGTH_LONG);
                } else {
                    intent.putExtra("uri", selectedFile.toString());
                    intent.putExtra("Hmin", H_MIN);
                    intent.putExtra("Smin", S_MIN);
                    intent.putExtra("Vmin", V_MIN);
                    intent.putExtra("Hmax", H_MAX);
                    intent.putExtra("Smax", S_MAX);
                    intent.putExtra("Vmax", V_MAX);
                    Switch switchShowThreshold = (Switch) findViewById(R.id.switchShowThreshold);
                    intent.putExtra("isChecked", switchShowThreshold.isChecked());
                    MainActivity.this.startActivity(intent);
                }
            }
        });
        Switch switchShowThreshold = (Switch) findViewById(R.id.switchShowThreshold);
        switchShowThreshold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateImage(grabbedMatFrame);
            }
        });
        SeekBar seekBarHmin = findViewById(R.id.seekBarHmin);
        SeekBar seekBarSmin = findViewById(R.id.seekBarSmin);
        SeekBar seekBarVmin = findViewById(R.id.seekBarVmin);
        SeekBar seekBarHmax = findViewById(R.id.seekBarHmax);
        SeekBar seekBarSmax = findViewById(R.id.seekBarSmax);
        SeekBar seekBarVmax = findViewById(R.id.seekBarVmax);
        seekBarHmin.setProgress(H_MIN);
        seekBarSmin.setProgress(S_MIN);
        seekBarVmin.setProgress(V_MIN);
        seekBarHmax.setProgress(H_MAX);
        seekBarSmax.setProgress(S_MAX);
        seekBarVmax.setProgress(V_MAX);
        seekBarHmin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                H_MIN = progress;
                updateImage(grabbedMatFrame);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Value changed to:" + H_MIN, Toast.LENGTH_LONG).show();
            }
        });
        seekBarSmin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                S_MIN = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Value changed to:" + S_MIN, Toast.LENGTH_LONG).show();
                updateImage(grabbedMatFrame);
            }
        });
        seekBarVmin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                V_MIN = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Value changed to:" + V_MIN, Toast.LENGTH_LONG).show();
                updateImage(grabbedMatFrame);
            }
        });
        seekBarHmax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                H_MAX = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Value changed to:" + H_MAX, Toast.LENGTH_LONG).show();
                updateImage(grabbedMatFrame);
            }
        });
        seekBarSmax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                S_MAX = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Value changed to:" + S_MAX, Toast.LENGTH_LONG).show();
                updateImage(grabbedMatFrame);
            }
        });
        seekBarVmax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                V_MAX = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Value changed to:" + V_MAX, Toast.LENGTH_LONG).show();
                updateImage(grabbedMatFrame);
            }
        });
    }

    // result from file chooser
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==123 && resultCode==RESULT_OK) {
            selectedFile = data.getData(); //The uri with the location of the file
            try {
                stream = getContentResolver().openInputStream(selectedFile);
                Toast.makeText(getApplicationContext(), stream.toString(), Toast.LENGTH_LONG).show();
                grabFirstFrame(stream);
                //updateImage(grabbedFrame);
                //img.setImageBitmap(currentImage);
            } catch(Exception e){

            }
            // start new activity
            //Intent intent = new Intent(MainActivity.this, Main2Activity.class);
            //intent.putExtra("selectedFile", getPath(getApplicationContext(), selectedFile));
            //startActivity(intent);
            //startVideoParsing(stream);

        }
    }
    public static InputStream getInStream() {
        return stream;
    }
/*
    private void startVideoParsing(final InputStream stream) {
        Toast.makeText(MainActivity.this,
                "playing..." + stream,
                Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    showFirstFrame(stream);
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
*/
    private void grabFirstFrame(InputStream stream) throws
            FrameGrabber.Exception,
            FrameRecorder.Exception,
            IOException, NullPointerException {
        FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(stream);
        Frame frame;
        //int count = 0;
        videoGrabber.start();

        frame = videoGrabber.grabFrame();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        //OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();


        grabbedMatFrame = matConverter.convert(frame.clone());
        Size newsize = new Size(frame.imageWidth/4, frame.imageHeight/4);
        resize(grabbedMatFrame, grabbedMatFrame, newsize);
        updateImage(grabbedMatFrame);




    }

    private void updateImage(Mat originalMatFrame) throws NullPointerException{
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

        //mask = cv2.bitwise_or(mask1, mask2)
        //CvMemStorage memory=CvMemStorage.create();
        //CvSeq cvSeq = new CvSeq();
        MatVector contours = new MatVector();
        Mat bestContour = new Mat();
        //cvFindContours(destMat.clone(), memory, cvSeq, Loader.sizeof(CvContour.class), RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        findContours(destMat.clone(), contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        Scalar color = new Scalar(239, 117, 94, 5);
        double maxVal = 0;
        int maxValIdx = 0;

        for (int i = 0; i < contours.size(); i++) {
            double eachContourArea = contourArea(contours.get(i));
            if (maxVal < eachContourArea) {
                maxVal = eachContourArea;
                maxValIdx = i;
            }
        }
        bestContour = contours.get(maxValIdx);
        //iplConverter.convert(matFrame);
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
            //
        }
        //drawContours(matFrame, contours, maxValIdx, color);
        //Mat blackMat = new Mat();
        Log.i("circle", "" + center.x() + "," + center.y() + "," + radius[0]);
        int intRadius = (int) radius[0];
        Point pointCenter = new Point(Math.round(center.x()), Math.round(center.y()));;
        circle(matFrame, pointCenter, intRadius, org.bytedeco.javacpp.helper.opencv_core.AbstractScalar.GREEN, 5, 8, 0);
        Switch switchShowThreshold = (Switch) findViewById(R.id.switchShowThreshold);
        if (switchShowThreshold.isChecked()) {
            currentImage = bitmapConverter.convert(matConverter.convert(destMat));
        } else {
            currentImage = bitmapConverter.convert(matConverter.convert(matFrame));
        }


//            final ArrayList<GestureBean> rst = Predictor.predict(currentImage, this);
        long endRenderImage = System.nanoTime();
        final Float renderFPS = 1000000000.0f / (endRenderImage - startRenderImage + 1);
        //return(currentImage);
        img.setImageBitmap(currentImage);
        //Log.i(TAG, "frame" + frame.imageHeight + ":" + frame.imageWidth);
        Log.i(TAG, "matFrame" + matFrame.rows() + ":" + matFrame.cols());


    }

}

