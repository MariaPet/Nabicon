package com.nabicon.roomkeeper;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by mariloo on 30/8/2016.
 */
public class TabsAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    private String tabTitles[] = new String[] { "Main", "Tasks", "Notes" };
    private Fragment mainRoomFragment, tasksFragment, notesFragment;

    public TabsAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.mainRoomFragment = MainRoomFragment.newInstance("", "");
        this.tasksFragment = RoomTasksFragment.newInstance("", "");
        this.notesFragment = RoomNotesFragment.newInstance("", "");
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return this.mainRoomFragment;
        }
        else if (position == 1) {
            return this.tasksFragment;
        }
        else {
            return this.notesFragment;
        }
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}
