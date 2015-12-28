package org.xbmc.kore.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


/**
 * Created by dql on 3/24/2014.
 */
public class PopUpListAdapter extends ArrayAdapter {

    private static final String TAG = "PopUpListAdapter";
    private Context mContext;
    private int id;
    private String[] items;

    public PopUpListAdapter(Context context, int textViewResourceId, String[] listArry) {
        super(context, textViewResourceId, listArry);
        mContext = context;
        id = textViewResourceId;
        items = listArry;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // setting the ID and text for every items in the list
        String item = (String) getItem(position);
        // visual settings for the list item
        TextView listItem = new TextView(mContext);

        listItem.setText(item);
        //listItem.setTextSize(22);
        listItem.setPadding(10, 10, 10, 10);
        listItem.setTextColor(Color.WHITE);
        listItem.setBackgroundColor(Color.BLACK);
        return listItem;
    }
}
