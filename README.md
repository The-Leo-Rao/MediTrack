# MediTrack

## Problem Statement

**Problem Statement Chosen:** #26ENMT2: Problem Statement 2 -> MediTrack

---

# About the Project

**MediTrack** is an Android application designed to help users manage their personal health information in one place. It allows users to securely record medical information, monitor vital signs through graphs, manage medication reminders, generate professional medical reports in PDF format, and quickly share emergency information including their live location and latest medical report with an emergency contact.

The application aims to make personal healthcare management simple, organized, and accessible during both everyday use and emergencies.

---

# Tech Stack

### Frontend

* Kotlin
* Jetpack Compose
* Material 3

### Backend

* Firebase Authentication
* Firebase Firestore

### Database

* SQLite (local medical records)
* Firebase Firestore (cloud user data)

### Libraries & Tools

* Android PdfDocument (PDF generation)
* MPAndroidChart (Vital statistics graphs)
* Coil (Image loading)
* Google Play Services Location API
* Android Studio
* Gradle
* Git & GitHub

---

# Features

* User Authentication (Firebase)
* Medical record management
* Medication reminders
* Vital statistics tracking
* Interactive health graphs
* Emergency SOS feature
* Share current location with emergency contact
* Generate professional medical reports in PDF format
* Store personal health information
* Image attachment support for records
* Local SQLite storage
* Cloud synchronization using Firebase
* Modern Material Design UI
* Clinical classification & alerting
* Spike-preserving graphs (min/max decimation downsampling)

---

# How to Run the Project

## Prerequisites

* Android Studio (latest version recommended)
* Android SDK 24+
* Gradle
* Internet connection (for Firebase)

## Installation

1. Clone the repository

```bash
git clone https://github.com/The-Leo-Rao/MediTrack
```

2. Open the project in Android Studio.

3. Allow Gradle to sync.

4. Add your own `google-services.json` file inside:

```
app/google-services.json
```

5. Build the project.

6. Run the application on an Android device or emulator.

## Demo & Testing

Both demo tools live in the same place:

**Profile screen → tap the ⓘ (info) button in the top-right corner → "App Info" dialog**

The dialog contains two **independent** buttons. You can use either one on its own, or both together.

---

### Demo Login

You can sign in with the pre-seeded demo account (no sign-up needed):

| Field    | Value                 |
|----------|-----------------------|
| Email    | `example@meditrack.com` |
| Password | `devfusion3.0`        |

> [!NOTE]
> The app opens on the login screen. Use the credentials above to sign in, or create
> a new account via **New to MediTrack?** on the login screen.

---

### 1. Seed Test Data (historical sample data)

**Button:** `Seed Test Data`

Populates the app with ~30 days of realistic **historical** data so the graphs, reports,
and records screens have something to show immediately:

- Sample readings for every vital (heart rate, SpO₂, blood pressure, temperature,
  blood sugar, weight) spread across the last 30 days
- Example medical records (doctor's note, prescription, symptom, follow-up)
- Example medication reminders

**How to use:** open the App Info dialog and tap **Seed Test Data**. Then go to the
**Vitals** screen - each vital card and its detail graph (1D / 1W / 1M) will now show
backfilled history. Use this to demo the **History graphs** and **PDF report**.

> [!WARNING]
> Seeding **replaces** existing vitals data and re-adds the demo records/reminders,
> so tapping it repeatedly will create duplicate records/reminders. Use it once for a clean demo.

---

### 2. Live Simulated Sensor (real-time feed)

**Button:** `Toggle live server`

Starts (or stops) a **simulated real-time vitals stream** that mimics a live bedside
sensor. This is separate from the historical seed above - it generates *new* readings
continuously while it's running.

When the live feed is **on**:
- The Vitals header shows **"Sensor Live · N readings recorded"**
- Vital cards update their numbers in real time and animate a live sparkline
- Readings are recorded to history as they stream in

When **off**, the header shows **"Sensor Offline"** and the last recorded values are kept.

**How to use:** open the App Info dialog and tap **Toggle live server** to start the feed,
then open the **Vitals** screen to watch values update live. Tap **Toggle live server**
again to stop it. Use this to demo **live monitoring** and the **abnormal-episode alerts**.

> [!WARNING]
> The live feed comes from an in-app simulator (`SimulatedVitalSource`), not a network
> server - the architecture is BLE-ready, so a real sensor can be swapped in without
> changing the rest of the app.

---

### SOS Feature - Required Permission

The SOS emergency feature requires a restricted permission that must be manually enabled on the device. After installing the app:

1. Open **Settings**
2. Go to **Apps** → **MediTrack**
3. Tap the **three-dot menu** (⋮) in the top right
4. Select **Allow restricted settings**

Without this step, the SOS button will not be able to send SMS messages or access location for the emergency contact.

---

# Google Drive Link

APK file:

**[Drive Link](https://drive.google.com/file/d/1lIJ50rXZf5mBwQ4kwImwC1yQxSIoi4qG/view?usp=sharing)**
> Demo login — Email: `example@meditrack.com` · Password: `devfusion3.0`

---

# Team Members


| Name                | Role                                                                                                                  |
|---------------------|-----------------------------------------------------------------------------------------------------------------------|
| **Abhay Rao**       | UI/UX Design, Android Application Development, Firebase Firestore Integration                                         |
| **Vansh Chaudhary** | Vital Statistics Module, Health Graph Visualization, Live Backend/API Integration, Data Seeding & Dataset Preparation |


---

# Known Bugs / Limitations

* Emergency messaging depends on device permissions and location availability.
* SQLite data is stored locally and is not automatically backed up unless synced through Firebase.
* The application currently supports Android devices only.

---

# Future Improvements

* Cloud backup for all medical records
* WearOS and sensor integration
* Doctor portal
* AI-powered health insights
* Medicine interaction warnings
* Multi-language support

---

