package model;

public abstract class Alimento implements Interactuable {

    protected int id;
    protected String nombre;
    protected double precio;
    protected int valorHambre;

    public Alimento() {
    }

    public Alimento(String nombre, double precio, int valorHambre) {
        this.nombre = nombre;
        this.precio = precio;
        this.valorHambre = valorHambre;
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

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getValorHambre() {
        return valorHambre;
    }

    public void setValorHambre(int valorHambre) {
        this.valorHambre = valorHambre;
    }

    @Override
    public abstract void aplicarEfecto(Pou pou);
}
