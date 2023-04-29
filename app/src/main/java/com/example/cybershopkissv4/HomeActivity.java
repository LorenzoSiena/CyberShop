package com.example.cybershopkissv4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**  HomeActivity
 *  Inizializza un menù da mettere sotto che porta a profilo dell'user oppure al logout
 *  estrae dal database la lista dei prodotti e la inserisce nel recyclerView dove mostra uno a uno i prodotti sotto forma di card (long_card.xml)
 *  implementa e setta un listener per ogni card che trasmette l'id nel database come riferimento per la visualizzazione per intero a FullProductActivity
 Database coinvolti
    [FirebaseAuth]
      Legge il nome user per salutare l'utente nella schermata in alto
    [Realtime Database]
        Viene generata una query da passare a FirebaseRecyclerOptions.Builder<Prodotto>() in modo da passare la lista di option a un RecyclerViewAdapterLong
        che lo elabora quando viene costruito e che viene attaccato al RecyclerView mostrando i prodotti dello shop.
 */
public class HomeActivity extends AppCompatActivity implements RecycleViewAdapterLong.onItemClickListener {
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private TextView message_user;
    private User loggedUser;

    //Menu sotto
    private BottomNavigationView navigationView;

    //contenuti da dare al costruttore del recycler
    private FirebaseRecyclerOptions<Prodotto> options;

    //adattatore per le card dei prodotti
    private RecycleViewAdapterLong adapter;

    //puntatore al recyclerview dove saranno visualizzati i prodotti
    private RecyclerView prodList;


    //Gestione Menu BottomNavigationView con opzioni (profilo e signout)
    private BottomNavigationView.OnItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.logout_menu:
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                    finish();
                    return true;
                case R.id.profilo_menu:
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getSupportActionBar().setTitle("Cyber Shop");


        //riferimento a firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        message_user = (TextView) findViewById(R.id.welcomeViewHome);
        navigationView = findViewById(R.id.navigator);
        navigationView.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
        prodList = findViewById(R.id.prodList);


        //Preparo la recyclerview
        prodList = (RecyclerView) findViewById(R.id.prodList);
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
                                loggedUser = snapshot.getValue(User.class);
                                if (loggedUser != null) {
                                    message_user.setText("Benvenuto, " + loggedUser.getName() + "!");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(HomeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
    }

    @Override
    protected void onStart() {
        super.onStart();

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
    public void onItemClick(DataSnapshot snapshot, int position) {
        // al click recuperiamo l'id dell'oggetto scelto e lo passiamo a FullProductActivity
        String uniqueId = snapshot.getKey();
        if (uniqueId != null && !uniqueId.isEmpty()) {
            startActivity(new Intent(HomeActivity.this, FullProductActivity.class)
                    .putExtra("id", uniqueId));
        }
    }


}