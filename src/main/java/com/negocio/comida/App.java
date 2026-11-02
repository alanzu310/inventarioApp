package com.negocio.comida;

import com.negocio.comida.modelo.DatosNegocio;
import com.negocio.comida.modelo.Repositorio;
import com.negocio.comida.ui.VentanaPrincipal;
import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("es"));
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // se usa el aspecto por defecto
            }
            DatosNegocio datos = Repositorio.cargar();
            new VentanaPrincipal(datos).setVisible(true);
        });
    }
}
