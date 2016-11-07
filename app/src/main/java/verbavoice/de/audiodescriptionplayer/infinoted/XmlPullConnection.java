package verbavoice.de.audiodescriptionplayer.infinoted;

import android.util.Log;
import android.util.Xml;
import de.lits.infinoted.io.IInfConnectionWorker;
import de.lits.infinoted.io.InfConnection;
import de.lits.infinoted.io.messages.MessageNode;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Marko Nalis on 05.10.2015.
 */
public class XmlPullConnection extends InfConnection {

    private final XmlPullIncomingWorker reader;

    public XmlPullConnection() {
        super();
        reader = new XmlPullIncomingWorker();


    }

    @Override
    protected IInfConnectionWorker getIncomingWorker() {
        return reader;
    }

    @Override
    protected String toXMLString(MessageNode msg) {
        StringWriter xmlString = new StringWriter();
        XmlSerializer writer = Xml.newSerializer();

        try {
            writer.setOutput(xmlString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            nodeToXmlString(msg, writer);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                xmlString.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = null;

        }

        return xmlString.toString();
    }

    private void nodeToXmlString(MessageNode current, XmlSerializer writer) throws IOException {
        writer.startTag("", current.getName());

        for (Map.Entry<String, String> entry : current.getAttributes().entrySet()) {
            writer.attribute("", entry.getKey(), entry.getValue());
        }
        if (current.hasText()) {
            writer.text(current.getText());
        }

        Iterator<MessageNode> iter = current.childIterator();

        while (iter.hasNext()) nodeToXmlString(iter.next(), writer);


        writer.endTag("", current.getName());
    }


    class XmlPullIncomingWorker implements IInfConnectionWorker {

        private final static String VERSION = "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
        private final String TAG = getClass().getName();

        private XmlPullParser parser;
        private boolean run;
        private boolean ignoreErrorOnClose;


        @Override
        public void run() {
            Throwable error = null;

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                parser = factory.newPullParser();
                parser.setInput(new BufferedInputStream(getInputStream()), "UTF-8");


                int eventType = 0;

                eventType = parser.getEventType();

                while (run || eventType != XmlPullParser.END_DOCUMENT) {

                    switch (eventType) {
                        case XmlPullParser.START_DOCUMENT:
                            if (parser != null)
                                Log.d(TAG, "START_DOCUMENT: " + parser.getProperty(VERSION));
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            Log.d(TAG, "END_DOCUMENT");
                            close();
                            break;

                        case XmlPullParser.START_TAG:
                            try {
                                Hashtable<String, String> attributes = new Hashtable<>();
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    String name = parser.getAttributeName(i);
                                    String value = parser.getAttributeValue(i);
                                    Log.d(TAG, "Attribute:" + name + " Value: " + value);
                                    attributes.put(name, value);
                                }
                                getConnectionWorker().elementStart(parser.getName(), attributes);
                            } catch (NullPointerException e) {
                                Log.e(TAG, "We had a nullpointer here");
                            }
                            break;
                        case XmlPullParser.TEXT:
                            if (parser != null) {
                                String text = parser.getText();
                                Log.d(TAG, "Characters: " + text);
                                getConnectionWorker().elementCharacters(parser.getText());
                            }

                            break;
                        case XmlPullParser.END_TAG:
                            if (parser != null) {
                                String name = parser.getName();
                                Log.d(TAG, "END_TAG: " + name);
                                try {
                                    getConnectionWorker().elementEnd(name);
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "We had a null pointer here");
                                }

                            } else throw new XmlPullParserException("parser is null");
                            break;
                        default:
                            //No default action
                            break;
                    }

                    if (parser != null) {
                        eventType = parser.next();
                        run = eventType != XmlPullParser.END_DOCUMENT;
                    } else run = false;



                    //TODO check has next run =
                }


            } catch (XmlPullParserException e) {
                error = e;
                e.printStackTrace();
                run = false;
            } catch (IOException e) {
                error = e;
                e.printStackTrace();
                run = false;

            } finally {
                if (parser != null) {

                    parser = null;
                }
            }
            parser = null;

            if (error != null && !ignoreErrorOnClose) {
                onError(error);
            }

        }

        @Override
        public void close() {
            run = false;
            ignoreErrorOnClose = true;
            if (parser != null) {

                parser = null;

            }
        }
    }
}
