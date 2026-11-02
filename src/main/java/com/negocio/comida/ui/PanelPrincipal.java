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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Pestaña principal (día de trabajo): menú del día, despachos a clientes,
 * planificación semanal, gastos (otros / combustible / gas / costo de
 * ingredientes) y el resumen de platos por cliente.
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
    private final BuscadorMenu buscadorDia;
    private final DefaultTableModel modeloMenuDia =
            Ui.modelo("Menú", "Precio", "Platos posibles", "Despachados", "Vendido");
    private final JTable tablaMenuDia = Ui.tabla(modeloMenuDia);
    private List<Menu> menusDia = List.of();

    // Menú de la semana
    private static final DateTimeFormatter DIA_SEMANA =
            DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JTabbedPane pestanasMenu = new JTabbedPane();
    private final BuscadorMenu buscadorSemana;
    private final JLabel lblSemana = new JLabel();
    private final DefaultTableModel modeloSemana = Ui.modelo("Día", "Menús planificados", "Platos despachados");
    private final JTable tablaSemana = Ui.tabla(modeloSemana);
    private List<LocalDate> diasSemana = List.of();

    // Despachos
    private final DefaultComboBoxModel<Cliente> modeloClientes = new DefaultComboBoxModel<>();
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>(modeloClientes);
    private final BuscadorMenu buscadorDespacho = new BuscadorMenu(() -> menusDia, "en el menú de este día");
    private final JSpinner spPlatos = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
    private final DefaultTableModel modeloDespachos = Ui.modelo("Cliente", "Menú", "Platos", "Total");
    private final JTable tablaDespachos = Ui.tabla(modeloDespachos);
    private List<Despacho> despachosDia = List.of();

    // Gastos
    private final JComboBox<TipoGasto> cmbTipo = new JComboBox<>(TipoGasto.manuales().toArray(new TipoGasto[0]));
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
        buscadorDia = new BuscadorMenu(datos::getMenus, "registrado");
        buscadorSemana = new BuscadorMenu(datos::getMenus, "registrado");
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(construirCabecera(), BorderLayout.NORTH);

        // Fila superior un poco más alta para que se vean los 7 días de la semana.
        JPanel centro = new JPanel(new GridBagLayout());
        agregarCelda(centro, construirMenuDelDia(), 0, 0, 0.58);
        agregarCelda(centro, construirDespachos(), 1, 0, 0.58);
        agregarCelda(centro, construirGastos(), 0, 1, 0.42);
        agregarCelda(centro, construirPorCliente(), 1, 1, 0.42);
        add(centro, BorderLayout.CENTER);

        datos.agregarOyente(this::refrescar);
        refrescar();
    }

    // ------------------------------------------------------------ construcción

    private static void agregarCelda(JPanel destino, JPanel celda, int x, int y, double pesoAlto) {
        java.awt.GridBagConstraints g = new java.awt.GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.weightx = 0.5;
        g.weighty = pesoAlto;
        g.fill = java.awt.GridBagConstraints.BOTH;
        g.insets = new java.awt.Insets(4, 4, 4, 4);
        celda.setPreferredSize(new java.awt.Dimension(100, 100));
        destino.add(celda, g);
    }

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
        JPanel p = Ui.titulo(new JPanel(new BorderLayout()), "Menú del día / de la semana");
        pestanasMenu.addTab("Día de trabajo", construirPestanaDia());
        pestanasMenu.addTab("Semana (lunes a domingo)", construirPestanaSemana());
        p.add(pestanasMenu);
        return p;
    }

    private JPanel construirPestanaDia() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton btnAgregar = new JButton("Agregar");
        JButton btnQuitar = new JButton("Quitar");
        barra.add(new JLabel("Menú:"));
        barra.add(buscadorDia);
        barra.add(btnAgregar);
        barra.add(btnQuitar);
        btnAgregar.addActionListener(e -> {
            Menu m = buscadorDia.getSeleccionado();
            if (m == null) { Ui.error(this, buscadorDia.motivoSinSeleccion()); return; }
            datos.agregarAlMenuDelDia(fecha, m);
            buscadorDia.limpiar();
            buscadorDia.enfocar();
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

    private JPanel construirPestanaSemana() {
        JPanel p = new JPanel(new BorderLayout(4, 4));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton btnAnterior = new JButton("◀");
        JButton btnSiguiente = new JButton("▶");
        btnAnterior.setToolTipText("Semana anterior");
        btnSiguiente.setToolTipText("Semana siguiente");
        btnAnterior.addActionListener(e -> spFecha.setValue(aDate(fecha.minusWeeks(1))));
        btnSiguiente.addActionListener(e -> spFecha.setValue(aDate(fecha.plusWeeks(1))));
        lblSemana.setFont(Ui.negrita(lblSemana.getFont(), 13f));
        nav.add(btnAnterior);
        nav.add(lblSemana);
        nav.add(btnSiguiente);
        JButton btnCopiar = new JButton("Copiar semana anterior");
        nav.add(btnCopiar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton btnAgregar = new JButton("Agregar a días seleccionados");
        JButton btnQuitar = new JButton("Quitar");
        acciones.add(new JLabel("Menú:"));
        acciones.add(buscadorSemana);
        acciones.add(btnAgregar);
        acciones.add(btnQuitar);
        btnAgregar.addActionListener(e -> agregarASemana());
        btnQuitar.addActionListener(e -> quitarDeSemana());
        btnCopiar.addActionListener(e -> {
            LocalDate lunes = DatosNegocio.inicioSemana(fecha);
            if (Ui.confirmar(this, "¿Copiar los menús de la semana del "
                    + lunes.minusWeeks(1).format(DIA_CORTO) + " a esta semana?")) {
                datos.copiarSemanaAnterior(lunes);
            }
        });

        JPanel norte = new JPanel(new GridLayout(2, 1));
        norte.add(nav);
        norte.add(acciones);
        p.add(norte, BorderLayout.NORTH);

        tablaSemana.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tablaSemana.setRowHeight(20);
        tablaSemana.getColumnModel().getColumn(0).setPreferredWidth(110);
        tablaSemana.getColumnModel().getColumn(1).setPreferredWidth(300);
        tablaSemana.getColumnModel().getColumn(2).setPreferredWidth(90);
        tablaSemana.setToolTipText("Seleccione uno o varios días (Ctrl/Shift + clic). Doble clic abre ese día.");
        tablaSemana.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = tablaSemana.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && r >= 0) {
                    spFecha.setValue(aDate(diasSemana.get(r)));
                    pestanasMenu.setSelectedIndex(0);
                }
            }
        });
        p.add(new JScrollPane(tablaSemana), BorderLayout.CENTER);
        return p;
    }

    private List<LocalDate> diasSeleccionados() {
        List<LocalDate> dias = new ArrayList<>();
        for (int r : tablaSemana.getSelectedRows()) dias.add(diasSemana.get(r));
        return dias;
    }

    private void agregarASemana() {
        Menu m = buscadorSemana.getSeleccionado();
        if (m == null) { Ui.error(this, buscadorSemana.motivoSinSeleccion()); return; }
        List<LocalDate> dias = diasSeleccionados();
        if (dias.isEmpty()) { Ui.error(this, "Seleccione uno o varios días de la tabla."); return; }
        for (LocalDate d : dias) datos.agregarAlMenuDelDia(d, m);
    }

    private void quitarDeSemana() {
        Menu m = buscadorSemana.getSeleccionado();
        if (m == null) { Ui.error(this, buscadorSemana.motivoSinSeleccion()); return; }
        List<LocalDate> dias = diasSeleccionados();
        if (dias.isEmpty()) { Ui.error(this, "Seleccione los días de los que desea quitar el menú."); return; }
        for (LocalDate d : dias) datos.quitarDelMenuDelDia(d, m);
    }

    private JPanel construirDespachos() {
        JPanel p = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Despachar platos");
        JPanel form = new JPanel(new GridBagLayout());
        form.add(new JLabel("Cliente:"), Ui.gbc(0, 0));
        form.add(cmbCliente, Ui.gbc(1, 0));
        form.add(new JLabel("Menú:"), Ui.gbc(2, 0));
        form.add(buscadorDespacho, Ui.gbc(3, 0));
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
        JPanel p = Ui.titulo(new JPanel(new BorderLayout(4, 4)), "Gastos del día (ingredientes, combustible, gas, otros)");
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
            Gasto gasto = gastosDia.get(r);
            if (gasto.esAutomatico()) {
                Ui.error(this, "Este gasto es el costo de ingredientes de un despacho.\n"
                        + "Para quitarlo, anule el despacho en \"Despachar platos\".");
                return;
            }
            datos.eliminarGasto(gasto);
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
        Menu m = buscadorDespacho.getSeleccionado();
        if (c == null) { Ui.error(this, "Registre clientes en la pestaña Clientes."); return; }
        if (m == null) {
            String motivo = menusDia.isEmpty()
                    ? "Agregue al menos un menú al menú de este día." : buscadorDespacho.motivoSinSeleccion();
            Ui.error(this, motivo);
            return;
        }
        int platos = (Integer) spPlatos.getValue();
        List<String> faltan = datos.faltantes(m, platos);
        if (!faltan.isEmpty() && !Ui.confirmar(this,
                "No alcanza el inventario:\n  " + String.join("\n  ", faltan)
                        + "\n\n¿Registrar el despacho de todos modos? (la existencia quedará en 0)")) {
            return;
        }
        datos.registrarDespacho(fecha, c, m, platos);
        spPlatos.setValue(1);
        // El costo de ingredientes queda registrado como gasto del día (ver "Gastos del día").
    }

    private void anularDespacho() {
        int r = tablaDespachos.getSelectedRow();
        if (r < 0) { Ui.error(this, "Seleccione un despacho."); return; }
        Despacho d = despachosDia.get(r);
        if (Ui.confirmar(this, "¿Anular el despacho de " + d.getPlatos() + " plato(s) a "
                + datos.nombreCliente(d.getClienteId()) + "?\nLos ingredientes regresarán al inventario y se quitará su costo de los gastos.")) {
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
        recargar(modeloClientes, List.copyOf(datos.getClientes()));

        // Menú del día
        menusDia = datos.getMenuDelDia(fecha);
        modeloMenuDia.setRowCount(0);
        for (Menu m : menusDia) {
            int despachados = datos.platosDelDia(fecha, m);
            modeloMenuDia.addRow(new Object[]{m.getNombre(), Ui.dinero(m.getPrecioVenta()),
                    datos.platosDisponibles(m), despachados, Ui.dinero(despachados * m.getPrecioVenta())});
        }

        // Semana (se conserva la selección de días)
        List<LocalDate> seleccion = diasSeleccionados();
        LocalDate lunes = DatosNegocio.inicioSemana(fecha);
        lblSemana.setText("Semana del " + lunes.format(DIA_CORTO) + " al " + lunes.plusDays(6).format(DIA_CORTO));
        List<LocalDate> dias = new ArrayList<>();
        modeloSemana.setRowCount(0);
        for (int i = 0; i < 7; i++) {
            LocalDate d = lunes.plusDays(i);
            dias.add(d);
            String menus = datos.getMenuDelDia(d).stream().map(Menu::getNombre).collect(Collectors.joining(", "));
            int platos = datos.platosDelDia(d, null);
            modeloSemana.addRow(new Object[]{(d.equals(fecha) ? "► " : "") + DIA_SEMANA.format(d),
                    menus.isEmpty() ? "—" : menus, platos == 0 ? "" : platos});
        }
        diasSemana = dias;
        for (LocalDate d : seleccion) {
            int idx = diasSemana.indexOf(d);
            if (idx >= 0) tablaSemana.addRowSelectionInterval(idx, idx);
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
        lblDesglose.setText(String.format(
                "Ingredientes: %s  |  Combustible: %s  |  Gas: %s  |  Otros: %s",
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.INGREDIENTES)),
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.COMBUSTIBLE)),
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.GAS)),
                Ui.dinero(datos.totalGastos(fecha, TipoGasto.OTROS))));

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
