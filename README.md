# Unify

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-FF0000?style=for-the-badge&logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)

> Unify is an all-in-one digital platform designed to simplify and centralize everyday university activities for students, faculty, and staff. By integrating academic, administrative, and lifestyle services into one unified system, Unify enhances convenience, improves operational efficiency, and delivers a smoother, more connected campus experience.

## 🎥 Project Demo

Check out the video demonstration of Unify in action:
**[Watch the Demo Video](https://drive.google.com/file/d/1di9_R_ByixBbL0oI7GVUsiOgYI8SSvDT/view?usp=sharing)**


## 🚀 Features

* **📢 Centralized Club Announcements:** A unified hub for all university clubs, making it easy for students to stay informed about extracurricular activities, meetings, and upcoming events.
* **🍔 Canteen Integration:** Skip the line! Seamlessly order and purchase food directly from campus canteens.
* **📚 Library Management:** Easily manage your academic resources, including borrowing, tracking, and returning library books.
* **🚌 Transportation Booking:** Get information for campus transportation.

## 🛠️ Tech Stack

* **Language:** Java
* **Framework:** JavaFX 
* **Build Tool:** Gradle (Kotlin DSL)
* **Database:** MySQL

## ⚙️ Prerequisites

Before you begin, ensure you have met the following requirements:
* You have installed **Java Development Kit (JDK) 17** (or your specific version).
* You have installed **MySQL Server** and have it running locally.
* You have an IDE set up (IntelliJ IDEA is recommended).

## 💻 Installation and Setup

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/IMTIAZ-2271/Unify.git](https://github.com/IMTIAZ-2271/Unify.git)
Navigate to the project directory:

Bash
cd Unify
Set up the Database:

Open your MySQL client.

Run the SQL scripts located in [insert path to your .sql files, e.g., src/main/resources/database.sql] to create the necessary tables.

Update the database connection credentials (username, password, URL) in your project's configuration file: [insert path, e.g., src/main/resources/application.properties or directly in your DB connection class].

Build the project:
This project uses the Gradle wrapper, so you don't need Gradle installed globally.

On Windows:

```bash
gradlew.bat build
```
On Mac/Linux:

```bash
./gradlew build
```
▶️ Running the Application
To run the application locally, use the following Gradle task:

On Windows:

```bash
gradlew.bat run
```
On Mac/Linux:

```bash
./gradlew run
```
