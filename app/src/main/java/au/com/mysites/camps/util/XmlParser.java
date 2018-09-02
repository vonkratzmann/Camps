package au.com.mysites.camps.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;

/**
 * Methods to parse the data from site XML file and store the result in the list of sites
 */

public class XmlParser {

    private final static String TAG = XmlParser.class.getSimpleName();
    private Context context;

    /**
     * Constructor
     *
     * @param callerContext used to access resources from the caller
     */
    XmlParser(Context callerContext) {
        context = callerContext;
    }

    /**
     * Parse input string and for each site store the site in the Array list of mySites
     *
     * @param text    input text to be parsed, containing data from read of xml file
     * @param mySites stores results of parsed sites
     */
    void parseSites(String text, ArrayList<Site> mySites) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "parseSites()");

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(text));

            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                //skip over the initial <camps> tag
                if (xpp.getName().equals(context.getString(R.string.xml_camps))) {
                    continue;
                } else if (xpp.getName().equals(context.getString(R.string.xml_site))) {
                    addSite(xpp, mySites);
                } else {
                    skip(xpp);
                }
            }
        } catch (Exception e) {
            // tell user error in file format and exit application
            exitApplication(R.string.ERROR_File_format);
        }
    }

    /**
     * Parse the site xml and add to the ArrayList of sites
     *
     * @param p       parser
     * @param mySites List to add new site from parsing input
     */
    private void addSite(XmlPullParser p, ArrayList<Site> mySites) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addSite()");

        // ArrayList<Comment> comments = new ArrayList<>();

        try { // Test if the current event is the type START_TAG and the string is "site",
            // otherwise an exception is thrown
            p.require(XmlPullParser.START_TAG, null, context.getString(R.string.xml_site));
            // create empty site
            Site site = new Site();
            while (p.next() != XmlPullParser.END_TAG) {
                //ensure we have a start tag
                if (p.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (p.getName().equals(context.getString(R.string.xml_site_name))) {
                    site.setName(readName(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_street))) {
                    site.setStreet(readStreet(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_city))) {
                    site.setCity(readCity(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_postcode))) {
                    site.setPostcode(readPostcode(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_state))) {
                    site.setState(readState(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_date_created))) {
                    site.setDateCreated(readDateCreated(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_latitude))) {
                    site.setLatitude(readLatitude(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_longitude))) {
                    site.setLongitude(readLongitude(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_comment))) {
                    site.addComment(readComment(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_facilities))) {
                    getFacility(p, site);
                } else if (p.getName().equals(context.getString(R.string.xml_site_thumbnail))) {
                    site.setThumbnail(readThumbnail(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_photo))) {
                    site.setSitePhoto(readPhoto(p));
                } else {
                    skip(p);
                }
            }
            mySites.add(site);
        } catch (XmlPullParserException | IOException e) {
            // tell user error in file format and exit application
            exitApplication(R.string.ERROR_File_format);
        }
    }

    /**
     * Read the name text
     *
     * @param XmlPP parser
     * @return the text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readName(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readName()");

        String myName;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_name)));
        myName = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_name)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Name: " + myName);
        return myName;
    }

    /**
     * read the city text
     *
     * @param XmlPP parser
     * @return city text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readCity(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readCity()");

        String myCity;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_city)));
        myCity = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_city)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "City: " + myCity);
        return myCity;
    }

    /**
     * read the postcode text
     *
     * @param XmlPP parser
     * @return postcode text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readPostcode(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readPostcode()");

        String myPostcode;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_postcode)));
        myPostcode = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_postcode)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Postcode: " + myPostcode);
        return myPostcode;
    }

    /**
     * read the street text
     *
     * @param XmlPP parser
     * @return street text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readStreet(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readStreet()");

        String myStreet;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_street)));
        myStreet = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_street)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Street: " + myStreet);
        return myStreet;
    }

    /**
     * read the state text
     *
     * @param XmlPP parser
     * @return state text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readState(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readState()");

        String myState;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_state)));
        myState = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_state)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "State: " + myState);
        return myState;
    }

    /**
     * read the date the site information was entered
     *
     * @param XmlPP parser
     * @return date site data was entered
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private String readDateCreated(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readDateCreated()");

        String myDateCreated;
        // Check if we have the correct start tag
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_date_created)));
        myDateCreated = readText(XmlPP);
        // Check if we have the correct end tag
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_date_created)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Date Created: " + myDateCreated);
        return myDateCreated;
    }


    /**
     * read the latitude text
     *
     * @param XmlPP parser
     * @return street text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private String readLatitude(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readLatitude()");

        String myLatitude;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_latitude)));
        myLatitude = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_latitude)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Latitude: " + myLatitude);
        return myLatitude;
    }

    /**
     * read the longitude text
     *
     * @param XmlPP parser
     * @return street text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private String readLongitude(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readLongitude()");

        String myLongitude;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_longitude)));
        myLongitude = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_longitude)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Longitude: " + myLongitude);
        return myLongitude;
    }

    /**
     * Reads a comment, extracting date, text and author
     *
     * @param XmlPP parser
     * @return the comment
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private Comment readComment(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readComment()");

        String commentDate = null;
        String commentText;
        String commentAuthor = null;
        String commentAuthorId = null;

        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_comment)));
        String commentAttribute;
        commentAttribute = XmlPP.getAttributeValue(null, context.getString(R.string.xml_comment_date));
        if (commentAttribute != null) {
            commentDate = commentAttribute;
            if (Debug.DEBUG_PARSING_COMMENTS) Log.d(TAG, "commentDate: " + commentDate);
        }
        commentAttribute = XmlPP.getAttributeValue(null, context.getString(R.string.xml_comment_author));
        if (commentAttribute != null) {
            commentAuthor = commentAttribute;
            if (Debug.DEBUG_PARSING_COMMENTS) Log.d(TAG, "commentAuthor: " + commentAuthor);
        }
        commentAttribute = XmlPP.getAttributeValue(null, context.getString(R.string.xml_comment_photo));
        if (commentAttribute != null) {
            commentAuthorId = commentAttribute;
            if (Debug.DEBUG_PARSING_COMMENTS) Log.d(TAG, "commentAuthorId: " + commentAuthorId);
        }
        commentText = readText(XmlPP).trim();
        if (Debug.DEBUG_PARSING_COMMENTS) Log.d(TAG, "commentText: " + commentText);

        Comment c = new Comment(commentText, commentDate,
                context.getString(R.string.dateformat), commentAuthor, commentAuthorId);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_comment)));
        return c;
    }

    /**
     * Checks if the facilities are available and updates the site
     *
     * @param XmlPP  parser
     * @param mySite site to store th efacility availability
     */
    private void getFacility(XmlPullParser XmlPP, Site mySite) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getFacility()");

        try {
            XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_facilities)));

            String facilityAttribute;
            facilityAttribute = XmlPP.getAttributeValue(null, context.getString(R.string.facilitytype));
            if (facilityAttribute != null) {
                return;
            }
            // Check which facility and store if available
            if (facilityAttribute.equals(context.getString(R.string.dumppoint))) {
                mySite.setDumpPoint(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.free))) {
                mySite.setFree(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.mobile))) {
                mySite.setMobile(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.playequipment))) {
                mySite.setPlayEquipment(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.scenic))) {
                mySite.setScenic(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.showers))) {
                mySite.setShowers(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.swimming))) {
                mySite.setSwimming(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.toilets))) {
                mySite.setToilets(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.tvreception))) {
                mySite.setTvReception(Boolean.valueOf(readText(XmlPP)));
            } else if (facilityAttribute.equals(context.getString(R.string.water))) {
                mySite.setWaterDrinking(Boolean.valueOf(readText(XmlPP)));
            }
            XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_facilities)));

        } catch (XmlPullParserException | IOException e) {
            // tell user error in file format and exit application
            exitApplication(R.string.ERROR_File_format);
        }
    }

    /**
     * read the thumbnail which is in ASCII text
     *
     * @param XmlPP parser
     * @return street text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private String readThumbnail(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readThumbnail()");

        String myThumbnail;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_thumbnail)));
        myThumbnail = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_thumbnail)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "thumbnail: " + myThumbnail);
        return myThumbnail;
    }

    /**
     * read the photo which is in ASCII text
     *
     * @param XmlPP parser
     * @return street text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private String readPhoto(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readPhoto()");

        String myPhoto;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_photo)));
        myPhoto = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_photo)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "photo: " + myPhoto);
        return myPhoto;
    }


    /**
     * @param parser parser
     * @throws XmlPullParserException parser exception
     * @throws IOException            parser exception
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "skip()");

        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            parser.getName();
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    // For the name, city and street, etc extracts their text content from the xml element.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readText()");

        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * Show a message passed to the method,
     * then show an exiting application message,
     * pause so the messages are shown in the main screen application,
     * then terminate the application
     *
     * @param id string reference id of message to be displayed
     */
    private void exitApplication(final int id) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "exit_Application()");

        Toast.makeText(context, context.getString(id), Toast.LENGTH_LONG).show();
        Toast.makeText(context, context.getString(R.string.Exiting_application),
                Toast.LENGTH_SHORT).show();

        // leave main screen of the application up for a while so the messages are shown in the app
        new CountDownTimer(4000, 1000) {
            public void onTick(long l) {
            }

            //exit application as no use proceeding
            public void onFinish() {
                System.exit(0);
            }
        }.start();
    }
}

