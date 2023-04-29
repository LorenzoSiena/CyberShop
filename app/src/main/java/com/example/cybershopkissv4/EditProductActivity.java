package com.example.cybershopkissv4;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/** EditProductActivity
 * Dopo aver cliccato su un prodotto della lista nella dashboard activity si apre questa activity dove è possibile modificare usando l'id del prodotto ricevuto :
 * nome,descrizione ,prezzo ,l'immagine del prodotto e il file stl del prodotto [che andranno in Storage]
 * I campi sono precompilati con i dati del prodotto da modificare.
 * E' presente un ImageButton con un listener che invoca selectImage() per scegliere l'immagine dalla memoria del dispositivo che:
 *  imposta il path del file in locale in mImageUri attraverso un onActivityResult e che Setta l'immagine nella previewImage sostituendo il riferimento precedente
 *
 * E' presente un ImageButton con un listener che invoca fileChooser() per scegliere il file stl dalla memoria del dispositivo che:
 *  imposta il path del file in locale in mStlUri attraverso un onActivityResult e che Setta [✔] nella previewStl sostituendo il riferimento precedente
 *
 *  E' presente un Button modButton con un listener settato che una volta cliccato invoca modItemToDatabase che :
 *
 *
 *  controlla che i dati inseriti siano tutti validi  rimandando il focus se qualche campo non è presente
 *  e infine contatta il db per inserire nel database[Realtime Database] il prodotto modificato mantenendo l'id
 *  con i riferimenti mStorageRef e mStorageRef2  per inserire l'immagine e l'stl in [Storage] in caso di modifica.
 *  Ovviamente cambiando i file stl e/o immagine i vecchi file in storage saranno eliminati.
 *  Per mantenere una consistenza rigida dei casi e ottimizzare gli accessi a [Storage] durante la modifica ci sono 4 casi possibili
 *     ->non cambio nè stl nè png
 *     ->cambio solo stl
 *     ->cambio solo png
 *     ->cambio entrambi
 *
 *  Se un dato non viene modificato si userà il dato del prodotto iniziale.
 *
 *  Se il prodotto viene modificato correttamente si viene rimandati a DashboardActivity con un toast di conferma
 *
 *  E' presente un Button deleteButton con un listener settato che una volta cliccato invoca deleteItemToDatabase che :
 *  interrogherà il database per l'id del prodotto e lo eliminerà dalla voce presente in [Realtime Database]
 *  e i relativi file associati in [Storage]
 *

 Database coinvolti

 [Realtime Database]
    Interroga il database per la lettura, modifica e eliminazione del prodotto
 [Storage]
    viene contattato per:
    caricare l'immagine del prodotto da modificare
    eliminare il file stl e png da sostiturire
    aggiungere l'immagine  il file stl in gs://cybershop.../IMG_STL/nome.png e FILE_STL gs://cybershop.../FILE_STL/nome.stl

 * */

