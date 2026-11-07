package dao.impl;

import dao.ConexionBD;
import dao.MedicamentoDao;
import model.Medicamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MedicamentoDaoImpl implements MedicamentoDao {

    @Override
    public void crear(Medicamento medicamento) {
        String sql = "INSERT INTO medicamento (nombre, precio, efectividad) VALUES (?, ?, ?)";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, medicamento.getNombre());
            ps.setDouble(2, medicamento.getPrecio());
            ps.setInt(3, medicamento.getEfectividad());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    medicamento.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Medicamento> listarTodo() {
        String sql = "SELECT * FROM medicamento ORDER BY id";
        List<Medicamento> lista = new ArrayList<>();

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Medicamento m = new Medicamento(rs.getString("nombre"), rs.getDouble("precio"),
                        rs.getInt("efectividad"));
                m.setId(rs.getInt("id"));
                lista.add(m);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
