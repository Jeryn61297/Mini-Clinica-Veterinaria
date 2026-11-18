package com.veterinaria.modelo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Clinica {
    public static final double DOSIS_MIN = 0.1;
    public static final double DOSIS_MAX = 50.0;
    private static final String CABECERA = 
    "fecha-duenioId-duenioNombre-duenioTelefono-mascota-especie-peso-diagnostico-medicamento-dosisMgKg";

    private final Map<String, Duenio> duenios = new LinkedHashMap<>();
    private final List<Mascota> mascotas = new ArrayList<>();
    private final List<Consulta> consultas = new ArrayList<>();

    // REGITRO
    public void registrarDuenio(Duenio d) {
        duenios.putIfAbsent(d.id(), d);
    }

    public Mascota registrarMascota(Especie especie, String nombre, double peso, String duenioId) {
        if (!duenios.containsKey(duenioId))
            throw new IllegalArgumentException("No existe el dueño con id " + duenioId);
        Mascota m = switch (especie) {
            case PERRO -> new Perro(nombre, peso, duenioId);
            case GATO -> new Gato(nombre, peso, duenioId);
        };
        mascotas.add(m);
        return m;
    }

    public Optional<Mascota> buscarMascota(String duenioId, String nombre) {
        return mascotas.stream()
                .filter(m -> m.getDuenioId().equals(duenioId) && m.getNombre().equalsIgnoreCase(nombre))
                .findFirst();
    }

    public Consulta registrarConsulta(Mascota m, String diagnostico, String medicamento,
            double dosisMgKg) throws DosisException {
        validarDosis(dosisMgKg);
        Consulta c = new Consulta(LocalDate.now(), m, diagnostico, medicamento, dosisMgKg);
        consultas.add(c);
        return c;
    }

    public static void validarDosis(double dosis) throws DosisException {
        if (dosis < DOSIS_MIN || dosis > DOSIS_MAX)
            throw new DosisException(
                    "Dosis %.2f mg/kg fuera de rango (%.1f - %.1f)".formatted(dosis, DOSIS_MIN, DOSIS_MAX));
    }

    public Map<String, Duenio> getDuenios() {
        return Collections.unmodifiableMap(duenios);
    }

    public List<Mascota> getMascotas() {
        return Collections.unmodifiableList(mascotas);
    }

    public List<Consulta> getConsultas() {
        return Collections.unmodifiableList(consultas);
    }

    public String reporte() {
        if (consultas.isEmpty())
            return "No hay consultas registradas.";

        var porEspecie = consultas.stream().collect(Collectors.groupingBy(
                c -> c.mascota().getEspecie(), () -> new EnumMap<>(Especie.class), Collectors.counting()));

        var dosisPromedio = consultas.stream().collect(Collectors.groupingBy(
                Consulta::medicamento, TreeMap::new, Collectors.averagingDouble(Consulta::dosisMgKg)));

        var masConsultas = consultas.stream()
                .collect(Collectors.groupingBy(c -> c.mascota().getNombre(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue());

        double pesoPromedio = mascotas.stream().mapToDouble(Mascota::getPeso).average().orElse(0);

        String detalle = consultas.stream()
                .sorted(Comparator.comparing(Consulta::fecha))
                .map(c -> "  %s | %-10s | %-15s | %-15s | %.2f mg/kg".formatted(
                        c.fecha(), c.mascota().getNombre(), c.diagnostico(), c.medicamento(), c.dosisMgKg()))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder("===== REPORTE DE LA CLINICA =====\n");
        sb.append("Dueños: ").append(duenios.size())
                .append(" | Mascotas: ").append(mascotas.size())
                .append(" | Consultas: ").append(consultas.size()).append('\n');
        sb.append("Peso promedio de mascotas: %.2f kg%n".formatted(pesoPromedio));
        sb.append("Consultas por especie: ").append(porEspecie).append('\n');
        dosisPromedio.forEach((med, prom) -> sb.append("Dosis promedio de %s: %.2f mg/kg%n".formatted(med, prom)));
        masConsultas
                .ifPresent(e -> sb.append("Mascota con más consultas: %s (%d)%n".formatted(e.getKey(), e.getValue())));
        sb.append("Detalle:\n").append(detalle);
        return sb.toString();
    }

    // ARCHIVO CSV
    public void guardarCsv(Path ruta) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(ruta)) {
            w.write(CABECERA);
            w.newLine();
            for (Consulta c : consultas) {
                Mascota m = c.mascota();
                Duenio d = duenios.get(m.getDuenioId());
                w.write(String.join(",",
                        c.fecha().toString(), esc(d.id()), esc(d.nombre()), esc(d.telefono()),
                        esc(m.getNombre()), m.getEspecie().name(), String.valueOf(m.getPeso()),
                        esc(c.diagnostico()), esc(c.medicamento()), String.valueOf(c.dosisMgKg())));
                w.newLine();
            }
        }
    }

    public void cargarCsv(Path ruta) throws IOException {
        duenios.clear();
        mascotas.clear();
        consultas.clear();
        try (BufferedReader r = Files.newBufferedReader(ruta)) {
            r.readLine(); // cabecera
            String linea;
            int nLinea = 1;
            while ((linea = r.readLine()) != null) {
                nLinea++;
                if (linea.isBlank())
                    continue;
                try {
                    List<String> f = parsear(linea);
                    if (f.size() != 10)
                        throw new IllegalArgumentException("se esperaban 10 columnas");
                    registrarDuenio(new Duenio(f.get(1), f.get(2), f.get(3)));
                    Especie esp = Especie.valueOf(f.get(5));
                    Mascota m = buscarMascota(f.get(1), f.get(4))
                            .orElseGet(() -> registrarMascota(esp, f.get(4), Double.parseDouble(f.get(6)), f.get(1)));
                    double dosis = Double.parseDouble(f.get(9));
                    validarDosis(dosis);
                    consultas.add(new Consulta(LocalDate.parse(f.get(0)), m, f.get(7), f.get(8), dosis));
                } catch (DosisException | IllegalArgumentException | java.time.DateTimeException e) {
                    System.err.println("Línea " + nLinea + " omitida: " + e.getMessage());
                }
            }
        }
    }

    private static String esc(String s) {
        s = s.replace('\n', ' ').replace('\r', ' ');
        return (s.contains(",") || s.contains("\""))
                ? "\"" + s.replace("\"", "\"\"") + "\""
                : s;
    }

    private static List<String> parsear(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (comillas) {
                if (c == '"') {
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else
                        comillas = false;
                } else
                    cur.append(c);
            } else if (c == '"')
                comillas = true;
            else if (c == ',') {
                campos.add(cur.toString());
                cur.setLength(0);
            } else
                cur.append(c);
        }
        campos.add(cur.toString());
        return campos;
    }
}