package com.nabicon.roomkeeper;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.nabicon.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link DialogFragment} subclass.
 */
public class NewTaskDialogFragment extends DialogFragment {

    private static final String TAG = NewTaskDialogFragment.class.getSimpleName();

    private static Date deadlineDate;

    private OnNewTaskButtonListener onNewTaskButtonListener;

    public NewTaskDialogFragment() {
        // Required empty public constructor
    }

    public interface OnNewTaskButtonListener {
        void onNewTaskButtonClicked(String task);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle("New Task");
        View view = inflater.inflate(R.layout.fragment_new_task_dialog, container, false);
        deadlineDate = null;
        final EditText newTask = (EditText) view.findViewById(R.id.task_title);
        Button deadlineButton = (Button) view.findViewById(R.id.task_deadline_button);
        deadlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeadlinePicker deadlinePicker = new DeadlinePicker();
                deadlinePicker.show(getActivity().getSupportFragmentManager(), "Set task deadline");
            }
        });
        Button newTaskButton = (Button) view.findViewById(R.id.new_task_button);
        newTaskButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String taskName = newTask.getText().toString();
                if (taskName.equals("")) {
                    Toast.makeText(getContext(), "Please fill the task title", Toast.LENGTH_SHORT).show();
                }
                else {
                    if(onNewTaskButtonListener != null) {
                        JSONObject taskDataJson = new JSONObject();
                        try {
                            taskDataJson.put("taskName", taskName);
                            if (deadlineDate == null) {
                                taskDataJson.put("deadlineDate", "");
                            }
                            else {
                                DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
                                String deadlineString = df.format(deadlineDate);
                                Log.i(TAG, deadlineString);
                                taskDataJson.put("deadlineDate", deadlineString);
                            }
                        }
                        catch (JSONException e) {
                            Log.e(TAG, "New task creation to JSON Object went wrong.");
                        }
                        String taskData = taskDataJson.toString();
                        onNewTaskButtonListener.onNewTaskButtonClicked(taskData);
                        dismiss();
                    }
                }
            }
        });
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNewTaskButtonListener) {
            onNewTaskButtonListener = (OnNewTaskButtonListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNewTaskButtonListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onNewTaskButtonListener = null;
    }

    public static class DeadlinePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            deadlineDate = c.getTime();
        }
    }

}
