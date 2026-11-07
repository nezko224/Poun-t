package dao;

import model.Pou;

import java.util.List;

public interface PouDao {
    void crear(Pou pou);
    Pou listarPorId(int id);
    List<Pou> listarTodo();
    void actualizar(Pou pou);
}
