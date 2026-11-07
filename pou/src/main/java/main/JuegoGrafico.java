package main;

import model.Pou;
import service.PouService;
import ui.Navegador;
import ui.PanelInicio;
import ui.PanelJuego;
import ui.PouArte;
import ui.VoleyPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Version con ventana de Poun't (reemplaza a JuegoConsola).
 * Usa exactamente las mismas clases de model / dao / service / exception.
 * Ejecutar esta clase para jugar.
 */
public class JuegoGrafico implements Navegador {

    private final PouService service = new PouService();
    private final JFrame frame = new JFrame("Poun't");
    private PanelInicio inicio;
    private PanelJuego juego;
    private VoleyPanel voley;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) {
            // se usa el aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> new JuegoGrafico().arrancar());
    }

    private void arrancar() {
        if (!service.hayConexion()) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar a MySQL. Revisar que XAMPP este prendido, que exista\n"
                            + "la base y que este el driver mysql-connector-j en el proyecto.",
                    "Poun't", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        Main.crearTablas();
        service.cargarCatalogoInicial();

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (juego != null) juego.cerrar();
                System.exit(0);
            }
        });
        irInicio(null);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void mostrar(JComponent c) {
        frame.setContentPane(c);
        frame.pack();
        frame.revalidate();
        frame.repaint();
        c.requestFocusInWindow();
    }

    @Override
    public void irInicio(String mensaje) {
        if (voley != null) { voley.detener(); voley = null; }
        juego = null;
        inicio = new PanelInicio(this, service, mensaje);
        mostrar(inicio);
    }

    @Override
    public void jugar(Pou pou) {
        if (inicio != null) inicio.detener();
        juego = new PanelJuego(this, service, pou);
        mostrar(juego);
    }

    @Override
    public void abrirVoley(Pou pou, PouArte.Ropa ropa, PanelJuego volverA) {
        volverA.pausar();
        voley = new VoleyPanel(this, volverA, pou.getNombre(), ropa);
        mostrar(voley);
        voley.iniciar();
    }

    @Override
    public void volverDelVoley(PanelJuego panel, int puntosGanados, boolean gano) {
        if (voley != null) { voley.detener(); voley = null; }
        mostrar(panel);
        panel.reanudar(puntosGanados, gano);
    }
}
