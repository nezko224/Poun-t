package exception;

/**
 * Se lanza cuando el nombre elegido para el Pou no se puede guardar
 * (esta vacio o es mas largo que la columna "nombre" de la base).
 *
 * Uso rapido:  NombreInvalidoException.validar(nombre);
 */
public class NombreInvalidoException extends RuntimeException {

    /** Coincide con nombre VARCHAR(50) de la tabla pou. */
    public static final int LARGO_MAXIMO = 50;

    public NombreInvalidoException(String mensaje) {
        super(mensaje);
    }

    public static NombreInvalidoException demasiadoLargo(int largoActual) {
        return new NombreInvalidoException("El nombre es muy largo: tiene " + largoActual
                + " letras y el maximo es " + LARGO_MAXIMO + ".");
    }

    /** Lanza la excepcion si el nombre no sirve; si esta bien, no hace nada. */
    public static void validar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new NombreInvalidoException("El nombre no puede estar vacio.");
        }
        if (nombre.length() > LARGO_MAXIMO) {
            throw demasiadoLargo(nombre.length());
        }
    }
}
