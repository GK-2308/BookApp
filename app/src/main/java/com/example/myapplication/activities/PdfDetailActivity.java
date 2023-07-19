package com.example.myapplication.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.MyApplication;
import com.example.myapplication.databinding.ActivityPdfDetailBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PdfDetailActivity extends AppCompatActivity {

    private ActivityPdfDetailBinding binding;
    String bookId,bookTitle,bookUrl;

    private static final String TAG_DOWNLOAD="DOWNLOAD_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookId = getIntent().getStringExtra("bookId");

        // at start hide download button we need book url that we will later later in loadBookDetails();
        binding.downloadBookBtn.setVisibility(View.GONE);

        loadBookDetails();
        MyApplication.incrementBookViewCount(bookId);

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // handle click , open to view pdf
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1=new Intent(PdfDetailActivity.this , PdfViewActivity.class);// create activity for reading book
                intent1.putExtra("bookId",bookId);
                startActivity(intent1);
            }
        });

        binding.downloadBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG_DOWNLOAD,"onClick : Checking permission ");
                if(ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
                {
                    MyApplication.downloadBook(PdfDetailActivity.this,""+bookId,""+bookTitle,""+bookUrl);
                }
                else
                {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });
    }

    private ActivityResultLauncher<String> requestPermissionLauncher=
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),isGranted-> {
                if(isGranted)
                {
                    Log.d(TAG_DOWNLOAD,"Permission Granted");
                    MyApplication.downloadBook(this,""+bookId,""+bookTitle,""+bookUrl);
                }
                else
                {
                    Toast.makeText(this,"permission was denied...",Toast.LENGTH_SHORT).show();
                }
            });
    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                         bookTitle = ""+snapshot.child("title").getValue();
                         bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String description = ""+snapshot.child("description").getValue();

                        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        binding.downloadBookBtn.setVisibility(View.VISIBLE);

                        MyApplication.loadCategory(""+categoryId,binding.categoryTv);
                        MyApplication.loadPdfFromUrlSinglePage(""+bookUrl,""+bookTitle,binding.pdfView,binding.progressBar);
                        MyApplication.loadPdfSize(""+bookUrl,""+bookTitle,binding.sizeTv);
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.dateTv.setText(date);
                        binding.viewsTv.setText(viewsCount);
                        binding.downloadsTv.setText(downloadsCount.replace("null","N/A"));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}