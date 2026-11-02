package com.negocio.comida.reporte;

import com.negocio.comida.reporte.ReporteVentas.Grupo;
import com.negocio.comida.reporte.ReporteVentas.Linea;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Genera el reporte de ventas en PDF (A4) sin librerías externas, usando las
 * fuentes estándar Helvetica y Helvetica-Bold que todo lector de PDF incluye.
 */
public final class ExportadorPdf {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final float ANCHO = 595.28f;
    private static final float ALTO = 841.89f;
    private static final float MARGEN = 42f;
    private static final float LIMITE_INFERIOR = MARGEN + 36f;
    private static final float FILA = 17f;
    private static final float CUERPO = 9.5f;

    // Columnas del detalle
    private static final float X_FECHA = MARGEN + 6;
    private static final float X_MENU = MARGEN + 78;
    private static final float DER_TOTAL = ANCHO - MARGEN - 6;
    private static final float DER_PRECIO = DER_TOTAL - 92;
    private static final float DER_PLATOS = DER_PRECIO - 92;
    private static final float ANCHO_MENU = DER_PLATOS - 52 - X_MENU;

    private final ReporteVentas r;
    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder cs;
    private float y;

    private ExportadorPdf(ReporteVentas r) {
        this.r = r;
    }

    public static void exportar(ReporteVentas reporte, Path archivo) throws IOException {
        ExportadorPdf e = new ExportadorPdf(reporte);
        e.componer();
        Files.write(archivo, e.escribirPdf());
    }

    // ------------------------------------------------------------ composición

    private void componer() {
        nuevaPagina();
        encabezado();
        for (Grupo g : r.getGrupos()) {
            grupo(g);
        }
        if (r.getGrupos().isEmpty()) {
            texto(MARGEN, y, false, 11, "No hay ventas registradas en el periodo.");
            y -= 24;
        }
        if (r.isTodosLosClientes() && r.getGrupos().size() > 1) resumen();
        totalFinal();
        firmas();
        pies();
    }

    private void nuevaPagina() {
        cs = new StringBuilder();
        paginas.add(cs);
        y = ALTO - MARGEN;
    }

    /** Pasa a otra página si no caben {@code alto} puntos más. */
    private boolean asegurar(float alto) {
        if (y - alto < LIMITE_INFERIOR) {
            nuevaPagina();
            return true;
        }
        return false;
    }

    private void encabezado() {
        String negocio = r.getNegocio();
        if (!negocio.isEmpty()) {
            texto(MARGEN, y - 14, true, 17, negocio);
            y -= 24;
        }
        texto(MARGEN, y - 12, true, 13, r.titulo());
        y -= 22;
        rect(MARGEN, y, ANCHO - 2 * MARGEN, 1.2f, "0.20 0.33 0.55");
        y -= 18;
        dato("Cliente:", r.descripcionClientes());
        if (!r.isTodosLosClientes() && !r.getGrupos().isEmpty()) {
            String tel = r.getGrupos().get(0).cliente.getTelefono();
            String dir = r.getGrupos().get(0).cliente.getDireccion();
            if (tel != null && !tel.isBlank()) dato("Teléfono:", tel);
            if (dir != null && !dir.isBlank()) dato("Dirección:", dir);
        }
        dato("Periodo:", r.periodo());
        dato("Fecha de emisión:", r.getEmision().format(ReporteVentas.FECHA));
        y -= 10;
    }

    private void dato(String etiqueta, String valor) {
        texto(MARGEN, y, true, 10, etiqueta);
        texto(MARGEN + 105, y, false, 10, recortar(valor, false, 10, ANCHO - 2 * MARGEN - 105));
        y -= 15;
    }

    private void grupo(Grupo g) {
        asegurar(FILA * 4 + 24);
        if (r.isTodosLosClientes()) tituloCliente(g, false);
        cabeceraTabla();
        if (g.lineas.isEmpty()) {
            texto(X_FECHA, y - 12, false, CUERPO, "Sin despachos en el periodo.");
            y -= FILA;
        }
        boolean alterna = false;
        for (Linea l : g.lineas) {
            if (asegurar(FILA)) {
                if (r.isTodosLosClientes()) tituloCliente(g, true);
                cabeceraTabla();
            }
            if (alterna) rect(MARGEN, y - FILA, ANCHO - 2 * MARGEN, FILA, "0.96 0.97 0.99");
            alterna = !alterna;
            float base = y - 12;
            texto(X_FECHA, base, false, CUERPO, l.fecha.format(ReporteVentas.FECHA));
            texto(X_MENU, base, false, CUERPO, recortar(l.menu, false, CUERPO, ANCHO_MENU));
            textoDer(DER_PLATOS, base, false, CUERPO, String.valueOf(l.platos));
            textoDer(DER_PRECIO, base, false, CUERPO, ReporteVentas.dinero(l.precio));
            textoDer(DER_TOTAL, base, false, CUERPO, ReporteVentas.dinero(l.total));
            y -= FILA;
        }
        asegurar(FILA + 4);
        linea(MARGEN, y, ANCHO - MARGEN, y, 0.8f);
        float base = y - 12;
        String etiqueta = r.isTodosLosClientes() ? "Subtotal " + g.cliente.getNombre() : "Subtotal";
        texto(X_MENU, base, true, CUERPO, recortar(etiqueta, true, CUERPO, ANCHO_MENU));
        textoDer(DER_PLATOS, base, true, CUERPO, String.valueOf(g.platos()));
        textoDer(DER_TOTAL, base, true, CUERPO, ReporteVentas.dinero(g.total()));
        y -= FILA + 16;
    }

