package main;

import dao.PouDao;
import dao.impl.PouDaoImpl;
import model.*;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class JuegoConsola {

    private static final long INTERVALO_MONEDA_MS = 60_000L; 
    private static final double PROBABILIDAD_ESPECIAL = 0.30; 

    private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Poun't - Juego de consola ===");
        iniciarLectorDeInput();

        System.out.print("Como se llama tu Pou? ");
        String nombre = inputQueue.take();
        Pou pou = new Pou(nombre.isBlank() ? "Rufus" : nombre.trim());

        PouDao pouDao = new PouDaoImpl();
        pouDao.crear(pou);
        System.out.println("Pou guardado en la base con id " + pou.getId());

        mostrarMenu();
        long ultimaAparicion = System.currentTimeMillis();
        boolean corriendo = true;

        while (corriendo && pou.getEstado() != Pou.EstadoPou.MUERTO) {

            if (System.currentTimeMillis() - ultimaAparicion >= INTERVALO_MONEDA_MS) {
                aparicionDeMoneda(pou);
                ultimaAparicion = System.currentTimeMillis();
                mostrarMenu();
            }

            String linea = inputQueue.poll(1, TimeUnit.SECONDS);
            if (linea == null) {
                continue;
            }

            corriendo = procesarOpcion(linea.trim(), pou);
        }

        System.out.println("=== Fin del juego === " + pou);
    }

    private static void iniciarLectorDeInput() {
        Thread lector = new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNextLine()) {
                inputQueue.offer(sc.nextLine());
            }
        });
        lector.setDaemon(true);
        lector.start();
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("--- Que queres hacer? ---");
        System.out.println("1) Darle una fruta");
        System.out.println("2) Darle un vegetal");
        System.out.println("3) Darle comida chatarra");
        System.out.println("4) Ponerle una prenda");
        System.out.println("5) Darle un medicamento");
        System.out.println("6) Ver estado");
        System.out.println("0) Salir");
        System.out.print("> ");
    }

    private static boolean procesarOpcion(String opcion, Pou pou) {
        Interactuable cosa = null;

        switch (opcion) {
            case "1":
                cosa = new Fruta("Manzana", 100.0, 10, 5);
                break;
            case "2":
                cosa = new Vegetal("Zanahoria", 80.0, 10, 1.5);
                break;
            case "3":
                cosa = new ComidaChatarra("Papas fritas", 60.0, 20, 8);
                break;
            case "4":
                cosa = new Prenda("Gorra", Prenda.TipoConjunto.CONJUNTO_1, 150.0);
                break;
            case "5":
                cosa = new Medicamento("Pastilla", 50.0, 100);
                break;
            case "6":
                System.out.println("Estado actual: " + pou);
                mostrarMenu();
                return true;
            case "0":
                return false;
            default:
                System.out.println("Opcion invalida.");
                mostrarMenu();
                return true;
        }

        cosa.aplicarEfecto(pou);
        System.out.println("Listo. " + pou);
        mostrarMenu();
        return true;
    }

    private static void aparicionDeMoneda(Pou pou) throws InterruptedException {
        int x = ThreadLocalRandom.current().nextInt(0, 100);
        int y = ThreadLocalRandom.current().nextInt(0, 100);

        Moneda moneda;
        String tipoTexto;
        if (ThreadLocalRandom.current().nextDouble() < PROBABILIDAD_ESPECIAL) {
            moneda = new MonedaEspecial(x, y, PROBABILIDAD_ESPECIAL);
            tipoTexto = "ESPECIAL";
        } else {
            moneda = new MonedaComun(x, y);
            tipoTexto = "comun";
        }

        System.out.println();
        System.out.println("Ha aparecido una moneda " + tipoTexto + " (vale "
                + (int) moneda.getValor() + " moneda/s). Quieres agarrarla? (Y/N) - tenes "
                + moneda.getTiempoVidaSegundos() + " segundos para responder...");

        String respuesta = inputQueue.poll(moneda.getTiempoVidaSegundos(), TimeUnit.SECONDS);

        if (respuesta != null && respuesta.trim().equalsIgnoreCase("Y")) {
            moneda.setRecolectada(true);
            pou.setSaldo(pou.getSaldo() + moneda.getValor());
            System.out.println("¡La agarraste! Saldo actual: " + pou.getSaldo());
        } else {
            System.out.println("La moneda desaparecio.");
        }
    }
}
