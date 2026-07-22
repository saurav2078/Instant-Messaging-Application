package server;

import database.DBOperations;
import model.Message;
import util.AppConstants;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class Server {

    // Shared state: username → PrintWriter
    private static final Map<String, PrintWriter> clientMap = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  ChatApp Server – BSc CSIT Advanced Java    ");
        System.out.println("==============================================");
        System.out.println("[Server] Starting on port " + AppConstants.SERVER_PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(AppConstants.SERVER_PORT)) {
            System.out.println("[Server] Listening. Press Ctrl+C to stop.\n");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New connection from: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket, clientMap));
            }
        } catch (IOException e) {
            System.err.println("[Server ERROR] Could not start: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    // ── Client Management ─────────────────────────────────────────────────────

    public static synchronized void addClient(String username, PrintWriter writer) {
        clientMap.put(username, writer);
        System.out.println("[Server] Online: " + clientMap.keySet());
    }

    public static synchronized void removeClient(String username) {
        clientMap.remove(username);
        System.out.println("[Server] Removed: " + username + ". Online: " + clientMap.keySet());
    }

    // ── Broadcast Methods (original, unchanged) ───────────────────────────────

    public static synchronized void broadcastMessage(String message) {
        for (PrintWriter writer : clientMap.values()) writer.println(message);
    }

    public static synchronized void broadcastSystem(String notification) {
        String formatted = AppConstants.formatSystem(notification);
        System.out.println("[System] " + formatted);
        for (PrintWriter writer : clientMap.values()) writer.println(formatted);
    }

    public static synchronized void broadcastUserList() {
        String userList = String.join(",", clientMap.keySet());
        String cmd = AppConstants.CMD_USER_LIST + userList;
        for (PrintWriter writer : clientMap.values()) writer.println(cmd);
    }

    public static void sendHistoryToClient(PrintWriter writer) {
        List<Message> history = DBOperations.getChatHistory();
        writer.println(AppConstants.CMD_HISTORY + "--- Chat History ---");
        for (Message msg : history) writer.println(msg.getFormattedMessage());
        writer.println(AppConstants.CMD_HISTORY + "--- End of History ---");
    }

    public static synchronized void sendPrivateMessage(
            String senderUsername, PrintWriter senderWriter,
            String recipientUsername, String messageText) {

        PrintWriter recipientWriter = clientMap.get(recipientUsername);
        if (recipientWriter != null) {
            recipientWriter.println(AppConstants.CMD_PM_IN + senderUsername + ":" + messageText);
            senderWriter.println(AppConstants.CMD_PM_ECHO + recipientUsername + ":" + messageText);
            DBOperations.saveMessage(senderUsername, recipientUsername, messageText);
            System.out.println("[PM] " + senderUsername + " -> " + recipientUsername + ": " + messageText);
        } else {
            senderWriter.println(AppConstants.CMD_PM_OFFLINE + recipientUsername);
        }
    }

    // ── NEW: Typing Indicator Relay ───────────────────────────────────────────

    /**
     * Forwards a typing notification to all clients except the typer.
     * For group chat we broadcast; for PM we send only to the recipient.
     *
     * @param typerUsername  the user who is typing
     * @param target         "ALL" for group chat, or a specific username for PM
     * @param isTyping       true = started, false = stopped
     */
    public static synchronized void relayTyping(String typerUsername, String target, boolean isTyping) {
        String cmd = isTyping
            ? AppConstants.CMD_TYPING_NOTIFY + typerUsername
            : AppConstants.CMD_TYPING_STOP_NOTIFY + typerUsername;

        if ("ALL".equals(target)) {
            // Broadcast to everyone except the typer
            for (Map.Entry<String, PrintWriter> entry : clientMap.entrySet()) {
                if (!entry.getKey().equals(typerUsername)) {
                    entry.getValue().println(cmd);
                }
            }
        } else {
            // Send only to the specific recipient
            PrintWriter pw = clientMap.get(target);
            if (pw != null) pw.println(cmd);
        }
    }

    // ── NEW: Admin Kick / Ban ─────────────────────────────────────────────────

    /**
     * Sends the CMD_BANNED command to a connected user, then forcibly
     * closes their connection. The ClientHandler cleanup() will fire
     * automatically when their socket read throws IOException.
     *
     * @param targetUsername  the user to kick off the server
     */
    public static synchronized void kickClient(String targetUsername) {
        PrintWriter pw = clientMap.get(targetUsername);
        if (pw != null) {
            pw.println(AppConstants.CMD_BANNED); // Tell client why they're disconnecting
            pw.flush();
            // The ClientHandler's socket.close() will be called by the handler itself
            // after it detects the stream end; we rely on CMD_BANNED for the client to
            // close cleanly. For a hard kick we remove from map immediately.
            clientMap.remove(targetUsername);
            broadcastUserList();
            broadcastSystem(targetUsername + " has been removed by an administrator.");
            System.out.println("[Admin] Kicked: " + targetUsername);
        }
    }
}
