package com.negocio.comida.reporte;

import static com.negocio.comida.reporte.Xlsx.DECIMAL;
import static com.negocio.comida.reporte.Xlsx.DINERO;
import static com.negocio.comida.reporte.Xlsx.DINERO_NEGRITA;
import static com.negocio.comida.reporte.Xlsx.FECHA;
import static com.negocio.comida.reporte.Xlsx.NEGRITA;
import static com.negocio.comida.reporte.Xlsx.NORMAL;
import static com.negocio.comida.reporte.Xlsx.TITULO;
import static com.negocio.comida.reporte.Xlsx.TOTAL_DINERO;
import static com.negocio.comida.reporte.Xlsx.TOTAL_ETIQUETA;
import static com.negocio.comida.reporte.Xlsx.TOTAL_NUMERO;
import static com.negocio.comida.reporte.Xlsx.col;

import com.negocio.comida.modelo.Cliente;
import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.DatosNegocio.PrecioProveedor;
import com.negocio.comida.modelo.Despacho;
import com.negocio.comida.modelo.Gasto;
import com.negocio.comida.modelo.Ingrediente;
import com.negocio.comida.modelo.ItemMenu;
import com.negocio.comida.modelo.Lote;
import com.negocio.comida.modelo.Menu;
import com.negocio.comida.modelo.Proveedor;
import com.negocio.comida.modelo.TipoGasto;
import com.negocio.comida.reporte.Xlsx.Hoja;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Reporte completo del negocio en Excel: resumen, ventas por día, despachos,
 * gastos, clientes, menús, inventario, compras, proveedores y planificación.
 * Los movimientos se filtran por periodo; inventario y menús muestran el
 * estado actual.
 */
public final class ReporteCompleto {

