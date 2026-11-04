package com.negocio.comida.modelo;

import java.io.Serializable;

/**
 * Ingrediente del inventario (por ejemplo: Arroz, en libras). No tiene precio
 * propio: la existencia y el costo salen de sus compras ({@link Lote}).
 */
public class Ingrediente implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private String nombre;
    private Unidad unidad;
    /** Solo para leer datos de versiones anteriores (se convierten en una compra). */
    private double cantidad;
    /** Solo para leer datos de versiones anteriores (se convierten en una compra). */
    private double precioPorUnidad;

    public Ingrediente(long id, String nombre, Unidad unidad) {
        this.id = id;
        this.nombre = nombre;
        this.unidad = unidad;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Unidad getUnidad() { return unidad; }
    public void setUnidad(Unidad unidad) { this.unidad = unidad; }

    double cantidadAnterior() { return cantidad; }
    double precioAnterior() { return precioPorUnidad; }

    void limpiarDatosAnteriores() {
        cantidad = 0;
        precioPorUnidad = 0;
    }

    @Override
    public String toString() {
        return nombre + " (" + unidad.getAbreviatura() + ")";
    }
}
