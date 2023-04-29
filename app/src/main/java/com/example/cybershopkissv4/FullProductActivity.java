package com.example.cybershopkissv4;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/** FullProductActivity
 * Activity col compito di visualizzare un prodotto nello shop(da HomeActivity ) o in wishlist (da Profile activity)
 *  Riceve da HomeActivy un intent con l'id del prodotto da visualizzare
 *  Da qui è possibile :
 *      visualizzare nome, descrizione,immagine,prezzo e recensione precedente [1-5] stelle (0 stelle se mai recensito)
 *
 *      **** visualizzare in 3d l'stl del prodotto attraverso floatingViewStlFab ****
 *      [Feature non presente in questa demo in quanto ARCore non viene implementato da specifiche,premento il fab un messaggio di Snackbar segnala avverte l'utente]
 *      [Il file stl è in ogni caso disponibile e funzionale nel database [Storage] per una successiva implementazione]
 *      ****
 *
 *      aggiungere il prodotto alla propria wishlist
 *      aggiungere una nuova recensione al prodotto [1-5] stelle
 *
 *
 *  L'Activity imposta i listener per:
 *  la risoluzione dell'aggiunta della review al prodotto da 1 a 5 stelle
 *   onRatingChanged:
 *      -trova l'uid del prodotto X ,va nella cartella review e aggiorna la recensione di questo user con rating a 1 a 5
 *      -Consulta la user_wish (Look up table) per controllare se qualche user ha il prodotto in wishlist
 *          - se è cosi va a modificare pure il prodotto X nelle wishlist per mantenere la coerenza dei dati
 *
 *   Per aggiornare i dati in modo atomico preparo un  HashMap con i percorsi e i valori da aggiornare  e li inserisco tutti insieme con  RootRef.updateChildren(updates);
 *
 *
 *  la risoluzione dell'aggiunta di un prodotto alla wishlist dell'utente controllando se è già presente/se vuole rimuoverlo /se vuole aggiungerlo
 *   buttonWishlist->Listener:
 *
 *          Controllo se l'user ha messo in wishlist il prodotto
 *          e aggiorno di conseguenza l'icona
 *          e la look up table per il path user_wish/(UID_PROD)/(UID_USER)
 *
 *
 *  *  Il motivo della seguente implementazione è dovuta alla nature Nosql(JSON) di Realtime Database (non permette join di chiavi ) e all utilizzo dei builder di Firebase che accettano l'intero prodotto
 *  *  Questo causa una duplicazione nel database di ogni prodotto aggiunto in wishlist dall'user ma è un design pattern(DENORMALIZATION) utile per la scalabilità e alla successiva velocità di accesso.
 *  *  Quindi si applica secondo le guide ufficiali di firebase il MultiPath Update che consiste in una look up table user_wish/(ChiaveProdotto)/(UID_USER):true ,
 *  *  che tiene conto delle wishlist degli utenti da consultare quando viene modificato il rating del prodotto Realtime Database.
 *  *  [Denormalization is normal with the Firebase Database]-> https://www.youtube.com/watch?v=vKqXSZLLnHA
 *

 Database coinvolti
 [FirebaseAuth]
    Per prendere l'uid dell'user loggato
 [Realtime Database]
    Per Visualizzare il prodotto e per aggiungere/rimuoverlo dalla wishlist dell'utente loggato
    Per mantenere la look up table nel path user_wish/(UID_PROD)/(UID_USER)
 [Storage]
    Per visualizzare con Glide l'immagine del prodotto e eventualemente l'stl da visualizzare
  */

