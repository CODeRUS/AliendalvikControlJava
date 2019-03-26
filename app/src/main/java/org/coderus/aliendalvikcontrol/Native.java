package org.coderus.aliendalvikcontrol;

class Native {
    static {
        System.loadLibrary("native-lib");
    }
    static native String reply(String data);
}
