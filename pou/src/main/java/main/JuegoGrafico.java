package main;

import model.Pou;
import service.PouService;
import ui.MenuMinijuegos;
import ui.Navegador;
import ui.PanelInicio;
import ui.PanelJuego;
import ui.PouArte;
import ui.ShooterPanel;
import ui.SnakePanel;
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
    private MenuMinijuegos menu;
    private SnakePanel snake;
    private ShooterPanel shooter;

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
        if (menu != null) { menu.detener(); menu = null; }
        if (snake != null) { snake.detener(); snake = null; }
        if (shooter != null) { shooter.detener(); shooter = null; }
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

    // ----- menu de minijuegos (Voley / Snake)

    @Override
    public void abrirMinijuegos(Pou pou, PouArte.Ropa ropa, PanelJuego volverA) {
        volverA.pausar();
        menu = new MenuMinijuegos(this, volverA, pou.getNombre(), ropa,
                () -> { menu = null; abrirVoley(pou, ropa, volverA); },
                () -> { menu = null; abrirSnake(pou, volverA); },
                () -> { menu = null; abrirShooter(pou, ropa, volverA); });
        mostrar(menu);
        menu.iniciar();
    }

    @Override
    public void abrirSnake(Pou pou, PanelJuego volverA) {
        volverA.pausar();
        snake = new SnakePanel(this, volverA);
        mostrar(snake);
        snake.iniciar();
    }

    @Override
    public void volverDelMenu(PanelJuego panel) {
        if (menu != null) { menu.detener(); menu = null; }
        mostrar(panel);
        panel.reanudarDelMenu();
    }

    @Override
    public void volverDelSnake(PanelJuego panel, int victorias, int monedas) {
        if (snake != null) { snake.detener(); snake = null; }
        mostrar(panel);
        panel.reanudarSnake(victorias, monedas);
    }

    @Override
    public void abrirShooter(Pou pou, PouArte.Ropa ropa, PanelJuego volverA) {
        volverA.pausar();
        shooter = new ShooterPanel(this, volverA, ropa);
        mostrar(shooter);
        shooter.iniciar();
    }

    @Override
    public void volverDelShooter(PanelJuego panel, int monedas, boolean gano, int capitulo) {
        if (shooter != null) { shooter.detener(); shooter = null; }
        mostrar(panel);
        panel.reanudarShooter(monedas, gano, capitulo);
    }
}
