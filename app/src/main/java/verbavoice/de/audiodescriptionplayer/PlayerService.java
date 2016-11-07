package verbavoice.de.audiodescriptionplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import de.lits.infinoted.InfinotedClientException;
import verbavoice.de.audiodescriptionplayer.infinoted.InfiManager;
import verbavoice.de.audiodescriptionplayer.infinoted.TextListener;
import verbavoice.de.audiodescriptionplayer.infinoted.VltConnectionListener;

import java.util.Locale;

public class PlayerService extends Service implements TextToSpeech.OnInitListener{

    private final static String TAG = "PlayerService";
    private final static int NOTIFICATION_ID = 555777;

    private final IBinder binder = new LocalBinder();
    private final static float SPEECH_RATE = 1.4f;
    private TextToSpeech textToSpeech;
    private InfiManager infiManager = new InfiManager();
    private Messenger messageHandler;
    private String lastText = "";
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;


    public PlayerService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PowerManager mgr = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");

        infiManager.setDelegate(new VltConnectionListener() {
            @Override
            public void onConnected() {
                Log.i(TAG,"On Connected");
                connectionState = ConnectionState.CONNECTED;
                sendMessage(PlayerMessages.ON_CONNECTED);
            }

            @Override
            public void onConnectionError() {
                connectionState = ConnectionState.CONNECTION_ERROR;
                sendMessage(PlayerMessages.ON_CONNECTION_ERROR);
            }
        });
        infiManager.add(textListener);
        textToSpeech=new TextToSpeech(getApplicationContext(), this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");
        dismissNotification();
       return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;

    }

    private TextListener textListener = new TextListener() {
        @Override
        public void onTextUpdate(final String text) {
            connectionState = ConnectionState.CONNECTED;
            textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
            lastText = text;
            sendMessage(PlayerMessages.TEXT_UPDATE,text);

        }

        @Override
        public void onLoading() {
            connectionState = ConnectionState.LOADING;
            sendMessage(PlayerMessages.ON_LOADING);
        }

        @Override
        public void onLoaded() {
            connectionState = ConnectionState.LOADED;
            sendMessage(PlayerMessages.ON_LOADED);

        }

        @Override
        public void onError(final String message) {
            connectionState = ConnectionState.ERROR;
            sendMessage(PlayerMessages.ON_ERROR,message);
        }

        @Override
        public void onConnectionError() {
            connectionState = ConnectionState.CONNECTION_ERROR;
            sendMessage(PlayerMessages.ON_CONNECTION_ERROR);
        }
    };

    public void sendMessage(int messageCode) {
        sendMessage(messageCode,null);
    }

    public void sendMessage(int messageCode, String content) {
        Message msg = Message.obtain();
        msg.obj = content;
        msg.what = messageCode;
        try {
            messageHandler.send(msg);
        }catch ( RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getLastText() {
        return lastText;
    }

    public void setLanguage(Locale locale) {
        textToSpeech.setLanguage(locale);
    }

    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
            textToSpeech.setLanguage(Locale.ENGLISH);
            textToSpeech.setSpeechRate(SPEECH_RATE);
        }
    }

    @Override
    public void onDestroy() {
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        infiManager.close();
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public boolean isConnected() {
        return infiManager.isConnected();
    }

    public void connect(String host, String path) throws InfinotedClientException {
        infiManager.connect(host, path);
    }

    public void close() {
        infiManager.close();
    }

    public class LocalBinder extends Binder {
        PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }


    public void addNotification() {

        // create the notification

        Intent pauseIntent = new Intent();
        Notification.InboxStyle myStyle = new Notification.InboxStyle();


            myStyle.addLine(lastText);


        Notification.Builder m_notificationBuilder = new Notification.Builder(this)
                .setContentTitle("AD Player")
                .setContentText("AD Player")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(myStyle)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(isConnected())
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 10,
                        new Intent(getApplicationContext(), MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        0));
        // send the notification
        startForeground(NOTIFICATION_ID,m_notificationBuilder.build());
    }

    public void dismissNotification(){
        notificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
    }
}
