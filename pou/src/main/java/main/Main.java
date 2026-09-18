package main;

import dao.ConexionBD;
import dao.PouDao;
import dao.impl.PouDaoImpl;
import model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        crearTablas();

        System.out.println("=== Probando la jerarquia del modelo con Interactuable ===");
        probarModelo();

        System.out.println();
        System.out.println("=== Probando el DAO de Pou (insertar + leer) ===");
        probarDaoPou();
    }

    private static void crearTablas() {
        String[] sentencias = {
                "CREATE TABLE IF NOT EXISTS pou (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "nombre VARCHAR(50) NOT NULL," +
                        "hambre INT NOT NULL DEFAULT 100," +
                        "higiene INT NOT NULL DEFAULT 100," +
                        "sueño INT NOT NULL DEFAULT 100," +
                        "estado ENUM('SANO','ENFERMO','DORMIDO','MUERTO') NOT NULL DEFAULT 'SANO'," +
                        "saldo DECIMAL(10,2) NOT NULL DEFAULT 0," +
                        "fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "ultima_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)",

                "CREATE TABLE IF NOT EXISTS alimento (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "tipo ENUM('FRUTA','VEGETAL','CHATARRA') NOT NULL," +
                        "nombre VARCHAR(50) NOT NULL," +
                        "precio DECIMAL(10,2) NOT NULL," +
                        "valor_hambre INT NOT NULL," +
                        "valor_higiene_bonus INT NULL," +
                        "multiplicador_hambre DECIMAL(4,2) NULL," +
                        "valor_higiene_penalizacion INT NULL)",

                "CREATE TABLE IF NOT EXISTS prenda (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "nombre VARCHAR(50) NOT NULL," +
                        "conjunto ENUM('CONJUNTO_1','CONJUNTO_2','CONJUNTO_3') NOT NULL," +
                        "precio DECIMAL(10,2) NOT NULL," +
                        "esta_puesta BOOLEAN NOT NULL DEFAULT FALSE," +
                        "id_pou INT NOT NULL," +
                        "FOREIGN KEY (id_pou) REFERENCES pou(id))",

                "CREATE TABLE IF NOT EXISTS medicamento (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "nombre VARCHAR(50) NOT NULL," +
                        "precio DECIMAL(10,2) NOT NULL," +
                        "efectividad INT NOT NULL)",

                "CREATE TABLE IF NOT EXISTS moneda (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "tipo ENUM('COMUN','ESPECIAL') NOT NULL," +
                        "valor DECIMAL(10,2) NOT NULL," +
                        "posicion_x INT NOT NULL," +
                        "posicion_y INT NOT NULL," +
                        "tiempo_aparicion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "tiempo_vida_segundos INT NOT NULL," +
                        "recolectada BOOLEAN NOT NULL DEFAULT FALSE," +
                        "probabilidad_aparicion DECIMAL(4,3) NULL," +
                        "id_pou INT NOT NULL," +
                        "FOREIGN KEY (id_pou) REFERENCES pou(id))"
        };

        try (Connection con = ConexionBD.conexionBd();
             Statement st = con.createStatement()) {

            for (String sql : sentencias) {
                st.executeUpdate(sql);
            }
            System.out.println("Tablas listas.");

        } catch (SQLException e) {
            System.out.println("No se pudo conectar/crear las tablas. Revisar que XAMPP este");
            System.out.println("prendido y que exista la base pount_db en phpMyAdmin.");
            e.printStackTrace();
        }
    }

    private static void probarModelo() {
        Pou pou = new Pou("Rufus");
        pou.setHambre(50);
        pou.setHigiene(50);
        System.out.println("Antes: " + pou);

        List<Interactuable> cosasParaUsar = new ArrayList<>();
        cosasParaUsar.add(new Fruta("Manzana", 100.0, 10, 5));
        cosasParaUsar.add(new Vegetal("Zanahoria", 80.0, 10, 1.5));
        cosasParaUsar.add(new ComidaChatarra("Papas fritas", 60.0, 20, 8));
        cosasParaUsar.add(new Prenda("Gorra", Prenda.TipoConjunto.CONJUNTO_1, 150.0));

        for (Interactuable cosa : cosasParaUsar) {
            cosa.aplicarEfecto(pou);
            System.out.println("Despues de " + cosa.getClass().getSimpleName() + ": " + pou);
        }

        MonedaComun mc = new MonedaComun(10, 20);
        MonedaEspecial me = new MonedaEspecial(30, 40, 0.1);
        System.out.println("Genero " + mc.getClass().getSimpleName() + " valor=" + mc.getValor());
        System.out.println("Genero " + me.getClass().getSimpleName() + " valor=" + me.getValor());
    }

    private static void probarDaoPou() {
        PouDao pouDao = new PouDaoImpl();

        Pou nuevoPou = new Pou("Pochoclo");
        pouDao.crear(nuevoPou);
        System.out.println("Se guardo con id: " + nuevoPou.getId());

        Pou pouRecuperado = pouDao.listarPorId(nuevoPou.getId());
        if (pouRecuperado != null) {
            System.out.println("Recuperado de la base: " + pouRecuperado);
        } else {
            System.out.println("No se pudo recuperar el Pou, revisar la conexion.");
        }
    }
}
