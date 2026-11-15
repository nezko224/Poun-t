# Poun't 🟤

Una mascota virtual al estilo **Pou**, hecha en **Java (Swing)** con base de datos **MySQL**.

Tenés que cuidar a tu Pou: darle de comer, bañarlo, dejarlo dormir, curarlo cuando se enferma, vestirlo y juntar monedas para comprarle cosas. Si lo descuidás demasiado... puede morirse.

## ¿De qué se trata?

- **Cuidado de la mascota:** hambre, higiene y sueño bajan con el tiempo y hay que mantenerlos.
- **Cocina:** frutas, vegetales y comida chatarra, cada una con efectos distintos.
- **Baño:** ducha y esponja para mantener la higiene.
- **Farmacia:** medicamentos para cuando el Pou se enferma.
- **Ropero:** prendas y conjuntos para personalizarlo.
- **Monedas:** aparecen en pantalla, se juntan con el mouse y sirven para comprar.
- **Minijuegos:**
  1. **Vóley**
  2. **Snake** (con dificultad fácil / normal / difícil)
  3. **Operación Mugre**: un shooter en primera persona (raycasting propio, sin sangre: los gérmenes se "limpian" con burbujas) con 5 capítulos y jefes.

El proyecto está organizado en capas: `model`, `dao`, `service`, `exception`, `ui` y `main`.

## Requisitos

- **Java JDK 17** o superior
- **Maven** (para descargar el driver de MySQL)
- **MySQL** corriendo en el puerto `3306` (por ejemplo con **XAMPP**)
- Un IDE como Eclipse, IntelliJ o VS Code (opcional, pero recomendado)

## Cómo ejecutarlo

### 1. Encender MySQL

Abrí XAMPP y arrancá el módulo **MySQL**.

### 2. Crear la base de datos

La base se tiene que llamar **`pou`**. Hay dos formas:

- **phpMyAdmin:** entrá a la pestaña **SQL** y ejecutá:
  ```sql
  CREATE DATABASE IF NOT EXISTS pou;
  ```
- **O bien** importá el script `crear_tablas.sql` incluido en el proyecto (crea la base y todas las tablas).

> Las tablas también se crean solas al iniciar el juego, y el catálogo inicial (comidas y medicamentos) se carga automáticamente la primera vez. Lo único que **tenés que crear a mano es la base `pou`**.

### 3. Revisar la conexión

La configuración está en `pou/src/main/java/dao/ConexionBD.java`:

```java
URL      = "jdbc:mysql://localhost:3306/pou?useSSL=false&serverTimezone=UTC"
USUARIO  = "root"
PASSWORD = ""
```

Si tu MySQL usa otro puerto, usuario o contraseña, cambialo ahí.

### 4. Descargar dependencias

Desde la carpeta `pou/`:

```bash
mvn clean compile
```

Esto baja el driver `mysql-connector-j 8.3.0` definido en el `pom.xml`.

### 5. Ejecutar el juego

Ejecutá la clase **`main.JuegoGrafico`** (es la que tiene la ventana).

- **Desde el IDE:** click derecho sobre `JuegoGrafico.java` → *Run as Java Application*.
- **Desde la terminal** (dentro de `pou/`, con el driver ya descargado por Maven):
  ```bash
  mvn dependency:copy-dependencies
  java -cp "target/classes:target/dependency/*" main.JuegoGrafico
  ```
  En Windows, el separador del classpath es `;` en vez de `:`:
  ```bash
  java -cp "target/classes;target/dependency/*" main.JuegoGrafico
  ```

> También existe `main.JuegoConsola` (versión por consola) y `main.Main` (pruebas del modelo y del DAO), pero la experiencia completa es con `JuegoGrafico`.

## Controles

### Casa

| Acción | Control |
|---|---|
| Caminar | `A` / `D` o flechas |
| Saltar | `Espacio` |
| Sala / Cocina / Baño | `H` / `C` / `B` |
| Dormir / Despertar | `S` |
| Ropero / Farmacia | `R` / `F` |
| Jugar (minijuegos) | `J` |
| Ver estado | `E` |
| Super Pou | `T` |
| Agarrar moneda | `Y` |
| Elegir ítem (cocina, farmacia, ropero) | `1` – `9` |
| Ducha (en el baño) | `Enter` |
| Silenciar | `M` |
| Volver | `Esc` |

**Mouse:** acariciar al Pou pasando el mouse, tocarlo, agarrarlo y tirarlo, doble click para una voltereta, arrastrar comida o remedios hasta su boca, click en las monedas y arrastrar la esponja en el baño.

### Snake

Flechas o `WASD` para moverse, `P` pausa, `Esc` volver. Si llegás a la meta ganás 2 monedas.

### Operación Mugre (shooter)

| Acción | Control |
|---|---|
| Moverse | `W A S D` |
| Girar cámara | Flechas izq/der o mouse (`Tab` activa/desactiva el mouse) |
| Disparar | `Espacio` / `Ctrl` / click |
| Usar puertas, válvulas y cadenas | `E` o `Enter` (en las válvulas hay que **mantener** `E`) |
| Correr | `Shift` |
| Pausa | `P` |
| Sonido | `M` |
| Salir | `Esc` |

Los capítulos se desbloquean de a uno. El progreso y los rangos se guardan localmente con `java.util.prefs` (no usan la base de datos).

| # | Capítulo | Jefe |
|---|---|---|
| 1 | La Casa Invadida | Rey Mugre |
| 2 | La Cocina Caótica | Chef Grasón |
| 3 | El Desván Polvoriento | Madame Telaraña |
| 4 | El Sótano Húmedo | Capitán Moho |
| 5 | El Gran Desagüe | La Mancha Madre |

Si sacás rango **S** en los cinco capítulos, desbloqueás el *traje dorado*.

## Problemas comunes

- **"No se pudo conectar a MySQL":** revisá que XAMPP esté prendido, que la base `pou` exista y que el puerto sea el `3306` (o el que hayas configurado en `ConexionBD.java`).
- **Error de driver / `ClassNotFoundException`:** corré `mvn clean compile` para que Maven descargue `mysql-connector-j`.
- **Versión de Java:** el proyecto compila con Java 17; con una versión anterior no va a andar.

## Estructura del proyecto

```
pou/
├── pom.xml
└── src/main/java/
    ├── model/       # Pou, Alimento, Prenda, Medicamento, Moneda...
    ├── dao/         # acceso a la base de datos (+ impl/)
    ├── service/     # lógica del juego (PouService)
    ├── exception/   # excepciones propias
    ├── ui/          # ventana, dibujos, sonido y minijuegos
    └── main/        # JuegoGrafico (arranque), JuegoConsola, Main
crear_tablas.sql
```
