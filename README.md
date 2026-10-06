# Item Reminder

An Android app for keeping track of everyday items, where they were last seen, and when they need attention. Item Reminder combines item organization with photos, optional location capture, reminders, favorites, categories, archiving, and Firebase-backed sharing.

> Built in Java for Android as a mobile application project. This repository has been cleaned for public portfolio use; Firebase project credentials/configuration are intentionally not committed.

## Why I Built It

It is easy to remember *what* you own and still forget *where* you left it or *when* you need it. I built Item Reminder around that practical problem: save an item with useful context, then surface it later through searchable metadata and notifications.

## Features

- Email/password authentication with Firebase Authentication
- Cloud-backed item storage with Cloud Firestore
- Create, edit, view, archive, favorite, and delete items
- Categorize items as School, Electronics, Clothing, Personal, Sports, or Other
- Attach a photo from the Android camera workflow
- Optionally capture the item's last-known location
- Schedule Android reminder notifications with `AlarmManager`
- Share items with other registered users
- Owner/shared-reader authorization enforced by Firestore security rules
- RecyclerView-based home screen for browsing saved items
- Defensive model normalization for Firestore-loaded data

## Demo

A short demo GIF will be added here. See [`docs/demo`](docs/demo/README.md) for the capture plan.

<!-- After recording, uncomment:
![Item Reminder demo](docs/demo/item-reminder-demo.gif)
-->

## Screenshots

Screenshot placeholders and recommended filenames are documented in [`docs/screenshots`](docs/screenshots/README.md).

<!-- After adding screenshots, uncomment:
| Home | Add Item | Item Details | Reminder |
| --- | --- | --- | --- |
| ![](docs/screenshots/home.png) | ![](docs/screenshots/add-item.png) | ![](docs/screenshots/item-details.png) | ![](docs/screenshots/reminder.png) |
-->

## Technical Highlights

### Firebase-backed user data

The app uses Firebase Authentication to identify users and Cloud Firestore to persist items and lightweight user profiles. Each item records an owner UID and may contain a list of shared-reader UIDs.

The included [`firestore.rules`](firestore.rules) enforce the important authorization boundary server-side:

- only the signed-in owner can create/update/delete an item;
- shared users can read an item;
- an update cannot silently transfer ownership to another UID.

### Reminder scheduling

`ReminderScheduler` creates a stable `PendingIntent` per item and schedules an `AlarmManager` event. `ReminderReceiver` receives the broadcast and posts the user-facing notification.

### Photo and location context

`AddItemActivity` supports camera capture through a `FileProvider` and can attach a last-known device location when the user grants location permission. Location is optional; the model tracks whether coordinates are actually present.

### Firestore-safe model defaults

The `Item` model normalizes nullable fields loaded from Firestore and provides small ownership/sharing helper methods. Unit tests cover these invariants in `app/src/test`.

## Architecture

```text
MainActivity
   └── authentication / entry flow
        └── HomeActivity
             ├── ItemAdapter + RecyclerView
             ├── AddItemActivity
             ├── DetailActivity
             └── EditItemActivity

FirebaseUtils
   ├── FirebaseAuth
   └── Cloud Firestore

ReminderScheduler
   └── AlarmManager
        └── ReminderReceiver
             └── Android notification
```

## Tech Stack

- Java 11
- Android SDK
- Firebase Authentication
- Cloud Firestore
- Firebase security rules
- RecyclerView / CardView
- Glide
- AlarmManager + notifications
- Android `FileProvider`
- JUnit / AndroidX Test

## Project Structure

```text
app/src/main/java/com/example/itemreminderapp/
├── MainActivity.java
├── HomeActivity.java
├── AddItemActivity.java
├── EditItemActivity.java
├── DetailActivity.java
├── Item.java
├── ItemAdapter.java
├── FirebaseUtils.java
├── ReminderScheduler.java
└── ReminderReceiver.java

firestore.rules
docs/
├── demo/
└── screenshots/
```

## Run Locally

### 1. Requirements

- Android Studio with a recent Android SDK
- JDK 11+
- A Firebase project
- Android emulator or device

### 2. Configure Firebase

1. Create a Firebase project in the Firebase console.
2. Add an Android app with package name `com.example.itemreminderapp`.
3. Download `google-services.json`.
4. Place it at:

```text
app/google-services.json
```

That file is intentionally ignored by Git.

5. Enable **Email/Password** under Firebase Authentication.
6. Create a Cloud Firestore database.
7. Review and deploy the repository's [`firestore.rules`](firestore.rules) to your Firebase project.

### 3. Build and run

Open the project in Android Studio, allow Gradle sync to finish, select a device/emulator, and run the `app` configuration.

From a terminal, once the Android/Firebase environment is configured:

```bash
./gradlew test
./gradlew assembleDebug
```

## Tests

The repository includes unit tests for the `Item` model's defaults, Firestore normalization behavior, and owner/shared-user helpers, plus a basic Android instrumentation package check.

```bash
./gradlew test
./gradlew connectedAndroidTest
```

The second command requires a running emulator or connected Android device.

## Privacy / Security Notes

- `google-services.json` is excluded from the repository so forks use their own Firebase project.
- Do not commit service-account keys, private keys, test-user passwords, or exported production data.
- Demo screenshots should use fake accounts and should not expose a real precise location.

## Future Improvements

- Migrate reminder scheduling to a more resilient scheduling strategy where appropriate
- Add UI tests for create/edit/share flows
- Add search and richer sorting/filtering
- Improve offline-state/error handling
- Move additional data logic out of Activities into a repository/view-model layer
- Add accessible empty/error/loading states

## What This Project Demonstrates

This project gave me experience building a complete Android workflow that crosses UI, local device capabilities, cloud authentication/database services, authorization rules, and notification scheduling rather than treating each as an isolated exercise.
