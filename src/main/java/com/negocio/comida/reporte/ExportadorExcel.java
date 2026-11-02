package com.negocio.comida.reporte;

import com.negocio.comida.reporte.ReporteVentas.Grupo;
import com.negocio.comida.reporte.ReporteVentas.Linea;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Genera el reporte de ventas como libro de Excel (.xlsx) sin librerías
 * externas. Hoja "Detalle" con cada despacho y hoja "Resumen" por cliente.
 * Los importes y fechas se guardan como números para que el contador pueda
 * sumar, filtrar y ordenar.
 */
public final class ExportadorExcel {

    // Índices de estilos definidos en styles.xml
    private static final int NEGRITA = 1;
    private static final int FECHA = 2;
    private static final int DINERO = 3;
    private static final int CABECERA = 5;
    private static final int TITULO = 6;
    private static final int TOTAL_ETIQUETA = 8;
    private static final int TOTAL_DINERO = 9;
    private static final int TOTAL_NUMERO = 10;
    private static final int CABECERA_DER = 11;

    private static final int FILA_CABECERA = 7;

    private ExportadorExcel() {
    }

    public static void exportar(ReporteVentas r, Path archivo) throws IOException {
        try (OutputStream os = Files.newOutputStream(archivo);
             ZipOutputStream zip = new ZipOutputStream(os)) {
            int ultimaDetalle = FILA_CABECERA + Math.max(1, r.totalLineas());
            entrada(zip, "[Content_Types].xml", contentTypes());
            entrada(zip, "_rels/.rels", RELS);
            entrada(zip, "xl/workbook.xml", workbook(ultimaDetalle));
            entrada(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            entrada(zip, "xl/styles.xml", STYLES);
            entrada(zip, "xl/worksheets/sheet1.xml", hojaDetalle(r));
            entrada(zip, "xl/worksheets/sheet2.xml", hojaResumen(r));
        }
    }

    // ------------------------------------------------------------ hojas

    private static String hojaDetalle(ReporteVentas r) {
        Hoja h = new Hoja();
        encabezado(h, r);
        int f = FILA_CABECERA;
        h.texto("A", f, "Cliente", CABECERA);
        h.texto("B", f, "Fecha", CABECERA);
        h.texto("C", f, "Menú", CABECERA);
        h.texto("D", f, "Platos", CABECERA_DER);
        h.texto("E", f, "Precio unitario", CABECERA_DER);
        h.texto("F", f, "Total", CABECERA_DER);
        h.finFila();
        for (Grupo g : r.getGrupos()) {
            for (Linea l : g.lineas) {
                f++;
                h.texto("A", f, g.cliente.getNombre(), 0);
                h.numero("B", f, serialFecha(l.fecha), FECHA);
                h.texto("C", f, l.menu, 0);
                h.numero("D", f, l.platos, 0);
                h.numero("E", f, l.precio, DINERO);
                h.formula("F", f, "D" + f + "*E" + f, l.total, DINERO);
                h.finFila();
            }
        }
        int primera = FILA_CABECERA + 1;
        int ultima = Math.max(f, primera);
        if (f == FILA_CABECERA) {
            f++;
            h.texto("A", f, "No hay ventas registradas en el periodo.", 0);
            h.finFila();
        }
        f += 2;
        h.texto("C", f, "TOTAL A PAGAR", TOTAL_ETIQUETA);
        h.formula("D", f, "SUM(D" + primera + ":D" + ultima + ")", r.totalPlatos(), TOTAL_NUMERO);
        h.texto("E", f, "", TOTAL_ETIQUETA);
        h.formula("F", f, "SUM(F" + primera + ":F" + ultima + ")", r.totalVentas(), TOTAL_DINERO);
        h.finFila();
        String filtro = "A" + FILA_CABECERA + ":F" + ultima;
        return h.xml(new double[]{30, 13, 34, 10, 16, 16}, FILA_CABECERA, filtro, true);
    }

    private static String hojaResumen(ReporteVentas r) {
        Hoja h = new Hoja();
        encabezado(h, r);
        int f = FILA_CABECERA;
        h.texto("A", f, "Cliente", CABECERA);
        h.texto("B", f, "Teléfono", CABECERA);
        h.texto("C", f, "Días con despacho", CABECERA_DER);
        h.texto("D", f, "Platos", CABECERA_DER);
        h.texto("E", f, "Total", CABECERA_DER);
        h.finFila();
        for (Grupo g : r.getGrupos()) {
            f++;
            h.texto("A", f, g.cliente.getNombre(), 0);
            h.texto("B", f, g.cliente.getTelefono() == null ? "" : g.cliente.getTelefono(), 0);
            h.numero("C", f, g.lineas.stream().map(l -> l.fecha).distinct().count(), 0);
            h.numero("D", f, g.platos(), 0);
            h.numero("E", f, g.total(), DINERO);
            h.finFila();
        }
        int primera = FILA_CABECERA + 1;
        int ultima = Math.max(f, primera);
        f += 2;
        h.texto("A", f, "TOTAL A PAGAR", TOTAL_ETIQUETA);
        h.texto("B", f, "", TOTAL_ETIQUETA);
        h.texto("C", f, "", TOTAL_ETIQUETA);
        h.formula("D", f, "SUM(D" + primera + ":D" + ultima + ")", r.totalPlatos(), TOTAL_NUMERO);
        h.formula("E", f, "SUM(E" + primera + ":E" + ultima + ")", r.totalVentas(), TOTAL_DINERO);
        h.finFila();
        return h.xml(new double[]{30, 16, 18, 10, 16}, FILA_CABECERA, null, false);
    }

    private static void encabezado(Hoja h, ReporteVentas r) {
        String negocio = r.getNegocio();
        h.texto("A", 1, negocio.isEmpty() ? r.titulo() : negocio, TITULO);
        h.finFila();
        if (!negocio.isEmpty()) {
            h.texto("A", 2, r.titulo(), NEGRITA);
            h.finFila();
        }
        h.texto("A", 3, "Cliente:", NEGRITA);
        h.texto("B", 3, r.descripcionClientes(), 0);
        h.finFila();
        h.texto("A", 4, "Periodo:", NEGRITA);
        h.texto("B", 4, r.periodo(), 0);
        h.finFila();
        h.texto("A", 5, "Fecha de emisión:", NEGRITA);
        h.numero("B", 5, serialFecha(r.getEmision()), FECHA);
        h.finFila();
    }

    /** Número de serie de fecha de Excel (días desde 30/12/1899). */
    private static long serialFecha(LocalDate d) {
        return ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), d);
    }

    // ------------------------------------------------------------ construcción de XML

    /** Acumula filas de una hoja; las celdas de cada fila se agregan en orden de columna. */
    private static final class Hoja {
        private final StringBuilder filas = new StringBuilder();
        private final StringBuilder fila = new StringBuilder();
        private int numFila;

        private void celda(String col, int f, String atributos, String contenido) {
            numFila = f;
            fila.append("<c r=\"").append(col).append(f).append('"').append(atributos).append('>')
                    .append(contenido).append("</c>");
        }

        void texto(String col, int f, String valor, int estilo) {
            celda(col, f, " t=\"inlineStr\"" + s(estilo), "<is><t xml:space=\"preserve\">" + esc(valor) + "</t></is>");
        }

        void numero(String col, int f, double valor, int estilo) {
            celda(col, f, s(estilo), "<v>" + num(valor) + "</v>");
        }

        void formula(String col, int f, String formula, double valorCalculado, int estilo) {
            celda(col, f, s(estilo), "<f>" + formula + "</f><v>" + num(valorCalculado) + "</v>");
        }

        void finFila() {
            filas.append("<row r=\"").append(numFila).append("\">").append(fila).append("</row>");
            fila.setLength(0);
        }

        String xml(double[] anchos, int filaFija, String filtro, boolean seleccionada) {
            StringBuilder x = new StringBuilder(XML_DECL);
            x.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                    .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
            x.append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
            x.append("<sheetViews><sheetView workbookViewId=\"0\"").append(seleccionada ? " tabSelected=\"1\"" : "")
                    .append("><pane ySplit=\"").append(filaFija).append("\" topLeftCell=\"A").append(filaFija + 1)
                    .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
            x.append("<cols>");
            for (int i = 0; i < anchos.length; i++) {
                x.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                        .append(num(anchos[i])).append("\" customWidth=\"1\"/>");
            }
            x.append("</cols><sheetData>").append(filas).append("</sheetData>");
            if (filtro != null) x.append("<autoFilter ref=\"").append(filtro).append("\"/>");
            x.append("<pageMargins left=\"0.6\" right=\"0.6\" top=\"0.7\" bottom=\"0.7\" header=\"0.3\" footer=\"0.3\"/>");
            x.append("<pageSetup paperSize=\"9\" orientation=\"portrait\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
            x.append("</worksheet>");
            return x.toString();
        }

        private static String s(int estilo) {
            return estilo == 0 ? "" : " s=\"" + estilo + "\"";
        }
    }

    private static String num(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.format(Locale.US, "%.10f", v).replaceAll("0+$", "");
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                default:
                    if (c >= 0x20 || c == '\t' || c == '\n') b.append(c);
            }
        }
        return b.toString();
    }

    private static void entrada(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ------------------------------------------------------------ partes fijas del paquete

    private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    private static String contentTypes() {
        return XML_DECL
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private static final String RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static String workbook(int ultimaFilaDetalle) {
        return XML_DECL
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<bookViews><workbookView activeTab=\"0\"/></bookViews>"
                + "<sheets><sheet name=\"Detalle\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "<sheet name=\"Resumen\" sheetId=\"2\" r:id=\"rId2\"/></sheets>"
                + "<definedNames><definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"0\" hidden=\"1\">"
                + "Detalle!$A$" + FILA_CABECERA + ":$F$" + ultimaFilaDetalle + "</definedName></definedNames>"
                + "</workbook>";
    }

    private static final String WORKBOOK_RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    private static final String STYLES = XML_DECL
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/>"
            + "<numFmt numFmtId=\"165\" formatCode=\"&quot;$&quot;\\ #,##0.00\"/></numFmts>"
            + "<fonts count=\"3\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF1F3864\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9E1F2\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left/><right/><top style=\"thin\"><color auto=\"1\"/></top>"
            + "<bottom style=\"thin\"><color auto=\"1\"/></bottom><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"12\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"                                         // 0
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                       // 1 negrita
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"><alignment horizontal=\"left\"/></xf>" // 2 fecha
            + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"             // 3 dinero
            + "<xf numFmtId=\"165\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>" // 4
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" // 5 cabecera
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                       // 6 título
            + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>" // 7
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"/>"    // 8 total etiqueta
            + "<xf numFmtId=\"165\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyBorder=\"1\"/>" // 9
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"/>"    // 10 total número
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\"/></xf>" // 11
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
