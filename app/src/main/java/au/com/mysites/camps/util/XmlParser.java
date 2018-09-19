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
import au.com.mysites.camps.models.User;

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
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, "parseSites " + context.getString(R.string.ERROR_File_format));
        }
    }

    /**
     * Parse input string and for each comment store the comment in the Array list of myComments
     *
     * @param text       input text to be parsed, containing data from read of xml file
     * @param myComments stores results of parsed comments
     */
    void parseComments(String text, ArrayList<Comment> myComments) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "parseComments()");

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
                } else if (xpp.getName().equals(context.getString(R.string.xml_comment))) {
                    addComment(xpp, myComments);
                } else {
                    skip(xpp);
                }
            }
        } catch (Exception e) {
            // tell user error in file format and exit application
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, " parseComments " + context.getString(R.string.ERROR_File_format));
        }
    }


    /**
     * Parse input string and for each user store the user in the Array list of myUsers
     *
     * @param text    input text to be parsed, containing data from read of xml file
     * @param myUsers stores results of parsed users
     */
    void parseUsers(String text, ArrayList<User> myUsers) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "parseUsers()");

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
                } else if (xpp.getName().equals(context.getString(R.string.xml_user))) {
                    addUser(xpp, myUsers);
                } else {
                    skip(xpp);
                }
            }
        } catch (Exception e) {
            // tell user error in file format and exit application
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, "parseUsers " + context.getString(R.string.ERROR_File_format));
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
                } else if (p.getName().equals(context.getString(R.string.xml_site_facilities))) {
                    getFacility(p, site);
                } else if (p.getName().equals(context.getString(R.string.xml_site_thumbnail))) {
                    site.setThumbnail(readThumbnail(p));
                } else if (p.getName().equals(context.getString(R.string.xml_site_rating))) {
                    site.setRating(Double.valueOf(readRating(p)));
                }
                else if (p.getName().equals(context.getString(R.string.xml_site_photo))) {
                    site.setSitePhoto(readPhoto(p));
                } else {
                    skip(p);
                }
            }
            mySites.add(site);
        } catch (XmlPullParserException | IOException e) {
            // tell user error in file format and exit application
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, "addSite " + context.getString(R.string.ERROR_File_format));
        }
    }

    /**
     * Parse the comment xml and add to the ArrayList of comments
     *
     * @param p          parser
     * @param myComments List to add new comment from parsing input
     */
    private void addComment(XmlPullParser p, ArrayList<Comment> myComments) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addComment()");

        try { // Test if the current event is the type START_TAG and the string is "comment",
            // otherwise an exception is thrown
            p.require(XmlPullParser.START_TAG, null, context.getString(R.string.xml_comment));
            // create empty comment
            Comment comment = new Comment();
            while (p.next() != XmlPullParser.END_TAG) {
                //ensure we have a start tag
                if (p.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (p.getName().equals(context.getString(R.string.xml_comment_author))) {
                    comment.setAuthor(readAuthor(p));
                } else if (p.getName().equals(context.getString(R.string.xml_comment_date_created))) {
                    comment.setCreatedDate(readDateCreated(p));
                } else if (p.getName().equals(context.getString(R.string.xml_comment_siteid))) {
                    comment.setSiteId(readSiteId(p));
                } else if (p.getName().equals(context.getString(R.string.xml_comment_text))) {
                    comment.setText(readCommentText(p));
                } else {
                    skip(p);
                }
            }
            myComments.add(comment);
        } catch (XmlPullParserException | IOException e) {
            // tell user error in file format and exit application
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, "addComment " + context.getString(R.string.ERROR_File_format));
        }
    }

    /**
     * Parse the user xml and add to the ArrayList of users
     *
     * @param p       parser
     * @param myUsers List to add new user from parsing input
     */
    private void addUser(XmlPullParser p, ArrayList<User> myUsers) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addUser()");

        try { // Test if the current event is the type START_TAG and the string is "user",
            // otherwise an exception is thrown
            p.require(XmlPullParser.START_TAG, null, context.getString(R.string.xml_user));
            // create empty user
            User user = new User();
            while (p.next() != XmlPullParser.END_TAG) {
                //ensure we have a start tag
                if (p.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (p.getName().equals(context.getString(R.string.xml_user_email))) {
                    user.setEmail(readEmail(p));
                } else if (p.getName().equals(context.getString(R.string.xml_user_lastused))) {
                    user.setLastUsed(readLastUsed(p));
                } else if (p.getName().equals(context.getString(R.string.xml_user_name))) {
                    user.setName(readName(p));
                } else {
                    skip(p);
                }
            }
            myUsers.add(user);
        } catch (XmlPullParserException | IOException e) {
            // tell user error in file format and exit application
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, "addUser " + context.getString(R.string.ERROR_File_format));
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
     * read the siteId text
     *
     * @param XmlPP parser
     * @return siteId text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readSiteId(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readSiteId()");

        String mySiteId;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_comment_siteid)));
        mySiteId = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_comment_siteid)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "SiteId: " + mySiteId);
        return mySiteId;
    }

    /**
     * read the user email text
     *
     * @param XmlPP parser
     * @return email text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readEmail(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readEmail()");

        String myEmail;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_user_email)));
        myEmail = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_user_email)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Email: " + myEmail);
        return myEmail;
    }

    /**
     * read the use last used text
     *
     * @param XmlPP parser
     * @return last used text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readLastUsed(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readLastUsed()");

        String myLastUsed;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_user_lastused)));
        myLastUsed = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_user_lastused)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "LastUsed: " + myLastUsed);
        return myLastUsed;
    }

    /**
     * read the author
     *
     * @param XmlPP parser
     * @return author
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readAuthor(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readAuthor()");

        String myAuthor;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_comment_author)));
        myAuthor = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_comment_author)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "Author: " + myAuthor);
        return myAuthor;
    }

    /**
     * read the comment text
     *
     * @param XmlPP parser
     * @return comment text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */
    private String readCommentText(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readCommentText()");

        String myCommentText;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_comment_text)));
        myCommentText = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_comment_text)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "CommentText: " + myCommentText);
        return myCommentText;
    }

    /**
     * Checks if the facilities are available and updates the site
     *
     * @param XmlPP  parser
     * @param mySite site to store the facility availability
     */
    private void getFacility(XmlPullParser XmlPP, Site mySite) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getFacility()");

        try {
            XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_facilities)));

            String facilityAttribute;
            facilityAttribute = XmlPP.getAttributeValue(null, context.getString(R.string.facilitytype));
            if (facilityAttribute == null) {
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
            exiting(R.string.ERROR_File_format);
            if (Debug.DEBUG_PARSING)
                Log.d(TAG, "getFacility " + context.getString(R.string.ERROR_File_format));
        }
    }

    /**
     * read the thumbnail which is a filename
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
     * read the rating which is a double
     *
     * @param XmlPP parser
     * @return street text
     * @throws IOException            handle any exceptions
     * @throws XmlPullParserException handle any exceptions
     */

    private String readRating(XmlPullParser XmlPP) throws IOException, XmlPullParserException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "readRating()");

        String myRating;
        XmlPP.require(XmlPullParser.START_TAG, null, (context.getString(R.string.xml_site_rating)));
        myRating = readText(XmlPP);
        XmlPP.require(XmlPullParser.END_TAG, null, (context.getString(R.string.xml_site_rating)));

        if (Debug.DEBUG_PARSING) Log.d(TAG, "rating: " + myRating);
        return myRating;
    }
    /**
     * read the photo which is a filename
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
    private void exiting(final int id) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "exiting()");

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

