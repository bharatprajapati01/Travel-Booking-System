package travel.core;

import java.sql.*;
import java.util.*;

/** Data operations used by the web API. Mirrors the rules of the Swing GUI. All methods are serialized
 *  because SQLite allows only one writer at a time. */
public class TravelService {
    public static class ApiException extends Exception {
        public final int status;
        public ApiException(int status, String message) { super(message); this.status = status; }
    }

    private static final Object LOCK = new Object();

    public static List<Map<String, Object>> packages() throws Exception {
        synchronized (LOCK) {
            List<Map<String, Object>> out = new ArrayList<>();
            try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT id,name,destination,price,seats FROM packages ORDER BY id")) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getInt(1)); m.put("name", r.getString(2)); m.put("destination", r.getString(3));
                    m.put("price", r.getDouble(4)); m.put("seats", r.getInt(5));
                    out.add(m);
                }
            }
            return out;
        }
    }

    public static List<Map<String, Object>> travellers() throws Exception {
        synchronized (LOCK) {
            List<Map<String, Object>> out = new ArrayList<>();
            try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT id,name,phone,email FROM travellers ORDER BY id")) {
                while (r.next()) out.add(traveller(r.getInt(1), r.getString(2), r.getString(3), r.getString(4)));
            }
            return out;
        }
    }

    private static Map<String, Object> traveller(int id, String name, String phone, String email) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", name); m.put("phone", phone); m.put("email", email);
        return m;
    }

    public static Map<String, Object> registerTraveller(Map<String, String> body) throws Exception {
        String name = clean(body.get("name")), phone = clean(body.get("phone")), email = clean(body.get("email"));
        if (name.isEmpty()) throw new ApiException(400, "Name is required.");
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new ApiException(400, "Email address looks invalid.");
        if (!phone.isEmpty() && !phone.matches("^[0-9+()\\-\\s]{6,20}$"))
            throw new ApiException(400, "Phone number looks invalid.");
        synchronized (LOCK) {
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement p = c.prepareStatement("INSERT INTO travellers(name,phone,email) VALUES(?,?,?)")) {
                p.setString(1, name); p.setString(2, phone); p.setString(3, email);
                p.executeUpdate();
                try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT last_insert_rowid()")) {
                    r.next();
                    return traveller(r.getInt(1), name, phone, email);
                }
            }
        }
    }

    public static List<Map<String, Object>> bookings() throws Exception {
        synchronized (LOCK) {
            List<Map<String, Object>> out = new ArrayList<>();
            String sql = "SELECT b.id,b.traveller_id,t.name,b.package_id,p.name,p.destination,b.seats,b.total,b.status "
                + "FROM bookings b JOIN travellers t ON b.traveller_id=t.id JOIN packages p ON b.package_id=p.id ORDER BY b.id DESC";
            try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getInt(1)); m.put("travellerId", r.getInt(2)); m.put("traveller", r.getString(3));
                    m.put("packageId", r.getInt(4)); m.put("package", r.getString(5)); m.put("destination", r.getString(6));
                    m.put("seats", r.getInt(7)); m.put("total", r.getDouble(8)); m.put("status", r.getString(9));
                    out.add(m);
                }
            }
            return out;
        }
    }

    public static Map<String, Object> book(Map<String, String> body) throws Exception {
        int tid = intField(body, "travellerId", "Traveller"), pid = intField(body, "packageId", "Package");
        int count = intField(body, "seats", "Seats");
        synchronized (LOCK) {
            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);
                try {
                    try (PreparedStatement q = c.prepareStatement("SELECT 1 FROM travellers WHERE id=?")) {
                        q.setInt(1, tid);
                        try (ResultSet r = q.executeQuery()) { if (!r.next()) throw new ApiException(404, "Traveller not found."); }
                    }
                    double price; int available;
                    try (PreparedStatement q = c.prepareStatement("SELECT price,seats FROM packages WHERE id=?")) {
                        q.setInt(1, pid);
                        try (ResultSet r = q.executeQuery()) {
                            if (!r.next()) throw new ApiException(404, "Package not found.");
                            price = r.getDouble(1); available = r.getInt(2);
                        }
                    }
                    if (count <= 0 || count > available) throw new ApiException(409, "Not enough seats available.");
                    try (PreparedStatement u = c.prepareStatement("UPDATE packages SET seats=seats-? WHERE id=?")) {
                        u.setInt(1, count); u.setInt(2, pid); u.executeUpdate();
                    }
                    int bookingId;
                    try (PreparedStatement b = c.prepareStatement(
                        "INSERT INTO bookings(traveller_id,package_id,seats,total,status) VALUES(?,?,?,?,'CONFIRMED')")) {
                        b.setInt(1, tid); b.setInt(2, pid); b.setInt(3, count); b.setDouble(4, price * count);
                        b.executeUpdate();
                    }
                    try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT last_insert_rowid()")) {
                        r.next(); bookingId = r.getInt(1);
                    }
                    c.commit();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", bookingId); m.put("seats", count); m.put("total", price * count); m.put("status", "CONFIRMED");
                    return m;
                } catch (Exception e) { c.rollback(); throw e; }
            }
        }
    }

    public static Map<String, Object> cancel(int bookingId) throws Exception {
        synchronized (LOCK) {
            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);
                try {
                    int pid, count; double total;
                    try (PreparedStatement q = c.prepareStatement("SELECT package_id,seats,total,status FROM bookings WHERE id=?")) {
                        q.setInt(1, bookingId);
                        try (ResultSet r = q.executeQuery()) {
                            if (!r.next()) throw new ApiException(404, "Booking not found.");
                            if (!"CONFIRMED".equals(r.getString(4))) throw new ApiException(409, "Booking is already cancelled.");
                            pid = r.getInt(1); count = r.getInt(2); total = r.getDouble(3);
                        }
                    }
                    double refund = new Booking(bookingId, pid, count, total).cancelBooking(total);
                    try (PreparedStatement up = c.prepareStatement("UPDATE bookings SET status='CANCELLED' WHERE id=?")) {
                        up.setInt(1, bookingId); up.executeUpdate();
                    }
                    try (PreparedStatement pp = c.prepareStatement("UPDATE packages SET seats=seats+? WHERE id=?")) {
                        pp.setInt(1, count); pp.setInt(2, pid); pp.executeUpdate();
                    }
                    c.commit();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", bookingId); m.put("status", "CANCELLED"); m.put("refund", refund); m.put("charge", total - refund);
                    return m;
                } catch (Exception e) { c.rollback(); throw e; }
            }
        }
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }

    private static int intField(Map<String, String> body, String key, String label) throws ApiException {
        String v = body.get(key);
        try { return Integer.parseInt(v == null ? "" : v.trim()); }
        catch (NumberFormatException e) { throw new ApiException(400, label + " must be a valid number."); }
    }
}
