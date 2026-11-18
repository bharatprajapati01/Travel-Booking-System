package travel.core;
import javax.swing.SwingUtilities;
import java.nio.file.Paths;

public class Main {
    /** Usage:  Main            -> Swing desktop app
     *          Main web [port] -> web frontend + API at http://localhost:<port> (default 8080) */
    public static void main(String[] args) throws Exception {
        DBConnection.initializeDatabase();
        if (args.length > 0 && args[0].equalsIgnoreCase("web")) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
            WebServer.start(port, Paths.get("web"));
            System.out.println("Travel Booking System running at http://localhost:" + port);
            return;
        }
        SwingUtilities.invokeLater(() -> new TravelBookingGUI().setVisible(true));
    }
}
