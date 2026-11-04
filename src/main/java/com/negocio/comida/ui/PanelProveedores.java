package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Ingrediente;
import com.negocio.comida.modelo.Lote;
import com.negocio.comida.modelo.Proveedor;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Pestaña Proveedores: registro de proveedores y su historial de compras. */
class PanelProveedores extends JPanel {
    private final DatosNegocio datos;

    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtTelefono = new JTextField(12);
    private final JButton btnGuardar = new JButton("Agregar proveedor");

    private final DefaultTableModel modelo = Ui.modelo("Proveedor", "Teléfono", "Compras", "Total comprado",
            "Última compra");
    private final JTable tabla = Ui.tabla(modelo);
    private List<Proveedor> filas = List.of();
    private Proveedor editando;

    private final JLabel lblHistorial = new JLabel();
    private final DefaultTableModel modeloCompras = Ui.modelo("Fecha", "Ingrediente", "Cantidad",
            "Precio por unidad", "Total pagado");
    private final JTable tablaCompras = Ui.tabla(modeloCompras);

    PanelProveedores(DatosNegocio datos) {
        super(new BorderLayout(8, 8));
        this.datos = datos;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = Ui.titulo(new JPanel(new GridBagLayout()), "Proveedor");
        form.add(new JLabel("Nombre:"), Ui.gbc(0, 0));
        form.add(txtNombre, Ui.gbc(1, 0));
        form.add(new JLabel("Teléfono:"), Ui.gbc(2, 0));
        form.add(txtTelefono, Ui.gbc(3, 0));
        JButton btnLimpiar = new JButton("Nuevo / Limpiar");
        JButton btnEliminar = new JButton("Eliminar proveedor");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.add(btnGuardar);
        botones.add(btnLimpiar);
        botones.add(btnEliminar);
        form.add(botones, Ui.gbc(4, 0));
        btnGuardar.addActionListener(e -> guardar());
        txtTelefono.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnEliminar.addActionListener(e -> eliminar());

        JPanel lista = Ui.titulo(new JPanel(new BorderLayout()), "Proveedores");
        lista.add(new JScrollPane(tabla));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        JPanel historial = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Historial de compras");
        lblHistorial.setFont(Ui.negrita(lblHistorial.getFont(), 13f));
        historial.add(lblHistorial, BorderLayout.NORTH);
        historial.add(new JScrollPane(tablaCompras), BorderLayout.CENTER);
        historial.add(new JLabel("Las compras se registran en la pestaña Inventario → \"Registrar compra\"."),
                BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lista, historial);
        split.setResizeWeight(0.45);
        add(form, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        datos.agregarOyente(this::refrescar);
        refrescar();
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) { Ui.error(this, "Escriba el nombre del proveedor."); return; }
        boolean duplicado = datos.getProveedores().stream()
                .anyMatch(p -> p != editando && p.getNombre().equalsIgnoreCase(nombre));
        if (duplicado) { Ui.error(this, "Ya existe un proveedor con ese nombre."); return; }
        if (editando == null) datos.agregarProveedor(nombre, txtTelefono.getText().trim());
        else datos.actualizarProveedor(editando, nombre, txtTelefono.getText().trim());
        limpiar();
    }

    private void eliminar() {
        if (editando == null) { Ui.error(this, "Seleccione un proveedor."); return; }
        if (datos.tieneCompras(editando)) {
            Ui.error(this, editando.getNombre() + " tiene compras registradas y no se puede eliminar\n"
                    + "(se perdería el historial de precios).");
            return;
        }
        if (Ui.confirmar(this, "¿Eliminar a " + editando.getNombre() + "?")) {
            datos.eliminarProveedor(editando);
            limpiar();
        }
    }

    private void limpiar() {
        editando = null;
        tabla.clearSelection();
        txtNombre.setText("");
        txtTelefono.setText("");
        btnGuardar.setText("Agregar proveedor");
        mostrarHistorial();
    }

    private void cargarSeleccion() {
        int r = tabla.getSelectedRow();
        if (r < 0) return;
        editando = filas.get(r);
        txtNombre.setText(editando.getNombre());
        txtTelefono.setText(editando.getTelefono());
        btnGuardar.setText("Actualizar proveedor");
        mostrarHistorial();
    }

    private void mostrarHistorial() {
        modeloCompras.setRowCount(0);
        if (editando == null) {
            lblHistorial.setText("Seleccione un proveedor");
            return;
        }
        lblHistorial.setText(editando.getNombre() + "  |  Total comprado: " + Ui.dinero(datos.totalComprado(editando)));
        for (Lote l : datos.compras(editando)) {
            Ingrediente i = datos.buscarIngrediente(l.getIngredienteId()).orElse(null);
            String ab = i == null ? "" : " " + i.getUnidad().getAbreviatura();
            modeloCompras.addRow(new Object[]{l.getFecha().format(PanelClientes.FECHA),
                    i == null ? "(eliminado)" : i.getNombre(),
                    Ui.numero(l.getCantidadComprada()) + ab,
                    Ui.dinero(l.getPrecioPorUnidad()) + " /" + ab,
                    Ui.dinero(l.getTotalCompra())});
        }
    }

    private void refrescar() {
        Proveedor sel = editando;
        filas = List.copyOf(datos.getProveedores());
        modelo.setRowCount(0);
        for (Proveedor p : filas) {
            List<Lote> compras = datos.compras(p);
            modelo.addRow(new Object[]{p.getNombre(), p.getTelefono(), compras.size(),
                    Ui.dinero(datos.totalComprado(p)),
                    compras.isEmpty() ? "—" : compras.get(0).getFecha().format(PanelClientes.FECHA)});
        }
        if (sel != null && filas.contains(sel)) {
            int idx = filas.indexOf(sel);
            tabla.setRowSelectionInterval(idx, idx);
        } else {
            limpiar();
        }
    }
}
