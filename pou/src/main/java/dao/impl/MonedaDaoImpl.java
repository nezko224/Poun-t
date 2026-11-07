package dao.impl;

import dao.ConexionBD;
import dao.MonedaDao;
import model.Moneda;
import model.MonedaEspecial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

public class MonedaDaoImpl implements MonedaDao {

    @Override
    public void crear(Moneda moneda, int idPou) {
        String sql = "INSERT INTO moneda (tipo, valor, posicion_x, posicion_y, tiempo_aparicion, "
                + "tiempo_vida_segundos, recolectada, probabilidad_aparicion, id_pou) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (moneda instanceof MonedaEspecial) {
                ps.setString(1, "ESPECIAL");
                ps.setDouble(8, ((MonedaEspecial) moneda).getProbabilidadAparicion());
            } else {
                ps.setString(1, "COMUN");
                ps.setNull(8, Types.DECIMAL);
            }
            ps.setDouble(2, moneda.getValor());
            ps.setInt(3, moneda.getPosicionX());
            ps.setInt(4, moneda.getPosicionY());
            ps.setTimestamp(5, Timestamp.valueOf(moneda.getTiempoAparicion()));
            ps.setInt(6, moneda.getTiempoVidaSegundos());
            ps.setBoolean(7, moneda.isRecolectada());
            ps.setInt(9, idPou);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    moneda.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void marcarRecolectada(int id) {
        String sql = "UPDATE moneda SET recolectada = TRUE WHERE id = ?";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
