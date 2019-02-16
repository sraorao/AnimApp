package com.oxford.srao.animapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    float scaleFactor = 1;
    double[] dblDistanceBins;
    int numBins;

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
        scaleFactor = getIntent().getFloatExtra("scaleFactor", 1);


        final GridLabelRenderer gridLabel = graphView.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Y");
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(framewidth + 10);
        graphView.getViewport().setMinX(0);
        gridLabel.setHorizontalAxisTitle("X");
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(frameheight + 10);
        graphView.getViewport().setMinY(0);
        graphView.setBackgroundColor(getResources().getColor(android.R.color.white));

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraphActivity.this, MainActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                GraphActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnTimeBins).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraphActivity.this, TimeBinsActivity.class); // replace PlayVideoActivity with PlayVideoImgActivity for ImageView version
                Bundle b = new Bundle();
                b.putDoubleArray("TimeBins", dblDistanceBins);
                intent.putExtras(b);
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
                List<String[]> csvLine = readCSVFromDownloadsFolder(fileDisplayName + ".csv");
                createLineGraph(csvLine);
                tvDistance.setText("Total distance: " + formatter.format(calculateDistance(csvLine)).toString());
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
                //Log.i(TAG, "totaldistance: " + calculateDistance(csvLine).toString());
                createLineGraph(csvLine);

                tvDistance.setText("Total distance: " + formatter.format(calculateDistance(csvLine)).toString());
            }
        });

        findViewById(R.id.fabShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphView.takeSnapshotAndShare(GraphActivity.this, "scatterplot", "AnimApp plot");
            }
        });
        List<String[]> csvLine = readCSVFromDownloadsFolder(fileDisplayName + ".csv");
        createLineGraph(csvLine);
        tvDistance.setText("Total distance: " + formatter.format(calculateDistance(csvLine)).toString());
        //Toast.makeText(GraphActivity.this, csvLine.get(0) + "," + csvLine.get(1), Toast.LENGTH_LONG);
    }

    private List<String[]> readCSVFromDownloadsFolder(String fileName){
        List<String[]> csvLine = new ArrayList<>();
        String[] content = null;
        try {
            //Log.i(TAG, fileName + ": display name - " + fileDisplayName);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
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
        result.remove(0); // remove column names
        //Log.i(TAG, "result: " + result.size());
        graphView.removeAllSeries();
        if (numFrames == 0 || numFrames > result.size()) {
            numFrames = result.size();
            editText.setText(Integer.toString(numFrames));
            seekBarFrame.setMax(result.size());
            seekBarFrame.setProgress(result.size());
        }
        DataPoint[] dataPoints = new DataPoint[numFrames];
        double[][] dataArray = new double[numFrames][2];
        for (int i = 0; i < numFrames; i++){
            Log.i(TAG, "row: " + result.get(i)[1] + ":" + i);
            String [] rows = result.get(i);
            //Log.d(TAG, "Output " + Double.parseDouble(rows[1]) + " " + Double.parseDouble(rows[2]));
            dataArray[i][0] = Double.parseDouble(rows[1]);
            dataArray[i][1] = Double.parseDouble(rows[2]);
            //dataPoints[i] = new DataPoint(Double.parseDouble(rows[2]), Double.parseDouble(rows[1]));
        }

        // sort array which is requirement for GraphView 4.2.2
        Log.i(TAG, "dataArray before sorting: " + dataArray[1][0] + dataArray[1][1]);
        java.util.Arrays.sort(dataArray, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a[0], b[0]);
            }
        });
        Log.i(TAG, "dataArray after sorting:" + dataArray[1][0] + dataArray[1][1]);

        // assign sorted array to datapoints
        for (int i = 0; i < numFrames; i++){
            Log.i(TAG, "row: " + result.get(i)[1] + ":" + i);
            dataPoints[i] = new DataPoint(dataArray[i][0], dataArray[i][1]);
        }
        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>(dataPoints);
        series.setSize(5);
        graphView.addSeries(series);
    }

    private Double sq(Double num) {
        return(num * num);
    }

    private Double calculateDistance(List<String[]> result){
        Double[][] dblDataFrame = new Double[result.size()][4];
        Double dblTotalDistance = 0.0;
        // processing for bins graph
        // TODO: move this to TimeBinsActivity
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int numBins = prefs.getInt("Bins", 10);
        Log.i(TAG, "Binwidth:" + numBins);
        dblDistanceBins = new double[(numFrames/numBins) + 1];

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
        }
        for (int i = 0; i < numFrames - 1; i++) {
            dblDataFrame[i][3] = Math.sqrt(sq(dblDataFrame[i][1] - dblDataFrame[i + 1][1]) + sq(dblDataFrame[i][2] - dblDataFrame[i + 1][2]));
            dblTotalDistance += dblDataFrame[i][3];
        }
        Log.i(TAG, "total distance calculated successfully");

        // processing for bins graph
        // TODO: move this to TimeBinsActivity
        int dfIndex = 0;
        for (int i = 0; i < dblDistanceBins.length; i++) { // NOT rolling sum
            dblDistanceBins[i] = 0.0;
            for (int j = 0; j < numBins; j++) {
                dfIndex = (numBins * i) + j;
                //Log.i(TAG, "dfIndex:" + dfIndex);
                //Log.i(TAG, "i:j=" + i + ":" + j);
                if (dfIndex >= numFrames - 1) {
                    break;}
                dblDistanceBins[i] += dblDataFrame[dfIndex][3];
            }
            //Log.i(TAG, "distancebins: " + dblDistanceBins[i] + ":" + dfIndex + ":" + i + ":" + dblDistanceBins.length);
        }
        //Log.i(TAG, "scaleFactor: " + scaleFactor);
        String summary = Calendar.getInstance().getTime() + "," + fileDisplayName + "," +
                numFrames + "," + dblTotalDistance + "," + dblTotalDistance*scaleFactor + "," + scaleFactor + "\n";
        //Log.i(TAG, "summary: " + summary);
        // write column names if writing file for the first time
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, "AnimApp_summary.csv");
        if (!file.exists()) {
            writeCSV("time,filename,number_of_frames,total_distance_px,total_distance_cm,scale_factor\n", "AnimApp_summary.csv", GraphActivity.this);
        }
        writeCSV(summary, "AnimApp_summary.csv", GraphActivity.this);
        //Log.i(TAG, "summary written successfully");
        return(dblTotalDistance);
    }

    private void writeCSV(String data, String fileName, Context context) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
            outputStreamWriter.close();
            fileOutputStream.close();
            //Log.i(TAG, "CSV written successfully");
            //Log.i(TAG, "filename: " + file.getPath());
        } catch(IOException e) {
            Log.i(TAG, "CSV Writing failed" + e.toString());

        }
    }


}
