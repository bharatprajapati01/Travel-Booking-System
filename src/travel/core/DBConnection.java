package travel.core;
import java.sql.*;
import java.io.File;
public class DBConnection {
    private static final String URL = "jdbc:sqlite:travel_booking.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        String[] sql = {
            "CREATE TABLE IF NOT EXISTS packages (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, destination TEXT NOT NULL, price REAL NOT NULL, seats INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS travellers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, email TEXT)",
            "CREATE TABLE IF NOT EXISTS bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, traveller_id INTEGER NOT NULL, package_id INTEGER NOT NULL, seats INTEGER NOT NULL, total REAL NOT NULL, status TEXT DEFAULT 'CONFIRMED', FOREIGN KEY(traveller_id) REFERENCES travellers(id), FOREIGN KEY(package_id) REFERENCES packages(id))"
        };
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            for (String query : sql) st.execute(query);
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM packages")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.executeUpdate("INSERT INTO packages(name,destination,price,seats) VALUES " +
                        "('Goa Beach Tour','Goa',8500,20),('Manali Adventure','Manali',12000,15)," +
                        "('Dubai City Tour','Dubai',45000,10),('Jaipur Heritage','Jaipur',7000,18)");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database setup failed: " + e.getMessage(), e);
        }
    }
}
