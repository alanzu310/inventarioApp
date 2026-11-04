package com.negocio.comida.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

/** Utilidades compartidas por las pestañas. */
final class Ui {
    private Ui() {
    }

    static String dinero(double v) {
        return String.format(Locale.US, "$ %,.2f", v);
    }

    static String numero(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(0);
        return nf.format(v);
    }

    /** Lee un número aceptando coma o punto decimal. Devuelve null si no es válido. */
    static Double leerNumero(String texto) {
        if (texto == null) return null;
        String t = texto.trim().replace(",", ".");
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(24);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
        return t;
    }

    static <T extends JComponent> T titulo(T c, String titulo) {
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(titulo),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        return c;
    }

    static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    static Font negrita(Font f, float tam) {
        return f.deriveFont(Font.BOLD, tam);
    }

    static void error(Component padre, String msg) {
        JOptionPane.showMessageDialog(padre, msg, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    static boolean confirmar(Component padre, String msg) {
        return JOptionPane.showConfirmDialog(padre, msg, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    static JSpinner spinnerFecha(LocalDate inicial) {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        ponerFecha(s, inicial);
        return s;
    }

    static LocalDate leerFecha(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static void ponerFecha(JSpinner s, LocalDate d) {
        s.setValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    /** Número para un campo de texto, sin separador de miles. */
    static String numeroCampo(double v) {
        return numero(v).replace(",", "");
    }

    /**
     * Quita la selección de {@code tabla} (ejecutando {@code deseleccionar}) cuando
     * se hace clic en una zona vacía de {@code panel}: el espacio sin filas de la
     * tabla o el fondo de la pestaña. Los clics en botones, campos, listas u otras
     * tablas no la quitan, porque esos controles trabajan con lo seleccionado.
     * También se deselecciona con la tecla Esc estando en la tabla.
     *
     * @param permitir si devuelve false no se deselecciona (p. ej. hay cambios sin guardar)
     */
    static void deseleccionarAlHacerClicFuera(JComponent panel, JTable tabla, BooleanSupplier permitir,
                                              Runnable deseleccionar) {
        Runnable accion = () -> {
            if (tabla.getSelectedRow() >= 0 && permitir.getAsBoolean()) deseleccionar.run();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(evento -> {
            if (evento.getID() != MouseEvent.MOUSE_PRESSED || !panel.isShowing()) return;
            MouseEvent e = (MouseEvent) evento;
            if (e.getComponent() == null) return;
            // Swing entrega el clic al primer componente que escucha el mouse (p. ej. el
            // contenedor de pestañas); se busca el componente que realmente está bajo el puntero.
            Component destino = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
            if (destino == null || !SwingUtilities.isDescendingFrom(destino, panel)) return;
            if (destino == tabla) {
                java.awt.Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tabla);
                if (tabla.rowAtPoint(p) < 0) accion.run(); // espacio en blanco debajo de las filas
                return;
            }
            for (Component c = destino; c != null && c != panel; c = c.getParent()) {
                if (esControl(c)) return;
            }
            accion.run();
        }, AWTEvent.MOUSE_EVENT_MASK);
        tabla.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "deseleccionar");
        tabla.getActionMap().put("deseleccionar", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                accion.run();
            }
        });
    }

    private static boolean esControl(Component c) {
        return c instanceof AbstractButton || c instanceof JTextComponent || c instanceof JComboBox
                || c instanceof JList || c instanceof JScrollBar || c instanceof JSpinner
                || c instanceof JTable || c instanceof javax.swing.table.JTableHeader
                || c instanceof BasicSplitPaneDivider || c instanceof BuscadorMenu;
    }

    // ------------------------------------------------------------ archivos

    private static final String CARPETA = "carpetaReportes";

    /**
     * Pide dónde guardar un archivo, recordando la última carpeta usada. Agrega la
     * extensión si falta y confirma antes de reemplazar. null si se cancela.
     */
    static java.io.File elegirDondeGuardar(Component padre, String titulo, String nombreSugerido,
                                           String extension, String descripcion) {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(Ui.class);
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser(prefs.get(CARPETA, System.getProperty("user.home")));
        fc.setDialogTitle(titulo);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                descripcion + " (*." + extension + ")", extension));
        fc.setSelectedFile(new java.io.File(fc.getCurrentDirectory(), nombreSugerido + "." + extension));
        if (fc.showSaveDialog(padre) != javax.swing.JFileChooser.APPROVE_OPTION) return null;
        java.io.File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith("." + extension)) {
            archivo = new java.io.File(archivo.getParentFile(), archivo.getName() + "." + extension);
        }
        if (archivo.exists() && !confirmar(padre, archivo.getName() + " ya existe. ¿Reemplazarlo?")) return null;
        if (archivo.getParentFile() != null) prefs.put(CARPETA, archivo.getParent());
        return archivo;
    }

    /** Pide un archivo para abrir, recordando la última carpeta usada. null si se cancela. */
    static java.io.File elegirParaAbrir(Component padre, String titulo, String extension, String descripcion) {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(Ui.class);
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser(prefs.get(CARPETA, System.getProperty("user.home")));
        fc.setDialogTitle(titulo);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                descripcion + " (*." + extension + ")", extension));
        if (fc.showOpenDialog(padre) != javax.swing.JFileChooser.APPROVE_OPTION) return null;
        java.io.File archivo = fc.getSelectedFile();
        if (archivo.getParentFile() != null) prefs.put(CARPETA, archivo.getParent());
        return archivo;
    }

    /** Avisa dónde quedó el archivo y ofrece abrirlo con el programa del sistema. */
    static void ofrecerAbrir(Component padre, String titulo, java.io.File archivo) {
        int op = JOptionPane.showOptionDialog(padre, "Guardado en:\n" + archivo.getAbsolutePath(), titulo,
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{"Abrir", "Cerrar"}, "Abrir");
        if (op != 0) return;
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(archivo);
                return;
            }
        } catch (java.io.IOException | UnsupportedOperationException ignored) {
            // se informa abajo
        }
        error(padre, "No se pudo abrir automáticamente. Ábralo desde:\n" + archivo.getAbsolutePath());
    }
}
