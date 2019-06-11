package org.linphone.contacts;

import android.os.AsyncTask;
import android.widget.Toast;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.linphone.LinphoneActivity;
import org.linphone.settings.RemotePhonebookSettingsFragment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RemotePhonebookParser extends AsyncTask<Void, Void, Boolean> {

    private String url;

    private String enteredURL;
    private boolean successfulParse;

    @Override
    protected Boolean doInBackground(Void... voids) {
        String name;
        String sipNumber;

        try {

            System.out.println("ENTERED URL IS -----------> " + url);

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

            System.out.println("-----------------------------");

            for (int i = 0; i < nodeList.getLength(); i++) {

                Node node = nodeList.item(i);

                System.out.println("Current item: " + node.getNodeName());

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    name = getValue("Name", element);
                    sipNumber = getValue("Telephone", element);

                    LinphoneActivity.instance().asyncAddContact(name, sipNumber);
                }
            }

            // successfulParse = true;
            return true;

        } catch (Exception e) {
            System.out.println("XML parser exception: " + e);

            // successfulParse = false;
            return false;
        }

        // return null;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            // launch contacts list and refresh
            Toast.makeText(
                            RemotePhonebookSettingsFragment.mContext,
                            "Successful Sync!",
                            Toast.LENGTH_LONG)
                    .show();

            LinphoneActivity.instance().displayContacts(true);
            ContactsManager.getInstance().fetchContactsAsync();

        } else if (url.matches("")) { // url field remained empty
            Toast.makeText(
                            RemotePhonebookSettingsFragment.mContext,
                            "URL field is empty",
                            Toast.LENGTH_LONG)
                    .show();
        } else { // success is false
            Toast.makeText(
                            RemotePhonebookSettingsFragment.mContext,
                            "There was an error while syncing contacts",
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

    public boolean successfulParse() {
        return successfulParse;
    }
}
