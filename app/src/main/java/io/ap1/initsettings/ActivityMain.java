package io.ap1.initsettings;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ActivityMain extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.tv_display)).setText(ActivitySplash.spAppSettings.getString("firebase_server", null)
                + "\n network status: " + getIntent().getStringExtra("networkStatus"));

    }

}
