package verbavoice.de.audiodescriptionplayer.infinoted;



/**
 * Created by Marko Nalis on 03.08.2015.
 */
public interface TextListener {
    void onTextUpdate(String text);
    void onLoading();
    void onLoaded();

    void onError(String message);
    void onConnectionError();

}
