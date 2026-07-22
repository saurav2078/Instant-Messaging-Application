package client;

import util.AppConstants;

import java.io.*;
import java.net.Socket;

public class Client {

    // ── Fields ────────────────────────────────────────────────────────────────
    private Socket         socket;
    private BufferedReader reader;   // Reads messages FROM server
    private PrintWriter    writer;   // Sends messages TO server
    private String         username;
    private boolean        connected = false;

    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionLost();
    }

    private MessageListener listener;

    // ── Constructor ───────────────────────────────────────────────────────────
    public Client(String username) {
        this.username = username;
    }

    // ── connect() ─────────────────────────────────────────────────────────────

    /**
     * Opens a TCP socket to the server, sends username, then starts
     * a background receiver thread.
     *
     * @param listener  callback for incoming messages
     * @throws IOException if the connection fails
     */
    public void connect(MessageListener listener) throws IOException {
        this.listener = listener;

        // Open TCP connection ──────────────────────────────────────
        // Socket constructor blocks until the server accepts the connection
        socket = new Socket(AppConstants.SERVER_HOST, AppConstants.SERVER_PORT);

        // Set up I/O streams ───────────────────────────────────────
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        // autoFlush=true: every println() is immediately sent over TCP

        connected = true;

        // Send username (handshake) ────────────────────────────────
        // The server reads this as the very first line to identify this client
        writer.println(username);

        // Start receiver thread ───────────────────────────────────
        startReceiverThread();

        System.out.println("[Client] Connected to server as: " + username);
    }

    
    private void startReceiverThread() {
        Thread receiverThread = new Thread(() -> {
            try {
                String line;
                // Blocks until a line arrives; null means server closed the connection
                while (connected && (line = reader.readLine()) != null) {
                    final String msg = line; // Must be effectively final for lambda
                    if (listener != null) {
                        // Deliver to GUI safely
                        javax.swing.SwingUtilities.invokeLater(() -> listener.onMessageReceived(msg));
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("[Client] Connection lost: " + e.getMessage());
                    if (listener != null) {
                        javax.swing.SwingUtilities.invokeLater(() -> listener.onConnectionLost());
                    }
                }
            }
        });

        receiverThread.setDaemon(true);    // Dies when the main thread exits
        receiverThread.setName("ReceiverThread-" + username);
        receiverThread.start();

        System.out.println("[Client] Receiver thread started.");
    }

    // ── sendMessage() ─────────────────────────────────────────────────────────

    /**
     * Sends a message to the server over the TCP connection.
     * Called from the GUI (Event Dispatch Thread).
     *
     * @param message  text to send
     */
    public void sendMessage(String message) {
        if (connected && writer != null) {
            writer.println(message); // autoFlush sends immediately
        }
    }

    // ── disconnect() ─────────────────────────────────────────────────────────

    public void disconnect() {
        connected = false;
        try {
            
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[Client] Disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() { return connected; }
    public String  getUsername() { return username;  }
}