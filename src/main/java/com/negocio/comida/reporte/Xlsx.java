package com.negocio.comida.reporte;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor mínimo de libros de Excel (.xlsx) sin librerías externas. Los
 * números, fechas y montos se guardan como valores numéricos con formato, para
 * que se puedan sumar, filtrar y ordenar en Excel.
 */
final class Xlsx {

    // Estilos (índices de cellXfs en styles.xml)
    static final int NORMAL = 0;
    static final int NEGRITA = 1;
    static final int FECHA = 2;
    static final int DINERO = 3;
    static final int DINERO_NEGRITA = 4;
    static final int CABECERA = 5;
    static final int TITULO = 6;
    static final int TOTAL_ETIQUETA = 8;
    static final int TOTAL_DINERO = 9;
    static final int TOTAL_NUMERO = 10;
    static final int CABECERA_DER = 11;
    static final int DECIMAL = 12;
    static final int TOTAL_DECIMAL = 13;

    private Xlsx() {
    }

    /** Letra de columna de Excel (0 → A, 25 → Z, 26 → AA). */
    static String col(int indice) {
        StringBuilder b = new StringBuilder();
        int n = indice + 1;
        while (n > 0) {
            int r = (n - 1) % 26;
            b.insert(0, (char) ('A' + r));
            n = (n - 1) / 26;
        }
        return b.toString();
    }

