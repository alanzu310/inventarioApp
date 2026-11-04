package com.negocio.comida.modelo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Contiene todos los datos del negocio y la lógica que los relaciona
 * (compras/proveedores ↔ inventario ↔ menús ↔ despachos ↔ clientes). Cada cambio notifica a los
 * oyentes para que todas las pestañas se refresquen y los datos se guarden.
 */
public class DatosNegocio implements Serializable {
    private static final long serialVersionUID = 1L;

    private long secuencia = 1;
    /** Nombre del negocio que aparece en los reportes (null en datos antiguos). */
    private String nombreNegocio;
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    private final List<Menu> menus = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Gasto> gastos = new ArrayList<>();
    private final List<Despacho> despachos = new ArrayList<>();
    private final Map<LocalDate, List<Long>> menuDelDia = new HashMap<>();
    // No son final: en datos de versiones anteriores llegan como null y se crean al cargar.
    private List<Proveedor> proveedores = new ArrayList<>();
    private List<Lote> lotes = new ArrayList<>();

    /** Margen para comparar cantidades decimales. */
    private static final double EPS = 1e-9;

    private transient List<Runnable> oyentes;

    /**
     * Convierte datos de versiones anteriores: gastos "Comida" → "Otros", y la
     * existencia/precio que tenía cada ingrediente → una compra "Sin proveedor".
     */
    @SuppressWarnings("deprecation")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (proveedores == null) proveedores = new ArrayList<>();
        if (lotes == null) {
            lotes = new ArrayList<>();
            for (Ingrediente i : ingredientes) {
                lotes.add(new Lote(siguienteId(), i.getId(), 0, LocalDate.now(),
                        Math.max(0, i.cantidadAnterior()), i.precioAnterior()));
                i.limpiarDatosAnteriores();
            }
        }
        gastos.replaceAll(g -> g.getTipo() == TipoGasto.COMIDA
                ? new Gasto(g.getId(), g.getFecha(), TipoGasto.OTROS, g.getDescripcion(), g.getMonto(),
                        g.getDespachoId())
                : g);
    }

    // ------------------------------------------------------------ oyentes

    public void agregarOyente(Runnable oyente) {
        if (oyentes == null) oyentes = new ArrayList<>();
        oyentes.add(oyente);
    }

    private void notificar() {
        if (oyentes == null) return;
        for (Runnable r : new ArrayList<>(oyentes)) r.run();
    }

    private long siguienteId() {
        return secuencia++;
    }

    public String getNombreNegocio() {
        return nombreNegocio == null ? "" : nombreNegocio;
    }

    public void setNombreNegocio(String nombreNegocio) {
        String n = nombreNegocio == null ? "" : nombreNegocio.trim();
        if (!n.equals(getNombreNegocio())) {
            this.nombreNegocio = n;
            notificar();
        }
    }

    // ------------------------------------------------------------ inventario

    public List<Ingrediente> getIngredientes() {
        return Collections.unmodifiableList(ingredientes);
    }

    public Optional<Ingrediente> buscarIngrediente(long id) {
        return ingredientes.stream().filter(i -> i.getId() == id).findFirst();
    }

    public Ingrediente agregarIngrediente(String nombre, Unidad unidad) {
        Ingrediente i = new Ingrediente(siguienteId(), nombre, unidad);
        ingredientes.add(i);
        notificar();
        return i;
    }

    public void actualizarIngrediente(Ingrediente i, String nombre, Unidad unidad) {
        i.setNombre(nombre);
        i.setUnidad(unidad);
        notificar();
    }

    /** Menús que usan el ingrediente (para impedir borrarlo). */
    public List<Menu> menusQueUsan(Ingrediente ing) {
        return menus.stream()
                .filter(m -> m.getItems().stream().anyMatch(it -> it.getIngredienteId() == ing.getId()))
                .collect(Collectors.toList());
    }

    /** Elimina el ingrediente junto con su historial de compras. */
    public void eliminarIngrediente(Ingrediente i) {
        ingredientes.remove(i);
        lotes.removeIf(l -> l.getIngredienteId() == i.getId());
        notificar();
    }

    /** Existencia total: lo que queda de todas sus compras. */
    public double existencia(Ingrediente i) {
        return lotes.stream().filter(l -> l.getIngredienteId() == i.getId())
                .mapToDouble(Lote::getCantidadRestante).sum();
    }

    /** Valor de la existencia, cada compra a su propio precio. */
    public double valorExistencia(Ingrediente i) {
        return lotes.stream().filter(l -> l.getIngredienteId() == i.getId())
                .mapToDouble(Lote::getValorRestante).sum();
    }

    /** Costo promedio por unidad de lo que hay en existencia (0 si no hay). */
    public double costoPromedio(Ingrediente i) {
        double e = existencia(i);
        return e > EPS ? valorExistencia(i) / e : 0;
    }

    /** Precio de la compra más reciente (0 si nunca se compró). */
    public double ultimoPrecio(Ingrediente i) {
        return compras(i).stream().findFirst().map(Lote::getPrecioPorUnidad).orElse(0.0);
    }

    /**
     * Costo de usar {@code cantidad} del ingrediente ahora mismo: se toma de las
     * compras más antiguas primero; si no alcanza, lo que falta se valora al
     * precio de la última compra.
     */
    public double costoDe(Ingrediente i, double cantidad) {
        double falta = cantidad;
        double costo = 0;
        for (Lote l : lotesDisponibles(i.getId())) {
            if (falta <= EPS) break;
            double usa = Math.min(falta, l.getCantidadRestante());
            costo += usa * l.getPrecioPorUnidad();
            falta -= usa;
        }
        if (falta > EPS) costo += falta * ultimoPrecio(i);
        return costo;
    }

    /** Compras con existencia, de la más antigua a la más reciente (orden PEPS). */
    private List<Lote> lotesDisponibles(long ingredienteId) {
        return lotes.stream()
                .filter(l -> l.getIngredienteId() == ingredienteId && l.getCantidadRestante() > EPS)
                .sorted(Comparator.comparing(Lote::getFecha).thenComparingLong(Lote::getId))
                .collect(Collectors.toList());
    }

    private Optional<Lote> buscarLote(long id) {
        return lotes.stream().filter(l -> l.getId() == id).findFirst();
    }

    // ------------------------------------------------------------ compras

    /** Compras de un ingrediente, de la más reciente a la más antigua. */
    public List<Lote> compras(Ingrediente i) {
        return lotes.stream().filter(l -> l.getIngredienteId() == i.getId())
                .sorted(Comparator.comparing(Lote::getFecha).thenComparingLong(Lote::getId).reversed())
                .collect(Collectors.toList());
    }

    /** Compras hechas a un proveedor, de la más reciente a la más antigua. */
    public List<Lote> compras(Proveedor p) {
        return lotes.stream().filter(l -> l.getProveedorId() == p.getId())
                .sorted(Comparator.comparing(Lote::getFecha).thenComparingLong(Lote::getId).reversed())
                .collect(Collectors.toList());
    }

    public Lote registrarCompra(Ingrediente i, Proveedor p, LocalDate fecha, double cantidad, double precioPorUnidad) {
        Lote l = new Lote(siguienteId(), i.getId(), p == null ? 0 : p.getId(), fecha, cantidad, precioPorUnidad);
        lotes.add(l);
        notificar();
        return l;
    }

    /**
     * Corrige una compra. Lo que ya se usó de ella se respeta: la cantidad
     * comprada no puede quedar por debajo de lo usado.
     */
    public void editarCompra(Lote l, Proveedor p, LocalDate fecha, double cantidad, double precioPorUnidad) {
        double usada = l.getCantidadUsada();
        if (cantidad + EPS < usada) {
            throw new IllegalArgumentException("Ya se usaron " + usada + " de esta compra.");
        }
        l.setProveedorId(p == null ? 0 : p.getId());
        l.setFecha(fecha);
        l.setCantidadComprada(cantidad);
        l.setPrecioPorUnidad(precioPorUnidad);
        l.setCantidadRestante(Math.max(0, cantidad - usada));
        notificar();
    }

    /** Ajusta lo que queda físicamente de una compra (merma, conteo, etc.). */
    public void ajustarRestante(Lote l, double restante) {
        if (restante < 0 || restante > l.getCantidadComprada() + EPS) {
            throw new IllegalArgumentException("Debe estar entre 0 y la cantidad comprada.");
        }
        l.setCantidadRestante(Math.min(restante, l.getCantidadComprada()));
        notificar();
    }

    /** Solo se pueden borrar compras de las que no se ha usado nada. */
    public boolean puedeEliminarCompra(Lote l) {
        return l.getCantidadUsada() <= EPS;
    }

    public void eliminarCompra(Lote l) {
        if (!puedeEliminarCompra(l)) {
            throw new IllegalArgumentException("Ya se usó parte de esta compra.");
        }
        lotes.remove(l);
        notificar();
    }

    /** Resumen de precios de un ingrediente por proveedor (el más barato primero). */
    public List<PrecioProveedor> preciosPorProveedor(Ingrediente i) {
        Map<Long, PrecioProveedor> res = new LinkedHashMap<>();
        for (Lote l : compras(i)) { // de la más reciente a la más antigua
            PrecioProveedor pp = res.computeIfAbsent(l.getProveedorId(),
                    id -> new PrecioProveedor(nombreProveedor(id), l.getPrecioPorUnidad(), l.getFecha()));
            pp.agregar(l);
        }
        List<PrecioProveedor> lista = new ArrayList<>(res.values());
        lista.sort(Comparator.comparingDouble(PrecioProveedor::getUltimoPrecio));
        return lista;
    }

    /** Precios de un proveedor para un ingrediente. */
    public static final class PrecioProveedor {
        private final String proveedor;
        private final double ultimoPrecio;
        private final LocalDate ultimaCompra;
        private double precioMinimo = Double.MAX_VALUE;
        private double cantidadTotal;
        private int compras;

        PrecioProveedor(String proveedor, double ultimoPrecio, LocalDate ultimaCompra) {
            this.proveedor = proveedor;
            this.ultimoPrecio = ultimoPrecio;
            this.ultimaCompra = ultimaCompra;
        }

        void agregar(Lote l) {
            precioMinimo = Math.min(precioMinimo, l.getPrecioPorUnidad());
            cantidadTotal += l.getCantidadComprada();
            compras++;
        }

        public String getProveedor() { return proveedor; }
        public double getUltimoPrecio() { return ultimoPrecio; }
        public LocalDate getUltimaCompra() { return ultimaCompra; }
        public double getPrecioMinimo() { return precioMinimo; }
        public double getCantidadTotal() { return cantidadTotal; }
        public int getCompras() { return compras; }
    }

    // ------------------------------------------------------------ proveedores

    public List<Proveedor> getProveedores() {
        return Collections.unmodifiableList(proveedores);
    }

    public Optional<Proveedor> buscarProveedor(long id) {
        return proveedores.stream().filter(p -> p.getId() == id).findFirst();
    }

    public String nombreProveedor(long id) {
        if (id == 0) return "Sin proveedor";
        return buscarProveedor(id).map(Proveedor::getNombre).orElse("(eliminado)");
    }

    public Proveedor agregarProveedor(String nombre, String telefono) {
        Proveedor p = new Proveedor(siguienteId(), nombre, telefono);
        proveedores.add(p);
        notificar();
        return p;
    }

    public void actualizarProveedor(Proveedor p, String nombre, String telefono) {
        p.setNombre(nombre);
        p.setTelefono(telefono);
        notificar();
    }

    /** Un proveedor con compras registradas no se puede borrar (se perdería el historial). */
    public boolean tieneCompras(Proveedor p) {
        return lotes.stream().anyMatch(l -> l.getProveedorId() == p.getId());
    }

    public void eliminarProveedor(Proveedor p) {
        if (tieneCompras(p)) throw new IllegalArgumentException("El proveedor tiene compras registradas.");
        proveedores.remove(p);
        notificar();
    }

    public double totalComprado(Proveedor p) {
        return lotes.stream().filter(l -> l.getProveedorId() == p.getId()).mapToDouble(Lote::getTotalCompra).sum();
    }

    // ------------------------------------------------------------ menús

    public List<Menu> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    public Optional<Menu> buscarMenu(long id) {
        return menus.stream().filter(m -> m.getId() == id).findFirst();
    }

    public void guardarMenu(Menu existente, String nombre, double precio, List<ItemMenu> items) {
        Menu m = existente;
        if (m == null) {
            m = new Menu(siguienteId(), nombre, precio);
            menus.add(m);
        }
        m.setNombre(nombre);
        m.setPrecioVenta(precio);
        m.getItems().clear();
        m.getItems().addAll(items);
        notificar();
    }

    public void eliminarMenu(Menu m) {
        menus.remove(m);
        for (List<Long> ids : menuDelDia.values()) ids.remove(m.getId());
        notificar();
    }

    /** Costo de ingredientes del próximo plato (según las compras en existencia, PEPS). */
    public double costoPlato(Menu m) {
        double total = 0;
        for (ItemMenu it : m.getItems()) {
            total += buscarIngrediente(it.getIngredienteId())
                    .map(i -> costoDe(i, it.getCantidad())).orElse(0.0);
        }
        return total;
    }

    /** Máximo de platos que se pueden preparar con el inventario actual. */
    public int platosDisponibles(Menu m) {
        if (m.getItems().isEmpty()) return 0;
        int max = Integer.MAX_VALUE;
        for (ItemMenu it : m.getItems()) {
            Optional<Ingrediente> ing = buscarIngrediente(it.getIngredienteId());
            if (ing.isEmpty() || it.getCantidad() <= 0) continue;
            max = Math.min(max, (int) Math.floor(existencia(ing.get()) / it.getCantidad() + 1e-9));
        }
        return max == Integer.MAX_VALUE ? 0 : Math.max(max, 0);
    }

    /** Ingredientes que faltan para preparar {@code platos} platos del menú. */
    public List<String> faltantes(Menu m, int platos) {
        List<String> faltan = new ArrayList<>();
        for (ItemMenu it : m.getItems()) {
            buscarIngrediente(it.getIngredienteId()).ifPresent(i -> {
                double necesario = it.getCantidad() * platos;
                double hay = existencia(i);
                if (hay + EPS < necesario) {
                    faltan.add(String.format("%s: necesita %.2f %s, hay %.2f",
                            i.getNombre(), necesario, i.getUnidad().getAbreviatura(), hay));
                }
            });
        }
        return faltan;
    }

    // ------------------------------------------------------------ menú del día

    public List<Menu> getMenuDelDia(LocalDate fecha) {
        List<Menu> lista = new ArrayList<>();
        for (Long id : menuDelDia.getOrDefault(fecha, List.of())) buscarMenu(id).ifPresent(lista::add);
        return lista;
    }

    public void agregarAlMenuDelDia(LocalDate fecha, Menu m) {
        List<Long> ids = menuDelDia.computeIfAbsent(fecha, f -> new ArrayList<>());
        if (!ids.contains(m.getId())) {
            ids.add(m.getId());
            notificar();
        }
    }

    public void quitarDelMenuDelDia(LocalDate fecha, Menu m) {
        List<Long> ids = menuDelDia.get(fecha);
        if (ids != null && ids.remove(m.getId())) notificar();
    }

    /** Lunes de la semana a la que pertenece la fecha. */
    public static LocalDate inicioSemana(LocalDate fecha) {
        return fecha.with(DayOfWeek.MONDAY);
    }

    /**
     * Copia la planificación de los 7 días anteriores a la semana que empieza en
     * {@code lunes}. Los menús ya planificados se conservan (no se duplican).
     */
    public void copiarSemanaAnterior(LocalDate lunes) {
        for (int i = 0; i < 7; i++) {
            LocalDate destino = lunes.plusDays(i);
            List<Long> origen = menuDelDia.getOrDefault(destino.minusWeeks(1), List.of());
            List<Long> ids = menuDelDia.computeIfAbsent(destino, f -> new ArrayList<>());
            for (Long id : origen) {
                if (!ids.contains(id) && buscarMenu(id).isPresent()) ids.add(id);
            }
        }
        notificar();
    }

    // ------------------------------------------------------------ gastos

    public List<Gasto> getGastos(LocalDate fecha) {
        return gastos.stream().filter(g -> g.getFecha().equals(fecha)).collect(Collectors.toList());
    }

    public void agregarGasto(LocalDate fecha, TipoGasto tipo, String descripcion, double monto) {
        gastos.add(new Gasto(siguienteId(), fecha, tipo, descripcion, monto));
        notificar();
    }

    /** Elimina un gasto manual. Los de costo de ingredientes se quitan anulando el despacho. */
    public void eliminarGasto(Gasto g) {
        if (g.esAutomatico()) {
            throw new IllegalArgumentException("El gasto pertenece a un despacho; anule el despacho.");
        }
        gastos.remove(g);
        notificar();
    }

    public double totalGastos(LocalDate fecha, TipoGasto tipo) {
        return getGastos(fecha).stream()
                .filter(g -> tipo == null || g.getTipo() == tipo)
                .mapToDouble(Gasto::getMonto).sum();
    }

    // ------------------------------------------------------------ clientes

    public List<Cliente> getClientes() {
        return Collections.unmodifiableList(clientes);
    }

    public Optional<Cliente> buscarCliente(long id) {
        return clientes.stream().filter(c -> c.getId() == id).findFirst();
    }

    public Cliente agregarCliente(String nombre, String telefono, String direccion) {
        Cliente c = new Cliente(siguienteId(), nombre, telefono, direccion);
        clientes.add(c);
        notificar();
        return c;
    }

    public void actualizarCliente(Cliente c, String nombre, String telefono, String direccion) {
        c.setNombre(nombre);
        c.setTelefono(telefono);
        c.setDireccion(direccion);
        notificar();
    }

    public void eliminarCliente(Cliente c) {
        clientes.remove(c);
        for (Despacho d : getDespachosCliente(c)) {
            gastos.removeIf(g -> g.getDespachoId() == d.getId());
        }
        despachos.removeIf(d -> d.getClienteId() == c.getId());
        notificar();
    }

    public String nombreCliente(long id) {
        return buscarCliente(id).map(Cliente::getNombre).orElse("(eliminado)");
    }

    // ------------------------------------------------------------ despachos

    /**
     * Registra platos entregados a un cliente. Los ingredientes se descuentan de
     * las compras más antiguas primero (PEPS) y su costo real se anota como
     * gasto del día. Si no alcanza la existencia, lo que falta se valora al
     * precio de la última compra (la existencia no queda negativa).
     */
    public void registrarDespacho(LocalDate fecha, Cliente c, Menu m, int platos) {
        List<Consumo> consumos = new ArrayList<>();
        double costo = 0;
        for (ItemMenu it : m.getItems()) {
            Optional<Ingrediente> ing = buscarIngrediente(it.getIngredienteId());
            if (ing.isEmpty()) continue;
            double falta = it.getCantidad() * platos;
            for (Lote l : lotesDisponibles(ing.get().getId())) {
                if (falta <= EPS) break;
                double usa = Math.min(falta, l.getCantidadRestante());
                l.setCantidadRestante(l.getCantidadRestante() - usa);
                consumos.add(new Consumo(l.getId(), usa, usa * l.getPrecioPorUnidad()));
                costo += usa * l.getPrecioPorUnidad();
                falta -= usa;
            }
            if (falta > EPS) costo += falta * ultimoPrecio(ing.get());
        }
        Despacho d = new Despacho(siguienteId(), fecha, c.getId(), m.getId(), m.getNombre(),
                platos, m.getPrecioVenta(), consumos);
        despachos.add(d);
        if (costo > 0) {
            gastos.add(new Gasto(siguienteId(), fecha, TipoGasto.INGREDIENTES,
                    platos + " x " + m.getNombre() + " - " + c.getNombre(), costo, d.getId()));
        }
        notificar();
    }

    /**
     * Elimina un despacho, devuelve los ingredientes a las mismas compras de las
     * que salieron y quita su gasto.
     */
    public void eliminarDespacho(Despacho d) {
        gastos.removeIf(g -> g.getDespachoId() == d.getId());
        if (d.getConsumos() != null) {
            for (Consumo co : d.getConsumos()) {
                buscarLote(co.getLoteId()).ifPresent(l -> l.setCantidadRestante(
                        Math.min(l.getCantidadComprada(), l.getCantidadRestante() + co.getCantidad())));
            }
        } else {
            // Despacho de una versión anterior: se devuelve a la compra más reciente.
            buscarMenu(d.getMenuId()).ifPresent(m -> {
                for (ItemMenu it : m.getItems()) {
                    buscarIngrediente(it.getIngredienteId())
                            .flatMap(i -> compras(i).stream().findFirst())
                            .ifPresent(l -> {
                                double nuevo = l.getCantidadRestante() + it.getCantidad() * d.getPlatos();
                                if (nuevo > l.getCantidadComprada()) l.setCantidadComprada(nuevo);
                                l.setCantidadRestante(nuevo);
                            });
                }
            });
        }
        despachos.remove(d);
        notificar();
    }

    public List<Despacho> getDespachos(LocalDate fecha) {
        return despachos.stream().filter(d -> d.getFecha().equals(fecha)).collect(Collectors.toList());
    }

    public List<Despacho> getDespachosCliente(Cliente c) {
        return despachos.stream().filter(d -> d.getClienteId() == c.getId()).collect(Collectors.toList());
    }

    /**
     * Despachos entre dos fechas (inclusive), de un cliente o de todos
     * ({@code cliente == null}), ordenados por cliente y fecha.
     */
    public List<Despacho> despachosEntre(LocalDate desde, LocalDate hasta, Cliente cliente) {
        List<Long> orden = clientes.stream().map(Cliente::getId).collect(Collectors.toList());
        return despachos.stream()
                .filter(d -> !d.getFecha().isBefore(desde) && !d.getFecha().isAfter(hasta))
                .filter(d -> cliente == null || d.getClienteId() == cliente.getId())
                .filter(d -> buscarCliente(d.getClienteId()).isPresent())
                .sorted(java.util.Comparator.comparingInt((Despacho d) -> orden.indexOf(d.getClienteId()))
                        .thenComparing(Despacho::getFecha)
                        .thenComparingLong(Despacho::getId))
                .collect(Collectors.toList());
    }

    public int platosDelDia(LocalDate fecha, Menu m) {
        return getDespachos(fecha).stream()
                .filter(d -> m == null || d.getMenuId() == m.getId())
                .mapToInt(Despacho::getPlatos).sum();
    }

    public double ventasDelDia(LocalDate fecha) {
        return getDespachos(fecha).stream().mapToDouble(Despacho::getTotal).sum();
    }

    /** Platos y monto por cliente en una fecha (orden de clientes registrados). */
    public Map<Cliente, double[]> resumenPorCliente(LocalDate fecha) {
        Map<Cliente, double[]> res = new LinkedHashMap<>();
        for (Despacho d : getDespachos(fecha)) {
            buscarCliente(d.getClienteId()).ifPresent(c -> {
                double[] v = res.computeIfAbsent(c, k -> new double[2]);
                v[0] += d.getPlatos();
                v[1] += d.getTotal();
            });
        }
        return res;
    }

    /** Reporte diario de un cliente: fecha → {platos, monto}, más reciente primero. */
    public Map<LocalDate, double[]> reporteDiarioCliente(Cliente c) {
        Map<LocalDate, double[]> res = new TreeMap<>(Collections.reverseOrder());
        for (Despacho d : getDespachosCliente(c)) {
            double[] v = res.computeIfAbsent(d.getFecha(), k -> new double[2]);
            v[0] += d.getPlatos();
            v[1] += d.getTotal();
        }
        return res;
    }

    public int totalPlatosCliente(Cliente c) {
        return getDespachosCliente(c).stream().mapToInt(Despacho::getPlatos).sum();
    }
}
