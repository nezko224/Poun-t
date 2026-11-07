package model;

public class Vegetal extends Alimento {

    private double multiplicadorHambre;

    public Vegetal() {
        super();
    }

    public Vegetal(String nombre, double precio, int valorHambre, double multiplicadorHambre) {
        super(nombre, precio, valorHambre);
        this.multiplicadorHambre = multiplicadorHambre;
    }

    public double getMultiplicadorHambre() {
        return multiplicadorHambre;
    }

    public void setMultiplicadorHambre(double multiplicadorHambre) {
        this.multiplicadorHambre = multiplicadorHambre;
    }

    @Override
    public void aplicarEfecto(Pou pou) {
        int hambreExtra = (int) (valorHambre * multiplicadorHambre);
        pou.setHambre(pou.getHambre() + hambreExtra);
    }
}
