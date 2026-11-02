package com.negocio.comida.reporte;

import com.negocio.comida.modelo.Cliente;
import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Despacho;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Datos de un reporte de ventas (platos despachados) para entregar al
 * contador del cliente. Es independiente del formato de salida.
 */
public class ReporteVentas {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Una línea del detalle. */
    public static final class Linea {
        public final LocalDate fecha;
        public final String menu;
        public final int platos;
        public final double precio;
        public final double total;

        Linea(Despacho d) {
            this.fecha = d.getFecha();
            this.menu = d.getMenuNombre();
            this.platos = d.getPlatos();
            this.precio = d.getPrecioUnitario();
            this.total = d.getTotal();
        }
    }

    /** Ventas de un cliente en el periodo. */
    public static final class Grupo {
        public final Cliente cliente;
        public final List<Linea> lineas = new ArrayList<>();

        Grupo(Cliente cliente) {
            this.cliente = cliente;
        }

        public int platos() {
            return lineas.stream().mapToInt(l -> l.platos).sum();
        }

        public double total() {
            return lineas.stream().mapToDouble(l -> l.total).sum();
        }
    }

    private final String negocio;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final boolean todosLosClientes;
    private final LocalDate emision = LocalDate.now();
    private final List<Grupo> grupos;

    public ReporteVentas(DatosNegocio datos, Cliente cliente, LocalDate desde, LocalDate hasta) {
        this.negocio = datos.getNombreNegocio();
        this.desde = desde;
        this.hasta = hasta;
        this.todosLosClientes = cliente == null;
        Map<Long, Grupo> porCliente = new LinkedHashMap<>();
        if (cliente != null) porCliente.put(cliente.getId(), new Grupo(cliente));
        for (Despacho d : datos.despachosEntre(desde, hasta, cliente)) {
            Grupo g = porCliente.computeIfAbsent(d.getClienteId(),
                    id -> new Grupo(datos.buscarCliente(id).orElseThrow()));
            g.lineas.add(new Linea(d));
        }
        this.grupos = Collections.unmodifiableList(new ArrayList<>(porCliente.values()));
    }

    public String getNegocio() { return negocio; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public LocalDate getEmision() { return emision; }
    public boolean isTodosLosClientes() { return todosLosClientes; }
    public List<Grupo> getGrupos() { return grupos; }

    public int totalPlatos() {
        return grupos.stream().mapToInt(Grupo::platos).sum();
    }

    public double totalVentas() {
        return grupos.stream().mapToDouble(Grupo::total).sum();
    }

    public int totalLineas() {
        return grupos.stream().mapToInt(g -> g.lineas.size()).sum();
    }

    public String titulo() {
        return "Reporte de ventas de almuerzos";
    }

    public String descripcionClientes() {
        if (!todosLosClientes && !grupos.isEmpty()) return grupos.get(0).cliente.getNombre();
        return "Todos los clientes";
    }

    public String periodo() {
        return "Del " + desde.format(FECHA) + " al " + hasta.format(FECHA);
    }

    /** Nombre de archivo sugerido, sin extensión. */
    public String nombreArchivo() {
        String quien = todosLosClientes ? "todos" : descripcionClientes();
        String limpio = java.text.Normalizer.normalize(quien, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "");
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "Reporte_ventas_" + limpio + "_" + desde.format(f) + "_a_" + hasta.format(f);
    }

    public static String dinero(double v) {
        return String.format(Locale.US, "$ %,.2f", v);
    }
}
