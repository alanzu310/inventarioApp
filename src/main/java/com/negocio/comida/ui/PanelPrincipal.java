package com.negocio.comida.ui;

import com.negocio.comida.modelo.Cliente;
import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Despacho;
import com.negocio.comida.modelo.Gasto;
import com.negocio.comida.modelo.Menu;
import com.negocio.comida.modelo.TipoGasto;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Pestaña principal (día de trabajo): menú del día, despachos a clientes,
 * gastos de comida / combustible / gas y el resumen de platos por cliente.
 */
class PanelPrincipal extends JPanel {
    private final DatosNegocio datos;
    private LocalDate fecha = LocalDate.now();

    private final JSpinner spFecha = new JSpinner(new SpinnerDateModel());

    // Tarjetas resumen
    private final JLabel lblPlatos = tarjetaValor();
    private final JLabel lblVentas = tarjetaValor();
    private final JLabel lblGastos = tarjetaValor();
    private final JLabel lblUtilidad = tarjetaValor();

    // Menú del día
    private final DefaultComboBoxModel<Menu> modeloTodosMenus = new DefaultComboBoxModel<>();
    private final JComboBox<Menu> cmbMenus = new JComboBox<>(modeloTodosMenus);
    private final DefaultTableModel modeloMenuDia =
            Ui.modelo("Menú", "Precio", "Platos posibles", "Despachados", "Vendido");
    private final JTable tablaMenuDia = Ui.tabla(modeloMenuDia);
    private List<Menu> menusDia = List.of();

    // Despachos
    private final DefaultComboBoxModel<Cliente> modeloClientes = new DefaultComboBoxModel<>();
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>(modeloClientes);
    private final DefaultComboBoxModel<Menu> modeloMenusDespacho = new DefaultComboBoxModel<>();
    private final JComboBox<Menu> cmbMenuDespacho = new JComboBox<>(modeloMenusDespacho);
    private final JSpinner spPlatos = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
    private final DefaultTableModel modeloDespachos = Ui.modelo("Cliente", "Menú", "Platos", "Total");
    private final JTable tablaDespachos = Ui.tabla(modeloDespachos);
    private List<Despacho> despachosDia = List.of();

    // Gastos
    private final JComboBox<TipoGasto> cmbTipo = new JComboBox<>(TipoGasto.values());
    private final JTextField txtDescripcion = new JTextField(12);
    private final JTextField txtMonto = new JTextField(7);
    private final DefaultTableModel modeloGastos = Ui.modelo("Tipo", "Descripción", "Monto");
    private final JTable tablaGastos = Ui.tabla(modeloGastos);
    private final JLabel lblDesglose = new JLabel(" ");
    private List<Gasto> gastosDia = List.of();

    // Platos por cliente
    private final DefaultTableModel modeloPorCliente = Ui.modelo("Cliente", "Platos despachados", "Total $");
    private final JTable tablaPorCliente = Ui.tabla(modeloPorCliente);

    PanelPrincipal(DatosNegocio datos) {
        super(new BorderLayout(8, 8));
        this.datos = datos;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(construirCabecera(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(2, 2, 8, 8));
        centro.add(construirMenuDelDia());
        centro.add(construirDespachos());
        centro.add(construirGastos());
        centro.add(construirPorCliente());
        add(centro, BorderLayout.CENTER);

        datos.agregarOyente(this::refrescar);
        refrescar();
    }

    // ------------------------------------------------------------ construcción

    private JPanel construirCabecera() {
        JPanel cab = new JPanel(new BorderLayout(8, 8));

        JPanel barraFecha = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel titulo = new JLabel("Día de trabajo:");
        titulo.setFont(Ui.negrita(titulo.getFont(), 15f));
        spFecha.setEditor(new JSpinner.DateEditor(spFecha, "EEEE dd/MM/yyyy"));
        ((JSpinner.DefaultEditor) spFecha.getEditor()).getTextField().setColumns(16);
        spFecha.setValue(aDate(fecha));
        spFecha.addChangeListener(e -> {
            fecha = ((Date) spFecha.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            refrescar();
        });
        JButton btnAnterior = new JButton("◀ Día anterior");
        JButton btnHoy = new JButton("Hoy");
        JButton btnSiguiente = new JButton("Día siguiente ▶");
        btnAnterior.addActionListener(e -> spFecha.setValue(aDate(fecha.minusDays(1))));
        btnHoy.addActionListener(e -> spFecha.setValue(aDate(LocalDate.now())));
        btnSiguiente.addActionListener(e -> spFecha.setValue(aDate(fecha.plusDays(1))));
        barraFecha.add(titulo);
        barraFecha.add(spFecha);
        barraFecha.add(btnAnterior);
        barraFecha.add(btnHoy);
        barraFecha.add(btnSiguiente);
        cab.add(barraFecha, BorderLayout.NORTH);

        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 8, 0));
        tarjetas.add(tarjeta("Platos despachados", lblPlatos));
        tarjetas.add(tarjeta("Ventas del día", lblVentas));
        tarjetas.add(tarjeta("Gastos del día", lblGastos));
        tarjetas.add(tarjeta("Utilidad (ventas − gastos)", lblUtilidad));
        cab.add(tarjetas, BorderLayout.CENTER);
        return cab;
    }

