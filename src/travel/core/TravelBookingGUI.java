package travel.core;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class TravelBookingGUI extends JFrame {
    private JTextArea output = new JTextArea(18,65);

    public TravelBookingGUI() {
        setTitle("Travel Booking System");
        setSize(800,550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        output.setEditable(false);
        output.setFont(new Font("Consolas", Font.PLAIN, 14));
        JPanel buttons = new JPanel(new GridLayout(2,4,8,8));
        addButton(buttons,"Package Listing",this::showPackages);
        addButton(buttons,"Register Traveller",this::registerTraveller);
        addButton(buttons,"Registered Users",this::showRegisteredUsers);
        addButton(buttons,"Book Package",this::bookPackage);
        addButton(buttons,"Cancel Booking",this::cancelBooking);
        addButton(buttons,"Booking Report",this::bookingReport);
        addButton(buttons,"Clear",()->output.setText(""));
        add(buttons,BorderLayout.NORTH);
        add(new JScrollPane(output),BorderLayout.CENTER);
        output.setText("Welcome to Travel Booking System!\nChoose a module above.");
    }

    private void addButton(JPanel p,String text,Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        p.add(b);
    }

    private void showPackages() {
        output.setText("TOUR PACKAGES\n");
        output.append(String.format("%-4s %-22s %-14s %10s %6s\n", "ID", "Name", "Destination", "Price(₹)", "Seats"));
        output.append("-------------------------------------------------------------\n");
        try (Connection c=DBConnection.getConnection();
             Statement s=c.createStatement();
             ResultSet r=s.executeQuery("SELECT * FROM packages")) {
            while(r.next()) output.append(String.format("%-4d %-22s %-14s %10.0f %6d\n",
                r.getInt("id"), r.getString("name"), r.getString("destination"),
                r.getDouble("price"), r.getInt("seats")));
        } catch(Exception e) { error(e); }
    }

    private void registerTraveller() {
        JTextField name=new JTextField(), phone=new JTextField(), email=new JTextField();
        Object[] fields={"Name:",name,"Phone:",phone,"Email:",email};
        if(JOptionPane.showConfirmDialog(this,fields,"Traveller Registration",
            JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            try(Connection c=DBConnection.getConnection();
                PreparedStatement p=c.prepareStatement(
                    "INSERT INTO travellers(name,phone,email) VALUES(?,?,?)")) {
                p.setString(1,name.getText()); p.setString(2,phone.getText());
                p.setString(3,email.getText()); p.executeUpdate();
                output.setText("Traveller registered successfully.\n");
                try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT last_insert_rowid()")) {
                    if(r.next()) output.append("Traveller ID: "+r.getInt(1));
                }
            } catch(Exception e) { error(e); }
        }
    }

    private void showRegisteredUsers() {
        output.setText("REGISTERED USERS\n");
        output.append(String.format("%-6s %-20s %-15s %-25s\n", "ID", "Name", "Phone", "Email"));
        output.append("----------------------------------------------------------------------\n");
        try (Connection c=DBConnection.getConnection();
             Statement s=c.createStatement();
             ResultSet r=s.executeQuery("SELECT * FROM travellers ORDER BY id")) {
            int count = 0;
            while(r.next()) {
                output.append(String.format("%-6d %-20s %-15s %-25s\n",
                    r.getInt("id"), r.getString("name"),
                    r.getString("phone"), r.getString("email")));
                count++;
            }
            output.append("----------------------------------------------------------------------\n");
            output.append("Total registered users: " + count + "\n");
        } catch(Exception e) { error(e); }
    }

    private synchronized void bookPackage() {
        JTextField traveller=new JTextField(), pkg=new JTextField(), seats=new JTextField("1");
        Object[] fields={"Traveller ID:",traveller,"Package ID:",pkg,"Seats:",seats};
        if(JOptionPane.showConfirmDialog(this,fields,"Book Package",
            JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            try(Connection c=DBConnection.getConnection()) {
                c.setAutoCommit(false);
                try {
                    int tid=Integer.parseInt(traveller.getText());
                    int pid=Integer.parseInt(pkg.getText());
                    int count=Integer.parseInt(seats.getText());
                    try(PreparedStatement q=c.prepareStatement("SELECT price,seats FROM packages WHERE id=?")) {
                        q.setInt(1,pid);
                        try(ResultSet r=q.executeQuery()) {
                            if(!r.next()) throw new Exception("Package not found.");
                            double price=r.getDouble("price");
                            int available=r.getInt("seats");
                            if(count<=0 || count>available) throw new SeatsFullException("Not enough seats available.");
                            try(PreparedStatement u=c.prepareStatement("UPDATE packages SET seats=seats-? WHERE id=?")) {
                                u.setInt(1,count); u.setInt(2,pid); u.executeUpdate();
                            }
                            try(PreparedStatement b=c.prepareStatement(
                                "INSERT INTO bookings(traveller_id,package_id,seats,total,status) VALUES(?,?,?,?,'CONFIRMED')")) {
                                b.setInt(1,tid); b.setInt(2,pid); b.setInt(3,count); b.setDouble(4,price*count);
                                b.executeUpdate();
                            }
                            c.commit();
                            output.setText("Booking successful!\nTotal amount: ₹"+(price*count));
                        }
                    }
                } catch(Exception e) {
                    c.rollback();
                    throw e;
                }
            } catch(Exception e) { error(e); }
        }
    }

    private void cancelBooking() {
        String input=JOptionPane.showInputDialog(this,"Enter Booking ID:");
        if(input==null) return;
        try(Connection c=DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int bookingId=Integer.parseInt(input);
                int pid,count; double total;
                try(PreparedStatement q=c.prepareStatement(
                    "SELECT package_id,seats,total,status FROM bookings WHERE id=?")) {
                    q.setInt(1,bookingId);
                    try(ResultSet r=q.executeQuery()) {
                        if(!r.next()) throw new Exception("Booking not found.");
                        if(!"CONFIRMED".equals(r.getString("status"))) throw new Exception("Booking is already cancelled.");
                        pid=r.getInt("package_id"); count=r.getInt("seats"); total=r.getDouble("total");
                    }
                }
                double refund=new Booking(bookingId,pid,count,total).cancelBooking(total);
                try(PreparedStatement up=c.prepareStatement("UPDATE bookings SET status='CANCELLED' WHERE id=?")) {
                    up.setInt(1,bookingId); up.executeUpdate();
                }
                try(PreparedStatement pp=c.prepareStatement("UPDATE packages SET seats=seats+? WHERE id=?")) {
                    pp.setInt(1,count); pp.setInt(2,pid); pp.executeUpdate();
                }
                c.commit();
                output.setText("Booking cancelled.\nRefund (90%): ₹"+refund+
                    "\nCancellation charge (10%): ₹"+(total-refund));
            } catch(Exception e) {
                c.rollback();
                throw e;
            }
        } catch(Exception e) { error(e); }
    }

    private void bookingReport() {
        output.setText("BOOKING REPORT\n");
        output.append(String.format("%-6s %-16s %-20s %6s %10s %-10s\n", "ID", "Traveller", "Package", "Seats", "Total(₹)", "Status"));
        output.append("------------------------------------------------------------------------\n");
        String sql="SELECT b.id,t.name,p.name,b.seats,b.total,b.status FROM bookings b "+
            "JOIN travellers t ON b.traveller_id=t.id JOIN packages p ON b.package_id=p.id ORDER BY b.id";
        try(Connection c=DBConnection.getConnection();
            Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) {
            while(r.next()) output.append(String.format("%-6d %-16s %-20s %6d %10.0f %-10s\n",
                r.getInt(1), r.getString(2), r.getString(3),
                r.getInt(4), r.getDouble(5), r.getString(6)));
        } catch(Exception e) { error(e); }
    }
    private void error(Exception e) { output.setText("Error: "+e.getMessage()); }
}
