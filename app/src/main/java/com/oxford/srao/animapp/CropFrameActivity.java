package com.oxford.srao.animapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.IOException;
import java.io.InputStream;

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
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class CropFrameActivity extends Activity {
    private static InputStream stream;
    //PinchImageView  pinchimageview;
    SelectView selectView;
    ImageView imageResult;
    opencv_core.Mat grabbedMatFrame;
    opencv_core.Mat matFrame;
    opencv_core.Size newsize;
    Point startPt = new Point(0, 0);
    Point endPt = new Point(0, 0);
    //int projectedX;
    //int projectedY;
    Bitmap bitmapMaster;
    Canvas canvasMaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_frame);
        Uri selectedFile = Uri.parse(getIntent().getStringExtra("uri"));
        //pinchimageview = findViewById(R.id.pinchImageView );
        //selectView = findViewById(R.id.selectView);
        imageResult = (ImageView)findViewById(R.id.result);
        try {
            stream = getContentResolver().openInputStream(selectedFile);
            Toast.makeText(getApplicationContext(), selectedFile.toString(), Toast.LENGTH_SHORT).show();
            grabFirstFrame(stream);
            //updateImage(grabbedFrame);
            //img.setImageBitmap(currentImage);
        } catch(Exception e){
            Log.i(TAG, "Something went wrong with reading video file!" + e.toString());
        }

        findViewById(R.id.btnSet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //updateCroppedImage(grabbedMatFrame);
                //pinchimageview.getCroppedImage(imageView);
                Intent intent = new Intent(CropFrameActivity.this, MainActivity.class);
                //intent.putExtra("startX", startPt.x);
                //intent.putExtra("startY", startPt.y);
                //intent.putExtra("endX", projectedX);
                //intent.putExtra("endY", projectedY);
                CropFrameActivity.this.startActivity(intent);
            }
        });

        imageResult.setOnTouchListener(new View.OnTouchListener(){

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
                        updateImage(grabbedMatFrame);
                        //Log.i(TAG,"ACTION_MOVE- " + x + " : " + y);
                        //drawOnRectProjectedBitMap((ImageView)v, grabbedMatFrame, x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG,"ACTION_UP- " + x + " : " + y);
                        endPt = projectXY((ImageView)v, grabbedMatFrame, x, y);
                        //drawOnRectProjectedBitMap((ImageView)v, grabbedMatFrame, x, y);
                        updateImage(grabbedMatFrame);
                        break;
                }
                /*
                 * Return 'true' to indicate that the event have been consumed.
                 * If auto-generated 'false', your code can detect ACTION_DOWN only,
                 * cannot detect ACTION_MOVE and ACTION_UP.
                 */
                //updateImage(grabbedMatFrame);
                return true;
            }});

    }
        //pinchimageview .setImageBitmap(bitmap);
        //pinchimageview .setImageResource(R.drawable.ic_launcher);
    private Point projectXY(ImageView iv, Mat mat, int x, int y){
        if(x<0 || y<0 || x > iv.getWidth() || y > iv.getHeight()){
            //outside ImageView
            return null;
        }else{
            int projectedX = (int)((double)x * ((double)mat.cols()/(double)iv.getWidth()));
            int projectedY = (int)((double)y * ((double)mat.rows()/(double)iv.getHeight()));

            return new Point(projectedX, projectedY);
        }
    }
    /*
    private void finalizeDrawing(){
        canvasMaster.drawBitmap(bitmapDrawingPane, 0, 0, null);
    }*/

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
        newsize = new opencv_core.Size(frame.imageWidth/4, frame.imageHeight/4);
        resize(grabbedMatFrame, grabbedMatFrame, newsize);
        updateImage(grabbedMatFrame);




    }

    private void updateImage(opencv_core.Mat originalMatFrame) throws NullPointerException{
        matFrame = originalMatFrame.clone();
        Bitmap currentImage;
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();
        Log.i(TAG, "points: " + startPt.x() + startPt.y() + endPt.x() + endPt.y());
        rectangle(matFrame, startPt, endPt, org.bytedeco.javacpp.helper.opencv_core.AbstractScalar.GREEN);

        currentImage = bitmapConverter.convert(matConverter.convert(matFrame));
        //selectView.setImageBitmap(currentImage);
        bitmapMaster = currentImage;


        imageResult.setImageBitmap(currentImage);


    }





}
