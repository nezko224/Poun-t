package exception;

import java.sql.SQLException;

/**
 * Envuelve un SQLException para que los DAO avisen del error hacia arriba
 * en vez de solo imprimirlo en consola y seguir como si nada.
 *
 * Uso rapido (dentro de un catch de un DAO):
 *     throw new BaseDeDatosException("No se pudo guardar el Pou", e);
 */
public class BaseDeDatosException extends RuntimeException {

    public BaseDeDatosException(String mensaje, SQLException causa) {
        super(mensaje, causa);
    }
}
