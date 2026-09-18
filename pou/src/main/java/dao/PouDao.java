package dao;

import model.Pou;

public interface PouDao {
    void crear(Pou pou);
    Pou listarPorId(int id);
}
