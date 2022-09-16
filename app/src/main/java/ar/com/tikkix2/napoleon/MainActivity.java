package ar.com.tikkix2.napoleon;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import ar.com.tikkix2.napoleon.databinding.ActivityMainBinding;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "mainActivity";

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://socket.napoleon.tikkix2.com.ar");
            Log.v(TAG, "socket is created");
        } catch (URISyntaxException e) {
            Log.v(TAG, "instance initializer: ", e);
        }
    }

    private TextView mText;

    private Emitter.Listener onNewAlert = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Smart Point Alert", Toast.LENGTH_LONG).show();

                    JSONObject data = (JSONObject) args[0];
                    String author;
                    int id_exhibition;
                    try {
                        author = data.getString("author");
                        id_exhibition = data.getInt("id_exhibition");
                    } catch (JSONException e) {
                        return;
                    }

                    // add the message to view
                    mText.setText(author + " - id: " + id_exhibition);
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Unable to connect to NodeJS server", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Log.v(TAG, "action");
                JSONObject json = new JSONObject();
                try {
                    json.put("author", "client");
                    json.put("msg", "hello web");
                    Log.v(TAG, "emit action");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("createdAlert", json);
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("newIncomingAlert", onNewAlert);
        mSocket.connect();
        Log.v(TAG, "socket connected ");

        mText = findViewById(R.id.text_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off("newIncomingAlert", onNewAlert);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}