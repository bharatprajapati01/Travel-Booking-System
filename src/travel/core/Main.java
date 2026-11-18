package travel.core;
import javax.swing.SwingUtilities;
public class Main {
    public static void main(String[] args) {
        DBConnection.initializeDatabase();
        SwingUtilities.invokeLater(() -> new TravelBookingGUI().setVisible(true));
    }
}
