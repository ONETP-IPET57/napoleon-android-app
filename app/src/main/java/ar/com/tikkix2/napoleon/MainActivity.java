package ar.com.tikkix2.napoleon;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Locale;

import ar.com.tikkix2.napoleon.databinding.ActivityMainBinding;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "mainActivity";
    private static final String API_URL = "http://thejairex.pythonanywhere.com/api/exhibitions";
    private static final String NAPOLEON_URL = "https://napoleon.tikkix2.com.ar";

    TextToSpeech mTtsInstance;

    private AppBarConfiguration mAppBarConfiguration;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://socket.napoleon.tikkix2.com.ar");
            Log.v(TAG, "socket is created");
        } catch (URISyntaxException e) {
            Log.v(TAG, "instance initializer: ", e);
        }
    }

    private int mLastIdExhibition = -1;

    private TextView mTextFetchAuthor;
    private TextView mTextFetchCreatedAt;
    private TextView mTextFetchName;
    private TextView mTextFetchInformation;

    private FloatingActionButton mFAB;

    AlertDialog.Builder builder;
    AlertDialog mLastAlert;

    public void updateFABVisibility() {
        if (mFAB != null) mFAB.setVisibility(mLastIdExhibition == -1 ? View.GONE : View.VISIBLE);
    }

    private void alertSmartPoint() {
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, API_URL, null, response -> {
            if (mLastAlert != null && mLastAlert.getClass() == AlertDialog.class) {
                mLastAlert.cancel();
            }
            mTtsInstance.speak(getString(R.string.smart_point_alert_message), TextToSpeech.QUEUE_FLUSH, null);
            String author = "";
            String createdAt = "";
            String information = "";
            String nameExhibition = "";

            try {
                for (int i = 0; i < response.length(); i++) {
                    JSONObject item = (JSONObject) response.get(i);
                    int item_id_exhibition = item.getInt("id_exhibition");
                    if (item_id_exhibition == (int) mLastIdExhibition) {
                        author = getString(R.string.smart_point_text_author) + ": " + item.getString("author");
                        createdAt = getString(R.string.smart_point_text_created_at) + ": " + item.getInt("created_at");
                        information = getString(R.string.smart_point_text_information) + ": " + item.getString("information");
                        nameExhibition = getString(R.string.smart_point_text_name_exhibition) + ": " + item.getString("name_exhibition");
                        int beepcon = item.getInt("beepcons");

                        JSONObject json = new JSONObject();
                        try {
                            json.put("author", "client");
                            json.put("beepconLocation", beepcon);
                            Log.v(TAG, "emit action");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.v(TAG, "emit action error", e);
                        }
                        mSocket.emit("createdLocation", json);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.v(TAG, "json exception error", e);
            }

            String finalAuthor = author;
            String finalCreatedAt = createdAt;
            String finalNameExhibition = nameExhibition;
            String finalInformation = information;
            builder.setMessage(getString(R.string.smart_point_alert_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.smart_point_alert_yes), (dialog, id) -> {
                        mTtsInstance.speak(getString(R.string.smart_point_alert_yes), TextToSpeech.QUEUE_FLUSH, null);

                        mTextFetchAuthor.setText(finalAuthor);
                        mTtsInstance.speak(finalAuthor, TextToSpeech.QUEUE_ADD, null);
                        mTextFetchCreatedAt.setText(finalCreatedAt);
                        mTtsInstance.speak(finalCreatedAt, TextToSpeech.QUEUE_ADD, null);
                        mTextFetchName.setText(finalNameExhibition);
                        mTtsInstance.speak(finalNameExhibition, TextToSpeech.QUEUE_ADD, null);
                        mTextFetchInformation.setText(finalInformation);
                        mTtsInstance.speak(finalInformation, TextToSpeech.QUEUE_ADD, null);
                    })
                    .setNegativeButton(getString(R.string.smart_point_alert_no), (dialog, id) -> {
                        //  Action for 'NO' Button
                        mTtsInstance.speak(getString(R.string.smart_point_alert_no), TextToSpeech.QUEUE_FLUSH, null);
                        dialog.cancel();
                    });
            //Creating dialog box
            AlertDialog alert = builder.create();
            //Setting the title manually
            alert.setTitle(getString(R.string.smart_point_alert_title));
            alert.show();
            mLastAlert = alert;
            updateFABVisibility();
        }, error -> {
            error.printStackTrace();
            Log.v(TAG, error.getMessage(), error);
        });

        requestQueue.add(jsonArrayRequest);
    }

    private final Emitter.Listener onNewAlert = args -> runOnUiThread(() -> {
        Toast.makeText(getApplicationContext(), getString(R.string.smart_point_alert_title), Toast.LENGTH_LONG).show();

        JSONObject data = (JSONObject) args[0];
        // String author;
        int id_exhibition;
        try {
            // author = data.getString("author");
            id_exhibition = data.getInt("id_exhibition");
        } catch (JSONException e) {
            return;
        }
        mLastIdExhibition = id_exhibition;

        alertSmartPoint();
    });

    private final Emitter.Listener onConnectError = args -> runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Unable to connect to NodeJS server", Toast.LENGTH_LONG).show());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ar.com.tikkix2.napoleon.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view -> {
            if (mLastIdExhibition != -1) {
                alertSmartPoint();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        builder = new AlertDialog.Builder(this);

        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("newIncomingAlert", onNewAlert);
        mSocket.connect();
        Log.v(TAG, "socket connected ");

        mTextFetchAuthor = findViewById(R.id.text_fetch_author);
        mTextFetchCreatedAt = findViewById(R.id.text_fetch_createdAt);
        mTextFetchName = findViewById(R.id.text_fetch_name);
        mTextFetchInformation = findViewById(R.id.text_fetch_information);

        mTtsInstance = new TextToSpeech(getApplicationContext(), status -> {
            Toast.makeText(getApplicationContext(), "TTS Status: " + status, Toast.LENGTH_LONG).show();
            if (status != TextToSpeech.ERROR) {
                mTtsInstance.setLanguage(Locale.UK);
                Locale locSpanish = new Locale("spa", "MEX");
                mTtsInstance.setLanguage(locSpanish);
            }
        });

        mFAB = binding.appBarMain.fab;
        updateFABVisibility();
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
        menu.getItem(0).setOnMenuItemClickListener(menuItem -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(NAPOLEON_URL));
            startActivity(browserIntent);
            return false;
        });
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}