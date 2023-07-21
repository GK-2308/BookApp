package com.example.myapplication.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.MyApplication;
import com.example.myapplication.R;
import com.example.myapplication.adapters.AdapterComment;
import com.example.myapplication.adapters.AdapterPdfFavorite;
import com.example.myapplication.databinding.ActivityPdfDetailBinding;
import com.example.myapplication.databinding.DialogCommentAddBinding;
import com.example.myapplication.models.ModelComment;
import com.example.myapplication.models.ModelPdf;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfDetailActivity extends AppCompatActivity {

    private ActivityPdfDetailBinding binding;
    String bookId,bookTitle,bookUrl;
    boolean isInMyFavorite = false;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private ArrayList<ModelComment> commentArrayList;
    private AdapterComment adapterComment;


    private static final String TAG_DOWNLOAD="DOWNLOAD_TAG";
    private static final int STORAGE_PERMISSION_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookId = getIntent().getStringExtra("bookId");

        // at start hide download button we need book url that we will later later in loadBookDetails();
        binding.downloadBookBtn.setVisibility(View.GONE);

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser()!=null)
        {
            checkIsFavorite();
        }

        MyApplication.incrementBookViewCount(bookId);
        loadBookDetails();
        loadComments();


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
                if(checkPermission())
                {
                    MyApplication.downloadBook(PdfDetailActivity.this,bookId,bookTitle,bookUrl,binding.downloadsTv);
                    //binding.downloadsTv.setText(""+(Long.parseLong(binding.downloadsTv.getText().toString())+1));
                }
                else
                {
                    requestPermission();
                }
            }
        });

        binding.favouriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(firebaseAuth.getCurrentUser() == null)
                {
                    Toast.makeText(PdfDetailActivity.this, "You're Not Logged In", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if(isInMyFavorite)
                    {
                        MyApplication.removeFromFavorites(PdfDetailActivity.this,bookId);
                    }
                    else
                    {
                        MyApplication.addToFavorite(PdfDetailActivity.this,bookId);
                    }
                }
            }
        });

        binding.addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(firebaseAuth.getCurrentUser()==null){
                    Toast.makeText(PdfDetailActivity.this,"You are not logged in...",Toast.LENGTH_SHORT).show();
                }
                else{
                        addCommentDialog();
                }

            }
        });

    }

    private void loadComments() {
        commentArrayList=new ArrayList<>();

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).child("Comments")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        commentArrayList.clear();
                        for(DataSnapshot ds :snapshot.getChildren()){
                            ModelComment model=ds.getValue(ModelComment.class);

                            commentArrayList.add(model);
                        }
                        adapterComment=new AdapterComment(PdfDetailActivity.this,commentArrayList);
                        binding.commentsRv.setAdapter(adapterComment);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String comment="";
    private void addCommentDialog() {
        DialogCommentAddBinding commentAddBinding=DialogCommentAddBinding.inflate(LayoutInflater.from(this));

        AlertDialog.Builder builder=new AlertDialog.Builder(this,R.style.CustomDialog);
        builder.setView(commentAddBinding.getRoot());

        AlertDialog alertDialog=builder.create();
        alertDialog.show();

        commentAddBinding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        commentAddBinding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comment = commentAddBinding.commentEt.getText().toString().trim();

                if(TextUtils.isEmpty(comment)){
                    Toast.makeText(PdfDetailActivity.this,"Enter your comment...",Toast.LENGTH_SHORT).show();
                    
                }
                else{
                    alertDialog.dismiss();
                    addComment();
                }
            }
        });
    }

    private void addComment() {
        progressDialog.setMessage("Adding comment...");
        progressDialog.show();

        String timestamp=""+System.currentTimeMillis();

        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("id",""+timestamp);
        hashMap.put("bookId",""+bookId);
        hashMap.put("timestamp",""+timestamp);
        hashMap.put("comment",""+comment);
        hashMap.put("uid",""+firebaseAuth.getUid());

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).child("Comments").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        Toast.makeText(PdfDetailActivity.this,"Comment Added...",Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(PdfDetailActivity.this,"Failed to add comment due to "+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });




    }

    private void requestPermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            try{

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package",this.getPackageName(),"null");
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);

            }catch(Exception e){

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    {
                        if(Environment.isExternalStorageManager())
                        {
                            MyApplication.downloadBook(PdfDetailActivity.this,bookId,bookTitle,bookUrl,binding.downloadsTv);
                        }
                        else
                        {
                            Toast.makeText(PdfDetailActivity.this, "Permission Is Denied...", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == STORAGE_PERMISSION_CODE)
        {
            if(grantResults.length > 0)
            {
                boolean write = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if(write && read)
                {
                    MyApplication.downloadBook(PdfDetailActivity.this,bookId,bookTitle,bookUrl,binding.downloadsTv);
                }
                else
                {
                    Toast.makeText(PdfDetailActivity.this, "Permission Is Denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public boolean checkPermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager();
        }
        else{
            int write = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }


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
                        viewsCount = ""+(Long.parseLong(viewsCount)+1);

                        binding.downloadBookBtn.setVisibility(View.VISIBLE);

                        MyApplication.loadCategory(""+categoryId,binding.categoryTv);
                        MyApplication.loadPdfFromUrlSinglePage(""+bookUrl,""+bookTitle,binding.pdfView,binding.progressBar,binding.pagesTv);
                        //MyApplication.loadPdfPageCount(PdfDetailActivity.this,bookUrl,binding.pagesTv);
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

    private void checkIsFavorite()
    {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavorite = snapshot.exists();
                        if(isInMyFavorite)
                        {
                            binding.favouriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_white,0,0);
                            binding.favouriteBtn.setText("Remove Favorite");
                        }
                        else
                        {
                            binding.favouriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white,0,0);
                            binding.favouriteBtn.setText("Add Favorite");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}