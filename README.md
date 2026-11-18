# Travel Booking System — SQLite edition

## Requirements
- JDK 17+
- Apache Maven (recommended)
- VS Code Java Extension Pack (optional)

## Run with Maven
Open this folder in VS Code, open Terminal at this folder, then run:
    mvn clean compile
    mvn exec:java -Dexec.mainClass="travel.core.Main"

If Maven's exec plugin is not available, run:
    mvn dependency:copy-dependencies
    javac -cp "target/dependency/*" -d out src/travel/core/*.java
    java -cp "out;target/dependency/*" travel.core.Main

The SQLite database file `travel_booking.db` is created automatically in the project working directory on first run. Tables and sample packages are also created automatically.

## Features
- Tour package listing
- Traveller registration (shows generated Traveller ID)
- Package booking (synchronized method + JDBC transaction)
- Booking cancellation (90% refund, 10% cancellation charge)
- Booking report

No MySQL server or MySQL Connector is needed. Maven downloads the SQLite JDBC driver automatically.
"# Travel-Booking-System" 
