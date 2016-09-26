package com.nabicon.roomkeeper;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.nabicon.R;

import java.util.ArrayList;

/**
 * Created by mariloo on 3/9/2016.
 */
public class TasksArrayAdapter extends RecyclerView.Adapter<TasksArrayAdapter.ViewHolder> {

    private static final String TAG = TasksArrayAdapter.class.getSimpleName();
    private Context activity;
    private ArrayList<Task> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public CheckedTextView checkedTextView;
        public ViewHolder(CheckedTextView v) {
            super(v);
            checkedTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TasksArrayAdapter(ArrayList<Task> myDataset, Context activity) {
        this.activity = activity;
        mDataset = myDataset;
    }

    @Override
    public TasksArrayAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        CheckedTextView v = (CheckedTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(TasksArrayAdapter.ViewHolder holder, int position) {
        final int pos = position;
        Task task = mDataset.get(pos);
        final String title = task.title;
        final String attachmentName = task.attachmentName;
        holder.checkedTextView.setText(title);
        holder.checkedTextView.setEnabled(true);
        holder.checkedTextView.setChecked(false);
        holder.checkedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CheckedTextView) v).toggle();
                if (((CheckedTextView) v).isChecked()) {
                    Log.i(TAG, "checkbox changed");
                    v.setEnabled(false);
                    Intent data = new Intent("deleteTask");
                    data.putExtra("deletedTask", attachmentName);
                    data.putExtra("taskPosition", pos);
                    activity.sendBroadcast(data);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
