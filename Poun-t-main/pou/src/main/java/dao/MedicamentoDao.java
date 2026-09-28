package dao;

import model.Medicamento;

import java.util.List;

public interface MedicamentoDao {
    void crear(Medicamento medicamento);
    List<Medicamento> listarTodo();
}
