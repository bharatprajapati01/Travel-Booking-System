package travel.core;
public class TravelPackage {
    protected int id, seats;
    protected String name, destination;
    protected double price;
    public TravelPackage(int id,String name,String destination,double price,int seats) {
        this.id=id; this.name=name; this.destination=destination; this.price=price; this.seats=seats;
    }
    public boolean reserveSeats(int count) throws SeatsFullException {
        if(count<=0 || count>seats) throw new SeatsFullException("Not enough seats available.");
        seats-=count; return true;
    }
    public void releaseSeats(int count) { seats+=count; }
    public int getId(){return id;} public String getName(){return name;}
    public String getDestination(){return destination;} public double getPrice(){return price;}
    public int getSeats(){return seats;}
}
