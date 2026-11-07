package dao;

import model.Moneda;

public interface MonedaDao {
    void crear(Moneda moneda, int idPou);
    void marcarRecolectada(int id);
}
