package verbavoice.de.audiodescriptionplayer.infinoted;

import android.os.Handler;
import android.os.Looper;



import java.util.ArrayList;
import java.util.List;

import de.lits.adopted.exception.BufferException;
import de.lits.adopted.model.Buffer;
import de.lits.adopted.model.Segment;
import de.lits.adopted.text.model.segmented.BufferSimple;

/**
 * Created by Marko Nalis on 06.10.2015.
 */
public class BufferWrapper extends BufferSimple {

    private final static String TAG = "BufferWrapper";

    private boolean stopped = false;
    private boolean fullWordsOnly = true;
    private Handler handler;
    private BufferWrapper parent;
    private long lastUpdate = 0;
    private long lastSentUpdate = 0;
    private List<TextListener> listenerList = new ArrayList<>();

    private boolean synced = false;


    public BufferWrapper() {
        parent = this;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void appendSegment(final Segment seg) {
        super.appendSegment(seg);
        updateListeners();
    }

    @Override
    public void splice(final int position, final int length, Buffer text) throws BufferException {

        super.splice(position, length, text);
        final String tex = text != null ? text.getText() : "";
        lastUpdate = System.currentTimeMillis();
        updateListeners();

    }

    public void updateListeners() {
        for (TextListener listener : listenerList) {
           listener.onTextUpdate(getText());
        }
        lastSentUpdate = lastUpdate;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
        updateListeners();
    }

    public List<TextListener> getListenerList() {
        return listenerList;
    }

    public void setListenerList(List<TextListener> listenerList) {
        this.listenerList = listenerList;
    }

    public void stop() {
        stopped = true;
    }

    public boolean add(TextListener textListener) {
        return listenerList.add(textListener);
    }

    public TextListener remove(int i) {
        return listenerList.remove(i);
    }

    public boolean remove(Object o) {
        return listenerList.remove(o);
    }

}
