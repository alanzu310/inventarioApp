package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Ingrediente;
import com.negocio.comida.modelo.ItemMenu;
import com.negocio.comida.modelo.Menu;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Pestaña Menús: se arman platos con los ingredientes que hay en el inventario. */
class PanelMenus extends JPanel {
    private final DatosNegocio datos;

    // Lista de menús
    private final DefaultTableModel modeloMenus =
            Ui.modelo("Menú", "Precio venta", "Costo ingredientes", "Ganancia por plato", "Platos posibles");
    private final JTable tablaMenus = Ui.tabla(modeloMenus);
    private List<Menu> menus = List.of();

    // Editor
    private final JTextField txtNombre = new JTextField(16);
    private final JTextField txtPrecio = new JTextField(8);
    private final DefaultComboBoxModel<Ingrediente> modeloIngredientes = new DefaultComboBoxModel<>();
    private final JComboBox<Ingrediente> cmbIngrediente = new JComboBox<>(modeloIngredientes);
    private final JTextField txtCantidad = new JTextField(6);
    private final JLabel lblUnidad = new JLabel("   ");
    private final DefaultTableModel modeloItems =
            Ui.modelo("Ingrediente", "Cantidad por plato", "Costo");
    private final JTable tablaItems = Ui.tabla(modeloItems);
    private final JLabel lblCosto = new JLabel();
    private final JLabel lblEstado = new JLabel();
    private final JButton btnGuardar = new JButton("Guardar menú");

    private final List<ItemMenu> items = new ArrayList<>();
    private Menu editando;

    PanelMenus(DatosNegocio datos) {
        super(new BorderLayout());
        this.datos = datos;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- izquierda: menús existentes
        JPanel izq = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Menús creados");
        izq.add(new JScrollPane(tablaMenus), BorderLayout.CENTER);
        JButton btnNuevo = new JButton("Nuevo menú");
        JButton btnEliminar = new JButton("Eliminar menú");
        JPanel accIzq = new JPanel(new FlowLayout(FlowLayout.LEFT));
        accIzq.add(btnNuevo);
        accIzq.add(btnEliminar);
        izq.add(accIzq, BorderLayout.SOUTH);
        btnNuevo.addActionListener(e -> limpiar());
        btnEliminar.addActionListener(e -> eliminarMenu());
        tablaMenus.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        // ---- derecha: editor
        JPanel der = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Armar menú");
        JPanel cab = new JPanel(new GridBagLayout());
        cab.add(new JLabel("Nombre del menú:"), Ui.gbc(0, 0));
        var g = Ui.gbc(1, 0);
        g.gridwidth = 3;
        cab.add(txtNombre, g);
        cab.add(new JLabel("Precio de venta por plato:"), Ui.gbc(0, 1));
        cab.add(txtPrecio, Ui.gbc(1, 1));
        cab.add(new JLabel("Ingrediente del inventario:"), Ui.gbc(0, 2));
        cab.add(cmbIngrediente, Ui.gbc(1, 2));
        cab.add(new JLabel("Cantidad por plato:"), Ui.gbc(0, 3));
        JPanel cantPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cantPanel.add(txtCantidad);
        cantPanel.add(lblUnidad);
        JButton btnAgregar = new JButton("Agregar ingrediente");
        cantPanel.add(btnAgregar);
        cab.add(cantPanel, Ui.gbc(1, 3));
        der.add(cab, BorderLayout.NORTH);
        der.add(new JScrollPane(tablaItems), BorderLayout.CENTER);

        JButton btnQuitar = new JButton("Quitar ingrediente");
        JPanel pie = new JPanel(new BorderLayout());
        JPanel pieBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pieBotones.add(btnQuitar);
        pieBotones.add(btnGuardar);
        lblCosto.setFont(Ui.negrita(lblCosto.getFont(), 13f));
        JPanel info = new JPanel(new java.awt.GridLayout(2, 1));
        info.add(lblCosto);
        info.add(lblEstado);
        pie.add(info, BorderLayout.NORTH);
        pie.add(pieBotones, BorderLayout.SOUTH);
        der.add(pie, BorderLayout.SOUTH);

        cmbIngrediente.addActionListener(e -> {
            Ingrediente i = (Ingrediente) cmbIngrediente.getSelectedItem();
            lblUnidad.setText(i == null ? "   " : i.getUnidad().getAbreviatura());
        });
        btnAgregar.addActionListener(e -> agregarItem());
        txtCantidad.addActionListener(e -> agregarItem());
        btnQuitar.addActionListener(e -> quitarItem());
        btnGuardar.addActionListener(e -> guardar());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        datos.agregarOyente(this::refrescar);
        refrescar();
        limpiar();
        // Clic en una zona vacía (o Esc): se deselecciona el menú y se limpia el editor.
        Ui.deseleccionarAlHacerClicFuera(this, tablaMenus, () -> !hayCambiosSinGuardar(), this::limpiar);
    }

    /** true si el menú seleccionado se modificó en el editor y no se guardó. */
    private boolean hayCambiosSinGuardar() {
        if (editando == null) return false;
        Double precio = Ui.leerNumero(txtPrecio.getText());
        if (!txtNombre.getText().trim().equals(editando.getNombre())) return true;
        if (precio == null || Math.abs(precio - editando.getPrecioVenta()) > 1e-9) return true;
        List<ItemMenu> guardados = editando.getItems();
        if (guardados.size() != items.size()) return true;
        for (int i = 0; i < items.size(); i++) {
            if (guardados.get(i).getIngredienteId() != items.get(i).getIngredienteId()
                    || Math.abs(guardados.get(i).getCantidad() - items.get(i).getCantidad()) > 1e-9) return true;
        }
        return false;
    }

