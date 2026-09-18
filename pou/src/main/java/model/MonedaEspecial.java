package model;

public class MonedaEspecial extends Moneda {

    private double probabilidadAparicion;

    public MonedaEspecial() {
        super();
    }

    public MonedaEspecial(int posicionX, int posicionY, double probabilidadAparicion) {
        super(5, posicionX, posicionY, 3);
        this.probabilidadAparicion = probabilidadAparicion;
    }

    public double getProbabilidadAparicion() {
        return probabilidadAparicion;
    }

    public void setProbabilidadAparicion(double probabilidadAparicion) {
        this.probabilidadAparicion = probabilidadAparicion;
    }
}
