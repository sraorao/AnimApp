package com.oxford.srao.animapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.net.Uri;

import org.bytedeco.javacpp.opencv_core;
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
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * TODO: document your custom view class.
 */
public class CanvasView extends View {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    InputStream stream;
    public Uri uri;
    public int H_MIN;
    public int S_MIN;
    public int V_MIN;
    public int H_MAX;
    public int S_MAX;
    public int V_MAX;
    public boolean isChecked;
    opencv_core.Mat grabbedMatFrame;
    Bitmap b;

    public CanvasView(Context context) {
        super(context);
        init(null, 0);
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }
/*
    public CanvasView(Context context, Uri uri, int Hmin, int Smin, int Vmin, int Hmax, int Smax, int Vmax) {
        super(context);
        init(null, 0);
        this.uri = uri;
        //this.H_MIN = Hmin;
        //this.S_MIN = Smin;
        //this.V_MIN = Vmin;
        //this.H_MAX = Hmax;
        //this.S_MAX = Smax;
        //this.V_MAX = Vmax;
    }
    */
    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.canvasView, defStyle, 0);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        //canvas.drawBitmap();
        //
        /*
        try {
            Bitmap b = grabFirstFrame(this.uri);
            canvas.drawBitmap(b,100, 3, null);
        } catch (IOException e) {
            //
        }
        this.invalidate();
        this.postInvalidateDelayed(500);*/
        Log.i(TAG, "uri: " + this.uri.toString());
        try {
            stream = this.getContext().getContentResolver().openInputStream(this.uri);
        } catch(Exception e) {
            //
        }

        startVideoParsing(stream);
        /* this doesn't crash, but doesn't show video
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (b != null) {
                    canvas.drawBitmap(b, 0, 0, null);
                }
                invalidate();
            }
        }); */
        canvas.drawBitmap(b, 0, 0, null);
        //postInvalidateDelayed(500);
    }

    private void startVideoParsing(final InputStream stream) {
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
        opencv_core.Size newsize = new opencv_core.Size(videoGrabber.getImageWidth()/4, videoGrabber.getImageHeight()/4);
        AndroidFrameConverter bitmapConverter = new AndroidFrameConverter();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
        opencv_core.Mat matHSV = new opencv_core.Mat();
        opencv_core.Mat destMat = new opencv_core.Mat();
        opencv_core.Scalar color = new opencv_core.Scalar(239, 117, 94, 5);
        opencv_core.Point2f center = new opencv_core.Point2f();
        opencv_core.MatVector contours = new opencv_core.MatVector();
        opencv_core.Mat bestContour;
        float[] radius = new float[1];
        opencv_core.Mat displayFrame;
        int length_frames = videoGrabber.getLengthInFrames();
        while (count < length_frames) {
            long startRenderImage = System.nanoTime();
            frame = videoGrabber.grabFrame();
            if (frame == null) {
                break;
            }
            if (frame.image == null) {
                continue;
            }
            count++;
            Log.i(TAG, "count: " + count);
            Log.i(TAG, "frame: " + frame.imageWidth);
            opencv_core.Mat matFrame = matConverter.convert(frame);
            resize(matFrame, matFrame, newsize);

            cvtColor(matFrame, destMat, COLOR_BGR2HSV);


            inRange(destMat,
                    new opencv_core.Mat(1, 1, CV_32SC4, new opencv_core.Scalar(H_MIN, S_MIN, V_MIN, 0)),
                    new opencv_core.Mat(1, 1, CV_32SC4, new opencv_core.Scalar(H_MAX, S_MAX, V_MAX, 0)),
                    destMat);
            //mask = cv2.bitwise_or(mask1, mask2)
            //CvMemStorage memory=CvMemStorage.create();
            //CvSeq cvSeq = new CvSeq();

            //cvFindContours(destMat.clone(), memory, cvSeq, Loader.sizeof(CvContour.class), RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
            findContours(destMat, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
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
            //opencv_core.Moments bestMoments = new opencv_core.Moments();
            //bestMoments = moments(bestContour);
            //Log.i("moments", "" + bestMoments.m00());
            minEnclosingCircle(bestContour, center, radius);
            //drawContours(matFrame, contours, maxValIdx, color);
            //Mat blackMat = new Mat();
            Log.i("circle", "" + center.x() + "," + center.y() + "," + radius[0]);
            int intRadius = (int) radius[0];
            opencv_core.Point pointCenter = new opencv_core.Point(Math.round(center.x()), Math.round(center.y()));;
            circle(matFrame, pointCenter, intRadius, org.bytedeco.javacpp.helper.opencv_core.AbstractScalar.GREEN, 5, 8, 0);
            if (isChecked) {
                b = bitmapConverter.convert(matConverter.convert(destMat));
                //displayFrame = destMat;
            } else {
                b = bitmapConverter.convert(matConverter.convert(matFrame));
                //displayFrame = matFrame;
            }

//            final ArrayList<GestureBean> rst = Predictor.predict(currentImage, this);
            //long endRenderImage = System.nanoTime();
            //final Float renderFPS = 1000000000.0f / (endRenderImage - startRenderImage + 1);
            //b = currentImage;
            Log.i(TAG, "bitmap: " + b.getByteCount());

            //return(currentImage);
            //final Handler handler = new Handler(); // newline
            /*
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img.setImageBitmap(currentImage);
                    //handler.postDelayed(this, 1000); // newline
                }
            });*/

        }
    }



}
