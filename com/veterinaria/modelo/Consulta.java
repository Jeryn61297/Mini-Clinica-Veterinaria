package com.veterinaria.modelo;

import java.time.LocalDate;

public record Consulta(
    LocalDate fecha, Mascota mascota, String diagnostico, 
    String medicamento, double dosisMgKg) {

}