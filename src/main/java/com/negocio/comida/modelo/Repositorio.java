package com.negocio.comida.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Guarda y carga los datos en un archivo dentro de la carpeta del usuario. */
public final class Repositorio {

    private static final Path ARCHIVO = Paths.get(
            System.getProperty("negocio.datos",
                    Paths.get(System.getProperty("user.home"), ".negocio-comida", "datos.dat").toString()));

    private Repositorio() {
    }

    public static Path getArchivo() {
        return ARCHIVO;
    }

    public static DatosNegocio cargar() {
        if (!Files.exists(ARCHIVO)) return new DatosNegocio();
        try (InputStream in = Files.newInputStream(ARCHIVO);
             ObjectInputStream ois = new ObjectInputStream(in)) {
            return (DatosNegocio) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // Se guarda una copia del archivo ilegible para no perderlo al guardar de nuevo.
            Path copia = ARCHIVO.resolveSibling(ARCHIVO.getFileName() + ".ilegible-" + System.currentTimeMillis());
            try {
                Files.copy(ARCHIVO, copia);
            } catch (IOException ignored) {
                // si tampoco se puede copiar, se continúa
            }
            System.err.println("No se pudieron leer los datos (" + e + "). Copia en " + copia + ". Se inicia vacío.");
            return new DatosNegocio();
        }
    }

    public static void guardar(DatosNegocio datos) throws IOException {
        Files.createDirectories(ARCHIVO.getParent());
        Path tmp = ARCHIVO.resolveSibling(ARCHIVO.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp);
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(datos);
        }
        Files.move(tmp, ARCHIVO, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
