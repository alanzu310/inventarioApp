package com.negocio.comida.modelo;

import java.io.Serializable;

/** Cantidad de un ingrediente que lleva UN plato de un menú. */
public class ItemMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long ingredienteId;
    private double cantidad;

    public ItemMenu(long ingredienteId, double cantidad) {
        this.ingredienteId = ingredienteId;
        this.cantidad = cantidad;
    }

    public long getIngredienteId() { return ingredienteId; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
}
