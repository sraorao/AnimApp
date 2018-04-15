package com.oxford.srao.animapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class SelectView extends AppCompatImageView {
    float x,y;
    Bitmap bmp;
    Paint mPaint;
    float width = 100.0f;
    float height = 50.0f;

    boolean touched = false;

    public SelectView (Context context)
    {
        super(context);
        x = y = 0;
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw (Canvas canvas)
    {
        canvas.drawColor(Color.WHITE);
        if(touched)
        {
            canvas.drawRect(x, y, x+width, y+height, mPaint);

        }
    }

    @Override
    public boolean onTouchEvent (MotionEvent event)
    {
        touched = true;
        //getting the touched x and y position
        x = event.getX();
        y = event.getY();
        invalidate();
        return true;
    }
}
