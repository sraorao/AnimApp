package com.oxford.srao.animapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.bytedeco.javacpp.opencv_core.*;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacpp.opencv_core.Mat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import static android.content.ContentValues.TAG;
import static org.bytedeco.javacpp.opencv_core.CV_32SC4;
import static org.bytedeco.javacpp.opencv_core.inRange;
import static org.bytedeco.javacpp.opencv_imgproc.CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.minEnclosingCircle;
import static org.bytedeco.javacpp.opencv_imgproc.moments;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class PlayVideoImgActivity extends Activity {
    public String selectedFile;
    InputStream stream;
    ImageView img;
    //Bitmap currentImage;

    //public Uri uri;
    public int H_MIN;
    public int S_MIN;
    public int V_MIN;
    public int H_MAX;
    public int S_MAX;
    public int V_MAX;
    public boolean isChecked;
    String fileDisplayName;
    Size newsize;
    Point startPt = new Point(0, 0);
    Point endPt = new Point(0, 0);
    float scaleFactor = 1;
    //opencv_core.Mat grabbedMatFrame;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_play_video_img);
        img = findViewById(R.id.image_view);
        /*
        findViewById(R.id.btnScreen2Next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlayVideoImgActivity.this, GraphActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                intent.putExtra("fileDisplayName", fileDisplayName);
                intent.putExtra("framewidth", newsize.width());
                intent.putExtra("frameheight", newsize.height());
                intent.putExtra("scaleFactor", scaleFactor);
                PlayVideoImgActivity.this.startActivity(intent);
            }
        });
        */
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlayVideoImgActivity.this, GraphActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                intent.putExtra("fileDisplayName", fileDisplayName);
                intent.putExtra("framewidth", newsize.width());
                intent.putExtra("frameheight", newsize.height());
                intent.putExtra("scaleFactor", scaleFactor);
                PlayVideoImgActivity.this.startActivity(intent);
            }
        });

        selectedFile = getIntent().getStringExtra("uri");
        H_MIN = getIntent().getIntExtra("Hmin", 0);
        S_MIN = getIntent().getIntExtra("Smin", 0);
        V_MIN = getIntent().getIntExtra("Vmin", 0);
        H_MAX = getIntent().getIntExtra("Hmax", 180);
        S_MAX = getIntent().getIntExtra("Smax", 255);
        V_MAX = getIntent().getIntExtra("Vmax", 30);
        startPt = new Point(getIntent().getIntExtra("startX", 0), getIntent().getIntExtra("startY", 0));
        endPt = new Point(getIntent().getIntExtra("endX", 0), getIntent().getIntExtra("endY", 0));
        isChecked = getIntent().getBooleanExtra("isChecked", false);
        fileDisplayName = getIntent().getStringExtra("fileDisplayName");
        scaleFactor = getIntent().getFloatExtra("scaleFactor", 1);
        newsize = new Size(getIntent().getIntExtra("width", 640), getIntent().getIntExtra("height", 480));

        try {
            stream = getContentResolver().openInputStream(Uri.parse(selectedFile));
            Toast.makeText(getApplicationContext(), stream.toString(), Toast.LENGTH_LONG).show();
        } catch(Exception e){

        }
        startVideoParsing(stream);




    }

/*
    public static InputStream getInStream() {
        return stream;
    }

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
        Toast.makeText(PlayVideoImgActivity.this,
                "playing..." + stream,
                Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playVideo(stream);
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

    private void playVideo(InputStream stream) throws
            FrameGrabber.Exception,
            FrameRecorder.Exception,
            IOException {
        FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(stream);
        Frame frame;
        int count = 0;
        videoGrabber.start();
        AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
        //newsize = new Size(videoGrabber.getImageWidth()/4, videoGrabber.getImageHeight()/4);
        Rect roi = new Rect(startPt, endPt);
        Mat matHSV = new Mat();
        Mat destMat = new Mat();

        String outputCSV = "";
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, fileDisplayName + ".csv");
        if (file.exists()) {
            boolean deleted = file.delete();
        }

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
            Mat matFrame = matConverter.convert(frame.clone());
            resize(matFrame, matFrame, newsize);

            if (startPt.x() > 0 && startPt.y() > 0 && endPt.x() > 0 && endPt.y() >0) {
                Log.i(TAG, "points: " + startPt.x() + "," + startPt.y() + "," + endPt.x() + "," +endPt.y());
                matFrame = new Mat(matFrame, roi);
            }
            cvtColor(matFrame, destMat, COLOR_BGR2HSV);


            inRange(destMat,
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
            } catch(NullPointerException e) {
                //
            }
            //Log.i("moments", "" + bestMoments.m00());
            Point2f center = new Point2f();
            float[] radius = new float[1];
            try {
                minEnclosingCircle(bestContour, center, radius);
            } catch(NullPointerException e) {
                //
            }
            //drawContours(matFrame, contours, maxValIdx, color);
            //Mat blackMat = new Mat();
            Log.i("circle", "" + center.x() + "," + center.y() + "," + radius[0]);
            outputCSV = count + "," + center.x() + "," + center.y() + "\n";
            writeCSV(outputCSV, fileDisplayName + ".csv", PlayVideoImgActivity.this);
            Log.i(TAG, "outputCSV:" + outputCSV);
            int intRadius = (int) radius[0];
            Point pointCenter = new Point(Math.round(center.x()), Math.round(center.y()));;
            circle(matFrame, pointCenter, intRadius, org.bytedeco.javacpp.helper.opencv_core.AbstractScalar.GREEN, 5, 8, 0);
            Mat displayFrame;
            if (isChecked) {
                displayFrame = destMat;
            } else {
                displayFrame = matFrame;
            }
            final Bitmap currentImage = bitmapConverter.convert(matConverter.convert(displayFrame));
//            final ArrayList<GestureBean> rst = Predictor.predict(currentImage, this);
            long endRenderImage = System.nanoTime();
            final Float renderFPS = 1000000000.0f / (endRenderImage - startRenderImage + 1);
            //final Handler handler = new Handler(); // newline
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img.setImageBitmap(currentImage);
                    //handler.postDelayed(this, 1000); // newline
                }
            });


        }
        //Log.i(TAG, "test_output: " + outputCSV);
    }

    private void writeCSV(String data, String fileName, Context context) {
        try {
            //File path = context.getExternalFilesDir(null);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
            outputStreamWriter.close();
            fileOutputStream.close();
            Log.i(TAG,"outputCSV length: " + data.length());
            Log.i(TAG, "CSV written successfully");
            Log.i(TAG, "filename: " + file.getPath());
            Log.i(TAG, "scaleFactor screen2: " + scaleFactor);
        } catch(IOException e) {
            Log.i(TAG, "CSV Writing failed" + e.toString());

        }
    }
}