    /** Número de serie de fecha de Excel (días desde 30/12/1899). */
    static long serialFecha(LocalDate d) {
        return ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), d);
    }

    /** Una hoja del libro. Las celdas de cada fila se agregan en orden y luego {@link #finFila()}. */
    static final class Hoja {
        final String nombre;
        private final StringBuilder filas = new StringBuilder();
        private final StringBuilder fila = new StringBuilder();
        private int numFila;
        private double[] anchos = new double[0];
        private int filaFija;
        private String filtro;
        private boolean horizontal;

        Hoja(String nombre) {
            this.nombre = nombre;
        }

        Hoja anchos(double... anchos) {
            this.anchos = anchos;
            return this;
        }

        /** Deja fijas (siempre visibles) las primeras {@code filas} filas. */
        Hoja fijarFilas(int filas) {
            this.filaFija = filas;
            return this;
        }

        /** Rango con filtros, p. ej. "A7:F40". */
        Hoja filtro(String rango) {
            this.filtro = rango;
            return this;
        }

        /** Imprimir en hoja horizontal (para tablas anchas). */
        Hoja horizontal() {
            this.horizontal = true;
            return this;
        }

        String getFiltro() {
            return filtro;
        }

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

        void fecha(String col, int f, LocalDate d, int estilo) {
            numero(col, f, serialFecha(d), estilo);
        }

        void formula(String col, int f, String formula, double valorCalculado, int estilo) {
            celda(col, f, s(estilo), "<f>" + esc(formula) + "</f><v>" + num(valorCalculado) + "</v>");
        }

        /** Escribe una fila de cabecera desde la columna A. Las de {@code derecha} se alinean a la derecha. */
        void cabecera(int f, String[] titulos, boolean[] derecha) {
            for (int i = 0; i < titulos.length; i++) {
                texto(col(i), f, titulos[i], derecha != null && derecha[i] ? CABECERA_DER : CABECERA);
            }
            finFila();
        }

        void finFila() {
            if (fila.length() == 0) return;
            filas.append("<row r=\"").append(numFila).append("\">").append(fila).append("</row>");
            fila.setLength(0);
        }

        String xml(boolean seleccionada) {
            StringBuilder x = new StringBuilder(XML_DECL);
            x.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                    .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
            x.append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
            x.append("<sheetViews><sheetView workbookViewId=\"0\"").append(seleccionada ? " tabSelected=\"1\"" : "");
            if (filaFija > 0) {
                x.append("><pane ySplit=\"").append(filaFija).append("\" topLeftCell=\"A").append(filaFija + 1)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
            } else {
                x.append("/></sheetViews>");
            }
            if (anchos.length > 0) {
                x.append("<cols>");
                for (int i = 0; i < anchos.length; i++) {
                    x.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                            .append(num(anchos[i])).append("\" customWidth=\"1\"/>");
                }
                x.append("</cols>");
            }
            x.append("<sheetData>").append(filas).append("</sheetData>");
            if (filtro != null) x.append("<autoFilter ref=\"").append(filtro).append("\"/>");
            x.append("<pageMargins left=\"0.6\" right=\"0.6\" top=\"0.7\" bottom=\"0.7\" header=\"0.3\" footer=\"0.3\"/>");
            x.append("<pageSetup paperSize=\"9\" orientation=\"").append(horizontal ? "landscape" : "portrait").append("\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
            x.append("</worksheet>");
            return x.toString();
        }

        private static String s(int estilo) {
            return estilo == 0 ? "" : " s=\"" + estilo + "\"";
        }
    }

    /** Escribe el libro con las hojas en el orden dado (la primera queda activa). */
    static void escribir(Path archivo, List<Hoja> hojas) throws IOException {
        try (OutputStream os = Files.newOutputStream(archivo);
             ZipOutputStream zip = new ZipOutputStream(os)) {
            entrada(zip, "[Content_Types].xml", contentTypes(hojas.size()));
            entrada(zip, "_rels/.rels", RELS);
            entrada(zip, "xl/workbook.xml", workbook(hojas));
            entrada(zip, "xl/_rels/workbook.xml.rels", workbookRels(hojas.size()));
            entrada(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < hojas.size(); i++) {
                entrada(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", hojas.get(i).xml(i == 0));
            }
        }
    }

    static String num(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.format(Locale.US, "%.10f", v).replaceAll("0+$", "");
    }

    static String esc(String s) {
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

    private static String contentTypes(int n) {
        StringBuilder b = new StringBuilder(XML_DECL)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= n; i++) {
            b.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return b.append("</Types>").toString();
    }

    private static final String RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static String workbook(List<Hoja> hojas) {
        StringBuilder b = new StringBuilder(XML_DECL)
                .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                .append("<bookViews><workbookView activeTab=\"0\"/></bookViews><sheets>");
        for (int i = 0; i < hojas.size(); i++) {
            b.append("<sheet name=\"").append(esc(hojas.get(i).nombre)).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        b.append("</sheets>");
        StringBuilder nombres = new StringBuilder();
        for (int i = 0; i < hojas.size(); i++) {
            String f = hojas.get(i).getFiltro();
            if (f == null) continue;
            String[] partes = f.split(":");
            nombres.append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"").append(i)
                    .append("\" hidden=\"1\">'").append(esc(hojas.get(i).nombre.replace("'", "''"))).append("'!")
                    .append(absoluta(partes[0])).append(':').append(absoluta(partes[1])).append("</definedName>");
        }
        if (nombres.length() > 0) b.append("<definedNames>").append(nombres).append("</definedNames>");
        return b.append("</workbook>").toString();
    }

    /** "A7" → "$A$7". */
    private static String absoluta(String ref) {
        int i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) i++;
        return "$" + ref.substring(0, i) + "$" + ref.substring(i);
    }

    private static String workbookRels(int n) {
        StringBuilder b = new StringBuilder(XML_DECL)
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= n; i++) {
            b.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        b.append("<Relationship Id=\"rId").append(n + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return b.append("</Relationships>").toString();
    }

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
            + "<cellXfs count=\"14\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"                                         // 0 normal
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                       // 1 negrita
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\"/></xf>" // 2 fecha
            + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"             // 3 dinero
            + "<xf numFmtId=\"165\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>" // 4 dinero negrita
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" // 5 cabecera
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                       // 6 título
            + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>" // 7
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"/>"    // 8 total etiqueta
            + "<xf numFmtId=\"165\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyBorder=\"1\"/>" // 9 total dinero
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"/>"    // 10 total número
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\"/></xf>" // 11 cabecera derecha
            + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"               // 12 decimal 2
            + "<xf numFmtId=\"4\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyBorder=\"1\"/>" // 13 total decimal
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
