package com.veterinaria.modelo;


public abstract class Mascota{

    private final String nombre;
    private final Especie especie;
    private final String duenioId;
    private double peso; 

    protected Mascota(String nombre, Especie especie, double peso, String duenioId) {
        this.nombre = nombre;
        this.especie = especie;
        this.duenioId = duenioId;
        setPeso(peso);
    }

    public abstract String sonido();

    public String getNombre()   { return nombre; }
    public Especie getEspecie() { return especie; }
    public String getDuenioId() { return duenioId; }
    public double getPeso()     { return peso; }

    public void setPeso(double peso) {
        if (peso <= 0) throw new IllegalArgumentException("El peso debe ser mayor de 0");
        this.peso = peso;
    }

    @Override
    public String toString() {
        return "%s (%s, %.1f kg) dice %s".formatted(nombre, especie, peso, sonido());
    }

    
}