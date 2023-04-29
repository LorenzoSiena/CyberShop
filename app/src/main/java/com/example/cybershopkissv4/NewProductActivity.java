package com.example.cybershopkissv4;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

/** NewProductActivity
 * Dopo aver cliccato sul fab (+) nella dashboard activity si apre questa activity dove è possibile aggiungere  :
 * nome,descrizione ,prezzo ,l'immagine del prodotto e il file stl del prodotto [che andranno in Storage]
 * E' presente un ImageButton con un listener che invoca selectImage() per scegliere l'immagine dalla memoria del dispositivo che:
 *  imposta il path del file in locale in mImageUri attraverso un onActivityResult e che Setta l'immagine nella previewImage
 *
 * E' presente un ImageButton con un listener che invoca fileChooser() per scegliere il file stl dalla memoria del dispositivo che:
 *  imposta il path del file in locale in mStlUri attraverso un onActivityResult e che Setta [✔] nella previewStl
 *
 *  E' presente un Button addButton con un listener settato che una volta cliccato invoca addNewItemToDatabase che :
 *  controlla che i dati inseriti siano tutti validi e non nulli rimandando il focus se qualche campo non è presente
 *  e infine contatta il db per inserire nel database[Realtime Database] il nuovo prodotto
 *  con i riferimenti mStorageRef e mStorageRef2  per inserire l'immagine e l'stl in [Storage].
 *  Se l'activity viene distrutta perchè il telefono viene ruotato attraverso onSaveInstanceState vengono ricaricati e controllati mStlUri e mImageUri
 *  in modo da non doverli reinserire.
 *  Se il prodotto viene inserito correttamente si viene rimandati a DashboardActivity con un toast di conferma

 Database coinvolti

 [Realtime Database]
    viene contattato per aggiungere il nuovo prodotto
 [Storage]
    viene contattato per aggiungere l'immagine  il file stl in gs://cybershop.../IMG_STL/nome.png e FILE_STL gs://cybershop.../FILE_STL/nome.stl
 * */

public class NewProductActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 101;

    private EditText name;
    private EditText description;
    private ImageView previewImage;
    private ImageView previewStl;
    private EditText price;
    private Button addButton;

    private Uri mImageUri;
    private String downloadUrlImage;

    private Uri mStlUri;
    private String downloadUrlStl;

    private static final int PICK_IMAGE_REQUEST = 102;

    private StorageReference mStorageRef, mStorageRef2;
    private int imageWidth;

    private ProgressBar progressBar;

    private FirebaseDatabase db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_product);
        getSupportActionBar().setTitle("Nuovo prodotto");

        name = findViewById(R.id.nomeNew);
        description = findViewById(R.id.descrizioneNew);
        previewStl = findViewById(R.id.previewStl);
        price = findViewById(R.id.prezzoNew);
        addButton = findViewById(R.id.addNewProductButton);
        previewImage = findViewById(R.id.previewImage);

        //istanza a RealTime Database
        db = FirebaseDatabase.getInstance();

        //Reference al db
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mStorageRef2 = FirebaseStorage.getInstance().getReference();
        progressBar = findViewById(R.id.progressBar2);


        previewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(NewProductActivity.this, "Scegli un immagine", Toast.LENGTH_SHORT).show();
                selectImage();
            }
        });

        previewStl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(NewProductActivity.this, "Scegli un file Stl", Toast.LENGTH_SHORT).show();
                fileChooser();
            }
        });

        //Ripristino le scelte in caso di distruzione
        if (savedInstanceState != null) {
            mImageUri = savedInstanceState.getParcelable("mImageUri");
            mStlUri = savedInstanceState.getParcelable("mStlUri");
            imageWidth = savedInstanceState.getInt("imageWidth");
        }

    }


    @Override
    protected void onStart() {
        super.onStart();

        if (mStlUri != null)
            previewStl.setImageResource(android.R.drawable.checkbox_on_background);
        if (mImageUri != null) {
            Glide.with(NewProductActivity.this).load(mImageUri).override(imageWidth).into(previewImage);


        }

        addButton.setOnClickListener(this::onClick);
    }

    private void onClick(View view) {
        addNewItemToDatabase();
    }

    private void addNewItemToDatabase() {


        float price;
        String name = this.name.getText().toString().trim();
        String description = this.description.getText().toString().trim();

        if (name.isEmpty()) {
            this.name.setError("Inserisci un nome");
            this.name.requestFocus();
            return;
        }

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


        if (mImageUri == null) {
            Toast.makeText(this, "Seleziona un immagine da caricare", Toast.LENGTH_SHORT).show();
            this.previewImage.requestFocus();
            return;
        }

        if (mStlUri == null) {
            Toast.makeText(this, "Seleziona un stl da caricare", Toast.LENGTH_SHORT).show();
            this.previewStl.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        addButton.setVisibility(View.GONE);

        Toast.makeText(this, "Caricamento del file in corso...", Toast.LENGTH_SHORT).show();


        if (mImageUri != null) {

            mStorageRef = mStorageRef.child("IMAGE_STL/" + name + UUID.randomUUID().toString());
            mStorageRef.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            downloadUrlImage = taskSnapshot.getStorage().toString();


                            if (downloadUrlImage != null) { //SE TUTTO VA BENE

                                mStorageRef2 = mStorageRef2.child("FILE_STL/" + name + UUID.randomUUID().toString());
                                mStorageRef2.putFile(mStlUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                                        downloadUrlStl = taskSnapshot.getStorage().toString();


                                        Prodotto prodotto = new Prodotto(name, description, downloadUrlImage, downloadUrlStl, price);


                                        db.getReference("prodotti")
                                                .push()
                                                .setValue(prodotto)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(NewProductActivity.this,
                                                                    "Prodotto aggiunto correttamente", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(NewProductActivity.this, DashboardActivity.class));
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
                                        addButton.setVisibility(View.VISIBLE);
                                        return;
                                    }
                                });


                            } else {
                                Toast.makeText(NewProductActivity.this, "IMG_UPLOAD Qualcosa è andato storto ", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                addButton.setVisibility(View.VISIBLE);
                            }


                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "Image Upload failed", exception);
                            progressBar.setVisibility(View.GONE);
                            addButton.setVisibility(View.VISIBLE);
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
            Glide.with(NewProductActivity.this).load(mImageUri).override(previewImage.getWidth()).into(previewImage);

            imageWidth = previewImage.getWidth();


        }
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mStlUri = data.getData();
            previewStl.setImageResource(android.R.drawable.checkbox_on_background);
        }


    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("mImageUri", mImageUri);
        outState.putParcelable("mStlUri", mStlUri);
        //parametro per Glide
        outState.putInt("imageWidth", imageWidth);
    }
}
