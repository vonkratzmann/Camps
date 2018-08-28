package au.com.mysites.camps.util;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "XmlFormatter()");
        context = callerContext;
    }

    /**
     * Takes a activities and formats into XML
     *
     * @param buffer Buffer to add activities XML description of the activities
     * @param site   Site to be converted to XML format
     */

    void formatSite(StringBuilder buffer, Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "formatSite()");

        // Start with a activities start tag and add to the buffer
        xmlStartTag(buffer, context.getString(R.string.site));
        buffer.append('\n');

        // convert activities name to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.name), site.getName());
        buffer.append('\n');
        // convert activities street to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.street), site.getStreet());
        buffer.append('\n');
        // convert activities city to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.city), site.getCity());
        buffer.append('\n');
        // convert activities state to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.state), site.getState());
        buffer.append('\n');
        // convert activities date to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.date_created), site.getDateCreated());
        buffer.append('\n');

        //Format latitude and latitude and store in buffer
        //Convert latitude to xml and store
        xmlElement(buffer, context.getString(R.string.latitude), site.getLatitude());
        buffer.append('\n');
        //Convert longitude to xml and store
        xmlElement(buffer, context.getString(R.string.longitude), site.getLongitude());
        buffer.append('\n');

        // convert the facilities to xml
        // add true or false if present or not present

        // facility DUMPPOINT
        {
            //get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.dumppoint));
            buffer.append('>');
            // check if present add value true or false to the buffer
            if (site.checkIfFacilityPresent(Site.Facility.DUMPPOINT))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            // add an end tag
            buffer.append('\n');
        }

        // facility FREE
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.free));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.FREE))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        //facility MOBILE
        {
            //yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.mobile));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.MOBILE))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility playequipment
        {
            //get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities),
                    context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.playequipment));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.PLAYEQUIPMENT))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility scenic
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities),
                    context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.scenic));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.SCENIC))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility SHOWER
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.showers));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.SHOWERS))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility SWIMMING
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.swimming));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.SWIMMING))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility toilet
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.toilets));
            buffer.append('>');
            // value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.TOILETS))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility tvreception
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities), context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.tvreception));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.TVRECEPTION))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // facility water
        {
            // yes so, get start tag and attribute name of facility and add to the buffer
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.facilities),
                    context.getString(R.string.facilitytype));
            // get the attribute type and add to the buffer
            xmlAttributeType(buffer, context.getString(R.string.water));
            buffer.append('>');
            // add value to the buffer with an end tag
            if (site.checkIfFacilityPresent(Site.Facility.WATER))
                xmlContentPlusEndTag(buffer, "true", context.getString(R.string.facilities));
            else
                xmlContentPlusEndTag(buffer, "false", context.getString(R.string.facilities));
            buffer.append('\n');
        }

        // convert activities thumbnail to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.thumbnail), site.getThumbnail());
        buffer.append('\n');

        // convert activities photo to xml and add to the buffer
        xmlElement(buffer, context.getString(R.string.photo), site.getSitePhoto());
        buffer.append('\n');

        // convert the activities comments to xml and add
        buffer.append(getCommentsInXml(site));

        // add the closing activities tag to the buffer
        xmlEndTag(buffer, context.getString(R.string.site));
        // add a return at the end
        buffer.append('\n');
    }

    /**
     * Get comments from activities and convert to xml and store in a StringBuilder
     *
     * @param site      activities to process
     * @return          StringBuilder with comments converted to xml
     */

    private StringBuilder getCommentsInXml(Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getCommentsInXml()");

        ArrayList<Comment> myArray = site.getComments();
        StringBuilder buffer = new StringBuilder();

        for (Comment c : myArray) {
            xmlStartTagPlusAttributeName(buffer, context.getString(R.string.comment), context.getString(R.string.commentdate));
            xmlAttributeValue(buffer, c.getCreatedDate());
            xmlAttributeName(buffer, context.getString(R.string.commentauthor));
            xmlAttributeValue(buffer, c.getAuthor());
            buffer.append('>');
            buffer.append('\n');
            xmlContentPlusEndTag(buffer, c.getText(), context.getString(R.string.comment));
            buffer.append('\n');
        }
        return buffer;
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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlElement()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlStartTag()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlEndTag()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlStartTag()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlContentPlusEndTag()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlAttributeType()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlAttributeName()");

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
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "xmlAttributeValue()");

        buffer.append("=\"").append(attributeValue).append('"');
    }
}

