package com.negocio.comida.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

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
}
