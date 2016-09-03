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
    private String tabTitles[] = new String[] { "Main", "To-Do's", "Notes" };
    private Context context;

    public TabsAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return MainRoomFragment.newInstance("", "");
        }
        else if (position == 1) {
            return RoomTasksFragment.newInstance("", "");
        }
        else {
            return RoomNotesFragment.newInstance("", "");
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
