package ui;

import exception.AccionNoPermitidaException;
import exception.SaldoInsuficienteException;
import model.*;
import service.PouService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal del juego (reemplaza al menu de consola).
 * Usa PouService tal cual: todas las reglas siguen siendo las originales.
 */
public class PanelJuego extends JPanel {

    // ---- constantes (las de tiempo son las mismas que JuegoConsola)
    private static final long TICK_MS = 5_000L;
    private static final long INTERVALO_MONEDA_MS = 60_000L;
    static final int W = 960, H = 640, HUD_H = 76, NAV_Y = 560, SUELO = 478;
    private static final double ESC = 0.95, VEL = 250, GRAV = 1900;
    private static final double SUPER_SEG = 14;

    private enum Escena { SALA, COCINA, BANO, ROPERO, FARMACIA }
    private static final int INFO = 0, ALERTA = 1, ERROR = 2;
    private static final String[] NAV_TXT = {"Sala", "Cocina", "Baño", "Dormir", "Ropero", "Farmacia", "Jugar", "Estado", "Salir"};
    private static final String[] NAV_TECLA = {"H", "C", "B", "S", "R", "F", "J", "E", "Esc"};

    private static final class Toast { String txt; int tipo; double vida = 4.5; }
    private static final class MonedaViva { Moneda m; double x, y, vida, max, giro, aparece; }
    private static final class Volador { String nombre; int tipo; boolean medicina; double x0, y0, t; }
    private static final class Retorno { int tipo, idx; double x0, y0, t; }
    private static final class Arrastre { int tipo, idx; double x, y, sx, sy; }

    private final Navegador nav;
    private final PouService service;
    private final Pou pou;
    private final Particulas fx = new Particulas();
    private final javax.swing.Timer timer;
    private long ultimoNanos;

    // reloj del juego
    private long ultimoTick, ultimaMoneda;
    private MonedaViva moneda;

    // catalogos
    private List<Alimento> alimentos;
    private List<Medicamento> medicamentos;
    private List<Prenda> tienda;
    private List<Prenda> mias = new ArrayList<>();
    private PouArte.Ropa ropa = new PouArte.Ropa();

    private Escena escena = Escena.SALA;
    private int tabRopero = 0;

    // cinematica del Pou
    private double px = 400, alt = 0, vx = 0, vy = 0, objetivoX = Double.NaN;
    private boolean sujetado, izq, der;
    private double offX, offY, mvx, mvy;
    private double sq = 0, sqVel = 0;
    private final PouArte.Pose pose = new PouArte.Pose();
    private Pou.EstadoPou estadoPrevio;

    // temporizadores de animacion
    private double t, comerT, risaT, sorpresaT, rechazoT, tristeT, bostezoT, despertarT, duchaT, muertoT,
            volteoT, parpadeoT, proxParpadeo = 2, caricia, cariciaCd, rumbleT, proxRumble = 3, proxBostezo = 6,
            proxToser = 4, proxZzz = 1, proxNota = 0, hop, noche, manta, flashT, sleepyBase, nivelAgua, superMix;
    private double miraX, miraY;
    private double dispH, dispI, dispS, dispSaldo;
    private int ultH, ultI, ultS;
    private double ultSaldo;
    private double carino = 0, superT = 0;
    private boolean gameOverVisible, estadoVisible, finGuardado;

    // interaccion
    private double mouseX = 480, mouseY = 300;
    private long presionT;
    private double presionX, presionY;
    private boolean presionSobrePou;
    private Arrastre arr;
    private final List<Volador> voladores = new ArrayList<>();
    private final List<Retorno> retornos = new ArrayList<>();
    private final List<Toast> toasts = new ArrayList<>();
    private int hoverNav = -1;
    private final double[] navPulso = new double[9];

    public PanelJuego(Navegador nav, PouService service, Pou pou) {
        this.nav = nav;
        this.service = service;
        this.pou = pou;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);

        alimentos = service.listarAlimentos();
        medicamentos = service.listarMedicamentos();
        refrescarRopa();

        estadoPrevio = pou.getEstado();
        dispH = ultH = pou.getHambre();
        dispI = ultI = pou.getHigiene();
        dispS = ultS = pou.getSueño();
        dispSaldo = ultSaldo = pou.getSaldo();
        ultimoTick = ultimaMoneda = System.currentTimeMillis();
        if (pou.getEstado() == Pou.EstadoPou.MUERTO) muertoT = 99;

        instalarEventos();
        ultimoNanos = System.nanoTime();
        timer = new javax.swing.Timer(16, e -> {
            long ahora = System.nanoTime();
            double dt = Math.min(0.05, (ahora - ultimoNanos) / 1e9);
            ultimoNanos = ahora;
            actualizar(dt);
            repaint();
        });
        timer.start();
        toast("Cuidá a " + pou.getNombre() + ": si no lo curás a tiempo, se muere.", INFO);
        toast("Agarrá las monedas con el mouse para comprar cosas.", INFO);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    // ===================================================================== ciclo de vida

    public void cerrar() {
        timer.stop();
        service.guardar(pou);
    }

    public void pausar() {
        timer.stop();
        fx.limpiar();
        voladores.clear();
        retornos.clear();
        arr = null;
        moneda = null;
        izq = der = false;
    }

    public void reanudar(int puntos, boolean gano) {
        long ahora = System.currentTimeMillis();
        ultimoTick = ahora;
        ultimaMoneda = ahora;
        ultimoNanos = System.nanoTime();
        izq = der = false;
        px = 480; alt = 0; vx = vy = 0;
        if (puntos > 0) ganarCarino(Math.min(0.6, puntos * 0.06 + (gano ? 0.25 : 0)));
        if (gano) {
            risaT = 2.5;
            toast("¡Ganaste al vóley! " + pou.getNombre() + " está chocho.", ALERTA);
        } else {
            toast("Buen partido. ¡Revancha cuando quieras!", INFO);
        }
        timer.start();
        requestFocusInWindow();
    }

    public PouArte.Ropa ropaPuesta() { return ropa; }

    private String resumen() {
        return pou.getNombre() + " [" + pou.getEstado() + "] hambre " + pou.getHambre()
                + " | higiene " + pou.getHigiene() + " | sueño " + pou.getSueño()
                + " | monedas " + (int) pou.getSaldo();
    }

    // ===================================================================== utilidades

    private void toast(String txt, int tipo) {
        Toast tt = new Toast();
        tt.txt = txt; tt.tipo = tipo;
        tt.vida = tipo == INFO ? 4.5 : 6;
        toasts.add(tt);
        while (toasts.size() > 4) toasts.remove(0);
    }

    private void refrescarRopa() {
        mias = service.listarPrendas(pou);
        PouArte.Ropa r = new PouArte.Ropa();
        for (Prenda p : mias) {
            if (!p.isEstaPuesta()) continue;
            String n = p.getNombre().toLowerCase();
            if (n.contains("gorra")) r.gorra = true;
            else if (n.contains("remera")) r.remera = true;
            else if (n.contains("zapatilla")) r.zapatillas = true;
            else if (n.contains("anteojo")) r.anteojos = true;
        }
        ropa = r;
    }

    private boolean muerto() { return pou.getEstado() == Pou.EstadoPou.MUERTO; }
    private boolean duerme() { return pou.getEstado() == Pou.EstadoPou.DORMIDO; }
    private boolean enfermo() { return pou.getEstado() == Pou.EstadoPou.ENFERMO; }

    private double ox() { return px + (enfermo() && !muerto() ? Math.sin(t * 45) * 1.6 : 0); }
    private double oy() { return SUELO - alt - hop; }

    private Point2D boca() { return PouArte.aPantalla(ox(), oy(), ESC, pose, 0, -58); }

    private boolean sobrePou(double mx, double my) {
        return PouArte.contiene(ox(), oy(), ESC, pose, mx, my);
    }

    private double minX() {
        switch (escena) {
            case COCINA: return 215;
            case BANO: return 345;
            case ROPERO: return 160;
            case FARMACIA: return 170;
            default: return 110;
        }
    }

    private double maxX() {
        switch (escena) {
            case COCINA: return 600;
            case BANO: return 555;
            case ROPERO: return 520;
            case FARMACIA: return 590;
            default: return 840;
        }
    }

    private void ganarCarino(double v) {
        if (superT > 0 || muerto() || duerme() || enfermo()) return;
        boolean antes = carino >= 1;
        carino = Math.min(1, carino + v);
        if (!antes && carino >= 1) {
            toast("¡Cariño al máximo! Presioná T (o tocá la barra rosa) para transformar a " + pou.getNombre() + " en SUPER POU.", ALERTA);
            Sonido.curar();
        }
    }

    private void transformar() {
        if (superT > 0) return;
        if (carino < 1 || pou.getEstado() != Pou.EstadoPou.SANO) {
            toast("Ojo: para transformarse necesita el cariño al máximo y estar sano y despierto.", ERROR);
            return;
        }
        superT = SUPER_SEG;
        flashT = 1;
        Sonido.magia();
        fx.estrellas(ox(), oy() - 80, 40);
        toast("¡SUPER POU! Volá con el mouse y hacé click para lanzar estrellas.", ALERTA);
    }

    private void cambiarEscena(Escena e) {
        if (e == escena) return;
        escena = e;
        arr = null;
        voladores.clear();
        retornos.clear();
        if (e == Escena.BANO) objetivoX = 450;
        else if (px < minX() || px > maxX()) objetivoX = Math.max(minX(), Math.min(maxX(), px));
        if (e == Escena.COCINA) alimentos = service.listarAlimentos();
        if (e == Escena.FARMACIA) medicamentos = service.listarMedicamentos();
        if (e == Escena.ROPERO) { tienda = service.catalogoPrendas(); refrescarRopa(); tabRopero = 0; }
        Sonido.pop();
    }

