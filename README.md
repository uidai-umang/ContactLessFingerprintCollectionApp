# Contactless Registration App

This project is a demonstration application for a proprietary **Fingerprint Capturing SDK**. It serves as a wrapper or a sample implementation to showcase the SDK's capabilities in a real-world user registration scenario.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Core Components](#core-components)
  - [Finger SDK Manager](#fingersdkmanager)
  - [Embedding Generation](#fingerembedder)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation & Setup](#installation--setup)

---

## Overview

The application allows for the registration of users by capturing the following information:
*   Username
*   Phone Number
*   Fingerprints (captured via the SDK)

Upon capturing the fingerprints, the app generates corresponding embeddings and stores all user information, including the embeddings, in a local **Room database**.

## Features

- **User Registration:** Simple UI for entering username and phone number.
- **SDK Integration:** Seamlessly invokes the fingerprint capturing SDK.
- **Fingerprint Embedding:** Generates a digital representation (embedding) of the captured fingerprint.
- **Local Storage:** Persists user data and embeddings locally using Android's Room Persistence Library.

## Architecture

The application is designed with a clean, modular architecture to separate concerns and improve maintainability.

- **`:app` Module:** This is the main application module that contains the UI (Activities/Fragments), ViewModels, and the core business logic for user registration. It is also responsible for integrating and invoking the SDK.

- **`:embedding` Module:** This module is solely responsible for the generation of fingerprint embeddings. It exposes an interface for creating embeddings, allowing the implementation to be swapped easily without affecting the rest of the application.

## Core Components

### [`FingerSDKManager`](app/src/main/java/app/gov/uidai/contactlessregistration/usecase/FingerSDKManager.kt)

1.  **Launching the SDK:** It handles the logic for initializing and launching the Fingerprint Capturing SDK with all the required input parameters.
2.  **Parsing the Response:** It is responsible for receiving and parsing the data returned by the SDK upon successful fingerprint capture.

### [`FingerEmbedder`](embedding/src/main/java/in/gov/uidai/embedding/FingerEmbedder.kt)

The generation of embeddings is handled by the `:embedding` module. An interface is defined to abstract the process of creating an embedding from the raw fingerprint data.

- **[`DemoFingerEmbedderImpl`](embedding/src/main/java/in/gov/uidai/embedding/DemoFingerEmbedderImpl.kt)**:
  > **IMPORTANT:** Currently, the project uses a **dummy implementation** of this interface. This means it does not perform any real image processing or feature extraction. It returns a placeholder value and is intended for demonstration and testing purposes only. For a production environment, this dummy implementation should be replaced with a concrete implementation that uses a real embedding generation library.

## Getting Started

Follow these steps to get the project up and running on your local machine.

### Prerequisites

- Android Studio (latest stable version recommended)
- An Android device
- The proprietary [**Fingerprint Capturing SDK**](https://github.com/mobilearchitect2023/ContactlessSDKForSITAProgram) App/Library File.

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/mobilearchitect2023/ContactlessRegDemoAppForSITAProgram.git
    ```

2.  **Open in Android Studio:**
    Open the cloned project directory in Android Studio.

3.  **Build and Run:**
    Build the project and run it on your connected Android device.