package com.nabicon.roomkeeper;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.nabicon.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariloo on 3/9/2016.
 */
public class TasksArrayAdapter extends ArrayAdapter<String> {

    private static final int GREY = Color.rgb(150, 150, 150);
    private static final int BLACK = Color.rgb(0, 0, 0);


    public TasksArrayAdapter(Context context, int resource, int textViewResource, ArrayList<String> objects) {
        super(context, resource, textViewResource, objects);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final String task = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_list_item, parent, false);
        }
        final TextView textView = (TextView) convertView.findViewById(R.id.task_id);
        textView.setText(task);
        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.task_checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    remove(task);
                    notifyDataSetChanged();
                }
            }
        });
        return  convertView;
    }
}
