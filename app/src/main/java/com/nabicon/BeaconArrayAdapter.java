package com.nabicon;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by mariloo on 13/7/2016.
 */
public class BeaconArrayAdapter extends ArrayAdapter<Beacon>{

    private static final int BLACK = Color.rgb(0, 0, 0);
    private static final int GREEN  = Color.rgb(0, 142, 9);
    private static final int ORANGE = Color.rgb(255, 165, 0);
    private static final int RED = Color.rgb(255, 5, 5);
    private static final int GREY = Color.rgb(150, 150, 150);

    public BeaconArrayAdapter(Context context, int resource, List<Beacon> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Beacon beacon = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.beacon_list_item, parent, false);
        }
        ImageView registrationStatus = (ImageView) convertView.findViewById(R.id.registrationStatus);
        TextView beaconId = (TextView) convertView.findViewById(R.id.beaconId);
        beaconId.setText(beacon.getHexId());

        switch (beacon.status) {
            //TODO why should there be a break statement at the end of each case?
            case Beacon.UNREGISTERED:
                registrationStatus.setImageResource(R.drawable.ic_action_lock_open);
                registrationStatus.setColorFilter(BLACK);
                beaconId.setTextColor(BLACK);
                break;
            case Beacon.STATUS_ACTIVE:
                registrationStatus.setImageResource(R.drawable.ic_action_check_circle);
                registrationStatus.setColorFilter(GREEN);
                beaconId.setTextColor(BLACK);
                break;
            case Beacon.STATUS_INACTIVE:
                registrationStatus.setImageResource(R.drawable.ic_action_check_circle);
                registrationStatus.setColorFilter(ORANGE);
                beaconId.setTextColor(BLACK);
                break;
            case Beacon.STATUS_DECOMMISSIONED:
                registrationStatus.setImageResource(R.drawable.ic_action_highlight_off);
                registrationStatus.setColorFilter(RED);
                beaconId.setTextColor(GREY);
                break;
            case Beacon.NOT_AUTHORIZED:
                registrationStatus.setImageResource(R.drawable.ic_action_lock);
                registrationStatus.setColorFilter(GREY);
                beaconId.setTextColor(GREY);
                break;
            case Beacon.STATUS_UNSPECIFIED:
                registrationStatus.setImageResource(R.drawable.ic_action_help);
                registrationStatus.setColorFilter(GREY);
                beaconId.setTextColor(GREY);
                break;
            default:
                registrationStatus.setImageResource(R.drawable.ic_action_help);
                registrationStatus.setColorFilter(BLACK);
                beaconId.setTextColor(BLACK);
                break;
        }
        return convertView;
    }
}
