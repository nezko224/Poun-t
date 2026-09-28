package main;

import exception.AccionNoPermitidaException;
import exception.SaldoInsuficienteException;
import model.*;
import service.PouService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class JuegoConsola {

    private static final long TICK_MS = 5_000L;               // cada 5s pasa un "tiempo" de vida
    private static final long INTERVALO_MONEDA_MS = 60_000L;  // aparece 1 moneda por minuto
    private static final String FIN_INPUT = "\u0000FIN";

    private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private static final PouService service = new PouService();

    private static Pou pou;
    private static long ultimoTick;
    private static long ultimaMoneda;

    public static void main(String[] args) {
        System.out.println("=== Poun't ===");
        iniciarLectorDeInput();

        if (!service.hayConexion()) {
            System.out.println("No se pudo conectar a MySQL. Revisar que XAMPP este prendido, que exista");
            System.out.println("la base y que este el driver mysql-connector-j en el proyecto.");
            return;
        }
        Main.crearTablas();
        service.cargarCatalogoInicial();

        while (true) {
            inputQueue.clear();
            pou = elegirPou();
            if (pou == null) {
                System.out.println("Hasta la proxima!");
                return;
            }
            jugar();
        }
    }

    // ---------- input ----------

    // Un unico hilo lee System.in y vuelca las lineas a una cola. Asi el menu
    // y la pregunta de la moneda leen de la misma cola sin pelearse por System.in.
    private static void iniciarLectorDeInput() {
        Thread lector = new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNextLine()) {
                inputQueue.offer(sc.nextLine());
            }
            inputQueue.offer(FIN_INPUT);
        });
        lector.setDaemon(true);
        lector.start();
    }

    private static String tomarLinea(long ms) {
        try {
            String linea = inputQueue.poll(ms, TimeUnit.MILLISECONDS);
            if (FIN_INPUT.equals(linea)) {
                inputQueue.offer(FIN_INPUT);
            }
            return linea;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FIN_INPUT;
        }
    }

    // Lectura bloqueante (solo para antes de arrancar la partida)
    private static String leer() {
        while (true) {
            String linea = tomarLinea(1000);
            if (linea == null) {
                continue;
            }
            return FIN_INPUT.equals(linea) ? "0" : linea.trim();
        }
    }

    // Espera una linea del jugador pero mientras tanto el juego sigue andando
    // (baja de estadisticas, monedas). Devuelve null si termino la partida.
    private static String esperarLinea(Runnable reimprimir) {
        while (true) {
            boolean huboEventos = procesarTiempo();
            if (pou.getEstado() == Pou.EstadoPou.MUERTO) {
                return null;
            }
            if (huboEventos) {
                reimprimir.run();
            }
            String linea = tomarLinea(500);
            if (linea != null) {
                return FIN_INPUT.equals(linea) ? null : linea.trim();
            }
        }
    }

    private static int numero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---------- antes de jugar ----------

    private static Pou elegirPou() {
        while (true) {
            System.out.println();
            System.out.println("1) Nuevo Pou");
            System.out.println("2) Cargar Pou guardado");
            System.out.println("0) Salir");
            System.out.print("> ");
            String op = leer();

            if (op.equals("1")) {
                System.out.print("Como se llama tu Pou? ");
                String nombre = leer();
                if (nombre.equals("0") || nombre.isBlank()) {
                    nombre = "Rufus";
                }
                return service.nuevoPou(nombre);

            } else if (op.equals("2")) {
                List<Pou> vivos = service.listarPousVivos();
                if (vivos.isEmpty()) {
                    System.out.println("No hay Pous guardados con vida.");
                    continue;
                }
                for (int i = 0; i < vivos.size(); i++) {
                    Pou p = vivos.get(i);
                    System.out.println((i + 1) + ") " + p.getNombre() + " - hambre " + p.getHambre()
                            + ", higiene " + p.getHigiene() + ", sueño " + p.getSueño()
                            + ", monedas " + (int) p.getSaldo());
                }
                System.out.println("0) Volver");
                System.out.print("> ");
                int i = numero(leer()) - 1;
                if (i >= 0 && i < vivos.size()) {
                    return service.cargarPou(vivos.get(i));
                }

            } else if (op.equals("0")) {
                return null;
            } else {
                System.out.println("Opcion invalida.");
            }
        }
    }

    // ---------- partida ----------

    private static void jugar() {
        ultimoTick = System.currentTimeMillis();
        ultimaMoneda = ultimoTick;

        System.out.println();
        System.out.println("Cuida a " + pou.getNombre() + ": si el hambre llega a 0 se muere.");
        System.out.println("Cada tanto aparecen monedas, agarralas para comprar cosas.");

        boolean salir = false;
        while (!salir && pou.getEstado() != Pou.EstadoPou.MUERTO) {
            mostrarMenuPrincipal();
            String op = esperarLinea(JuegoConsola::mostrarMenuPrincipal);
            if (op == null) {
                break;
            }
            salir = procesarMenuPrincipal(op);
        }

        service.guardar(pou);
        long minutos = Duration.between(pou.getFechaCreacion(), LocalDateTime.now()).toMinutes();
        System.out.println();
        if (pou.getEstado() == Pou.EstadoPou.MUERTO) {
            System.out.println("=== GAME OVER ===");
            System.out.println(pou.getNombre() + " vivio " + minutos + " minuto/s y termino con "
                    + (int) pou.getSaldo() + " monedas.");
        } else {
            System.out.println("Partida guardada. " + resumen());
        }
    }

    private static String resumen() {
        return pou.getNombre() + " [" + pou.getEstado() + "] hambre " + pou.getHambre()
                + " | higiene " + pou.getHigiene() + " | sueño " + pou.getSueño()
                + " | monedas " + (int) pou.getSaldo();
    }

    private static String barra(int valor) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < valor / 10 ? '#' : '-');
        }
        return sb.append("] ").append(valor).toString();
    }

    private static void mostrarMenuPrincipal() {
        System.out.println();
        System.out.println(resumen());
        if (pou.getEstado() == Pou.EstadoPou.DORMIDO) {
            System.out.println("Zzz... esta durmiendo.");
            System.out.println("1) Despertar");
        } else {
            System.out.println("1) Cocina");
            System.out.println("2) Bañarlo");
            System.out.println("3) Mandarlo a dormir");
            System.out.println("4) Ropero");
            System.out.println("5) Farmacia");
        }
        System.out.println("6) Ver estado");
        System.out.println("0) Salir y guardar");
        System.out.print("> ");
    }

    private static boolean procesarMenuPrincipal(String op) {
        if (op.equals("0")) {
            return true;
        }
        try {
            if (op.equals("6")) {
                mostrarEstado();
            } else if (pou.getEstado() == Pou.EstadoPou.DORMIDO) {
                if (op.equals("1")) {
                    System.out.println(service.despertar(pou));
                } else {
                    System.out.println("Ahora no se puede, esta durmiendo.");
                }
            } else {
                switch (op) {
                    case "1": cocina(); break;
                    case "2": System.out.println(service.banar(pou)); break;
                    case "3": System.out.println(service.dormir(pou)); break;
                    case "4": ropero(); break;
                    case "5": farmacia(); break;
                    default: System.out.println("Opcion invalida.");
                }
            }
        } catch (SaldoInsuficienteException | AccionNoPermitidaException e) {
            System.out.println("Ojo: " + e.getMessage());
        }
        return false;
    }

    private static void mostrarEstado() {
        System.out.println();
        System.out.println(pou.getNombre() + " - " + pou.getEstado());
        System.out.println("Hambre  " + barra(pou.getHambre()));
        System.out.println("Higiene " + barra(pou.getHigiene()));
        System.out.println("Sueño   " + barra(pou.getSueño()));
        System.out.println("Monedas " + (int) pou.getSaldo());
        StringBuilder ropa = new StringBuilder();
        for (Prenda p : service.listarPrendas(pou)) {
            if (p.isEstaPuesta()) {
                ropa.append(ropa.length() > 0 ? ", " : "").append(p.getNombre());
            }
        }
        System.out.println("Ropa puesta: " + (ropa.length() > 0 ? ropa : "nada"));
    }

    // ---------- submenus ----------

    private static boolean puedeSeguirEnSubmenu() {
        return pou.getEstado() != Pou.EstadoPou.MUERTO && pou.getEstado() != Pou.EstadoPou.DORMIDO;
    }

    private static String describirAlimento(Alimento a) {
        if (a instanceof Fruta) {
            return "hambre +" + a.getValorHambre() + ", higiene +" + ((Fruta) a).getValorHigieneBonus();
        } else if (a instanceof Vegetal) {
            return "hambre +" + (int) (a.getValorHambre() * ((Vegetal) a).getMultiplicadorHambre());
        }
        return "hambre +" + a.getValorHambre() + ", higiene -" + ((ComidaChatarra) a).getValorHigienePenalizacion();
    }

    private static void cocina() {
        List<Alimento> lista = service.listarAlimentos();
        Runnable menu = () -> {
            System.out.println();
            System.out.println("--- COCINA --- (monedas: " + (int) pou.getSaldo() + ")");
            for (int i = 0; i < lista.size(); i++) {
                Alimento a = lista.get(i);
                System.out.println((i + 1) + ") " + a.getNombre() + " - " + (int) a.getPrecio()
                        + " monedas (" + describirAlimento(a) + ")");
            }
            System.out.println("0) Volver");
            System.out.print("> ");
        };

        while (puedeSeguirEnSubmenu()) {
            menu.run();
            String op = esperarLinea(menu);
            if (op == null || op.equals("0")) {
                return;
            }
            int i = numero(op) - 1;
            if (i < 0 || i >= lista.size()) {
                System.out.println("Opcion invalida.");
                continue;
            }
            try {
                System.out.println(service.comprarAlimento(pou, lista.get(i)));
            } catch (SaldoInsuficienteException | AccionNoPermitidaException e) {
                System.out.println("Ojo: " + e.getMessage());
            }
        }
    }

    private static void farmacia() {
        List<Medicamento> lista = service.listarMedicamentos();
        Runnable menu = () -> {
            System.out.println();
            System.out.println("--- FARMACIA --- (monedas: " + (int) pou.getSaldo() + ")");
            for (int i = 0; i < lista.size(); i++) {
                Medicamento m = lista.get(i);
                System.out.println((i + 1) + ") " + m.getNombre() + " - " + (int) m.getPrecio()
                        + " monedas (efectividad " + m.getEfectividad() + "%)");
            }
            System.out.println("0) Volver");
            System.out.print("> ");
        };

        while (puedeSeguirEnSubmenu()) {
            menu.run();
            String op = esperarLinea(menu);
            if (op == null || op.equals("0")) {
                return;
            }
            int i = numero(op) - 1;
            if (i < 0 || i >= lista.size()) {
                System.out.println("Opcion invalida.");
                continue;
            }
            try {
                System.out.println(service.comprarMedicamento(pou, lista.get(i)));
            } catch (SaldoInsuficienteException | AccionNoPermitidaException e) {
                System.out.println("Ojo: " + e.getMessage());
            }
        }
    }

    private static void ropero() {
        Runnable menu = () -> {
            System.out.println();
            System.out.println("--- ROPERO ---");
            System.out.println("1) Comprar prenda");
            System.out.println("2) Mis prendas (poner / sacar)");
            System.out.println("0) Volver");
            System.out.print("> ");
        };

        while (puedeSeguirEnSubmenu()) {
            menu.run();
            String op = esperarLinea(menu);
            if (op == null || op.equals("0")) {
                return;
            }
            if (op.equals("1")) {
                comprarPrenda();
            } else if (op.equals("2")) {
                misPrendas();
            } else {
                System.out.println("Opcion invalida.");
            }
        }
    }

    private static void comprarPrenda() {
        List<Prenda> catalogo = service.catalogoPrendas();
        Runnable menu = () -> {
            System.out.println();
            System.out.println("--- TIENDA DE ROPA --- (monedas: " + (int) pou.getSaldo() + ")");
            for (int i = 0; i < catalogo.size(); i++) {
                Prenda p = catalogo.get(i);
                System.out.println((i + 1) + ") " + p.getNombre() + " - " + (int) p.getPrecio()
                        + " monedas (" + p.getTipo() + ")");
            }
            System.out.println("0) Volver");
            System.out.print("> ");
        };

        while (puedeSeguirEnSubmenu()) {
            menu.run();
            String op = esperarLinea(menu);
            if (op == null || op.equals("0")) {
                return;
            }
            int i = numero(op) - 1;
            if (i < 0 || i >= catalogo.size()) {
                System.out.println("Opcion invalida.");
                continue;
            }
            try {
                System.out.println(service.comprarPrenda(pou, catalogo.get(i)));
            } catch (SaldoInsuficienteException | AccionNoPermitidaException e) {
                System.out.println("Ojo: " + e.getMessage());
            }
        }
    }

    private static void misPrendas() {
        Runnable menu = () -> {
            System.out.println();
            System.out.println("--- MIS PRENDAS ---");
            List<Prenda> mias = service.listarPrendas(pou);
            if (mias.isEmpty()) {
                System.out.println("(todavia no tenes prendas)");
            }
            for (int i = 0; i < mias.size(); i++) {
                Prenda p = mias.get(i);
                System.out.println((i + 1) + ") " + p.getNombre() + " (" + p.getTipo() + ")"
                        + (p.isEstaPuesta() ? " [PUESTA]" : ""));
            }
            System.out.println("0) Volver");
            System.out.print("> ");
        };

        while (puedeSeguirEnSubmenu()) {
            menu.run();
            String op = esperarLinea(menu);
            if (op == null || op.equals("0")) {
                return;
            }
            List<Prenda> mias = service.listarPrendas(pou);
            int i = numero(op) - 1;
            if (i < 0 || i >= mias.size()) {
                System.out.println("Opcion invalida.");
                continue;
            }
            try {
                System.out.println(service.alternarPrenda(pou, mias.get(i)));
            } catch (AccionNoPermitidaException e) {
                System.out.println("Ojo: " + e.getMessage());
            }
        }
    }

    // ---------- reloj del juego y monedas ----------

    // Aplica los ticks que hayan pasado y, si toca, tira una moneda.
    // Devuelve true si imprimio algo (para que se reimprima el menu).
    private static boolean procesarTiempo() {
        boolean huboEventos = false;
        boolean cambio = false;

        while (System.currentTimeMillis() - ultimoTick >= TICK_MS && pou.getEstado() != Pou.EstadoPou.MUERTO) {
            ultimoTick += TICK_MS;
            List<String> avisos = service.pasarTiempo(pou);
            for (String aviso : avisos) {
                System.out.println();
                System.out.println("* " + aviso);
                huboEventos = true;
            }
            cambio = true;
        }
        if (cambio) {
            service.guardar(pou);
        }
        if (pou.getEstado() == Pou.EstadoPou.MUERTO) {
            return huboEventos;
        }

        boolean duerme = pou.getEstado() == Pou.EstadoPou.DORMIDO;
        if (!duerme && System.currentTimeMillis() - ultimaMoneda >= INTERVALO_MONEDA_MS) {
            aparicionDeMoneda();
            ultimaMoneda = System.currentTimeMillis();
            huboEventos = true;
        }
        return huboEventos;
    }

    private static void aparicionDeMoneda() {
        Moneda moneda = service.generarMoneda(pou);
        boolean especial = moneda instanceof MonedaEspecial;
        int segundos = moneda.getTiempoVidaSegundos();

        inputQueue.clear();   // lo que tipeo antes no cuenta como respuesta
        System.out.println();
        System.out.println("Ha aparecido una moneda " + (especial ? "ESPECIAL" : "comun")
                + " (vale " + (int) moneda.getValor() + "). Quieres agarrarla? (Y/N) - tenes "
                + segundos + " segundos...");

        String respuesta = tomarLinea(segundos * 1000L);

        if (respuesta != null && respuesta.trim().equalsIgnoreCase("Y")) {
            service.recolectarMoneda(pou, moneda);
            System.out.println("La agarraste! +" + (int) moneda.getValor() + ". Monedas: " + (int) pou.getSaldo());
        } else {
            System.out.println("La moneda desaparecio.");
        }
    }
}
