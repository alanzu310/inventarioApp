package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.reporte.ReporteCompleto;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

/** Elige el periodo y genera el reporte completo del negocio en Excel. */
class DialogoReporteCompleto extends JDialog {
    private final DatosNegocio datos;
    private final JSpinner spDesde = Ui.spinnerFecha(LocalDate.now());
    private final JSpinner spHasta = Ui.spinnerFecha(LocalDate.now());

    DialogoReporteCompleto(Window padre, DatosNegocio datos) {
        super(padre, "Reporte completo en Excel", ModalityType.APPLICATION_MODAL);
        this.datos = datos;
        todoElHistorial();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        var g = Ui.gbc(0, 0);
        g.gridwidth = 4;
        form.add(new JLabel("<html>Incluye: resumen, ventas por día, despachos, gastos, clientes, menús,<br>"
                + "inventario, compras, proveedores y planificación (una hoja para cada uno).</html>"), g);
        form.add(new JLabel("Desde:"), Ui.gbc(0, 1));
        form.add(spDesde, Ui.gbc(1, 1));
        form.add(new JLabel("Hasta:"), Ui.gbc(2, 1));
        form.add(spHasta, Ui.gbc(3, 1));

        JPanel atajos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnTodo = new JButton("Todo el historial");
        JButton btnMes = new JButton("Este mes");
        JButton btnMesAnterior = new JButton("Mes anterior");
        JButton btnAnio = new JButton("Este año");
        atajos.add(btnTodo);
        atajos.add(btnMes);
        atajos.add(btnMesAnterior);
        atajos.add(btnAnio);
        btnTodo.addActionListener(e -> todoElHistorial());
        btnMes.addActionListener(e -> {
            LocalDate h = LocalDate.now();
            periodo(h.withDayOfMonth(1), h.with(TemporalAdjusters.lastDayOfMonth()));
        });
        btnMesAnterior.addActionListener(e -> {
            LocalDate m = LocalDate.now().minusMonths(1);
            periodo(m.withDayOfMonth(1), m.with(TemporalAdjusters.lastDayOfMonth()));
        });
        btnAnio.addActionListener(e -> {
            LocalDate h = LocalDate.now();
            periodo(h.withDayOfYear(1), h.with(TemporalAdjusters.lastDayOfYear()));
        });
        g = Ui.gbc(0, 2);
        g.gridwidth = 4;
        form.add(atajos, g);
        g = Ui.gbc(0, 3);
        g.gridwidth = 4;
        form.add(new JLabel("Inventario y menús muestran el estado actual."), g);

        JButton btnGenerar = new JButton("Generar Excel...");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnGenerar);
        getRootPane().setDefaultButton(btnGenerar);
        btnCancelar.addActionListener(e -> dispose());
        btnGenerar.addActionListener(e -> generar());

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(padre);
    }

    private void periodo(LocalDate desde, LocalDate hasta) {
        Ui.ponerFecha(spDesde, desde);
        Ui.ponerFecha(spHasta, hasta);
    }

    /** Desde el primer movimiento registrado hasta la última fecha con datos (o hoy). */
    private void todoElHistorial() {
        LocalDate hasta = LocalDate.now();
        for (LocalDate f : datos.getPlanificacion().keySet()) if (f.isAfter(hasta)) hasta = f;
        for (var dsp : datos.getDespachos()) if (dsp.getFecha().isAfter(hasta)) hasta = dsp.getFecha();
        for (var g : datos.getGastos()) if (g.getFecha().isAfter(hasta)) hasta = g.getFecha();
        for (var l : datos.getLotes()) if (l.getFecha().isAfter(hasta)) hasta = l.getFecha();
        LocalDate desde = datos.primeraFecha();
        for (LocalDate f : datos.getPlanificacion().keySet()) if (f.isBefore(desde)) desde = f;
        periodo(desde, hasta);
    }

    private void generar() {
        LocalDate desde = Ui.leerFecha(spDesde);
        LocalDate hasta = Ui.leerFecha(spHasta);
        if (desde.isAfter(hasta)) {
            Ui.error(this, "La fecha \"Desde\" no puede ser posterior a \"Hasta\".");
            return;
        }
        File archivo = Ui.elegirDondeGuardar(this, "Guardar reporte completo",
                ReporteCompleto.nombreArchivo(desde, hasta), "xlsx", "Libro de Excel");
        if (archivo == null) return;
        try {
            ReporteCompleto.exportar(datos, desde, hasta, archivo.toPath());
        } catch (IOException ex) {
            Ui.error(this, "No se pudo guardar el reporte:\n" + ex.getMessage()
                    + "\n\nSi el archivo está abierto en otro programa, ciérrelo e intente de nuevo.");
            return;
        }
        Ui.ofrecerAbrir(this, "Reporte completo generado", archivo);
        dispose();
    }
}
