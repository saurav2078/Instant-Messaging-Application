package util;

import java.text.SimpleDateFormat;
import java.util.Date;


public class AppConstants {

    // ── Network ──────────────────────────────────────────────────────────────
    public static final int    SERVER_PORT = 12345;
    public static final String SERVER_HOST = "localhost";

    // ── Protocol Tokens (original) ────────────────────────────────────────────
    public static final String CMD_PREFIX      = "CMD:";
    public static final String CMD_USER_LIST   = "CMD:USER_LIST:";
    public static final String CMD_USER_JOINED = "CMD:JOINED:";
    public static final String CMD_USER_LEFT   = "CMD:LEFT:";
    public static final String CMD_HISTORY     = "CMD:HISTORY:";

    public static final String CMD_PM          = "CMD:PM:";
    public static final String CMD_PM_IN       = "CMD:PM_IN:";
    public static final String CMD_PM_ECHO     = "CMD:PM_ECHO:";
    public static final String CMD_PM_OFFLINE  = "CMD:PM_OFFLINE:";
    public static final String CMD_PM_HISTORY  = "CMD:PM_HISTORY:";

    // ── NEW: Typing Indicator ────────────────────────────────────────────────
    /** Client → Server: "CMD:TYPING:recipientOrALL"  */
    public static final String CMD_TYPING      = "CMD:TYPING:";
    /** Client → Server: "CMD:TYPING_STOP:recipientOrALL" */
    public static final String CMD_TYPING_STOP = "CMD:TYPING_STOP:";
    /** Server → Client: "CMD:TYPING_NOTIFY:username"  */
    public static final String CMD_TYPING_NOTIFY      = "CMD:TYPING_NOTIFY:";
    public static final String CMD_TYPING_STOP_NOTIFY = "CMD:TYPING_STOP_NOTIFY:";

    // ── NEW: Admin Commands ───────────────────────────────────────────────────
    /** Server → banned client: "CMD:BANNED" – kick and inform */
    public static final String CMD_BANNED      = "CMD:BANNED";
    /** Admin requests full user list from server */
    public static final String CMD_ADMIN_USERS = "CMD:ADMIN_USERS";
    /** Server response: "CMD:ADMIN_USER_DATA:json" */
    public static final String CMD_ADMIN_USER_DATA = "CMD:ADMIN_USER_DATA:";
    /** Admin kicks a user: "CMD:ADMIN_KICK:username" */
    public static final String CMD_ADMIN_KICK  = "CMD:ADMIN_KICK:";
    /**
     * NEW: One-shot kick sent on the handshake line so the server does NOT
     * create a full chat session for the admin connection.
     * Format: "CMD:ADMIN_KICK_DIRECT:<adminUsername>:<targetUsername>"
     */
    public static final String CMD_ADMIN_KICK_DIRECT = "CMD:ADMIN_KICK_DIRECT:";

    // ── UI Strings ────────────────────────────────────────────────────────────
    public static final String APP_TITLE      = "ChatApp – BSc CSIT Advanced Java";
    public static final String LOGIN_TITLE    = "ChatApp – Login";
    public static final String REGISTER_TITLE = "ChatApp – Register";
    public static final String ADMIN_TITLE    = "ChatApp – Admin Panel";

    // ── Date / Time ───────────────────────────────────────────────────────────
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static String getCurrentTime() { return TIME_FORMAT.format(new Date()); }
    public static String getCurrentDate() { return DATE_FORMAT.format(new Date()); }

    public static String formatMessage(String sender, String text) {
        return "[" + getCurrentTime() + "] " + sender + ": " + text;
    }
    public static String formatSystem(String text) {
        return "[" + getCurrentTime() + "] *** " + text + " ***";
    }
    public static String formatPrivate(String from, String to, String text) {
        return "[" + getCurrentTime() + "] [PM " + from + " -> " + to + "]: " + text;
    }

    private AppConstants() {}
}
