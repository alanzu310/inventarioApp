package com.negocio.comida.modelo;

/** Unidad de medida con la que se compra y se descuenta un ingrediente. */
public enum Unidad {
    UNIDAD("Unidad", "und"),
    LIBRA("Libra", "lb"),
    KILO("Kilo", "kg");

    private final String nombre;
    private final String abreviatura;

    Unidad(String nombre, String abreviatura) {
        this.nombre = nombre;
        this.abreviatura = abreviatura;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
