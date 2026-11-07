package dao;

import model.Alimento;

import java.util.List;

public interface AlimentoDao {
    void crear(Alimento alimento);
    List<Alimento> listarTodo();
}
