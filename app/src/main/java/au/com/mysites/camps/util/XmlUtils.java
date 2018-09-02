package au.com.mysites.camps.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.models.User;

/*
 * Various utilities to process xml files
 */

public class XmlUtils {
    private final static String TAG = XmlUtils.class.getSimpleName();

    private StringBuilder mBuffer;
    private String mPath;
    private String mFileName;
    private XmlFormatter mXmlformatter;
    private UtilFile mFop;


    public void initXmlFile(Context context, String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "initXmlFile()");

        mFileName = filename;
        mBuffer = new StringBuilder();
        mXmlformatter = new XmlFormatter(context);
        // format sites into xml
        // add a camps start tag to the beginning of the file
        mXmlformatter.xmlStartTag(mBuffer, context.getString(R.string.xml_camps));
        mBuffer.append('\n');

        mFop = new UtilFile();

        //get the path for the file
        mPath = Environment.getExternalStoragePublicDirectory(context.getString
                (R.string.directory)).getAbsolutePath();
    }


    public void siteSaveToXMLFile(Site s) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "siteSaveToXMLFile()");

        // save site to buffer, format site into xml

        mXmlformatter.formatSite(mBuffer, s);
    }

    public void commentSaveToXMLFile(Comment c) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "commentSaveToXMLFile()");

        // save comment to buffer, format site into xml

        mXmlformatter.formatComment(mBuffer, c);
    }

    public void userSaveToXMLFile(User u) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "userSaveToXMLFile()");

        // save site to buffer, format site into xml

        mXmlformatter.formatUser(mBuffer, u);
    }

   public boolean endXmlFile(Context context) {
       if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "endXmlFile()");

       //add a camps end tag to the end of the file
        mXmlformatter.xmlEndTag(mBuffer, context.getString(R.string.xml_camps));

        UtilFile fop = new UtilFile();
        //write the xml to file
        //firstly, get the path for the file
        //Write the file, and return the outcome
        return fop.write(context, mPath, mFileName, mBuffer.toString(), false);
    }

    /**
     * Read the xml file and populate the list called sites
     *
     * @param filename   file with xml data
     * @param path       path to file
     * @param sites      arraylist where the sites are stored after parsing
     * @return boolean   if all ok return true
     *
     */
    public boolean readXmlfile(Context context, String filename, String path, ArrayList<Site> sites) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readXmlFile()");

        // Read the xml file and populate the list called sites
        //Setup new file operations to call read and write methods
        UtilFile fop = new UtilFile();
        //Read the file
        String text = fop.read(context, filename, path);
        if (text == null)
            return false;
        //create a parser to parse the input for the site and display the results
        XmlParser parser = new XmlParser(context);
        parser.parseSites(text, sites);
        // If parsing errors sites will not be loaded into ArrayList
        return true;
    }
}
