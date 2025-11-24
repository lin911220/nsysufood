package com.example.nsysufood;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nsysufood.adapter.RestaurantAdapter;
import com.example.nsysufood.model.Restaurant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RestaurantAdapter adapter;
    private List<Restaurant> originalList; // 存原始資料 (用於搜尋後還原)
    private ProgressBar progressBar;
    private SearchView searchView;

    // ★★★ 新增：按鈕變數 ★★★
    private Button btnFilterAll, btnFilterOpen, btnFilterClosed;

    // ★★★ 新增：狀態變數，用來記住現在的篩選條件 ★★★
    private String currentStatusFilter = "ALL"; // 可選值: "ALL", "OPEN", "CLOSED"
    private String currentSearchText = "";      // 記住搜尋文字

    // ★★★ 新增：用來暫存「今日回報數據」的 Map (Key: 餐廳ID, Value: [營業數, 休息數]) ★★★
    private Map<String, int[]> dailyStatsMap = new HashMap<>();
    private String todayDate;

    private TextView tvHeaderUserName;
    private LinearLayout userProfileContainer;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化元件
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();

        // ★★★ 1. 初始化 Header UI ★★★
        tvHeaderUserName = findViewById(R.id.tvHeaderUserName);
        userProfileContainer = findViewById(R.id.userProfileContainer);

        // ★★★ 2. 更新使用者顯示狀態 ★★★
        updateUserHeader();

        // ★★★ 3. 設定點擊監聽 (跳出選單) ★★★
        userProfileContainer.setOnClickListener(v -> showUserMenu());

        // ★★★ 初始化按鈕 ★★★
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterOpen = findViewById(R.id.btnFilterOpen);
        btnFilterClosed = findViewById(R.id.btnFilterClosed);

        originalList = new ArrayList<>();

        // 2. 初始化 Adapter
        adapter = new RestaurantAdapter(originalList, new RestaurantAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Restaurant restaurant) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, RestaurantDetailActivity.class);
                intent.putExtra("restaurant_data", restaurant);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // 3. 開始從 Firebase 讀取資料
        fetchRestaurantsFromFirebase();

        fetchReportData();

        // 4. 設定搜尋監聽器 (功能 5)
        setupSearch();

        // ★★★ 設定按鈕點擊事件 ★★★
        setupFilterButtons();
    }

    // 新增方法：更新右上角顯示文字
    private void updateUserHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 有登入：顯示 Email 前綴
            String name = user.getEmail();
            if (name != null && name.contains("@")) {
                name = name.split("@")[0];
            }
            tvHeaderUserName.setText("👤 " + name);
        } else {
            // 沒登入：顯示訪客
            tvHeaderUserName.setText("👤 訪客");
        }
    }

    // 新增方法：顯示彈出選單
    private void showUserMenu() {
        PopupMenu popup = new PopupMenu(this, userProfileContainer);
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // === 已登入狀態 ===
            // 加入選項：顯示完整 Email (不可點)
            popup.getMenu().add("帳號: " + user.getEmail()).setEnabled(false);
            // 加入選項：登出
            popup.getMenu().add(0, 1, 0, "登出");
        } else {
            // === 訪客狀態 ===
            // 加入選項：前往登入
            popup.getMenu().add(0, 2, 0, "前往登入 / 註冊");
        }

        // 設定點擊事件
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // 登出
                    mAuth.signOut();
                    updateUserHeader(); // 更新右上角文字變回訪客
                    Toast.makeText(MainActivity.this, "已登出", Toast.LENGTH_SHORT).show();
                    // 選擇性：登出後要不要跳回登入頁？如果不用，就留在這
                    return true;

                case 2: // 前往登入
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish(); // 關閉主頁，避免按返回鍵又回來
                    return true;
            }
            return false;
        });

        popup.show();
    }

    // ★★★ 新增：按鈕點擊邏輯 ★★★
    private void setupFilterButtons() {
        btnFilterAll.setOnClickListener(v -> {
            currentStatusFilter = "ALL";
            updateButtonStyles(); // 更新按鈕顏色
            applyFilters();       // 執行篩選
        });

        btnFilterOpen.setOnClickListener(v -> {
            currentStatusFilter = "OPEN";
            updateButtonStyles();
            applyFilters();
        });

        btnFilterClosed.setOnClickListener(v -> {
            currentStatusFilter = "CLOSED";
            updateButtonStyles();
            applyFilters();
        });
    }

    // ★★★ 新增：更新按鈕顏色 (選中的變深色，沒選的變白色) ★★★
    private void updateButtonStyles() {
        // 先重置所有按鈕為 "未選取狀態" (白底黑字)
        int inactiveBg = Color.parseColor("#FFFFFF");
        int inactiveText = Color.parseColor("#333333");

        btnFilterAll.setBackgroundColor(inactiveBg);
        btnFilterAll.setTextColor(inactiveText);
        btnFilterOpen.setBackgroundColor(inactiveBg);
        btnFilterOpen.setTextColor(inactiveText);
        btnFilterClosed.setBackgroundColor(inactiveBg);
        btnFilterClosed.setTextColor(inactiveText);

        // 再將 "被選取" 的按鈕變色 (深底白字)
        int activeBg = Color.parseColor("#2C3E50"); // 深藍色
        int activeText = Color.parseColor("#FFFFFF");

        switch (currentStatusFilter) {
            case "ALL":
                btnFilterAll.setBackgroundColor(activeBg);
                btnFilterAll.setTextColor(activeText);
                break;
            case "OPEN":
                btnFilterOpen.setBackgroundColor(activeBg);
                btnFilterOpen.setTextColor(activeText);
                break;
            case "CLOSED":
                btnFilterClosed.setBackgroundColor(activeBg);
                btnFilterClosed.setTextColor(activeText);
                break;
        }
    }


    // ★★★ 核心方法：綜合篩選邏輯 (同時處理 按鈕 + 搜尋文字) ★★★
    private void applyFilters() {
        List<Restaurant> filteredList = new ArrayList<>();

        String input = currentSearchText.toLowerCase();

        for (Restaurant r : originalList) {
            // 1. 檢查【文字搜尋】: 名稱或地點
            boolean matchSearch = false;
            if (input.isEmpty()) {
                matchSearch = true; // 沒打字就代表全部匹配
            } else {
                if (r.getName().toLowerCase().contains(input) ||
                        (r.getLocationName() != null && r.getLocationName().toLowerCase().contains(input))) {
                    matchSearch = true;
                }
            }

            // 2. 檢查【按鈕狀態】: 全部 / 營業中 / 休息中
            boolean matchStatus = false;
            if (currentStatusFilter.equals("ALL")) {
                matchStatus = true;
            } else if (currentStatusFilter.equals("OPEN") && r.isOperatingNow()) {
                matchStatus = true;
            } else if (currentStatusFilter.equals("CLOSED") && !r.isOperatingNow()) {
                matchStatus = true;
            }

            // ★★★ 只有「兩者都符合」才加入列表 ★★★
            if (matchSearch && matchStatus) {
                filteredList.add(r);
            }
        }

        // 3. 排序 (若是 "全部"，則營業中的排前面；若已篩選狀態則不需要特別排，但保持邏輯一致無妨)
        sortRestaurants(filteredList);

        // 更新 UI
        adapter.updateList(filteredList);
    }

    // 建立選單 (右上角的三個點)
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        // 為了簡單，我們直接用程式碼加一個 "登出" 按鈕，不另外建 xml
        menu.add(0, 1, 0, "登出");
        return true;
    }

    // 處理選單點擊事件
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == 1) { // 如果點了 "登出"
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

            // 回到登入頁面
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchRestaurantsFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("buildings");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalList.clear();
                // 用來統計總數
                int totalBuildings = 0;
                int totalRestaurantsFound = 0;

                android.util.Log.d("FirebaseDebug", "=== 開始讀取資料庫 ===");

                for (DataSnapshot buildingSnapshot : snapshot.getChildren()) {
                    String buildingName = buildingSnapshot.getKey();
                    totalBuildings++;
                    android.util.Log.d("FirebaseDebug", "正在檢查大樓: " + buildingName);

                    // 1. 判斷資料層級結構
                    DataSnapshot targetSnapshot;
                    if (buildingSnapshot.hasChild("restaurants")) {
                        android.util.Log.d("FirebaseDebug", "  -> 偵測到 'restaurants' 子節點 (結構 A)");
                        targetSnapshot = buildingSnapshot.child("restaurants");
                    } else {
                        android.util.Log.d("FirebaseDebug", "  -> 無 'restaurants' 子節點，假設直接是餐廳列表 (結構 B)");
                        targetSnapshot = buildingSnapshot;
                    }

                    // 2. 遍歷該節點下的所有子項目
                    for (DataSnapshot itemSnapshot : targetSnapshot.getChildren()) {
                        String key = itemSnapshot.getKey();

                        // 過濾掉非餐廳的欄位 (例如 contactWindow, holiday)
                        // 我們假設餐廳的 Key 都是 "restaurant_" 開頭
                        if (!key.startsWith("restaurant")) {
                            android.util.Log.d("FirebaseDebug", "  -> 跳過非餐廳欄位: " + key);
                            continue;
                        }

                        try {
                            // 嘗試轉換
                            Restaurant restaurant = itemSnapshot.getValue(Restaurant.class);

                            if (restaurant != null) {
                                // 檢查是否有名字 (有些可能是空資料)
                                if (restaurant.getName() == null || restaurant.getName().isEmpty()) {
                                    android.util.Log.w("FirebaseDebug", "  -> ⚠️ 警告: 找到 " + key + " 但沒有餐廳名稱 (Name is null)");
                                    continue;
                                }

                                restaurant.setLocationName(buildingName);
                                restaurant.setId(itemSnapshot.getKey());
                                originalList.add(restaurant);
                                totalRestaurantsFound++;
                                android.util.Log.d("FirebaseDebug", "  -> ✅ 成功讀取: " + restaurant.getName());
                            } else {
                                android.util.Log.e("FirebaseDebug", "  -> ❌ 失敗: " + key + " 轉換結果為 null");
                            }
                        } catch (Exception e) {
                            // 這裡會抓出具體為什麼失敗！
                            android.util.Log.e("FirebaseDebug", "  -> 🔥 嚴重錯誤: " + key + " 資料格式不符! 原因: " + e.getMessage());
                        }
                    }
                }

                android.util.Log.d("FirebaseDebug", "=== 讀取結束，共找到 " + totalBuildings + " 棟大樓，" + totalRestaurantsFound + " 間餐廳 ===");

                // ★★★ 修改這裡：讀取完基本資料後，立刻填入回報數據並更新畫面 ★★★
                updateListWithStats();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                android.util.Log.e("FirebaseDebug", "資料庫讀取被取消: " + error.getMessage());
            }
        });
    }

    // ★★★ 新增：讀取今日回報數據並更新列表 ★★★
    private void fetchReportData() {
        todayDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("daily_reports").child(todayDate);

        reportRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dailyStatsMap.clear();

                // 1. 遍歷每一間餐廳的回報資料
                for (DataSnapshot restaurantSnapshot : snapshot.getChildren()) {
                    String restaurantId = restaurantSnapshot.getKey();
                    int openCount = 0;
                    int closedCount = 0;

                    // 2. 統計該餐廳下的所有投票 (open 或 closed)
                    for (DataSnapshot userVote : restaurantSnapshot.getChildren()) {
                        String status = userVote.getValue(String.class);
                        if ("open".equals(status)) {
                            openCount++;
                        } else if ("closed".equals(status)) {
                            closedCount++;
                        }
                    }

                    // 3. 存入 Map 備用
                    dailyStatsMap.put(restaurantId, new int[]{openCount, closedCount});
                }

                // 4. 將最新的統計數據填入目前的餐廳列表 (originalList)
                updateListWithStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ★★★ 輔助方法：把 Map 裡的數據塞進 List 並刷新畫面 ★★★
    private void updateListWithStats() {
        if (originalList == null) return;

        for (Restaurant r : originalList) {
            // 根據 ID 去 Map 找有沒有今天的回報數據
            if (dailyStatsMap.containsKey(r.getId())) {
                int[] stats = dailyStatsMap.get(r.getId());
                r.setUserReportOpenCount(stats[0]);
                r.setUserReportClosedCount(stats[1]);
            } else {
                // 如果沒資料，歸零
                r.setUserReportOpenCount(0);
                r.setUserReportClosedCount(0);
            }
        }

        // 通知 Adapter 數據變了，請重新整理畫面
        // 注意：如果你正在搜尋中，這裡可能需要呼叫 applyFilters() 才能同步更新搜尋結果
        applyFilters();
    }

    // 排序邏輯：營業中 (Open=true) 排在最上面
    private void sortRestaurants(List<Restaurant> list) {
        Collections.sort(list, new Comparator<Restaurant>() {
            @Override
            public int compare(Restaurant r1, Restaurant r2) {
                // 如果 r1 營業中 (true) 且 r2 休息 (false)，r1 排前面 (-1)
                if (r1.isOperatingNow() && !r2.isOperatingNow()) return -1;
                // 如果 r1 休息 且 r2 營業中，r2 排前面 (1)
                if (!r1.isOperatingNow() && r2.isOperatingNow()) return 1;
                return 0; // 狀態一樣就不變
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 當輸入文字改變時執行過濾
                currentSearchText = newText;
                applyFilters();
                return true;
            }
        });
    }
}