# Unify

An all-in-one digital platform designed to streamline and centralize university activities for students and faculty.

## Features
* **Centralized Club Announcements:** Stay up to date with events, notices, and activities from all university clubs in one place.
* **Canteen Integration:** Browse menus, order, and purchase food directly through the application.
* **Library Management:** Seamlessly search for books, manage borrowing, and track return dates.
* **Transportation Booking:** Check bus schedules and easily book tickets for university transport.


## How to Run the Project

### Prerequisites
1. **Java Development Kit (JDK)** installed.
2. **MySQL** installed and running.
3. Please ensure you have **imported** the provided database `.sql` file
4. Updated the database credentials in the ...\Unify\src\main\java\com\Unify\util\DB.java.(Your DATABASE name, USER name, PASSWORD etc.)

### Launching the Application

**For Windows:**
Simply double-click the included batch file:
* `RunApp.bat`

**For Mac / Linux:**
Double-click the included command script, or run it via terminal:
* `RunApp.command` (or `.sh` equivalent)

**Manual Terminal Launch (Any OS):**
If you prefer to run the project manually from the command line, open a terminal in the root project directory and execute:

#### Windows
```bash
.\gradlew run
```


#### Mac / Linux
```bash
./gradlew run
```