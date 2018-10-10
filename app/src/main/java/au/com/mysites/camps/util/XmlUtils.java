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


    public void initXmlFile(Context context, String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "initXmlFile()");

        mFileName = filename;
        mBuffer = new StringBuilder();
        mXmlformatter = new XmlFormatter(context);
        // format sites into xml
        // add a camps start tag to the beginning of the file
        mXmlformatter.xmlStartTag(mBuffer, context.getString(R.string.xml_camps));
        mBuffer.append('\n');

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
     * Read the site xml file and populate the list called sites
     * If parsing errors sites will not be loaded into ArrayList
     *
     * @param filename   file with xml data
     * @param path       path to file
     * @param sites      arraylist where the sites are stored after parsing
     * @return boolean   if all ok return true
     *
     */
    public boolean readSiteXmlfile(Context context, String filename, String path, ArrayList<Site> sites) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readXmlFile()");

        //Setup new file operations to call read and write methods
        UtilFile fop = new UtilFile();
        
        //Read the file
        String siteText = fop.read(context, filename, path);
        
        if (siteText == null)
            return false;
        //create a parser to parse the input for the site and display the results
        XmlParser parser = new XmlParser(context);
        parser.parseSites(siteText, sites);
        
        return true;
    }

    /**
     * Read the comment xml file and populate the list called comments
     * If parsing errors comments will not be loaded into ArrayList
     *
     * @param filename   file with xml data
     * @param path       path to file
     * @param comments   arraylist where the comments are stored after parsing
     * @return boolean   if all ok return true
     *
     */
    public boolean readCommentXmlfile(Context context, String filename, String path, ArrayList<Comment> comments) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readXmlFile()");

        //Setup new file operations to call read and write methods
        UtilFile fop = new UtilFile();

        //Read the file
        String commentText = fop.read(context, filename, path);

        if (commentText == null)
            return false;
        //create a parser to parse the input for the comment and display the results
        XmlParser parser = new XmlParser(context);
        parser.parseComments(commentText, comments);

        return true;
    }

    /**
     * Read the user xml file and populate the list called users
     * If parsing errors users will not be loaded into ArrayList
     *
     * @param filename   file with xml data
     * @param path       path to file
     * @param users   arraylist where the users are stored after parsing
     * @return boolean   if all ok return true
     *
     */
    public boolean readUserXmlfile(Context context, String filename, String path, ArrayList<User> users) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readXmlFile()");

        //Setup new file operations to call read and write methods
        UtilFile fop = new UtilFile();

        //Read the file
        String userText = fop.read(context, filename, path);

        if (userText == null)
            return false;
        //create a parser to parse the input for the user and display the results
        XmlParser parser = new XmlParser(context);
        parser.parseUsers(userText, users);

        return true;
    }
}
