# Travel Booking System

A Java-based desktop application for managing tour packages, traveller registration, package bookings, booking cancellations, and booking reports. The application uses a Swing graphical user interface and stores its data in an SQLite database.

## Features

- **Package Listing** – View available tour packages, destinations, prices, and seats.
- **Traveller Registration** – Register travellers and generate traveller IDs.
- **Registered Users** – View registered travellers.
- **Package Booking** – Book a tour package using the available seats.
- **Booking Cancellation** – Cancel a booking. The cancellation policy applies a 10% cancellation charge and a 90% refund.
- **Booking Report** – View booking information and reports.
- **Database Management** – Store application data locally using SQLite.

## Web Frontend

A responsive web UI (packages, booking, cancellation with refund preview, booking report, traveller registration) runs on top of the same SQLite database. It uses only the JDK's built-in HTTP server, so there are no extra dependencies.

```bash
./run-web.sh          # Windows: run-web.bat   (optional port argument, default 8080)
```

Then open <http://localhost:8080>. Run it from the project root so `web/` and `travel_booking.db` are found. The Swing app still works as before (`run.bat`).

| Method & path | Purpose |
|---|---|
| `GET /api/packages` | List packages |
| `GET /api/travellers`, `POST /api/travellers` | List / register travellers |
| `GET /api/bookings`, `POST /api/bookings` | Booking report / create booking |
| `POST /api/bookings/{id}/cancel` | Cancel (90% refund, 10% charge) |

## Technologies Used

- Java 17
- Java Swing (GUI)
- JDBC
- SQLite
- Apache Maven

## Requirements

Make sure the following are installed:

- JDK 17 or later
- Apache Maven
- An IDE such as Visual Studio Code or IntelliJ IDEA (optional)

## Project Structure

```text
Travel_Booking_System/
├── src/
│   └── travel/
│       └── core/
│           ├── Main.java
│           ├── WebServer.java
│           ├── TravelService.java
│           ├── Json.java
│           ├── TravelBookingGUI.java
│           ├── DBConnection.java
│           ├── TravelPackage.java
│           ├── DomesticPackage.java
│           ├── InternationalPackage.java
│           ├── Traveller.java
│           ├── Booking.java
│           ├── Cancellable.java
│           └── SeatsFullException.java
├── web/            (index.html, styles.css, app.js)
├── run-web.sh / run-web.bat
├── pom.xml
└── README.md
```

## How to Run

1. Clone or download this repository.
2. Open the project folder in your terminal or IDE.
3. Make sure Java 17 and Maven are installed.
4. Compile the project:

   ```bash
   mvn clean compile
   ```

5. Run the application from the project root:

   ```bash
   mvn exec:java -Dexec.mainClass="travel.core.Main"
   ```

If the Maven Exec plugin is not available, configure the Maven Exec plugin or run the compiled application with the SQLite JDBC dependency on the classpath.

## Database

The application uses SQLite, so a separate database server is not required. The `travel_booking.db` database is initialized automatically when the application starts. It is created in the project's working directory if it does not already exist.

## Notes

- Keep the project structure unchanged so that Java package paths resolve correctly.
- Do not commit personal or sensitive database records to a public repository.

## Author

**Bharat Prajapati**

## License

This project is available for learning and educational purposes.
