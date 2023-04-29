package com.example.cybershopkissv4;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
/** Classe RecycleViewAdapterLong
 *  estende FirebaseRecyclerAdapter
 *  Il suo scopo è adattare la lista di card dei prodotti "long_card.xml" costruite interrogando il database [Realtime Database]
 *  in un oggetto compatibile con i recyclerView presenti nella home user(lista Prodotto in store),
 *  nella dashboard dell'admin(lista prodotti da modificare/eleminare)
 *  e nel profilo personale utente(lista wishlist).
 *
 *  Per ogni item vi è associato un interfaccia onItemClickListener implementata in DashboardActivity,HomeActivity e ProfileActivity
 *  La sua funzione è quella di passare l'id del prodotto cliccato a FullProductActivity per essere visualizzato in fullscreen.
 *  In questa Activity viene calcolata la media del rating del prodotto.
 *
 Database coinvolti
 [Storage]
    Per le immagini da inserire con Glide

 [Realtime Database indirettamente]
     in quanto riceve tutta la lista dei prodotti in onBindViewHolder, quando viene creato e istanziato da un altra activity

 * */
public class RecycleViewAdapterLong extends FirebaseRecyclerAdapter<Prodotto, RecycleViewAdapterLong.MyViewHolder> {

    private onItemClickListener listener; //interfaccia per il listener
    private StorageReference storageRef;
    private FirebaseStorage storage;

    public RecycleViewAdapterLong(@NonNull FirebaseRecyclerOptions<Prodotto> options) {
        super(options);
        storage = FirebaseStorage.getInstance(); //CREO UN ISTANZA

    }

    public void setOnItemClickListener(onItemClickListener listener) {
        this.listener = listener;
    }


    //SETTO L'HOLDER
    @Override
    protected void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull Prodotto model) {


        //inserisco contenuti di prodotto
        holder.title.setText(model.getNome());
        holder.description.setText(model.getDescrizione());
        holder.price.setText(String.valueOf(model.getPrezzo()) + "€");
        storageRef = storage.getReferenceFromUrl(model.getUrlImage());
        //calcolo la media
        if (model.getReviews() != null) {
            ArrayList<Float> reviews = new ArrayList<>(model.getReviews().values());
            double sum = 0.0;
            for (Float value : reviews) {
                sum = sum + value;
            }
            float average = (float) sum / reviews.size();
            holder.ratingBar.setRating(average);
        } else
            holder.ratingBar.setRating(0);

        Glide.with(holder.image.getContext()).load(storageRef).into(holder.image);
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // qui settiamo il file xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.long_card, parent, false);
        return new MyViewHolder(view);
    }

    // SottoClasse che estende RecyclerView.ViewHolder
    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, price;
        ImageView image;
        RatingBar ratingBar;


        //costruttore
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.title_card);
            description = (TextView) itemView.findViewById(R.id.description_card);
            image = (ImageView) itemView.findViewById(R.id.image_card);
            price = (TextView) itemView.findViewById(R.id.price_card);
            ratingBar = (RatingBar) itemView.findViewById(R.id.rating_card);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null)
                        listener.onItemClick(getSnapshots().getSnapshot(position), position);
                }
            });
        }
    }

    //Interfaccia per il onItemClickListener
    public interface onItemClickListener {
        void onItemClick(DataSnapshot snapshot, int position);
    }


}
