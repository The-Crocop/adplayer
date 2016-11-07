package verbavoice.de.audiodescriptionplayer.infinoted;

//import android.util.Log;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import de.lits.adopted.exception.RequestException;
import de.lits.adopted.model.Buffer;
import de.lits.adopted.text.model.segmented.BufferSimple;
import de.lits.infinoted.InfinotedClient;
import de.lits.infinoted.InfinotedClientException;
import de.lits.infinoted.InfinotedSession;
import de.lits.infinoted.event.InfinotedClientHandler;
import de.lits.infinoted.event.SessionEventCallback;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class takes care of the connection between our service and the infinoted
 * server it makes a connection takes care of writing the transcription results
 * to the infinoted server and closes the connection
 *
 * @author Marko Nalis
 * @author Benny
 *
 */
public class InfiManager implements Serializable {

    private final static String TAG = "InfiManager";

    private static final String USER = "StageCap";

    private InfinotedClient client;

    private BufferWrapper buffer;

    private InfinotedSession session;

    private String path;

    private boolean connected;

    private VltConnectionListener delegate;

    private List<TextListener> listenerList = new ArrayList<>();



    /**
     * constructor
     */
    public InfiManager() {
        client = new InfinotedClient(new XmlPullConnection());

        client.setClientHandler(new InfinotedClientHandler() {

            @Override
            public void onError(Throwable arg0) {
                System.out.println(TAG +": onError " + path);
                close();
            }

            @Override
            public void onConnect() {
                //System.out.println("connected!!!");
                buffer = new BufferWrapper();
                buffer.add(textListener);
                client.createSession(InfiManager.this.path, buffer, new SessionEventCallback() {

                    @Override
                    public void onSessionSynced(InfinotedSession arg0) {
                        //Log.i(TAG,"Session synched" + path+" !!!");
                        session = arg0;
                        session.join(USER, buffer.length(), 0.123456789);
                    }

                    @Override
                    public void onSessionSyncError() {
                        Log.e(TAG,"onSessionSyncError! " + path);
//                        notifyListeners(VltIntent.SESSION_ERROR);
                        close();
                    }

                    @Override
                    public void onSessionSubscribeError() {
//                        notifyListeners(VltIntent.SESSION_ERROR);
                        Log.e(TAG,"onSessionSubscribeError! "+ path);
                        close();
                    }

                    @Override
                    public void onSessionJoin() {
                        connected = true;
                        delegate.onConnected();
                        Log.d(TAG, "Connected to " + " " + path);
                        //notify Fragments
                        //removeAll();
                        //close();
                        //Log.i(TAG,"session joined!!!" + path);
                    }

                    @Override
                    public void onSessionError() {
                        //Log.e(TAG,"onSessionError! "+ path);
                        //notify Fragments , show loadin dialog try to reconnect
//                        notifyListeners(VltIntent.SESSION_ERROR);
                        close();
                    }

                    @Override
                    public void onSessionClose() {
                        //Log.i(TAG,"session closed!!! "+ path);
//                        notifyListeners(VltIntent.SESSION_ERROR);
                        close();
                    }
                });
            }
        });

    }

    /**
     * initialize connection to infinoted server
     *
     * @param host
     *            url of infinoted
     * @param path
     *            name of document
     * @throws InfinotedClientException
     */
    public void connect(String host, String path) throws InfinotedClientException {
        new ConnectTask(host).execute();
        this.path = path;
    }

    /**
     * add text to the infinoted document we are connected to be careful with
     * formats like utf-8 and iso there may be errors between different
     * operating systems like windows and ubuntu
     *
     * @param text
     *            text to add to the infinoted document
     * @throws RequestException
     */
    public void appendText(String text) throws RequestException {
        //	session.insertText(0,text)
        // builder.append(newtxt.trim());
        session.insertText(0, text);
        Log.d(TAG, buffer.getText());
    }

    public void removeAll() throws RequestException {
        if(buffer.length()>0)session.deleteText(0,buffer.length());
    }

    /**
     * getter for connection status
     *
     * @return return true if we are connected else return false
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * closes connection to infinoted sevrer
     */
    public void close() {
        //Log.i(TAG, "close infinote session!");
        if (connected) {
            connected = false;
            try {
                client.disconnect();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void setDelegate(VltConnectionListener delegate) {
        this.delegate = delegate;
    }

    public boolean add(TextListener object) {
        return listenerList.add(object);
    }

    public boolean remove(Object object) {
        return listenerList.remove(object);
    }

    private TextListener textListener = new TextListener() {
        @Override
        public void onTextUpdate(String text) {
            for(TextListener listener:listenerList)listener.onTextUpdate(text);
        }

        @Override
        public void onLoading() {
            for(TextListener listener:listenerList)listener.onLoading();
        }

        @Override
        public void onLoaded() {
            for(TextListener listener:listenerList)listener.onLoaded();
        }

        @Override
        public void onError(String message) {
            for(TextListener listener:listenerList)listener.onError(message);
        }

        @Override
        public void onConnectionError() {
            for(TextListener listener:listenerList)listener.onConnectionError();
        }
    };


    //    private void notifyListeners(String intent) {
//        LocalBroadcastManager.getInstance(MyApp.getContext()).sendBroadcast(new Intent(intent));
//    }

    class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private String host;

        public ConnectTask(String host) {
            this.host = host;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                client.connect(host, "SubPresenter");
                return true;
            } catch (InfinotedClientException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (delegate != null) {
                if (!success) {
                    delegate.onConnectionError();
                    Log.d(TAG, "Error connecting to " + host + " " + path);
                }
            }
        }
    }
}