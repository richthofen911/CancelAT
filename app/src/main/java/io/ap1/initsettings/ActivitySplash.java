package io.ap1.initsettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ActivitySplash extends AppCompatActivity {

    public static SharedPreferences spAppSettings;
    private SharedPreferences.Editor editorAppSettings;
    private final String urlCheckSettingsUpdateTimestamp = "http://104.236.111.213/aloha/api/latestupdate.php";
    private final String urlGetSettingsContent = "http://104.236.111.213/aloha/api/settings.php";
    private volatile String networkStatus = "bad";
    private volatile CheckAppSettingsLastUpdateTimestamp checkAppSettingsLastUpdateTimestamp;
    private volatile GetRemoteSettingsUpdateContent getRemoteSettingsUpdateContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ((ImageView) findViewById(R.id.iv_splash)).setImageResource(R.drawable.splash2);

        if(((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() == null)
            Toast.makeText(getApplicationContext(), "Network is currently unavailable", Toast.LENGTH_SHORT).show();

        spAppSettings = getApplication().getSharedPreferences("AppSettings", 0);
        editorAppSettings = spAppSettings.edit();
        //get local last settings update timestamp, for new installed app, it's 0
        long lastUpdateTimestamp = spAppSettings.getLong("lastUpdateTimestamp", 0);
        //compare the local timestamp with the one retrieving from remote server
        checkAppSettingsLastUpdateTimestamp = new CheckAppSettingsLastUpdateTimestamp();
        checkAppSettingsLastUpdateTimestamp.execute(lastUpdateTimestamp);

        Thread countTime = new Thread(new Runnable() { // splash screen stays for 3 seconds
            @Override
            public void run() {
                try{
                    Thread.sleep(3000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                startMainScreen(networkStatus);
            }
        });
        countTime.start();

    }

    //this class check the remote server's latest settings update timestamp, if the result is
    //bigger than the local when, it retrieves the new update content by executing -->
    //getRemoteSettingsUpdateContent
    private class CheckAppSettingsLastUpdateTimestamp extends AsyncTask<Long, String, String>{
        long localLastSettingsUpdateTimestamp;

        @Override
        protected String doInBackground(Long... params) {
            localLastSettingsUpdateTimestamp = params[0];

            try{
                HttpURLConnection connection = (HttpURLConnection) new URL(urlCheckSettingsUpdateTimestamp).openConnection();
                connection.connect();
                Log.e("checkUpdate", "");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer stringBuffer = new StringBuffer();
                String tmp = reader.readLine();
                while (tmp != null){
                    stringBuffer.append(tmp);
                    tmp = reader.readLine();
                }
                Log.e("resp:", stringBuffer.toString());
                return stringBuffer.toString();

            }catch (MalformedURLException e){
                e.printStackTrace();
                return "exception";
            }catch (IOException e){
                e.printStackTrace();
                return "exception";
            }
        }
        @Override
        protected void onPostExecute(String result){
            if(!result.equals("exception")){
                result = result.replace("\\", "");
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    long remoteLastSettingsUpdateTimestamp = jsonObject.getLong("last update");
                    if(remoteLastSettingsUpdateTimestamp > localLastSettingsUpdateTimestamp){
                        editorAppSettings.putLong("lastUpdateTimestamp", remoteLastSettingsUpdateTimestamp).commit();
                        getRemoteSettingsUpdateContent = new GetRemoteSettingsUpdateContent();
                        getRemoteSettingsUpdateContent.execute();
                    }else {
                        networkStatus = "good";
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }else
                Toast.makeText(getApplicationContext(), "Exception error", Toast.LENGTH_SHORT).show();
        }
    }

    private class GetRemoteSettingsUpdateContent extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... params) {
            try{
                HttpURLConnection connection = (HttpURLConnection) new URL(urlGetSettingsContent).openConnection();
                connection.connect();
                Log.e("get update content", "");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer stringBuffer = new StringBuffer();
                String tmp = reader.readLine();
                while (tmp != null){
                    stringBuffer.append(tmp);
                    tmp = reader.readLine();
                }
                Log.e("resp:", stringBuffer.toString());
                return stringBuffer.toString();

            }catch (MalformedURLException e){
                e.printStackTrace();
                return "exception";
            }catch (IOException e){
                e.printStackTrace();
                return "exception";
            }
        }
        @Override
        protected void onPostExecute(String result){
            if(!result.equals("exception")){
                result = result.replace("\\", "");
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    editorAppSettings.putString("id", jsonObject.getString("id"));
                    editorAppSettings.putString("checkout_time", jsonObject.getString("checkout_time"));
                    editorAppSettings.putString("max_show_length", jsonObject.getString("max_show_length"));
                    editorAppSettings.putString("min_show_length", jsonObject.getString("min_show_length"));
                    editorAppSettings.putString("refresh_every", jsonObject.getString("refresh_every"));
                    editorAppSettings.putString("display_notification_time", jsonObject.getString("display_notification_time"));
                    editorAppSettings.putString("firebase_server", jsonObject.getString("firebase_server"));
                    editorAppSettings.putString("firebase_serverM", jsonObject.getString("firebase_serverM"));
                    editorAppSettings.putString("default_video", jsonObject.getString("default_video"));
                    editorAppSettings.putString("videopath", jsonObject.getString("videopath"));
                    editorAppSettings.putString("act", jsonObject.getString("act"));
                    editorAppSettings.putString("token", jsonObject.getString("token"));
                    editorAppSettings.putString("tokenM", jsonObject.getString("tokenM")).commit();
                }catch (JSONException e){
                    e.printStackTrace();
                }
                networkStatus = "good";
            }else
                Toast.makeText(getApplicationContext(), "Exception error", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMainScreen(String networkStatus){
        //if the AsyncTasks are still running, probably because the network connection is bad, just cancel them
        if(checkAppSettingsLastUpdateTimestamp != null){
            if(checkAppSettingsLastUpdateTimestamp.getStatus() == AsyncTask.Status.RUNNING)
                checkAppSettingsLastUpdateTimestamp.cancel(true);
        }
        if(getRemoteSettingsUpdateContent != null){
            if(getRemoteSettingsUpdateContent.getStatus() == AsyncTask.Status.RUNNING)
                getRemoteSettingsUpdateContent.cancel(true);
        }
        startActivity(new Intent(ActivitySplash.this, ActivityMain.class).putExtra("networkStatus", networkStatus));
        finish();  //this activity will be destroyed after it starts the main activity
    }

}
