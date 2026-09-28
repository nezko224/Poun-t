package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {


    private static final String URL = "jdbc:mysql://localhost:3307/pou?useSSL=false&serverTimezone=UTC";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "";

    public static Connection conexionBd() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
}
