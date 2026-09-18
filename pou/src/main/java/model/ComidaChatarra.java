package model;

public class ComidaChatarra extends Alimento {

    private int valorHigienePenalizacion;

    public ComidaChatarra() {
        super();
    }

    public ComidaChatarra(String nombre, double precio, int valorHambre, int valorHigienePenalizacion) {
        super(nombre, precio, valorHambre);
        this.valorHigienePenalizacion = valorHigienePenalizacion;
    }

    public int getValorHigienePenalizacion() {
        return valorHigienePenalizacion;
    }

    public void setValorHigienePenalizacion(int valorHigienePenalizacion) {
        this.valorHigienePenalizacion = valorHigienePenalizacion;
    }

    @Override
    public void aplicarEfecto(Pou pou) {
        pou.setHambre(pou.getHambre() + valorHambre);
        pou.setHigiene(pou.getHigiene() - valorHigienePenalizacion);
    }
}
