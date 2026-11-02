package com.negocio.comida.modelo;

/** Categorías de gasto diario del negocio. */
public enum TipoGasto {
    COMIDA("Comida"),
    COMBUSTIBLE("Combustible"),
    GAS("Gas de cocina");

    private final String nombre;

    TipoGasto(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
