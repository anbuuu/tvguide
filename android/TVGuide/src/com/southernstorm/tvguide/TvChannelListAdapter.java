package com.southernstorm.tvguide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.DataSetObserver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class TvChannelListAdapter implements ListAdapter {

    private Context context;
    private List<TvChannel> channels;
    private List<DataSetObserver> observers;
    private LayoutInflater inflater;
    private String region;
    private Map< String, List<String> > regionTree = new TreeMap< String, List<String> >();
    
    public TvChannelListAdapter(Context context) {
        this.context = context;
        this.channels = new ArrayList<TvChannel>();
        this.observers = new ArrayList<DataSetObserver>();
        this.inflater = LayoutInflater.from(context);
        this.region = "brisbane";    // TODO
        loadChannels();
    }

    public int getCount() {
        return channels.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public int getItemViewType(int position) {
        return 0;
    }

    public TvChannel getChannel(int position) {
        return channels.get(position);
    }
    
    private class ViewDetails {
        public ImageView icon;
        public TextView name;
        public TextView numbers;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewDetails view = null;
        if (convertView != null) {
            view = (ViewDetails)convertView.getTag();
        } else {
            convertView = inflater.inflate(R.layout.channel, null);
            view = new ViewDetails();
            view.icon = (ImageView)convertView.findViewById(R.id.channel_icon);
            view.name = (TextView)convertView.findViewById(R.id.channel_name);
            view.numbers = (TextView)convertView.findViewById(R.id.channel_numbers);
            convertView.setTag(view);
        }
        TvChannel channel = channels.get(position);
        view.icon.setImageResource(channel.getIconResource());
        view.name.setText(channel.getName());
        String numbers = channel.getNumbers();
        if (numbers == null)
        	numbers = "";
        view.numbers.setText(numbers);
        return convertView;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isEmpty() {
        return channels.isEmpty();
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public boolean isEnabled(int position) {
        return true;
    }

    /**
     * Loads channel information from the channels.xml file embedded in the resources.
     */
    private void loadChannels() {
        channels.clear();
        XmlResourceParser parser = context.getResources().getXml(R.xml.channels);
        regionTree = new TreeMap< String, List<String> >();
        String id = null;
        String parent;
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("region")) {
                        // Parse the contents of a <region> element.
                        id = parser.getAttributeValue(null, "id");
                        parent = parser.getAttributeValue(null, "parent");
                        if (id != null && parent != null) {
                            if (!regionTree.containsKey(id))
                                regionTree.put(id, new ArrayList<String>());
                            regionTree.get(id).add(parent);
                        }
                    } else if (name.equals("other-parent")) {
                        // Secondary parent for the current region.
                        parent = Utils.getContents(parser, name);
                        if (!regionTree.containsKey(id))
                            regionTree.put(id, new ArrayList<String>());
                        regionTree.get(id).add(parent);
                    } else if (name.equals("channel")) {
                        // Parse the contents of a <channel> element.
                        TvChannel channel = loadChannel(parser);
                        if (channel != null)
                            channels.add(channel);
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            // Ignore - just stop parsing at the first error.
        } catch (IOException e) {
        }
        parser.close();
        Collections.sort(channels);
        regionTree = null;
    }
    
    private TvChannel loadChannel(XmlPullParser parser) throws XmlPullParserException, IOException {
        TvChannel channel = new TvChannel();
        channel.setId(parser.getAttributeValue(null, "id"));
        String region = parser.getAttributeValue(null, "region");
        if (region == null || !regionMatch(region))
            return null;
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.equals("display-name")) {
                    channel.setName(Utils.getContents(parser, name));
                } else if (name.equals("icon")) {
                    String src = parser.getAttributeValue(null, "src");
                    if (src != null) {
                        int index = src.lastIndexOf('/');
                        if (index >= 0)
                            channel.setIconResource(IconFactory.getInstance().getChannelIconResource(src.substring(index + 1)));
                    }
                } else if (name.equals("number")) {
                    String system = parser.getAttributeValue(null, "system");
                    String currentNumbers = channel.getNumbers();
                    if (!system.equals("digital")) {
                        if (currentNumbers == null)
                            return null;    // Ignore Pay TV only channels for now.
                    } else {
                        String number = Utils.getContents(parser, name);
                        if (currentNumbers == null) {
                            channel.setNumbers(number);
                            channel.setPrimaryChannelNumber(Integer.valueOf(number));
                        } else {
                            channel.setNumbers(currentNumbers + ", " + number);
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("channel")) {
                break;
            }
            eventType = parser.next();
        }
        List<String> baseUrls = new ArrayList<String>();
        baseUrls.add("http://www.oztivo.net/xmltv/");
        baseUrls.add("http://xml.oztivo.net/xmltv/");
        channel.setBaseUrls(baseUrls);
        return channel;
    }
    
    private boolean regionMatch(String r) {
        if (r.equals(region))
            return true;
        List<String> testRegions = new ArrayList<String>();
        testRegions.add(region);
        return regionMatch(r, testRegions);
    }
    
    private boolean regionMatch(String r, List<String> regions) {
        for (String region: regions) {
            if (r.equals(region))
                return true;
            List<String> testRegions = regionTree.get(region);
            if (testRegions != null && regionMatch(r, testRegions))
                return true;
        }
        return false;
    }
}
