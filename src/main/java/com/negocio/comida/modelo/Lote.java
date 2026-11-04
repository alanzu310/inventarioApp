package com.negocio.comida.modelo;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Una compra de un ingrediente a un proveedor. Cada compra conserva su propio
 * precio; al despachar se consume primero la compra más antigua (PEPS).
 */
public class Lote implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final long ingredienteId;
    /** 0 = sin proveedor (datos de versiones anteriores). */
    private long proveedorId;
    private LocalDate fecha;
    private double cantidadComprada;
    private double precioPorUnidad;
    private double cantidadRestante;

    Lote(long id, long ingredienteId, long proveedorId, LocalDate fecha, double cantidad, double precioPorUnidad) {
        this.id = id;
        this.ingredienteId = ingredienteId;
        this.proveedorId = proveedorId;
        this.fecha = fecha;
        this.cantidadComprada = cantidad;
        this.precioPorUnidad = precioPorUnidad;
        this.cantidadRestante = cantidad;
    }

    public long getId() { return id; }
    public long getIngredienteId() { return ingredienteId; }
    public long getProveedorId() { return proveedorId; }
    public LocalDate getFecha() { return fecha; }
    public double getCantidadComprada() { return cantidadComprada; }
    public double getPrecioPorUnidad() { return precioPorUnidad; }
    public double getCantidadRestante() { return cantidadRestante; }

    /** Cantidad que ya salió de esta compra (despachos o ajustes). */
    public double getCantidadUsada() {
        return cantidadComprada - cantidadRestante;
    }

    public double getTotalCompra() {
        return cantidadComprada * precioPorUnidad;
    }

    public double getValorRestante() {
        return cantidadRestante * precioPorUnidad;
    }

    void setProveedorId(long proveedorId) { this.proveedorId = proveedorId; }
    void setFecha(LocalDate fecha) { this.fecha = fecha; }
    void setCantidadComprada(double c) { this.cantidadComprada = c; }
    void setPrecioPorUnidad(double p) { this.precioPorUnidad = p; }
    void setCantidadRestante(double c) { this.cantidadRestante = c; }
}
