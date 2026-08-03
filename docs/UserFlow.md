# User Flow Document

This document outlines the user flows for key operations in **Safe75**.

---

## 1. Onboarding & First Launch Flow
This flow describes what a new user experiences when launching the application for the first time.

```mermaid
graph TD
    Start([Launch App]) --> CheckPrefs{Onboarding Complete?}
    CheckPrefs -- Yes --> Home[Main Dashboard]
    CheckPrefs -- No --> Welcome[Welcome Slides]
    Welcome --> Slider[Swipe through intro slides]
    Slider --> SetupSelection{Choose Setup Mode}
    SetupSelection -->|OCR Scan| OCR[Timetable Camera Scan]
    SetupSelection -->|Manual| Manual[Manual Timetable Config]
    OCR --> ConfirmOnboarding[Complete Onboarding]
    Manual --> ConfirmOnboarding
    ConfirmOnboarding --> WritePrefs[Save onboarding_complete = true]
    WritePrefs --> Home
```

---

## 2. Timetable Import Flow (OCR & Manual)
How users configure their subjects and weekly class timings.

```mermaid
graph TD
    Start[Timetable Configuration] --> ChooseMethod{Choose Method}
    
    %% OCR Path
    ChooseMethod -->|Camera / Gallery| OCRInput[Take photo / Select image]
    OCRInput --> Process[ML Kit Text Processing]
    Process --> Parse[Identify subjects, days, and times]
    Parse --> Review[OCR Review Screen]
    Review --> Edit[Edit mismatched times / names]
    
    %% Manual Path
    ChooseMethod -->|Manual Entry| ManualAdd[Add Subject CRUD]
    ManualAdd --> AddSchedule[Specify day, start, and end time]
    
    Edit --> Save[Write to SQLite tables]
    AddSchedule --> Save
    Save --> End([Timetable Loaded])
```

---

## 3. Daily Attendance Logging Flow
The flow for logging attendance entries daily.

```mermaid
graph TD
    Start[Log Attendance] --> Trigger{Trigger Event}
    Trigger -->|Notification Click| Notification[Load Dashboard]
    Trigger -->|Manual Navigation| ManualNav[Open Schedule Screen]
    
    Notification --> SelectClass[Select active class card]
    ManualNav --> SelectClass
    
    SelectClass --> Options{Select Status}
    Options -->|Present| LogP[Mark PRESENT]
    Options -->|Absent| LogA[Mark ABSENT]
    Options -->|Cancelled| LogC[Mark CANCELLED]
    
    LogP --> AddNote[Optional: Add Comment Note]
    LogA --> AddNote
    LogC --> AddNote
    
    AddNote --> SaveLog[Write Log entry to Room DB]
    SaveLog --> RefreshUI[Refresh Dashboard charts]
    RefreshUI --> End([Attendance Updated])
```

---

## 4. Semester Archive & Reset Flow
The process to archive completed semester stats and prepare for a new academic term.

```mermaid
graph TD
    Start[Initiate Semester Reset] --> Warn{Verify Reset Request}
    Warn -->|Cancel| Exit[Exit without changes]
    Warn -->|Confirm| Archive{Archive past data?}
    
    Archive -->|Yes| SaveArchive[Write all current subjects and logs to SemesterArchive]
    Archive -->|No| ClearDB[Clear current subjects and logs]
    
    SaveArchive --> ClearDB
    ClearDB --> CreateNew[Prompt: Create New Semester]
    CreateNew --> InputName[Input new Semester Name]
    InputName --> DBInsert[Insert new Semester record]
    DBInsert --> RefreshDashboard[Reset Dashboard percentage to 100%]
    RefreshDashboard --> End([Ready for New Term])
```

---

## 5. Local Backup & Restore Flow
Safeguarding database files offline.

```mermaid
graph TD
    Start[Backup & Restore Settings] --> Action{Choose Action}
    
    Action -->|Backup| Export[Query Room DB & Preferences]
    Export --> Serial[Serialize records to JSON string]
    Serial --> WriteFile[Save JSON file to device Storage]
    WriteFile --> Toast[Show Toast: Backup Completed]
    
    Action -->|Restore| ChooseFile[Select backup JSON file]
    ChooseFile --> ReadFile[Read JSON string]
    ReadFile --> ValidateJSON{Validate Schema Signature}
    ValidateJSON -- Invalid --> Error[Show Error Message]
    ValidateJSON -- Valid --> ConfirmWipe{Wipe current database?}
    ConfirmWipe -- Yes --> ReplaceDB[Overwrite Room tables and datastores]
    ReplaceDB --> Restart[Restart App Context]
```
