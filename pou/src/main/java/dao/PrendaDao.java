package dao;

import model.Prenda;

import java.util.List;

public interface PrendaDao {
    void crear(Prenda prenda, int idPou);
    List<Prenda> listarPorPou(int idPou);
    void actualizar(Prenda prenda);
}
