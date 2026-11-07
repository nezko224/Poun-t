package dao.impl;

import dao.ConexionBD;
import dao.PrendaDao;
import model.Prenda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PrendaDaoImpl implements PrendaDao {

    @Override
    public void crear(Prenda prenda, int idPou) {
        String sql = "INSERT INTO prenda (nombre, conjunto, precio, esta_puesta, id_pou) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, prenda.getNombre());
            ps.setString(2, prenda.getTipo().name());
            ps.setDouble(3, prenda.getPrecio());
            ps.setBoolean(4, prenda.isEstaPuesta());
            ps.setInt(5, idPou);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    prenda.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Prenda> listarPorPou(int idPou) {
        String sql = "SELECT * FROM prenda WHERE id_pou = ? ORDER BY id";
        List<Prenda> lista = new ArrayList<>();

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPou);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Prenda p = new Prenda(rs.getString("nombre"),
                            Prenda.TipoConjunto.valueOf(rs.getString("conjunto")),
                            rs.getDouble("precio"));
                    p.setId(rs.getInt("id"));
                    p.setEstaPuesta(rs.getBoolean("esta_puesta"));
                    lista.add(p);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    @Override
    public void actualizar(Prenda prenda) {
        String sql = "UPDATE prenda SET esta_puesta = ? WHERE id = ?";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, prenda.isEstaPuesta());
            ps.setInt(2, prenda.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
