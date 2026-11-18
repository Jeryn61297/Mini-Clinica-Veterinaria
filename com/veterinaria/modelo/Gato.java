package com.veterinaria.modelo;

public class Gato extends Mascota{

    public Gato(String nombre, double peso, String duenoId) {
        super(nombre, Especie.GATO, peso, duenoId);
    }

    @Override 
    public String sonido() {
        {return "Miau miau";}
    }
    
}