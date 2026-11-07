package exception;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(double precio, double saldo) {
        super("No te alcanza: cuesta " + (int) precio + " y tenes " + (int) saldo + " monedas.");
    }
}