    // ===================================================================== actualizacion

    private void actualizar(double dt) {
        t += dt;
        relojDelJuego();
        detectarCambios();
        animarBarras(dt);
        actualizarPou(dt);
        actualizarMoneda(dt);
        actualizarVoladores(dt);
        ambiente(dt);
        fx.actualizar(dt);
        for (int i = toasts.size() - 1; i >= 0; i--) {
            toasts.get(i).vida -= dt;
            if (toasts.get(i).vida <= 0) toasts.remove(i);
        }
        for (int i = 0; i < navPulso.length; i++) {
            double objetivo = (i == hoverNav) ? 1 : 0;
            navPulso[i] += (objetivo - navPulso[i]) * Math.min(1, dt * 14);
        }
        flashT = Math.max(0, flashT - dt * 1.6);
    }

    /** Equivale a procesarTiempo() de JuegoConsola. */
    private void relojDelJuego() {
        if (muerto()) return;
        long ahora = System.currentTimeMillis();
        boolean cambio = false;
        while (ahora - ultimoTick >= TICK_MS && !muerto()) {
            ultimoTick += TICK_MS;
            for (String aviso : service.pasarTiempo(pou)) {
                toast(aviso, ALERTA);
            }
            cambio = true;
        }
        if (cambio) service.guardar(pou);
        if (muerto()) return;
        if (!duerme() && moneda == null && ahora - ultimaMoneda >= INTERVALO_MONEDA_MS) {
            aparecerMoneda();
        }
    }

    private void aparecerMoneda() {
        Moneda m = service.generarMoneda(pou);
        MonedaViva mv = new MonedaViva();
        mv.m = m;
        mv.x = 90 + m.getPosicionX() / 100.0 * 780;
        mv.y = 150 + m.getPosicionY() / 100.0 * 250;
        mv.vida = mv.max = m.getTiempoVidaSegundos();
        moneda = mv;
        boolean esp = m instanceof MonedaEspecial;
        toast("Ha aparecido una moneda " + (esp ? "ESPECIAL" : "común") + " (vale " + (int) m.getValor()
                + "). ¡Agarrala con un click (o tecla Y)! Tenés " + m.getTiempoVidaSegundos() + " segundos...", ALERTA);
        Sonido.pop();
        fx.brillos(mv.x, mv.y, 14, esp ? new Color(0x7FE9FF) : new Color(0xFFE066));
    }

    private void recolectarMoneda() {
        if (moneda == null) return;
        Moneda m = moneda.m;
        service.recolectarMoneda(pou, m);
        fx.brillos(moneda.x, moneda.y, 24, new Color(0xFFE066));
        fx.estrellas(moneda.x, moneda.y, 6);
        fx.texto(moneda.x - 14, moneda.y - 24, "+" + (int) m.getValor(), new Color(0xFFD23F));
        toast("¡La agarraste! +" + (int) m.getValor() + ". Monedas: " + (int) pou.getSaldo(), INFO);
        Sonido.moneda();
        risaT = Math.max(risaT, 0.7);
        ganarCarino(0.03);
        moneda = null;
        ultimaMoneda = System.currentTimeMillis();
    }

    private void actualizarMoneda(double dt) {
        if (moneda == null) return;
        moneda.vida -= dt;
        moneda.giro += dt * 5;
        moneda.aparece = Math.min(1, moneda.aparece + dt * 4);
        if (moneda.vida <= 0 || muerto()) {
            toast("La moneda desapareció.", INFO);
            fx.polvo(moneda.x, moneda.y, 8);
            moneda = null;
            ultimaMoneda = System.currentTimeMillis();
        }
    }

    private void detectarCambios() {
        Pou.EstadoPou e = pou.getEstado();
        if (e != estadoPrevio) {
            Pou.EstadoPou antes = estadoPrevio;
            estadoPrevio = e;
            if (e == Pou.EstadoPou.ENFERMO) {
                sorpresaT = 1.2; Sonido.error(); superT = 0;
                fx.sudor(ox(), oy() - 150);
            }
            if (e == Pou.EstadoPou.DORMIDO) {
                escena = Escena.SALA; arr = null; superT = 0; carino = Math.min(carino, 0.99);
                if (superMix > 0) superMix = 0;
            }
            if (antes == Pou.EstadoPou.DORMIDO && e == Pou.EstadoPou.SANO) {
                despertarT = 1.6; bostezoT = 1.4; sqVel += 6;
            }
            if (antes == Pou.EstadoPou.ENFERMO && e == Pou.EstadoPou.SANO) {
                fx.brillos(ox(), oy() - 90, 30, new Color(0x9DFFB0));
                fx.corazones(ox(), oy() - 150, 6);
                risaT = 1.8; vy = 650; Sonido.curar();
            }
            if (e == Pou.EstadoPou.MUERTO) {
                muertoT = 0; Sonido.perder(); superT = 0; moneda = null;
                if (!finGuardado) { service.guardar(pou); finGuardado = true; }
            }
        }
        // textos flotantes de cambios grandes
        int h = pou.getHambre(), i = pou.getHigiene(), s = pou.getSueño();
        double ex = ox(), ey = oy() - 190;
        if (Math.abs(h - ultH) >= 5) fx.texto(ex - 70, ey, (h > ultH ? "+" : "") + (h - ultH) + " hambre", h > ultH ? new Color(0xFFB347) : new Color(0xFF6B6B));
        if (Math.abs(i - ultI) >= 5) fx.texto(ex + 10, ey - 10, (i > ultI ? "+" : "") + (i - ultI) + " higiene", i > ultI ? new Color(0x6BD6FF) : new Color(0xB08968));
        if (Math.abs(s - ultS) >= 5 && !duerme()) fx.texto(ex - 20, ey - 24, (s > ultS ? "+" : "") + (s - ultS) + " sueño", new Color(0xC3B1FF));
        ultH = h; ultI = i; ultS = s;
        double sd = pou.getSaldo();
        if (Math.abs(sd - ultSaldo) >= 1 && moneda == null || (sd < ultSaldo)) {
            if (sd < ultSaldo) fx.texto(ex + 60, ey + 30, "-" + (int) (ultSaldo - sd) + " monedas", new Color(0xFF8A80));
        }
        ultSaldo = sd;
    }

    private void animarBarras(double dt) {
        double k = Math.min(1, dt * 6);
        dispH += (pou.getHambre() - dispH) * k;
        dispI += (pou.getHigiene() - dispI) * k;
        dispS += (pou.getSueño() - dispS) * k;
        dispSaldo += (pou.getSaldo() - dispSaldo) * Math.min(1, dt * 8);
    }

    private void impulso(double v) { sqVel += v; }

