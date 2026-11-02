package com.negocio.comida.ui;

import com.negocio.comida.modelo.Cliente;
import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.reporte.ExportadorExcel;
import com.negocio.comida.reporte.ExportadorPdf;
import com.negocio.comida.reporte.ReporteVentas;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Diálogo para exportar el reporte de ventas a PDF o Excel. */
class DialogoReporte extends JDialog {
    private static final String TODOS = "Todos los clientes";

    private final DatosNegocio datos;
    private final JComboBox<Object> cmbCliente;
    private final JSpinner spDesde = spinnerFecha();
    private final JSpinner spHasta = spinnerFecha();
    private final JRadioButton rbPdf = new JRadioButton("PDF (para imprimir o enviar)", true);
    private final JRadioButton rbExcel = new JRadioButton("Excel .xlsx (para que el contador sume y filtre)");
    private final JTextField txtNegocio = new JTextField(24);
    private final JLabel lblVista = new JLabel(" ");

    DialogoReporte(Window padre, DatosNegocio datos, Cliente seleccionado) {
        super(padre, "Reporte de ventas", ModalityType.APPLICATION_MODAL);
        this.datos = datos;

        DefaultComboBoxModel<Object> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(TODOS);
        for (Cliente c : datos.getClientes()) modelo.addElement(c);
        cmbCliente = new JComboBox<>(modelo);
        cmbCliente.setSelectedItem(seleccionado != null ? seleccionado : TODOS);

        LocalDate hoy = LocalDate.now();
        ponerPeriodo(hoy.withDayOfMonth(1), hoy.with(TemporalAdjusters.lastDayOfMonth()));
        txtNegocio.setText(datos.getNombreNegocio());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        int f = 0;
        form.add(new JLabel("Cliente:"), Ui.gbc(0, f));
        var g = Ui.gbc(1, f++);
        g.gridwidth = 3;
        form.add(cmbCliente, g);

        form.add(new JLabel("Desde:"), Ui.gbc(0, f));
        form.add(spDesde, Ui.gbc(1, f));
        form.add(new JLabel("Hasta:"), Ui.gbc(2, f));
        form.add(spHasta, Ui.gbc(3, f++));

        JPanel atajos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnSemana = new JButton("Esta semana");
        JButton btnQuincena = new JButton("Quincena actual");
        JButton btnMes = new JButton("Este mes");
        JButton btnMesAnterior = new JButton("Mes anterior");
        atajos.add(btnSemana);
        atajos.add(btnQuincena);
        atajos.add(btnMes);
        atajos.add(btnMesAnterior);
        btnSemana.addActionListener(e -> {
            LocalDate lunes = DatosNegocio.inicioSemana(LocalDate.now());
            ponerPeriodo(lunes, lunes.plusDays(6));
        });
        btnQuincena.addActionListener(e -> {
            LocalDate h = LocalDate.now();
            if (h.getDayOfMonth() <= 15) ponerPeriodo(h.withDayOfMonth(1), h.withDayOfMonth(15));
            else ponerPeriodo(h.withDayOfMonth(16), h.with(TemporalAdjusters.lastDayOfMonth()));
        });
        btnMes.addActionListener(e -> {
            LocalDate h = LocalDate.now();
            ponerPeriodo(h.withDayOfMonth(1), h.with(TemporalAdjusters.lastDayOfMonth()));
        });
        btnMesAnterior.addActionListener(e -> {
            LocalDate m = LocalDate.now().minusMonths(1);
            ponerPeriodo(m.withDayOfMonth(1), m.with(TemporalAdjusters.lastDayOfMonth()));
        });
        g = Ui.gbc(1, f++);
        g.gridwidth = 3;
        form.add(atajos, g);

        form.add(new JLabel("Formato:"), Ui.gbc(0, f));
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbPdf);
        grupo.add(rbExcel);
        JPanel formatos = new JPanel(new java.awt.GridLayout(2, 1));
        formatos.add(rbPdf);
        formatos.add(rbExcel);
        g = Ui.gbc(1, f++);
        g.gridwidth = 3;
        form.add(formatos, g);

