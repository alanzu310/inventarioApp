package com.negocio.comida.modelo;

import java.io.Serializable;

/** Cantidad tomada de una compra (lote) para un despacho, con su costo. */
public class Consumo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long loteId;
    private final double cantidad;
    private final double costo;

    public Consumo(long loteId, double cantidad, double costo) {
        this.loteId = loteId;
        this.cantidad = cantidad;
        this.costo = costo;
    }

    public long getLoteId() { return loteId; }
    public double getCantidad() { return cantidad; }
    public double getCosto() { return costo; }
}
