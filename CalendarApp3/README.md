# CalendarApp — Setup Guide (Mac)

## Step 1 — MySQL Database

Open MySQL Workbench (or Terminal with `mysql -u root -p`) and run:
```sql
source /path/to/CalendarApp/src/main/resources/com/calendarapp/sql/schema.sql
```
Or paste the contents of `schema.sql` and execute.

---

## Step 2 — Set Your DB Password

Open this file in IntelliJ:
```
src/main/java/com/calendarapp/util/DB.java
```
Change line 8:
```java
private static final String PASSWORD = "your_password"; // ← put your MySQL password here
```

---

## Step 3 — Open in IntelliJ

1. `File → Open` → select the `CalendarApp` folder
2. Wait for the Gradle sync popup → click **"Load Gradle Project"**
3. Wait for Gradle to download all dependencies (JavaFX, MySQL driver, BCrypt)

---

## Step 4 — Run the App (TWO OPTIONS)

### ✅ Option A — Gradle panel (RECOMMENDED, always works)
1. Open the **Gradle** panel on the right side of IntelliJ
2. `CalendarApp → Tasks → application → run`
3. Double-click **run**

### Option B — IntelliJ Run button
1. `Run → Edit Configurations → + → Application`
2. **Name:** CalendarApp
3. **Main class:** `com.calendarapp.Main`
4. **Module:** `CalendarApp.main`
5. Click **OK** → press the green ▶ button

---

## Step 5 — Log In

| Field    | Value      |
|----------|------------|
| Username | `admin`    |
| Password | `admin123` |

Or click **Create an account** to register.

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `Communications link failure` | Start MySQL — open MySQL Workbench and connect, or run `brew services start mysql` in Terminal |
| `Access denied for user 'root'` | Wrong password in `DB.java` |
| `Unknown database 'calendar_app'` | Run `schema.sql` first |
| `Gradle sync failed` | `File → Invalidate Caches → Invalidate and Restart` |
| JavaFX error with run button | Use the Gradle panel `run` task instead |
| `Table doesn't exist` | Re-run the full `schema.sql` |
