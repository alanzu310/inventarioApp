package com.negocio.comida.ui;

import com.negocio.comida.modelo.Cliente;
import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Despacho;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Pestaña Clientes: registro de clientes y reporte de platos despachados por día. */
class PanelClientes extends JPanel {
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DatosNegocio datos;

    private final JTextField txtNombre = new JTextField(16);
    private final JTextField txtTelefono = new JTextField(10);
    private final JTextField txtDireccion = new JTextField(20);
    private final JButton btnGuardar = new JButton("Agregar cliente");

    private final DefaultTableModel modeloClientes =
            Ui.modelo("Cliente", "Teléfono", "Dirección", "Platos hoy", "Platos totales");
    private final JTable tablaClientes = Ui.tabla(modeloClientes);
    private List<Cliente> clientes = List.of();
    private Cliente editando;

    private final JLabel lblReporte = new JLabel("Seleccione un cliente para ver su reporte");
    private final DefaultTableModel modeloDiario = Ui.modelo("Fecha", "Platos despachados", "Total $");
    private final JTable tablaDiario = Ui.tabla(modeloDiario);
    private final DefaultTableModel modeloDetalle = Ui.modelo("Fecha", "Menú", "Platos", "Precio", "Total");
    private final JTable tablaDetalle = Ui.tabla(modeloDetalle);
    private final JLabel lblTotales = new JLabel(" ");

    PanelClientes(DatosNegocio datos) {
        super(new BorderLayout(8, 8));
        this.datos = datos;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = Ui.titulo(new JPanel(new GridBagLayout()), "Datos del cliente");
        form.add(new JLabel("Nombre:"), Ui.gbc(0, 0));
        form.add(txtNombre, Ui.gbc(1, 0));
        form.add(new JLabel("Teléfono:"), Ui.gbc(2, 0));
        form.add(txtTelefono, Ui.gbc(3, 0));
        form.add(new JLabel("Dirección:"), Ui.gbc(4, 0));
        form.add(txtDireccion, Ui.gbc(5, 0));
        JButton btnLimpiar = new JButton("Nuevo / Limpiar");
        JButton btnEliminar = new JButton("Eliminar cliente");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.add(btnGuardar);
        botones.add(btnLimpiar);
        botones.add(btnEliminar);
        var g = Ui.gbc(0, 1);
        g.gridwidth = 6;
        form.add(botones, g);
        btnGuardar.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnEliminar.addActionListener(e -> eliminar());

        JPanel lista = Ui.titulo(new JPanel(new BorderLayout()), "Clientes");
        lista.add(new JScrollPane(tablaClientes));
        tablaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionar();
        });

        JPanel reporte = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Reporte de platos despachados");
        lblReporte.setFont(Ui.negrita(lblReporte.getFont(), 14f));
        reporte.add(lblReporte, BorderLayout.NORTH);
        JSplitPane tablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Ui.titulo(new JScrollPane(tablaDiario), "Platos por día"),
                Ui.titulo(new JScrollPane(tablaDetalle), "Detalle de despachos"));
        tablas.setResizeWeight(0.5);
        reporte.add(tablas, BorderLayout.CENTER);
        lblTotales.setFont(Ui.negrita(lblTotales.getFont(), 13f));
        reporte.add(lblTotales, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lista, reporte);
        split.setResizeWeight(0.55);

        add(form, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        datos.agregarOyente(this::refrescar);
        refrescar();
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) { Ui.error(this, "Escriba el nombre del cliente."); return; }
        boolean duplicado = datos.getClientes().stream()
                .anyMatch(c -> c != editando && c.getNombre().equalsIgnoreCase(nombre));
        if (duplicado) { Ui.error(this, "Ya existe un cliente con ese nombre."); return; }
        if (editando == null) {
            datos.agregarCliente(nombre, txtTelefono.getText().trim(), txtDireccion.getText().trim());
        } else {
            datos.actualizarCliente(editando, nombre, txtTelefono.getText().trim(), txtDireccion.getText().trim());
        }
        limpiar();
    }

    private void eliminar() {
        if (editando == null) { Ui.error(this, "Seleccione un cliente."); return; }
        if (Ui.confirmar(this, "¿Eliminar a " + editando.getNombre()
                + "?\nTambién se borrará su historial de despachos.")) {
            datos.eliminarCliente(editando);
            limpiar();
        }
    }

    private void limpiar() {
        editando = null;
        tablaClientes.clearSelection();
        txtNombre.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        btnGuardar.setText("Agregar cliente");
        mostrarReporte();
    }

    private void seleccionar() {
        int r = tablaClientes.getSelectedRow();
        if (r < 0) return;
        editando = clientes.get(r);
        txtNombre.setText(editando.getNombre());
        txtTelefono.setText(editando.getTelefono());
        txtDireccion.setText(editando.getDireccion());
        btnGuardar.setText("Actualizar cliente");
        mostrarReporte();
    }

    private void mostrarReporte() {
        modeloDiario.setRowCount(0);
        modeloDetalle.setRowCount(0);
        if (editando == null) {
            lblReporte.setText("Seleccione un cliente para ver su reporte");
            lblTotales.setText(" ");
            return;
        }
        lblReporte.setText("Cliente: " + editando.getNombre());
        Map<LocalDate, double[]> diario = datos.reporteDiarioCliente(editando);
        int platos = 0;
        double total = 0;
        for (Map.Entry<LocalDate, double[]> e : diario.entrySet()) {
            modeloDiario.addRow(new Object[]{e.getKey().format(FECHA), (int) e.getValue()[0],
                    Ui.dinero(e.getValue()[1])});
            platos += (int) e.getValue()[0];
            total += e.getValue()[1];
        }
        List<Despacho> lista = datos.getDespachosCliente(editando);
        lista.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        for (Despacho d : lista) {
            modeloDetalle.addRow(new Object[]{d.getFecha().format(FECHA), d.getMenuNombre(), d.getPlatos(),
                    Ui.dinero(d.getPrecioUnitario()), Ui.dinero(d.getTotal())});
        }
        double promedio = diario.isEmpty() ? 0 : (double) platos / diario.size();
        lblTotales.setText(String.format("Total: %d platos en %d día(s)  |  Promedio diario: %.1f  |  Facturado: %s",
                platos, diario.size(), promedio, Ui.dinero(total)));
    }

    private void refrescar() {
        clientes = List.copyOf(datos.getClientes());
        LocalDate hoy = LocalDate.now();
        Map<Cliente, double[]> deHoy = datos.resumenPorCliente(hoy);
        modeloClientes.setRowCount(0);
        for (Cliente c : clientes) {
            double[] v = deHoy.get(c);
            modeloClientes.addRow(new Object[]{c.getNombre(), c.getTelefono(), c.getDireccion(),
                    v == null ? 0 : (int) v[0], datos.totalPlatosCliente(c)});
        }
        if (editando != null) {
            int idx = clientes.indexOf(editando);
            if (idx < 0) limpiar();
            else {
                tablaClientes.setRowSelectionInterval(idx, idx);
                mostrarReporte();
            }
        }
    }
}
