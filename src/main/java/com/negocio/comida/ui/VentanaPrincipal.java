package com.negocio.comida.ui;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Repositorio;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.JFrame;
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
}
