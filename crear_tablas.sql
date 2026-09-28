-- Poun't: base de datos y tablas
-- Correr en phpMyAdmin (pestaña SQL) o con: mysql -uroot -P3307 < crear_tablas.sql

CREATE DATABASE IF NOT EXISTS pou;
USE pou;

CREATE TABLE IF NOT EXISTS pou (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    hambre INT NOT NULL DEFAULT 100,
    higiene INT NOT NULL DEFAULT 100,
    sueño INT NOT NULL DEFAULT 100,
    estado ENUM('SANO', 'ENFERMO', 'DORMIDO', 'MUERTO') NOT NULL DEFAULT 'SANO',
    saldo DECIMAL(10,2) NOT NULL DEFAULT 0,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- tabla única para Fruta / Vegetal / ComidaChatarra, la columna "tipo" discrimina
CREATE TABLE IF NOT EXISTS alimento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo ENUM('FRUTA', 'VEGETAL', 'CHATARRA') NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    valor_hambre INT NOT NULL,
    valor_higiene_bonus INT NULL,
    multiplicador_hambre DECIMAL(4,2) NULL,
    valor_higiene_penalizacion INT NULL
);

CREATE TABLE IF NOT EXISTS prenda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    conjunto ENUM('CONJUNTO_1', 'CONJUNTO_2', 'CONJUNTO_3') NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    esta_puesta BOOLEAN NOT NULL DEFAULT FALSE,
    id_pou INT NOT NULL,
    FOREIGN KEY (id_pou) REFERENCES pou(id)
);

CREATE TABLE IF NOT EXISTS medicamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    efectividad INT NOT NULL
);

-- tabla única para MonedaComun / MonedaEspecial, la columna "tipo" discrimina
CREATE TABLE IF NOT EXISTS moneda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo ENUM('COMUN', 'ESPECIAL') NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    posicion_x INT NOT NULL,
    posicion_y INT NOT NULL,
    tiempo_aparicion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tiempo_vida_segundos INT NOT NULL,
    recolectada BOOLEAN NOT NULL DEFAULT FALSE,
    probabilidad_aparicion DECIMAL(4,3) NULL,
    id_pou INT NOT NULL,
    FOREIGN KEY (id_pou) REFERENCES pou(id)
);

-- OPCIONAL: catálogo inicial. El juego ya lo carga solo la primera vez si
-- las tablas están vacías, así que NO hace falta correr esto (y si lo corrés
-- dos veces se duplican los productos).
--
-- INSERT INTO alimento (tipo, nombre, precio, valor_hambre, valor_higiene_bonus, multiplicador_hambre, valor_higiene_penalizacion) VALUES
--   ('FRUTA', 'Manzana', 2, 10, 5, NULL, NULL),
--   ('FRUTA', 'Banana', 3, 15, 3, NULL, NULL),
--   ('VEGETAL', 'Zanahoria', 3, 10, NULL, 2.0, NULL),
--   ('VEGETAL', 'Brocoli', 4, 12, NULL, 2.5, NULL),
--   ('CHATARRA', 'Papas fritas', 1, 20, NULL, NULL, 8),
--   ('CHATARRA', 'Hamburguesa', 2, 30, NULL, NULL, 12);
-- INSERT INTO medicamento (nombre, precio, efectividad) VALUES
--   ('Jarabe', 6, 50),
--   ('Pastilla', 10, 100);
