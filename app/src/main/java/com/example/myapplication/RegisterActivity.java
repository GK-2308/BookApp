package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

    }

    private String name="",email="",password="";
    private void validateData(){

        name = binding.nameEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();
        String cPassword = binding.cpasswordEt.getText().toString().trim();

        if(TextUtils.isEmpty(name)){
            Toast.makeText(this,"Enter Your Name",Toast.LENGTH_SHORT).show();
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            Toast.makeText(this, "Invalid Email Address", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(password))
        {
            Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(cPassword))
        {
            Toast.makeText(this, "Enter Confirm Password", Toast.LENGTH_SHORT).show();
        }
        else if(!password.equals(cPassword))
        {
            Toast.makeText(this, "Password Doesn't Match", Toast.LENGTH_SHORT).show();
        }
        else
        {
            createUserAccount();
        }

    }

    private void createUserAccount() {
        progressDialog.setMessage("Creating Account");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        updateUserInfo();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserInfo() {
        progressDialog.setMessage("Saving User Info");
        long timespamp = System.currentTimeMillis();
        String uid = firebaseAuth.getUid();
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("uid",uid);
        hashMap.put("email",email);
        hashMap.put("name",name);
        hashMap.put("profilePic","");
        hashMap.put("userType","user");
        hashMap.put("timestamp",timespamp);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(uid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //data added
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Account Created", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this,DashboardUserActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //data not added
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}