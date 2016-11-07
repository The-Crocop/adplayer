package verbavoice.de.audiodescriptionplayer;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import de.lits.infinoted.InfinotedClientException;
import verbavoice.de.audiodescriptionplayer.infinoted.InfiManager;
import verbavoice.de.audiodescriptionplayer.infinoted.TextListener;
import verbavoice.de.audiodescriptionplayer.infinoted.VltConnectionListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private InfiManager infiManager = new InfiManager();


    private EditText hostEditText;
    private EditText pathEditText;
    private TextView textView;
    private TextView stateTextView;
    private Button connectBtn;
    private Spinner spinner;

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        infiManager.setDelegate(new VltConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"connected!",Toast.LENGTH_SHORT).show();
                        stateTextView.setText("Connected!");
                        connectBtn.setText(R.string.disconnect);
                    }
                });
            }

            @Override
            public void onConnectionError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Error!",Toast.LENGTH_SHORT).show();
                        stateTextView.setText("Error Connecting");
                    }
                });
            }
        });
        infiManager.add(textListener);
        connectBtn = (Button) findViewById(R.id.connectButton);
        hostEditText = (EditText)findViewById(R.id.hostEditText);
        pathEditText = (EditText)findViewById(R.id.pathEditText);
        textView = (TextView)findViewById(R.id.textView);
        stateTextView = (TextView) findViewById(R.id.stateTextView);
        spinner = (Spinner) findViewById(R.id.tts_spinner);
        initializeSpinner();

    }

    @Override
    protected void onResume() {
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.GERMANY);
                    textToSpeech.setSpeechRate(1.6f);
                }
            }
        });
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private TextListener textListener = new TextListener() {
        @Override
        public void onTextUpdate(final String text) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                    textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
                }
            });

        }

        @Override
        public void onLoading() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stateTextView.setText("loading...");
                }
            });
        }

        @Override
        public void onLoaded() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        stateTextView.setText(R.string.loaded);
                    }
                });
        }

        @Override
        public void onError(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectBtn.setText(R.string.connect);
                    stateTextView.setText("error:"+ message);
                }
            });
        }

        @Override
        public void onConnectionError() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectBtn.setText(R.string.connect);
                    stateTextView.setText("on Connection Error");
                }
            });
        }
    };
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
        if(!infiManager.isConnected()){
            String host = hostEditText.getText().toString();
            String path = pathEditText.getText().toString();


            try {
                infiManager.connect(host,path);
            } catch (InfinotedClientException e) {
                e.printStackTrace();
                Toast.makeText(this,"An error Occured!", Toast.LENGTH_SHORT).show();
            }

        }else {
            infiManager.close();
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
        switch (language){
            case "GERMAN":
                textToSpeech.setLanguage(Locale.GERMANY);
                break;
            case "ENGLISH":
                textToSpeech.setLanguage(Locale.ENGLISH);
                break;
            case "ITALIAN":
                textToSpeech.setLanguage(Locale.ITALY);
                break;
            case "FRENCH":
                textToSpeech.setLanguage(Locale.FRANCE);
                break;
            default:
                textToSpeech.setLanguage(Locale.GERMANY);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
