package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.DatosNegocio.PrecioProveedor;
import com.negocio.comida.modelo.Ingrediente;
import com.negocio.comida.modelo.Lote;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Pestaña Inventario: ingredientes y sus compras. Cada compra guarda su
 * proveedor y su propio precio, así comprar más barato a otro proveedor no
 * cambia el valor de lo que ya se tenía.
 */
class PanelInventario extends JPanel {
    private final DatosNegocio datos;

    // Formulario del ingrediente
    private final JTextField txtNombre = new JTextField(16);
    private final JComboBox<Unidad> cmbUnidad = new JComboBox<>(Unidad.values());
    private final JButton btnGuardar = new JButton("Agregar ingrediente");

    // Ingredientes
    private final DefaultTableModel modelo = Ui.modelo("Ingrediente", "Unidad", "Existencia",
            "Costo promedio", "Último precio", "Mejor precio (proveedor)", "Valor en inventario");
    private final JTable tabla = Ui.tabla(modelo);
    private final JLabel lblValor = new JLabel();
    private List<Ingrediente> filas = List.of();
    private final JTextField txtBuscar = new JTextField(22);
    private final JLabel lblConteo = new JLabel();
    private Ingrediente editando;

    // Compras del ingrediente seleccionado
    private final JLabel lblCompras = new JLabel();
    private final DefaultTableModel modeloCompras = Ui.modelo("Fecha", "Proveedor", "Comprado", "Queda",
            "Precio por unidad", "Total pagado");
    private final JTable tablaCompras = Ui.tabla(modeloCompras);
    private List<Lote> compras = List.of();
    private final DefaultTableModel modeloPrecios = Ui.modelo("Proveedor", "Último precio", "Más bajo",
            "Compras", "Última compra");
    private final JTable tablaPrecios = Ui.tabla(modeloPrecios);

