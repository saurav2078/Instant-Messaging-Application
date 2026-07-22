package server;

import database.DBOperations;
import util.AppConstants;

import java.io.*;
import java.net.Socket;
import java.util.Map;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter    writer;
    private String         username;
    private final Map<String, PrintWriter> clientMap;

    public ClientHandler(Socket socket, Map<String, PrintWriter> clientMap) {
        this.socket    = socket;
        this.clientMap = clientMap;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // ── Handshake: first line = username ────────────────────────────
            username = reader.readLine();
            if (username == null || username.isBlank()) { socket.close(); return; }

            // FIX: Detect a one-shot admin kick command on the handshake line.
            // This prevents the admin connection from being registered as a
            // normal chat user (which caused "admin has joined!" broadcasts).
            if (username.startsWith(AppConstants.CMD_ADMIN_KICK_DIRECT)) {
                String payload = username.substring(AppConstants.CMD_ADMIN_KICK_DIRECT.length());
                int colon = payload.indexOf(':');
                if (colon > 0) {
                    String adminName  = payload.substring(0, colon);
                    String targetName = payload.substring(colon + 1).trim();
                    System.out.println("[Admin] Direct kick from " + adminName + " -> " + targetName);
                    Server.kickClient(targetName);
                }
                socket.close();
                return; // Do NOT continue into chat session setup
            }

            System.out.println("[Server] " + username + " connected from " + socket.getInetAddress());

            Server.addClient(username, writer);
            Server.broadcastSystem(username + " has joined the chat!");
            Server.broadcastUserList();

            // Send chat history to this new client only
            Server.sendHistoryToClient(writer);

            // ── Message Loop ─────────────────────────────────────────────────
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                // ── Private Message ──────────────────────────────────────────
                if (line.startsWith(AppConstants.CMD_PM)) {
                    String payload  = line.substring(AppConstants.CMD_PM.length());
                    int    colon    = payload.indexOf(':');
                    if (colon > 0) {
                        String recipient = payload.substring(0, colon);
                        String pmText    = payload.substring(colon + 1);
                        Server.sendPrivateMessage(username, writer, recipient, pmText);
                    }

                // ── NEW: Typing Indicator (group) ────────────────────────────
                } else if (line.startsWith(AppConstants.CMD_TYPING)) {
                    String target = line.substring(AppConstants.CMD_TYPING.length()).trim();
                    Server.relayTyping(username, target, true);

                // ── NEW: Typing Stop (group) ─────────────────────────────────
                } else if (line.startsWith(AppConstants.CMD_TYPING_STOP)) {
                    String target = line.substring(AppConstants.CMD_TYPING_STOP.length()).trim();
                    Server.relayTyping(username, target, false);

                // ── NEW: Admin Kick Command ──────────────────────────────────
                } else if (line.startsWith(AppConstants.CMD_ADMIN_KICK)) {
                    // Only honour kick commands from users whose username is in DB as admin
                    // (Simple check — in production you'd verify the role from the session)
                    String target = line.substring(AppConstants.CMD_ADMIN_KICK.length()).trim();
                    System.out.println("[Admin] " + username + " is kicking: " + target);
                    Server.kickClient(target);

                // ── Group / Broadcast Message ────────────────────────────────
                } else {
                    String formatted = AppConstants.formatMessage(username, line);
                    DBOperations.saveMessage(username, "ALL", line);
                    Server.broadcastMessage(formatted);
                    System.out.println("[Chat] " + formatted);
                }
            }

        } catch (IOException e) {
            System.out.println("[Server] " + username + " disconnected unexpectedly.");
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (username != null) {
            Server.removeClient(username);
            Server.broadcastSystem(username + " has left the chat.");
            Server.broadcastUserList();
            System.out.println("[Server] Cleaned up: " + username);
        }
        try { if (!socket.isClosed()) socket.close(); } catch (IOException e) { e.printStackTrace(); }
    }

    public String getUsername() { return username; }
}