    private void tituloCliente(Grupo g, boolean continuacion) {
        String t = g.cliente.getNombre() + (continuacion ? " (continuación)" : "");
        texto(MARGEN, y - 12, true, 11.5f, recortar(t, true, 11.5f, 330));
        String tel = g.cliente.getTelefono();
        if (tel != null && !tel.isBlank()) textoDer(ANCHO - MARGEN, y - 12, false, 9, "Tel.: " + tel);
        y -= 20;
    }

    private void cabeceraTabla() {
        rect(MARGEN, y - FILA, ANCHO - 2 * MARGEN, FILA, "0.85 0.89 0.95");
        float base = y - 12;
        texto(X_FECHA, base, true, CUERPO, "Fecha");
        texto(X_MENU, base, true, CUERPO, "Menú");
        textoDer(DER_PLATOS, base, true, CUERPO, "Platos");
        textoDer(DER_PRECIO, base, true, CUERPO, "Precio unit.");
        textoDer(DER_TOTAL, base, true, CUERPO, "Total");
        y -= FILA;
    }

    private void resumen() {
        asegurar(FILA * 3 + 30);
        texto(MARGEN, y - 12, true, 12, "Resumen por cliente");
        y -= 22;
        float derDias = DER_PLATOS - 70;
        rect(MARGEN, y - FILA, ANCHO - 2 * MARGEN, FILA, "0.85 0.89 0.95");
        texto(X_FECHA, y - 12, true, CUERPO, "Cliente");
        textoDer(derDias, y - 12, true, CUERPO, "Días");
        textoDer(DER_PLATOS, y - 12, true, CUERPO, "Platos");
        textoDer(DER_TOTAL, y - 12, true, CUERPO, "Total");
        y -= FILA;
        for (Grupo g : r.getGrupos()) {
            asegurar(FILA);
            long dias = g.lineas.stream().map(l -> l.fecha).distinct().count();
            texto(X_FECHA, y - 12, false, CUERPO, recortar(g.cliente.getNombre(), false, CUERPO, derDias - 40 - X_FECHA));
            textoDer(derDias, y - 12, false, CUERPO, String.valueOf(dias));
            textoDer(DER_PLATOS, y - 12, false, CUERPO, String.valueOf(g.platos()));
            textoDer(DER_TOTAL, y - 12, false, CUERPO, ReporteVentas.dinero(g.total()));
            y -= FILA;
        }
        y -= 10;
    }

    private void totalFinal() {
        asegurar(64 + 80); // el total y las firmas van juntos en la misma página
        float alto = 44;
        float x = ANCHO - MARGEN - 250;
        rect(x, y - alto, 250, alto, "0.20 0.33 0.55");
        cs.append("1 g\n");
        texto(x + 12, y - 17, false, 9.5f, "Total de platos: " + r.totalPlatos());
        texto(x + 12, y - 35, true, 13, "TOTAL A PAGAR");
        textoDer(ANCHO - MARGEN - 12, y - 35, true, 13, ReporteVentas.dinero(r.totalVentas()));
        cs.append("0 g\n");
        y -= alto + 20;
    }

    private void firmas() {
        y -= 40;
        float ancho = 200;
        float x2 = ANCHO - MARGEN - ancho;
        linea(MARGEN, y, MARGEN + ancho, y, 0.7f);
        linea(x2, y, x2 + ancho, y, 0.7f);
        texto(MARGEN, y - 13, false, 9, "Entregado por");
        texto(x2, y - 13, false, 9, "Recibido / revisado (contabilidad)");
        y -= 30;
    }