public class FullProductActivity extends AppCompatActivity {
    private String id;
    private Prodotto prodotto;
    private ImageView image;
    private TextView price, description;
    private RatingBar ratingBar;
    private ImageButton buttonWishlist;
    private FirebaseDatabase db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    private FirebaseStorage storage;
    private FloatingActionButton fab;
    private float ratingX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_product);
        //riceve il messaggio da HomeActivity con l'id del prodotto nel database
        id = getIntent().getStringExtra("id");
        //si connette ai db
        db = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        //creo un istanza in STORAGE per passare il riferimento dell'immagine a Glide
        storage = FirebaseStorage.getInstance();
        image = (ImageView) findViewById(R.id.fullImageProd);
        buttonWishlist = (ImageButton) findViewById(R.id.imageButtonWish);
        price = (TextView) findViewById(R.id.textPrice);
        description = (TextView) findViewById(R.id.fullDescr);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        fab= findViewById(R.id.floatingViewStlFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "La libreria ARCore non è implementata\nQuesta è solo una Demo.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        //Interrogo il database per costruire il prodotto che visualizzo a schermo intero
        db.getReference("prodotti").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {

                    prodotto = snapshot.getValue(Prodotto.class);

                    storageRef = storage.getReferenceFromUrl(prodotto.getUrlImage());
                    //carica l'immagine dal riferimento in realtimeDB
                    Glide.with(FullProductActivity.this).load(storageRef).into(image);
                    getSupportActionBar().setTitle(prodotto.getNome());
                    price.setText(" Prezzo: " + prodotto.getPrezzo() + "€ ");
                    description.setText(prodotto.getDescrizione());

                    HashMap<String, Float> reviews = prodotto.getReviews();
                    if (reviews != null) { // se il prodotto ha almeno una review
                        for (String key : reviews.keySet()) {
                            //controllo se l'utente ha già messo una review sul prodotto
                            if (key.equals(mAuth.getCurrentUser().getUid())) {
                                // se troviamo tra le chiavi quella dell'utente loggato,
                                // allora l'utente ha già messo una review per quel prodotto
                                ratingX = reviews.get(key);

                                ratingBar.setRating(ratingX);
                                break;
                            }
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FullProductActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


        //Legge dal db se l'utente ha il prodotto in wishlist
        //PATH[RealTime Database]: users -> (UID_USER)-> wishlist ->Prodotto(id)
        //Controllo se l'utente ha messo il prodotto in wishlist ciclando la lista delle wishlist per la chiave del prodotto
        db.getReference("users")
                .child(mAuth.getCurrentUser().getUid())
                .child("wishlist")
                .child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) { //il prodotto è già in wishlist
                            buttonWishlist.setImageResource(R.drawable.wishlist_full);

                            prodotto = snapshot.getValue(Prodotto.class);

                            HashMap<String, Float> reviews;
                            if (prodotto.getReviews() != null) { // se il già una review

                                reviews = prodotto.getReviews();

                                for (String key : reviews.keySet()) {
                                    //controllo se l'utente ha già messo una review sul prodotto
                                    if (key.equals(mAuth.getCurrentUser().getUid())) {
                                        // se troviamo tra le chiavi quella dell'utente loggato,
                                        // allora l'utente ha già messo una review per quel prodotto
                                        ratingBar.setRating(ratingX);
                                        break;
                                    }
                                }
                            }


                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FullProductActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        //PATH[RealTime Database]: users -> (UID_USER)-> wishlist ->Prodotto(id)
        // Imposto un Listener sul pulsante "wishlist"
        buttonWishlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //accedo alla wishlist dell'user loggato
                db.getReference("users")
                        .child(mAuth.getCurrentUser().getUid())
                        .child("wishlist")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {


                                DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference(); //reference a root
                                Map<String, Object> updates = new HashMap<>();                             //preparo map con i path da usare
                                String uid_user = mAuth.getCurrentUser().getUid();                         //uid_user
                                Map<String, Object> wishlistUpdates = new HashMap<>();                     //preparo map con i path da usare per la lookuptable

                                if (snapshot.exists() && snapshot.getValue() != null) { //Se wishlist ha questo uid_user


                                    if (!snapshot.hasChild(id)) { //Se user NON ha questo prodotto(id) in wishlist


                                        wishlistUpdates.put(uid_user, true);
                                        updates.put("user_wish/" + id, wishlistUpdates);


                                        if (!prodotto.updateRating(uid_user, ratingX)) //aggiorno l'oggetto con il nuovo rating
                                            Log.e("tag", "Errore!");


                                        snapshot.getRef().child(id).setValue(prodotto);
                                        RootRef.updateChildren(updates);

                                        buttonWishlist.setImageResource(R.drawable.wishlist_full);  //imposto il tasto per segnalarlo
                                        Toast.makeText(FullProductActivity.this, "Prodotto aggiunto!", Toast.LENGTH_SHORT).show();
                                    } else { //Se user ha questo prodotto(id)

                                        updates.put("user_wish/" + id, null);
                                        snapshot.child(id).getRef().removeValue();
                                        RootRef.updateChildren(updates);
                                        buttonWishlist.setImageResource(R.drawable.wishlist_empty);
                                        Toast.makeText(FullProductActivity.this, "Prodotto rimosso!", Toast.LENGTH_SHORT).show();
                                    }


                                } else { //Se wishlist NON ha questo uid_user


                                    wishlistUpdates.put(uid_user, true);
                                    updates.put("user_wish/" + id, wishlistUpdates);
                                    if (!prodotto.updateRating(uid_user, ratingX)) //aggiorno l'oggetto con il nuovo rating
                                        Log.e("tag", "Errore!");
                                    snapshot.getRef().child(id).setValue(prodotto);
                                    RootRef.updateChildren(updates);

                                    buttonWishlist.setImageResource(R.drawable.wishlist_full);
                                    Toast.makeText(FullProductActivity.this, "Prodotto aggiunto!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(FullProductActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // Imposto un Listener sulla ratingBar
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (rating < 1.0f) {                //limito il rating minimo a 1 stella
                    ratingBar.setRating(1.0f);
                }
                ratingX = rating;

                DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();  //reference a root
                Map<String, Object> updates = new HashMap<>();                              //preparo map con i path da usare
                String uid_user = mAuth.getCurrentUser().getUid();                          //uid_user


                db.getReference("user_wish").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists() && snapshot.getValue() != null) {         //user_wish esiste e non è nullo
                            if (!snapshot.hasChild(id)) { //Il prodotto non è presente in nessuna wishlist

                                updates.put("prodotti/" + id + "/reviews/" + uid_user, rating);
                                RootRef.updateChildren(updates);

                            } else {     //Il prodotto (id) è presente in almeno una wishlist

                                updates.put("prodotti/" + id + "/reviews/" + uid_user, ratingX);

                                //interrogo lo snapshot e  la LookUpTable
                                for (DataSnapshot user : snapshot.child(id).getChildren()) {

                                    updates.put("users/" + user.getKey() + "/wishlist/" + id + "/reviews/" + user.getKey(), ratingX);

                                }
                                RootRef.updateChildren(updates);
                            }
                        } else { // Nessun user ha mai messo una recensione

                            //Update solo il prodotto
                            updates.put("prodotti/" + id + "/reviews/" + uid_user, ratingX);
                            RootRef.updateChildren(updates);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FullProductActivity.this, "Errore salvataggio db", Toast.LENGTH_SHORT).show();
                    }
                });

            }

        });

    }
}