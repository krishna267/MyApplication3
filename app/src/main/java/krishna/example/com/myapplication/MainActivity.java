package krishna.example.com.myapplication;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;


public class MainActivity extends AppCompatActivity {
    private EditText mUsername;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //INITIALIZE VIEWS
        mUsername = (EditText)findViewById(R.id.TFusername);
    }


    //LOGIN BUTTON CLICK EVENT TO GO TO NEXT ACTIVITY
    public void onClickButton(View v)
    {
        if (v.getId()==R.id.Blogin)
        {
            //ADDING USERNAME AS EXTRA
            //SENDING TO NEXT ACTIVITY IF USERNAME IS GIVEN
            if(mUsername.getText().toString()!=null) {
                String mUsernameString = mUsername.getText().toString();
                Intent i = new Intent(MainActivity.this, ble.class);
                i.putExtra("Username", mUsernameString);
                startActivity(i);
            }else {
                Toast.makeText(this,"Please enter a Username",Toast.LENGTH_SHORT).show();
            }
        }
    }
}