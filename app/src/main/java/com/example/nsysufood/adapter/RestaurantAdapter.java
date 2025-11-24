package com.example.nsysufood.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nsysufood.R;
import com.example.nsysufood.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private List<Restaurant> restaurantList;
    private OnItemClickListener listener;

    // 定義一個介面，讓 MainActivity 可以監聽點擊事件
    public interface OnItemClickListener {
        void onItemClick(Restaurant restaurant);
    }

    // 建構子 (Constructor)
    public RestaurantAdapter(List<Restaurant> list, OnItemClickListener listener) {
        this.restaurantList = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    // 更新列表資料 (用於搜尋或讀取完資料庫後)
    public void updateList(List<Restaurant> newList) {
        this.restaurantList = newList;
        notifyDataSetChanged(); // 通知 UI 重繪
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 載入我們剛剛寫好的 item_restaurant.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);

        // 1. 設定名稱與位置
        holder.tvName.setText(restaurant.getName());

        // ★★★ 新增：設定今日營業時間 ★★★
        String todayHours = getTodayHoursString(restaurant);
        holder.tvTodayHours.setText(todayHours);

        // 判斷是否有位置名稱 (如: 山海樓)，沒有的話就顯示 "未知位置"
        String location = restaurant.getLocationName();
        if (location == null || location.isEmpty()) {
            location = "校園餐廳";
        }
        holder.tvLocation.setText(location);

        // 2. 設定營業狀態標籤顏色
        if (restaurant.isOperatingNow()) {
            holder.tvStatusBadge.setText("營業中");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#00695C"));
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#E0F2F1"));
        } else {
            holder.tvStatusBadge.setText("休息中");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#616161"));
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }

        // 3. 設定回報數據 (目前資料庫還沒讀取到這塊，預設會是 0)
        holder.tvReportOpen.setText("🟢 " + restaurant.getUserReportOpenCount() + " 人回報營業");
        holder.tvReportClosed.setText("🔴 " + restaurant.getUserReportClosedCount() + " 人回報休息");

        // 4. 設定點擊監聽
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(restaurant);
            }
        });
    }

    // ★★★ 輔助方法：取得今天的時間字串 ★★★
    private String getTodayHoursString(Restaurant restaurant) {
        if (restaurant.getBusinessHours() == null) {
            return "時間：詳見公告";
        }

        // 取得台灣時間的星期幾
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Taipei"));
        int day = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        String hours = "";

        switch (day) {
            case java.util.Calendar.MONDAY:    hours = restaurant.getBusinessHours().getMonday(); break;
            case java.util.Calendar.TUESDAY:   hours = restaurant.getBusinessHours().getTuesday(); break;
            case java.util.Calendar.WEDNESDAY: hours = restaurant.getBusinessHours().getWednesday(); break;
            case java.util.Calendar.THURSDAY:  hours = restaurant.getBusinessHours().getThursday(); break;
            case java.util.Calendar.FRIDAY:    hours = restaurant.getBusinessHours().getFriday(); break;
            case java.util.Calendar.SATURDAY:  hours = restaurant.getBusinessHours().getSaturday(); break;
            case java.util.Calendar.SUNDAY:    hours = restaurant.getBusinessHours().getSunday(); break;
        }

        if (hours == null || hours.isEmpty()) return "今日未營業";
        return "今日: " + hours;
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    // ViewHolder 類別：用來抓取 xml 裡面的元件
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvStatusBadge, tvReportOpen, tvReportClosed;
        TextView tvTodayHours; // ★★★ 新增變數
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRestaurantName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTodayHours = itemView.findViewById(R.id.tvTodayHours);
            tvReportOpen = itemView.findViewById(R.id.tvReportOpen);
            tvReportClosed = itemView.findViewById(R.id.tvReportClosed);
        }
    }
}