package com.negocio.comida.ui;

import com.negocio.comida.modelo.Menu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Campo para elegir un menú escribiendo su nombre o palabras clave (sin
 * importar mayúsculas ni tildes). Mientras se escribe aparecen solo los menús
 * que coinciden; se elige con clic, flechas + Enter, o escribiendo hasta que
 * quede uno solo.
 */
class BuscadorMenu extends JPanel {
    private static final int MAX_SUGERENCIAS = 8;
    private static final Color SIN_COINCIDENCIAS = new Color(0xB0, 0x20, 0x20);

    private final Supplier<List<Menu>> fuente;
    private final String ambito;
    private final JTextField txt = new JTextField(16);
    private final DefaultListModel<Menu> modeloLista = new DefaultListModel<>();
    private final JList<Menu> lista = new JList<>(modeloLista);
    private final JPopupMenu popup = new JPopupMenu();
    private final Color colorNormal;
    private boolean ajustando;

    /**
     * @param fuente menús entre los que se busca
     * @param ambito texto para los mensajes, p. ej. "en el menú del día"
     */
    BuscadorMenu(Supplier<List<Menu>> fuente, String ambito) {
        super(new BorderLayout());
        this.fuente = fuente;
        this.ambito = ambito;
        add(txt);
        colorNormal = txt.getForeground();
        txt.setToolTipText("Escriba el nombre del menú o una palabra (por ejemplo: pollo)");

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFocusable(false);
        popup.setFocusable(false);
        popup.setBorder(UIManager.getBorder("PopupMenu.border"));
        popup.setLayout(new BorderLayout());
        popup.add(lista, BorderLayout.CENTER);

        txt.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cambio(); }
            @Override public void removeUpdate(DocumentEvent e) { cambio(); }
            @Override public void changedUpdate(DocumentEvent e) { cambio(); }
        });
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int i = lista.locationToIndex(e.getPoint());
                if (i >= 0) elegir(modeloLista.get(i));
            }
        });
        txt.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.setVisible(false);
            }
        });

        teclado("DOWN", () -> mover(1));
        teclado("UP", () -> mover(-1));
        teclado("ESCAPE", () -> popup.setVisible(false));
        teclado("ENTER", () -> {
            if (popup.isVisible() && lista.getSelectedValue() != null) {
                elegir(lista.getSelectedValue());
            } else {
                Menu m = getSeleccionado();
                if (m != null) elegir(m);
            }
        });
    }

    // ------------------------------------------------------------ API

    /**
     * Menú elegido: el que coincide exactamente con el texto o, si no, el único
     * que contiene todas las palabras escritas. null si no hay o hay varios.
     */
    Menu getSeleccionado() {
        String q = normalizar(txt.getText());
        if (q.isEmpty()) return null;
        List<Menu> todos = fuente.get();
        for (Menu m : todos) {
            if (normalizar(m.getNombre()).equals(q)) return m;
        }
        List<Menu> c = coincidencias(todos, q);
        return c.size() == 1 ? c.get(0) : null;
    }

    /** Explica por qué no hay un menú elegido (para mostrar al usuario). */
    String motivoSinSeleccion() {
        String texto = txt.getText().trim();
        List<Menu> todos = fuente.get();
        if (todos.isEmpty()) return "No hay menús " + ambito + ".";
        if (texto.isEmpty()) return "Escriba el nombre del menú o una palabra para buscarlo.";
        List<Menu> c = coincidencias(todos, normalizar(texto));
        if (c.isEmpty()) return "Ningún menú " + ambito + " coincide con \"" + texto + "\".";
        return "Hay " + c.size() + " menús que coinciden con \"" + texto + "\":\n  "
                + c.stream().map(Menu::getNombre).collect(Collectors.joining("\n  "))
                + "\n\nEscriba más palabras o elija uno de la lista.";
    }

    void limpiar() {
        ajustando = true;
        txt.setText("");
        txt.setForeground(colorNormal);
        ajustando = false;
        popup.setVisible(false);
    }

    void enfocar() {
        txt.requestFocusInWindow();
    }

    // ------------------------------------------------------------ búsqueda

    static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /** Menús cuyo nombre contiene todas las palabras de la búsqueda. */
    static List<Menu> coincidencias(List<Menu> menus, String busquedaNormalizada) {
        String[] palabras = busquedaNormalizada.split(" ");
        List<Menu> res = new ArrayList<>();
        for (Menu m : menus) {
            String nombre = normalizar(m.getNombre());
            boolean todas = true;
            for (String p : palabras) {
                if (!nombre.contains(p)) {
                    todas = false;
                    break;
                }
            }
            if (todas) res.add(m);
        }
        // primero los que empiezan con lo escrito, luego en orden alfabético
        res.sort(Comparator.comparing((Menu m) -> !normalizar(m.getNombre()).startsWith(busquedaNormalizada))
                .thenComparing(m -> normalizar(m.getNombre())));
        return res;
    }

    // ------------------------------------------------------------ interno

    private void teclado(String tecla, Runnable accion) {
        txt.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(tecla), "buscador-" + tecla);
        txt.getActionMap().put("buscador-" + tecla, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accion.run();
            }
        });
    }

    private void cambio() {
        if (ajustando) return;
        SwingUtilities.invokeLater(this::mostrarSugerencias);
    }

    private void mostrarSugerencias() {
        String q = normalizar(txt.getText());
        if (q.isEmpty()) {
            txt.setForeground(colorNormal);
            popup.setVisible(false);
            return;
        }
        List<Menu> c = coincidencias(fuente.get(), q);
        txt.setForeground(c.isEmpty() ? SIN_COINCIDENCIAS : colorNormal);
        boolean yaElegido = c.size() == 1 && normalizar(c.get(0).getNombre()).equals(q);
        if (c.isEmpty() || yaElegido || !txt.isShowing() || !txt.hasFocus()) {
            popup.setVisible(false);
            return;
        }
        modeloLista.clear();
        for (int i = 0; i < Math.min(MAX_SUGERENCIAS, c.size()); i++) modeloLista.addElement(c.get(i));
        lista.setVisibleRowCount(modeloLista.size());
        lista.setSelectedIndex(0);
        popup.setPopupSize(new Dimension(Math.max(txt.getWidth(), lista.getPreferredSize().width + 12),
                lista.getPreferredSize().height + 6));
        popup.show(txt, 0, txt.getHeight());
        txt.requestFocusInWindow();
    }

    private void mover(int delta) {
        if (!popup.isVisible()) {
            mostrarSugerencias();
            return;
        }
        int n = modeloLista.size();
        if (n == 0) return;
        int i = Math.floorMod(lista.getSelectedIndex() + delta, n);
        lista.setSelectedIndex(i);
    }

    private void elegir(Menu m) {
        ajustando = true;
        txt.setText(m.getNombre());
        txt.setForeground(colorNormal);
        ajustando = false;
        popup.setVisible(false);
    }
}
