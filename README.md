# 🛡️ SecureScan — Enterprise Mobile Threat Intelligence & SOC

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-00B4D8?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![API](https://img.shields.io/badge/API-VirusTotal%20v3-394EFF?style=for-the-badge)](https://developers.virustotal.com/reference/overview)
[![DI](https://img.shields.io/badge/DI-Dagger%20Hilt-FF6F00?style=for-the-badge)](https://dagger.dev/hilt/)
[![Status](https://img.shields.io/badge/Status-Enterprise--Ready-00E676?style=for-the-badge)](#)

**SecureScan** is a professional, enterprise-grade cybersecurity platform for Android. Engineered as a portable **Security Operations Center (SOC)**, it delivers real-time threat intelligence by indexing suspicious URLs and sandboxing local files through the **VirusTotal v3 API**.

---

## 📑 Table of Contents

1. [Core Capabilities](#-core-capabilities)
2. [System Architecture & Data Flow](#-system-architecture--data-flow)
3. [Project Structure](#-project-structure)
4. [Tech Stack & Tools](#-tech-stack--tools)
5. [Dynamic Verdict Engine & Risk Scoring](#-dynamic-verdict-engine--risk-scoring)
6. [UI/UX: The "CyberShield" Design Philosophy](#-uiux-the-cybershield-design-philosophy)
7. [Engineering Challenges & Solutions](#-engineering-challenges--solutions)
8. [Getting Started & Installation](#-getting-started--installation)
9. [Future Roadmap](#-future-roadmap)
10. [Author & Academic Context](#-author--academic-context)

---

## 🎯 Core Capabilities

* **Multi-Vector Threat Indexing:** High-speed reputation analysis of URLs/domains alongside cloud sandboxing and static/heuristic analysis of local files (`APK`, `PDF`, `ZIP`, etc.).
* **Automated Intelligence Polling:** Non-blocking coroutine-based polling engine that tracks `in-progress` cloud sandbox tasks until a final `"completed"` verdict is returned.
* **Dynamic Threat Reporting:** Adaptive intelligence reports featuring a Verdict Engine (`VERIFIED SAFE`, `SUSPICIOUS`, `MALICIOUS`), actionable **Evidence Items** (e.g., *"5 security vendors detected phishing"*), and a searchable breakdown of 90+ antivirus engines.
* **SOC Command Center Dashboard:** Real-time visibility into aggregate security posture via a normalized **0–100 Core Security Score**, API health telemetry, and a weekly threat forecast chart.
* **Forensic Logs & Storage Lifecycle Management:** Persistent Room (v2) database tracking scan metadata, file sizes, and internal file paths—complete with secure deletion that purges both database records and physical cached files.

---

## 🏛️ System Architecture & Data Flow

SecureScan follows **Clean Architecture** and **MVVM (Model-View-ViewModel)** principles to enforce strict separation of concerns, configuration-change resilience, and maintainability.

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                            │
│  ┌─────────────────────┐   Observes StateFlow   ┌────────────────────┐  │
│  │  Fragments & Views  │ ◄───────────────────── │     ViewModels     │  │
│  │    (ViewBinding)    │ ─────────────────────► │ (Coroutine Scopes) │  │
│  └─────────────────────┘     User Actions       └─────────┬──────────┘  │
└───────────────────────────────────────────────────────────┼─────────────┘
                                                            │
                                                            ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN LAYER                               │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │               ScanRepository (Single Source of Truth)             │  │
│  │   • Arbitrates Remote API vs. Local Room DB   • File Hygiene      │  │
│  └─────────────────────────────────┬─────────────────────────────────┘  │
└────────────────────────────────────┼────────────────────────────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│        REMOTE DATA LAYER        │     │        LOCAL DATA LAYER         │
│  • Retrofit & OkHttp Client     │     │  • Room Database (v2)           │
│  • Auth Header Interceptor      │     │  • ScanEntity (Indexed Queries) │
│  • VirusTotal v3 REST API       │     │  • Internal Cache File Storage  │
└─────────────────────────────────┘     └─────────────────────────────────┘
```

### Unidirectional Data Flow (UDF)
1. **DTO Ingestion:** Retrofit parses complex JSON responses from VirusTotal v3 into `AnalysisStats` (aggregate malicious, suspicious, and harmless counts) and `EngineResult` (individual vendor verdicts).
2. **Domain Mapping:** ViewModels extract normalized metrics and compute risk tiers from the raw DTOs.
3. **Local Persistence:** Processed intelligence is mapped to `ScanEntity` (storing `localFilePath`, `fileSize`, and indexed `maliciousCount`) and persisted in Room for offline forensic auditing.

---

## 📂 Project Structure

```text
app/src/main/java/com/example/securescan/
├── data/
│   ├── local/               # Room Database (v2), DAO, and ScanEntity models
│   ├── remote/              # Retrofit API interfaces, OkHttp interceptors, and DTOs
│   └── repository/          # ScanRepository (Single Source of Truth & file deletion logic)
├── di/                      # Dagger Hilt modules providing Network & Database singletons
├── ui/
│   ├── splash/              # Entrance animation & initialization screen
│   ├── dashboard/           # Main SOC Command Center, Security Score & weekly forecast chart
│   ├── urlscanner/          # URL submission interface with recursive pulse animations
│   ├── filescanner/         # Local file picker & multi-part upload interface
│   ├── results/             # Dynamic Verdict Engine, Evidence builder & vendor filter list
│   └── history/             # Consolidated forensic logs & storage utilization management
├── util/                    # Multi-part file processing utilities, risk calculators & constants
└── MainActivity.kt          # Jetpack Navigation host & dynamic bottom navigation controller
```

---

## 🛠️ Tech Stack & Tools

| Component | Technology | Role in SecureScan |
| :--- | :--- | :--- |
| **Language** | Kotlin | Core application development and type-safe concurrency |
| **Architecture** | MVVM + Clean Architecture | Separation of UI, business logic, and data sources |
| **Dependency Injection** | Dagger Hilt (`@Inject`) | Singleton lifecycle management across `ScanRepository` and ViewModels |
| **Networking** | Retrofit 2 & OkHttp 4 | Custom `x-apikey` header injection and debug `HttpLoggingInterceptor` |
| **Local Persistence** | Jetpack Room (v2) | SQLite persistence with indexed `maliciousCount` for fast threat stats |
| **Navigation** | Jetpack Navigation Component | Deterministic back-stack with Safe Args and dynamic UI chrome control |
| **Asynchronous Engine** | Kotlin Coroutines & Flow | Non-blocking cloud sandbox polling and reactive UI telemetry updates |
| **UI Toolkit** | XML, ViewBinding, CoordinatorLayout | Hardware-accelerated 60 FPS layouts and custom property animations |

---

## 📊 Dynamic Verdict Engine & Risk Scoring

Rather than displaying static text, the Intelligence Report screen dynamically rebuilds its layout based on a tiered **0–100 Risk Scoring Engine**:

| Score Range | Verdict Badge | Threat Level | Classification Logic |
| :---: | :--- | :--- | :--- |
| **81 – 100** | `VERIFIED SAFE` | **Safe** | `0` malicious detections across participating security engines |
| **61 – 80** | `LOW RISK` | **Low Risk** | Clean verdict with high reputation or limited engine coverage |
| **41 – 60** | `SUSPICIOUS` | **Suspicious** | `1 – 2` malicious or heuristic vendor flags triggered |
| **0 – 20** | `MALICIOUS` | **Critical Alert** | `> 5` malicious detections confirmed across security vendors |

### Key Report Capabilities
* **Dynamic Evidence List:** Populates a `LinearLayout` of security checks explaining the verdict (e.g., *"High security risk detected"* vs. *"No blacklist matches"*).
* **Vendor Transparency Filter:** Searchable `RecyclerView` allowing analysts to verify verdicts from specific security vendors (e.g., Kaspersky, Microsoft, Google Safe Browsing).

---

## 🎨 UI/UX: The "CyberShield" Design Philosophy

SecureScan replaces standard mobile UI patterns with a high-contrast industrial **Command Center** interface:

* **Midnight & Neon Palette:** Built on a deep **Midnight Blue (`#050B18`)** canvas paired with functional semantic accents—**Neon Blue** for active scanning telemetry, **Emerald Green** for verified safety, **Amber** for warnings, and **Crimson** for critical threats.
* **Glassmorphism & Depth:** Semi-transparent cards (`#0DFFFFFF` — 5% white alpha) layered over a subtle technical grid background, accented with left-aligned color status bars on forensic history cards.
* **Kinetic Feedback:**
  * **Splash Entrance:** Scaled logo transition powered by `AccelerateDecelerateInterpolator`.
  * **Active Pulse (`vPulse`):** Recursive pulse animations on URL and File scanners signaling live background threat indexing.
* **Maximized Report Canvas:** A custom `OnDestinationChangedListener` in `MainActivity` automatically hides the Bottom Navigation bar when viewing detailed intelligence reports.

---

## 🔍 Engineering Challenges & Solutions

1. **White Header Flicker on Scroll:**
   * *Issue:* Scrolling the `CoordinatorLayout` on the SOC Dashboard caused a white status-bar flicker that broke the dark aesthetic.
   * *Solution:* Configured `android:fitsSystemWindows="true"` and set the `AppBarLayout` background to `@android:color/transparent`, extending the dark grid canvas seamlessly edge-to-edge.
2. **Orphaned Cached Files on Log Deletion:**
   * *Issue:* Deleting a scan record from Room removed database metadata while leaving uploaded files stranded in internal storage.
   * *Solution:* Updated `ScanRepository.deleteScan()` to verify `localFilePath` and invoke `java.io.File.delete()` prior to completing the database transaction.
3. **Perceived Latency During Cloud Sandboxing:**
   * *Issue:* Deep file analyses can take over 2 minutes, risking user confusion during polling.
   * *Solution:* Exposed a reactive `scanStatus` `Flow` in the ViewModel that dynamically updates UI progress stages (*"Cloud sandboxing..."* → *"Deep heuristics..."*) based on elapsed time.

---

## 🚀 Getting Started & Installation

### Prerequisites
* **Android Studio:** Koala / Ladybug or newer (JDK 17+)
* **Android SDK:** Min SDK 24 (Android 7.0) | Target SDK 34+
* **VirusTotal API Key:** Obtain a free or enterprise API key from [VirusTotal](https://www.virustotal.com/)

### Setup Instructions

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/OthmaneKhachini/Secure-Scan.git
   cd Secure-Scan
   ```

2. **Configure Your API Key:**
   Open `local.properties` in the project root (automatically excluded from Git via `.gitignore`) and add your VirusTotal v3 API key:
   ```properties
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   VIRUSTOTAL_API_KEY="your_virustotal_v3_api_key_here"
   ```

3. **Build & Run:**
   * Open the project directory in **Android Studio**.
   * Allow Gradle to sync dependencies.
   * Deploy the `app` configuration to an emulator or physical Android device.

---

## 🔮 Future Roadmap

* [ ] **Offline On-Device AI Triage:** Integrate **TensorFlow Lite** models to perform preliminary static malware classification offline prior to cloud transmission.
* [ ] **Forensic PDF Export:** Generate shareable, timestamped PDF intelligence reports directly from the results interface.
* [ ] **Background Scan Notifications:** Offload multi-minute file sandbox polling to `WorkManager` with push notifications upon verdict completion.

---

## 👨‍💻 Author & Academic Context

* **Developer:** Othmane Khachini (`20230929`)
* **Institution:** China Jiliang University — *Android Application and Development*
* **GitHub:** [@OthmaneKhachini](https://github.com/OthmaneKhachini)
* **Repository:** [Secure-Scan](https://github.com/OthmaneKhachini/Secure-Scan)