package model;

/**
 * User.java  –  MODIFIED
 *
 * Changes from original:
 *   + role   field  ('user' or 'admin')
 *   + status field  ('active' or 'banned')
 *   + Updated constructors and getters/setters
 */
public class User {

    private int    id;
    private String username;
    private String password;
    private String role;    // NEW: 'user' or 'admin'
    private String status;  // NEW: 'active' or 'banned'

    public User() {}

    /** Used after login */
    public User(int id, String username) {
        this.id       = id;
        this.username = username;
        this.role     = "user";
        this.status   = "active";
    }

    /** Full constructor including role and status */
    public User(int id, String username, String role, String status) {
        this.id       = id;
        this.username = username;
        this.role     = role;
        this.status   = status;
    }

    /** Used during registration */
    public User(int id, String username, String password) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.role     = "user";
        this.status   = "active";
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getUsername()         { return username; }
    public void   setUsername(String u) { this.username = u; }

    public String getPassword()         { return password; }
    public void   setPassword(String p) { this.password = p; }

    public String getRole()             { return role; }
    public void   setRole(String r)     { this.role = r; }

    public String getStatus()           { return status; }
    public void   setStatus(String s)   { this.status = s; }

    /** Convenience helper */
    public boolean isAdmin()  { return "admin".equalsIgnoreCase(role); }
    public boolean isBanned() { return "banned".equalsIgnoreCase(status); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "', status='" + status + "'}";
    }
}
