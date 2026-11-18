package travel.core;
public class Booking implements Cancellable {
    private int id, packageId, seats;
    private double total;
    public Booking(int id,int packageId,int seats,double total) {
        this.id=id; this.packageId=packageId; this.seats=seats; this.total=total;
    }
    public double cancelBooking(double amount) { return amount*0.90; }
}
