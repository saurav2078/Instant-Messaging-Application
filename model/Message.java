package model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Message.java
 *
 * Model class representing a single chat message.
 */
public class Message {

    private int       id;
    private String    sender;
    private String    receiver;   // "ALL" for group broadcast
    private String    message;
    private Timestamp timestamp;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    // ── Constructors ─────────────────────────────────────────────────────────

    public Message() {}

    public Message(int id, String sender, String receiver, String message, Timestamp timestamp) {
        this.id        = id;
        this.sender    = sender;
        this.receiver  = receiver;
        this.message   = message;
        this.timestamp = timestamp;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int       getId()          { return id; }
    public String    getSender()      { return sender; }
    public String    getReceiver()    { return receiver; }
    public String    getMessage()     { return message; }
    public Timestamp getTimestamp()   { return timestamp; }

    public void setId(int id)               { this.id = id; }
    public void setSender(String s)         { this.sender = s; }
    public void setReceiver(String r)       { this.receiver = r; }
    public void setMessage(String m)        { this.message = m; }
    public void setTimestamp(Timestamp t)   { this.timestamp = t; }

    /**
     * Returns a nicely formatted string for display in the chat window.
     * Format: [HH:mm:ss] username: message text
     */
    public String getFormattedMessage() {
        String time = (timestamp != null) ? "[" + SDF.format(timestamp) + "] " : "";
        return time + sender + ": " + message;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
