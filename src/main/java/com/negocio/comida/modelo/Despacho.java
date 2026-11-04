package com.negocio.comida.modelo;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

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
    /** De qué compras salieron los ingredientes (null en despachos de versiones anteriores). */
    private final List<Consumo> consumos;

    public Despacho(long id, LocalDate fecha, long clienteId, long menuId, String menuNombre,
                    int platos, double precioUnitario) {
        this(id, fecha, clienteId, menuId, menuNombre, platos, precioUnitario, null);
    }

    public Despacho(long id, LocalDate fecha, long clienteId, long menuId, String menuNombre,
                    int platos, double precioUnitario, List<Consumo> consumos) {
        this.id = id;
        this.fecha = fecha;
        this.clienteId = clienteId;
        this.menuId = menuId;
        this.menuNombre = menuNombre;
        this.platos = platos;
        this.precioUnitario = precioUnitario;
        this.consumos = consumos == null ? null : List.copyOf(consumos);
    }

    public long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public long getClienteId() { return clienteId; }
    public long getMenuId() { return menuId; }
    public String getMenuNombre() { return menuNombre; }
    public int getPlatos() { return platos; }
    public double getPrecioUnitario() { return precioUnitario; }
    public double getTotal() { return platos * precioUnitario; }
    /** null si el despacho es de una versión anterior (sin detalle por compra). */
    public List<Consumo> getConsumos() { return consumos; }
}
