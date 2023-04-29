package com.example.cybershopkissv4;

import java.util.HashMap;



/** Classe Prodotto
 *
 *  Ha nome del prodotto, descrizione , prezzo
 *  una lista con le recensioni "review" <Chiave User , float valutazione da 1 a 5> -> Chiave User è salvato in Realtime Database
 *  una stringa urlImage col path di [Storage Database] (gs://xxxxxxx.appspot.com/IMAGE_STL/nomefile.png) del file immagine da utilizzare con glide quando visualizzo il prodotto.
 *  una stringa urlStl col path di [Storage Database] (gs://xxxxxxx.appspot.com/FILE_STL/nomefile.stl) del file stl (Formato di stampa 3D) da visualizzare con ARCore e scaricare sul dispositivo
 *  [N.B. la visualizzazione e il download del file stl non è stato implementato secondo le specifiche del progetto in quanto coinvolge la libreria ARCore]
 *
 * [Realtime Database!]
 *  Nell'applicazione prodotto viene salvato nei seguenti path di Realtime Database
 *
 *   PATH:
 *  "prodotti"->Prodotto
 *  "users/(UID_USER)/wishlist/Prodotto
 *  "user_wish/(UID_USER)/(UID_Prodotto:true)
 *
 *  Il motivo della seguente implementazione è dovuta alla nature Nosql(JSON) di Realtime Database (non permette join di chiavi ) e all utilizzo dei builder di Firebase che accettano l'intero prodotto
 *  Questo causa una duplicazione nel database di ogni prodotto aggiunto in wishlist dall'user ma è un design pattern(DENORMALIZATION) utile per la scalabilità e alla successiva velocità di accesso.
 *  Quindi si applica secondo le guide ufficiali di firebase il MultiPath Update che consiste in una look up table user_wish/(ChiaveProdotto)/(UID_USER):true , che tiene conto delle wishlist degli utenti da consultare quando viene modificato il rating del prodotto Realtime Database.
 *  [Denormalization is normal with the Firebase Database]-> https://www.youtube.com/watch?v=vKqXSZLLnHA
 * */

public class Prodotto {

    public Prodotto() {
    }


    private String nome;
    private String descrizione;
    private String urlImage;
    private String urlStl;
    private float prezzo;

    private HashMap<String, Float> reviews;

    public HashMap<String, Float> getReviews() {
        return reviews;
    }

    public void setReviews(HashMap<String, Float> reviews) {
        this.reviews = reviews;
    }

    public Prodotto(String nome, String descrizione, String urlImage, String urlStl, float prezzo) {

        this.nome = nome;
        this.descrizione = descrizione;
        this.urlImage = urlImage;
        this.urlStl = urlStl;
        this.prezzo = prezzo;
    }



    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public float getPrezzo() {
        return prezzo;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public String getUrlStl() {
        return urlStl;
    }

    @Override
    public String toString() {
        return "Prodotto{" +
                "nome='" + nome + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", urlImage='" + urlImage + '\'' +
                ", urlStl='" + urlStl + '\'' +
                ", prezzo=" + prezzo +
                ", reviews=" + reviews +
                '}';
    }

    public boolean updateRating(String uid_user, float ratingX) {

        if (this.reviews.containsKey(uid_user)) {
            this.reviews.replace(uid_user, ratingX);
            return true;
        }
            return false;
    }
}

