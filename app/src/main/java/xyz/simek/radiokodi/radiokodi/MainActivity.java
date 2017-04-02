package xyz.simek.radiokodi.radiokodi;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public class Radio {
        private String url;
        private String name;

        public Radio(String name, String url)
        {
            this.name = name;
            this.url = url;
        }

        public String getUrl()
        {
            return this.url;
        }

        public String getName()
        {
            return this.name;
        }

        @Override
        public String toString()
        {
            return this.name;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof Radio)
            {
                Radio r = (Radio)obj;
                if(r.getName().equals(name)) return true;
            }
            return false;
        }
    }

    KodiJSON kodijson;

    ArrayList<Radio> radios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Available streams
        radios = new ArrayList<>();
        radios.add(new Radio("Blaník", "http://kocka.limemedia.cz:8000/blanikcz128.mp3"));
        radios.add(new Radio("Český rozhlas 2", "http://amp1.cesnet.cz:8000/cro2-256.ogg"));
        radios.add(new Radio("Impuls", "https://listenonline.eu/00000375.m3u"));
        radios.add(new Radio("Frekvence 1", "https://listenonline.eu/00000224.m3u"));
        radios.add(new Radio("Beat", "http://www.play.cz/radio/beat128.ogg.m3u"));

        final Spinner selectRadio = (Spinner) findViewById(R.id.selectRadio);

        ArrayAdapter<Radio> radiosAdapter = new ArrayAdapter<Radio>(this, R.layout.spinner_layout, radios);
        // android.R.layout.simple_spinner_dropdown_item
        selectRadio.setAdapter(radiosAdapter);
        selectRadio.setSelected(false);

        selectRadio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String radio = selectRadio.getSelectedItem().toString();
                String url = "";

                for (Radio r : radios)
                {
                    if(r.getName() == radio) url = r.getUrl();
                }

                JSONObject params = new JSONObject();
                try {
                    JSONObject paramsFile = new JSONObject();
                    paramsFile.put("file", url);
                    params.put("item", paramsFile);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JSONObject request = createJSONRequest("Player.Open", params);
                new MakeJSONRequest().execute(request);

                Toast.makeText(getApplicationContext(), radio, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button buttonRadioOff = (Button) findViewById(R.id.buttonRadioOff);

        buttonRadioOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject params = new JSONObject();
                try {
                    params.put("playerid", 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JSONObject request = createJSONRequest("Player.Stop", params);

                new MakeJSONRequest().execute(request);
            }
        });

        Button buttonVolumeDown = (Button) findViewById(R.id.buttonVolumeDown);

        buttonVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject params = new JSONObject();
                int decvolume = getVolume() - 10;
                if (decvolume < 0) decvolume = 0;

                try {
                    params.put("volume", decvolume);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                JSONObject request = createJSONRequest("Application.SetVolume", params);
                new MakeJSONRequest().execute(request);
                getVolume();
            }
        });

        Button buttonVolumeUp = (Button) findViewById(R.id.buttonVolumeUp);

        buttonVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject params = new JSONObject();
                int incvolume = getVolume() + 10;
                if (incvolume > 100) incvolume = 100;

                try {
                    params.put("volume", incvolume);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                JSONObject request = createJSONRequest("Application.SetVolume", params);
                new MakeJSONRequest().execute(request);
                getVolume();
            }
        });

        getVolume();
    }

    public int getVolume() {
        JSONObject params = new JSONObject();
        JSONArray properties = new JSONArray();
        properties.put("volume");
        try {
            params.put("properties", properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject request = createJSONRequest("Application.GetProperties", params);
        JSONObject result = null;
        try {
            result = new JSONObject(new MakeJSONRequest().execute(request).get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TextView textVolume = (TextView) findViewById(R.id.textVolume);
        ProgressBar progressVolume = (ProgressBar) findViewById(R.id.progressVolume);

        try {
            textVolume.setText(String.valueOf(result.getInt("volume")));
            progressVolume.setProgress(result.getInt("volume"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return progressVolume.getProgress();
    }

    private JSONObject createJSONRequest(String method, JSONObject params) {
        JSONObject request = new JSONObject();
        try {

            request.put("id", UUID.randomUUID().hashCode());
            request.put("jsonrpc", "2.0");
            request.put("method", method);

            if (params != null) request.put("params", params);

            return request;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    public class MakeJSONRequest extends AsyncTask<JSONObject, Void, String> {

        @Override
        protected String doInBackground(JSONObject... params) {
            URL url = null;
            try {
                JSONObject jsonRequest = params[0];

                url = new URL("http://192.168.1.114:8080/jsonrpc");

                Log.v(TAG, "Sending: " + jsonRequest.toString() + " to " + url.toString());

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);

                con.setRequestProperty("Content-Type", "application/json; charset=utf8");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Method", "POST");

                OutputStream os = con.getOutputStream();
                os.write(jsonRequest.toString().getBytes("UTF-8"));
                os.close();

                StringBuilder sb = new StringBuilder();
                int HttpResult = con.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));

                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    Log.v(TAG, "" + sb.toString());

                    try {
                        JSONObject msg = new JSONObject(sb.toString());
                        con.disconnect();
                        return msg.getString("result");
                    } catch (JSONException ex) {
                        con.disconnect();
                        return "Chyba v překladu";
                    }
                } else {
                    Log.v(TAG, "Response code: " + con.getResponseCode());
                    Log.v(TAG, "Response msg: " + con.getResponseMessage());

                    con.disconnect();
                    return "Chyba ovládání";

                }
            } catch (MalformedURLException e) {
                Log.v(TAG, "ERROR MalformedURLException");
                e.printStackTrace();
            } catch (UnsupportedEncodingException e1) {
                Log.v(TAG, "ERROR UnsupportedEncodingException");
                e1.printStackTrace();
            } catch (IOException e1) {
                Log.v(TAG, "ERROR IOException");
                e1.printStackTrace();


            }
            return "nothing returned";
        }

        @Override
        protected void onPostExecute(String result) {
            TextView textStatus = (TextView) findViewById(R.id.textStatus);
            textStatus.setText("Status: " + result);

            super.onPostExecute(result);
        }
    }
}
