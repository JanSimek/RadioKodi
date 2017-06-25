package xyz.simek.radiokodi.radiokodi;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ListView listRadio;


    KodiJSON kodijson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        kodijson = new KodiJSON();

        listRadio = (ListView) findViewById(R.id.listRadio);

        ArrayAdapter<KodiJSON.Radio> radiosAdapter = new ArrayAdapter<KodiJSON.Radio>(this, android.R.layout.simple_list_item_single_choice, kodijson.getRadios());

        listRadio.setAdapter(radiosAdapter);
        listRadio.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listRadio.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String radio = listRadio.getItemAtPosition(position).toString();

                Log.i(TAG, "Selected radio: " + radio);
                kodijson.selectRadio(radio);

                /*
                try {
                    Thread.currentThread();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setSelectedRadio();
                */
            }
        });

        Button buttonRadioOff = (Button) findViewById(R.id.buttonRadioOff);

        buttonRadioOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            kodijson.stopRadio();
            }
        });

        Button buttonVolumeDown = (Button) findViewById(R.id.buttonVolumeDown);

        buttonVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int volume = kodijson.changeVolume(-10);
                setVolumeProgress(volume);
            }
        });

        Button buttonVolumeUp = (Button) findViewById(R.id.buttonVolumeUp);

        buttonVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int volume = kodijson.changeVolume(+10);
                setVolumeProgress(volume);
            }
        });

        // Set current volume
        setVolumeProgress(kodijson.getVolume());

        if(kodijson.getActivePlayer()) setSelectedRadio();
    }

    public void setSelectedRadio()
    {
        String currentRadio = kodijson.getCurrentlyPlaying();
        //this.setTitle(currentRadio);

        for (int i = 0; i < listRadio.getAdapter().getCount(); i++)
        {
            if(listRadio.getItemAtPosition(i).toString().equals(currentRadio))
            {
                listRadio.setItemChecked(i, true);
                break;
            }
        }
    }

    public void setVolumeProgress(int volume)
    {
        ProgressBar volumeProgress = (ProgressBar) findViewById(R.id.progressVolume);
        volumeProgress.setProgress(volume);

        TextView volumeText = (TextView) findViewById(R.id.textVolume);
        volumeText.setText(Integer.toString(volume));
    }
}
