package dao.impl;

import dao.ConexionBD;
import dao.PouDao;
import model.Pou;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class PouDaoImpl implements PouDao {

    @Override
    public void crear(Pou pou) {
        String sql = "INSERT INTO pou (nombre, hambre, higiene, sueño, estado, saldo, fecha_creacion, ultima_actualizacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pou.getNombre());
            ps.setInt(2, pou.getHambre());
            ps.setInt(3, pou.getHigiene());
            ps.setInt(4, pou.getSueño());
            ps.setString(5, pou.getEstado().name());
            ps.setDouble(6, pou.getSaldo());
            ps.setTimestamp(7, Timestamp.valueOf(pou.getFechaCreacion()));
            ps.setTimestamp(8, Timestamp.valueOf(pou.getUltimaActualizacion()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pou.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Pou listarPorId(int id) {
        String sql = "SELECT * FROM pou WHERE id = ?";
        Pou pou = null;

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pou = new Pou();
                    pou.setId(rs.getInt("id"));
                    pou.setNombre(rs.getString("nombre"));
                    pou.setHambre(rs.getInt("hambre"));
                    pou.setHigiene(rs.getInt("higiene"));
                    pou.setSueño(rs.getInt("sueño"));
                    pou.setEstado(Pou.EstadoPou.valueOf(rs.getString("estado")));
                    pou.setSaldo(rs.getDouble("saldo"));
                    pou.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                    pou.setUltimaActualizacion(rs.getTimestamp("ultima_actualizacion").toLocalDateTime());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pou;
    }
}
