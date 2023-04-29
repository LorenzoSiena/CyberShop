package com.example.cybershopkissv4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * Login Activity
 *
 * Activity che mette 2 listener sul tasto di login e sulla scritta signup
 * se fa il login
 * fa il check delle credenziali inserite
 * e in base al tipo di utente manda un intent verso la home o la dashboard
 * se clicca su signup
 * spedisce l'utente nella pagina di registrazione.

 Database coinvolti
 [FirebaseAuth]
    Autenticazione User
 [Realtime Database]
    Controlla che l'user sia regolarmente registrato

 */

public class LoginActivity extends AppCompatActivity {
    private EditText mail, password;
    private TextView signup;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private User user;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().setTitle("CyberShop Demo");

        //Istanze db
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        mail = (EditText) findViewById(R.id.get_name_signup);
        password = (EditText) findViewById(R.id.get_pass_signup);
        signup = (TextView) findViewById(R.id.loginBack);
        loginButton = (Button) findViewById(R.id.buttonSignup);
        progressBar = findViewById(R.id.progressBar3);
    }

    @Override
    protected void onStart() {
        super.onStart();
        signup.setOnClickListener(this::onClick);
        loginButton.setOnClickListener(this::onClick);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginBack: //Vai a Registrarti!
                startActivity(new Intent(this, SignupActivity.class));
                break;
            case R.id.buttonSignup:
                userLogin();
                break;
            default:
                Toast.makeText(LoginActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                break;
        }


    }

    private void userLogin() { //Funzione per il tasto Login
        String mail = this.mail.getText().toString().trim();
        String password = this.password.getText().toString().trim();

        //Errore mail vuota
        if (mail.isEmpty()) {
            this.mail.setError("Mail required");
            this.mail.requestFocus();
            return;
        }
        //Errore password vuota
        if (password.isEmpty()) {
            this.password.setError("Password required");
            this.password.requestFocus();
            return;
        }
        //Errore formato email non valida
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            this.mail.setError("Invalid mail format");
            this.mail.requestFocus();
            return;
        }
        //Errore password < 8
        if (password.length() < 8) {
            this.password.setError("Password must be at least 8 characters");
            this.password.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.GONE);
        //Contatto il server Auth per il login
        mAuth.signInWithEmailAndPassword(mail, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            redirectUser();
                        } else {
                            progressBar.setVisibility(View.GONE);
                            loginButton.setVisibility(View.VISIBLE);
                            Toast.makeText(LoginActivity.this,
                                    "User not found, retry",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });


    }

    private void redirectUser() { //spedisco l'user alla home o alla dashboard se è un admin

        db.getReference("users")
                .child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) {

                            user = snapshot.getValue(User.class);
                            if (user != null && user.isAdmin())
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            else
                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LoginActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