    private static final int FILA_CABECERA = 4;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("es"));

    private final DatosNegocio d;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final List<Despacho> despachos;
    private final List<Gasto> gastos;
    private final List<Lote> compras;

    private ReporteCompleto(DatosNegocio datos, LocalDate desde, LocalDate hasta) {
        this.d = datos;
        this.desde = desde;
        this.hasta = hasta;
        this.despachos = datos.getDespachos().stream().filter(x -> enPeriodo(x.getFecha()))
                .sorted(Comparator.comparing(Despacho::getFecha).thenComparingLong(Despacho::getId))
                .collect(Collectors.toList());
        this.gastos = datos.getGastos().stream().filter(x -> enPeriodo(x.getFecha()))
                .sorted(Comparator.comparing(Gasto::getFecha).thenComparingLong(Gasto::getId))
                .collect(Collectors.toList());
        this.compras = datos.getLotes().stream().filter(x -> enPeriodo(x.getFecha()))
                .sorted(Comparator.comparing(Lote::getFecha).thenComparingLong(Lote::getId))
                .collect(Collectors.toList());
    }

    public static void exportar(DatosNegocio datos, LocalDate desde, LocalDate hasta, Path archivo) throws IOException {
        ReporteCompleto r = new ReporteCompleto(datos, desde, hasta);
        Xlsx.escribir(archivo, List.of(r.resumen(), r.ventasPorDia(), r.despachos(), r.gastos(), r.clientes(),
                r.menus(), r.inventario(), r.compras(), r.proveedores(), r.planificacion()));
    }

    /** Nombre de archivo sugerido, sin extensión. */
    public static String nombreArchivo(LocalDate desde, LocalDate hasta) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "Reporte_completo_" + desde.format(f) + "_a_" + hasta.format(f);
    }

    private boolean enPeriodo(LocalDate f) {
        return !f.isBefore(desde) && !f.isAfter(hasta);
    }

    private String periodo() {
        return "Del " + desde.format(ReporteVentas.FECHA) + " al " + hasta.format(ReporteVentas.FECHA);
    }

    // ------------------------------------------------------------ utilidades de hojas

    /** Título, subtítulo y periodo en las filas 1-2; la cabecera va en la fila 4. */
    private Hoja nueva(String nombre, String subtitulo, boolean conPeriodo) {
        Hoja h = new Hoja(nombre);
        String negocio = d.getNombreNegocio();
        h.texto("A", 1, (negocio.isEmpty() ? "" : negocio + " - ") + subtitulo, TITULO);
        h.finFila();
        h.texto("A", 2, conPeriodo ? periodo() : "Estado al " + LocalDate.now().format(ReporteVentas.FECHA), NEGRITA);
        h.finFila();
        return h;
    }

    private static void cabecera(Hoja h, String... titulos) {
        boolean[] der = new boolean[titulos.length];
        for (int i = 0; i < titulos.length; i++) der[i] = titulos[i].startsWith(">");
        String[] limpios = new String[titulos.length];
        for (int i = 0; i < titulos.length; i++) limpios[i] = titulos[i].replaceFirst("^>", "");
        h.cabecera(FILA_CABECERA, limpios, der);
    }

    /** Fila de totales: etiqueta en la primera columna y SUM en las columnas indicadas. */
    private static void totales(Hoja h, int fila, int primera, int ultima, int columnas, Map<Integer, double[]> sumas) {
        h.texto("A", fila, "TOTAL", TOTAL_ETIQUETA);
        for (int c = 1; c < columnas; c++) {
            double[] v = sumas.get(c);
            if (v == null) {
                h.texto(col(c), fila, "", TOTAL_ETIQUETA);
            } else {
                int estilo = (int) v[1];
                h.formula(col(c), fila, "SUM(" + col(c) + primera + ":" + col(c) + ultima + ")", v[0], estilo);
            }
        }
        h.finFila();
    }

    private Hoja terminar(Hoja h, int ultimaFila, int columnas, double... anchos) {
        h.anchos(anchos).fijarFilas(FILA_CABECERA);
        if (columnas >= 7) h.horizontal();
        if (ultimaFila > FILA_CABECERA) h.filtro("A" + FILA_CABECERA + ":" + col(columnas - 1) + ultimaFila);
        return h;
    }

    private static void sinDatos(Hoja h, int fila) {
        h.texto("A", fila, "Sin registros en el periodo.", NORMAL);
        h.finFila();
    }

    // ------------------------------------------------------------ hojas

    private Hoja resumen() {
        Hoja h = new Hoja("Resumen");
        String negocio = d.getNombreNegocio();
        h.texto("A", 1, negocio.isEmpty() ? "Reporte completo del negocio" : negocio, TITULO);
        h.finFila();
        if (!negocio.isEmpty()) {
            h.texto("A", 2, "Reporte completo del negocio", NEGRITA);
            h.finFila();
        }
        h.texto("A", 3, "Periodo:", NEGRITA);
        h.texto("B", 3, periodo(), NORMAL);
        h.finFila();
        h.texto("A", 4, "Fecha de emisión:", NEGRITA);
        h.fecha("B", 4, LocalDate.now(), FECHA);
        h.finFila();

        int platos = despachos.stream().mapToInt(Despacho::getPlatos).sum();
        double ventas = despachos.stream().mapToDouble(Despacho::getTotal).sum();
        int f = 6;
        h.texto("A", f, "VENTAS", Xlsx.CABECERA);
        h.texto("B", f, "", Xlsx.CABECERA);
        h.finFila();
        f = fila(h, f + 1, "Platos despachados", platos, NORMAL);
        f = fila(h, f, "Despachos registrados", despachos.size(), NORMAL);
        f = fila(h, f, "Clientes atendidos", despachos.stream().map(Despacho::getClienteId).distinct().count(), NORMAL);
        int filaVentas = f;
        f = fila(h, f, "Total vendido", ventas, DINERO_NEGRITA);

        f++;
        h.texto("A", f, "GASTOS", Xlsx.CABECERA);
        h.texto("B", f, "", Xlsx.CABECERA);
        h.finFila();
        int primeraGasto = f + 1;
        f = fila(h, f + 1, "Costo de ingredientes", gastoTipo(TipoGasto.INGREDIENTES), DINERO);
        f = fila(h, f, "Combustible", gastoTipo(TipoGasto.COMBUSTIBLE), DINERO);
        f = fila(h, f, "Gas de cocina", gastoTipo(TipoGasto.GAS), DINERO);
        f = fila(h, f, "Otros", gastoTipo(TipoGasto.OTROS), DINERO);
        double totalGastos = gastos.stream().mapToDouble(Gasto::getMonto).sum();
        int filaGastos = f;
        h.texto("A", f, "Total gastos", TOTAL_ETIQUETA);
        h.formula("B", f, "SUM(B" + primeraGasto + ":B" + (f - 1) + ")", totalGastos, TOTAL_DINERO);
        h.finFila();
        f += 2;
        h.texto("A", f, "UTILIDAD (ventas - gastos)", TOTAL_ETIQUETA);
        h.formula("B", f, "B" + filaVentas + "-B" + filaGastos, ventas - totalGastos, TOTAL_DINERO);
        h.finFila();

        f += 2;
        h.texto("A", f, "COMPRAS E INVENTARIO", Xlsx.CABECERA);
        h.texto("B", f, "", Xlsx.CABECERA);
        h.finFila();
        f = fila(h, f + 1, "Compras en el periodo", compras.size(), NORMAL);
        f = fila(h, f, "Total pagado en compras", compras.stream().mapToDouble(Lote::getTotalCompra).sum(), DINERO);
        f = fila(h, f, "Valor actual del inventario",
                d.getIngredientes().stream().mapToDouble(d::valorExistencia).sum(), DINERO);
        f = fila(h, f, "Ingredientes registrados", d.getIngredientes().size(), NORMAL);
        f = fila(h, f, "Menús registrados", d.getMenus().size(), NORMAL);
        f = fila(h, f, "Clientes registrados", d.getClientes().size(), NORMAL);
        fila(h, f, "Proveedores registrados", d.getProveedores().size(), NORMAL);
        return h.anchos(34, 22);
    }

    private int fila(Hoja h, int f, String etiqueta, double valor, int estilo) {
        h.texto("A", f, etiqueta, NORMAL);
        h.numero("B", f, valor, estilo);
        h.finFila();
        return f + 1;
    }

    private double gastoTipo(TipoGasto t) {
        return gastos.stream().filter(g -> g.getTipo() == t).mapToDouble(Gasto::getMonto).sum();
    }

    private Hoja ventasPorDia() {
        Hoja h = nueva("Ventas por día", "Ventas y gastos por día", true);
        cabecera(h, "Fecha", "Día", ">Platos", ">Ventas", ">Costo ingredientes", ">Combustible", ">Gas",
                ">Otros", ">Total gastos", ">Utilidad");
        TreeSet<LocalDate> fechas = new TreeSet<>();
        despachos.forEach(x -> fechas.add(x.getFecha()));
        gastos.forEach(x -> fechas.add(x.getFecha()));
        int f = FILA_CABECERA;
        double[] t = new double[8];
        for (LocalDate fecha : fechas) {
            f++;
            int platos = despachos.stream().filter(x -> x.getFecha().equals(fecha)).mapToInt(Despacho::getPlatos).sum();
            double ventas = despachos.stream().filter(x -> x.getFecha().equals(fecha)).mapToDouble(Despacho::getTotal).sum();
            double[] g = new double[4];
            TipoGasto[] tipos = {TipoGasto.INGREDIENTES, TipoGasto.COMBUSTIBLE, TipoGasto.GAS, TipoGasto.OTROS};
            for (int i = 0; i < 4; i++) {
                TipoGasto tipo = tipos[i];
                g[i] = gastos.stream().filter(x -> x.getFecha().equals(fecha) && x.getTipo() == tipo)
                        .mapToDouble(Gasto::getMonto).sum();
            }
            double totalG = g[0] + g[1] + g[2] + g[3];
            h.fecha("A", f, fecha, FECHA);
            h.texto("B", f, DIA.format(fecha), NORMAL);
            h.numero("C", f, platos, NORMAL);
            h.numero("D", f, ventas, DINERO);
            for (int i = 0; i < 4; i++) h.numero(col(4 + i), f, g[i], DINERO);
            h.formula("I", f, "SUM(E" + f + ":H" + f + ")", totalG, DINERO);
            h.formula("J", f, "D" + f + "-I" + f, ventas - totalG, DINERO);
            h.finFila();
            t[0] += platos;
            t[1] += ventas;
            for (int i = 0; i < 4; i++) t[2 + i] += g[i];
            t[6] += totalG;
            t[7] += ventas - totalG;
        }
        if (fechas.isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 10, Map.of(2, new double[]{t[0], TOTAL_NUMERO},
                3, new double[]{t[1], TOTAL_DINERO}, 4, new double[]{t[2], TOTAL_DINERO},
                5, new double[]{t[3], TOTAL_DINERO}, 6, new double[]{t[4], TOTAL_DINERO},
                7, new double[]{t[5], TOTAL_DINERO}, 8, new double[]{t[6], TOTAL_DINERO},
                9, new double[]{t[7], TOTAL_DINERO}));
        return terminar(h, fechas.isEmpty() ? FILA_CABECERA : f, 10, 12, 11, 9, 13, 18, 13, 11, 11, 14, 13);
    }

    private Hoja despachos() {
        Hoja h = nueva("Despachos", "Despachos de platos", true);
        cabecera(h, "Fecha", "Cliente", "Menú", ">Platos", ">Precio unitario", ">Total",
                ">Costo ingredientes", ">Ganancia");
        int f = FILA_CABECERA;
        double platos = 0, total = 0, costo = 0;
        for (Despacho x : despachos) {
            f++;
            double c = d.costoDespacho(x);
            h.fecha("A", f, x.getFecha(), FECHA);
            h.texto("B", f, d.nombreCliente(x.getClienteId()), NORMAL);
            h.texto("C", f, x.getMenuNombre(), NORMAL);
            h.numero("D", f, x.getPlatos(), NORMAL);
            h.numero("E", f, x.getPrecioUnitario(), DINERO);
            h.formula("F", f, "D" + f + "*E" + f, x.getTotal(), DINERO);
            h.numero("G", f, c, DINERO);
            h.formula("H", f, "F" + f + "-G" + f, x.getTotal() - c, DINERO);
            h.finFila();
            platos += x.getPlatos();
            total += x.getTotal();
            costo += c;
        }
        if (despachos.isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 8, Map.of(3, new double[]{platos, TOTAL_NUMERO},
                5, new double[]{total, TOTAL_DINERO}, 6, new double[]{costo, TOTAL_DINERO},
                7, new double[]{total - costo, TOTAL_DINERO}));
        return terminar(h, despachos.isEmpty() ? FILA_CABECERA : f, 8, 12, 28, 30, 9, 15, 13, 18, 13);
    }

    private Hoja gastos() {
        Hoja h = nueva("Gastos", "Gastos", true);
        cabecera(h, "Fecha", "Tipo", "Descripción", ">Monto");
        int f = FILA_CABECERA;
        double total = 0;
        for (Gasto g : gastos) {
            f++;
            h.fecha("A", f, g.getFecha(), FECHA);
            h.texto("B", f, g.getTipo().toString(), NORMAL);
            h.texto("C", f, g.getDescripcion(), NORMAL);
            h.numero("D", f, g.getMonto(), DINERO);
            h.finFila();
            total += g.getMonto();
        }
        if (gastos.isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 4, Map.of(3, new double[]{total, TOTAL_DINERO}));
        return terminar(h, gastos.isEmpty() ? FILA_CABECERA : f, 4, 12, 22, 48, 14);
    }

    private Hoja clientes() {
        Hoja h = nueva("Clientes", "Clientes (ventas del periodo)", true);
        cabecera(h, "Cliente", "Teléfono", "Dirección", ">Días con despacho", ">Platos", ">Total vendido");
        int f = FILA_CABECERA;
        double platos = 0, total = 0;
        for (Cliente c : d.getClientes()) {
            List<Despacho> suyos = despachos.stream().filter(x -> x.getClienteId() == c.getId()).collect(Collectors.toList());
            int p = suyos.stream().mapToInt(Despacho::getPlatos).sum();
            double t = suyos.stream().mapToDouble(Despacho::getTotal).sum();
            f++;
            h.texto("A", f, c.getNombre(), NORMAL);
            h.texto("B", f, nn(c.getTelefono()), NORMAL);
            h.texto("C", f, nn(c.getDireccion()), NORMAL);
            h.numero("D", f, suyos.stream().map(Despacho::getFecha).distinct().count(), NORMAL);
            h.numero("E", f, p, NORMAL);
            h.numero("F", f, t, DINERO);
            h.finFila();
            platos += p;
            total += t;
        }
        if (d.getClientes().isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 6, Map.of(4, new double[]{platos, TOTAL_NUMERO},
                5, new double[]{total, TOTAL_DINERO}));
        return terminar(h, d.getClientes().isEmpty() ? FILA_CABECERA : f, 6, 30, 15, 30, 18, 10, 15);
    }

    private Hoja menus() {
        Hoja h = nueva("Menús", "Menús", false);
        cabecera(h, "Menú", ">Precio de venta", ">Costo actual por plato", ">Ganancia por plato",
                ">Platos posibles", ">Platos vendidos (periodo)", ">Ventas (periodo)", "Ingredientes por plato");
        int f = FILA_CABECERA;
        for (Menu m : d.getMenus()) {
            f++;
            double costo = d.costoPlato(m);
            List<Despacho> vendidos = despachos.stream().filter(x -> x.getMenuId() == m.getId()).collect(Collectors.toList());
            String ingredientes = m.getItems().stream().map(this::describir).collect(Collectors.joining("; "));
            h.texto("A", f, m.getNombre(), NORMAL);
            h.numero("B", f, m.getPrecioVenta(), DINERO);
            h.numero("C", f, costo, DINERO);
            h.formula("D", f, "B" + f + "-C" + f, m.getPrecioVenta() - costo, DINERO);
            h.numero("E", f, d.platosDisponibles(m), NORMAL);
            h.numero("F", f, vendidos.stream().mapToInt(Despacho::getPlatos).sum(), NORMAL);
            h.numero("G", f, vendidos.stream().mapToDouble(Despacho::getTotal).sum(), DINERO);
            h.texto("H", f, ingredientes, NORMAL);
            h.finFila();
        }
        if (d.getMenus().isEmpty()) sinDatos(h, ++f);
        h.texto("A", f + 2, "El costo actual usa las compras en existencia (primero las más antiguas).", NORMAL);
        h.finFila();
        return terminar(h, d.getMenus().isEmpty() ? FILA_CABECERA : f, 8, 28, 15, 20, 17, 14, 22, 15, 50);
    }

    private String describir(ItemMenu it) {
        return d.buscarIngrediente(it.getIngredienteId())
                .map(i -> i.getNombre() + " " + cantidad(it.getCantidad()) + " " + i.getUnidad().getAbreviatura())
                .orElse("(ingrediente eliminado)");
    }

    private Hoja inventario() {
        Hoja h = nueva("Inventario", "Inventario", false);
        cabecera(h, "Ingrediente", "Unidad", ">Existencia", ">Costo promedio", ">Último precio",
                ">Mejor precio", "Proveedor más barato", ">Valor en inventario");
        int f = FILA_CABECERA;
        double valor = 0;
        for (Ingrediente i : d.getIngredientes()) {
            f++;
            List<PrecioProveedor> precios = d.preciosPorProveedor(i);
            double v = d.valorExistencia(i);
            h.texto("A", f, i.getNombre(), NORMAL);
            h.texto("B", f, i.getUnidad().toString(), NORMAL);
            h.numero("C", f, d.existencia(i), DECIMAL);
            h.numero("D", f, d.costoPromedio(i), DINERO);
            h.numero("E", f, d.ultimoPrecio(i), DINERO);
            if (precios.isEmpty()) {
                h.texto("F", f, "", NORMAL);
                h.texto("G", f, "", NORMAL);
            } else {
                h.numero("F", f, precios.get(0).getUltimoPrecio(), DINERO);
                h.texto("G", f, precios.get(0).getProveedor(), NORMAL);
            }
            h.numero("H", f, v, DINERO);
            h.finFila();
            valor += v;
        }
        if (d.getIngredientes().isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 8, Map.of(7, new double[]{valor, TOTAL_DINERO}));
        return terminar(h, d.getIngredientes().isEmpty() ? FILA_CABECERA : f, 8, 26, 10, 13, 15, 14, 14, 22, 18);
    }

    private Hoja compras() {
        Hoja h = nueva("Compras", "Compras de ingredientes", true);
        cabecera(h, "Fecha", "Ingrediente", "Proveedor", ">Cantidad", "Unidad", ">Precio por unidad",
                ">Total pagado", ">Queda");
        int f = FILA_CABECERA;
        double total = 0;
        for (Lote l : compras) {
            f++;
            Ingrediente i = d.buscarIngrediente(l.getIngredienteId()).orElse(null);
            h.fecha("A", f, l.getFecha(), FECHA);
            h.texto("B", f, i == null ? "(eliminado)" : i.getNombre(), NORMAL);
            h.texto("C", f, d.nombreProveedor(l.getProveedorId()), NORMAL);
            h.numero("D", f, l.getCantidadComprada(), DECIMAL);
            h.texto("E", f, i == null ? "" : i.getUnidad().getAbreviatura(), NORMAL);
            h.numero("F", f, l.getPrecioPorUnidad(), DINERO);
            h.formula("G", f, "D" + f + "*F" + f, l.getTotalCompra(), DINERO);
            h.numero("H", f, l.getCantidadRestante(), DECIMAL);
            h.finFila();
            total += l.getTotalCompra();
        }
        if (compras.isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 8, Map.of(6, new double[]{total, TOTAL_DINERO}));
        return terminar(h, compras.isEmpty() ? FILA_CABECERA : f, 8, 12, 24, 24, 11, 8, 17, 14, 11);
    }

    private Hoja proveedores() {
        Hoja h = nueva("Proveedores", "Proveedores (compras del periodo)", true);
        cabecera(h, "Proveedor", "Teléfono", ">Compras", ">Total comprado", "Última compra");
        int f = FILA_CABECERA;
        double total = 0;
        List<Proveedor> lista = new ArrayList<>(d.getProveedores());
        for (Proveedor p : lista) {
            List<Lote> suyas = compras.stream().filter(l -> l.getProveedorId() == p.getId()).collect(Collectors.toList());
            double t = suyas.stream().mapToDouble(Lote::getTotalCompra).sum();
            List<Lote> todas = d.compras(p);
            f++;
            h.texto("A", f, p.getNombre(), NORMAL);
            h.texto("B", f, nn(p.getTelefono()), NORMAL);
            h.numero("C", f, suyas.size(), NORMAL);
            h.numero("D", f, t, DINERO);
            if (todas.isEmpty()) h.texto("E", f, "", NORMAL);
            else h.fecha("E", f, todas.get(0).getFecha(), FECHA);
            h.finFila();
            total += t;
        }
        if (lista.isEmpty()) sinDatos(h, ++f);
        else totales(h, f + 2, FILA_CABECERA + 1, f, 5, Map.of(3, new double[]{total, TOTAL_DINERO}));
        return terminar(h, lista.isEmpty() ? FILA_CABECERA : f, 5, 28, 15, 10, 16, 15);
    }

    private Hoja planificacion() {
        Hoja h = nueva("Planificación", "Menús planificados", true);
        cabecera(h, "Fecha", "Día", "Menús planificados", ">Platos despachados");
        Map<LocalDate, List<Long>> plan = new TreeMap<>(d.getPlanificacion());
        int f = FILA_CABECERA;
        for (Map.Entry<LocalDate, List<Long>> e : plan.entrySet()) {
            if (!enPeriodo(e.getKey())) continue;
            List<Menu> menus = d.getMenuDelDia(e.getKey());
            if (menus.isEmpty()) continue;
            f++;
            h.fecha("A", f, e.getKey(), FECHA);
            h.texto("B", f, DIA.format(e.getKey()), NORMAL);
            h.texto("C", f, menus.stream().map(Menu::getNombre).collect(Collectors.joining(", ")), NORMAL);
            h.numero("D", f, d.platosDelDia(e.getKey(), null), NORMAL);
            h.finFila();
        }
        boolean vacio = f == FILA_CABECERA;
        if (vacio) sinDatos(h, ++f);
        return terminar(h, vacio ? FILA_CABECERA : f, 4, 12, 11, 50, 18);
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }

    /** Cantidad para texto (hasta 3 decimales, sin ceros sobrantes). */
    private static String cantidad(double v) {
        return String.format(Locale.US, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
