package au.com.mysites.camps.util;

import android.content.Context;
import android.util.Log;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.models.User;

/**
 * XML Formatter
 * Converts data to xml format to save to a xml file
 */

class XmlFormatter {
    private final static String TAG = XmlFormatter.class.getSimpleName();
    private Context context;

    /**
     * Constructor
     *
     * @param callerContext used to access resources from the caller
     */

    XmlFormatter(Context callerContext) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "XmlFormatter()");
        context = callerContext;
    }

    /**
     * Takes a site and formats into XML
     *
     * @param buffer Buffer to add site XML description of the site
     * @param site   Site to be converted to XML format
     */

    void formatSite(StringBuilder buffer, Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "formatSite()");

        // Start with a site start tag and add to the buffer
        xmlStartTag(buffer, context.getString(R.string.xml_site));
        buffer.append('\n');

        // convert site name to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_name), site.getName());
        buffer.append('\n');
        // convert site street to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_street), site.getStreet());
        buffer.append('\n');
        // convert site city to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_city), site.getCity());
        buffer.append('\n');
        // convert site postcode to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_postcode), site.getPostcode());
        buffer.append('\n');
        // convert site state to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_state), site.getState());
        buffer.append('\n');
        // convert site date to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_date_created), site.getDateCreated());
        buffer.append('\n');

        //Format latitude and latitude and store in buffer
        //Convert latitude to xml and store
        xmlElement(buffer, context.getString(R.string.xml_site_latitude), site.getLatitude());
        buffer.append('\n');
        //Convert longitude to xml and store
        xmlElement(buffer, context.getString(R.string.xml_site_longitude), site.getLongitude());
        buffer.append('\n');

        // convert the facilities to xml
        // add true or false if present or not present

        // facility DUMPPOINT
        {
            //get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.dumppoint));
            buffer.append('>');
            // check if present add value true or false to the buffer
            if (site.checkIfFacilityPresent(Site.Facility.DUMPPOINT))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            // add an end tag
            buffer.append('\n');
        }

        // facility FREE
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.free));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.FREE))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        //facility MOBILE
        {
            //yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.mobile));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.MOBILE))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility playequipment
        {
            //get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities),
                    context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.playequipment));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.PLAYEQUIPMENT))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility scenic
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities),
                    context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.scenic));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.SCENIC))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility SHOWER
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.showers));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.SHOWERS))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility SWIMMING
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.swimming));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.SWIMMING))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility toilet
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.toilets));
            buffer.append('>');
            // value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.TOILETS))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility tvreception
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.tvreception));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.TVRECEPTION))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // facility water
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.xml_site_facilities),
                    context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.water));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.WATER))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.xml_site_facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.xml_site_facilities));
            buffer.append('\n');
        }

        // convert site thumbnail to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_thumbnail), site.getThumbnail());
        buffer.append('\n');

        // convert site photo to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_site_photo), site.getSitePhoto());
        buffer.append('\n');

        // add the closing site tag to the buffer
        xmlEndTag(buffer, context.getString(R.string.xml_site));
        // add a return at the end
        buffer.append('\n');
    }

    /**
     * Takes a comment and formats into XML
     *
     * @param buffer    Buffer to add site XML description of the site
     * @param comment   Comment to be converted to XML format
     */

    public void formatComment( StringBuilder buffer, Comment comment) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "formatComment()");

        // Start with a comment start tag and add to the buffer
        xmlStartTag(buffer, context.getString(R.string.xml_comment));
        buffer.append('\n');

        // convert author to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_comment_author), comment.getAuthor());
        buffer.append('\n');

        // convert created date  to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_comment_date_created), comment.getCreatedDate());
        buffer.append('\n');

        // convert siteId to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_comment_siteid), comment.getSiteId());
        buffer.append('\n');

        // convert text to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_comment_text), comment.getText());
        buffer.append('\n');

        // add the closing site tag to the buffer
        xmlEndTag(buffer, context.getString(R.string.xml_comment));
        // add a return at the end
        buffer.append('\n');
    }

    /**
     * Takes a comment and formats into XML
     *
     * @param buffer    Buffer to add site XML description of the site
     * @param user   user to be converted to XML format
     */

    public void formatUser( StringBuilder buffer, User user) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "formatuser()");

        // Start with a user start tag and add to the buffer
        xmlStartTag(buffer, context.getString(R.string.xml_user));
        buffer.append('\n');

        // convert user name to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_user_name), user.getName());
        buffer.append('\n');

        // convert author to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_user_email), user.getEmail());
        buffer.append('\n');

        // convert created date  to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.xml_user_lastused), user.getLastUsed());
        buffer.append('\n');

        // add the closing site tag to the buffer
        xmlEndTag(buffer, context.getString(R.string.xml_user));
        // add a return at the end
        buffer.append('\n');
    }

    /**
     * Append a start tag with an element name of name,
     * then with the content of text
     * and end tag with an element name of name
     * to the stringBuilder buffer
     * result: <name>content</name>
     *
     * @param name   name of tag
     * @param text   text for tag
     * @param buffer buffer to add new start tag
     */

    private void xmlElement(StringBuilder buffer, String name, String text) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlElement()");

        xmlStartTag(buffer, name);
        buffer.append(text);
        xmlEndTag(buffer, name);
    }

    /**
     * Append a start tag with an element name of name to the stringBuilder buffer
     * result: <name>
     *
     * @param name   name of start tag
     * @param buffer buffer to add new start tag
     */

    void xmlStartTag(StringBuilder buffer, String name) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlStartTag()");

        buffer.append('<').append(name).append(">");
    }

    /**
     * Append an end tag with an element name of name to the stringBuilder buffer
     * result: </name>
     *
     * @param name   name of start tag
     * @param buffer buffer to add new start tag
     */

    void xmlEndTag(StringBuilder buffer, String name) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlEndTag()");

        buffer.append("</").append(name).append(">");
    }

    /**
     * Append a start tag with an element name of name and attribute name to the stringBuilder buffer
     * result: <name attributename
     *
     * @param elementName   name of start tag
     * @param attributeName name of attribute tag
     * @param buffer        buffer to add new start tag
     */

    private void xmlStartTagPlusAttributeName(StringBuilder buffer,
                                              String elementName, String attributeName) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlStartTag()");

        buffer.append("<").append(elementName).append(' ').append(attributeName);
    }


    /**
     * Append element content, and then end tag to the buffer
     * result:  content</name>
     *
     * @param buffer         buffer to add new start tag
     * @param elementContent name of start tag
     * @param elementName    name of attribute tag
     */

    private void xmlContentPlusEndTag(StringBuilder buffer,
                                      String elementContent, String elementName) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlContentPlusEndTag()");

        buffer.append(elementContent);
        xmlEndTag(buffer, elementName);
    }

    /**
     * Append attribute type,to the stringBuilder buffer
     * result: ="attributetype"
     *
     * @param attributeType value to be added
     * @param buffer        buffer to add new start tag
     */

    private void xmlAttributeType(StringBuilder buffer, String attributeType) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlAttributeType()");

        buffer.append("=\"").append(attributeType).append('"');
    }

    /**
     * Append an attribute name to the stringBuilder buffer
     * result: attributename
     *
     * @param buffer        buffer to add new start tag
     * @param attributeName name of attribute tag
     */

    private void xmlAttributeName(StringBuilder buffer, String attributeName) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlAttributeName()");

        buffer.append(' ').append(attributeName);
    }

    /**
     * Append a attribute value to the stringBuilder buffer
     * result: = attributevalue
     *
     * @param attributeValue value to be added
     * @param buffer         buffer to add new start tag
     */

    private void xmlAttributeValue(StringBuilder buffer, String attributeValue) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_XMLFORMATTER) Log.d(TAG, "xmlAttributeValue()");

        buffer.append("=\"").append(attributeValue).append('"');
    }
}