    private void actualizarPou(double dt) {
        boolean muerto = muerto(), duerme = duerme();
        // resorte de squash & stretch
        sqVel += (-190 * sq - 14 * sqVel) * dt;
        sq += sqVel * dt;
        sq = Math.max(-0.6, Math.min(0.6, sq));

        for (int tmr = 0; tmr < 1; tmr++) {
            comerT = Math.max(0, comerT - dt);
            risaT = Math.max(0, risaT - dt);
            sorpresaT = Math.max(0, sorpresaT - dt);
            rechazoT = Math.max(0, rechazoT - dt);
            tristeT = Math.max(0, tristeT - dt);
            bostezoT = Math.max(0, bostezoT - dt);
            despertarT = Math.max(0, despertarT - dt);
            duchaT = Math.max(0, duchaT - dt);
            volteoT = Math.max(0, volteoT - dt);
            cariciaCd = Math.max(0, cariciaCd - dt);
        }

        // super pou
        if (superT > 0) {
            superT -= dt;
            carino = Math.max(0, superT / SUPER_SEG);
            if (superT <= 0) {
                superT = 0; carino = 0;
                fx.brillos(ox(), oy() - 90, 20, new Color(0xFFE066));
                toast(pou.getNombre() + " volvió a la normalidad. ¡Qué viaje!", INFO);
            }
        }
        superMix += ((superT > 0 ? 1 : 0) - superMix) * Math.min(1, dt * 4);

        double hopObj = 0;
        if (muerto) {
            muertoT += dt;
            vx = 0; vy = 0; sujetado = false;
            double k = Math.min(1, muertoT / 1.6);
            pose.fantasma = k;
            alt += ((70 + Math.sin(t * 2) * 12) * k - alt) * Math.min(1, dt * 2);
            if (muertoT > 3.2) gameOverVisible = true;
        } else if (duerme) {
            sujetado = false; vx = vy = 0;
            double dx = Fondos.CAMA_X - px;
            px += dx * Math.min(1, dt * 2.6);
            double cerca = Math.abs(dx) < 25 ? 1 : 0;
            double altObj = cerca * 40;
            alt += (altObj - alt) * Math.min(1, dt * 7);
            hopObj = Math.abs(dx) > 25 ? Math.abs(Math.sin(t * 12)) * 10 : 0;
            pose.giro += ((cerca > 0 ? -0.10 : 0) - pose.giro) * Math.min(1, dt * 5);
        } else if (sujetado) {
            double objX = mouseX - offX;
            double feetY = mouseY - offY;
            double objAlt = Math.max(0, Math.min(380, SUELO - feetY));
            px += (objX - px) * Math.min(1, dt * 20);
            double altAnt = alt;
            alt += (objAlt - alt) * Math.min(1, dt * 20);
            vy = (alt - altAnt) / Math.max(dt, 1e-4) * 0.2;
            pose.giro += ((mvx * 0.0007) - pose.giro) * Math.min(1, dt * 8);
            pose.giro = Math.max(-0.5, Math.min(0.5, pose.giro));
            sorpresaT = Math.max(sorpresaT, 0.15);
        } else if (superT > 0) {
            // vuelo: sigue al mouse
            double ox = Math.max(minXVuelo(), Math.min(maxXVuelo(), mouseX));
            double objAlt = Math.max(30, Math.min(330, SUELO - (mouseY + 60)));
            double ant = px;
            px += (ox - px) * Math.min(1, dt * 3.2);
            alt += (objAlt + Math.sin(t * 3) * 10 - alt) * Math.min(1, dt * 3.2);
            vx = (px - ant) / Math.max(dt, 1e-4);
            vy = 0;
            pose.giro += (Math.max(-0.35, Math.min(0.35, vx * 0.0011)) - pose.giro) * Math.min(1, dt * 6);
            if (Math.abs(vx) > 40 || t % 0.1 < dt) fx.arcoiris(ox() - Math.signum(vx) * 30, oy() - 55);
            if (((int) (t * 30)) % 3 == 0) fx.add(Particulas.Tipo.BRILLO, ox() + (Math.random() - 0.5) * 120, oy() - 40 - Math.random() * 100,
                    0, 30, 0.7, 6, 0, new Color(0xFFE066), null);
        } else {
            // fisica normal
            double dir = 0;
            if (izq) dir -= 1;
            if (der) dir += 1;
            if (dir != 0) objetivoX = Double.NaN;
            else if (!Double.isNaN(objetivoX)) {
                if (Math.abs(objetivoX - px) < 7) objetivoX = Double.NaN;
                else dir = Math.signum(objetivoX - px);
            }
            boolean suelo = alt <= 0.01 && vy <= 0;
            if (suelo) {
                if (alt > 0 && escena == Escena.SALA) { /* bajar de la cama */ }
                // fuera de limites de la escena: caminar hacia adentro
                if (px < minX() - 4) dir = 1;
                else if (px > maxX() + 4) dir = -1;
                double dlim = dir;
                if ((px <= minX() && dlim < 0) || (px >= maxX() && dlim > 0)) dlim = 0;
                if (escena != Escena.SALA || true) dir = dlim;
                vx += (dir * VEL - vx) * Math.min(1, dt * 10);
                if (dir == 0 && Math.abs(vx) < 5) vx = 0;
            } else {
                vx += dir * 500 * dt;
                vx *= 1 - Math.min(1, dt * 0.35);
            }
            vy -= GRAV * dt;
            alt += vy * dt;
            if (alt <= 0) {
                if (vy < -250) {
                    aterrizar(-vy);
                    vy = -vy * 0.33;
                    if (vy < 140) vy = 0;
                    alt = 0.001;
                } else {
                    vy = 0; alt = 0;
                }
            }
            px += vx * dt;
            if (px < 90) { px = 90; vx = Math.abs(vx) * 0.5; }
            if (px > 880) { px = 880; vx = -Math.abs(vx) * 0.5; }
            double marcha = Math.min(1, Math.abs(vx) / VEL);
            if (suelo) {
                pose.giro += (Math.sin(t * 16) * 0.07 * marcha - pose.giro) * Math.min(1, dt * 14);
                hopObj = Math.abs(Math.sin(t * 16)) * 7 * marcha;
            } else {
                pose.giro += (-vx * 0.0009 - pose.giro) * Math.min(1, dt * 6);
            }
            if (volteoT > 0) pose.giro = -2 * Math.PI * (1 - volteoT / 0.85);
            if (rechazoT > 0) pose.giro += Math.sin(t * 38) * 0.09;
            if (duchaT > 0 || risaT > 0) pose.giro += Math.sin(t * 22) * 0.025;
        }
        hop += (hopObj - hop) * Math.min(1, dt * 20);

        // animacion de respiracion y gestos
        double resp = Math.sin(t * 2.3);
        pose.sy = 1 + sq + (muerto ? 0 : resp * 0.016);
        pose.sx = 1 - sq * 0.7 - (muerto ? 0 : resp * 0.011);
        if (rumbleT > 0) {
            rumbleT -= dt;
            pose.sx *= 1 + Math.sin(t * 50) * 0.02;
        }
        // parpadeo
        parpadeoT -= dt;
        double parp = 0;
        if (parpadeoT > 0) parp = Math.sin((parpadeoT / 0.16) * Math.PI);
        else { proxParpadeo -= dt; if (proxParpadeo <= 0) { parpadeoT = 0.16; proxParpadeo = 1.8 + Math.random() * 3.5; } }
        double sueno = pou.getSueño();
        sleepyBase += ((sueno <= 20 && !duerme ? 0.45 : 0) - sleepyBase) * Math.min(1, dt * 3);
        pose.parpadeo = Math.max(parp, sleepyBase);
        if (bostezoT > 0) pose.parpadeo = 1;

        // mirada hacia el mouse
        Point2D ojo = PouArte.aPantalla(ox(), oy(), ESC, pose, 0, -100);
        double tx = Math.max(-1, Math.min(1, (mouseX - ojo.getX()) / 170));
        double ty = Math.max(-1, Math.min(1, (mouseY - ojo.getY()) / 170));
        if (arr != null && arr.tipo < 2) { tx = Math.max(-1, Math.min(1, (arr.x - ojo.getX()) / 140)); ty = Math.max(-1, Math.min(1, (arr.y - ojo.getY()) / 140)); }
        miraX += (tx - miraX) * Math.min(1, dt * 9);
        miraY += (ty - miraY) * Math.min(1, dt * 9);
        pose.miraX = miraX; pose.miraY = miraY;

        // cara
        boolean anticipa = arr != null && arr.tipo < 2 && boca().distance(arr.x, arr.y) < 150;
        double min = Math.min(pou.getHambre(), Math.min(pou.getHigiene(), pou.getSueño()));
        double max = Math.max(pou.getHambre(), Math.max(pou.getHigiene(), pou.getSueño()));
        PouArte.Cara c;
        if (muerto) c = PouArte.Cara.MUERTO;
        else if (duerme) c = PouArte.Cara.DORMIDO;
        else if (comerT > 0 || anticipa) { c = PouArte.Cara.COMIENDO; pose.boca = anticipa && comerT <= 0 ? 0.85 : 0.35 + 0.65 * Math.abs(Math.sin(t * 13)); }
        else if (bostezoT > 0) { c = PouArte.Cara.COMIENDO; pose.boca = Math.sin((1 - bostezoT / 1.4) * Math.PI); }
        else if (sorpresaT > 0) c = PouArte.Cara.SORPRESA;
        else if (rechazoT > 0) c = PouArte.Cara.ASCO;
        else if (risaT > 0 || duchaT > 0 || superT > 0) c = PouArte.Cara.RISA;
        else if (tristeT > 0) c = PouArte.Cara.TRISTE;
        else if (enfermo()) c = PouArte.Cara.ENFERMO;
        else if (min <= 20) c = PouArte.Cara.TRISTE;
        else if (min >= 70) c = PouArte.Cara.FELIZ;
        else c = PouArte.Cara.NORMAL;
        pose.cara = c;
        if (c == PouArte.Cara.RISA && superT <= 0 && duchaT <= 0 && risaT > 0) pose.parpadeo = 0;
        pose.enfermo += ((enfermo() ? 1 : 0) - pose.enfermo) * Math.min(1, dt * 3);
        pose.sucio = Math.max(0, (70 - dispI) / 70.0);
        pose.moco = duerme ? 0.5 + 0.5 * Math.sin(t * 1.6) : 0;
        pose.color = PouArte.mezclar(PouArte.MARRON, new Color(0xFFC62E), superMix * 0.85);
        if (!muerto) pose.fantasma = 0;

        // espuma baja sola; en la ducha se lava
        if (duchaT > 0) pose.espuma = Math.max(0, pose.espuma - dt * 0.45);
        else pose.espuma = Math.max(0, pose.espuma - dt * 0.02);
        double nivelObj = duchaT > 0 ? 1 : 0.15;
        nivelAgua += (nivelObj - nivelAgua) * Math.min(1, dt * 1.5);
    }

    private double minXVuelo() { return 100; }
    private double maxXVuelo() { return 860; }

    private void aterrizar(double v) {
        impulso(-Math.min(9, v / 140));
        if (v > 350) {
            fx.polvo(ox() - 40, SUELO, 4);
            fx.polvo(ox() + 40, SUELO, 4);
            Sonido.boing();
        }
        if (v > 800) { risaT = Math.max(risaT, 0.6); }
    }

    private void ambiente(double dt) {
        double nObj = duerme() ? 1 : 0;
        noche += (nObj - noche) * Math.min(1, dt * 1.8);
        double mObj = duerme() && Math.abs(px - Fondos.CAMA_X) < 25 ? 1 : 0;
        manta += (mObj - manta) * Math.min(1, dt * 3);

        double cx = ox(), cy = oy();
        if (duerme() && manta > 0.8) {
            proxZzz -= dt;
            if (proxZzz <= 0) { fx.zzz(cx + 45, cy - 150); proxZzz = 1.3; }
        }
        if (enfermo() && !muerto()) {
            proxToser -= dt;
            if (proxToser <= 0) {
                fx.sudor(cx + 55, cy - 135);
                fx.add(Particulas.Tipo.TEXTO, cx + 40, cy - 170, 0, -40, 1.0, 18, 0, new Color(0xA8D06B), "*cof*");
                impulso(-4); proxToser = 3 + Math.random() * 3;
            }
        }
        if (!muerto() && !duerme() && !enfermo()) {
            if (pou.getHambre() <= 20) {
                proxRumble -= dt;
                if (proxRumble <= 0) {
                    rumbleT = 0.9; proxRumble = 3.5;
                    fx.add(Particulas.Tipo.TEXTO, cx - 10, cy - 30, 0, -30, 1.2, 18, 0, new Color(0xFFB347), "grrr...");
                }
            }
            if (pou.getSueño() <= 20 && bostezoT <= 0) {
                proxBostezo -= dt;
                if (proxBostezo <= 0) { bostezoT = 1.4; proxBostezo = 7; }
            }
            if (pou.getHigiene() <= 30 && Math.random() < dt * 3) {
                double a = t * 3 + Math.random() * 6;
                fx.add(Particulas.Tipo.VAPOR, cx + Math.cos(a) * 80, cy - 70 + Math.sin(a) * 60, 0, -25, 1.0, 7, 0, null, null);
            }
        }
        if (duchaT > 0) {
            fx.gotas(450, 128, 2);
            if (Math.random() < dt * 14) fx.burbujas(cx, cy - 70, 1);
            if (Math.random() < dt * 5) fx.vapor(450, 330);
            proxNota -= dt;
            if (proxNota <= 0) { fx.nota(cx + 40, cy - 190); proxNota = 0.45; }
        }
        // super: orbitas de estrellas
        if (superMix > 0.5 && Math.random() < dt * 8) fx.add(Particulas.Tipo.BRILLO, cx + (Math.random() - 0.5) * 160, cy - 20 - Math.random() * 150,
                0, -20, 0.8, 7, 0, new Color(0xFFF2A8), null);
    }

