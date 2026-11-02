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
    /** Despacho que originó el gasto (costo de ingredientes); 0 si se registró a mano. */
    private final long despachoId;

    public Gasto(long id, LocalDate fecha, TipoGasto tipo, String descripcion, double monto) {
        this(id, fecha, tipo, descripcion, monto, 0);
    }

    public Gasto(long id, LocalDate fecha, TipoGasto tipo, String descripcion, double monto, long despachoId) {
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.despachoId = despachoId;
    }

    public long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public TipoGasto getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public double getMonto() { return monto; }
    public long getDespachoId() { return despachoId; }

    /** true si lo generó un despacho (no se puede borrar a mano). */
    public boolean esAutomatico() {
        return despachoId != 0;
    }
}
