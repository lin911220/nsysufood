# 學餐開了沒 (School Food App) 🍱

**「學餐開了沒」** 是一款專為校園設計的餐廳即時資訊整合 App。

鑑於校內餐廳營業時間常有變動（如臨時公休、寒暑假調整），本 App 結合 **Firebase Realtime Database** 與 **群眾回報機制**，提供最即時的營業狀態、評論交流與精確的營業時間查詢，解決學生「走到餐廳才發現沒開」的痛點。

## ✨ 核心功能

### 1. 🚦 智慧營業狀態與篩選
* **即時狀態判斷**：App 內建演算法，根據 **台灣時區 (Asia/Taipei)** 與資料庫中的詳細營業時段（支援午休、多時段營業），自動判斷顯示「🟢 營業中」或「🔴 休息中」。
* **快速篩選**：使用者可透過頂部按鈕快速切換「全部」、「只看營業中」或「只看休息中」。
* **即時搜尋**：支援餐廳名稱與地點（如：山海樓）的關鍵字搜尋。

### 2. 📢 群眾即時回報系統 (Crowdsourcing)
* **即時投票**：若現場狀況與表定不同，使用者可即時回報狀態。
* **防作弊機制**：系統綁定 User ID，**每人每日對同一間餐廳限回報一次**，新回報會自動覆蓋舊回報，確保數據準確性。
* **每日自動重置**：回報數據以「日期」為 Key，隔日自動歸零重新計算。

### 3. 💬 評論與評分互動
* **留言板**：使用者可撰寫評論並給予 1~5 星評分。
* **即時同步**：評論送出後，所有使用者端會即時更新列表。
* **強制時區顯示**：評論時間戳記強制轉換為台灣時間顯示，避免模擬器時區誤差。

### 4. 👤 會員與訪客系統
* **Firebase Auth**：支援 Email/密碼註冊與登入。
* **訪客模式**：未登入者僅能瀏覽資訊，需登入後才能執行「回報」與「評論」操作。
* **友善錯誤提示**：自動將 Firebase 的英文錯誤代碼（如密碼過短、帳號重複）轉換為易懂的中文提示。

---

## 🏗️ 系統架構與技術棧

本專案採用標準的 **MVC (Model-View-Controller)** 架構模式進行開發，確保程式碼職責分離，易於維護。

* **開發語言**：Java
* **IDE**：Android Studio
* **後端資料庫**：Firebase Realtime Database (NoSQL)
* **身份驗證**：Firebase Authentication
* **UI 元件**：RecyclerView, CardView, ConstraintLayout

---

## 📂 檔案結構說明 (File Structure)

以下是專案主要檔案 (`app/src/main/java/com/example/nsysufood/`) 的功能說明：

### 1. View (Activity / UI 層)
負責處理畫面顯示與使用者互動。

* **`MainActivity.java`** (主畫面)
    * 負責顯示餐廳列表。
    * 處理搜尋 (SearchView) 與 篩選按鈕 (Filter Buttons) 的邏輯。
    * 處理頂部使用者選單 (登入/登出)。
    * 監聽 Firebase 的 `buildings` (基本資料) 與 `daily_reports` (即時回報數據)。
* **`LoginActivity.java`** (登入/註冊頁)
    * 處理 Firebase Authentication 的登入與註冊邏輯。
    * 包含 `getFriendlyErrorMessage` 方法，提供中文錯誤反饋。
* **`RestaurantDetailActivity.java`** (詳細頁)
    * 顯示單一餐廳的完整資訊與整週營業時間表。
    * **核心功能**：實作「回報營業/休息」的寫入邏輯。
    * **核心功能**：讀取與撰寫評論 (Reviews) 及即時顯示。

### 2. Model (資料模型層)
負責定義資料結構，對應 Firebase JSON 格式。

* **`Restaurant.java`**
    * 餐廳物件，包含名稱、位置、座標等。
    * **關鍵方法**：`isOperatingNow()` -> 內含複雜的時間判斷邏輯 (處理跨日、午休、台灣時區)。
* **`BusinessHours.java`**
    * 儲存週一至週日的營業時間字串。
* **`Review.java`**
    * 評論物件，包含留言內容、星數、作者 ID 與時間戳記。

### 3. Adapter (連接器)
負責將資料填入 RecyclerView 列表。

* **`RestaurantAdapter.java`**
    * 將餐廳資料綁定到 `item_restaurant.xml` 卡片上。
    * 根據營業狀態動態改變標籤顏色 (綠/灰)。
    * 顯示今日營業時間摘要。
* **`ReviewAdapter.java`**
    * 將評論資料綁定到 `item_review.xml`。
    * 負責將時間戳記 (Timestamp) 轉換為可讀日期格式。

---

## 🚀 安裝與執行指南 (Installation)

由於本專案包含敏感的 Firebase 設定檔 (`google-services.json`)，該檔案已被 `.gitignore` 排除。請依以下步驟設定開發環境：

### 步驟 1：複製專案
開啟 Terminal 或 Git Bash：
```bash
git clone [https://github.com/lin911220/nsysufood.git](https://github.com/lin911220/nsysufood.git)
````

### 步驟 2：設定 Firebase (關鍵！)

你無法直接執行本專案，因為缺少與 Firebase 的連結。

1.  前往 [Firebase Console](https://console.firebase.google.com/) 建立一個新專案。
2.  新增 Android 應用程式，套件名稱 (Package Name) 請填寫：`com.example.nsysufood` (需與 `build.gradle` 內一致)。
3.  下載 **`google-services.json`** 檔案。
4.  將該檔案放入專案的 **`app/`** 資料夾底下。
5.  在 Firebase Console 開啟以下功能：
    * **Authentication**：啟用 Email/Password 登入。
    * **Realtime Database**：建立資料庫並設定規則為 `read: true, write: true` (測試用)。

### 步驟 3：編譯與執行

使用 Android Studio 開啟專案，同步 Gradle 後，點擊 **Run** (綠色播放鍵) 即可在模擬器或實體手機上執行。

-----

## 🗄️ 資料庫結構 (Database Schema)

Firebase Realtime Database 主要分為三個節點：

```json
{
  "buildings": {
    // 儲存餐廳靜態資料 (名稱、電話、營業時間表)
  },
  "daily_reports": {
    "20251124": { // 依照日期自動分類
      "restaurant_id": {
        "user_uid": "open" // 記錄每位使用者的投票狀態，防止重複投票
      }
    }
  },
  "reviews": {
    "restaurant_id": {
      "review_id": {
        "comment": "好吃",
        "rating": 5,
        "timestamp": 1732456789000,
        "userId": "..."
      }
    }
  }
}
```

-----

## 📝 授權與開發者

Developed by **[lin911220]**
專案僅供學術交流與學習使用。

```
```