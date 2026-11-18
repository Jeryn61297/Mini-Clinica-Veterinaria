package com.veterinaria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import com.veterinaria.modelo.Clinica;
import com.veterinaria.modelo.DosisException;
import com.veterinaria.modelo.Duenio;
import com.veterinaria.modelo.Especie;
import com.veterinaria.modelo.Mascota;

public class Main {

    private static final Path ARCHIVO = Path.of("consultas.csv");

    public static void main(String[] args) {
        Clinica clinica = new Clinica();

        if (Files.exists(ARCHIVO)) {
            try {
                clinica.cargarCsv(ARCHIVO);
                System.out.println("Datos cargados de " + ARCHIVO);
            } catch (IOException e) {
                System.err.println("No se pudo cargar: " + e.getMessage());
            }
        }

        try (Scanner in = new Scanner(System.in)) {
            int op;
            do {
                System.out.print("""

                        === MINI CLINICA VETERINARIA ===
                        1. Registrar dueño
                        2. Registrar mascota
                        3. Registrar consulta
                        4. Ver reporte
                        5. Guardar CSV
                        6. Cargar CSV
                        7. Listar Dueños
                        8. Listar mascotas
                        0. Salir
                        Opcion: """);
                op = leerEntero(in);
                try {
                    switch (op) {
                        case 1 -> {
                            String id = pedir(in, "Id del dueño: ");
                            clinica.registrarDuenio(new Duenio(id, pedir(in, "Nombre: "), pedir(in, "Teléfono: ")));
                            System.out.println("Dueño registrado.");
                        }
                        case 2 -> {
                            String idD = pedir(in, "Id del dueño: ");
                            String nombre = pedir(in, "Nombre de la mascota: ");
                            Especie esp = Especie.valueOf(pedir(in, "Especie (PERRO/GATO): ").toUpperCase());
                            double peso = Double.parseDouble(pedir(in, "Peso (kg): "));
                            Mascota m = clinica.registrarMascota(esp, nombre, peso, idD);
                            System.out.println("Registrada: " + m);
                        }
                        case 3 -> {
                            String idD = pedir(in, "Id del dueño: ");
                            String nombre = pedir(in, "Nombre de la mascota: ");
                            var mascota = clinica.buscarMascota(idD, nombre);
                            if (mascota.isEmpty()) {
                                System.out.println("Mascota no encontrada.");
                                break;
                            }
                            String diag = pedir(in, "Diagnóstico: ");
                            String med = pedir(in, "Medicamento: ");
                            double dosis = Double.parseDouble(pedir(in, "Dosis (mg/kg): "));
                            clinica.registrarConsulta(mascota.get(), diag, med, dosis);
                            System.out.println("Consulta registrada.");
                        }
                        case 4 -> System.out.println(clinica.reporte());
                        case 5 -> {
                            clinica.guardarCsv(ARCHIVO);
                            System.out.println("Archivo guardado en: " + ARCHIVO);
                        }
                        case 6 -> {
                            clinica.cargarCsv(ARCHIVO);
                            System.out.println("Archivo cargado de: " + ARCHIVO);
                        }
                        case 7 -> {
                            if (clinica.getDuenios().isEmpty()) System.out.println("No hay dueños registrados.");
                            else clinica.getDuenios().values().forEach(d ->
                                    System.out.println("  [%s] %s - Tel: %s".formatted(d.id(), d.nombre(), d.telefono())));
                        }
                        case 8 -> {
                            if (clinica.getMascotas().isEmpty()) System.out.println("No hay mascotas registradas.");
                            else clinica.getMascotas().forEach(m -> {
                                var d = clinica.getDuenios().get(m.getDuenioId());
                                System.out.println("  %s | Dueño: %s".formatted(m, d != null ? d.nombre() : m.getDuenioId()));
                            });
                        }
                        case 0 -> System.out.println("Vuelva pronto");
                        default -> System.out.println("Opción inválida.");
                    }
                } catch (DosisException e) {
                    System.out.println("Dosis inválida: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.out.println("Dato inválido: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Error de archivo: " + e.getMessage());
                }
            } while (op != 0);
        }
    }

    private static String pedir(Scanner in, String msg) {
        System.out.print(msg);
        return in.nextLine().trim();
    }

    private static int leerEntero(Scanner in) {
        try {
            return Integer.parseInt(in.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}