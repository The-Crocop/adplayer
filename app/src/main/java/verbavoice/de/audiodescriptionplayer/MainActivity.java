package verbavoice.de.audiodescriptionplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import de.lits.infinoted.InfinotedClientException;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private PlayerService playerService;
    boolean bound = false;



    private EditText hostEditText;
    private EditText pathEditText;
    private TextView textView;
    private TextView stateTextView;
    private Button connectBtn;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectBtn = (Button) findViewById(R.id.connectButton);
        hostEditText = (EditText)findViewById(R.id.hostEditText);
        pathEditText = (EditText)findViewById(R.id.pathEditText);
        textView = (TextView)findViewById(R.id.textView);
        stateTextView = (TextView) findViewById(R.id.stateTextView);
        spinner = (Spinner) findViewById(R.id.tts_spinner);
        initializeSpinner();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        MessageHandler handler = new MessageHandler();
        intent.putExtra("MESSENGER", new Messenger(handler));
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            playerService = binder.getService();
             bound = true;
            playerService.dismissNotification();
            init(playerService.getLastText(), playerService.getConnectionState());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        Intent intent = new Intent(this, PlayerService.class);
        boolean connected = false;
        if (bound) {
            connected = playerService.isConnected();
            unbindService(connection);
            bound = false;
        }
        if(!connected){
           stopService(intent);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, PlayerService.class);
        stopService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(playerService.isConnected()){
            playerService.addNotification();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    public class MessageHandler extends  Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PlayerMessages.TEXT_UPDATE:
                    final String text = (String) msg.obj;
                    updateText(text);
                    break;
                case PlayerMessages.ON_LOADING:
                    onLoading();
                    break;
                case PlayerMessages.ON_LOADED:
                    onLoaded();
                    break;
                case PlayerMessages.ON_ERROR:
                    final String message = (String) msg.obj;
                    onError(message);
                    break;
                case PlayerMessages.ON_CONNECTION_ERROR:
                    onConnectionError();
                    break;
                case PlayerMessages.ON_CONNECTED:
                    onConnected();
                    break;
                default:
                    //do nothing
            }
        }
    };

    private void updateText(final String text) {
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    private void onLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stateTextView.setText("loading...");
            }
        });
    }

    private void onLoaded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stateTextView.setText(R.string.loaded);
            }
        });
    }

    private void onError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectBtn.setText(R.string.connect);
                stateTextView.setText("error:"+ message);
            }
        });
    }

    private void onConnectionError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectBtn.setText(R.string.connect);
                stateTextView.setText("on Connection Error");
            }
        });
    }

    private void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,"connected!",Toast.LENGTH_SHORT).show();
                stateTextView.setText("Connected!");
                connectBtn.setText(R.string.disconnect);
            }
        });
    }

    private void init(String text, ConnectionState connectionState) {
        updateText(text);
        stateTextView.setText(connectionState.name());
        if(!(connectionState == ConnectionState.CONNECTED))connectBtn.setText(R.string.connect);
        else  connectBtn.setText(R.string.disconnect);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connect(View view){
        if(!playerService.isConnected()){
            String host = hostEditText.getText().toString();
            String path = pathEditText.getText().toString();


            try {
                playerService.connect(host,path);
            } catch (InfinotedClientException e) {
                e.printStackTrace();
                Toast.makeText(this,"An error Occured!", Toast.LENGTH_SHORT).show();
            }

        }else {
            playerService.close();
            connectBtn.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    connectBtn.setText(R.string.connect);
                    stateTextView.setText(R.string.disconnect);
                }
            });
        }
    }

    public void initializeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.tts_languages, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String language = (String)adapterView.getSelectedItem();
        if(bound) {
            switch (language){
                case "GERMAN":
                    playerService.setLanguage(Locale.GERMANY);
                    break;
                case "ENGLISH":
                    playerService.setLanguage(Locale.ENGLISH);
                    break;
                case "ITALIAN":
                    playerService.setLanguage(Locale.ITALY);
                    break;
                case "FRENCH":
                    playerService.setLanguage(Locale.FRANCE);
                    break;
                default:
                    playerService.setLanguage(Locale.ENGLISH);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
