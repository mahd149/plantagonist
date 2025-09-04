---
# 🌱 Plantagonist

Plantagonist is a desktop application built with Java & JavaFX to help houseplant enthusiasts care for their plants consistently.  
It promotes sustainable urban living and biodiversity by aligning with UN SDG 15: Life on Land and SDG 3: Good Health and Well-being.

---

## ✨ Features

- 👤 **Multi-user login** (local accounts stored offline).
    
- 🌿 **Plant management**: add, edit, delete plants with custom care schedules.
- 💧 **Care logging**: track watering, sunlight, and fertilizing with history.
- 📅 **Dashboard & notifications**: upcoming tasks, overdue reminders.
- 🔔 **Streaks & badges**: gamified motivation to maintain care consistency.
- 📦 **Supplies checklist**: monitor soil, fertilizer, pots, etc. with refill reminders.
- 🌟 (Stretch Goals) Growth journal with notes/images, virtual garden, community sharing, weather-aware UI.

---

## 🛠️ Tech Stack

- **Language**: Java (JDK 24)
- **UI Framework**: JavaFX + Scene Builder
- **Data Storage**: JSON (offline persistence)
- **Version Control**: Git + GitHub
- **Build Tool**: Maven

---

## 📂 Project Structure

`plantagonist/  ├── src/main/java/org/plantagonist/  │   ├── core/        # Models, repositories, services  │   ├── ui/          # JavaFX controllers  │   └── App.java     # Entry point  ├── src/main/resources/org/plantagonist/ui/  │   ├── *.fxml       # UI layouts  │   └── css/         # Stylesheets  ├── data/  │   └── plantagonist.json   # Local storage file  └── README.md`

---

## 🚀 Getting Started

### Prerequisites

- Install Java JDK 24 (Liberica recommended).
- Install Maven.
- Install JavaFX SDK.
---

## 🗂️ Data Storage

- All data is stored **locally in JSON** at:
    `~/.plantagonist/data/`
- No internet or external database required.
---

## 👥 Team **CookiesAndCaches:**

- [Mahdi Sahil](https://github.com/mahd149)
- [Maisha Sanjida](https://github.com/Loona6)
- [Obidit Islam](https://github.com/tashobi02)
---

## 📜 License

This project is for **educational purposes** under CSE 4402: Visual Programming Lab.  
