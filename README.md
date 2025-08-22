# Weight Tracker App  

## 📱 Overview  
The **Weight Tracker App** is a mobile application developed for **CS-360: Mobile Architect & Programming** at Southern New Hampshire University.  

The app allows users to:  
- Create and log in with a secure username and password  
- Add, edit, and delete daily weight entries  
- Set a personal weight goal  
- View how close they are to reaching their goal  
- Toggle between **kg** and **lbs** units  
- (Optional) Receive SMS notifications when their goal weight is achieved  

This project demonstrates Android development fundamentals, SQLite database persistence, permissions handling, and user-centered UI design.  

---

## ⚙️ Features  

- **Login & Account Creation**  
  - Stores user credentials in SQLite database  
  - Supports first-time account creation and persistent login sessions  

- **Weight Tracking**  
  - Add daily weight with a date stamp  
  - Edit or delete entries  
  - View all data in a **RecyclerView grid list**  

- **Goal Management**  
  - Users can set and update their personal weight goal  
  - Progress is displayed dynamically (e.g., “You are 5 lbs away from your goal”)  

- **Unit Toggle (kg/lbs)**  
  - Default units depend on locale (US defaults to lbs; others default to kg)  
  - Toggle switch allows seamless conversion between units  

- **SMS Notifications (Optional)**  
  - Requests runtime permission for SMS  
  - Sends a notification if the latest entry meets or falls below the user’s goal  
  - Application still functions normally if SMS is denied  

---

## 🗄️ Technical Implementation  

- **Language:** Java  
- **IDE:** Android Studio  
- **Database:** SQLite (via `AppDatabaseHelper`)  
- **UI Components:**  
  - `RecyclerView` with `WeightAdapter` for displaying weights  
  - `SwitchCompat` for unit toggle  
  - `EditText` + `Button` controls for weight/goal entry  
- **Persistence:**  
  - `SharedPreferences` used for unit preference and weight goal  
  - SQLite used for all user and weight data  

---

## 📲 Permissions  

The app requests the following Android permission:  
- `SEND_SMS` – optional, used only if the user enables goal notifications.  
- No unnecessary permissions are requested.  

---

## 🖼️ App Icon  

The app uses a custom **purple background icon** with centered **WT** letters to represent "Weight Tracker."  
This design is simple, calming, and clear in app listings.  

---

## 📦 Installation  

1. Clone or download this repository  
2. Open the project in **Android Studio**  
3. Build the project with **Gradle**  
4. Run on an emulator (Pixel 6, API 34 recommended) or a physical Android device  

---

## 🚀 Future Improvements  

- Cloud sync for data backup across devices  
- Advanced statistics and charts (BMI, weekly averages, trends)  
- Dark mode for UI  
- Push notifications instead of SMS  

---

## 📝 Author  

Developed by **Emmalie Cole** for CS-360 (SNHU).  