        form.add(new JLabel("Nombre del negocio:"), Ui.gbc(0, f));
        g = Ui.gbc(1, f++);
        g.gridwidth = 3;
        txtNegocio.setToolTipText("Aparece en el encabezado del reporte (opcional)");
        form.add(txtNegocio, g);

        lblVista.setFont(Ui.negrita(lblVista.getFont(), 13f));
        g = Ui.gbc(0, f);
        g.gridwidth = 4;
        g.insets = new java.awt.Insets(12, 4, 4, 4);
        form.add(lblVista, g);

        JButton btnGenerar = new JButton("Generar reporte...");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnGenerar);
        btnCancelar.addActionListener(e -> dispose());
        btnGenerar.addActionListener(e -> generar());
        getRootPane().setDefaultButton(btnGenerar);

        cmbCliente.addActionListener(e -> actualizarVista());
        spDesde.addChangeListener(e -> actualizarVista());
        spHasta.addChangeListener(e -> actualizarVista());

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        actualizarVista();
        pack();
        setResizable(false);
        setLocationRelativeTo(padre);
    }

    private static JSpinner spinnerFecha() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        return s;
    }

    private static LocalDate leer(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void ponerPeriodo(LocalDate desde, LocalDate hasta) {
        spDesde.setValue(Date.from(desde.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        spHasta.setValue(Date.from(hasta.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private Cliente cliente() {
        Object o = cmbCliente.getSelectedItem();
        return o instanceof Cliente c ? c : null;
    }

    private ReporteVentas construir() {
        return new ReporteVentas(datos, cliente(), leer(spDesde), leer(spHasta));
    }

    private void actualizarVista() {
        if (leer(spDesde).isAfter(leer(spHasta))) {
            lblVista.setText("La fecha \"Desde\" es posterior a \"Hasta\".");
            return;
        }
        ReporteVentas r = construir();
        lblVista.setText(r.totalPlatos() + " platos  |  " + r.totalLineas() + " despachos  |  Total a pagar: "
                + ReporteVentas.dinero(r.totalVentas()));
    }

    private void generar() {
        if (leer(spDesde).isAfter(leer(spHasta))) {
            Ui.error(this, "La fecha \"Desde\" no puede ser posterior a \"Hasta\".");
            return;
        }
        datos.setNombreNegocio(txtNegocio.getText());
        ReporteVentas r = construir();
        if (r.totalLineas() == 0 && !Ui.confirmar(this,
                "No hay ventas en ese periodo. ¿Generar el reporte de todos modos?")) {
            return;
        }

        boolean pdf = rbPdf.isSelected();
        String ext = pdf ? "pdf" : "xlsx";
        Preferences prefs = Preferences.userNodeForPackage(DialogoReporte.class);
        JFileChooser fc = new JFileChooser(prefs.get("carpetaReportes", System.getProperty("user.home")));
        fc.setDialogTitle("Guardar reporte");
        fc.setFileFilter(pdf ? new FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf")
                : new FileNameExtensionFilter("Libro de Excel (*.xlsx)", "xlsx"));
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), r.nombreArchivo() + "." + ext));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith("." + ext)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + "." + ext);
        }
        if (archivo.exists() && !Ui.confirmar(this, archivo.getName() + " ya existe. ¿Reemplazarlo?")) return;

        try {
            if (pdf) ExportadorPdf.exportar(r, archivo.toPath());
            else ExportadorExcel.exportar(r, archivo.toPath());
        } catch (IOException ex) {
            Ui.error(this, "No se pudo guardar el reporte:\n" + ex.getMessage()
                    + "\n\nSi el archivo está abierto en otro programa, ciérrelo e intente de nuevo.");
            return;
        }
        if (archivo.getParentFile() != null) prefs.put("carpetaReportes", archivo.getParent());

        int op = JOptionPane.showOptionDialog(this, "Reporte guardado en:\n" + archivo.getAbsolutePath(),
                "Reporte generado", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{"Abrir reporte", "Cerrar"}, "Abrir reporte");
        if (op == 0) abrir(archivo);
        dispose();
    }

    private void abrir(File archivo) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(archivo);
                return;
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // se informa abajo
        }
        Ui.error(this, "No se pudo abrir automáticamente. Ábralo desde:\n" + archivo.getAbsolutePath());
    }
}
