package com.simplecity.amp_library.playback.constants;

public final class ExternalIntents {
    public static final String PLAY_STATUS_REQUEST = "com.android.music.playstatusrequest";
    public static final String PLAY_STATUS_RESPONSE = "com.android.music.playstatusresponse";
    public static final String AVRCP_PLAY_STATE_CHANGED = "com.android.music.playstatechanged";
    public static final String AVRCP_META_CHANGED = "com.android.music.metachanged";
    public static final String TASKER = "net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS";
    public static final String SCROBBLER = "com.adam.aslfms.notify.playstatechanged";
    public static final String PEBBLE = "com.getpebble.action.NOW_PLAYING";

    // Constructeur privé pour empêcher l'instanciation de cette classe
    private ExternalIntents() {
        throw new AssertionError("This class should not be instantiated.");
    }
}