    private void agregarItem() {
        Ingrediente ing = (Ingrediente) cmbIngrediente.getSelectedItem();
        if (ing == null) { Ui.error(this, "Primero agregue ingredientes en la pestaña Inventario."); return; }
        Double cant = Ui.leerNumero(txtCantidad.getText());
        if (cant == null || cant <= 0) { Ui.error(this, "Escriba una cantidad por plato mayor a 0."); return; }
        ItemMenu existente = items.stream().filter(it -> it.getIngredienteId() == ing.getId()).findFirst().orElse(null);
        if (existente != null) existente.setCantidad(cant);
        else items.add(new ItemMenu(ing.getId(), cant));
        txtCantidad.setText("");
        refrescarItems();
    }

    private void quitarItem() {
        int r = tablaItems.getSelectedRow();
        if (r < 0) { Ui.error(this, "Seleccione el ingrediente a quitar."); return; }
        items.remove(r);
        refrescarItems();
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        Double precio = Ui.leerNumero(txtPrecio.getText());
        if (nombre.isEmpty()) { Ui.error(this, "Escriba el nombre del menú."); return; }
        if (precio == null || precio < 0) { Ui.error(this, "Escriba un precio de venta válido."); return; }
        if (items.isEmpty()) { Ui.error(this, "Agregue al menos un ingrediente al menú."); return; }
        boolean duplicado = datos.getMenus().stream()
                .anyMatch(m -> m != editando && m.getNombre().equalsIgnoreCase(nombre));
        if (duplicado) { Ui.error(this, "Ya existe un menú con ese nombre."); return; }
        List<ItemMenu> copia = new ArrayList<>();
        for (ItemMenu it : items) copia.add(new ItemMenu(it.getIngredienteId(), it.getCantidad()));
        datos.guardarMenu(editando, nombre, precio, copia);
        limpiar();
    }

    private void eliminarMenu() {
        int r = tablaMenus.getSelectedRow();
        if (r < 0) { Ui.error(this, "Seleccione un menú."); return; }
        Menu m = menus.get(r);
        if (Ui.confirmar(this, "¿Eliminar el menú " + m.getNombre() + "?\n(El historial de despachos se conserva.)")) {
            datos.eliminarMenu(m);
            limpiar();
        }
    }

    private void cargarSeleccion() {
        int r = tablaMenus.getSelectedRow();
        if (r < 0) return;
        editando = menus.get(r);
        txtNombre.setText(editando.getNombre());
        txtPrecio.setText(String.valueOf(editando.getPrecioVenta()));
        items.clear();
        for (ItemMenu it : editando.getItems()) items.add(new ItemMenu(it.getIngredienteId(), it.getCantidad()));
        btnGuardar.setText("Actualizar menú");
        refrescarItems();
    }

    private void limpiar() {
        editando = null;
        tablaMenus.clearSelection();
        txtNombre.setText("");
        txtPrecio.setText("");
        txtCantidad.setText("");
        items.clear();
        btnGuardar.setText("Guardar menú");
        refrescarItems();
    }

    private void refrescarItems() {
        modeloItems.setRowCount(0);
        double costo = 0;
        items.removeIf(it -> datos.buscarIngrediente(it.getIngredienteId()).isEmpty());
        for (ItemMenu it : items) {
            Ingrediente i = datos.buscarIngrediente(it.getIngredienteId()).orElseThrow();
            double c = datos.costoDe(i, it.getCantidad());
            costo += c;
            modeloItems.addRow(new Object[]{i.getNombre(),
                    Ui.numero(it.getCantidad()) + " " + i.getUnidad().getAbreviatura(), Ui.dinero(c)});
        }
        Double precio = Ui.leerNumero(txtPrecio.getText());
        String ganancia = precio == null ? "" : "   |   Ganancia por plato: " + Ui.dinero(precio - costo);
        lblCosto.setText("Costo por plato: " + Ui.dinero(costo) + ganancia);
        lblEstado.setText(editando == null ? "Creando un menú nuevo" : "Editando: " + editando.getNombre());
    }

    private void refrescar() {
        // combo de ingredientes (ligado al inventario)
        Object sel = cmbIngrediente.getSelectedItem();
        modeloIngredientes.removeAllElements();
        for (Ingrediente i : datos.getIngredientes()) modeloIngredientes.addElement(i);
        if (sel != null && datos.getIngredientes().contains(sel)) cmbIngrediente.setSelectedItem(sel);

        menus = List.copyOf(datos.getMenus());
        modeloMenus.setRowCount(0);
        for (Menu m : menus) {
            double costo = datos.costoPlato(m);
            modeloMenus.addRow(new Object[]{m.getNombre(), Ui.dinero(m.getPrecioVenta()), Ui.dinero(costo),
                    Ui.dinero(m.getPrecioVenta() - costo), datos.platosDisponibles(m)});
        }
        if (editando != null && !menus.contains(editando)) limpiar();
        else refrescarItems();
    }
}
