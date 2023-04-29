package com.example.cybershopkissv4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
/**
 * Signup Activity
 *
 *  Activity di registrazione che mette 2 listener sul tasto di signup e sulla scritta login
 *  Quando l'user clicca su "Registrati" avviene il controlla che i dati inseriti non siano vuoti e che siano validi
 *  se tutto va bene
 *      reinderizza alla pagina di login
 *
 *  se clicca sulla scritta "login"
 *      spedisce indietro l'utente nella pagina di login.
 *
 Database coinvolti
    [FirebaseAuth]
        Creazione dell'user
    [Realtime Database]
        Controllare che l'user sia stato creato
 */
public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseDatabase db;

    private Button signupButton;
    private EditText name, surname, mail, password, password2;
    private TextView login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getSupportActionBar().setTitle("CyberShop Demo");
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        name = (EditText) findViewById(R.id.get_name_signup);
        surname = (EditText) findViewById(R.id.get_surname_signup);
        mail = (EditText) findViewById(R.id.get_email_signup);
        password = (EditText) findViewById(R.id.get_pass_signup);
        password2 = (EditText) findViewById(R.id.get_pass2_signup);
        signupButton = (Button) findViewById(R.id.buttonSignup);
        login = findViewById(R.id.loginBack);
    }


    @Override
    protected void onStart() {
        super.onStart();
        signupButton.setOnClickListener(this::signupUser);
        login.setOnClickListener(this::backToLogin);
    }

    private void backToLogin(View view) {
        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        finish();
    }

    private void signupUser(View view) {

        String name = this.name.getText().toString().trim();
        String surname = this.surname.getText().toString().trim();
        String mail = this.mail.getText().toString().trim();
        String password = this.password.getText().toString().trim();
        String password2 = this.password2.getText().toString().trim();


        if (name.isEmpty()) {
            this.name.setError("Empty name field,retry");
            this.name.requestFocus();
            return;
        }

        if (surname.isEmpty()) {
            this.surname.setError("Empty surname field,retry");
            this.surname.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            this.password.setError("Empty password field,retry");
            this.password.requestFocus();
            return;
        }
        if (mail.isEmpty()) {
            this.mail.setError("Empty mail field,retry");
            this.mail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            this.mail.setError("Invalid mail,retry");
            this.mail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            this.password.setError("Password must be at least 8 characters,retry");
            this.password.requestFocus();
            return;
        }
        if (!password.equals(password2)) {
            this.password.setError("Passwords do not match,retry");
            this.password.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(mail, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            // Se la registrazione ha successo, creo una nuova istanza di User
                            // e la inserisco nella cartella users in FirebaseDatabase
                            // creando nuovo un nodo che ha come chiave l'UID dell'User registrato in FirebaseAuth

                            User newUser = new User(name, surname, mail, false);

                            if (mAuth.getCurrentUser() != null)
                                db.getReference("users")
                                        .child(mAuth.getCurrentUser().getUid())
                                        .setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SignupActivity.this,
                                                            "Registration successful!",
                                                            Toast.LENGTH_LONG).show();
                                                    startActivity(new Intent(SignupActivity.this, LoginActivity.class)); //-> vai a loggare
                                                    finish();
                                                } else
                                                    Toast.makeText(SignupActivity.this,
                                                            "Something went wrong",
                                                            Toast.LENGTH_LONG).show();
                                            }
                                        });
                        } else {
                            Toast.makeText(SignupActivity.this,
                                    "The user already exists", Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }
}