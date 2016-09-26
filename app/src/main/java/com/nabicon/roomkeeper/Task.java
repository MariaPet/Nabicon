package com.nabicon.roomkeeper;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by mariloo on 24/9/2016.
 */
public class Task implements Parcelable{

    String attachmentName;
    String title;
    Calendar deadline;

    public Task() {

    }

    protected Task(Parcel in) {
    }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) {
            return new Task(in);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
