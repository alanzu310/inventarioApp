package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Ingrediente;
import com.negocio.comida.modelo.Menu;
import com.negocio.comida.modelo.Unidad;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Pestaña Inventario: alta, edición, reabastecimiento y baja de ingredientes. */
class PanelInventario extends JPanel {
    private final DatosNegocio datos;

    private final JTextField txtNombre = new JTextField(16);
    private final JComboBox<Unidad> cmbUnidad = new JComboBox<>(Unidad.values());
    private final JTextField txtCantidad = new JTextField(8);
    private final JTextField txtPrecio = new JTextField(8);
    private final JLabel lblPrecio = new JLabel();
    private final JLabel lblCantidad = new JLabel();
    private final JButton btnGuardar = new JButton("Agregar ingrediente");
    private final JLabel lblValor = new JLabel();

    private final DefaultTableModel modelo =
            Ui.modelo("Ingrediente", "Unidad", "Existencia", "Precio por unidad", "Valor en inventario");
    private final JTable tabla = Ui.tabla(modelo);
    private List<Ingrediente> filas = List.of();
    private Ingrediente editando;

    PanelInventario(DatosNegocio datos) {
        super(new BorderLayout(8, 8));
        this.datos = datos;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = Ui.titulo(new JPanel(new GridBagLayout()), "Ingrediente");
        form.add(new JLabel("Nombre:"), Ui.gbc(0, 0));
        form.add(txtNombre, Ui.gbc(1, 0));
        form.add(new JLabel("Se mide por:"), Ui.gbc(2, 0));
        form.add(cmbUnidad, Ui.gbc(3, 0));
        form.add(lblCantidad, Ui.gbc(0, 1));
        form.add(txtCantidad, Ui.gbc(1, 1));
        form.add(lblPrecio, Ui.gbc(2, 1));
        form.add(txtPrecio, Ui.gbc(3, 1));

        JButton btnLimpiar = new JButton("Nuevo / Limpiar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.add(btnGuardar);
        botones.add(btnLimpiar);
        var g = Ui.gbc(0, 2);
        g.gridwidth = 4;
        form.add(botones, g);

        cmbUnidad.addActionListener(e -> actualizarEtiquetas());
        actualizarEtiquetas();
        btnGuardar.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        txtPrecio.addActionListener(e -> guardar());

        JButton btnReabastecer = new JButton("Reabastecer (sumar existencia)");
        JButton btnEliminar = new JButton("Eliminar");
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(btnReabastecer);
        acciones.add(btnEliminar);
        acciones.add(new JLabel("    Valor total del inventario: "));
        lblValor.setFont(Ui.negrita(lblValor.getFont(), 14f));
        acciones.add(lblValor);
        btnReabastecer.addActionListener(e -> reabastecer());
        btnEliminar.addActionListener(e -> eliminar());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        add(form, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(acciones, BorderLayout.SOUTH);
        datos.agregarOyente(this::refrescar);
        refrescar();
    }

    private void actualizarEtiquetas() {
        Unidad u = (Unidad) cmbUnidad.getSelectedItem();
        String ab = u == null ? "" : u.getAbreviatura();
        lblCantidad.setText("Existencia (" + ab + "):");
        lblPrecio.setText("Precio por " + (u == null ? "" : u.toString().toLowerCase()) + ":");
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        Double cant = Ui.leerNumero(txtCantidad.getText());
        Double precio = Ui.leerNumero(txtPrecio.getText());
        if (nombre.isEmpty()) { Ui.error(this, "Escriba el nombre del ingrediente."); return; }
        if (cant == null || cant < 0) { Ui.error(this, "La existencia debe ser un número mayor o igual a 0."); return; }
        if (precio == null || precio < 0) { Ui.error(this, "El precio debe ser un número mayor o igual a 0."); return; }
        boolean duplicado = datos.getIngredientes().stream()
                .anyMatch(i -> i != editando && i.getNombre().equalsIgnoreCase(nombre));
        if (duplicado) { Ui.error(this, "Ya existe un ingrediente con ese nombre."); return; }

        Unidad u = (Unidad) cmbUnidad.getSelectedItem();
        if (editando == null) {
            datos.agregarIngrediente(nombre, u, cant, precio);
        } else {
            datos.actualizarIngrediente(editando, nombre, u, cant, precio);
        }
        limpiar();
    }

    private void limpiar() {
        editando = null;
        tabla.clearSelection();
        txtNombre.setText("");
        txtCantidad.setText("");
        txtPrecio.setText("");
        cmbUnidad.setSelectedIndex(0);
        btnGuardar.setText("Agregar ingrediente");
        txtNombre.requestFocusInWindow();
    }

    private Ingrediente seleccionado() {
        int r = tabla.getSelectedRow();
        return r < 0 ? null : filas.get(tabla.convertRowIndexToModel(r));
    }

    private void cargarSeleccion() {
        Ingrediente i = seleccionado();
        if (i == null) return;
        editando = i;
        txtNombre.setText(i.getNombre());
        cmbUnidad.setSelectedItem(i.getUnidad());
        txtCantidad.setText(Ui.numero(i.getCantidad()).replace(",", ""));
        txtPrecio.setText(String.valueOf(i.getPrecioPorUnidad()));
        btnGuardar.setText("Actualizar ingrediente");
    }

    private void reabastecer() {
        Ingrediente i = seleccionado();
        if (i == null) { Ui.error(this, "Seleccione un ingrediente de la tabla."); return; }
        String r = JOptionPane.showInputDialog(this,
                "¿Cuántas " + i.getUnidad().getAbreviatura() + " de " + i.getNombre() + " ingresan?");
        if (r == null) return;
        Double c = Ui.leerNumero(r);
        if (c == null || c <= 0) { Ui.error(this, "Cantidad no válida."); return; }
        datos.reabastecer(i, c);
        limpiar();
    }

    private void eliminar() {
        Ingrediente i = seleccionado();
        if (i == null) { Ui.error(this, "Seleccione un ingrediente de la tabla."); return; }
        List<Menu> usan = datos.menusQueUsan(i);
        if (!usan.isEmpty()) {
            Ui.error(this, "No se puede eliminar: lo usan los menús "
                    + usan.stream().map(Menu::getNombre).collect(Collectors.joining(", ")) + ".");
            return;
        }
        if (Ui.confirmar(this, "¿Eliminar " + i.getNombre() + "?")) {
            datos.eliminarIngrediente(i);
            limpiar();
        }
    }

    private void refrescar() {
        filas = List.copyOf(datos.getIngredientes());
        modelo.setRowCount(0);
        double total = 0;
        for (Ingrediente i : filas) {
            modelo.addRow(new Object[]{
                    i.getNombre(), i.getUnidad(),
                    Ui.numero(i.getCantidad()) + " " + i.getUnidad().getAbreviatura(),
                    Ui.dinero(i.getPrecioPorUnidad()) + " / " + i.getUnidad().getAbreviatura(),
                    Ui.dinero(i.getValorTotal())});
            total += i.getValorTotal();
        }
        lblValor.setText(Ui.dinero(total));
        if (editando != null && !filas.contains(editando)) limpiar();
    }
}
