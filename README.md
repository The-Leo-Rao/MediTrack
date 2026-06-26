# MediTrack

## Problem Statement

**Problem Statement Chosen:** #26ENMT2: Problem Statement 2 -> MediTrack

---

# About the Project

**MediTrack** is an Android application designed to help users manage their personal health information in one place. It allows users to securely record medical information, monitor vital signs through graphs, manage medication reminders, generate professional medical reports in PDF format, and quickly share emergency information—including their live location and latest medical report—with an emergency contact.

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

### Seeding Example Data

To populate the app with example health data for testing or demonstration purposes, navigate to:

**Profile → Info tab**

There you will find buttons to:
- **Seed example data** — populates the app with sample vitals, records, and reminders
- **Simulate live server** — starts a simulated live vitals feed, mimicking a real-time sensor stream on the vitals screen

### SOS Feature — Required Permission

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

