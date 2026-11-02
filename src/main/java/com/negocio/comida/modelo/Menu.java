package com.negocio.comida.modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Plato/menú armado con ingredientes del inventario. */
public class Menu implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private String nombre;
    private double precioVenta;
    private final List<ItemMenu> items = new ArrayList<>();

    public Menu(long id, String nombre, double precioVenta) {
        this.id = id;
        this.nombre = nombre;
        this.precioVenta = precioVenta;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }
    public List<ItemMenu> getItems() { return items; }

    @Override
    public String toString() {
        return nombre;
    }
}