    private void actualizarVoladores(double dt) {
        for (int i = voladores.size() - 1; i >= 0; i--) {
            Volador v = voladores.get(i);
            v.t += dt / 0.38;
            if (v.t >= 1) {
                Point2D b = boca();
                fx.migas(b.getX(), b.getY(), v.medicina ? 4 : 9, v.medicina ? new Color(0xF2F2F2) : new Color(0xE0A24A));
                if (v.medicina) Sonido.pop(); else Sonido.comer();
                impulso(-3.5);
                voladores.remove(i);
            }
        }
        for (int i = retornos.size() - 1; i >= 0; i--) {
            Retorno r = retornos.get(i);
            r.t += dt / 0.3;
            if (r.t >= 1) retornos.remove(i);
        }
    }

    // ===================================================================== acciones (usan PouService)

    private void rechazar(String msg, boolean saldo) {
        toast("Ojo: " + msg, ERROR);
        Sonido.error();
        if (saldo) tristeT = 1.4;
        else if (msg.contains("limpito")) risaT = 0.9;
        else rechazoT = 1.1;
    }

    private void comprarAlimento(int i, double ox, double oy) {
        if (i < 0 || i >= alimentos.size()) return;
        Alimento a = alimentos.get(i);
        try {
            String msg = service.comprarAlimento(pou, a);
            toast(msg, INFO);
            Volador v = new Volador();
            v.nombre = a.getNombre(); v.tipo = tipoAlimento(a); v.x0 = ox; v.y0 = oy;
            voladores.add(v);
            comerT = 1.9;
            if (a instanceof ComidaChatarra) { risaT = 0; }
            ganarCarino(0.01);
        } catch (SaldoInsuficienteException ex) {
            rechazar(ex.getMessage(), true);
            devolver(0, i, ox, oy);
        } catch (AccionNoPermitidaException ex) {
            rechazar(ex.getMessage(), false);
            devolver(0, i, ox, oy);
        }
    }

    private void comprarMedicamento(int i, double ox, double oy) {
        if (i < 0 || i >= medicamentos.size()) return;
        Medicamento m = medicamentos.get(i);
        try {
            String msg = service.comprarMedicamento(pou, m);
            toast(msg, pou.getEstado() == Pou.EstadoPou.SANO ? INFO : ALERTA);
            Volador v = new Volador();
            v.nombre = m.getNombre(); v.medicina = true; v.x0 = ox; v.y0 = oy;
            voladores.add(v);
            comerT = 1.2;
            if (pou.getEstado() != Pou.EstadoPou.SANO) rechazoT = 1.0;
        } catch (SaldoInsuficienteException ex) {
            rechazar(ex.getMessage(), true);
            devolver(1, i, ox, oy);
        } catch (AccionNoPermitidaException ex) {
            rechazar(ex.getMessage(), false);
            devolver(1, i, ox, oy);
        }
    }

    private void devolver(int tipo, int idx, double x, double y) {
        Retorno r = new Retorno();
        r.tipo = tipo; r.idx = idx; r.x0 = x; r.y0 = y;
        retornos.add(r);
    }

    private int tipoAlimento(Alimento a) {
        return a instanceof Fruta ? 0 : a instanceof Vegetal ? 1 : 2;
    }

    private void ducha() {
        if (duchaT > 0) return;
        try {
            toast(service.banar(pou), INFO);
            duchaT = 3.4;
            Sonido.tono(300, 600, 600, 0.3);
            ganarCarino(0.01);
        } catch (AccionNoPermitidaException ex) {
            rechazar(ex.getMessage(), false);
        }
    }

    private void accionDormir() {
        try {
            if (duerme()) {
                toast(service.despertar(pou), INFO);
            } else {
                String m = service.dormir(pou);
                toast(m, INFO);
                escena = Escena.SALA;
                objetivoX = Double.NaN;
                arr = null;
            }
        } catch (AccionNoPermitidaException ex) {
            rechazar(ex.getMessage(), false);
        }
    }

    private void comprarPrenda(int i) {
        if (tienda == null || i < 0 || i >= tienda.size()) return;
        try {
            String m = service.comprarPrenda(pou, tienda.get(i));
            toast(m, INFO);
            refrescarRopa();
            fx.brillos(ox(), oy() - 90, 16, new Color(0xFFE066));
            risaT = 1.0;
            Sonido.moneda();
        } catch (SaldoInsuficienteException ex) {
            rechazar(ex.getMessage(), true);
        } catch (AccionNoPermitidaException ex) {
            rechazar(ex.getMessage(), false);
        }
    }

    private void alternarPrenda(int i) {
        if (i < 0 || i >= mias.size()) return;
        try {
            String m = service.alternarPrenda(pou, mias.get(i));
            toast(m, INFO);
            refrescarRopa();
            fx.brillos(ox(), oy() - 100, 12, new Color(0xFFFFFF));
            impulso(5);
            Sonido.pop();
            if (m.startsWith("Le pusiste")) { risaT = 0.9; fx.corazones(ox(), oy() - 150, 2); }
        } catch (AccionNoPermitidaException ex) {
            rechazar(ex.getMessage(), false);
        }
    }

    private void abrirVoley() {
        if (duerme()) { toast("Ahora no se puede, esta durmiendo.", ERROR); Sonido.error(); return; }
        if (enfermo()) { toast("Ojo: " + pou.getNombre() + " esta enfermo: primero hay que curarlo en la farmacia.", ERROR); Sonido.error(); return; }
        if (muerto()) return;
        nav.abrirVoley(pou, ropa, this);
    }

    private void salir() {
        service.guardar(pou);
        timer.stop();
        long minutos = Duration.between(pou.getFechaCreacion(), LocalDateTime.now()).toMinutes();
        if (muerto()) {
            nav.irInicio("GAME OVER: " + pou.getNombre() + " vivió " + minutos + " minuto/s y terminó con " + (int) pou.getSaldo() + " monedas.");
        } else {
            nav.irInicio("Partida guardada. " + resumen());
        }
    }

    private void navAccion(int id) {
        if (muerto()) return;
        boolean noPuede = duerme() && id != 0 && id != 3 && id != 7 && id != 8;
        if (noPuede) {
            toast("Ahora no se puede, esta durmiendo.", ERROR);
            Sonido.error();
            return;
        }
        switch (id) {
            case 0: cambiarEscena(Escena.SALA); break;
            case 1: cambiarEscena(Escena.COCINA); break;
            case 2: cambiarEscena(Escena.BANO); break;
            case 3: accionDormir(); break;
            case 4: cambiarEscena(Escena.ROPERO); break;
            case 5: cambiarEscena(Escena.FARMACIA); break;
            case 6: abrirVoley(); break;
            case 7: estadoVisible = !estadoVisible; Sonido.pop(); break;
            default: salir();
        }
    }

    // ===================================================================== entrada

    private Rectangle2D navRect(int i) { return new Rectangle2D.Double(6 + i * 106, NAV_Y + 8, 100, 66); }

    private double[] slot(int i) {
        switch (escena) {
            case COCINA: return new double[]{690 + (i % 3) * 95, 218 + (i / 3) * 130};
            case FARMACIA: return new double[]{735 + i * 120, 230};
            default: return new double[]{790, 335};
        }
    }

    private int slotEn(double x, double y) {
        int n = escena == Escena.COCINA ? alimentos.size() : escena == Escena.FARMACIA ? medicamentos.size() : escena == Escena.BANO ? 1 : 0;
        for (int i = 0; i < n; i++) {
            double[] s = slot(i);
            if (Math.hypot(x - s[0], y - s[1]) < 46) return i;
        }
        return -1;
    }

    private Rectangle2D tabRect(int i) { return new Rectangle2D.Double(598 + i * 160, 96, 150, 32); }
    private Rectangle2D cardRect(int i) { return new Rectangle2D.Double(598 + (i % 2) * 160, 140 + (i / 2) * 140, 150, 130); }
    private Rectangle2D verRect() { return new Rectangle2D.Double(W / 2.0 - 110, 400, 220, 52); }

    private boolean sobreLampara(double x, double y) { return escena == Escena.SALA && x > 345 && x < 415 && y > 130 && y < 200; }
    private boolean sobreCanilla(double x, double y) { return escena == Escena.BANO && Math.hypot(x - 720, y - 250) < 38; }
    private boolean sobreCarino(double x, double y) { return x > 226 && x < 750 && y > 44 && y < 66; }

