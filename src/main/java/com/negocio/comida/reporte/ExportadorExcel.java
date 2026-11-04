package com.negocio.comida.reporte;

import static com.negocio.comida.reporte.Xlsx.CABECERA;
import static com.negocio.comida.reporte.Xlsx.CABECERA_DER;
import static com.negocio.comida.reporte.Xlsx.DINERO;
import static com.negocio.comida.reporte.Xlsx.FECHA;
import static com.negocio.comida.reporte.Xlsx.NEGRITA;
import static com.negocio.comida.reporte.Xlsx.TITULO;
import static com.negocio.comida.reporte.Xlsx.TOTAL_DINERO;
import static com.negocio.comida.reporte.Xlsx.TOTAL_ETIQUETA;
import static com.negocio.comida.reporte.Xlsx.TOTAL_NUMERO;

import com.negocio.comida.reporte.ReporteVentas.Grupo;
import com.negocio.comida.reporte.ReporteVentas.Linea;
import com.negocio.comida.reporte.Xlsx.Hoja;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Reporte de ventas para el contador en Excel (.xlsx): hoja "Detalle" con cada
 * despacho y hoja "Resumen" por cliente.
 */
public final class ExportadorExcel {

    private static final int FILA_CABECERA = 7;

    private ExportadorExcel() {
    }

    public static void exportar(ReporteVentas r, Path archivo) throws IOException {
        Xlsx.escribir(archivo, List.of(hojaDetalle(r), hojaResumen(r)));
    }

    private static Hoja hojaDetalle(ReporteVentas r) {
        Hoja h = new Hoja("Detalle");
        encabezado(h, r);
        int f = FILA_CABECERA;
        h.texto("A", f, "Cliente", CABECERA);
        h.texto("B", f, "Fecha", CABECERA);
        h.texto("C", f, "Menú", CABECERA);
        h.texto("D", f, "Platos", CABECERA_DER);
        h.texto("E", f, "Precio unitario", CABECERA_DER);
        h.texto("F", f, "Total", CABECERA_DER);
        h.finFila();
        for (Grupo g : r.getGrupos()) {
            for (Linea l : g.lineas) {
                f++;
                h.texto("A", f, g.cliente.getNombre(), 0);
                h.fecha("B", f, l.fecha, FECHA);
                h.texto("C", f, l.menu, 0);
                h.numero("D", f, l.platos, 0);
                h.numero("E", f, l.precio, DINERO);
                h.formula("F", f, "D" + f + "*E" + f, l.total, DINERO);
                h.finFila();
            }
        }
        int primera = FILA_CABECERA + 1;
        int ultima = Math.max(f, primera);
        if (f == FILA_CABECERA) {
            f++;
            h.texto("A", f, "No hay ventas registradas en el periodo.", 0);
            h.finFila();
        }
        f += 2;
        h.texto("C", f, "TOTAL A PAGAR", TOTAL_ETIQUETA);
        h.formula("D", f, "SUM(D" + primera + ":D" + ultima + ")", r.totalPlatos(), TOTAL_NUMERO);
        h.texto("E", f, "", TOTAL_ETIQUETA);
        h.formula("F", f, "SUM(F" + primera + ":F" + ultima + ")", r.totalVentas(), TOTAL_DINERO);
        h.finFila();
        return h.anchos(30, 13, 34, 10, 16, 16).fijarFilas(FILA_CABECERA).filtro("A" + FILA_CABECERA + ":F" + ultima);
    }

    private static Hoja hojaResumen(ReporteVentas r) {
        Hoja h = new Hoja("Resumen");
        encabezado(h, r);
        int f = FILA_CABECERA;
        h.texto("A", f, "Cliente", CABECERA);
        h.texto("B", f, "Teléfono", CABECERA);
        h.texto("C", f, "Días con despacho", CABECERA_DER);
        h.texto("D", f, "Platos", CABECERA_DER);
        h.texto("E", f, "Total", CABECERA_DER);
        h.finFila();
        for (Grupo g : r.getGrupos()) {
            f++;
            h.texto("A", f, g.cliente.getNombre(), 0);
            h.texto("B", f, g.cliente.getTelefono() == null ? "" : g.cliente.getTelefono(), 0);
            h.numero("C", f, g.lineas.stream().map(l -> l.fecha).distinct().count(), 0);
            h.numero("D", f, g.platos(), 0);
            h.numero("E", f, g.total(), DINERO);
            h.finFila();
        }
        int primera = FILA_CABECERA + 1;
        int ultima = Math.max(f, primera);
        f += 2;
        h.texto("A", f, "TOTAL A PAGAR", TOTAL_ETIQUETA);
        h.texto("B", f, "", TOTAL_ETIQUETA);
        h.texto("C", f, "", TOTAL_ETIQUETA);
        h.formula("D", f, "SUM(D" + primera + ":D" + ultima + ")", r.totalPlatos(), TOTAL_NUMERO);
        h.formula("E", f, "SUM(E" + primera + ":E" + ultima + ")", r.totalVentas(), TOTAL_DINERO);
        h.finFila();
        return h.anchos(30, 16, 18, 10, 16).fijarFilas(FILA_CABECERA);
    }

    private static void encabezado(Hoja h, ReporteVentas r) {
        String negocio = r.getNegocio();
        h.texto("A", 1, negocio.isEmpty() ? r.titulo() : negocio, TITULO);
        h.finFila();
        if (!negocio.isEmpty()) {
            h.texto("A", 2, r.titulo(), NEGRITA);
            h.finFila();
        }
        h.texto("A", 3, "Cliente:", NEGRITA);
        h.texto("B", 3, r.descripcionClientes(), 0);
        h.finFila();
        h.texto("A", 4, "Periodo:", NEGRITA);
        h.texto("B", 4, r.periodo(), 0);
        h.finFila();
        h.texto("A", 5, "Fecha de emisión:", NEGRITA);
        h.fecha("B", 5, r.getEmision(), FECHA);
        h.finFila();
    }
}
