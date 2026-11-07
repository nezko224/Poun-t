package model;

import java.time.LocalDateTime;

public abstract class Moneda {

    protected int id;
    protected double valor;
    protected int posicionX;
    protected int posicionY;
    protected LocalDateTime tiempoAparicion;
    protected int tiempoVidaSegundos;
    protected boolean recolectada;

    public Moneda() {
    }

    public Moneda(double valor, int posicionX, int posicionY, int tiempoVidaSegundos) {
        this.valor = valor;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.tiempoVidaSegundos = tiempoVidaSegundos;
        this.tiempoAparicion = LocalDateTime.now();
        this.recolectada = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public int getPosicionX() {
        return posicionX;
    }

    public void setPosicionX(int posicionX) {
        this.posicionX = posicionX;
    }

    public int getPosicionY() {
        return posicionY;
    }

    public void setPosicionY(int posicionY) {
        this.posicionY = posicionY;
    }

    public LocalDateTime getTiempoAparicion() {
        return tiempoAparicion;
    }

    public void setTiempoAparicion(LocalDateTime tiempoAparicion) {
        this.tiempoAparicion = tiempoAparicion;
    }

    public int getTiempoVidaSegundos() {
        return tiempoVidaSegundos;
    }

    public void setTiempoVidaSegundos(int tiempoVidaSegundos) {
        this.tiempoVidaSegundos = tiempoVidaSegundos;
    }

    public boolean isRecolectada() {
        return recolectada;
    }

    public void setRecolectada(boolean recolectada) {
        this.recolectada = recolectada;
    }
}
