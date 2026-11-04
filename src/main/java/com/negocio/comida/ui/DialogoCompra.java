package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Ingrediente;
import com.negocio.comida.modelo.Lote;
import com.negocio.comida.modelo.Proveedor;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Registrar o corregir una compra de un ingrediente: proveedor, fecha,
 * cantidad y precio (por unidad o total pagado; uno se calcula del otro).
 */
class DialogoCompra extends JDialog {
    private static final String SIN_PROVEEDOR = "Sin proveedor";

    private final DatosNegocio datos;
    private final Lote editando;
    private final DefaultComboBoxModel<Ingrediente> modeloIngredientes = new DefaultComboBoxModel<>();
    private final JComboBox<Ingrediente> cmbIngrediente = new JComboBox<>(modeloIngredientes);
    private final DefaultComboBoxModel<Object> modeloProveedores = new DefaultComboBoxModel<>();
    private final JComboBox<Object> cmbProveedor = new JComboBox<>(modeloProveedores);
    private final JSpinner spFecha = Ui.spinnerFecha(LocalDate.now());
    private final JTextField txtCantidad = new JTextField(8);
    private final JTextField txtPrecio = new JTextField(8);
    private final JTextField txtTotal = new JTextField(8);
    private final JLabel lblUnidadCantidad = new JLabel();
    private final JLabel lblUnidadPrecio = new JLabel();
    private final JLabel lblReferencia = new JLabel(" ");
    private boolean calculando;
    /** true si el usuario escribió el total pagado (se calcula el precio por unidad). */
    private boolean mandaTotal;

    DialogoCompra(Window padre, DatosNegocio datos, Ingrediente ingrediente, Lote editando) {
        super(padre, editando == null ? "Registrar compra" : "Corregir compra", ModalityType.APPLICATION_MODAL);
        this.datos = datos;
        this.editando = editando;

        for (Ingrediente i : datos.getIngredientes()) modeloIngredientes.addElement(i);
        if (ingrediente != null) cmbIngrediente.setSelectedItem(ingrediente);
        cmbIngrediente.setEnabled(editando == null);
        cargarProveedores(editando == null ? null : datos.buscarProveedor(editando.getProveedorId()).orElse(null));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        int f = 0;
        form.add(new JLabel("Ingrediente:"), Ui.gbc(0, f));
        var g = Ui.gbc(1, f++);
        g.gridwidth = 2;
        form.add(cmbIngrediente, g);

        form.add(new JLabel("Proveedor:"), Ui.gbc(0, f));
        form.add(cmbProveedor, Ui.gbc(1, f));
        JButton btnNuevoProv = new JButton("Nuevo proveedor...");
        form.add(btnNuevoProv, Ui.gbc(2, f++));

        form.add(new JLabel("Fecha de compra:"), Ui.gbc(0, f));
        form.add(spFecha, Ui.gbc(1, f++));

        form.add(new JLabel("Cantidad comprada:"), Ui.gbc(0, f));
        form.add(txtCantidad, Ui.gbc(1, f));
        form.add(lblUnidadCantidad, Ui.gbc(2, f++));

        form.add(new JLabel("Precio por unidad:"), Ui.gbc(0, f));
        form.add(txtPrecio, Ui.gbc(1, f));
        form.add(lblUnidadPrecio, Ui.gbc(2, f++));

        form.add(new JLabel("Total pagado:"), Ui.gbc(0, f));
        form.add(txtTotal, Ui.gbc(1, f));
        form.add(new JLabel("(escriba el precio por unidad o el total)"), Ui.gbc(2, f++));

        g = Ui.gbc(0, f);
        g.gridwidth = 3;
        g.insets = new java.awt.Insets(10, 4, 4, 4);
        form.add(lblReferencia, g);

        JButton btnGuardar = new JButton(editando == null ? "Registrar compra" : "Guardar cambios");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        getRootPane().setDefaultButton(btnGuardar);
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        btnNuevoProv.addActionListener(e -> nuevoProveedor());
        cmbIngrediente.addActionListener(e -> actualizarEtiquetas());
        cmbProveedor.addActionListener(e -> actualizarEtiquetas());

        escuchar(txtCantidad, this::recalcular);
        // Solo cuenta lo que escribe el usuario, no lo que se calcula automáticamente.
        escuchar(txtPrecio, () -> { if (!calculando) { mandaTotal = false; recalcular(); } });
        escuchar(txtTotal, () -> { if (!calculando) { mandaTotal = true; recalcular(); } });

        if (editando != null) {
            Ui.ponerFecha(spFecha, editando.getFecha());
            txtCantidad.setText(Ui.numeroCampo(editando.getCantidadComprada()));
            txtPrecio.setText(Ui.numeroCampo(editando.getPrecioPorUnidad()));
        }
        actualizarEtiquetas();

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(padre);
    }

