package ui;

import model.Pou;

/** Lo que las pantallas necesitan para cambiar de pantalla sin conocer la ventana. */
public interface Navegador {
    void irInicio(String mensaje);
    void jugar(Pou pou);
    void abrirVoley(Pou pou, PouArte.Ropa ropa, PanelJuego volverA);
    void volverDelVoley(PanelJuego panel, int puntosGanados, boolean gano);

    // --- menu de minijuegos
    void abrirMinijuegos(Pou pou, PouArte.Ropa ropa, PanelJuego volverA);
    void abrirSnake(Pou pou, PanelJuego volverA);
    void volverDelMenu(PanelJuego panel);
    void volverDelSnake(PanelJuego panel, int victorias, int monedas);
    void abrirShooter(Pou pou, PouArte.Ropa ropa, PanelJuego volverA);
    void volverDelShooter(PanelJuego panel, int monedas, boolean gano, int capitulo);
}
