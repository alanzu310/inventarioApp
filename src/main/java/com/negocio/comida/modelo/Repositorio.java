package com.negocio.comida.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Guarda y carga los datos en un archivo dentro de la carpeta del usuario, y
 * exporta/importa respaldos para llevar los datos a otra computadora.
 */
public final class Repositorio {

    private static final Path ARCHIVO = Paths.get(
            System.getProperty("negocio.datos",
                    Paths.get(System.getProperty("user.home"), ".negocio-comida", "datos.dat").toString()));

    /** Solo se aceptan clases de la aplicación y tipos básicos de Java al leer un archivo. */
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "maxdepth=50;com.negocio.comida.modelo.*;java.lang.*;java.util.*;java.time.*;!*");

    private Repositorio() {
    }

    public static Path getArchivo() {
        return ARCHIVO;
    }

    /** Carga los datos de la aplicación (vacíos si es la primera vez). */
    public static DatosNegocio cargar() {
        if (!Files.exists(ARCHIVO)) return new DatosNegocio();
        try {
            return leer(ARCHIVO);
        } catch (IOException e) {
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

    /**
     * Lee un archivo de datos o de respaldo.
     *
     * @throws IOException con un mensaje entendible si el archivo no es un respaldo válido
     */
    public static DatosNegocio leer(Path archivo) throws IOException {
        try (InputStream in = Files.newInputStream(archivo);
             ObjectInputStream ois = new ObjectInputStream(in)) {
            ois.setObjectInputFilter(FILTRO);
            Object o = ois.readObject();
            if (!(o instanceof DatosNegocio d)) throw new IOException("El archivo no contiene datos del negocio.");
            return d;
        } catch (StreamCorruptedException | ClassNotFoundException | InvalidClassException | ClassCastException e) {
            throw new IOException("El archivo no es un respaldo válido de esta aplicación.", e);
        }
    }

    /** Guarda los datos de la aplicación. */
    public static void guardar(DatosNegocio datos) throws IOException {
        escribir(datos, ARCHIVO);
    }

    /**
     * Escribe los datos en cualquier archivo (se usa también para exportar). Se
     * escribe primero en un temporal para no dejar el archivo a medias.
     */
    public static void escribir(DatosNegocio datos, Path archivo) throws IOException {
        Path destino = archivo.toAbsolutePath();
        if (destino.getParent() != null) Files.createDirectories(destino.getParent());
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp);
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(datos);
        }
        try {
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING); // p. ej. memorias USB
        }
    }

    /** Copia de seguridad automática de los datos actuales (antes de importar). */
    public static Path respaldoAutomatico(DatosNegocio datos) throws IOException {
        String marca = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path p = ARCHIVO.resolveSibling("respaldo-antes-de-importar_" + marca + ".dat");
        escribir(datos, p);
        return p;
    }
}
