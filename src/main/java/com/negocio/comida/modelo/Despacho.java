package com.negocio.comida.modelo;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Platos entregados a un cliente en una fecha. Guarda copia del nombre y el
 * precio para que el historial no cambie si luego se edita o borra el menú.
 */
public class Despacho implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final LocalDate fecha;
    private final long clienteId;
    private final long menuId;
    private final String menuNombre;
    private final int platos;
    private final double precioUnitario;

    public Despacho(long id, LocalDate fecha, long clienteId, long menuId, String menuNombre,
                    int platos, double precioUnitario) {
        this.id = id;
        this.fecha = fecha;
        this.clienteId = clienteId;
        this.menuId = menuId;
        this.menuNombre = menuNombre;
        this.platos = platos;
        this.precioUnitario = precioUnitario;
    }

    public long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public long getClienteId() { return clienteId; }
    public long getMenuId() { return menuId; }
    public String getMenuNombre() { return menuNombre; }
    public int getPlatos() { return platos; }
    public double getPrecioUnitario() { return precioUnitario; }
    public double getTotal() { return platos * precioUnitario; }
}