    private JPanel construirMenuDelDia() {
        JPanel p = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Menú del día");
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnAgregar = new JButton("Agregar");
        JButton btnQuitar = new JButton("Quitar");
        barra.add(new JLabel("Menú:"));
        barra.add(cmbMenus);
        barra.add(btnAgregar);
        barra.add(btnQuitar);
        btnAgregar.addActionListener(e -> {
            Menu m = (Menu) cmbMenus.getSelectedItem();
            if (m == null) { Ui.error(this, "Cree menús en la pestaña Menús."); return; }
            datos.agregarAlMenuDelDia(fecha, m);
        });
        btnQuitar.addActionListener(e -> {
            int r = tablaMenuDia.getSelectedRow();
            if (r < 0) { Ui.error(this, "Seleccione un menú de la tabla."); return; }
            datos.quitarDelMenuDelDia(fecha, menusDia.get(r));
        });
        p.add(barra, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaMenuDia), BorderLayout.CENTER);
        return p;
    }

    private JPanel construirDespachos() {
        JPanel p = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Despachar platos");
        JPanel form = new JPanel(new GridBagLayout());
        form.add(new JLabel("Cliente:"), Ui.gbc(0, 0));
        form.add(cmbCliente, Ui.gbc(1, 0));
        form.add(new JLabel("Menú:"), Ui.gbc(2, 0));
        form.add(cmbMenuDespacho, Ui.gbc(3, 0));
        form.add(new JLabel("Platos:"), Ui.gbc(0, 1));
        form.add(spPlatos, Ui.gbc(1, 1));
        JButton btnRegistrar = new JButton("Registrar despacho");
        JButton btnEliminar = new JButton("Anular seleccionado");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botones.add(btnRegistrar);
        botones.add(btnEliminar);
        var g = Ui.gbc(2, 1);
        g.gridwidth = 2;
        form.add(botones, g);
        btnRegistrar.addActionListener(e -> registrarDespacho());
        btnEliminar.addActionListener(e -> anularDespacho());
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaDespachos), BorderLayout.CENTER);
        return p;
    }

    private JPanel construirGastos() {
        JPanel p = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Gastos del día (comida, combustible, gas)");
        JPanel form = new JPanel(new GridBagLayout());
        form.add(new JLabel("Tipo:"), Ui.gbc(0, 0));
        form.add(cmbTipo, Ui.gbc(1, 0));
        form.add(new JLabel("Monto:"), Ui.gbc(2, 0));
        form.add(txtMonto, Ui.gbc(3, 0));
        form.add(new JLabel("Detalle:"), Ui.gbc(0, 1));
        form.add(txtDescripcion, Ui.gbc(1, 1));
        JButton btnAgregar = new JButton("Agregar gasto");
        JButton btnEliminar = new JButton("Eliminar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botones.add(btnAgregar);
        botones.add(btnEliminar);
        var g = Ui.gbc(2, 1);
        g.gridwidth = 2;
        form.add(botones, g);
        btnAgregar.addActionListener(e -> agregarGasto());
        txtMonto.addActionListener(e -> agregarGasto());
        btnEliminar.addActionListener(e -> {
            int r = tablaGastos.getSelectedRow();
            if (r < 0) { Ui.error(this, "Seleccione un gasto."); return; }
            datos.eliminarGasto(gastosDia.get(r));
        });
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaGastos), BorderLayout.CENTER);
        p.add(lblDesglose, BorderLayout.SOUTH);
        return p;
    }

    private JPanel construirPorCliente() {
        JPanel p = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Platos despachados por cliente");
        p.add(new JScrollPane(tablaPorCliente), BorderLayout.CENTER);
        return p;
    }

    private static JLabel tarjetaValor() {
        JLabel l = new JLabel("0", SwingConstants.CENTER);
        l.setFont(Ui.negrita(l.getFont(), 22f));
        return l;
    }

    private static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC8, 0xC8, 0xC8)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel t = new JLabel(titulo, SwingConstants.CENTER);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private static Date aDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ------------------------------------------------------------ acciones

    private void registrarDespacho() {
        Cliente c = (Cliente) cmbCliente.getSelectedItem();
        Menu m = (Menu) cmbMenuDespacho.getSelectedItem();
        if (c == null) { Ui.error(this, "Registre clientes en la pestaña Clientes."); return; }
        if (m == null) { Ui.error(this, "Agregue al menos un menú al menú del día."); return; }
        int platos = (Integer) spPlatos.getValue();
        List<String> faltan = datos.faltantes(m, platos);
        if (!faltan.isEmpty() && !Ui.confirmar(this,
                "No alcanza el inventario:\n  " + String.join("\n  ", faltan)
                        + "\n\n¿Registrar el despacho de todos modos? (la existencia quedará en 0)")) {
            return;
        }
        datos.registrarDespacho(fecha, c, m, platos);
        spPlatos.setValue(1);
    }

    private void anularDespacho() {
        int r = tablaDespachos.getSelectedRow();
        if (r < 0) { Ui.error(this, "Seleccione un despacho."); return; }
        Despacho d = despachosDia.get(r);
        if (Ui.confirmar(this, "¿Anular el despacho de " + d.getPlatos() + " plato(s) a "
                + datos.nombreCliente(d.getClienteId()) + "?\nLos ingredientes regresarán al inventario.")) {
            datos.eliminarDespacho(d);
        }
    }

    private void agregarGasto() {
        Double monto = Ui.leerNumero(txtMonto.getText());
        if (monto == null || monto <= 0) { Ui.error(this, "Escriba un monto mayor a 0."); return; }
        datos.agregarGasto(fecha, (TipoGasto) cmbTipo.getSelectedItem(), txtDescripcion.getText().trim(), monto);
        txtDescripcion.setText("");
        txtMonto.setText("");
    }

    // ------------------------------------------------------------ refresco

    private static <T> void recargar(DefaultComboBoxModel<T> modelo, List<T> elementos) {
        Object sel = modelo.getSelectedItem();
        modelo.removeAllElements();
        for (T e : elementos) modelo.addElement(e);
        if (sel != null && elementos.contains(sel)) modelo.setSelectedItem(sel);
    }

    private void refrescar() {
        recargar(modeloTodosMenus, List.copyOf(datos.getMenus()));
        recargar(modeloClientes, List.copyOf(datos.getClientes()));

        // Menú del día
        menusDia = datos.getMenuDelDia(fecha);
        recargar(modeloMenusDespacho, menusDia);
        modeloMenuDia.setRowCount(0);
        for (Menu m : menusDia) {
            int despachados = datos.platosDelDia(fecha, m);
            modeloMenuDia.addRow(new Object[]{m.getNombre(), Ui.dinero(m.getPrecioVenta()),
                    datos.platosDisponibles(m), despachados, Ui.dinero(despachados * m.getPrecioVenta())});
        }

        // Despachos
        despachosDia = datos.getDespachos(fecha);
        modeloDespachos.setRowCount(0);
        for (Despacho d : despachosDia) {
            modeloDespachos.addRow(new Object[]{datos.nombreCliente(d.getClienteId()), d.getMenuNombre(),
                    d.getPlatos(), Ui.dinero(d.getTotal())});
        }

        // Gastos
        gastosDia = datos.getGastos(fecha);
        modeloGastos.setRowCount(0);
        for (Gasto g : gastosDia) {
            modeloGastos.addRow(new Object[]{g.getTipo(), g.getDescripcion(), Ui.dinero(g.getMonto())});
        }
        lblDesglose.setText(String.format("Comida: %s   |   Combustible: %s   |   Gas de cocina: %s",
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.COMIDA)),
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.COMBUSTIBLE)),
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.GAS))));

        // Platos por cliente
        modeloPorCliente.setRowCount(0);
        for (Map.Entry<Cliente, double[]> e : datos.resumenPorCliente(fecha).entrySet()) {
            modeloPorCliente.addRow(new Object[]{e.getKey().getNombre(), (int) e.getValue()[0],
                    Ui.dinero(e.getValue()[1])});
        }

        // Tarjetas
        double ventas = datos.ventasDelDia(fecha);
        double gastos = datos.totalGastos(fecha, null);
        double utilidad = ventas - gastos;
        lblPlatos.setText(String.valueOf(datos.platosDelDia(fecha, null)));
        lblVentas.setText(Ui.dinero(ventas));
        lblGastos.setText(Ui.dinero(gastos));
        lblUtilidad.setText(Ui.dinero(utilidad));
        lblUtilidad.setForeground(utilidad < 0 ? new Color(0xB0, 0x20, 0x20) : new Color(0x1E, 0x7B, 0x34));
    }
}
