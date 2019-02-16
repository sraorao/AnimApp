package com.oxford.srao.animapp;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.Arrays;
import java.util.List;


import static android.content.ContentValues.TAG;

public class TimeBinsActivity extends AppCompatActivity {
double[] dblDistanceBins;
GraphView graphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_bins);
        Bundle b = this.getIntent().getExtras();
        dblDistanceBins = b.getDoubleArray("TimeBins");
        double max = Arrays.stream(dblDistanceBins).max().getAsDouble();
        Log.i(TAG, "TimeBins: " + dblDistanceBins[0]);

        findViewById(R.id.fabShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphView.takeSnapshotAndShare(TimeBinsActivity.this, "barplot", "AnimApp plot");
            }
        });

        graphView = findViewById(R.id.graph_view);

        final GridLabelRenderer gridLabel = graphView.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Y");
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(dblDistanceBins.length + 2);
        graphView.getViewport().setMinX(0);
        gridLabel.setHorizontalAxisTitle("Time");
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(max);
        graphView.getViewport().setMinY(0);
        graphView.setBackgroundColor(ContextCompat.getColor(this, R.color.white));


        // assign sorted array to datapoints
        DataPoint[] dataPoints = new DataPoint[dblDistanceBins.length];
        for (int i = 0; i < dblDistanceBins.length; i++){
            Log.i(TAG, "length: " + dblDistanceBins.length);
            dataPoints[i] = new DataPoint(i + 1, dblDistanceBins[i]);
        }
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);
        //series.setSize(5);
        series.setSpacing(1);
        graphView.addSeries(series);
    }
}
