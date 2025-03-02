SmartVote is a secure and efficient voting application that integrates Aadhaar-based authentication, OTP verification, and face authentication to ensure a transparent and tamper-proof voting process.

Features

Aadhaar-Based Authentication: Users verify their identity using their Aadhaar number.

OTP Verification: A one-time password (OTP) is sent to the registered mobile number for authentication.

Face Authentication: Utilizes the Face++ API for facial verification to prevent impersonation.

Data Binding: Ensures seamless UI updates and efficient data handling.

Technologies Used

Android Studio (Kotlin)

Firebase Firestore (Database for user authentication)

Face++ API (Facial recognition and liveness detection)

OTP Authentication (Mobile number-based verification)

Installation

Clone the repository:

git clone https://github.com/your-username/SmartVote.git

Open the project in Android Studio.

Configure Firebase:

Add the google-services.json file to the app directory.

Enable Firestore Database and Authentication (Phone OTP verification).

Set up the Face++ API:

Obtain API keys from Face++.

Add them to the appropriate configuration file.

Run the application on an Android device or emulator.

Usage

Register/Login using an Aadhaar number.

Verify identity using OTP authentication.

Perform face authentication for additional security.

Cast your vote securely.
