package com.negocio.comida.modelo;

import java.io.Serializable;

/** Ingrediente del inventario. Cantidad y precio se expresan en su unidad. */
public class Ingrediente implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private String nombre;
    private Unidad unidad;
    private double cantidad;
    private double precioPorUnidad;

    public Ingrediente(long id, String nombre, Unidad unidad, double cantidad, double precioPorUnidad) {
        this.id = id;
        this.nombre = nombre;
        this.unidad = unidad;
        this.cantidad = cantidad;
        this.precioPorUnidad = precioPorUnidad;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Unidad getUnidad() { return unidad; }
    public void setUnidad(Unidad unidad) { this.unidad = unidad; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public double getPrecioPorUnidad() { return precioPorUnidad; }
    public void setPrecioPorUnidad(double precioPorUnidad) { this.precioPorUnidad = precioPorUnidad; }

    public double getValorTotal() {
        return cantidad * precioPorUnidad;
    }

    @Override
    public String toString() {
        return nombre + " (" + unidad.getAbreviatura() + ")";
    }
}
