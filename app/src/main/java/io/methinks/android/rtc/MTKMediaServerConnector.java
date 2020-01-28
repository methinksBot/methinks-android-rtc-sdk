package io.methinks.android.rtc;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MTKMediaServerConnector {
    private static final String TAG = MTKMediaServerConnector.class.getSimpleName();
    protected static final String URL = "http://ec2-3-84-189-62.compute-1.amazonaws.com:3000";
    protected Callback callback;

    public void getPorts(String fileName, Callback callback){



        this.callback = callback;
        String[]args = new String[]{URL + "/api/v1/port", fileName};
        new MTKMediaServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    }


    class MTKMediaServerTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                JSONObject args = new JSONObject();
                args.put("fileName", params[1]);


                java.net.URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Accept-Charset", "utf-8");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                OutputStream os = connection.getOutputStream();
                os.write(args.toString().getBytes());
                os.flush();

                connection.connect();

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while((line = reader.readLine()) != null){
                    buffer.append(line);
                }
                JSONObject json = new JSONObject(buffer.toString());

                return json;
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if(json != null){
                callback.getResult(json);
                return;
            }

            callback.getResult(null);
        }
    }

    public void close(int pid, Callback callback){

        this.callback = callback;
        String[]args = new String[]{URL + "/api/v1/close", String.valueOf(pid)};
        new MTKMediaServerStopTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    }

    class MTKMediaServerStopTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                JSONObject args = new JSONObject();
                args.put("pid", Integer.parseInt(params[1]));

                java.net.URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Accept-Charset", "utf-8");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
//                OutputStream os = connection.getOutputStream();
//                os.write(args.toString().getBytes());
//                os.flush();

                connection.connect();

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while((line = reader.readLine()) != null){
                    buffer.append(line);
                }
                JSONObject json = new JSONObject(buffer.toString());

                return json;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if(json != null){
                callback.getResult(json);
                return;
            }

            callback.getResult(null);
        }
    }

    public interface Callback{
        void getResult(JSONObject json);
    }
}
