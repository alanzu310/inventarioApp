package com.negocio.comida.modelo;

import java.util.List;

/** Categorías de gasto diario del negocio. */
public enum TipoGasto {
    OTROS("Otros"),
    COMBUSTIBLE("Combustible"),
    GAS("Gas de cocina"),
    /** Generado automáticamente al despachar platos (costo de los ingredientes usados). */
    INGREDIENTES("Costo de ingredientes"),
    /**
     * Ya no se usa: existía en versiones anteriores. Se conserva solo para poder
     * leer datos guardados antes; al cargarlos se convierte en {@link #OTROS}.
     */
    @Deprecated
    COMIDA("Otros");

    private final String nombre;

    TipoGasto(String nombre) {
        this.nombre = nombre;
    }

    /** Tipos que el usuario puede registrar a mano en "Gastos del día". */
    public static List<TipoGasto> manuales() {
        return List.of(OTROS, COMBUSTIBLE, GAS);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