public class EditProductActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 101;
    private static final int PICK_IMAGE_REQUEST = 102;
    private EditText name;
    private EditText description;
    private ImageView previewImage;
    private ImageView previewStl;
    private EditText price;
    private Button modButton, deleteButton;


    private TextView starsMod;

    private Uri mImageUri;
    private String downloadUrlImage;

    private Uri mStlUri;
    private String downloadUrlStl;
    private ProgressBar progressBar;


    private StorageReference mStorageRef, mStorageRef2;
    private String id;
    private Prodotto oldProdotto;

    private FirebaseDatabase db;
    private FirebaseStorage storage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);
        getSupportActionBar().setTitle("Modifica prodotto");

        //ricevo l'oggetto da visualizzare e modificare
        id = getIntent().getStringExtra("id");

        //cambio il titolo
        name = findViewById(R.id.nomeMod);
        description = findViewById(R.id.descrizioneMod);
        previewStl = findViewById(R.id.previewStlMod);
        price = findViewById(R.id.prezzoMod);


        modButton = findViewById(R.id.modProductButton);
        deleteButton = findViewById(R.id.deleteProductButton);

        previewImage = findViewById(R.id.previewImageMod);

        db = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference(); //reference img STORAGE
        mStorageRef2 = FirebaseStorage.getInstance().getReference(); //reference stl STORAGE
        storage = FirebaseStorage.getInstance();
        starsMod = findViewById(R.id.starsMod);
        progressBar= findViewById(R.id.progressBar);

        //costruisci prodotto


        db.getReference("prodotti").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    //costruisco l'interfaccia
                    oldProdotto = snapshot.getValue(Prodotto.class);


                    mStorageRef = storage.getReferenceFromUrl(oldProdotto.getUrlImage());
                    //carica l'immagine dal riferimento in realtimeDB
                    Glide.with(EditProductActivity.this).load(mStorageRef).override(previewImage.getWidth()).into(previewImage);

                    name.setText(oldProdotto.getNome());
                    price.setText(String.valueOf(oldProdotto.getPrezzo()));
                    description.setText(oldProdotto.getDescrizione());

                    HashMap<String, Float> reviews = oldProdotto.getReviews();

                    if (reviews != null) {

                        ArrayList<Float> valueReviews;
                        valueReviews = new ArrayList<>(oldProdotto.getReviews().values());

                        double sum = 0.0;
                        for (Float value : valueReviews) {
                            sum = sum + value;
                        }
                        float average = (float) sum / valueReviews.size();
                        String formattedAverage = String.format("%.2f", average);
                        starsMod.setText(formattedAverage + "/5 stelle");
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProductActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


        previewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(EditProductActivity.this, "Scegli un immagine", Toast.LENGTH_SHORT).show();
                selectImage();
            }
        });

        previewStl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(EditProductActivity.this, "Scegli un file Stl", Toast.LENGTH_SHORT).show();
                fileChooser();
            }
        });

    }



    @Override
    protected void onStart() {
        super.onStart();
        deleteButton.setOnClickListener(this::onClick);
        modButton.setOnClickListener(this::onClick);
    }

    private void onClick(View view) {

        switch (view.getId()) {
            case R.id.modProductButton:
                modItemToDatabase(id, oldProdotto);
                break;
            case R.id.deleteProductButton:
                deleteItemToDatabase(id);
                break;
        }

    }

    private void deleteItemToDatabase(String id) {
        progressBar.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.GONE);
        modButton.setVisibility(View.GONE);
        Toast.makeText(this, "Eliminazione in corso...", Toast.LENGTH_SHORT).show();
        db.getReference("prodotti") //
                .child(id)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {


                            //cancella l'immagine
                            StorageReference deleteReference = FirebaseStorage.getInstance().getReferenceFromUrl(oldProdotto.getUrlImage());
                            deleteReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.e("firebasestorage", "onSuccess: deleted image");

                                    //cancella l'stl
                                    StorageReference deleteReference2 = FirebaseStorage.getInstance().getReferenceFromUrl(oldProdotto.getUrlStl());
                                    deleteReference2.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // File deleted successfully
                                            Log.e("firebasestorage", "onSuccess: deleted file");

                                            Toast.makeText(EditProductActivity.this,
                                                    "Prodotto rimosso correttamente", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(EditProductActivity.this, DashboardActivity.class));
                                            finish();


                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Uh-oh, an error occurred!
                                            Log.e("firebasestorage", "onFailure: did not delete file");
                                        }
                                    });

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Log.e("firebasestorage", "onFailure: did not delete image");
                                }
                            });


                        }
                    }
                });

    }


    private void modItemToDatabase(String id, Prodotto oldProdotto) {



        String oldImageUrl = oldProdotto.getUrlImage();
        String oldStlUrl = oldProdotto.getUrlStl();


        String name = this.name.getText().toString().trim();
        String description = this.description.getText().toString().trim();

        if (name.isEmpty()) {
            this.name.setError("Inserisci un nome");
            this.name.requestFocus();
            return;
        }

        float price;
        try {
            price = Float.parseFloat(this.price.getText().toString());
        } catch (NumberFormatException e) {
            this.price.setError("Inserisci un prezzo valido");
            this.price.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            this.description.setError("Inserisci una descrizione");
            this.description.requestFocus();
            return;
        }

        //Se url vecchio era vuoto/nullo
        if (oldImageUrl == null || oldImageUrl.isEmpty()) {
            Toast.makeText(this, "Seleziona un immagine da caricare", Toast.LENGTH_SHORT).show();
            this.previewImage.requestFocus();
            return;
        }
        //Se url vecchio era vuoto/nullo
        if (oldStlUrl == null || oldStlUrl.isEmpty()) {
            Toast.makeText(this, "Seleziona un stl da caricare", Toast.LENGTH_SHORT).show();
            this.previewStl.requestFocus();
            return;
        }



        progressBar.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.GONE);
        modButton.setVisibility(View.GONE);
        Toast.makeText(this, "Modifica in corso...", Toast.LENGTH_SHORT).show();


        //Cambio solo attributi (nome,descrizioneprezzo)
        if (mImageUri == null && mStlUri == null) {

            db.getReference("prodotti") //SOVRASCRIVO il vecchio id
                    .child(id)
                    .setValue(new Prodotto(name, description, oldImageUrl, oldStlUrl, price))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(EditProductActivity.this,
                                        "Prodotto modificato correttamente", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(EditProductActivity.this, DashboardActivity.class));
                                finish();
                            }
                        }
                    });

            //Cambio solo STL
        } else if (mImageUri == null && mStlUri != null) {

            //tolgo l'stl vecchio
            StorageReference deleteReference2 = FirebaseStorage.getInstance().getReferenceFromUrl(oldProdotto.getUrlStl());
            deleteReference2.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // File deleted successfully
                    Log.e("firebasestorage", "onSuccess: deleted file");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Uh-oh, an error occurred!
                    Log.e("firebasestorage", "onFailure: did not delete file");
                    progressBar.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    modButton.setVisibility(View.VISIBLE);
                }
            });


            //Aggiungo STL
            mStorageRef2 = mStorageRef2.child("FILE_STL/" + UUID.randomUUID().toString());
            mStorageRef2.putFile(mStlUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //stl nuovo OK!

                    downloadUrlStl = taskSnapshot.getStorage().toString();

                    Prodotto prodotto = new Prodotto(name, description, oldImageUrl, downloadUrlStl, price); // Creo prodotto



                    db.getReference("prodotti") //SOVRASCRIVO il vecchio id
                            .child(id)
                            .setValue(prodotto)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(EditProductActivity.this,
                                                "Prodotto modificato correttamente", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(EditProductActivity.this, DashboardActivity.class));
                                        finish();
                                    }
                                }
                            });


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e(TAG, "Stl Upload failed", exception);
                    progressBar.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    modButton.setVisibility(View.VISIBLE);
                    return;
                }
            });

            //Cambio solo Immagine
        } else if (mImageUri != null && mStlUri == null) {

            //Tolgo l'immagine vecchia
            StorageReference deleteReference = FirebaseStorage.getInstance().getReferenceFromUrl(oldProdotto.getUrlImage());
            deleteReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.e("firebasestorage", "onSuccess: deleted image");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("firebasestorage", "onFailure: did not delete image");
                    progressBar.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    modButton.setVisibility(View.VISIBLE);
                }
            });


            mStorageRef = null; //svuoto
            mStorageRef = FirebaseStorage.getInstance().getReference(); //riempio

            //Aggiungo Immagine
            mStorageRef = mStorageRef.child("IMAGE_STL/" + UUID.randomUUID().toString());
            mStorageRef.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //immagine nuova OK!
                            downloadUrlImage = taskSnapshot.getStorage().toString();
                            Prodotto prodotto = new Prodotto(name, description, downloadUrlImage, oldStlUrl, price); // Creo prodotto


                            db.getReference("prodotti") //SOVRASCRIVO il vecchio id
                                    .child(id)
                                    .setValue(prodotto)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(EditProductActivity.this,
                                                        "Prodotto modificato correttamente", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(EditProductActivity.this, DashboardActivity.class));
                                                finish();
                                            }
                                        }
                                    });


                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "Image Upload failed", exception);
                            progressBar.setVisibility(View.GONE);
                            deleteButton.setVisibility(View.VISIBLE);
                            modButton.setVisibility(View.VISIBLE);
                            return;
                        }
                    });


        } else { //Cambio immagine e stl

            //Tolgo l'immagine vecchia
            StorageReference deleteReference = FirebaseStorage.getInstance().getReferenceFromUrl(oldProdotto.getUrlImage());
            deleteReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.e("firebasestorage", "onSuccess: deleted image");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    modButton.setVisibility(View.VISIBLE);
                    Log.e("firebasestorage", "onFailure: did not delete image");
                }
            });


            mStorageRef = null; //svuoto
            mStorageRef = FirebaseStorage.getInstance().getReference(); //riempio

            //Aggiungo Immagine
            mStorageRef = mStorageRef.child("IMAGE_STL/" + UUID.randomUUID().toString());
            mStorageRef.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //immagine nuova OK!
                            downloadUrlImage = taskSnapshot.getStorage().toString();



                            //tolgo l'stl vecchio
                            StorageReference deleteReference2 = FirebaseStorage.getInstance().getReferenceFromUrl(oldProdotto.getUrlStl());
                            deleteReference2.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // File deleted successfully
                                    Log.e("firebasestorage", "onSuccess: deleted file");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Uh-oh, an error occurred!
                                    progressBar.setVisibility(View.GONE);
                                    deleteButton.setVisibility(View.VISIBLE);
                                    modButton.setVisibility(View.VISIBLE);
                                    Log.e("firebasestorage", "onFailure: did not delete file");
                                }
                            });


                            //Aggiungo STL
                            mStorageRef2 = mStorageRef2.child("FILE_STL/" + UUID.randomUUID().toString());
                            mStorageRef2.putFile(mStlUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //stl nuovo OK!

                                    downloadUrlStl = taskSnapshot.getStorage().toString();

                                    Prodotto prodotto = new Prodotto(name, description, downloadUrlImage, downloadUrlStl, price); // Creo prodotto



                                    db.getReference("prodotti") //SOVRASCRIVO il vecchio id
                                            .child(id)
                                            .setValue(prodotto)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(EditProductActivity.this,
                                                                "Prodotto modificato correttamente", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(EditProductActivity.this, DashboardActivity.class));
                                                        finish();
                                                    }
                                                }
                                            });


                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    progressBar.setVisibility(View.GONE);
                                    deleteButton.setVisibility(View.VISIBLE);
                                    modButton.setVisibility(View.VISIBLE);
                                    Log.e(TAG, "Stl Upload failed", exception);
                                    return;
                                }
                            });


                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            deleteButton.setVisibility(View.VISIBLE);
                            modButton.setVisibility(View.VISIBLE);
                            Log.e(TAG, "Image Upload failed", exception);
                            return;
                        }
                    });

        }

    }


    private void fileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a File"), PICK_FILE_REQUEST);

    }
    public void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    //Gestione immagine con mImageUri
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mImageUri = data.getData(); //salva i dati
            previewImage.setImageURI(mImageUri); //setta l'immagine di preview
        }
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mStlUri = data.getData();
            previewStl.setImageResource(android.R.drawable.checkbox_on_background);
        }
    }


}