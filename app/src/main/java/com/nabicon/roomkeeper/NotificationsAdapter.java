package com.nabicon.roomkeeper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nabicon.R;

import java.util.List;

/**
 * Created by mariloo on 24/9/2016.
 */
public class NotificationsAdapter extends ArrayAdapter<String>{

    public NotificationsAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String task = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.notification_list_item, parent, false);
        }
        TextView taskTextView = (TextView) convertView.findViewById(R.id.notification);
        taskTextView.setText(task);
        return convertView;
    }
}
