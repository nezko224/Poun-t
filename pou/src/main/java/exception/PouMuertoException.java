package exception;

import model.Pou;

/**
 * Se lanza cuando se intenta hacer algo con un Pou que ya murio.
 * Extiende AccionNoPermitidaException, asi que los catch que ya existen
 * en JuegoConsola y PanelJuego la atrapan sin cambiar nada.
 *
 * Uso rapido:  PouMuertoException.verificarVivo(pou);
 */
public class PouMuertoException extends AccionNoPermitidaException {

    public PouMuertoException(String nombrePou) {
        super(nombrePou + " esta muerto.");
    }

    /** Lanza la excepcion si el Pou esta muerto; si esta vivo, no hace nada. */
    public static void verificarVivo(Pou pou) {
        if (pou.getEstado() == Pou.EstadoPou.MUERTO) {
            throw new PouMuertoException(pou.getNombre());
        }
    }
}
