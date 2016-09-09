package com.nabicon.roomkeeper;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nabicon.R;

/**
 * A simple {@link DialogFragment} subclass.
 */
public class NewTaskDialogFragment extends DialogFragment {


    public NewTaskDialogFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle("New Task");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_task_dialog, container, false);
    }

}
