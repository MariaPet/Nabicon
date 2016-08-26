package com.nabicon;

import android.util.Base64;
import android.view.View;

/**
 * Created by mariloo on 12/7/2016.
 */
class Utils {

    private Utils() {}

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    //TODO what does Base64 do?
    static byte[] base64Decode(String s) { return Base64.decode(s, Base64.DEFAULT);}

    static String base64Encode(byte[] b) { return Base64.encodeToString(b, Base64.DEFAULT).trim();}

    static String toHexString(byte[] bytes) {
        //TODO why is the length doubled??
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            //TODO no fucking clue what's going on here
            int c = bytes[i] & 0xFF;
            chars[i * 2] = HEX[c >>> 4];
            chars[i * 2 + 1] = HEX[c & 0x0F];
        }
        return new String(chars).toLowerCase();
    }

    static void setEnabledViews(boolean enabled, View... views) {
        if (views == null || views.length == 0) {
            return;
        }
        for (View v : views) {
            v.setEnabled(enabled);
        }
    }
}
