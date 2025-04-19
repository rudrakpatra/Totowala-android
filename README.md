# Totowala Android App

Totowala is an Android application designed to connect passengers with totowalas (local cab service providers) on a college campus. The app allows passengers to create auctions for rides, where totowalas can bid on the job. This competitive bidding ensures reasonable pricing for both parties. The app aims to replace the traditional phone-based system where passengers often have to call multiple totowalas to find one who is available and offer a more streamlined and cost-effective solution.

[Video Demonstration on Youtube](https://www.youtube.com/watch?v=DsnparWte8A&t=18s)

## Features

- **Passenger/Driver Accounts**: Users can create accounts as either a passenger or a totowala (driver) using their phone numbers. Firebase Authentication is used for secure sign-in via OTP.
  
- **Location Sharing**: Users can share their real-time locations with others to make their interest in a ride or service visible on a shared platform.

- **Auction System**: Passengers can create auctions for rides with details such as from/to places, expected fare, and real-time location. Totowalas can bid on these auctions, and the passenger can choose the best bid based on fare and proximity.

- **Bid Management**: The bidding process is competitive, with fare-based and proximity-based factors influencing the selection of bids. The closest totowalas may charge slightly higher fares due to the reduced travel time.

- **Real-time Location Updates**: Totowalas' locations are updated in real-time, but only those near the passenger are shown to ensure efficient and relevant data for decision-making.

- **Phone Number Exchange**: Once an auction ends, the selected totowala and passenger can exchange phone numbers. This system is aimed at reducing the need for direct phone calls before the ride, ensuring safer communication.

- **Simple UI/UX**: The app is designed with a clean and user-friendly interface, optimized for Android users. The core screens include:
  - Nearby Users: View available totowalas and passengers nearby.
  - Auction: Create and view live auctions.
  - Bid: Place bids on available auctions.
  - OTP Verification: Secure user login via phone number OTP.
  - Settings: Customize user preferences.

- **Firebase Backend**: Firebase Firestore is used for real-time data management, including auctions, bids, and location updates.

- **Safety and Privacy**: While the app allows for phone number exchange, users can also communicate through an in-app chat feature (coming soon), ensuring privacy.

## Setup Instructions

To run the Totowala Android app locally on your machine using Android Studio, follow the steps below:

### Prerequisites

1. **Android Studio**: Make sure you have the latest version of Android Studio installed. You can download it from [here](https://developer.android.com/studio).
2. **Firebase Account**: The app uses Firebase for authentication and data storage. You'll need a Firebase account to configure the app for your environment.
3. **UPI Payment Setup**: (Optional) If you'd like to enable UPI payments for testing purposes, you will need a setup with a UPI-enabled account.

### Steps to Set Up

1. **Clone the Repository**:
   Open your terminal or command prompt and clone the repository to your local machine:
   ```bash
   git clone https://github.com/rudrakpatra/Totowala-android.git

2. **Open the Project in Android Studio**:
   - Launch Android Studio.
   - Select **Open an existing project** and navigate to the cloned repository folder.
   - Open the project folder to load it into Android Studio.

3. **Set Up Firebase**:
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Create a new project (if you don't have one already).
   - Follow the steps to add Firebase to your Android project:
     - Click on **Add Firebase to your Android app**.
     - Download the `google-services.json` file and place it in the `app/` directory of your project.
     - Add Firebase dependencies in your `build.gradle` files (both project-level and app-level):
       ```gradle
       // Project-level build.gradle
       classpath 'com.google.gms:google-services:4.3.3'  // Add this line
       
       // App-level build.gradle
       apply plugin: 'com.google.gms.google-services'  // Add this line at the bottom
       ```

4. **Enable OTP Verification (Phone Authentication)**:
   - In the Firebase console, enable **Phone Authentication** under the **Authentication** section.
   - To use OTP verification in your Android app, you need to add your **SHA fingerprint** to the Firebase console.
     - You can obtain the SHA-1 fingerprint from Android Studio by following these steps:
       1. Open the **Gradle** window on the right side of Android Studio.
       2. Navigate to `Tasks > android > signingReport`.
       3. Run the `signingReport` task, and it will generate the SHA-1 fingerprint.
     - Once you have the SHA-1 fingerprint, go to the Firebase console, select your project, and navigate to **Project settings > General**. 
     - In the **Your apps** section, add the SHA-1 fingerprint to the list of fingerprints.
   - Make sure your Android app is set up to handle phone authentication with OTP verification.

5. **Run the App**:
   - Once the Firebase configuration is complete, sync your project in Android Studio to resolve dependencies.
   - Connect a physical Android device or use the Android Emulator to test the app.
   - Click on the **Run** button in Android Studio to launch the app.

6. **Testing the App**:
   - After successfully running the app, you should be able to sign in via phone number OTP, view nearby totowalas, create auctions, and place bids.
   - You can modify the Firebase Firestore rules as needed for testing purposes, but be sure to implement secure rules for production.


