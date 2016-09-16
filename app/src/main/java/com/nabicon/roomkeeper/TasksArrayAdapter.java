package com.nabicon.roomkeeper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.nabicon.R;

import java.util.ArrayList;

/**
 * Created by mariloo on 3/9/2016.
 */
public class TasksArrayAdapter extends ArrayAdapter<String> {

    private static final String TAG = TasksArrayAdapter.class.getSimpleName();
    private Context activity;


    public TasksArrayAdapter(Context context, int textViewResource, ArrayList<String> objects) {
        super(context, textViewResource, objects);
        this.activity = context;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        String[] tasks = getItem(position).split("\\$");
        final String attachmentName = tasks[0];
        final String task = tasks[1];
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_list_item, parent, false);
        }
        final CheckedTextView textView = (CheckedTextView) convertView.findViewById(R.id.checked_task_item);
        textView.setText(task);
        textView.setEnabled(true);
        textView.setChecked(false);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CheckedTextView) v).toggle();
                if (((CheckedTextView) v).isChecked()) {
                    Log.i(TAG, "checkbox changed");
                    v.setEnabled(false);
                    Intent data = new Intent("deleteTask");
                    data.putExtra("deletedTask", attachmentName);
                    data.putExtra("taskPosition", position);
                    activity.sendBroadcast(data);
                }
            }
        });
        return  convertView;
    }
}