    private void cargarProveedores(Proveedor seleccionado) {
        modeloProveedores.removeAllElements();
        for (Proveedor p : datos.getProveedores()) modeloProveedores.addElement(p);
        modeloProveedores.addElement(SIN_PROVEEDOR);
        if (seleccionado != null) modeloProveedores.setSelectedItem(seleccionado);
        else if (editando != null && editando.getProveedorId() == 0) modeloProveedores.setSelectedItem(SIN_PROVEEDOR);
    }

    private Proveedor proveedor() {
        Object o = cmbProveedor.getSelectedItem();
        return o instanceof Proveedor p ? p : null;
    }

    private void nuevoProveedor() {
        JTextField nombre = new JTextField(18);
        JTextField tel = new JTextField(12);
        JPanel p = new JPanel(new GridLayout(2, 2, 4, 4));
        p.add(new JLabel("Nombre:"));
        p.add(nombre);
        p.add(new JLabel("Teléfono:"));
        p.add(tel);
        SwingUtilities.invokeLater(nombre::requestFocusInWindow);
        if (JOptionPane.showConfirmDialog(this, p, "Nuevo proveedor", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String n = nombre.getText().trim();
        if (n.isEmpty()) { Ui.error(this, "Escriba el nombre del proveedor."); return; }
        Optional<Proveedor> existente = datos.getProveedores().stream()
                .filter(x -> x.getNombre().equalsIgnoreCase(n)).findFirst();
        Proveedor nuevo = existente.orElseGet(() -> datos.agregarProveedor(n, tel.getText().trim()));
        cargarProveedores(nuevo);
    }

    private static void escuchar(JTextField campo, Runnable r) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    /** Calcula el total a partir del precio, o el precio a partir del total. */
    private void recalcular() {
        if (calculando) return;
        SwingUtilities.invokeLater(() -> {
            calculando = true;
            try {
                Double cant = Ui.leerNumero(txtCantidad.getText());
                if (mandaTotal) {
                    Double total = Ui.leerNumero(txtTotal.getText());
                    txtPrecio.setText(cant != null && cant > 0 && total != null
                            ? String.format(java.util.Locale.US, "%.4f", total / cant).replaceAll("0+$", "").replaceAll("\\.$", "")
                            : "");
                } else {
                    Double precio = Ui.leerNumero(txtPrecio.getText());
                    txtTotal.setText(cant != null && precio != null
                            ? String.format(java.util.Locale.US, "%.2f", cant * precio) : "");
                }
            } finally {
                calculando = false;
            }
        });
    }

    private void actualizarEtiquetas() {
        Ingrediente i = (Ingrediente) cmbIngrediente.getSelectedItem();
        if (i == null) return;
        lblUnidadCantidad.setText(i.getUnidad().toString().toLowerCase() + "s (" + i.getUnidad().getAbreviatura() + ")");
        lblUnidadPrecio.setText("por " + i.getUnidad().toString().toLowerCase());
        // Referencia: último precio de este proveedor para este ingrediente
        Proveedor p = proveedor();
        long provId = p == null ? 0 : p.getId();
        Optional<Lote> anterior = datos.compras(i).stream()
                .filter(l -> l.getProveedorId() == provId && l != editando).findFirst();
        lblReferencia.setText(anterior.map(l -> "Última compra a " + datos.nombreProveedor(provId) + ": "
                + Ui.dinero(l.getPrecioPorUnidad()) + " / " + i.getUnidad().getAbreviatura()
                + " (" + l.getFecha().format(PanelClientes.FECHA) + ")").orElse(" "));
    }

    private void guardar() {
        Ingrediente i = (Ingrediente) cmbIngrediente.getSelectedItem();
        if (i == null) { Ui.error(this, "Primero agregue el ingrediente en Inventario."); return; }
        Double cant = Ui.leerNumero(txtCantidad.getText());
        Double precio = Ui.leerNumero(txtPrecio.getText());
        if (cant == null || cant <= 0) { Ui.error(this, "Escriba la cantidad comprada (mayor a 0)."); return; }
        if (precio == null || precio < 0) { Ui.error(this, "Escriba el precio por unidad o el total pagado."); return; }
        LocalDate fecha = Ui.leerFecha(spFecha);
        try {
            if (editando == null) datos.registrarCompra(i, proveedor(), fecha, cant, precio);
            else datos.editarCompra(editando, proveedor(), fecha, cant, precio);
        } catch (IllegalArgumentException ex) {
            Ui.error(this, "De esta compra ya se usaron " + Ui.numero(editando.getCantidadUsada()) + " "
                    + i.getUnidad().getAbreviatura() + ".\nLa cantidad comprada no puede ser menor.");
            return;
        }
        dispose();
    }
}
