package com.negocio.comida.modelo;

import java.io.Serializable;

public class Proveedor implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private String nombre;
    private String telefono;

    public Proveedor(long id, String nombre, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    @Override
    public String toString() {
        return nombre;
    }
}
