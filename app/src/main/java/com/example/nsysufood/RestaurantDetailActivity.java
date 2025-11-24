package com.example.nsysufood;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nsysufood.adapter.ReviewAdapter;
import com.example.nsysufood.model.BusinessHours;
import com.example.nsysufood.model.Restaurant;
import com.example.nsysufood.model.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RestaurantDetailActivity extends AppCompatActivity {

    private TextView tvName, tvLocation, tvStatus, tvHours, tvStats;
    private Button btnReportOpen, btnReportClosed;
    private Restaurant restaurant;
    private String todayDate;

    // 評論相關變數
    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private Button btnAddReview;
    private TextView tvNoReviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        restaurant = (Restaurant) getIntent().getSerializableExtra("restaurant_data");
        if (restaurant == null) {
            finish();
            return;
        }

        todayDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        initViews();
        setupUI();
        setupButtons();

        // 啟動監聽器
        loadReportStats();
        loadReviews();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvDetailName);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvHours = findViewById(R.id.tvDetailHours);
        tvStats = findViewById(R.id.tvReportStats);
        btnReportOpen = findViewById(R.id.btnReportOpen);
        btnReportClosed = findViewById(R.id.btnReportClosed);

        // 評論區
        rvReviews = findViewById(R.id.rvReviews);
        btnAddReview = findViewById(R.id.btnAddReview);
        tvNoReviews = findViewById(R.id.tvNoReviews);

        // 初始化評論列表
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupUI() {
        tvName.setText(restaurant.getName());
        tvLocation.setText(restaurant.getLocationName());
        tvHours.setText(getAllWeekHours());

        if (restaurant.isOperatingNow()) {
            tvStatus.setText("目前狀態：營業中");
            tvStatus.setTextColor(Color.parseColor("#00695C"));
            tvStatus.setBackgroundColor(Color.parseColor("#E0F2F1"));
        } else {
            tvStatus.setText("目前狀態：休息中");
            tvStatus.setTextColor(Color.parseColor("#616161"));
            tvStatus.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }
    }

    private String getAllWeekHours() {
        if (restaurant.getBusinessHours() == null) return "營業時間：未提供";
        BusinessHours hours = restaurant.getBusinessHours();
        StringBuilder sb = new StringBuilder();
        sb.append("【營業時間表】\n");
        sb.append("週一 : ").append(formatHour(hours.getMonday())).append("\n");
        sb.append("週二 : ").append(formatHour(hours.getTuesday())).append("\n");
        sb.append("週三 : ").append(formatHour(hours.getWednesday())).append("\n");
        sb.append("週四 : ").append(formatHour(hours.getThursday())).append("\n");
        sb.append("週五 : ").append(formatHour(hours.getFriday())).append("\n");
        sb.append("週六 : ").append(formatHour(hours.getSaturday())).append("\n");
        sb.append("週日 : ").append(formatHour(hours.getSunday()));
        return sb.toString();
    }

    private String formatHour(String time) {
        return (time == null || time.isEmpty()) ? "休息" : time;
    }

    private void setupButtons() {
        btnReportOpen.setOnClickListener(v -> submitReport("open"));
        btnReportClosed.setOnClickListener(v -> submitReport("closed"));
        btnAddReview.setOnClickListener(v -> showAddReviewDialog());
    }

    // --- 回報功能 ---
    private void submitReport(String status) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "請先登入才能回報！", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("daily_reports")
                .child(todayDate).child(restaurant.getId()).child(user.getUid());
        reportRef.setValue(status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) Toast.makeText(this, "感謝回報！", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadReportStats() {
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("daily_reports")
                .child(todayDate).child(restaurant.getId());
        statsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long openCount = 0, closedCount = 0;
                for (DataSnapshot userVote : snapshot.getChildren()) {
                    String status = userVote.getValue(String.class);
                    if ("open".equals(status)) openCount++;
                    else if ("closed".equals(status)) closedCount++;
                }
                tvStats.setText(String.format(Locale.getDefault(), "今日回報統計：🟢 %d 人營業 / 🔴 %d 人休息", openCount, closedCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- 評論功能 ---
    private void showAddReviewDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "請先登入才能留言", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        builder.setView(view);

        final android.widget.EditText etComment = view.findViewById(R.id.etDialogComment);
        final android.widget.RatingBar rbRating = view.findViewById(R.id.rbDialogRating);

        builder.setPositiveButton("送出", (dialog, which) -> {
            String comment = etComment.getText().toString();
            float rating = rbRating.getRating();
            if (rating == 0) {
                Toast.makeText(this, "請至少給一顆星", Toast.LENGTH_SHORT).show();
                return;
            }
            saveReviewToFirebase(user, rating, comment);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void saveReviewToFirebase(FirebaseUser user, float rating, String comment) {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(restaurant.getId());
        String reviewId = reviewsRef.push().getKey();
        String displayName = user.getEmail() != null ? user.getEmail().split("@")[0] : "匿名";
        Review newReview = new Review(reviewId, user.getUid(), displayName, rating, comment, System.currentTimeMillis());
        if (reviewId != null) {
            reviewsRef.child(reviewId).setValue(newReview)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "評論已送出", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadReviews() {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(restaurant.getId());
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Review review = ds.getValue(Review.class);
                    if (review != null) reviewList.add(review);
                }
                Collections.reverse(reviewList);
                reviewAdapter.updateList(reviewList);

                if (reviewList.isEmpty()) {
                    tvNoReviews.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.GONE);
                } else {
                    tvNoReviews.setVisibility(View.GONE);
                    rvReviews.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}