package ui;

import model.Pou;

/** Lo que las pantallas necesitan para cambiar de pantalla sin conocer la ventana. */
public interface Navegador {
    void irInicio(String mensaje);
    void jugar(Pou pou);
    void abrirVoley(Pou pou, PouArte.Ropa ropa, PanelJuego volverA);
    void volverDelVoley(PanelJuego panel, int puntosGanados, boolean gano);
}
