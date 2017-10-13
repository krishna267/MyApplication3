package krishna.example.com.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class graph extends AppCompatActivity{
    GraphView g;
    int highest = 0;
    double threshold;
    //TextView tv;
    Button b;
    int y[] = new int[1000];
    ArrayList<Integer> count  = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testgraphview);
        b = (Button)findViewById(R.id.gotofft);
        //tv = (TextView)findViewById(R.id.gi);
        g = (GraphView)findViewById(R.id.test);

        g.getViewport().setXAxisBoundsManual(true);
        g.getViewport().setMaxX(1000);
        g.getViewport().setMinX(1);
        plot();
        findpeakvel();
        getzerovelpoints();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(graph.this,calculate.class);
                startActivity(i);
            }
        });
    }

    private void findpeakvel() {
        int a;
        for(int i=0;i<998;i++){
            a = Math.abs(y[i+1] - y[i]);
            if(a>highest){
                highest = a;
            }
        }
        threshold = 0.01*highest;
    }

    public void plot(){
        String line;
        DataPoint d;
        int i=0;
        InputStream is;
        try{
            is = (InputStream)openFileInput("eeg.txt");
        }catch (Exception e){
            e.printStackTrace();
            is = getResources().openRawResource(R.raw.data1);
            Toast.makeText(graph.this,"Default data",Toast.LENGTH_SHORT).show();
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        try{
        while((line = bufferedReader.readLine())!=null){
            y[i] = Integer.parseInt(line);
            d = new DataPoint(i,y[i]);
            series.appendData(d,true,1000);
            i++;
        }
        is.close();
        }
        catch (IOException e ){
            System.out.print(e);
        }
        g.addSeries(series);
    }

    public void getzerovelpoints(){
        for (int i =0;i<998;i++){
            if(Math.abs(y[i]-y[i+1])<threshold){
                count.add(i);
            }
        }
    }
}
