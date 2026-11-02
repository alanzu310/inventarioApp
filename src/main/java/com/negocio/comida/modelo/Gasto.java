package com.negocio.comida.modelo;

import java.io.Serializable;
import java.time.LocalDate;

public class Gasto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final LocalDate fecha;
    private final TipoGasto tipo;
    private final String descripcion;
    private final double monto;

    public Gasto(long id, LocalDate fecha, TipoGasto tipo, String descripcion, double monto) {
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.monto = monto;
    }

    public long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public TipoGasto getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public double getMonto() { return monto; }
}
