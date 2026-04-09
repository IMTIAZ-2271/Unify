#  Unify

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-FF0000?style=for-the-badge&logo=java&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)

> Unify is an all-in-one digital platform designed to simplify and centralize everyday university activities for students, faculty, and staff. By integrating academic, administrative, and lifestyle services into one unified system, Unify enhances convenience, improves operational efficiency, and delivers a smoother, more connected campus experience.

[![GitHub Repository](https://img.shields.io/badge/GitHub-View_Repository-blue?logo=github)](https://github.com/IMTIAZ-2271/Unify.git)

---

## 🎥 Project Demo

Check out the video demonstration of Unify in action:
**[Watch the Demo Video](https://drive.google.com/file/d/1di9_R_ByixBbL0oI7GVUsiOgYI8SSvDT/view?usp=sharing)**

---

## ✨ Key Features

* **📢 Centralized Club Announcements:** Stay up to date with events, notices, and activities from all university clubs in one convenient place.
* **🍔 Canteen Integration:** Browse daily menus, place orders, and purchase food directly through the application.
* **📚 Library Management:** Seamlessly search for books, manage your borrowed items, and track return dates.
* **🚌 Transportation Booking:** Check live bus schedules and easily book tickets for university transport.

---

## 🛠️ Tech Stack

* **Language/Framework:** Java, JavaFX
* **Database:** MySQL
* **Build Tool:** Gradle

---

## 📋 Prerequisites

Before installing, ensure you have the following installed on your system:
* **[Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/)**
* **[MySQL Server](https://dev.mysql.com/downloads/)** (installed and running)
* **Git** (optional, for cloning the repository)

---

## ⚙️ Installation

Follow these steps to get your development environment set up:

**1. Clone the repository**
Open your terminal and clone the project to your local machine:
```bash
git clone [https://github.com/IMTIAZ-2271/Unify.git](https://github.com/IMTIAZ-2271/Unify.git)
cd Unify
```

**2. Setup the Database**
* Locate the provided `.sql` database file included in the repository.
* Open your MySQL client (e.g., MySQL Workbench, phpMyAdmin, or terminal) and import the `schema.sql` file to create the necessary tables and schema.

**3. Configure Database Credentials**
* Navigate to the database configuration file located at: 
  `src/main/java/com/Unify/util/DB.java`
* Open the file in your text editor and update the connection details to match your local MySQL setup:
  ```java
    private static final String HOST = "Your_Host";
    private static final String PORT = "Your_Port";
    private static final String DATABASE = "Your_Database_name";
    private static final String USER = "Your_Username";
    private static final String PASSWORD = "Your_Password";
  ```

---

## 🚀 Running the Application

Once installed and configured, you can launch the application using the included scripts or manually via the terminal.

### Option A: Using Quick Scripts (Recommended)

**For Windows:**
Simply locate and double-click the batch file in the root directory:
* `RunAppWindows.bat`

**For Mac:**
Double-click the command script, or execute it directly from your terminal:
* `RunAppMAC.command`

### Option B: Manual Terminal Launch

If you prefer to run the project manually from the command line, ensure you are in the root directory (`Unify/`) and execute the Gradle wrapper:

**Windows:**
```bash
.\gradlew run
```

**Mac / Linux:**
```bash
./gradlew run
```
