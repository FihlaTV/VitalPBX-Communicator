package org.vpbxcommunicator.contacts;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.vpbxcommunicator.LinphoneActivity;
import org.vpbxcommunicator.R;
import org.vpbxcommunicator.settings.RemotePhonebookSettingsFragment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * Written by Farhat Samara */

public class RemotePhonebookParser extends AsyncTask<Void, Void, Boolean> {

    private String url;
    private ProgressDialog waitDialog = new ProgressDialog(LinphoneActivity.instance());

    // keep a list of contacts added through remote phonebook
    public List<LinphoneContact> contactList = new ArrayList<>();

    @Override
    protected void onPreExecute() {
        this.waitDialog.setMessage(
                RemotePhonebookSettingsFragment.mContext
                        .getResources()
                        .getString(R.string.sync_in_progress));
        this.waitDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        String name;
        String sipNumber;

        LinphoneActivity.instance().clearRemotePhonebookContacts();

        try {
            URL enteredURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) enteredURL.openConnection();

            // set up header for authentication
            connection.setDoOutput(true);
            connection.setRequestProperty("User-Agent", "vpbxCommunicator");
            connection.getRequestMethod();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(connection.getInputStream()));

            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Contact");

            for (int i = 0; i < nodeList.getLength(); i++) {

                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    name = getValue("Name", element);
                    sipNumber = getValue("Telephone", element);

                    LinphoneActivity.instance().asyncAddContact(name, sipNumber);
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("XML parser exception: " + e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (waitDialog.isShowing()) {
            waitDialog.dismiss();
        }

        if (success) {
            // launch contacts list and refresh
            Toast.makeText(
                            RemotePhonebookSettingsFragment.mContext,
                            RemotePhonebookSettingsFragment.mContext
                                    .getResources()
                                    .getString(R.string.successful_sync),
                            Toast.LENGTH_LONG)
                    .show();

            // TODO: there might be a more efficient way to approach this
            // the following operations are used for ContactsManager to delete every contact
            // cached. this avoids duplicates
            ContactsManager.getInstance().destroy();
            ContactsManager.getInstance().initializeContactManager(LinphoneActivity.instance());

            LinphoneActivity.instance().displayContacts(false);

        } else if (url.matches("")) { // url field remained empty
            Toast.makeText(
                            RemotePhonebookSettingsFragment.mContext,
                            RemotePhonebookSettingsFragment.mContext
                                    .getResources()
                                    .getString(R.string.empty_url),
                            Toast.LENGTH_LONG)
                    .show();
        } else { // success is false
            Toast.makeText(
                            RemotePhonebookSettingsFragment.mContext,
                            RemotePhonebookSettingsFragment.mContext
                                    .getResources()
                                    .getString(R.string.sync_error),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    // used to get specific attributes for each XML tag
    // in this case: pass the attribute name, and the populated element
    private static String getValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);

        return node.getNodeValue();
    }

    // retrieve url from edittext in RemotePhonebookSettingsFragment
    public void setURL(String enteredURL) {
        url = enteredURL;
    }

    public String getURL() {
        return url;
    }
}