    private void pies() {
        int n = paginas.size();
        for (int i = 0; i < n; i++) {
            cs = paginas.get(i);
            linea(MARGEN, MARGEN + 14, ANCHO - MARGEN, MARGEN + 14, 0.4f);
            String izq = (r.getNegocio().isEmpty() ? "" : r.getNegocio() + " - ") + r.titulo() + " - " + r.periodo();
            texto(MARGEN, MARGEN, false, 8, recortar(izq, false, 8, ANCHO - 2 * MARGEN - 80));
            textoDer(ANCHO - MARGEN, MARGEN, false, 8, "Página " + (i + 1) + " de " + n);
        }
    }

    // ------------------------------------------------------------ primitivas

    private void texto(float x, float yy, boolean negrita, float tam, String s) {
        cs.append("BT /").append(negrita ? "F2 " : "F1 ").append(num(tam)).append(" Tf ")
                .append(num(x)).append(' ').append(num(yy)).append(" Td (")
                .append(escapar(s)).append(") Tj ET\n");
    }

    private void textoDer(float xDer, float yy, boolean negrita, float tam, String s) {
        texto(xDer - ancho(s, negrita, tam), yy, negrita, tam, s);
    }

    private void rect(float x, float yy, float w, float h, String rgb) {
        cs.append("q ").append(rgb).append(" rg ").append(num(x)).append(' ').append(num(yy)).append(' ')
                .append(num(w)).append(' ').append(num(h)).append(" re f Q\n");
    }

    private void linea(float x1, float y1, float x2, float y2, float grosor) {
        cs.append("q 0.45 G ").append(num(grosor)).append(" w ").append(num(x1)).append(' ').append(num(y1))
                .append(" m ").append(num(x2)).append(' ').append(num(y2)).append(" l S Q\n");
    }

    private static String num(float v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\r", " ").replace("\n", " ");
    }

    private static String recortar(String s, boolean negrita, float tam, float max) {
        if (s == null) return "";
        if (ancho(s, negrita, tam) <= max) return s;
        String t = s;
        while (!t.isEmpty() && ancho(t + "...", negrita, tam) > max) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    // Anchos (en milésimas de em) de Helvetica y Helvetica-Bold, caracteres 32..126
    private static final short[] HELV = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};
    private static final short[] HELV_B = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
        975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
        333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
        611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};

    static float ancho(String s, boolean negrita, float tam) {
        short[] t = negrita ? HELV_B : HELV;
        int total = 0;
        for (char c : s.toCharArray()) {
            char base = c;
            if (c > 126) {
                String n = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                base = n.isEmpty() ? c : n.charAt(0); // á -> a, Ñ -> N
            }
            total += (base >= 32 && base <= 126) ? t[base - 32] : 556;
        }
        return total * tam / 1000f;
    }

    // ------------------------------------------------------------ archivo PDF

    private byte[] escribirPdf() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int nPag = paginas.size();
        // Objetos: 1 catálogo, 2 páginas, 3 Helvetica, 4 Helvetica-Bold, 5 info,
        // luego por cada página: página y contenido.
        int total = 5 + 2 * nPag;

        escribir(out, "%PDF-1.4\n");
        out.write(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        objeto(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < nPag; i++) kids.append(6 + 2 * i).append(" 0 R ");
        objeto(out, offsets, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + nPag + " >>");
        objeto(out, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        objeto(out, offsets, 5, "<< /Title (" + escapar(r.titulo() + " - " + r.descripcionClientes())
                + ") /Producer (Negocio de Comida) >>");

        for (int i = 0; i < nPag; i++) {
            int idPagina = 6 + 2 * i;
            int idContenido = idPagina + 1;
            objeto(out, offsets, idPagina, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + num(ANCHO) + " "
                    + num(ALTO) + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                    + idContenido + " 0 R >>");
            byte[] datos = comprimir(paginas.get(i).toString().getBytes(WIN_ANSI));
            offsets.add(out.size());
            escribir(out, idContenido + " 0 obj\n<< /Length " + datos.length + " /Filter /FlateDecode >>\nstream\n");
            out.write(datos);
            escribir(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        escribir(out, "xref\n0 " + (total + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) escribir(out, String.format("%010d 00000 n \n", off));
        escribir(out, "trailer\n<< /Size " + (total + 1) + " /Root 1 0 R /Info 5 0 R >>\nstartxref\n"
                + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void objeto(ByteArrayOutputStream out, List<Integer> offsets, int id, String cuerpo)
            throws IOException {
        offsets.add(out.size());
        out.write((id + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
        out.write(cuerpo.getBytes(StandardCharsets.ISO_8859_1));
        out.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
    }

    private static void escribir(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] comprimir(byte[] datos) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (DeflaterOutputStream d = new DeflaterOutputStream(b, new Deflater(Deflater.BEST_COMPRESSION))) {
            d.write(datos);
        }
        return b.toByteArray();
    }
}
