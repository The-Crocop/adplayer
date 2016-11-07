package verbavoice.de.audiodescriptionplayer.infinoted;

import java.io.Serializable;

/**
 * Created by crocop on 04.01.16.
 */
public interface VltConnectionListener extends Serializable {
    void onConnected();

    void onConnectionError();
}
