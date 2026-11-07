package dao.impl;

import dao.AlimentoDao;
import dao.ConexionBD;
import model.Alimento;
import model.ComidaChatarra;
import model.Fruta;
import model.Vegetal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AlimentoDaoImpl implements AlimentoDao {

    @Override
    public void crear(Alimento alimento) {
        String sql = "INSERT INTO alimento (tipo, nombre, precio, valor_hambre, valor_higiene_bonus, "
                + "multiplicador_hambre, valor_higiene_penalizacion) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(2, alimento.getNombre());
            ps.setDouble(3, alimento.getPrecio());
            ps.setInt(4, alimento.getValorHambre());
            ps.setNull(5, Types.INTEGER);
            ps.setNull(6, Types.DECIMAL);
            ps.setNull(7, Types.INTEGER);

            if (alimento instanceof Fruta) {
                ps.setString(1, "FRUTA");
                ps.setInt(5, ((Fruta) alimento).getValorHigieneBonus());
            } else if (alimento instanceof Vegetal) {
                ps.setString(1, "VEGETAL");
                ps.setDouble(6, ((Vegetal) alimento).getMultiplicadorHambre());
            } else {
                ps.setString(1, "CHATARRA");
                ps.setInt(7, ((ComidaChatarra) alimento).getValorHigienePenalizacion());
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    alimento.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Alimento> listarTodo() {
        String sql = "SELECT * FROM alimento ORDER BY id";
        List<Alimento> lista = new ArrayList<>();

        try (Connection con = ConexionBD.conexionBd();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String tipo = rs.getString("tipo");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                int valorHambre = rs.getInt("valor_hambre");

                Alimento alimento;
                if (tipo.equals("FRUTA")) {
                    alimento = new Fruta(nombre, precio, valorHambre, rs.getInt("valor_higiene_bonus"));
                } else if (tipo.equals("VEGETAL")) {
                    alimento = new Vegetal(nombre, precio, valorHambre, rs.getDouble("multiplicador_hambre"));
                } else {
                    alimento = new ComidaChatarra(nombre, precio, valorHambre, rs.getInt("valor_higiene_penalizacion"));
                }
                alimento.setId(rs.getInt("id"));
                lista.add(alimento);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
