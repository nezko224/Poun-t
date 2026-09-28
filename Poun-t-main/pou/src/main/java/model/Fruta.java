package model;

public class Fruta extends Alimento {

    private int valorHigieneBonus;

    public Fruta() {
        super();
    }

    public Fruta(String nombre, double precio, int valorHambre, int valorHigieneBonus) {
        super(nombre, precio, valorHambre);
        this.valorHigieneBonus = valorHigieneBonus;
    }

    public int getValorHigieneBonus() {
        return valorHigieneBonus;
    }

    public void setValorHigieneBonus(int valorHigieneBonus) {
        this.valorHigieneBonus = valorHigieneBonus;
    }

    @Override
    public void aplicarEfecto(Pou pou) {
        pou.setHambre(pou.getHambre() + valorHambre);
        pou.setHigiene(pou.getHigiene() + valorHigieneBonus);
    }
}
