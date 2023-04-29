package com.example.cybershopkissv4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** ProfileActivity
 * estrae dal database la lista dei prodotti dalla wishlist dell'user
 * e la inserisce nel recyclerView dove mostra uno a uno i prodotti sotto forma di card (long_card.xml)
 * implementa e setta un listener per ogni card che trasmette l'id nel database come riferimento per la visualizzazione per intero a FullProductActivity
 *
   Database coinvolti
      [FirebaseAuth]
        Visualizza e stampa :nome ,cognome e mail.

      [Realtime Database]
          Viene generata una query da passare a FirebaseRecyclerOptions.Builder<Prodotto>() in modo da passare la lista di option a un RecyclerViewAdapterLong
          che lo elabora quando viene costruito e che viene attaccato al RecyclerView mostrando i prodotti in wishliste dell'user.
          [NB] La lista dei prodotti in wishlist è salvata direttamente in "users/(UID_USER)/wishlist/ChiaveProdotto ->Prodotto quindi c'è una ridondanza dei dati
            ma i dati rimangono aggiornati e consistenti in quanto viene applicata la MultipathUpdate. (Vedi la documentazione di Prodotto)

 */
public class ProfileActivity extends AppCompatActivity implements RecycleViewAdapterLong.onItemClickListener{

    private FirebaseDatabase db;
    private FirebaseAuth mAuth;
    private User loggedUser;
    private TextView name;
    private TextView surname;
    private TextView mail;

    private FirebaseRecyclerOptions<Prodotto> options;
    private RecycleViewAdapterLong adapter;
    private RecyclerView wishlist;

    public ProfileActivity() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setTitle("Il tuo profilo");
        name = (TextView) findViewById(R.id.nameAdminText);
        surname = (TextView) findViewById(R.id.numberProductText);
        mail = (TextView) findViewById(R.id.numberUserText);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        wishlist = (RecyclerView) findViewById(R.id.adminProductRecyclerView);
        wishlist.setLayoutManager(new LinearLayoutManager(this));



          //Uso FirebaseRecyclerOptions per buildare i prodotti in serie dal database
        options = new FirebaseRecyclerOptions.Builder<Prodotto>()
                .setQuery(db.getReference("users").child(mAuth.getCurrentUser().getUid()).child("wishlist"), Prodotto.class)
               .build();


        db.getReference("users")
                .child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()  && snapshot.getValue() != null) {
                            loggedUser = snapshot.getValue(User.class);
                            if (loggedUser != null) {
                                name.setText("Nome:"+loggedUser.getName());
                                surname.setText("Cognome:"+loggedUser.getSurname());
                                mail.setText("Mail:"+loggedUser.getMail());
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    @Override
    protected void onStart() {
        super.onStart();

        // per la RecyclerView
        adapter = new RecycleViewAdapterLong(options);
        wishlist.setAdapter(adapter);
        adapter.startListening();


        // per il click di ogni elemento
        adapter.setOnItemClickListener(this::onItemClick);
        adapter.startListening();

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
            startActivity(new Intent(ProfileActivity.this, FullProductActivity.class)
                    .putExtra("id", uniqueId));
        }
    }

}
