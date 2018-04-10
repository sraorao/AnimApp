package com.oxford.srao.animapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class GraphActivity extends Activity {
    GraphView graphView;
    int numFrames = 0;
    EditText editText;
    TextView tvDistance;
    NumberFormat formatter = new DecimalFormat("#0.00");
    String fileDisplayName;
    int framewidth;
    int frameheight;
    SeekBar seekBarFrame;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        graphView = findViewById(R.id.graph_view);
        editText = findViewById(R.id.editText);
        final TextView tvDistance = findViewById(R.id.tvDistance);
        seekBarFrame = findViewById(R.id.seekBarFrame);
        fileDisplayName = getIntent().getStringExtra("fileDisplayName");
        framewidth = getIntent().getIntExtra("framewidth", 480);
        frameheight = getIntent().getIntExtra("frameheight", 270);

        final GridLabelRenderer gridLabel = graphView.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Y");
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(framewidth + 10);
        graphView.getViewport().setMinX(0);
        gridLabel.setHorizontalAxisTitle("X");
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(frameheight + 10);
        graphView.getViewport().setMinY(0);
        Log.i(TAG, "X: " + framewidth + "Y: " + frameheight);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraphActivity.this, MainActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                GraphActivity.this.startActivity(intent);
            }
        });
        seekBarFrame.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numFrames = progress;
                editText.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        findViewById(R.id.btnReanalyse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    numFrames = Integer.parseInt(editText.getText().toString());
                } catch(NumberFormatException e) {
                    Toast.makeText(GraphActivity.this, "Enter an integer", Toast.LENGTH_SHORT).show();
                }
                List<String[]> csvLine = readCSVFromDownloadsFolder(fileDisplayName + ".csv");
                Log.i(TAG, "totaldistance: " + calculateDistance(csvLine).toString());
                createLineGraph(csvLine);

                tvDistance.setText("Total distance: " + formatter.format(calculateDistance(csvLine)).toString());
            }
        });


        List<String[]> csvLine = readCSVFromDownloadsFolder(fileDisplayName + ".csv");
        Log.i(TAG, "-------------------test");

        createLineGraph(csvLine);
        tvDistance.setText("Total distance: " + formatter.format(calculateDistance(csvLine)).toString());
        //Toast.makeText(GraphActivity.this, csvLine.get(0) + "," + csvLine.get(1), Toast.LENGTH_LONG);
    }

    private List<String[]> readCSVFromDownloadsFolder(String fileName){
        List<String[]> csvLine = new ArrayList<>();
        String[] content = null;
        try {
            Log.i(TAG, fileName + ": display name - " + fileDisplayName);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            //InputStream inputStream = getAssets().open("local.cvs");
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            String line = "";
            while((line = br.readLine()) != null){
                content = line.split(",");
                csvLine.add(content);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvLine;
    }

    private void createLineGraph(List<String[]> result){
        graphView.removeAllSeries();
        if (numFrames == 0 || numFrames > result.size()) {
            numFrames = result.size();
            editText.setText(Integer.toString(numFrames));
            seekBarFrame.setMax(result.size());
            seekBarFrame.setProgress(result.size());
        }
        DataPoint[] dataPoints = new DataPoint[numFrames];
        for (int i = 0; i < numFrames; i++){
            String [] rows = result.get(i);
            //Log.d(TAG, "Output " + Double.parseDouble(rows[1]) + " " + Double.parseDouble(rows[2]));
            dataPoints[i] = new DataPoint(Double.parseDouble(rows[1]), Double.parseDouble(rows[2]));
        }
        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>(dataPoints);
        series.setSize(5);
        //LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints);
        graphView.addSeries(series);
    }

    private Double sq(Double num) {
        return(num * num);
    }

    private Double calculateDistance(List<String[]> result){
        Double[][] dblDataFrame = new Double[result.size()][4];
        Double dblTotalDistance = 0.0;

        if (numFrames == 0 || numFrames > result.size()) {
            numFrames = result.size();
            editText.setText(Integer.toString(numFrames));
            seekBarFrame.setMax(result.size());
            seekBarFrame.setProgress(result.size());
        }
        for (int i = 0; i < result.size(); i++){
            String [] rows = result.get(i);
            //Log.d(TAG, "DataFrame " + Double.parseDouble(rows[0]) + " " + Double.parseDouble(rows[1]) + " " + Double.parseDouble(rows[2]));
            dblDataFrame[i][0] = Double.parseDouble(rows[0]);
            dblDataFrame[i][1] = Double.parseDouble(rows[1]);
            dblDataFrame[i][2] = Double.parseDouble(rows[2]);
            //dblTotalDistance += dblDataFrame[i][3];
        }
        for (int i = 0; i < numFrames - 1; i++) {
            dblDataFrame[i][3] = Math.sqrt(sq(dblDataFrame[i][1] - dblDataFrame[i + 1][1]) + sq(dblDataFrame[i][2] - dblDataFrame[i + 1][2]));
            dblTotalDistance += dblDataFrame[i][3];
        }
        return(dblTotalDistance);
    }
}
