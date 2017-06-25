package xyz.simek.radiokodi.radiokodi;

import android.os.AsyncTask;
import android.util.Log;

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


/**
 * http://kodi.wiki/view/JSON-RPC_API/Examples
 * - are we connected to the right Player? Is it type=audio?
 *  - {"jsonrpc": "2.0", "method": "Player.GetActivePlayers", "id": 1}
 * - catch errors
 *  - {"error":{"code":-32100,"message":"Failed to execute method."},"id":1419505815,"jsonrpc":"2.0"}
 *  - http://192.168.1.114:8080/jsonrpc?request={%22id%22:1419505815,%22jsonrpc%22:%222.0%22,%22method%22:%22Player.Stop%22,%22params%22:{%22playerid%22:0}}
 * - try/catch connection errors
 * - sleep when idle?
 */

public class KodiJSON {

    private static final String TAG = "JSON";

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

    private ArrayList<Radio> radios;

    // FIXME: setting playerid to 1 is wrong, but calling GetActivePlayer() every time is slooow
    private int playerid = 1;

    public KodiJSON()
    {
        // Available streams
        radios = new ArrayList<>();
        radios.add(new Radio("Blaník", "http://ice.abradio.cz/blanikfm128.mp3.m3u"));
        radios.add(new Radio("Český rozhlas 2", "http://amp1.cesnet.cz:8000/cro2-256.ogg"));
        radios.add(new Radio("Impuls", "http://www.play.cz/radio/impuls128.mp3.m3u"));
        radios.add(new Radio("Frekvence 1", "http://icecast4.play.cz/frekvence1-128.mp3.m3u"));
        //radios.add(new Radio("Beat", "http://www.play.cz/radio/beat128.ogg.m3u"));

        // FIXME: check connection first

        //JSONObject request = createJSONRequest("JSONRPC.Version");

    }

    public boolean selectRadio(String radio)
    {
        String url = "";

        for (KodiJSON.Radio r : radios)
        {
            if(r.getName() == radio)
            {
                url = r.getUrl();
                break;
            }
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

        return true;
    }

    // http://192.168.1.114:8080/jsonrpc?request={%22jsonrpc%22:%20%222.0%22,%20%22id%22:%201,%20%22method%22:%20%22Player.GetActivePlayers%22}
    public boolean getActivePlayer()
    {
        JSONObject request = createJSONRequest("Player.GetActivePlayers");
        JSONArray result = null;

        try {
            result = new JSONArray(checkJSONResponse(request));

            for (int i = 0; i < result.length(); i++)
            {
                JSONObject active = result.getJSONObject(i);

                if(active.getString("type").equals("audio"))
                {
                    this.playerid = active.getInt("playerid");
                    Log.i(TAG, "Active player set to " + this.playerid);

                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getCurrentlyPlaying()
    {
        String file = "";
        JSONObject params = new JSONObject();

        try {
            params.put("playerid", this.playerid);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject request = createJSONRequest("Player.GetItem", params);
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

        if(result.has("item"))
        {
            try {
                file = result.getJSONObject("item").getString("label");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.i(TAG, file);

        for (Radio r : radios)
        {
            String filename = r.url.substring(r.url.lastIndexOf('/') + 1, r.url.length());

            if(file.equals(filename))
            {
                return r.name;
            }
        }

        return "Rádio nehraje";
    }


    public String checkJSONResponse(JSONObject request)
    {
        String parsed = null;

        try {
            parsed = new MakeJSONRequest().execute(request).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        //} catch (JSONException e) {
        //    e.printStackTrace();
        }

        return parsed;
    }

    public ArrayList<Radio> getRadios()
    {
        return radios;
    }

    public JSONObject createJSONRequest(String method) {
        return createJSONRequest(method, null);
    }

    public JSONObject createJSONRequest(String method, JSONObject params) {
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

    public void stopRadio()
    {
        JSONObject params = new JSONObject();
        try {
            params.put("playerid", this.playerid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject request = createJSONRequest("Player.Stop", params);

        new MakeJSONRequest().execute(request);
    }

    public int changeVolume(int volume)
    {
        JSONObject params = new JSONObject();
        int decvolume = getVolume() + volume;
        if (decvolume < 0) decvolume = 0;
        if (decvolume > 100) decvolume = 100;

        try {
            params.put("volume", decvolume);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        JSONObject request = createJSONRequest("Application.SetVolume", params);
        new MakeJSONRequest().execute(request);

        return getVolume();
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

        int volume = 0;
        try {
            volume = result.getInt("volume");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return volume;
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
                        if(msg.has("result"))
                        {
                            con.disconnect();
                            return msg.getString("result");
                        }
                        else if (msg.has("error"))
                        {
                            Log.e(TAG, "Kodi error: " + msg.getJSONObject("error").getString("message"));
                        }
                        else
                        {
                            Log.e(TAG, "Unknown Kodi message: " + msg.toString());
                        }
                    } catch (JSONException ex) {
                        //con.disconnect();
                        Log.e(TAG, "JSONException caught: " + ex.getMessage());
                        //return "{\"error\":{ \"message\": \"Chyba v překladu\"}}";
                    }
                } else {
                    Log.e(TAG, "Response code: " + con.getResponseCode());
                    Log.e(TAG, "Response msg: " + con.getResponseMessage());
                }

                con.disconnect();

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
            return "{\"error\":{ \"message\": \"nothing returned\"}}";
        }

        @Override
        protected void onPostExecute(String result) {
            /*
            TextView textStatus = (TextView) findViewById(R.id.textStatus);
            textStatus.setText("Status: " + result);
            */
            super.onPostExecute(result);
        }
    }
}
