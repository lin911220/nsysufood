package com.example.nsysufood.model;

import com.google.firebase.database.Exclude;
import java.io.Serializable;

public class Restaurant implements Serializable {
    private String id;
    private String name;
    private String contact;
    private boolean open; // 這是官方的營業狀態
    private String lastUpdated;
    private double latitude;
    private double longitude;
    private BusinessHours businessHours; // 巢狀物件

    // --- 以下是用於 App 內部邏輯的欄位 (不需要存入 Database) ---
    @Exclude
    private int userReportOpenCount = 0; // 今日使用者回報營業數
    @Exclude
    private int userReportClosedCount = 0; // 今日使用者回報休息數
    @Exclude
    private float distance = 0; // 與使用者的距離 (公尺)
    @Exclude
    private String locationName; // 用來存 "山海樓" 這種大樓名稱

    // Firebase 需要空的建構式
    public Restaurant() {
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public boolean isOpen() { return open; }
    public String getLastUpdated() { return lastUpdated; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public BusinessHours getBusinessHours() { return businessHours; }

    // Setters (Firebase 寫入需要，或者單純讀取時需要 Public 權限)
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getLocationName() { return locationName; }

    public void setId(String id) { this.id = id; }

    @Exclude
    public int getUserReportOpenCount() { return userReportOpenCount; }
    @Exclude
    public void setUserReportOpenCount(int count) { this.userReportOpenCount = count; }

    @Exclude
    public int getUserReportClosedCount() { return userReportClosedCount; }
    @Exclude
    public void setUserReportClosedCount(int count) { this.userReportClosedCount = count; }

    @Exclude
    public float getDistance() { return distance; }
    @Exclude
    public void setDistance(float distance) { this.distance = distance; }

    public boolean isOperatingNow() {
        if (this.businessHours == null) {
            return this.open;
        }

        try {
            // 1. 設定時區 (台灣時間)
            java.util.TimeZone timeZone = java.util.TimeZone.getTimeZone("Asia/Taipei");
            java.util.Calendar calendar = java.util.Calendar.getInstance(timeZone);
            int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);

            // 2. 取得今天的營業時間字串
            String timeString = "";
            switch (dayOfWeek) {
                case java.util.Calendar.MONDAY: timeString = businessHours.getMonday(); break;
                case java.util.Calendar.TUESDAY: timeString = businessHours.getTuesday(); break;
                case java.util.Calendar.WEDNESDAY: timeString = businessHours.getWednesday(); break;
                case java.util.Calendar.THURSDAY: timeString = businessHours.getThursday(); break;
                case java.util.Calendar.FRIDAY: timeString = businessHours.getFriday(); break;
                case java.util.Calendar.SATURDAY: timeString = businessHours.getSaturday(); break;
                case java.util.Calendar.SUNDAY: timeString = businessHours.getSunday(); break;
            }

            // 3. 基本過濾
            if (timeString == null || timeString.isEmpty() || timeString.contains("公休") || timeString.contains("未營業")) {
                return false;
            }

            // 4. ★★★ 處理多時段邏輯 ★★★
            // 先把全形符號換成半形，統一格式
            // 將 "11:00-14:00、16:30-20:00" 裡面的 "、" 換成 "," 方便切割
            String cleanTime = timeString.replace("～", "-").replace("、", ",").replace(" ", "");

            // 使用逗號切割成多個區段： ["11:00-14:00", "16:30-20:00"]
            String[] periods = cleanTime.split(",");

            // 計算現在的分鐘數
            int currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE);

            // 遍歷每一個時段，只要有一個符合，就回傳 true
            for (String period : periods) {
                String[] parts = period.split("-");
                if (parts.length == 2) {
                    int startMinutes = parseTimeToMinutes(parts[0]);
                    int endMinutes = parseTimeToMinutes(parts[1]);

                    if (startMinutes == -1 || endMinutes == -1) continue; // 格式錯誤就跳過

                    // 判斷是否在這個區間內
                    if (endMinutes < startMinutes) {
                        // 跨日情況 (例如 17:00-02:00)
                        if (currentMinutes >= startMinutes || currentMinutes <= endMinutes) {
                            return true; // 只要有一個時段符合就是營業中
                        }
                    } else {
                        // 一般情況 (例如 11:00-14:00)
                        if (currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                            return true; // 只要有一個時段符合就是營業中
                        }
                    }
                }
            }

            // 如果迴圈跑完都沒有符合的時段，那就是休息中
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return this.open; // 發生錯誤時退回預設值
        }
    }


    // 小工具：把 "11:30" 轉成 690 分鐘
    private int parseTimeToMinutes(String timeStr) {
        try {
            String[] split = timeStr.split(":");
            int h = Integer.parseInt(split[0]);
            int m = Integer.parseInt(split[1]);
            return h * 60 + m;
        } catch (Exception e) {
            return -1;
        }
    }
}