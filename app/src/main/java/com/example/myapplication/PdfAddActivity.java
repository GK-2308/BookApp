package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityPdfAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {

    private ActivityPdfAddBinding binding;

    private FirebaseAuth firebaseAuth;

    // arraylist to hold pdf categories
    private ArrayList<String> categoryTitleArrayList,categoryIdArrayList;

    // progress dialog
    private ProgressDialog progressDialog;

    // uri of picked pdf
    private Uri pdfUri=null;

    private static final int PDF_PICK_CODE=1000;
    // tag for debugging
    private static final String TAG="ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth=FirebaseAuth.getInstance();

        loadPdfCategories();

        // setup progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);


        
        // handle click,go to prev activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // handle click,attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdfPickIntent();
            }
        });
        //handle click,pick category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryPickDialog();
            }
        });
        //handle click,upload pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });


    }
    private String title="",description="";
    private void validateData() {
        //step 1: validate data
        Log.d(TAG,"Validating the data...");

        // get data
        title=binding.titleEt.getText().toString().trim();
        description=binding.descriptionEt.getText().toString().trim();

        // validate data
        if(TextUtils.isEmpty(title)){
            Toast.makeText(this,"Enter Title...",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(description)){
            Toast.makeText(this,"Enter description...",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(selectedCategoryTitle)){
            Toast.makeText(this,"Pick category...",Toast.LENGTH_SHORT).show();
        }
        else if(pdfUri==null){
            Toast.makeText(this," Pick Pdf...",Toast.LENGTH_SHORT).show();
        }
        else {
            // all data is valid ,can upload now
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        // Step 2: upload pdf to firebase storage
        Log.d(TAG,"UploadingPdfTOStorage : uploading pdf to storage...");

        // show progress
        progressDialog.setMessage("Uploading Pdf...");
        progressDialog.show();

        // timestamp
        long timestamp=System.currentTimeMillis();

        // path of pdf in firebase storage
        String filePathAddName="Books/"+timestamp;
        // storage reference
        StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathAddName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG,"onSuccess Pdf uploaded to storage...");
                        Log.d(TAG,"onSuccess getting pdf url");

                        // get pdf url
                        Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadedPdfUrl=""+uriTask.getResult();

                        // upload to firebase db
                        uploadPdfInfoToDb(uploadedPdfUrl,timestamp);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG,"on Failure :Pdf upload failed due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this,"Pdf upload failed due to "+e.getMessage(),Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PdfAddActivity.this,DashboardAdminActivity.class));
                    }
                });
    }

    private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
        // Step 3: upload pdf info to firebase db
        Log.d(TAG,"UploadingPdfTOStorage : uploading pdf Info to firebase Db...");
        progressDialog.setMessage("Uploading Pdf Info...");

        String uid=firebaseAuth.getUid();

        // set data to upload
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("uid",""+uid);
        hashMap.put("id",""+timestamp);
        hashMap.put("title",""+title);
        hashMap.put("description",""+description);
        hashMap.put("categoryId",""+selectedCategoryId);
        hashMap.put("url",""+uploadedPdfUrl);
        hashMap.put("timestamp",timestamp);
        hashMap.put("viewsCount",0);
        hashMap.put("downloadsCount",0);

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG,"onSuccess: Successfully Uploaded...");
                        Toast.makeText(PdfAddActivity.this," Successfully Uploaded...",Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PdfAddActivity.this,DashboardAdminActivity.class));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG,"onFailure: failed to upload to db due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this,"failed to upload to db due to "+e.getMessage(),Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PdfAddActivity.this,DashboardAdminActivity.class));
                    }
                });
    }

    private void loadPdfCategories() {
        Log.d(TAG,"loadPdfCategories : Loading Pdf Categories...");

        //init arraylist
        categoryTitleArrayList =new ArrayList<>();
        categoryIdArrayList = new ArrayList<>();

        // get all categories from firebase
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTitleArrayList.clear();
                categoryIdArrayList.clear();
                for(DataSnapshot ds : snapshot.getChildren())
                {
                    //get data
                    String categoryId = ""+ds.child("id").getValue();
                    String categoryTitle = ""+ds.child("category").getValue();

                    categoryTitleArrayList.add(categoryTitle);
                    categoryIdArrayList.add(categoryId);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String selectedCategoryId,selectedCategoryTitle;
    private void categoryPickDialog() {
        Log.d(TAG,"CategoryPickDialog : showing category Pick dialog");

        String[] categoriesArray=new String[categoryTitleArrayList.size()];
        for(int i = 0; i< categoryTitleArrayList.size(); i++)
        {
            categoriesArray[i]= categoryTitleArrayList.get(i);
        }

        // alert dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // handle item click
                        // get clicked item from list
                        selectedCategoryTitle=categoryTitleArrayList.get(which);
                        selectedCategoryId = categoryIdArrayList.get(which);
                        // set to category textview
                        binding.categoryTv.setText(selectedCategoryTitle);

                        Log.d(TAG,"onClick : Selected category:"+selectedCategoryTitle);
                    }
                })
                .show();
    }

    private void pdfPickIntent() {
    Log.d(TAG,"pdfPickIntent : starting pdf pick intent");

        Intent intent=new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf"),PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode==RESULT_OK){
            if(requestCode==PDF_PICK_CODE){
                Log.d(TAG,"OnActivityResult : Pdf Picked");

                pdfUri=data.getData();
                Log.d(TAG,"OnActivityResult : Uri: " +pdfUri);
            }

        }
        else {
            Log.d(TAG,"OnActivityResult : cancelled picking Pdf");
            Toast.makeText(this," cancelled picking Pdf",Toast.LENGTH_SHORT).show();
        }
    }
}