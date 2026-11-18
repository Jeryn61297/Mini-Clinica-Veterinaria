package com.veterinaria.modelo;

public class Perro extends Mascota {
    public Perro(String nombre, double peso, String duenioId) {
        super(nombre, Especie.PERRO, peso, duenioId);
    }

    @Override
    public String sonido() {
        return "Guau guau";
    }
}