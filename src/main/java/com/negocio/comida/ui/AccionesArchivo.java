package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Despacho;
import com.negocio.comida.modelo.Repositorio;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** Menú Archivo: exportar/importar los datos y reporte completo en Excel. */
final class AccionesArchivo {
    private static final String EXTENSION = "dat";
    private static final String DESCRIPCION = "Respaldo de datos del negocio";

    private final JFrame ventana;
    private final DatosNegocio datos;

    AccionesArchivo(JFrame ventana, DatosNegocio datos) {
        this.ventana = ventana;
        this.datos = datos;
    }

    /** Guarda todos los datos en un archivo para llevarlos a otra computadora. */
    void exportar() {
        String nombre = "Respaldo_negocio_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File archivo = Ui.elegirDondeGuardar(ventana, "Exportar datos (respaldo)", nombre, EXTENSION, DESCRIPCION);
        if (archivo == null) return;
        try {
            Repositorio.escribir(datos, archivo.toPath());
        } catch (IOException e) {
            Ui.error(ventana, "No se pudo exportar:\n" + e.getMessage());
            return;
        }
        JOptionPane.showMessageDialog(ventana,
                "Datos exportados en:\n" + archivo.getAbsolutePath() + "\n\n"
                        + "Para usarlos en otra computadora:\n"
                        + "  1. Copie este archivo (memoria USB, correo, nube...).\n"
                        + "  2. Instale y abra el programa en la otra computadora.\n"
                        + "  3. Use Archivo → Importar datos... y elija este archivo.",
                "Datos exportados", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Reemplaza los datos actuales por los de un respaldo (guardando antes una copia). */
    void importar() {
        File archivo = Ui.elegirParaAbrir(ventana, "Importar datos (respaldo)", EXTENSION, DESCRIPCION);
        if (archivo == null) return;
        DatosNegocio nuevos;
        try {
            nuevos = Repositorio.leer(archivo.toPath());
        } catch (IOException e) {
            Ui.error(ventana, "No se pudo leer el archivo:\n" + e.getMessage());
            return;
        }
        if (!Ui.confirmar(ventana, "El respaldo contiene:\n" + resumen(nuevos)
                + "\n\nSe REEMPLAZARÁN todos los datos actuales de este programa:\n" + resumen(datos)
                + "\n\nAntes se guardará automáticamente una copia de los datos actuales.\n¿Continuar?")) {
            return;
        }
        Path copia;
        try {
            copia = Repositorio.respaldoAutomatico(datos);
        } catch (IOException e) {
            Ui.error(ventana, "No se pudo guardar la copia de los datos actuales, no se importó nada:\n"
                    + e.getMessage());
            return;
        }
        datos.reemplazarCon(nuevos); // también guarda los datos importados
        JOptionPane.showMessageDialog(ventana, "Datos importados correctamente.\n\n"
                        + "Copia de los datos anteriores:\n" + copia
                        + "\n(si necesita volver atrás, importe ese archivo)",
                "Datos importados", JOptionPane.INFORMATION_MESSAGE);
    }

    void reporteCompleto() {
        new DialogoReporteCompleto(ventana, datos).setVisible(true);
    }

    void verCarpetaDatos() {
        JOptionPane.showMessageDialog(ventana, "Los datos se guardan automáticamente en:\n"
                + Repositorio.getArchivo().toAbsolutePath()
                + "\n\nPara llevarlos a otra computadora use Archivo → Exportar datos.",
                "Ubicación de los datos", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String resumen(DatosNegocio d) {
        String rango = d.getDespachos().isEmpty() ? "sin despachos"
                : d.getDespachos().size() + " despachos (del "
                + d.getDespachos().stream().map(Despacho::getFecha).min(LocalDate::compareTo).orElseThrow()
                        .format(PanelClientes.FECHA) + " al "
                + d.getDespachos().stream().map(Despacho::getFecha).max(LocalDate::compareTo).orElseThrow()
                        .format(PanelClientes.FECHA) + ")";
        return "  • " + d.getIngredientes().size() + " ingredientes, " + d.getLotes().size() + " compras, "
                + d.getProveedores().size() + " proveedores\n"
                + "  • " + d.getMenus().size() + " menús, " + d.getClientes().size() + " clientes\n"
                + "  • " + rango + ", " + d.getGastos().size() + " gastos";
    }
}
