package model;

public class Prenda implements Interactuable {

    public enum TipoConjunto {
        CONJUNTO_1, CONJUNTO_2, CONJUNTO_3
    }

    private int id;
    private String nombre;
    private TipoConjunto tipo;
    private double precio;
    private boolean estaPuesta;

    public Prenda() {
    }

    public Prenda(String nombre, TipoConjunto tipo, double precio) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.estaPuesta = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoConjunto getTipo() {
        return tipo;
    }

    public void setTipo(TipoConjunto tipo) {
        this.tipo = tipo;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public boolean isEstaPuesta() {
        return estaPuesta;
    }

    public void setEstaPuesta(boolean estaPuesta) {
        this.estaPuesta = estaPuesta;
    }

    @Override
    public void aplicarEfecto(Pou pou) {
        this.estaPuesta = true;
    }
}