    PanelInventario(DatosNegocio datos) {
        super(new BorderLayout(8, 8));
        this.datos = datos;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- formulario
        JPanel form = Ui.titulo(new JPanel(new GridBagLayout()), "Ingrediente");
        form.add(new JLabel("Nombre:"), Ui.gbc(0, 0));
        form.add(txtNombre, Ui.gbc(1, 0));
        form.add(new JLabel("Se mide por:"), Ui.gbc(2, 0));
        form.add(cmbUnidad, Ui.gbc(3, 0));
        JButton btnLimpiar = new JButton("Nuevo / Limpiar");
        JButton btnEliminar = new JButton("Eliminar ingrediente");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botones.add(btnGuardar);
        botones.add(btnLimpiar);
        botones.add(btnEliminar);
        form.add(botones, Ui.gbc(4, 0));
        btnGuardar.addActionListener(e -> guardar());
        txtNombre.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnEliminar.addActionListener(e -> eliminar());

        // ---- tabla de ingredientes
        JPanel panelIngredientes = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Ingredientes");
        JPanel barraBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(Ui.negrita(lblBuscar.getFont(), lblBuscar.getFont().getSize2D()));
        JButton btnBorrarBusqueda = new JButton("✕");
        btnBorrarBusqueda.setToolTipText("Borrar búsqueda");
        txtBuscar.setToolTipText("Escriba el nombre o una palabra del ingrediente (no importan mayúsculas ni tildes)");
        barraBuscar.add(lblBuscar);
        barraBuscar.add(txtBuscar);
        barraBuscar.add(btnBorrarBusqueda);
        barraBuscar.add(lblConteo);
        btnBorrarBusqueda.addActionListener(e -> {
            txtBuscar.setText("");
            txtBuscar.requestFocusInWindow();
        });
        txtBuscar.addActionListener(e -> { // Enter: selecciona el primer resultado
            if (!filas.isEmpty()) tabla.setRowSelectionInterval(0, 0);
        });
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        panelIngredientes.add(barraBuscar, BorderLayout.NORTH);
        panelIngredientes.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pie.add(new JLabel("Valor total del inventario:"));
        lblValor.setFont(Ui.negrita(lblValor.getFont(), 14f));
        pie.add(lblValor);
        pie.add(new JLabel("     Costo promedio = valor de lo que queda ÷ existencia, cada compra a su precio."));
        panelIngredientes.add(pie, BorderLayout.SOUTH);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        // ---- compras del ingrediente
        JPanel panelCompras = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Compras");
        lblCompras.setFont(Ui.negrita(lblCompras.getFont(), 13f));
        panelCompras.add(lblCompras, BorderLayout.NORTH);
        panelCompras.add(new JScrollPane(tablaCompras), BorderLayout.CENTER);
        JButton btnComprar = new JButton("Registrar compra...");
        btnComprar.setFont(Ui.negrita(btnComprar.getFont(), btnComprar.getFont().getSize2D()));
        JButton btnEditar = new JButton("Corregir compra...");
        JButton btnAjustar = new JButton("Ajustar lo que queda...");
        JButton btnBorrarCompra = new JButton("Eliminar compra");
        JPanel accCompras = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        accCompras.add(btnComprar);
        accCompras.add(btnEditar);
        accCompras.add(btnAjustar);
        accCompras.add(btnBorrarCompra);
        panelCompras.add(accCompras, BorderLayout.SOUTH);
        btnComprar.addActionListener(e -> registrarCompra());
        btnEditar.addActionListener(e -> editarCompra());
        btnAjustar.addActionListener(e -> ajustarRestante());
        btnBorrarCompra.addActionListener(e -> eliminarCompra());
        tablaCompras.setToolTipText("Las compras se usan de la más antigua a la más reciente.");

        JPanel panelPrecios = Ui.titulo(new JPanel(new BorderLayout()), "Precios por proveedor (más barato primero)");
        panelPrecios.add(new JScrollPane(tablaPrecios));

        JSplitPane abajo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelCompras, panelPrecios);
        abajo.setResizeWeight(0.62);
        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelIngredientes, abajo);
        centro.setResizeWeight(0.5);

        add(form, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        datos.agregarOyente(this::refrescar);
        refrescar();
    }

    // ------------------------------------------------------------ ingrediente

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) { Ui.error(this, "Escriba el nombre del ingrediente."); return; }
        boolean duplicado = datos.getIngredientes().stream()
                .anyMatch(i -> i != editando && i.getNombre().equalsIgnoreCase(nombre));
        if (duplicado) {
            Ui.error(this, "Ya existe un ingrediente con ese nombre.\n"
                    + "Para el mismo producto de otro proveedor, use \"Registrar compra\".");
            return;
        }
        Unidad u = (Unidad) cmbUnidad.getSelectedItem();
        if (editando == null) {
            txtBuscar.setText(""); // para que el nuevo ingrediente se vea en la tabla
            Ingrediente nuevo = datos.agregarIngrediente(nombre, u);
            limpiar();
            seleccionar(nuevo);
            if (Ui.confirmar(this, "Ingrediente agregado. ¿Registrar ahora la primera compra?")) registrarCompra();
        } else {
            if (u != editando.getUnidad() && !datos.compras(editando).isEmpty() && !Ui.confirmar(this,
                    "Las compras de " + editando.getNombre() + " están registradas en "
                            + editando.getUnidad().toString().toLowerCase() + "s.\n"
                            + "Cambiar la unidad no convierte las cantidades. ¿Continuar?")) {
                return;
            }
            datos.actualizarIngrediente(editando, nombre, u);
        }
    }

    private void limpiar() {
        reiniciarFormulario();
        txtNombre.requestFocusInWindow();
    }

    private void reiniciarFormulario() {
        editando = null;
        tabla.clearSelection();
        txtNombre.setText("");
        cmbUnidad.setSelectedIndex(0);
        btnGuardar.setText("Agregar ingrediente");
        mostrarCompras();
    }

    private void seleccionar(Ingrediente i) {
        int idx = filas.indexOf(i);
        if (idx >= 0) tabla.setRowSelectionInterval(idx, idx);
    }

    private void cargarSeleccion() {
        int r = tabla.getSelectedRow();
        if (r < 0) return;
        editando = filas.get(r);
        txtNombre.setText(editando.getNombre());
        cmbUnidad.setSelectedItem(editando.getUnidad());
        btnGuardar.setText("Actualizar ingrediente");
        mostrarCompras();
    }

    private void eliminar() {
        if (editando == null) { Ui.error(this, "Seleccione un ingrediente de la tabla."); return; }
        List<Menu> usan = datos.menusQueUsan(editando);
        if (!usan.isEmpty()) {
            Ui.error(this, "No se puede eliminar: lo usan los menús "
                    + usan.stream().map(Menu::getNombre).collect(Collectors.joining(", ")) + ".");
            return;
        }
        if (Ui.confirmar(this, "¿Eliminar " + editando.getNombre() + " y todo su historial de compras?")) {
            datos.eliminarIngrediente(editando);
            limpiar();
        }
    }

    // ------------------------------------------------------------ compras

    private Lote compraSeleccionada() {
        int r = tablaCompras.getSelectedRow();
        return r < 0 ? null : compras.get(r);
    }

    private void registrarCompra() {
        if (datos.getIngredientes().isEmpty()) {
            Ui.error(this, "Primero agregue un ingrediente.");
            return;
        }
        new DialogoCompra(SwingUtilities.getWindowAncestor(this), datos, editando, null).setVisible(true);
    }

    private void editarCompra() {
        Lote l = compraSeleccionada();
        if (l == null) { Ui.error(this, "Seleccione una compra de la tabla."); return; }
        new DialogoCompra(SwingUtilities.getWindowAncestor(this), datos, editando, l).setVisible(true);
    }

    private void ajustarRestante() {
        Lote l = compraSeleccionada();
        if (l == null) { Ui.error(this, "Seleccione una compra de la tabla."); return; }
        String ab = editando.getUnidad().getAbreviatura();
        String r = JOptionPane.showInputDialog(this,
                "Compra del " + l.getFecha().format(PanelClientes.FECHA) + " a "
                        + datos.nombreProveedor(l.getProveedorId()) + " (" + Ui.numero(l.getCantidadComprada())
                        + " " + ab + ").\n¿Cuánto queda realmente? (por merma, daño o conteo físico)",
                Ui.numeroCampo(l.getCantidadRestante()));
        if (r == null) return;
        Double c = Ui.leerNumero(r);
        if (c == null || c < 0 || c > l.getCantidadComprada() + 1e-9) {
            Ui.error(this, "Debe estar entre 0 y " + Ui.numero(l.getCantidadComprada()) + " " + ab + ".");
            return;
        }
        datos.ajustarRestante(l, c);
    }

    private void eliminarCompra() {
        Lote l = compraSeleccionada();
        if (l == null) { Ui.error(this, "Seleccione una compra de la tabla."); return; }
        if (!datos.puedeEliminarCompra(l)) {
            Ui.error(this, "De esta compra ya se usaron " + Ui.numero(l.getCantidadUsada()) + " "
                    + editando.getUnidad().getAbreviatura() + " en despachos o ajustes.\n"
                    + "Use \"Corregir compra\" o \"Ajustar lo que queda\" en su lugar.");
            return;
        }
        if (Ui.confirmar(this, "¿Eliminar la compra del " + l.getFecha().format(PanelClientes.FECHA) + " a "
                + datos.nombreProveedor(l.getProveedorId()) + "?")) {
            datos.eliminarCompra(l);
        }
    }

    private void mostrarCompras() {
        modeloCompras.setRowCount(0);
        modeloPrecios.setRowCount(0);
        if (editando == null) {
            compras = List.of();
            lblCompras.setText("Seleccione un ingrediente para ver sus compras");
            return;
        }
        String ab = editando.getUnidad().getAbreviatura();
        lblCompras.setText(editando.getNombre() + ": quedan " + Ui.numero(datos.existencia(editando)) + " " + ab
                + "  (se usa primero la compra más antigua)");
        compras = datos.compras(editando);
        for (Lote l : compras) {
            modeloCompras.addRow(new Object[]{l.getFecha().format(PanelClientes.FECHA),
                    datos.nombreProveedor(l.getProveedorId()),
                    Ui.numero(l.getCantidadComprada()) + " " + ab,
                    l.getCantidadRestante() > 1e-9 ? Ui.numero(l.getCantidadRestante()) + " " + ab : "agotada",
                    Ui.dinero(l.getPrecioPorUnidad()) + " / " + ab,
                    Ui.dinero(l.getTotalCompra())});
        }
        for (PrecioProveedor p : datos.preciosPorProveedor(editando)) {
            modeloPrecios.addRow(new Object[]{p.getProveedor(), Ui.dinero(p.getUltimoPrecio()) + " / " + ab,
                    Ui.dinero(p.getPrecioMinimo()) + " / " + ab, p.getCompras(),
                    p.getUltimaCompra().format(PanelClientes.FECHA)});
        }
    }

    // ------------------------------------------------------------ refresco

    private void refrescar() {
        Ingrediente sel = editando;
        filas = filtrar(datos.getIngredientes());
        int totalIngredientes = datos.getIngredientes().size();
        lblConteo.setText(txtBuscar.getText().isBlank() ? ""
                : filas.isEmpty() ? "Ningún ingrediente coincide"
                : "Mostrando " + filas.size() + " de " + totalIngredientes);
        modelo.setRowCount(0);
        double total = 0;
        for (Ingrediente i : filas) {
            String ab = i.getUnidad().getAbreviatura();
            double existencia = datos.existencia(i);
            double valor = datos.valorExistencia(i);
            List<PrecioProveedor> precios = datos.preciosPorProveedor(i);
            String mejor = precios.isEmpty() ? "—"
                    : Ui.dinero(precios.get(0).getUltimoPrecio()) + " (" + precios.get(0).getProveedor() + ")";
            modelo.addRow(new Object[]{i.getNombre(), i.getUnidad(),
                    Ui.numero(existencia) + " " + ab,
                    existencia > 1e-9 ? Ui.dinero(datos.costoPromedio(i)) + " / " + ab : "—",
                    precios.isEmpty() ? "—" : Ui.dinero(datos.ultimoPrecio(i)) + " / " + ab,
                    mejor,
                    Ui.dinero(valor)});
            total += valor;
        }
        total = datos.getIngredientes().stream().mapToDouble(datos::valorExistencia).sum();
        lblValor.setText(Ui.dinero(total));
        if (sel != null && filas.contains(sel)) {
            editando = sel;
            seleccionar(sel);
            mostrarCompras();
        } else {
            reiniciarFormulario();
        }
    }

    /** Ingredientes cuyo nombre contiene todas las palabras buscadas. */
    private List<Ingrediente> filtrar(List<Ingrediente> todos) {
        String q = BuscadorMenu.normalizar(txtBuscar.getText());
        if (q.isEmpty()) return List.copyOf(todos);
        String[] palabras = q.split(" ");
        return todos.stream().filter(i -> {
            String nombre = BuscadorMenu.normalizar(i.getNombre());
            for (String p : palabras) if (!nombre.contains(p)) return false;
            return true;
        }).collect(Collectors.toList());
    }
}
