package model;

import java.time.LocalDateTime;

public class Pou {

    public enum EstadoPou {
        SANO, ENFERMO, DORMIDO, MUERTO
    }

    private int id;
    private String nombre;
    private int hambre;
    private int higiene;
    private int sueño;
    private EstadoPou estado;
    private double saldo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;

    public Pou() {
    }

    public Pou(String nombre) {
        this.nombre = nombre;
        this.hambre = 100;
        this.higiene = 100;
        this.sueño = 100;
        this.estado = EstadoPou.SANO;
        this.saldo = 0;
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaActualizacion = LocalDateTime.now();
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

    public int getHambre() {
        return hambre;
    }

    public void setHambre(int hambre) {
        if (hambre > 100) hambre = 100;
        if (hambre < 0) hambre = 0;
        this.hambre = hambre;
    }

    public int getHigiene() {
        return higiene;
    }

    public void setHigiene(int higiene) {
        if (higiene > 100) higiene = 100;
        if (higiene < 0) higiene = 0;
        this.higiene = higiene;
    }

    public int getSueño() {
        return sueño;
    }

    public void setSueño(int sueño) {
        this.sueño = sueño;
    }

    public EstadoPou getEstado() {
        return estado;
    }

    public void setEstado(EstadoPou estado) {
        this.estado = estado;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    @Override
    public String toString() {
        return "Pou #" + id + " [" + nombre + "] hambre=" + hambre
                + " higiene=" + higiene + " sueño=" + sueño
                + " estado=" + estado + " saldo=" + saldo;
    }
}