    private void instalarEventos() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); presionar(e); }
            @Override public void mouseReleased(MouseEvent e) { soltar(e); }
            @Override public void mouseMoved(MouseEvent e) { mover(e, false); }
            @Override public void mouseDragged(MouseEvent e) { mover(e, true); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { tecla(e, true); }
            @Override public void keyReleased(KeyEvent e) { tecla(e, false); }
        });
    }

    private void presionar(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        double x = e.getX(), y = e.getY();
        mouseX = x; mouseY = y;
        if (gameOverVisible) {
            if (verRect().contains(x, y)) salir();
            return;
        }
        if (estadoVisible) { estadoVisible = false; return; }
        for (int i = 0; i < 9; i++) if (navRect(i).contains(x, y)) { navAccion(i); return; }
        if (y >= NAV_Y) return;
        if (sobreCarino(x, y)) { transformar(); return; }
        if (y < HUD_H) return;
        if (muerto()) return;
        if (moneda != null && Math.hypot(x - moneda.x, y - moneda.y) < 34) { recolectarMoneda(); return; }
        if (sobreLampara(x, y)) { navAccion(3); return; }
        if (sobreCanilla(x, y)) { ducha(); return; }

        if (escena == Escena.ROPERO && x > 590) {
            for (int i = 0; i < 2; i++) if (tabRect(i).contains(x, y)) { tabRopero = i; refrescarRopa(); Sonido.pop(); return; }
            int n = tabRopero == 0 ? (tienda == null ? 0 : tienda.size()) : mias.size();
            for (int i = 0; i < n; i++) if (cardRect(i).contains(x, y)) {
                if (tabRopero == 0) comprarPrenda(i); else alternarPrenda(i);
                return;
            }
            return;
        }
        if (!duerme()) {
            int s = slotEn(x, y);
            if (s >= 0) {
                arr = new Arrastre();
                arr.tipo = escena == Escena.COCINA ? 0 : escena == Escena.FARMACIA ? 1 : 2;
                arr.idx = s; arr.x = x; arr.y = y; arr.sx = x; arr.sy = y;
                presionT = System.currentTimeMillis();
                Sonido.pop();
                return;
            }
        }
        presionT = System.currentTimeMillis();
        presionX = x; presionY = y;
        presionSobrePou = sobrePou(x, y);
        if (presionSobrePou) {
            if (duerme()) {
                fx.zzz(ox() + 40, oy() - 150);
                impulso(-2);
                return;
            }
            if (superT <= 0) {
                sujetado = true;
                offX = x - px;
                offY = y - (SUELO - alt);
                mvx = mvy = 0;
                objetivoX = Double.NaN;
                if (e.getClickCount() >= 2) { sujetado = false; saltar(true); }
            } else {
                fx.corazones(ox(), oy() - 120, 3);
                impulso(-5);
            }
        } else if (superT > 0) {
            fx.estrellas(x, y, 14);
            Sonido.magia();
        } else if (!duerme() && y > 200) {
            objetivoX = Math.max(minX(), Math.min(maxX(), x));
        }
    }

    private void soltar(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        double x = e.getX(), y = e.getY();
        mouseX = x; mouseY = y;
        if (arr != null) {
            Arrastre a = arr;
            arr = null;
            boolean clic = Math.hypot(x - a.sx, y - a.sy) < 8;
            if (a.tipo == 2) return;
            boolean cerca = clic || sobrePou(x, y) || boca().distance(x, y) < 110;
            if (cerca) {
                if (a.tipo == 0) comprarAlimento(a.idx, x, y);
                else comprarMedicamento(a.idx, x, y);
            } else {
                devolver(a.tipo, a.idx, x, y);
            }
            return;
        }
        if (sujetado) {
            sujetado = false;
            boolean clic = Math.hypot(x - presionX, y - presionY) < 7 && System.currentTimeMillis() - presionT < 400;
            if (clic) {
                // toque: cosquillas
                impulso(-7);
                risaT = 0.7;
                fx.corazones(ox(), oy() - 130, 2);
                Sonido.pop();
                ganarCarino(0.012);
            } else {
                vx = Math.max(-800, Math.min(800, mvx * 0.9));
                vy = Math.max(-600, Math.min(900, -mvy * 0.9));
                if (alt > 60) risaT = 0.8;
            }
        }
    }

    private void mover(MouseEvent e, boolean arrastrando) {
        double nx = e.getX(), ny = e.getY();
        double dx = nx - mouseX, dy = ny - mouseY;
        mvx = mvx * 0.6 + dx * 60 * 0.4;
        mvy = mvy * 0.6 + dy * 60 * 0.4;
        mouseX = nx; mouseY = ny;
        if (arr != null) {
            arr.x = nx; arr.y = ny;
            if (arr.tipo == 2 && sobrePou(nx, ny) && !muerto() && !duerme()) {
                double v = Math.hypot(dx, dy);
                pose.espuma = Math.min(1, pose.espuma + v * 0.0035);
                if (v > 3 && Math.random() < 0.5) fx.burbujas(nx, ny, 1);
                risaT = Math.max(risaT, 0.25);
            }
        } else if (!arrastrando && !sujetado && !muerto() && !duerme() && superT <= 0 && sobrePou(nx, ny)) {
            caricia += Math.hypot(dx, dy);
            if (caricia > 480 && cariciaCd <= 0) {
                caricia = 0; cariciaCd = 0.5;
                fx.corazones(nx, ny - 20, 2);
                risaT = Math.max(risaT, 0.9);
                impulso(2.5);
                Sonido.pop();
                ganarCarino(0.04);
            }
        }
        actualizarCursor();
    }

    private void actualizarCursor() {
        boolean mano = false;
        for (int i = 0; i < 9; i++) if (navRect(i).contains(mouseX, mouseY)) { hoverNav = i; mano = true; break; }
        if (!mano) hoverNav = -1;
        if (!mano && mouseY < NAV_Y && mouseY > HUD_H && !muerto()) {
            if (moneda != null && Math.hypot(mouseX - moneda.x, mouseY - moneda.y) < 34) mano = true;
            else if (slotEn(mouseX, mouseY) >= 0 || sobreLampara(mouseX, mouseY) || sobreCanilla(mouseX, mouseY) || sobrePou(mouseX, mouseY)) mano = true;
            else if (escena == Escena.ROPERO && mouseX > 590) mano = true;
        }
        if (mouseY > 44 && mouseY < 66 && sobreCarino(mouseX, mouseY) && carino >= 1) mano = true;
        setCursor(Cursor.getPredefinedCursor(sujetado || arr != null ? Cursor.MOVE_CURSOR : mano ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void saltar(boolean voltereta) {
        if (muerto() || duerme() || sujetado || superT > 0) return;
        if (alt > 2) return;
        vy = voltereta ? 760 : 620;
        impulso(7);
        alt = 0.5;
        if (voltereta) { volteoT = 0.85; risaT = 1.0; }
        Sonido.boing();
        fx.polvo(ox(), SUELO, 4);
    }

    private void tecla(KeyEvent e, boolean abajo) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) izq = abajo;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) der = abajo;
        if (!abajo) return;
        if (gameOverVisible) { if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_ESCAPE) salir(); return; }
        if (k == KeyEvent.VK_ESCAPE) {
            if (estadoVisible) estadoVisible = false;
            else if (escena != Escena.SALA && !muerto()) cambiarEscena(Escena.SALA);
            return;
        }
        if (estadoVisible && k != KeyEvent.VK_E) { estadoVisible = false; return; }
        switch (k) {
            case KeyEvent.VK_SPACE: saltar(false); break;
            case KeyEvent.VK_H: navAccion(0); break;
            case KeyEvent.VK_C: navAccion(1); break;
            case KeyEvent.VK_B: navAccion(2); break;
            case KeyEvent.VK_S: navAccion(3); break;
            case KeyEvent.VK_R: navAccion(4); break;
            case KeyEvent.VK_F: navAccion(5); break;
            case KeyEvent.VK_J: navAccion(6); break;
            case KeyEvent.VK_E: navAccion(7); break;
            case KeyEvent.VK_T: transformar(); break;
            case KeyEvent.VK_Y: if (moneda != null) recolectarMoneda(); break;
            case KeyEvent.VK_M: Sonido.setActivo(!Sonido.isActivo()); toast(Sonido.isActivo() ? "Sonido activado" : "Sonido silenciado", INFO); break;
            case KeyEvent.VK_ENTER: if (escena == Escena.BANO && !duerme()) ducha(); break;
            default:
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9 && !muerto() && !duerme()) {
                    int i = k - KeyEvent.VK_1;
                    Point2D b = boca();
                    if (escena == Escena.COCINA) comprarAlimento(i, slot(Math.min(i, Math.max(0, alimentos.size() - 1)))[0], slot(Math.min(i, Math.max(0, alimentos.size() - 1)))[1]);
                    else if (escena == Escena.FARMACIA) comprarMedicamento(i, slot(Math.min(i, Math.max(0, medicamentos.size() - 1)))[0], slot(Math.min(i, Math.max(0, medicamentos.size() - 1)))[1]);
                    else if (escena == Escena.ROPERO) { if (tabRopero == 0) comprarPrenda(i); else alternarPrenda(i); }
                }
        }
    }

    // ===================================================================== dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setClip(0, 0, W, H);

        dibujarEscena(g);
        dibujarMoneda(g);
        fx.dibujar(g);
        dibujarHud(g);
        dibujarToasts(g);
        dibujarNav(g);
        if (estadoVisible) dibujarEstado(g);
        if (gameOverVisible) dibujarGameOver(g);
        if (flashT > 0) {
            g.setColor(new Color(255, 255, 255, (int) (200 * flashT)));
            g.fillRect(0, 0, W, H);
        }
        g.dispose();
    }

    private void dibujarEscena(Graphics2D g) {
        Shape clip = g.getClip();
        g.clip(new Rectangle2D.Double(0, 0, W, NAV_Y));
        switch (escena) {
            case SALA:
                Fondos.sala(g, t, noche);
                Fondos.cama(g, noche);
                break;
            case COCINA:
                Fondos.cocina(g, t);
                dibujarSlots(g);
                break;
            case BANO:
                Fondos.banoFondo(g, t, duchaT);
                dibujarSlots(g);
                break;
            case ROPERO:
                Fondos.ropero(g, t);
                break;
            default:
                Fondos.farmacia(g, t);
                dibujarSlots(g);
        }
        dibujarPou(g);
        if (escena == Escena.SALA) {
            Fondos.manta(g, manta);
            if (noche > 0.01) {
                g.setColor(new Color(10, 14, 50, (int) (125 * noche)));
                g.fillRect(0, 0, W, NAV_Y);
                // luz de luna
                RadialGradientPaint rg = new RadialGradientPaint(new Point2D.Double(185, 190), 260,
                        new float[]{0f, 1f}, new Color[]{new Color(190, 210, 255, (int) (70 * noche)), new Color(190, 210, 255, 0)});
                g.setPaint(rg);
                g.fill(new Rectangle2D.Double(0, 0, W, NAV_Y));
            }
        } else if (escena == Escena.BANO) {
            Fondos.banoFrente(g, t, nivelAgua, 0);
            if (duchaT > 0) {
                g.setColor(new Color(170, 215, 255, 120));
                g.setStroke(new BasicStroke(2f));
                for (int i = 0; i < 26; i++) {
                    double xx = 405 + (i * 37) % 90;
                    double yy = 128 + ((t * 700 + i * 41) % 330);
                    g.draw(new Line2D.Double(xx, yy, xx - 2, yy + 16));
                }
            }
        } else if (escena == Escena.ROPERO) {
            dibujarPanelRopero(g);
        }
        dibujarVoladores(g);
        dibujarArrastre(g);
        g.setClip(clip);
    }

    private void dibujarPou(Graphics2D g) {
        double cx = ox(), cy = oy();
        double groundY = duerme() ? SUELO - 40 : SUELO;
        if (!muerto()) {
            double sh = Math.max(0.3, 1 - Math.max(0, alt - (duerme() ? 40 : 0)) / 280.0);
            g.setColor(new Color(0, 0, 0, (int) (70 * sh)));
            g.fill(new Ellipse2D.Double(cx - 85 * ESC * sh, groundY - 6, 170 * ESC * sh, 18 * sh));
        }
        double s = superMix;
        if (s > 0.02) {
            double pul = 1 + 0.08 * Math.sin(t * 6);
            RadialGradientPaint aura = new RadialGradientPaint(new Point2D.Double(cx, cy - 85), (float) (190 * pul),
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(255, 236, 130, (int) (170 * s)), new Color(255, 190, 60, (int) (80 * s)), new Color(255, 190, 60, 0)});
            g.setPaint(aura);
            g.fill(new Ellipse2D.Double(cx - 200, cy - 285, 400, 400));
            // alas
            double aleteo = Math.sin(t * 14) * 0.35;
            for (int lado = -1; lado <= 1; lado += 2) {
                AffineTransform at = new AffineTransform();
                at.translate(cx + lado * 60, cy - 105);
                at.rotate(lado * (0.6 + aleteo));
                Shape ala = at.createTransformedShape(new Ellipse2D.Double(lado > 0 ? 0 : -110, -22, 110, 44));
                g.setColor(new Color(255, 255, 255, (int) (220 * s)));
                g.fill(ala);
                g.setColor(new Color(255, 214, 90, (int) (255 * s)));
                g.setStroke(new BasicStroke(3f));
                g.draw(ala);
            }
        }
        PouArte.dibujar(g, cx, cy, ESC, ropa, pose);
        if (s > 0.02) {
            for (int i = 0; i < 5; i++) {
                double a = t * 2.2 + i * Math.PI * 2 / 5;
                double ex = cx + Math.cos(a) * 125, ey = cy - 90 + Math.sin(a) * 38;
                AffineTransform at = AffineTransform.getTranslateInstance(ex, ey);
                at.rotate(a * 2);
                g.setColor(new Color(255, 232, 120, (int) (255 * s)));
                g.fill(at.createTransformedShape(Particulas.estrella(11, 5, 0.45)));
            }
            // halo
        }
        if (muerto() && pose.fantasma > 0.3) {
            g.setColor(new Color(255, 236, 130, (int) (230 * pose.fantasma)));
            g.setStroke(new BasicStroke(5f));
            g.draw(new Ellipse2D.Double(cx - 34, cy - 205, 68, 18));
        }
    }

    private void dibujarSlots(Graphics2D g) {
        int n = escena == Escena.COCINA ? alimentos.size() : escena == Escena.FARMACIA ? medicamentos.size() : escena == Escena.BANO ? 1 : 0;
        int hover = slotEn(mouseX, mouseY);
        for (int i = 0; i < n; i++) {
            double[] s = slot(i);
            boolean h = i == hover && arr == null;
            boolean oculto = arr != null && arr.tipo == (escena == Escena.COCINA ? 0 : escena == Escena.FARMACIA ? 1 : 2) && arr.idx == i;
            double bob = h ? Math.sin(t * 9) * 3 : 0;
            double tam = (escena == Escena.FARMACIA ? 72 : 58) * (h ? 1.16 : 1);
            if (!oculto) {
                if (escena == Escena.BANO) dibujarEsponja(g, s[0], s[1] + bob, 1.0 * (h ? 1.12 : 1));
                else if (escena == Escena.COCINA) Iconos.alimento(g, alimentos.get(i).getNombre(), tipoAlimento(alimentos.get(i)), s[0], s[1] + bob, tam);
                else Iconos.medicina(g, medicamentos.get(i).getNombre(), s[0], s[1] + bob, tam);
            }
            if (escena == Escena.BANO) continue;
            // etiqueta
            String nombre = escena == Escena.COCINA ? alimentos.get(i).getNombre() : medicamentos.get(i).getNombre();
            double precio = escena == Escena.COCINA ? alimentos.get(i).getPrecio() : medicamentos.get(i).getPrecio();
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String num = nombre;
            int wTxt = g.getFontMetrics().stringWidth(num);
            double ly = s[1] + (escena == Escena.FARMACIA ? 50 : 42);
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Double(s[0] - wTxt / 2.0 - 6, ly - 12, wTxt + 12, 16, 8, 8));
            g.setColor(Color.WHITE);
            g.drawString(num, (float) (s[0] - wTxt / 2.0), (float) ly);
            Iconos.moneda(g, s[0] - 14, ly + 14, 7, false, 0);
            g.setColor(new Color(0xFFE066));
            g.drawString(String.valueOf((int) precio), (float) (s[0] - 4), (float) (ly + 18));
        }
        if (hover >= 0 && arr == null && escena != Escena.BANO) dibujarTooltip(g, hover);
    }

    private void dibujarTooltip(Graphics2D g, int i) {
        String txt;
        if (escena == Escena.COCINA) txt = describirAlimento(alimentos.get(i));
        else txt = "efectividad " + medicamentos.get(i).getEfectividad() + "%";
        txt += "  [tecla " + (i + 1) + "]";
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int w = g.getFontMetrics().stringWidth(txt) + 20;
        double[] s = slot(i);
        double x = Math.max(8, Math.min(W - w - 8, s[0] - w / 2.0)), y = s[1] - 78;
        g.setColor(new Color(30, 20, 50, 230));
        g.fill(new RoundRectangle2D.Double(x, y, w, 26, 12, 12));
        g.setColor(Color.WHITE);
        g.drawString(txt, (float) (x + 10), (float) (y + 18));
    }

    private String describirAlimento(Alimento a) {
        if (a instanceof Fruta) return "hambre +" + a.getValorHambre() + ", higiene +" + ((Fruta) a).getValorHigieneBonus();
        if (a instanceof Vegetal) return "hambre +" + (int) (a.getValorHambre() * ((Vegetal) a).getMultiplicadorHambre());
        return "hambre +" + a.getValorHambre() + ", higiene -" + ((ComidaChatarra) a).getValorHigienePenalizacion();
    }

    private void dibujarEsponja(Graphics2D g, double cx, double cy, double s) {
        Graphics2D h = (Graphics2D) g.create();
        h.translate(cx, cy);
        h.scale(s, s);
        h.rotate(-0.2);
        h.setColor(new Color(0xFFD93D));
        h.fill(new RoundRectangle2D.Double(-34, -20, 68, 40, 14, 14));
        h.setColor(new Color(0xE0A800));
        h.setStroke(new BasicStroke(2.5f));
        h.draw(new RoundRectangle2D.Double(-34, -20, 68, 40, 14, 14));
        h.setColor(new Color(0xE0A800));
        for (int i = 0; i < 6; i++) h.fill(new Ellipse2D.Double(-24 + (i % 3) * 20, -10 + (i / 3) * 16, 7, 7));
        h.setColor(new Color(0x4CAF50));
        h.fill(new RoundRectangle2D.Double(-34, 8, 68, 12, 8, 8));
        h.dispose();
    }

    private void dibujarVoladores(Graphics2D g) {
        Point2D b = boca();
        for (Volador v : voladores) {
            double u = Math.min(1, v.t);
            double e = u * u * (3 - 2 * u);
            double x = v.x0 + (b.getX() - v.x0) * e;
            double y = v.y0 + (b.getY() - v.y0) * e - Math.sin(u * Math.PI) * 50;
            double tam = 58 * (1 - 0.55 * u);
            if (v.medicina) Iconos.medicina(g, v.nombre, x, y, tam + 10);
            else Iconos.alimento(g, v.nombre, v.tipo, x, y, tam);
        }
        for (Retorno r : retornos) {
            double[] s = slot(r.idx);
            double u = Math.min(1, r.t), e = u * u * (3 - 2 * u);
            double x = r.x0 + (s[0] - r.x0) * e, y = r.y0 + (s[1] - r.y0) * e;
            if (escena == Escena.COCINA && r.idx < alimentos.size()) Iconos.alimento(g, alimentos.get(r.idx).getNombre(), tipoAlimento(alimentos.get(r.idx)), x, y, 58);
            else if (escena == Escena.FARMACIA && r.idx < medicamentos.size()) Iconos.medicina(g, medicamentos.get(r.idx).getNombre(), x, y, 70);
        }
    }

    private void dibujarArrastre(Graphics2D g) {
        if (arr == null) return;
        double bob = Math.sin(t * 10) * 2;
        if (arr.tipo == 0 && arr.idx < alimentos.size()) Iconos.alimento(g, alimentos.get(arr.idx).getNombre(), tipoAlimento(alimentos.get(arr.idx)), arr.x, arr.y + bob, 70);
        else if (arr.tipo == 1 && arr.idx < medicamentos.size()) Iconos.medicina(g, medicamentos.get(arr.idx).getNombre(), arr.x, arr.y + bob, 80);
        else if (arr.tipo == 2) dibujarEsponja(g, arr.x, arr.y, 1.2);
    }

    private void dibujarMoneda(Graphics2D g) {
        if (moneda == null) return;
        boolean esp = moneda.m instanceof MonedaEspecial;
        double bob = Math.sin(t * 4) * 6;
        double cx = moneda.x, cy = moneda.y + bob;
        double esc = moneda.aparece;
        double r = 22 * esc;
        RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Double(cx, cy), 50,
                new float[]{0f, 1f}, new Color[]{esp ? new Color(120, 230, 255, 140) : new Color(255, 225, 90, 120), new Color(255, 225, 90, 0)});
        g.setPaint(glow);
        g.fill(new Ellipse2D.Double(cx - 50, cy - 50, 100, 100));
        Iconos.moneda(g, cx, cy, r, esp, moneda.giro);
        // anillo de tiempo
        double f = Math.max(0, moneda.vida / moneda.max);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(f > 0.35 ? new Color(255, 255, 255, 220) : new Color(255, 90, 90, 240));
        g.draw(new Arc2D.Double(cx - 30, cy - 30, 60, 60, 90, 360 * f, Arc2D.OPEN));
        if (esp && (int) (t * 8) % 2 == 0) fx.add(Particulas.Tipo.BRILLO, cx + (Math.random() - 0.5) * 50, cy + (Math.random() - 0.5) * 50, 0, -20, 0.5, 5, 0, new Color(0x9FF0FF), null);
    }

    // ---- ropero

    private void dibujarPanelRopero(Graphics2D g) {
        String[] tabs = {"Tienda", "Mis prendas"};
        for (int i = 0; i < 2; i++) {
            Rectangle2D r = tabRect(i);
            boolean sel = i == tabRopero;
            g.setColor(sel ? new Color(0xFFF3D6) : new Color(255, 255, 255, 120));
            g.fill(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 14, 14));
            g.setColor(new Color(0x6A4420));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 14, 14));
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            int w = g.getFontMetrics().stringWidth(tabs[i]);
            g.drawString(tabs[i], (float) (r.getX() + (r.getWidth() - w) / 2), (float) (r.getY() + 22));
        }
        List<Prenda> lista = tabRopero == 0 ? tienda : mias;
        if (lista == null || lista.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            String m = "(todavía no tenés prendas)";
            g.drawString(m, (float) (755 - g.getFontMetrics().stringWidth(m) / 2.0), 260);
            return;
        }
        for (int i = 0; i < lista.size() && i < 4; i++) {
            Prenda p = lista.get(i);
            Rectangle2D r = cardRect(i);
            boolean h = r.contains(mouseX, mouseY);
            double dy = h ? -3 : 0;
            g.setColor(new Color(0, 0, 0, 40));
            g.fill(new RoundRectangle2D.Double(r.getX() + 3, r.getY() + 5, r.getWidth(), r.getHeight(), 18, 18));
            g.setColor(h ? new Color(0xFFFFFF) : new Color(0xFFF6E6));
            g.fill(new RoundRectangle2D.Double(r.getX(), r.getY() + dy, r.getWidth(), r.getHeight(), 18, 18));
            g.setColor(new Color(0x6A4420));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new RoundRectangle2D.Double(r.getX(), r.getY() + dy, r.getWidth(), r.getHeight(), 18, 18));
            Iconos.prenda(g, p.getNombre(), r.getCenterX(), r.getY() + 46 + dy, 70);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(0x4A2C12));
            String n = (i + 1) + ". " + p.getNombre();
            g.drawString(n, (float) (r.getCenterX() - g.getFontMetrics().stringWidth(n) / 2.0), (float) (r.getY() + 92 + dy));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            if (tabRopero == 0) {
                Iconos.moneda(g, r.getCenterX() - 16, r.getY() + 110 + dy, 8, false, 0);
                g.setColor(new Color(0xB8860B));
                g.drawString((int) p.getPrecio() + " monedas", (float) (r.getCenterX() - 6), (float) (r.getY() + 115 + dy));
            } else {
                g.setColor(p.isEstaPuesta() ? new Color(0x2E9B3E) : new Color(0x7A5A3A));
                String tg = p.isEstaPuesta() ? "PUESTA (sacar)" : "Poner";
                g.drawString(tg, (float) (r.getCenterX() - g.getFontMetrics().stringWidth(tg) / 2.0), (float) (r.getY() + 115 + dy));
            }
        }
    }

    // ---- HUD

    private void barra(Graphics2D g, double x, double y, double w, double h, double v, String etiqueta, Color c1, Color c2) {
        boolean bajo = v <= 20;
        double pulso = bajo ? 0.5 + 0.5 * Math.sin(t * 9) : 0;
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString(etiqueta, (float) x, (float) (y - 4));
        String num = String.valueOf((int) Math.round(v));
        g.drawString(num, (float) (x + w - g.getFontMetrics().stringWidth(num)), (float) (y - 4));
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        double ww = Math.max(h, w * Math.max(0, Math.min(100, v)) / 100.0);
        Color a = bajo ? PouArte.mezclar(c1, new Color(0xFF3B3B), pulso) : c1;
        Color b = bajo ? PouArte.mezclar(c2, new Color(0xB00020), pulso) : c2;
        g.setPaint(new GradientPaint(0, (float) y, a, 0, (float) (y + h), b));
        if (v > 0.5) g.fill(new RoundRectangle2D.Double(x, y, ww, h, h, h));
        g.setColor(new Color(255, 255, 255, 70));
        if (v > 0.5) g.fill(new RoundRectangle2D.Double(x + 3, y + 2, Math.max(0, ww - 6), h / 3, h, h));
        g.setColor(new Color(255, 255, 255, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
    }

    private void dibujarHud(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(34, 22, 56, 235), 0, HUD_H, new Color(34, 22, 56, 190)));
        g.fillRect(0, 0, W, HUD_H);
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(0, HUD_H - 2, W, 2);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString(pou.getNombre(), 16, 30);

        String et; Color col;
        if (superT > 0) { et = "SUPER POU"; col = new Color(0xFFC62E); }
        else switch (pou.getEstado()) {
            case SANO: et = "SANO"; col = new Color(0x4CD97B); break;
            case ENFERMO: et = "ENFERMO"; col = new Color(0xB5D334); break;
            case DORMIDO: et = "DORMIDO"; col = new Color(0x8EA2FF); break;
            default: et = "MUERTO"; col = new Color(0xB0B0B0);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        int w = g.getFontMetrics().stringWidth(et) + 18;
        g.setColor(col);
        g.fill(new RoundRectangle2D.Double(16, 42, w, 20, 20, 20));
        g.setColor(new Color(30, 20, 40));
        g.drawString(et, 25, 56);

        barra(g, 230, 22, 165, 14, dispH, "HAMBRE", new Color(0xFFB347), new Color(0xE67E22));
        barra(g, 405, 22, 165, 14, dispI, "HIGIENE", new Color(0x6BD6FF), new Color(0x2E9BD6));
        barra(g, 580, 22, 165, 14, dispS, "SUEÑO", new Color(0xC3B1FF), new Color(0x7E5BEF));

        // medidor de cariño
        double ca = carino;
        boolean lleno = ca >= 1 && superT <= 0;
        double px0 = 230, py0 = 46, pw = 515, ph = 16;
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new RoundRectangle2D.Double(px0, py0, pw, ph, ph, ph));
        if (ca > 0.01) {
            Color a = superT > 0 ? new Color(0xFFE066) : new Color(0xFF8FB1);
            Color b = superT > 0 ? new Color(0xFF9F1C) : new Color(0xFF4F7B);
            g.setPaint(new GradientPaint((float) px0, 0, a, (float) (px0 + pw), 0, b));
            g.fill(new RoundRectangle2D.Double(px0, py0, Math.max(ph, pw * ca), ph, ph, ph));
        }
        if (lleno) {
            double p = 0.5 + 0.5 * Math.sin(t * 8);
            g.setColor(new Color(255, 230, 120, (int) (140 * p)));
            g.setStroke(new BasicStroke(3.5f));
            g.draw(new RoundRectangle2D.Double(px0 - 2, py0 - 2, pw + 4, ph + 4, ph + 4, ph + 4));
        }
        g.setColor(new Color(255, 255, 255, 160));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(px0, py0, pw, ph, ph, ph));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        String tx = superT > 0 ? "¡SUPER POU ACTIVO! " + (int) Math.ceil(superT) + "s  (click = lanzar estrellas)"
                : lleno ? "¡CARIÑO AL MÁXIMO! Presioná T para transformar" : "CARIÑO: acariciá al Pou y jugá al vóley para llenarlo";
        g.drawString(tx, (float) (px0 + pw / 2 - g.getFontMetrics().stringWidth(tx) / 2.0), (float) (py0 + 12));
        g.setColor(new Color(255, 120, 160));
        g.fill(AffineTransform.getTranslateInstance(px0 - 10, py0 + 8).createTransformedShape(Particulas.corazon(7)));

        // monedas
        Iconos.moneda(g, 790, 38, 17, false, t * 2.5);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(new Color(0xFFE066));
        g.drawString(String.valueOf((int) Math.round(dispSaldo)), 816, 47);

        if (enfermo() && !muerto()) {
            long seg = service.segundosRestantesEnfermo(pou);
            double p = 0.5 + 0.5 * Math.sin(t * (seg < 30 ? 12 : 6));
            String s = "¡ENFERMO! Tiempo para curarlo: " + (seg / 60) + ":" + String.format("%02d", seg % 60);
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            int ws = g.getFontMetrics().stringWidth(s) + 30;
            g.setColor(new Color(180, 30, 30, (int) (170 + 70 * p)));
            g.fill(new RoundRectangle2D.Double(W / 2.0 - ws / 2.0, HUD_H + 6, ws, 28, 28, 28));
            g.setColor(Color.WHITE);
            g.drawString(s, (float) (W / 2.0 - ws / 2.0 + 15), HUD_H + 25);
        }
    }

    private void dibujarToasts(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        double y = HUD_H + (enfermo() ? 44 : 10);
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast tt = toasts.get(i);
            double a = Math.min(1, tt.vida * 2.5);
            double entra = Math.min(1, (tt.tipo == INFO ? 4.5 : 6) - tt.vida) * 6;
            entra = Math.min(1, entra);
            String txt = tt.txt;
            int maxW = 760;
            while (fm.stringWidth(txt) > maxW) txt = txt.substring(0, txt.length() - 4) + "...";
            int w = fm.stringWidth(txt) + 28;
            Color bg = tt.tipo == ERROR ? new Color(150, 40, 50) : tt.tipo == ALERTA ? new Color(170, 120, 20) : new Color(40, 30, 70);
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, Math.min(1, a))));
            double yy = y - (1 - entra) * 14;
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(new RoundRectangle2D.Double(W / 2.0 - w / 2.0 + 2, yy + 3, w, 28, 28, 28));
            g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 235));
            g.fill(new RoundRectangle2D.Double(W / 2.0 - w / 2.0, yy, w, 28, 28, 28));
            g.setColor(Color.WHITE);
            g.drawString(txt, (float) (W / 2.0 - w / 2.0 + 14), (float) (yy + 19));
            g.setComposite(AlphaComposite.SrcOver);
            y += 34;
        }
    }

    private void dibujarNav(Graphics2D g) {
        g.setPaint(new GradientPaint(0, NAV_Y, new Color(52, 34, 86), 0, H, new Color(30, 20, 52)));
        g.fillRect(0, NAV_Y, W, H - NAV_Y);
        g.setColor(new Color(255, 255, 255, 50));
        g.fillRect(0, NAV_Y, W, 2);
        for (int i = 0; i < 9; i++) {
            Rectangle2D r = navRect(i);
            boolean sinAcceso = muerto() || (duerme() && i != 0 && i != 3 && i != 7 && i != 8);
            boolean activo = (i == 0 && escena == Escena.SALA) || (i == 1 && escena == Escena.COCINA) || (i == 2 && escena == Escena.BANO)
                    || (i == 4 && escena == Escena.ROPERO) || (i == 5 && escena == Escena.FARMACIA) || (i == 7 && estadoVisible);
            double p = navPulso[i];
            double dy = -3 * p;
            Color fondo = activo ? new Color(0xFFC247) : PouArte.mezclar(new Color(0x5B4396), new Color(0x7E63D8), p);
            if (sinAcceso) fondo = new Color(0x3C3052);
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new RoundRectangle2D.Double(r.getX() + 2, r.getY() + 4, r.getWidth(), r.getHeight(), 18, 18));
            g.setColor(fondo);
            g.fill(new RoundRectangle2D.Double(r.getX(), r.getY() + dy, r.getWidth(), r.getHeight(), 18, 18));
            g.setColor(new Color(255, 255, 255, 45));
            g.fill(new RoundRectangle2D.Double(r.getX() + 4, r.getY() + dy + 3, r.getWidth() - 8, 12, 12, 12));
            Color ic = sinAcceso ? new Color(0x7B6E94) : activo ? new Color(0x4A2C12) : Color.WHITE;
            int icono = i;
            if (i == 3) icono = duerme() ? 4 : 3;
            else if (i >= 4) icono = i + 1;
            Iconos.nav(g, icono, r.getCenterX(), r.getY() + 28 + dy + (p * -1), 30, ic);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            String txt = i == 3 && duerme() ? "Despertar" : NAV_TXT[i];
            g.setColor(ic);
            g.drawString(txt, (float) (r.getCenterX() - g.getFontMetrics().stringWidth(txt) / 2.0), (float) (r.getY() + 58 + dy));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(sinAcceso ? new Color(0x7B6E94) : new Color(255, 255, 255, activo ? 170 : 110));
            g.drawString(NAV_TECLA[i], (float) (r.getX() + 8), (float) (r.getY() + 14 + dy));
        }
    }

    private void dibujarEstado(Graphics2D g) {
        g.setColor(new Color(10, 5, 30, 150));
        g.fillRect(0, 0, W, H);
        double x = 200, y = 100, w = 560, h = 380;
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new RoundRectangle2D.Double(x + 5, y + 8, w, h, 28, 28));
        g.setColor(new Color(0xFFF6E6));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 28, 28));
        g.setColor(new Color(0x6A4420));
        g.setStroke(new BasicStroke(4f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 28, 28));
        // mini pou
        PouArte.Pose mp = new PouArte.Pose();
        mp.cara = muerto() ? PouArte.Cara.MUERTO : duerme() ? PouArte.Cara.DORMIDO : enfermo() ? PouArte.Cara.ENFERMO : PouArte.Cara.FELIZ;
        mp.enfermo = enfermo() ? 1 : 0;
        mp.sucio = pose.sucio;
        mp.miraX = Math.sin(t) * 0.5;
        mp.sy = 1 + Math.sin(t * 2.3) * 0.016;
        mp.color = pose.color;
        PouArte.dibujar(g, x + 120, y + 250, 0.85, ropa, mp);
        g.setColor(new Color(0x4A2C12));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(pou.getNombre() + " - " + pou.getEstado(), (float) (x + 230), (float) (y + 52));
        barra2(g, x + 230, y + 96, 290, "Hambre", pou.getHambre(), new Color(0xFFB347));
        barra2(g, x + 230, y + 142, 290, "Higiene", pou.getHigiene(), new Color(0x4DB5E8));
        barra2(g, x + 230, y + 188, 290, "Sueño", pou.getSueño(), new Color(0x8E6BEF));
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(0x4A2C12));
        g.drawString("Monedas: " + (int) pou.getSaldo(), (float) (x + 230), (float) (y + 244));
        StringBuilder r = new StringBuilder();
        for (Prenda p : mias) if (p.isEstaPuesta()) r.append(r.length() > 0 ? ", " : "").append(p.getNombre());
        g.drawString("Ropa puesta: " + (r.length() > 0 ? r : "nada"), (float) (x + 230), (float) (y + 274));
        if (enfermo()) {
            g.setColor(new Color(0xB00020));
            g.drawString("TIEMPO PARA CURARLO: " + service.segundosRestantesEnfermo(pou) + "s", (float) (x + 230), (float) (y + 304));
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(0x7A5A3A));
        g.drawString("Click o cualquier tecla para cerrar", (float) (x + w / 2 - 100), (float) (y + h - 18));
    }

    private void barra2(Graphics2D g, double x, double y, double w, String et, int v, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(0x4A2C12));
        g.drawString(et + "  " + v, (float) x, (float) (y - 5));
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new RoundRectangle2D.Double(x, y, w, 16, 16, 16));
        g.setColor(c);
        if (v > 0) g.fill(new RoundRectangle2D.Double(x, y, Math.max(16, w * v / 100.0), 16, 16, 16));
        g.setColor(new Color(0x6A4420));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new RoundRectangle2D.Double(x, y, w, 16, 16, 16));
    }

    private void dibujarGameOver(Graphics2D g) {
        double a = Math.min(1, (muertoT - 3.2) * 1.5);
        g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, a)));
        g.setColor(new Color(10, 5, 20, 200));
        g.fillRect(0, 0, W, H);
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        String s = "GAME OVER";
        g.setColor(new Color(0xFF5A5A));
        g.drawString(s, W / 2 - g.getFontMetrics().stringWidth(s) / 2, 250);
        long minutos = Duration.between(pou.getFechaCreacion(), LocalDateTime.now()).toMinutes();
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        String m = pou.getNombre() + " vivió " + minutos + " minuto/s y terminó con " + (int) pou.getSaldo() + " monedas.";
        g.drawString(m, W / 2 - g.getFontMetrics().stringWidth(m) / 2, 305);
        Rectangle2D r = verRect();
        boolean h = r.contains(mouseX, mouseY);
        g.setColor(h ? new Color(0xFFD866) : new Color(0xFFC247));
        g.fill(new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 26, 26));
        g.setColor(new Color(0x4A2C12));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        String b = "Volver al menú";
        g.drawString(b, (float) (r.getCenterX() - g.getFontMetrics().stringWidth(b) / 2.0), (float) (r.getY() + 33));
        g.setComposite(AlphaComposite.SrcOver);
    }
}
