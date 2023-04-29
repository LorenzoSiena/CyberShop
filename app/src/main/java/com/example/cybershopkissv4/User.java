package com.example.cybershopkissv4;

import java.util.HashMap;


/** Classe User
 *
 *  Ha il nome, cognome ,mail,
 *  una wishlist <Chiave Prodotto ,Prodotto> -> Chiave Prodotto è salvato in [Realtime Database]
 *  e un boolean per capire se è un admin
 *
 * */
public class User {

    private String name, surname, mail;

    private boolean admin;

    private HashMap<String, Prodotto> wishlist=null;

    public User() {
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getMail() {
        return mail;
    }

    public User(String name, String surname, String mail, boolean admin) {
        this.name = name;
        this.surname = surname;
        this.mail = mail;
        this.admin = admin;
    }

    public void setWishlist(HashMap<String, Prodotto> wishlist) {
        this.wishlist = wishlist;
    }

    public HashMap<String, Prodotto> getWishlist() {
        return wishlist;
    }

    public boolean isAdmin() {
        return admin;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", mail='" + mail + '\'' +
                ", admin=" + admin +
                ", wishlist=" + wishlist +
                '}';
    }
}
