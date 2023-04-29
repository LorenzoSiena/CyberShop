package com.example.cybershopkissv4;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**Dashboard Activity
 * Questa è la home per l'admin che visualizza :
 * i dati dell'amministratore: nome,cognome e mail.
 * statisiche del negozio: numero prodotti in negozio e numero user registrati.
 * Da qui è possibile manipolare il database e i prodotti dello shop per:
 * 1) Modificare o cancellare un prodotto cliccando su un elemento della lista dei prodotti che una volta cliccati reinderizzano a -->EditProductActivity[con id prodotto da editare]
 * 2) aggiungere un prodotto nel negozio cliccando sul FloatingActionButton fluttuante sopra la lista dei prodotti del negozio --->NewProductActivity.
 * Cliccando la navigation bar in basso si eseguire il logout.

 Database coinvolti
 [FirebaseAuth]
    Vengono controllati i privilegi dell'user nella pagina
 [Realtime Database]
    Si ricava nome,cognome e mail dell'admin.
    Si conta il numero di utenti registrati.
    Si conta il numero di prodotti nel negozio.

 * */


public class DashboardActivity extends AppCompatActivity implements RecycleViewAdapterLong.onItemClickListener {
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;

    private TextView numeroProdotti;
    private TextView numeroUserIscritti;
    private TextView nameAdmin,surnameAdmin,mailAdmin;


    private User loggedAdmin;
    private FloatingActionButton fab;
    //Menu sotto
    private BottomNavigationView navigationView;

    //contenuti da dare al costruttore del recycler
    private FirebaseRecyclerOptions<Prodotto> options;

    //adattatore per le card dei prodotti
    private RecycleViewAdapterLong adapter;
    //puntatore al recyclerview dove saranno visualizzati i prodotti
    private RecyclerView prodList;

    private BottomNavigationView.OnItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.logout_menu:
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
                    finish();
                    return true;
            }
            return false;
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        getSupportActionBar().setTitle("Dashboard Admin");
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        navigationView = findViewById(R.id.navigator);
        navigationView.setOnItemSelectedListener(mOnNavigationItemSelectedListener);

        //statistiche shop
        numeroProdotti = findViewById(R.id.numberProductText);
        numeroUserIscritti = findViewById(R.id.numberUserText);

        //dati Admin
        nameAdmin = findViewById(R.id.nameAdminText);
        surnameAdmin = findViewById(R.id.surnameAdminText);
        mailAdmin = findViewById(R.id.mailAdminText);




        fab = findViewById(R.id.floatingNewProduct);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DashboardActivity.this, NewProductActivity.class));
            }
        });


        //Preparo la recyclerview
        prodList = (RecyclerView) findViewById(R.id.adminProductRecyclerView);
        prodList.setLayoutManager(new LinearLayoutManager(this));

        //Uso FirebaseRecyclerOptions per buildare i prodotti in serie dal database
        options = new FirebaseRecyclerOptions.Builder<Prodotto>()
                .setQuery(db.getReference().child("prodotti"), Prodotto.class)
                .build();

        if (mAuth.getCurrentUser() != null)
            db.getReference("users")
                    .child(mAuth.getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && snapshot.getValue() != null) {
                                loggedAdmin = snapshot.getValue(User.class);
                                if (loggedAdmin != null) {
                                    //mostro i dati dell'admin
                                    nameAdmin.setText( "nome: "+loggedAdmin.getName());
                                    surnameAdmin.setText("cognome: "+loggedAdmin.getSurname());
                                    mailAdmin.setText("mail: "+loggedAdmin.getMail());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(DashboardActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        else { //Qua è successo qualcosa di brutto
            startActivity(new Intent(DashboardActivity.this, FirstActivity.class));
            finish();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        // riempiamo il contatore utenti registrati
        db.getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            numeroUserIscritti.setText(String.valueOf(snapshot.getChildrenCount())+" utenti registrati");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(DashboardActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // riempiamo il contatore sale pubblicate
        db.getReference("prodotti")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            numeroProdotti.setText(String.valueOf(snapshot.getChildrenCount())+" prodotti disponibili");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(DashboardActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        // per la RecyclerView
        adapter = new RecycleViewAdapterLong(options);
        prodList.setAdapter(adapter);
        adapter.startListening();

        // per il click di ogni elemento
        adapter.setOnItemClickListener(this::onItemClick);
    }


    @Override
    protected void onStop() {
        super.onStop();

        adapter.stopListening();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        adapter.startListening();
    }

        @Override
        public void onItemClick (DataSnapshot snapshot,int position){
            String uniqueId = snapshot.getKey();
            if (uniqueId != null && !uniqueId.isEmpty()) {
                startActivity(new Intent(DashboardActivity.this, EditProductActivity.class)
                        .putExtra("id", uniqueId));
            }
        }
    }
