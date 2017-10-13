
package krishna.example.com.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;



public class Display extends AppCompatActivity {
    private ProgressBar mStressProgress;
    private TextView t,mUsername;
    private stress s;
    private Button gsra,eega;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);

        //INITIALIZE VIEWS
        t = (TextView)findViewById(R.id.ss);
        mStressProgress = (ProgressBar)findViewById(R.id.progressBar);
        gsra = (Button)findViewById(R.id.gbutton);
        eega = (Button)findViewById(R.id.ebutton);
        mUsername = (TextView)findViewById(R.id.TVusername);

        //RETRIEVING USERNAME
        String user=getIntent().getStringExtra("Username");

        //SET USERNAME AFTER WELCOME MESSAGE
        mUsername.setText(user);
    }

    //GSR BUTTON CLICK EVENT
    public void analysisgsr(View v){
        s = new stress();
        InputStream is;

        //TRY TO READ DATA RECEIVED FROM BLUETOOTH STORED IN "gsr.txt"
        try{
            is = openFileInput("gsr.txt");
        }catch (Exception e){
            e.printStackTrace();

            //IF FILE IS NOT AVAILABLE READ DEFAULT DATA
            is = getResources().openRawResource(R.raw.str);
            Toast.makeText(Display.this,"Default data",Toast.LENGTH_SHORT).show();
        }

        //PASS INPUTSTREAM POINTER TO READFILE METHOD
        s.readfile(is);

        //MEAN REMOVAL
        s.meanremoval();

        //CALCULATE STRESS PERCENTAGE
        int per = (int)(s.stresspoints()*100);
        t.setText("Cognitive Stress level = "+per+"%");

        //SET COGNITIVE STRESS PERCENTAGE
        mStressProgress.setProgress(per);
    }

    //CAPTURE BUTTON CLICK EVENT
    public void bluetoothquery(View v){
        try {
                Intent i = new Intent(Display.this,listofbtdevices.class);
                startActivity(i);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //EEG BUTTON CLICK EVENT
    public void plotting(View v){
        try{
            Intent i = new Intent(Display.this,graph.class);
            startActivity(i);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
