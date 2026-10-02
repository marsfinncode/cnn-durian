# Diseases Detection Application v.1.0

<img src="yoedo_logo.svg" alt="App Logo" width="150" /> 

## Overview
**Diseases Detection Application** is an Android mobile application designed to analyze uploaded images and identify potential diseases. Built using Kotlin and modern Android development practices, this app provides an intuitive interface for users to upload images and view analysis results.

## Features
* **Image Upload & Capture:** Easily upload images from your gallery or capture new ones using the device camera.
* **Disease Analysis:** (Add details about your machine learning model or API used for disease detection).
* **Disease List:** View a comprehensive list of potential diseases and their details.
* **Modern UI:** A clean, user-friendly interface built with Android XML layouts and custom themes.

## Tech Stack
* **Language:** [Kotlin](https://kotlinlang.org/)
* **Platform:** Android
* **UI:** XML Layouts (Views)

## Project Structure
* `app/src/main/java/.../` - Contains the Kotlin source code (e.g., `ImageAnalysisActivity.kt`).
* `app/src/main/res/` - Contains all Android resources:
  * `/layout` - UI definitions like `activity_disease_list.xml`.
  * `/drawable` - Vector assets and backgrounds (`ic_image_upload.xml`, `bg_circle_accent.xml`, etc.).
  * `/values` - Themes, colors, and strings.

## Getting Started

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Latest version recommended)
* Android SDK 

### Installation
1. Clone this repository:
   ```bash
   git clone https://github.com/marsfinncode/cnn-durian.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and download the necessary dependencies.
4. Run the app on an Android Emulator or a physical device via USB/Wi-Fi debugging.


## License
This is a project under Burapha University grant and Mahidol Wittayanusorn School. Using this project commercially without proper permission is strictly prohibited.

## Publication

This project is based on the research presented in our paper:

**"Durian Leaf Disease Detection Using CNNs with Oriented Bounding Boxes on Drone-Simulated Imagery"**  
Published in the Proceedings of the 30th Annual Meeting in Mathematics 2026 & Conference in Number Theory and Applications 2026.  
[Read the full proceedings here (Page 303-309)](https://amm2026cna.sc.kku.ac.th/wp-content/uploads/2026/06/e-proc-final_compressed.pdf)

### Citation

If you find this project useful for your research, please consider citing our paper:

```bibtex
@inproceedings{durian_disease_detection_2026,
  title={Durian Leaf Disease Detection Using CNNs with Oriented Bounding Boxes on Drone-Simulated Imagery},
  author={(Suphanat Kladsa-ard, Pakorn Wongaroon, Nachanon Krinchai)},
  booktitle={Proceedings of the 30th Annual Meeting in Mathematics 2026 & Conference in Number Theory and Applications 2026},
  pages={303–309},
  year={2026},
  url={https://amm2026cna.sc.kku.ac.th/wp-content/uploads/2026/06/e-proc-final_compressed.pdf}
}
```

## Project Developers
1. Suphanat Kladsa-ard (MWIT 34) (Project Leader): Responsible for model training and model adjustment techniques.
2. Pakorn Wongaroon (MWIT 34): Responsible for assisting the data acquisition and paper edit.
3. Nachanon Krinchai (MWIT 34): Responsible for application development.

<a href="https://mwit.ac.th/" target="_blank">
 <img src="https://upload.wikimedia.org/wikipedia/th/4/47/Mwit-logo.png?utm_source=th.wikipedia.org&utm_campaign=index&utm_content=original" alt="MWIT logo" width="75" />
</a>


<a href="https://tjsif2026.pcshsbr.ac.th/" target="_blank">
  <img src="https://tjsif2026.pcshsbr.ac.th/img/logo.png" alt="Thailand-Japan Student ICT Fair 2026" width="70">
</a>

