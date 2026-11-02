package com.negocio.comida.modelo;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Contiene todos los datos del negocio y la lógica que los relaciona
 * (inventario ↔ menús ↔ despachos ↔ clientes). Cada cambio notifica a los
 * oyentes para que todas las pestañas se refresquen y los datos se guarden.
 */
public class DatosNegocio implements Serializable {
    private static final long serialVersionUID = 1L;

    private long secuencia = 1;
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    private final List<Menu> menus = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Gasto> gastos = new ArrayList<>();
    private final List<Despacho> despachos = new ArrayList<>();
    private final Map<LocalDate, List<Long>> menuDelDia = new HashMap<>();

    private transient List<Runnable> oyentes;

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

    // ------------------------------------------------------------ inventario

    public List<Ingrediente> getIngredientes() {
        return Collections.unmodifiableList(ingredientes);
    }

    public Optional<Ingrediente> buscarIngrediente(long id) {
        return ingredientes.stream().filter(i -> i.getId() == id).findFirst();
    }

    public Ingrediente agregarIngrediente(String nombre, Unidad unidad, double cantidad, double precio) {
        Ingrediente i = new Ingrediente(siguienteId(), nombre, unidad, cantidad, precio);
        ingredientes.add(i);
        notificar();
        return i;
    }

    public void actualizarIngrediente(Ingrediente i, String nombre, Unidad unidad, double cantidad, double precio) {
        i.setNombre(nombre);
        i.setUnidad(unidad);
        i.setCantidad(cantidad);
        i.setPrecioPorUnidad(precio);
        notificar();
    }

    public void reabastecer(Ingrediente i, double cantidad) {
        i.setCantidad(i.getCantidad() + cantidad);
        notificar();
    }

    /** Menús que usan el ingrediente (para impedir borrarlo). */
    public List<Menu> menusQueUsan(Ingrediente ing) {
        return menus.stream()
                .filter(m -> m.getItems().stream().anyMatch(it -> it.getIngredienteId() == ing.getId()))
                .collect(Collectors.toList());
    }

    public void eliminarIngrediente(Ingrediente i) {
        ingredientes.remove(i);
        notificar();
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

    /** Costo de ingredientes de un plato. */
    public double costoPlato(Menu m) {
        double total = 0;
        for (ItemMenu it : m.getItems()) {
            total += buscarIngrediente(it.getIngredienteId())
                    .map(i -> i.getPrecioPorUnidad() * it.getCantidad()).orElse(0.0);
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
            max = Math.min(max, (int) Math.floor(ing.get().getCantidad() / it.getCantidad() + 1e-9));
        }
        return max == Integer.MAX_VALUE ? 0 : Math.max(max, 0);
    }

    /** Ingredientes que faltan para preparar {@code platos} platos del menú. */
    public List<String> faltantes(Menu m, int platos) {
        List<String> faltan = new ArrayList<>();
        for (ItemMenu it : m.getItems()) {
            buscarIngrediente(it.getIngredienteId()).ifPresent(i -> {
                double necesario = it.getCantidad() * platos;
                if (i.getCantidad() + 1e-9 < necesario) {
                    faltan.add(String.format("%s: necesita %.2f %s, hay %.2f",
                            i.getNombre(), necesario, i.getUnidad().getAbreviatura(), i.getCantidad()));
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

    // ------------------------------------------------------------ gastos

    public List<Gasto> getGastos(LocalDate fecha) {
        return gastos.stream().filter(g -> g.getFecha().equals(fecha)).collect(Collectors.toList());
    }

    public void agregarGasto(LocalDate fecha, TipoGasto tipo, String descripcion, double monto) {
        gastos.add(new Gasto(siguienteId(), fecha, tipo, descripcion, monto));
        notificar();
    }

    public void eliminarGasto(Gasto g) {
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
        despachos.removeIf(d -> d.getClienteId() == c.getId());
        notificar();
    }

    public String nombreCliente(long id) {
        return buscarCliente(id).map(Cliente::getNombre).orElse("(eliminado)");
    }

    // ------------------------------------------------------------ despachos

    /**
     * Registra platos entregados a un cliente y descuenta del inventario los
     * ingredientes usados (sin dejar existencias negativas).
     */
    public void registrarDespacho(LocalDate fecha, Cliente c, Menu m, int platos) {
        for (ItemMenu it : m.getItems()) {
            buscarIngrediente(it.getIngredienteId()).ifPresent(i ->
                    i.setCantidad(Math.max(0, i.getCantidad() - it.getCantidad() * platos)));
        }
        despachos.add(new Despacho(siguienteId(), fecha, c.getId(), m.getId(), m.getNombre(),
                platos, m.getPrecioVenta()));
        notificar();
    }

    /** Elimina un despacho y devuelve al inventario los ingredientes. */
    public void eliminarDespacho(Despacho d) {
        buscarMenu(d.getMenuId()).ifPresent(m -> {
            for (ItemMenu it : m.getItems()) {
                buscarIngrediente(it.getIngredienteId()).ifPresent(i ->
                        i.setCantidad(i.getCantidad() + it.getCantidad() * d.getPlatos()));
            }
        });
        despachos.remove(d);
        notificar();
    }

    public List<Despacho> getDespachos(LocalDate fecha) {
        return despachos.stream().filter(d -> d.getFecha().equals(fecha)).collect(Collectors.toList());
    }

    public List<Despacho> getDespachosCliente(Cliente c) {
        return despachos.stream().filter(d -> d.getClienteId() == c.getId()).collect(Collectors.toList());
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
