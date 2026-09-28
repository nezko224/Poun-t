package model;

public class Medicamento implements Interactuable {

    private int id;
    private String nombre;
    private double precio;
    private int efectividad; 

    public Medicamento() {
    }

    public Medicamento(String nombre, double precio, int efectividad) {
        this.nombre = nombre;
        this.precio = precio;
        this.efectividad = efectividad;
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

    public int getEfectividad() {
        return efectividad;
    }

    public void setEfectividad(int efectividad) {
        this.efectividad = efectividad;
    }

    @Override
    public void aplicarEfecto(Pou pou) {
        if (pou.getEstado() == Pou.EstadoPou.ENFERMO) {
            pou.setHigiene(pou.getHigiene() + efectividad);
            if (efectividad >= 100) {
                pou.setEstado(Pou.EstadoPou.SANO);
            }
        }
    }
}
