package krishna.example.com.myapplication;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class calculate extends AppCompatActivity {
    FFT f;
    TextView tv;
    GraphView g;
    LineGraphSeries<DataPoint> series;
    public double x[] = new double[1024];
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fft);
        g = (GraphView)findViewById(R.id.gigi);
        //g.getViewport().setScrollable(true);
        //g.getViewport().setScalable(true);
        g.getViewport().setXAxisBoundsManual(true);
        g.getViewport().setMinX(8);
        g.getViewport().setMaxX(12);
        tv = (TextView)findViewById(R.id.tv);
    }

    public void onResume(){
        super.onResume();
        f = new FFT(1024); // 1024 point fft
        try {
            displayfromfile();
            f.fft(x);
            DataPoint[] d = new DataPoint[1024];
            for(int i=0;i<1024;i++){
                d[i] = new DataPoint(i*0.128,Math.abs(x[i]));
            }
            series = new LineGraphSeries<>(d);
            g.addSeries(series);
            tv.setText("The aplha energy of the signal sampled at 128Hz is "+ calalphaenergy(128,1024));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void displayfromfile() throws IOException{
        String line;
        int i=0;
            InputStream is;
            try{
                is = (InputStream)openFileInput("eeg.txt");
            }catch (Exception e){
                e.printStackTrace();
                is = getResources().openRawResource(R.raw.data1);
                Toast.makeText(calculate.this,"Default data fft plot",Toast.LENGTH_SHORT).show();
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            while((line = bufferedReader.readLine())!=null) {
                x[i] = Integer.parseInt(line);
                i++;
            }
            for(;i<1024;i++){
                x[i] = 0;
            }
            is.close();
    }

    public double calalphaenergy(int s,int n){
        int min = (int)(8*n/s);
        int max = (int)(12*n/s);
        double energy = 0;
        for(;min<max;min++){
            energy = energy + x[min]*x[min];
        }
        return energy/1000000;
    }
}
