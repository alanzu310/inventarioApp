package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Repositorio;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal(DatosNegocio datos) {
        super("Negocio de Comida - Gestión diaria");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Guardado automático tras cada cambio
        datos.agregarOyente(() -> {
            try {
                Repositorio.guardar(datos);
            } catch (IOException e) {
                Ui.error(this, "No se pudieron guardar los datos: " + e.getMessage());
            }
        });

        setJMenuBar(crearMenu(datos));

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Principal", new PanelPrincipal(datos));
        pestanas.addTab("Inventario", new PanelInventario(datos));
        pestanas.addTab("Proveedores", new PanelProveedores(datos));
        pestanas.addTab("Menús", new PanelMenus(datos));
        pestanas.addTab("Clientes", new PanelClientes(datos));
        add(pestanas);

        setMinimumSize(new Dimension(1000, 650));
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    private JMenuBar crearMenu(DatosNegocio datos) {
        AccionesArchivo acciones = new AccionesArchivo(this, datos);
        JMenu archivo = new JMenu("Archivo");
        archivo.setMnemonic('A');
        archivo.add(item("Exportar datos (respaldo)...", acciones::exportar));
        archivo.add(item("Importar datos...", acciones::importar));
        archivo.addSeparator();
        archivo.add(item("Reporte completo en Excel...", acciones::reporteCompleto));
        archivo.addSeparator();
        archivo.add(item("Ubicación de los datos", acciones::verCarpetaDatos));
        archivo.add(item("Salir", () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));
        JMenuBar barra = new JMenuBar();
        barra.add(archivo);
        return barra;
    }

    private static JMenuItem item(String texto, Runnable accion) {
        JMenuItem i = new JMenuItem(texto);
        i.addActionListener(e -> accion.run());
        return i;
    }
}
