# 📁 File Organizer Tool (Java)

## 📌 Overview

The File Organizer Tool is a Java-based application that automatically organizes files in a selected directory into categorized folders based on their file types. This helps in maintaining a clean and structured file system.

---

## 🎯 Features

* Automatically scans a folder
* Sorts files into categories (Images, Documents, Videos, Audio, Archives, Others)
* Creates folders if they do not exist
* Moves files safely using Java file handling
* Simple command-line interface

---

## 🛠️ Technologies Used

* Java
* File Handling (`java.io.File`)
* NIO Package (`java.nio.file.Files`)
* Collections (`HashMap`)

---

## 📂 Project Structure

```
FileOrganizer/
│
├── FileOrganizer.java
└── README.md
```

---

## 🚀 How to Run

### 1. Compile the program

```
javac FileOrganizer.java
```

### 2. Run the program

```
java FileOrganizer
```

### 3. Enter folder path

Example:

```
C:\Users\YourName\Downloads
```

---

## 📊 Example

### Before:

```
Downloads/
file1.jpg
file2.pdf
song.mp3
video.mp4
```

### After:

```
Downloads/
Images/
Documents/
Audio/
Videos/
```

---

## 💡 Future Improvements

* Add duplicate file renaming
* Add GUI interface (Java Swing)
* Add logging system to track moved files

---

 Author

Magizh P

