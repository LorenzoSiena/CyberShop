package com.example.cybershopkissv4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** First Activity
 *   Activity di ingresso che reindirizza l'utente in 3 stati possibili
 *   Loggato con diritti di User ->HomeActivity
 *   Loggato con diritti di Superuser -> DashboardActiviy
 *   Non loggato -> LoginActivity
 *

 Database coinvolti
    [FirebaseAuth] + [Realtime Database]
        Si controlla che l'user corrente sia autenticato ,che esista nel database e se è un admin.
 */

public class FirstActivity extends AppCompatActivity {


    //Riferimenti al db
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //User loggato ed esiste
        if(mAuth.getCurrentUser()!=null){
        database.getReference("users").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists() && snapshot.getValue()!= null){
                        user =snapshot.getValue(User.class);
                        if (user !=null && user.isAdmin())
                            startActivity(new Intent(FirstActivity.this,DashboardActivity.class)); //-> Sei un admin
                        else
                            startActivity(new Intent(FirstActivity.this,HomeActivity.class)); //Sei un user
                        finish();
                    }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FirstActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


        } else  { //Non Loggato
            startActivity(new Intent(FirstActivity.this, LoginActivity.class));
            finish();
        }


    }
}