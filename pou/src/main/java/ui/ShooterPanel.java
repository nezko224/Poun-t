package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import static ui.ShooterArte.*;

/**
 * "Operacion Mugre": shooter en primera persona estilo clasico (raycasting), version para todo publico.
 * Pou limpia su casa con el Burbujero 3000: los germenes se convierten en burbujas, no hay sangre ni violencia.
 * Cinco capitulos (La Casa Invadida, La Cocina Caotica, El Desvan Polvoriento, El Sotano Humedo y El Gran Desague),
 * cada uno con monstruos, mecanicas y jefe propios.  El progreso se guarda con java.util.prefs.
 * No toca model / dao / service.
 */
public class ShooterPanel extends JPanel {

    /** Para probar: deja elegir cualquier capitulo sin haber terminado el anterior. */
    public static final boolean TODOS_DESBLOQUEADOS = false;

    private static final int W = 960, H = 640, VW = 480, VH = 272, HUDY = 544;
    private static final double PLANO = 0.85;

    // tipos de cosas: 0..ULT_ENEMIGO son enemigos
    private static final int MUGRITO = 0, TIRADOR = 1, REY = 2, MIGAJIN = 3, TOMATON = 4, GRASON = 5, PELUSON = 6,
            TELARANIN = 7, MADAME = 8, GOTERA = 9, MOHO = 10, CAPITAN = 11, GELATINA = 12, SOMBRA = 13, MADRE = 14,
            ORBE = 15, CAPULLO = 16, ULT_ENEMIGO = 16;
    private static final int JABON = 17, BURBUJAS = 18, ESTRELLA = 19, LLAVE = 20, BGRANDE = 21, SUPERJ = 22,
            ESCUDO = 23, LINTERNA = 24, TURBO = 25;
    private static final int BALA = 26, MUGRE = 27, POP = 28, TOMATE = 29, RED = 30, ANCLA = 31, ONDA = 32,
            ESPORABOLA = 33, GOTAFIJA = 34, CHARCO = 35, NUBE = 36, DUCHA = 37, LLUVIA = 38;
    private static final int QUIETO = 0, PERSIGUE = 1, MURIENDO = 2;

    private enum Estado { SELECCION, INTRO, CAIDA, JUGANDO, PAUSA, MUERTO, FIN }

    private static boolean esEnemigo(int tipo) { return tipo <= ULT_ENEMIGO; }
    private static boolean esItem(int tipo) { return tipo >= JABON && tipo <= TURBO; }

    private static final class Cosa {
        int tipo, estado, sub, fase, tam, n;
        double x, y, vx, vy, vida, t, cd, flash, alt, esc, hp, maxHp, danio, dist, disp, t2, t3, a1, a2;
        boolean activa = true, cuenta, guardia, inmune, x2, inerte, alto;
    }

    private final Navegador nav;
    private final PanelJuego origen;
    private final PouArte.Ropa ropa;
    private final PouArte.Pose rostro = new PouArte.Pose();
    private final Particulas fx = new Particulas();
    private final javax.swing.Timer timer;
    private final Preferences prefs = Preferences.userNodeForPackage(ShooterPanel.class);
    private long ultimoNanos;
    private double t;

    private final BufferedImage buf = new BufferedImage(VW, VH, BufferedImage.TYPE_INT_RGB);
    private final int[] pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
    private final double[] zbuf = new double[VW];

    // nivel actual
    private ShooterNiveles.Nivel niv;
    private int cap = 1, sel = 0;
    private int[][] mapa, muroHp, pisoG, techoG;
    private boolean[][] secreto, luzCelda;
    private final List<Cosa> cosas = new ArrayList<>();
    private final List<int[]> hornallas = new ArrayList<>(), rejas = new ArrayList<>();
    private Cosa jefe;
    private final boolean[] teclas = new boolean[256];
    private boolean disparando;

    private Estado estado = Estado.SELECCION;
    private double px, py, dirX, dirY, plX, plY, ang;
    private double limpieza, bobT, cdArma, disparoT, golpeT, brilloT, shakeT, lentoT, escudoEsp, charcoLento;
    private int municion, bgrande;
    private boolean turbo, linterna, tieneLlave, jefeMuerto;
    private int muertes, totEn, objetos, totObj, secretos, totSec;
    private double tiempo, finT, caidaT, finalT;
    private String msg = ""; private double msgT;

    // mecanicas de capitulo
    private int valvulas, locksPlaced, puertaX = -1, puertaY = -1, ventX, ventY;
    private double valvProg, cadenaCd, puertaAnim = -1, apagon, duX, duY, ducha;
    private boolean ventanaRota, girando;

    // resultados
    private int pEn, pObj, pSec, finPag;
    private String rango = "D";
    private boolean monedasDadas;
    private int monedasSesion, capSesion;

    private Robot robot;
    private boolean mouseLook;
    private boolean mouseUsuario = true;      // el mouse gira la camara; Tab lo apaga/prende
    private boolean avisoMouse;
    private int ultimoMX = Integer.MIN_VALUE;
    private double avisoBurbT;

    public ShooterPanel(Navegador nav, PanelJuego origen, PouArte.Ropa ropa) {
        this.nav = nav;
        this.origen = origen;
        this.ropa = ropa;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { soltarMouse(); disparando = false; }
        });
        niv = ShooterNiveles.get(1);
        cargar();
        sel = siguienteSinTerminar() - 1;
        dorado = trajeDorado();
        estado = Estado.SELECCION;

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { tecla(e.getKeyCode(), true); }
            @Override public void keyReleased(KeyEvent e) { tecla(e.getKeyCode(), false); }
        });
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (estado == Estado.SELECCION) clickSeleccion(e.getX(), e.getY());
                else if (estado == Estado.INTRO) empezar();
                else if (estado == Estado.MUERTO) { cargar(); estado = Estado.JUGANDO; }
                else if (estado == Estado.FIN) avanzarFin();
                else if (estado == Estado.PAUSA) estado = Estado.JUGANDO;
                else if (estado == Estado.JUGANDO) disparando = true;
            }
            @Override public void mouseReleased(MouseEvent e) { disparando = false; }
            @Override public void mouseMoved(MouseEvent e) { girarConMouse(e); }
            @Override public void mouseDragged(MouseEvent e) { girarConMouse(e); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        ultimoNanos = System.nanoTime();
        timer = new javax.swing.Timer(16, e -> {
            long ahora = System.nanoTime();
            double dt = Math.min(0.05, (ahora - ultimoNanos) / 1e9);
            ultimoNanos = ahora;
            actualizar(dt);
            repaint();
        });
    }

    @Override public void addNotify() { super.addNotify(); SwingUtilities.invokeLater(this::requestFocusInWindow); }
    public void iniciar() { ultimoNanos = System.nanoTime(); timer.start(); requestFocusInWindow(); }
    public void detener() { timer.stop(); soltarMouse(); }

    private void salir(boolean ignorado) {
        timer.stop();
        soltarMouse();
        nav.volverDelShooter(origen, monedasSesion, monedasSesion > 0, capSesion);
    }

    // ================================================================== progreso (java.util.prefs)

    private String rangoGuardado(int n) { try { return prefs.get("rango." + n, ""); } catch (Exception ex) { return ""; } }

    private void guardarRango(int n, String r) {
        try {
            String viejo = rangoGuardado(n);
            if (valorRango(r) > valorRango(viejo)) prefs.put("rango." + n, r);
        } catch (Exception ignorado) { }
    }

    private static int valorRango(String r) {
        switch (r) { case "S": return 5; case "A": return 4; case "B": return 3; case "C": return 2; case "D": return 1; default: return 0; }
    }

    private boolean desbloqueado(int n) { return TODOS_DESBLOQUEADOS || n == 1 || !rangoGuardado(n - 1).isEmpty(); }

    private int siguienteSinTerminar() {
        for (int n = 1; n <= ShooterNiveles.TOTAL; n++) if (rangoGuardado(n).isEmpty() && desbloqueado(n)) return n;
        return ShooterNiveles.TOTAL;
    }

    private boolean trajeDorado() {
        for (int n = 1; n <= ShooterNiveles.TOTAL; n++) if (!rangoGuardado(n).equals("S")) return false;
        return true;
    }

    private String rangoGeneral() {
        double s = 0;
        for (int n = 1; n <= ShooterNiveles.TOTAL; n++) s += Math.max(1, valorRango(rangoGuardado(n)));
        s /= ShooterNiveles.TOTAL;
        return s >= 4.5 ? "S" : s >= 3.5 ? "A" : s >= 2.5 ? "B" : s >= 1.5 ? "C" : "D";
    }

    private int coronas() {
        if (cap <= 1) return 0;
        if (cap >= 5) return 3;
        return niv.coronaBase + (jefeMuerto ? 1 : 0);
    }

    // ================================================================== nivel

    private void setAng(double a) {
        ang = a; dirX = Math.cos(a); dirY = Math.sin(a);
        plX = -dirY * PLANO; plY = dirX * PLANO;
    }

    private void elegir(int n) {
        cap = n;
        niv = ShooterNiveles.get(n);
        cargar();
        finPag = 0;
        estado = Estado.INTRO;
        Sonido.pop();
    }

    private static boolean dentro(int[] r, int x, int y) { return x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]; }

    private void cargar() {
        String[] M = niv.mapa;
        int hh = M.length, ww = M[0].length();
        mapa = new int[hh][ww]; secreto = new boolean[hh][ww]; muroHp = new int[hh][ww]; luzCelda = new boolean[hh][ww];
        pisoG = new int[hh][ww]; techoG = new int[hh][ww];
        for (int y = 0; y < hh; y++) for (int x = 0; x < ww; x++) { pisoG[y][x] = niv.pisoDef; techoG[y][x] = niv.techoDef; }
        for (int[] r : niv.regiones) for (int y = Math.max(0, r[1]); y <= Math.min(hh - 1, r[3]); y++) for (int x = Math.max(0, r[0]); x <= Math.min(ww - 1, r[2]); x++) {
            pisoG[y][x] = r[4]; techoG[y][x] = r[5];
        }
        cosas.clear(); hornallas.clear(); rejas.clear(); jefe = null;
        fx.limpiar();
        muertes = objetos = secretos = 0;
        totEn = totObj = totSec = 0;
        ventX = ventY = 0; duX = duY = 0;
        for (int y = 0; y < hh; y++) for (int x = 0; x < ww; x++) {
            char ch = M[y].charAt(x);
            double cx = x + 0.5, cy = y + 0.5;
            int v = 0;
            if (ch >= '1' && ch <= '9') v = niv.paredes[ch - '0'];
            else switch (ch) {
                case 'D': v = niv.texPuerta; break;
                case 'K': v = niv.texCandado; break;
                case 'X': v = niv.texExitOff; break;
                case 'S': v = niv.secretTex[0]; secreto[y][x] = true; totSec++; break;
                case 'Y': v = niv.secretTex[1]; secreto[y][x] = true; totSec++; break;
                case 'Z': v = niv.secretTex[2]; secreto[y][x] = true; totSec++; break;
                case 'F': v = T_HORN_DIN; hornallas.add(new int[]{x, y}); break;
                case 'V': v = T_VALV_OFF; break;
                case 'H': v = T_CADENA; break;
                case 'r': v = T_REJA; rejas.add(new int[]{x, y}); break;
                case 'w': v = T_TELA; muroHp[y][x] = 2; break;
                case 'N': v = T_VENTANA; ventX = x - 1; ventY = y; break;
                case 'A': v = T_P3_0; puertaX = x; puertaY = y; break;
                case 'P': px = cx; py = cy; break;
                case '*': luzCelda[y][x] = true; break;
                case 'g': nuevoEnemigo(MUGRITO, cx, cy, true); break;
                case 't': nuevoEnemigo(TIRADOR, cx, cy, true); break;
                case 'R': jefe = nuevoEnemigo(REY, cx, cy, true); break;
                case 'm': {
                    int nn = 5 + (int) (Math.random() * 2);
                    for (int i = 0; i < nn; i++) {
                        double a = i * Math.PI * 2 / nn;
                        nuevoEnemigo(MIGAJIN, cx + Math.cos(a) * 0.42, cy + Math.sin(a) * 0.42, true);
                    }
                    break;
                }
                case 'o': nuevoEnemigo(TOMATON, cx, cy, true); break;
                case 'G': jefe = nuevoEnemigo(GRASON, cx, cy, true); break;
                case 'l': nuevoEnemigo(PELUSON, cx, cy, true); break;
                case 'a': nuevoEnemigo(TELARANIN, cx, cy, true); break;
                case 'M': jefe = nuevoEnemigo(MADAME, cx, cy, true); break;
                case 'c': { Cosa c = nuevoEnemigo(CAPULLO, cx, cy, false); c.inerte = true; break; }
                case 'q': nuevoEnemigo(GOTERA, cx, cy, true); break;
                case 'd': { Cosa c = nuevoItem(GOTAFIJA, cx, cy); c.t = Math.random() * 3.2; break; }
                case 'h': nuevoEnemigo(MOHO, cx, cy, true); break;
                case 'C': jefe = nuevoEnemigo(CAPITAN, cx, cy, true); break;
                case 'n': nuevoEnemigo(GELATINA, cx, cy, true); break;
                case 'u': nuevoEnemigo(SOMBRA, cx, cy, true); break;
                case 'W': jefe = nuevoEnemigo(MADRE, cx, cy, true); break;
                case 'U': nuevoItem(DUCHA, cx, cy); duX = cx; duY = cy; break;
                case 'j': nuevoItem(JABON, cx, cy).cuenta = true; totObj++; break;
                case 'b': nuevoItem(BURBUJAS, cx, cy).cuenta = true; totObj++; break;
                case 'e': nuevoItem(ESTRELLA, cx, cy).cuenta = true; totObj++; break;
                case 'B': nuevoItem(BGRANDE, cx, cy).cuenta = true; totObj++; break;
                case 'J': nuevoItem(SUPERJ, cx, cy).cuenta = true; totObj++; break;
                case 'E': nuevoItem(ESCUDO, cx, cy).cuenta = true; totObj++; break;
                case 'L': nuevoItem(LINTERNA, cx, cy).cuenta = true; totObj++; break;
                case 'Q': nuevoItem(TURBO, cx, cy).cuenta = true; totObj++; break;
                case 'k': nuevoItem(LLAVE, cx, cy); break;
                default: break;
            }
            mapa[y][x] = v;
        }
        // las gelatinas de la pantalla suman 7 vencidos cada una (1 grande + 2 medianas + 4 chiquitas)
        int gel = 0;
        for (Cosa c : cosas) if (c.tipo == GELATINA) gel++;
        totEn += gel * 6;
        if (jefe != null) jefe.maxHp = jefe.hp;
        setAng(niv.angIni);
        apagonOn = false;
        limpieza = 100; municion = 60;
        tieneLlave = false; jefeMuerto = false;
        linterna = false; turbo = false; bgrande = 0; escudoEsp = 0; lentoT = 0; shakeT = 0; charcoLento = 0;
        valvulas = 0; valvProg = 0; cadenaCd = 0; locksPlaced = 0; puertaAnim = -1; apagon = 0; ventanaRota = false; girando = false;
        ducha = 0; finalT = 0; caidaT = 0;
        tiempo = 0; cdArma = 0; disparoT = golpeT = brilloT = 0;
        msg = ""; msgT = 0; monedasDadas = false;
        java.util.Arrays.fill(teclas, false);
        disparando = false;
    }

    private Cosa nuevoEnemigo(int tipo, double x, double y, boolean cuenta) {
        Cosa c = new Cosa();
        c.tipo = tipo; c.x = x; c.y = y; c.estado = QUIETO; c.cuenta = cuenta;
        switch (tipo) {
            case TIRADOR: c.hp = 4; break;
            case REY: c.hp = 30; break;
            case MIGAJIN: c.hp = 1; break;
            case TOMATON: c.hp = 5; break;
            case GRASON: c.hp = 45; c.fase = 1; break;
            case TELARANIN: c.hp = 4; c.alto = true; break;
            case MADAME: c.hp = 60; c.fase = 1; c.alto = true; break;
            case GOTERA: c.hp = 2; c.alto = true; c.a1 = x; c.a2 = y; break;
            case MOHO: c.hp = 7; break;
            case CAPITAN: c.hp = 75; c.fase = 1; c.inmune = true; break;
            case GELATINA: c.hp = 6; c.tam = 2; break;
            case MADRE: c.hp = 100; c.fase = 1; break;
            case ORBE: c.hp = 6; break;
            case CAPULLO: c.hp = 3; break;
            default: c.hp = 3;
        }
        c.maxHp = c.hp;
        boolean clasico = tipo == MUGRITO || tipo == TIRADOR || tipo == MIGAJIN || tipo == TOMATON || tipo == PELUSON
                || tipo == TELARANIN || tipo == GOTERA || tipo == MOHO;
        if (niv != null && niv.guardiaX >= 0 && x >= niv.guardiaX && clasico && cuenta) {
            c.guardia = true; c.hp = Math.ceil(c.hp * 1.5); c.maxHp = c.hp;
        }
        c.cd = 0.6 + Math.random();
        c.t = Math.random() * 3;
        cosas.add(c);
        if (cuenta) totEn++;
        return c;
    }

    /** Enemigo invocado por un jefe: no suma al porcentaje de gérmenes del capítulo. */
    private Cosa invocar(int tipo, double x, double y) {
        Cosa c = new Cosa();
        c.tipo = tipo; c.x = x; c.y = y; c.estado = PERSIGUE; c.cuenta = false;
        c.hp = tipo == MIGAJIN ? 1 : tipo == MOHO ? 7 : tipo == GELATINA ? 6 : tipo == TELARANIN ? 4 : 3;
        c.tam = tipo == GELATINA ? 2 : 0;
        if (tipo == TELARANIN) { c.sub = 2; c.alto = false; }
        c.maxHp = c.hp; c.cd = 0.8 + Math.random(); c.t = Math.random() * 3;
        cosas.add(c);
        return c;
    }

    private Cosa nuevoItem(int tipo, double x, double y) {
        Cosa c = new Cosa();
        c.tipo = tipo; c.x = x; c.y = y; c.t = Math.random() * 6;
        cosas.add(c);
        return c;
    }

    private void empezar() {
        Sonido.pop();
        if (cap == 5) { estado = Estado.CAIDA; caidaT = 0; }
        else estado = Estado.JUGANDO;
    }

    private void avanzarFin() {
        if (finT < 4.6) { finT = 4.6; return; }
        if (cap == 5 && finPag == 0) { finPag = 1; finT = 5; Sonido.ganar(); return; }
        irASeleccion();
    }

    private void irASeleccion() {
        estado = Estado.SELECCION;
        sel = Math.min(ShooterNiveles.TOTAL, cap + 1) - 1;
        if (cap == ShooterNiveles.TOTAL) sel = 0;
        if (!desbloqueado(sel + 1)) sel = cap - 1;
    }

    // ================================================================== entrada

    private boolean k(int code) { return teclas[code]; }

    private Rectangle2D rCarta(int i) { return new Rectangle2D.Double(30 + i * 180, 190, 168, 290); }

    private void clickSeleccion(int mx, int my) {
        for (int i = 0; i < ShooterNiveles.TOTAL; i++) {
            if (rCarta(i).contains(mx, my)) {
                if (sel == i && desbloqueado(i + 1)) elegir(i + 1);
                else { sel = i; Sonido.pop(); }
                return;
            }
        }
    }

    private void tecla(int code, boolean abajo) {
        if (code < 256) teclas[code] = abajo;
        if (!abajo) return;
        if (code == KeyEvent.VK_M) { Sonido.setActivo(!Sonido.isActivo()); return; }
        switch (estado) {
            case SELECCION:
                if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) { sel = Math.max(0, sel - 1); Sonido.pop(); }
                else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) { sel = Math.min(ShooterNiveles.TOTAL - 1, sel + 1); Sonido.pop(); }
                else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
                    if (desbloqueado(sel + 1)) elegir(sel + 1); else { Sonido.error(); }
                } else if (code == KeyEvent.VK_ESCAPE) salir(false);
                break;
            case INTRO:
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) empezar();
                else if (code == KeyEvent.VK_ESCAPE) estado = Estado.SELECCION;
                break;
            case CAIDA:
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) caidaT = 99;
                break;
            case JUGANDO:
                if (code == KeyEvent.VK_ESCAPE) { estado = Estado.PAUSA; soltarMouse(); }
                else if (code == KeyEvent.VK_P) estado = Estado.PAUSA;
                else if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) usar();
                else if (code == KeyEvent.VK_TAB) alternarMouse();
                break;
            case PAUSA:
                if (code == KeyEvent.VK_ESCAPE) { estado = Estado.SELECCION; sel = cap - 1; }
                else if (code == KeyEvent.VK_P || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) estado = Estado.JUGANDO;
                break;
            case MUERTO:
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_R) { cargar(); estado = Estado.JUGANDO; }
                else if (code == KeyEvent.VK_ESCAPE) { estado = Estado.SELECCION; sel = cap - 1; }
                break;
            case FIN:
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ESCAPE) avanzarFin();
                break;
        }
    }

    /** Tab: el jugador prende o apaga el giro con el mouse (por defecto esta prendido). */
    private void alternarMouse() {
        mouseUsuario = !mouseUsuario;
        mensaje(mouseUsuario ? "Mouse para girar la cámara: ACTIVADO (Tab para sacarlo)" : "Mouse para girar la cámara: DESACTIVADO (Tab para activarlo)");
        if (!mouseUsuario) soltarMouse();
    }

    /** El mouse se engancha solo mientras se juega y se suelta en pausa, menus y pantallas finales. */
    private void sincronizarMouse() {
        boolean querer = mouseUsuario && estado == Estado.JUGANDO && isFocusOwner();
        if (querer && !mouseLook) activarMouse();
        else if (!querer && mouseLook) soltarMouse();
    }

    private void activarMouse() {
        mouseLook = true;
        ultimoMX = Integer.MIN_VALUE;
        try { if (robot == null) robot = new Robot(); } catch (Exception | Error ex) { robot = null; }
        if (robot != null) {
            setCursor(Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "vacio"));
            centrarMouse();
        }
        if (!avisoMouse) { avisoMouse = true; mensaje("Mové el mouse para girar la cámara (Tab lo activa o desactiva)"); }
    }

    private void soltarMouse() {
        mouseLook = false;
        ultimoMX = Integer.MIN_VALUE;
        setCursor(Cursor.getDefaultCursor());
    }

    private void centrarMouse() {
        if (robot == null || !isShowing()) return;
        Point p = getLocationOnScreen();
        robot.mouseMove(p.x + getWidth() / 2, p.y + getHeight() / 2);
    }

    private void girarConMouse(MouseEvent e) {
        if (!mouseLook || estado != Estado.JUGANDO || girando) return;
        int dx;
        if (robot != null) dx = e.getX() - getWidth() / 2;
        else { dx = ultimoMX == Integer.MIN_VALUE ? 0 : e.getX() - ultimoMX; ultimoMX = e.getX(); }
        if (dx == 0) return;
        dx = Math.max(-120, Math.min(120, dx));
        setAng(ang + dx * 0.0035);
        if (robot != null) centrarMouse();
    }

    private void mensaje(String s) { msg = s; msgT = 3.2; }

    /** Primera celda solida que Pou tiene adelante (a menos de 1.5), o null. */
    private int[] celdaFrente() {
        for (double s = 0.4; s <= 1.5; s += 0.2) {
            int cx = (int) (px + dirX * s), cy = (int) (py + dirY * s);
            if (cy < 0 || cx < 0 || cy >= mapa.length || cx >= mapa[0].length) return null;
            if (mapa[cy][cx] > 0) return new int[]{cx, cy};
        }
        return null;
    }

    private void usar() {
        int[] f = celdaFrente();
        if (f == null) return;
        int cx = f[0], cy = f[1], v = mapa[cy][cx];
        if (secreto[cy][cx]) {
            secreto[cy][cx] = false; mapa[cy][cx] = 0; secretos++;
            mensaje("¡Encontraste un secreto!");
            Sonido.magia(); fx.estrellas(W / 2.0, 300, 10);
        } else if (v == niv.texPuerta) { mapa[cy][cx] = 0; Sonido.boing(); mensaje("Puerta abierta"); }
        else if (v == niv.texCandado && niv.texCandado != 0) {
            if (!tieneLlave) { Sonido.error(); mensaje(niv.msgLlave); }
            else if (niv.requiereLinterna && !linterna) { Sonido.error(); mensaje(niv.msgOscuro); }
            else { mapa[cy][cx] = 0; Sonido.magia(); mensaje(niv.msgPuertaOk); }
        } else if (v == niv.texExitOff && niv.texExitOff != 0) {
            if (jefeMuerto) ganar();
            else { Sonido.error(); mensaje(niv.msgSalidaOff); }
        } else if (v == T_VALV_OFF) {
            mensaje("Mantené apretada la E para girar la válvula (" + valvulas + "/3)");
        } else if (v == T_VALV_ON) {
            mensaje("Esta válvula ya está girada");
        } else if (v == T_CADENA || v == T_CADENA_REC) usarCadena();
        else if (v >= T_P3_0 && v <= T_P3_3) usarPuertaTres();
        else if (v == T_REJA) { Sonido.error(); mensaje("La reja se abre sola cuando limpies a todos los de este tramo."); }
        else if (v == T_TELA) mensaje("Una telaraña gigante: ¡reventala con burbujas!");
    }

    private void usarPuertaTres() {
        if (puertaAnim >= 0) return;
        if (coronas() < 3) { Sonido.error(); mensaje("Faltan pedazos de corona para abrir esta puerta."); return; }
        puertaAnim = 0;
        mensaje("Los pedazos de la corona se acomodan solos...");
        Sonido.magia();
    }

    private void usarCadena() {
        Cosa m = jefe;
        if (cadenaCd > 0) { Sonido.error(); mensaje("La cadena se está recargando (" + (int) Math.ceil(cadenaCd) + " s)"); return; }
        if (m == null || !m.activa || m.estado == MURIENDO || m.fase != 3) { Sonido.error(); mensaje("¡Todavía no! La ducha se usa en la última etapa."); return; }
        if (Math.hypot(m.x - duX, m.y - duY) > 2.1) { Sonido.error(); mensaje("¡Todavía no está justo abajo! Atraela al centro."); return; }
        cadenaCd = 15; ducha = 1.2; m.t3 = 6; m.flash = 0.4;
        Sonido.magia(); mensaje("¡DUCHA MAESTRA! -15");
        for (int i = 0; i < 6; i++) pop(duX + (Math.random() - 0.5) * 2, duY + (Math.random() - 0.5) * 2, 0.6, 0.2 + Math.random() * 0.7);
        fx.estrellas(W / 2.0, 250, 14);
        m.hp -= 15;
        if (m.hp <= 0) morir(m);
    }

    // ================================================================== reglas

    private static final int SOMBRAGOTA = 39;
    private boolean apagonOn;

    private boolean solido(double x, double y) {
        int cx = (int) Math.floor(x), cy = (int) Math.floor(y);
        if (cx < 0 || cy < 0 || cy >= mapa.length || cx >= mapa[0].length) return true;
        return mapa[cy][cx] > 0;
    }

    private boolean chocaCon(double x, double y, double r) {
        return solido(x - r, y - r) || solido(x + r, y - r) || solido(x - r, y + r) || solido(x + r, y + r);
    }

    private boolean veLinea(double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, d = Math.hypot(dx, dy);
        int n = (int) (d / 0.15) + 1;
        for (int i = 1; i < n; i++) if (solido(x0 + dx * i / n, y0 + dy * i / n)) return false;
        return true;
    }

    private void actualizar(double dt) {
        t += dt;
        fx.actualizar(dt);
        msgT = Math.max(0, msgT - dt);
        avisoBurbT = Math.max(0, avisoBurbT - dt);
        sincronizarMouse();
        if (estado == Estado.FIN) { finT += dt; tickFin(); return; }
        if (estado == Estado.CAIDA) {
            caidaT += dt;
            if (caidaT > 3.4) { estado = Estado.JUGANDO; Sonido.pop(); }
            return;
        }
        if (estado != Estado.JUGANDO) return;
        tiempo += dt;
        cdArma = Math.max(0, cdArma - dt);
        disparoT = Math.max(0, disparoT - dt);
        golpeT = Math.max(0, golpeT - dt);
        brilloT = Math.max(0, brilloT - dt);
        shakeT = Math.max(0, shakeT - dt);
        lentoT = Math.max(0, lentoT - dt);
        cadenaCd = Math.max(0, cadenaCd - dt);
        ducha = Math.max(0, ducha - dt);
        apagon = Math.max(0, Math.min(1, apagon + (apagonOn ? 1 : -1) * dt * 1.2));

        // valvulas: hay que mantener la E
        boolean mantiene = k(KeyEvent.VK_E) || k(KeyEvent.VK_ENTER);
        int[] fr = celdaFrente();
        girando = mantiene && fr != null && mapa[fr[1]][fr[0]] == T_VALV_OFF;
        if (girando) {
            valvProg += dt / 1.5;
            if (((int) (valvProg * 12)) % 2 == 0 && Math.random() < 0.3) Sonido.tono(150, 200, 30, 0.2);
            if (valvProg >= 1) completarValvula(fr[0], fr[1]);
        } else valvProg = 0;

        // movimiento
        double slowCharco = charcoLento; charcoLento = 0;
        double mf = (k(KeyEvent.VK_W) || k(KeyEvent.VK_UP) ? 1 : 0) - (k(KeyEvent.VK_S) || k(KeyEvent.VK_DOWN) ? 1 : 0);
        double ms = (k(KeyEvent.VK_D) ? 1 : 0) - (k(KeyEvent.VK_A) ? 1 : 0);
        double rot = ((k(KeyEvent.VK_RIGHT) ? 1 : 0) - (k(KeyEvent.VK_LEFT) ? 1 : 0)) * 2.5 * dt;
        if (girando) { mf = 0; ms = 0; rot = 0; }
        if (rot != 0) setAng(ang + rot);
        double vx = dirX * mf - dirY * ms, vy = dirY * mf + dirX * ms;
        double len = Math.hypot(vx, vy);
        boolean camina = len > 0;
        if (camina) {
            double mult = (lentoT > 0 ? 0.5 : 1.0) * (slowCharco > 0 ? 0.6 : 1.0);
            double vel = 3.2 * (k(KeyEvent.VK_SHIFT) ? 1.6 : 1.0) * mult * dt / Math.max(1, len);
            double nx = px + vx * vel, ny = py + vy * vel;
            if (!chocaCon(nx, py, 0.22)) px = nx;
            if (!chocaCon(px, ny, 0.22)) py = ny;
        }
        bobT += dt * (camina ? 9 : 2);

        if (!girando && (disparando || k(KeyEvent.VK_SPACE) || k(KeyEvent.VK_CONTROL)) && cdArma <= 0) disparar();

        // puerta de las tres cerraduras
        if (puertaAnim >= 0) {
            double antes = puertaAnim;
            puertaAnim += dt;
            for (int i = 1; i <= 3; i++) {
                if (antes < 0.7 * i && puertaAnim >= 0.7 * i) {
                    locksPlaced = i; Sonido.tono(600 + i * 200, 1200 + i * 200, 250, 0.5);
                    fx.estrellas(W / 2.0, 280, 12);
                }
            }
            if (puertaAnim >= 2.9) {
                puertaAnim = -1;
                mapa[puertaY][puertaX] = 0;
                Sonido.ganar(); mensaje("¡Se abrió la puerta! Adelante está el Trono de la Mugre");
            }
        }
        // rejas: se abren al limpiar al grupo de ese tramo
        if (!rejas.isEmpty()) abrirRejas();

        for (int i = 0; i < cosas.size(); i++) { Cosa c = cosas.get(i); if (c.activa) actualizarCosa(c, dt); }
        cosas.removeIf(c -> !c.activa);

        if (finalT > 0) {
            finalT += dt;
            if (finalT >= 5.6 && estado == Estado.JUGANDO) ganar();
        }
        if (limpieza <= 0 && estado == Estado.JUGANDO) {
            estado = Estado.MUERTO; Sonido.perder(); soltarMouse(); disparando = false;
        }
    }

    private void abrirRejas() {
        for (int[] r : new ArrayList<>(rejas)) {
            if (mapa[r[1]][r[0]] != T_REJA) { rejas.remove(r); continue; }
            boolean quedan = false;
            for (Cosa c : cosas) {
                if (!c.activa || !esEnemigo(c.tipo) || c.estado == MURIENDO || !c.cuenta) continue;
                if (c.x < r[0]) { quedan = true; break; }
            }
            if (!quedan) {
                for (int[] o : new ArrayList<>(rejas)) if (o[0] == r[0]) { mapa[o[1]][o[0]] = 0; rejas.remove(o); }
                Sonido.boing(); mensaje("¡La reja se abrió! Seguí por el túnel");
                return;
            }
        }
    }

    private void completarValvula(int cx, int cy) {
        mapa[cy][cx] = T_VALV_ON;
        valvulas++; valvProg = 0;
        Sonido.magia(); fx.estrellas(W / 2.0, 280, 10);
        for (int i = 0; i < 4; i++) pop(px + dirX * 1.0 + (Math.random() - 0.5), py + dirY * 1.0 + (Math.random() - 0.5), 0.5, 0.2 + Math.random() * 0.5);
        if (valvulas < 3) mensaje("¡Válvula girada! El agua baja y el escudo del Capitán se debilita (" + valvulas + "/3)");
        else mensaje("¡Última válvula! La sala quedó seca");
    }

    private void disparar() {
        if (municion <= 0) {                      // sin burbujas: no se dispara, solo se avisa
            cdArma = 0.35;
            if (avisoBurbT <= 0) { mensaje("¡Ya no tenés más burbujas! Buscá más para poder disparar"); Sonido.error(); avisoBurbT = 1.6; }
            return;
        }
        municion--;
        cdArma = 0.28 * (turbo ? 0.5 : 1.0);
        disparoT = 0.18;
        double dmg = 1;
        if (bgrande > 0) { dmg = 2; bgrande--; }
        Cosa b = new Cosa();
        b.tipo = BALA; b.x = px + dirX * 0.35; b.y = py + dirY * 0.35;
        b.vx = dirX * 10; b.vy = dirY * 10; b.vida = 1.6; b.alt = 0.28;
        b.esc = dmg > 1 ? 0.32 : 0.22; b.danio = dmg;
        cosas.add(b);
        Sonido.tono(turbo ? 900 : 700, turbo ? 1500 : 1150, 60, 0.35);
        for (Cosa c : cosas) {
            if (esEnemigo(c.tipo) && c.estado == QUIETO && c.tipo != PELUSON && c.tipo != TELARANIN && !c.inerte && Math.hypot(c.x - px, c.y - py) < 14) c.estado = PERSIGUE;
        }
    }

    private void danio(double n) {
        if (finalT > 0 || estado != Estado.JUGANDO) return;
        if (escudoEsp > 0) {
            double ab = Math.min(escudoEsp, n); escudoEsp -= ab; n -= ab;
            golpeT = 0.15; Sonido.tono(500, 700, 50, 0.4);
            if (n <= 0) return;
        }
        limpieza = Math.max(0, limpieza - n);
        golpeT = 0.3;
        Sonido.golpe();
    }

    private void pop(double x, double y, double esc, double alt) {
        Cosa p = new Cosa();
        p.tipo = POP; p.x = x; p.y = y; p.esc = esc; p.alt = alt;
        cosas.add(p);
    }

    private double radio(Cosa e) {
        switch (e.tipo) {
            case REY: case GRASON: case MADAME: return 0.7;
            case CAPITAN: return 0.75;
            case MADRE: return e.fase == 1 ? 0.8 : 0.95;
            case GELATINA: return e.tam == 2 ? 0.45 : e.tam == 1 ? 0.34 : 0.26;
            case MIGAJIN: return 0.25;
            case ORBE: return 0.42;
            case CAPULLO: return 0.35;
            default: return 0.35;
        }
    }

    private void mover(Cosa e, double dx, double dy, double r) {
        if (!chocaCon(e.x + dx, e.y, r)) e.x += dx;
        if (!chocaCon(e.x, e.y + dy, r)) e.y += dy;
    }

    private void lanzar(int tipo, double x, double y, double a, double vel, double dmg, double vida, double esc, double alt) {
        Cosa b = new Cosa();
        b.tipo = tipo; b.x = x; b.y = y; b.vx = Math.cos(a) * vel; b.vy = Math.sin(a) * vel;
        b.vida = vida; b.alt = alt; b.esc = esc; b.danio = dmg;
        cosas.add(b);
    }

    private void disparaMugre(Cosa e, double desv, double vel, double dmg) {
        double a = Math.atan2(py - e.y, px - e.x) + desv;
        lanzar(MUGRE, e.x, e.y, a, vel, dmg, 4, 0.22, 0.3);
    }

    private void nube(double x, double y, double dur, double radio) {
        Cosa n = new Cosa();
        n.tipo = NUBE; n.x = x; n.y = y; n.vida = dur; n.a1 = radio; n.esc = radio * 1.3; n.cd = 0.3;
        cosas.add(n);
    }

    private void charco(double x, double y) {
        int cuantos = 0; Cosa viejo = null;
        for (Cosa c : cosas) if (c.activa && c.tipo == CHARCO) { cuantos++; if (viejo == null) viejo = c; }
        if (cuantos > 40 && viejo != null) viejo.activa = false;
        Cosa c = new Cosa();
        c.tipo = CHARCO; c.x = x; c.y = y; c.vida = 7; c.esc = 0.6;
        cosas.add(c);
    }

    private boolean golpeable(Cosa e) {
        if (e.inerte) return false;
        switch (e.tipo) {
            case TELARANIN: return e.sub >= 2;
            case MADAME: return !(e.fase == 1 && e.sub != 2);
            case GOTERA: return e.sub == 0;
            case SOMBRA: return true;
            default: return true;
        }
    }

    private int orbesVivos() {
        int n = 0;
        for (Cosa c : cosas) if (c.activa && c.tipo == ORBE && c.estado != MURIENDO) n++;
        return n;
    }

    private boolean inmuneAhora(Cosa e) {
        switch (e.tipo) {
            case GRASON: return e.fase == 1 && e.sub == 0;
            case CAPITAN: return valvulas < 3;
            case MADRE: return e.fase == 2 && orbesVivos() > 0;
            default: return false;
        }
    }

    private void actualizarCosa(Cosa c, double dt) {
        c.t += dt;
        c.flash = Math.max(0, c.flash - dt);
        c.disp = Math.max(0, c.disp - dt);
        switch (c.tipo) {
            case JABON: case BURBUJAS: case ESTRELLA: case LLAVE: case BGRANDE: case SUPERJ: case ESCUDO: case LINTERNA: case TURBO:
                if (Math.hypot(c.x - px, c.y - py) < 0.6) recoger(c);
                break;
            case POP:
                if (c.t > 0.3) c.activa = false;
                break;
            case DUCHA: break;
            case BALA: {
                c.vida -= dt;
                c.x += c.vx * dt; c.y += c.vy * dt;
                int bx = (int) Math.floor(c.x), by = (int) Math.floor(c.y);
                if (bx >= 0 && by >= 0 && by < mapa.length && bx < mapa[0].length && mapa[by][bx] == T_TELA) {
                    muroHp[by][bx] -= c.danio >= 2 ? 2 : 1;
                    pop(c.x, c.y, 0.4, 0.3); c.activa = false;
                    if (muroHp[by][bx] <= 0) { mapa[by][bx] = 0; Sonido.tono(500, 1200, 150, 0.5); mensaje("¡Reventaste la telaraña!"); }
                    else Sonido.tono(400, 700, 60, 0.4);
                    break;
                }
                if (c.vida <= 0 || solido(c.x, c.y)) { pop(c.x - c.vx * dt, c.y - c.vy * dt, 0.35, 0.2); c.activa = false; break; }
                for (Cosa e : cosas) {
                    if (!esEnemigo(e.tipo) || !e.activa || e.estado == MURIENDO || !golpeable(e)) continue;
                    if (Math.hypot(e.x - c.x, e.y - c.y) < radio(e)) {
                        golpear(e, c.danio);
                        pop(c.x, c.y, 0.3, 0.3);
                        c.activa = false;
                        break;
                    }
                }
                break;
            }
            case MUGRE: case TOMATE: case RED: case ANCLA: case ONDA: case ESPORABOLA:
                proyectil(c, dt);
                break;
            case GOTAFIJA: {
                double ph = c.t % 3.2;
                if (ph < 1.0) c.n = 0;
                else if (c.n == 0) { c.n = 1; splash(c.x, c.y); }
                break;
            }
            case CHARCO:
                c.vida -= dt;
                if (c.vida <= 0) c.activa = false;
                else if (Math.hypot(px - c.x, py - c.y) < 0.7) charcoLento = 1;
                break;
            case NUBE:
                c.vida -= dt; c.cd -= dt;
                if (c.vida <= 0) { c.activa = false; break; }
                if (c.cd <= 0 && Math.hypot(px - c.x, py - c.y) < c.a1) { danio(1); c.cd = 0.5; }
                break;
            case LLUVIA:
                if (c.t >= 0.9) {
                    pop(c.x, c.y, 0.5, 0.1); Sonido.tono(250, 120, 100, 0.4);
                    if (Math.hypot(px - c.x, py - c.y) < 0.8) danio(5);
                    c.activa = false;
                }
                break;
            default: enemigo(c, dt);
        }
    }

    private void proyectil(Cosa c, double dt) {
        c.vida -= dt;
        c.x += c.vx * dt; c.y += c.vy * dt;
        boolean fin = c.vida <= 0 || solido(c.x, c.y);
        double rr = c.tipo == ANCLA ? 0.5 : c.tipo == ONDA ? 0.4 : 0.35;
        if (!fin && Math.hypot(px - c.x, py - c.y) < rr) {
            if (c.tipo != ESPORABOLA) danio(c.danio);
            if (c.tipo == RED) { lentoT = 3; mensaje("¡Telaraña! Te movés más lento"); }
            fin = true;
        }
        if (fin) {
            if (c.tipo == ESPORABOLA) nube(c.x, c.y, 3, 1.0);
            if (c.tipo != ONDA) pop(c.x, c.y, 0.3, 0.2);
            c.activa = false;
        }
    }

    private void splash(double x, double y) {
        pop(x, y, 0.8, 0.1);
        Sonido.tono(300, 150, 120, 0.5);
        if (Math.hypot(px - x, py - y) < 1.0) danio(5);
    }

    private void recoger(Cosa c) {
        c.activa = false;
        brilloT = 0.25;
        if (c.tipo == LLAVE) { tieneLlave = true; mensaje(niv.msgLlaveOk); Sonido.magia(); return; }
        if (c.cuenta) objetos++;
        switch (c.tipo) {
            case JABON: limpieza = Math.min(100, limpieza + 25); mensaje("Jabón: +25% de limpieza"); Sonido.curar(); break;
            case BURBUJAS: { int n = c.sub == 10 ? 10 : 20; municion = Math.min(99, municion + n); mensaje("Burbujas: +" + n); Sonido.pop(); break; }
            case BGRANDE: bgrande = 10; mensaje("¡Burbuja Grande! Los próximos 10 disparos hacen doble daño"); Sonido.magia(); break;
            case SUPERJ: limpieza = Math.min(100, limpieza + 50); mensaje("¡Super Jabón: +50% de limpieza!"); Sonido.curar(); break;
            case ESCUDO: escudoEsp = 30; mensaje("¡Escudo de Espuma! Absorbe los próximos 30 de daño"); Sonido.magia(); break;
            case LINTERNA: linterna = true; mensaje("¡Linterna de Burbujas! Ahora ves mucho más lejos"); Sonido.magia(); break;
            case TURBO: turbo = true; mensaje("¡Burbujero 5000 Turbo! Dispara al doble de velocidad"); Sonido.magia(); break;
            default: mensaje("¡Estrella!"); Sonido.moneda();
        }
    }

    // ------------------------------------------------------------------ daño y muertes

    private void golpear(Cosa e, double d) {
        if (e.estado == MURIENDO) return;
        if (inmuneAhora(e)) {
            Sonido.tono(1500, 1900, 70, 0.4);
            e.flash = 0.05;
            return;
        }
        if (e.x2) d *= 2;
        if (e.tipo == MADRE && e.fase == 3 && e.t3 <= 0) d *= 0.25;
        e.hp -= d;
        e.flash = 0.12;
        if (e.estado == QUIETO) e.estado = PERSIGUE;
        if (e.tipo == PELUSON && e.sub == 0) { e.sub = 2; }
        if (e.tipo == MADRE) {
            if (e.fase == 1) e.hp = Math.max(e.hp, 67);
            else if (e.fase == 2) e.hp = Math.max(e.hp, 34);
        }
        if (e.hp <= 0) morir(e);
        else Sonido.tono(400, 600, 40, 0.3);
    }

    private boolean esJefe(Cosa e) { return e == jefe; }

    private void limpiarPeligros() {
        for (Cosa c : cosas) {
            if (!c.activa) continue;
            switch (c.tipo) {
                case MUGRE: case TOMATE: case RED: case ANCLA: case ONDA: case ESPORABOLA: case NUBE: case LLUVIA: c.activa = false; break;
                default:
                    if (esEnemigo(c.tipo) && !c.cuenta && c != jefe && c.estado != MURIENDO) { c.estado = MURIENDO; c.t = 0; c.cuenta = false; }
            }
        }
    }

    private void morir(Cosa e) {
        e.estado = MURIENDO; e.t = 0;
        if (e.cuenta) muertes++;
        Sonido.tono(900, 1500, 120, 0.5);
        switch (e.tipo) {
            case REY: case GRASON: case MADAME: case CAPITAN:
                jefeMuerto = true;
                mensaje(niv.msgJefe);
                Sonido.ganar();
                for (int i = 0; i < 4; i++) pop(e.x + (Math.random() - 0.5), e.y + (Math.random() - 0.5), 0.6, 0.4 + Math.random() * 0.5);
                limpiarPeligros();
                if (e.tipo == MADAME) {
                    apagonOn = false; ventanaRota = true;
                    for (Cosa c : cosas) if (c.tipo == CAPULLO) c.activa = false;
                }
                break;
            case MADRE:
                finalT = 0.001;
                limpiarPeligros();
                for (Cosa c : cosas) if (c.tipo == ORBE) c.activa = false;
                Sonido.ganar();
                mensaje("¡La Mancha Madre se está achicando!");
                break;
            case MOHO:
                nube(e.x, e.y, 2.0, 1.3);
                break;
            case GELATINA:
                if (e.tam > 0) {
                    for (int i = 0; i < 2; i++) {
                        Cosa h = new Cosa();
                        h.tipo = GELATINA; h.tam = e.tam - 1; h.hp = h.tam == 1 ? 3 : 1; h.maxHp = h.hp;
                        h.estado = PERSIGUE; h.cuenta = e.cuenta; h.cd = 0.8;
                        double ox = (i == 0 ? -1 : 1) * 0.35, oy = (Math.random() - 0.5) * 0.3;
                        h.x = solido(e.x + ox, e.y + oy) ? e.x : e.x + ox; h.y = solido(e.x + ox, e.y + oy) ? e.y : e.y + oy;
                        cosas.add(h);
                    }
                }
                break;
            default: break;
        }
        if (e.cuenta && e.tipo != REY && e.tipo != GRASON && e.tipo != MADAME && e.tipo != CAPITAN && e.tipo != MADRE && Math.random() < 0.25) {
            Cosa d = nuevoItem(BURBUJAS, e.x, e.y);
            d.sub = 10;
            if (solido(d.x, d.y)) d.activa = false;
        }
    }

    // ------------------------------------------------------------------ inteligencia de los monstruos

    private boolean despierto(Cosa e, double d) {
        if (e.estado == QUIETO) {
            if (d < 10 && veLinea(e.x, e.y, px, py)) {
                e.estado = PERSIGUE;
                if (e == jefe) mensaje("¡" + niv.jefe + "!");
                if (e.tipo == MIGAJIN) for (Cosa o : cosas) if (o.tipo == MIGAJIN && o.estado == QUIETO && Math.hypot(o.x - e.x, o.y - e.y) < 2.5) o.estado = PERSIGUE;
            } else return false;
        }
        return true;
    }

    private void enemigo(Cosa e, double dt) {
        if (e.estado == MURIENDO) {
            if (e.tipo == MADRE) {
                if (e.t > 1.2 && e.n == 0) { e.n = 1; mensaje("“…solo quería que alguien me limpiara con cariño.”"); Sonido.curar(); }
                return;
            }
            if (e.t > 0.7) e.activa = false;
            return;
        }
        double dx = px - e.x, dy = py - e.y, d = Math.hypot(dx, dy);
        e.cd -= dt;
        double ux = dx / Math.max(0.001, d), uy = dy / Math.max(0.001, d);
        switch (e.tipo) {
            case MUGRITO: case TIRADOR: case REY: clasico(e, dt, d, ux, uy); break;
            case MIGAJIN: migajin(e, dt, d, ux, uy); break;
            case TOMATON: tomaton(e, dt, d, ux, uy); break;
            case GRASON: grason(e, dt, d, ux, uy); break;
            case PELUSON: peluson(e, dt, d, ux, uy); break;
            case TELARANIN: telaranin(e, dt, d, ux, uy); break;
            case MADAME: madame(e, dt, d, ux, uy); break;
            case CAPULLO: capullo(e, dt); break;
            case GOTERA: gotera(e, dt, d); break;
            case MOHO: moho(e, dt, d, ux, uy); break;
            case CAPITAN: capitan(e, dt, d, ux, uy); break;
            case GELATINA: gelatina(e, dt, d, ux, uy); break;
            case SOMBRA: sombra(e, dt, d, ux, uy); break;
            case MADRE: madre(e, dt, d, ux, uy); break;
            case ORBE: orbe(e, dt); break;
            default: break;
        }
        if (e.tipo != ORBE && e.tipo != CAPULLO && e.tipo != MADRE && e.tipo != GOTERA && !(e.tipo == TELARANIN && e.sub < 2)) separar(e, dt);
    }

    private void separar(Cosa e, double dt) {
        double r = 0.28;
        for (Cosa o : cosas) {
            if (o == e || !esEnemigo(o.tipo) || !o.activa || o.estado == MURIENDO || o.tipo == ORBE || o.tipo == CAPULLO) continue;
            double ox = e.x - o.x, oy = e.y - o.y, od = Math.hypot(ox, oy);
            double lim = e.tipo == MIGAJIN ? 0.35 : 0.55;
            if (od < lim && od > 0.001) mover(e, ox / od * 0.8 * dt, oy / od * 0.8 * dt, r);
        }
    }

    private void clasico(Cosa e, double dt, double d, double ux, double uy) {
        if (e.estado == QUIETO) {
            if (d < 10 && veLinea(e.x, e.y, px, py)) { e.estado = PERSIGUE; if (e == jefe) mensaje("¡" + niv.jefe + "!"); }
            return;
        }
        double r = e.tipo == REY ? 0.55 : 0.28;
        if (e.tipo == MUGRITO) {
            if (d > 0.8) mover(e, ux * 1.5 * dt, uy * 1.5 * dt, r);
            else if (e.cd <= 0) { danio(5); e.cd = 1.0; }
        } else if (e.tipo == TIRADOR) {
            if (d > 5.5) mover(e, ux * 1.2 * dt, uy * 1.2 * dt, r);
            else if (d < 3.5) mover(e, -ux * 1.0 * dt, -uy * 1.0 * dt, r);
            else mover(e, -uy * 0.8 * dt, ux * 0.8 * dt, r);
            if (e.cd <= 0 && d < 12 && veLinea(e.x, e.y, px, py)) {
                disparaMugre(e, (Math.random() - 0.5) * 0.15, 4.0, 6);
                e.cd = 1.8; e.disp = 0.35;
            }
        } else {
            if (d > 4.5) mover(e, ux * 0.9 * dt, uy * 0.9 * dt, r);
            else mover(e, -uy * 0.7 * dt, ux * 0.7 * dt, r);
            if (e.cd <= 0 && veLinea(e.x, e.y, px, py)) {
                for (int i = -1; i <= 1; i++) disparaMugre(e, i * 0.22, 3.6, 8);
                e.cd = 1.7; e.disp = 0.4;
            }
            if (d < 1.2 && e.cd <= 0.9) { danio(6); e.cd = 1.2; }
        }
    }

    private void migajin(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        if (d > 0.55) mover(e, ux * 2.4 * dt, uy * 2.4 * dt, 0.2);
        else if (e.cd <= 0) { danio(2); e.cd = 0.6; }
    }

    private void tomaton(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        double r = 0.28;
        switch (e.sub) {
            case 0:
                if (d > 1.6) mover(e, ux * 0.9 * dt, uy * 0.9 * dt, r);
                if (d < 7.5 && e.cd <= 0 && veLinea(e.x, e.y, px, py)) { e.sub = 1; e.t2 = 0; Sonido.tono(300, 520, 300, 0.4); }
                break;
            case 1:
                e.t2 += dt;
                if (e.t2 >= 1.0) {
                    double a = Math.atan2(py - e.y, px - e.x);
                    e.vx = Math.cos(a) * 4; e.vy = Math.sin(a) * 4; e.sub = 2; e.t2 = 0; e.a2 = 0;
                    Sonido.tono(200, 420, 300, 0.5);
                }
                break;
            case 2: {
                e.t2 += dt;
                double nx = e.x + e.vx * dt, ny = e.y + e.vy * dt;
                if (chocaCon(nx, ny, r) || e.t2 > 2.4) { e.sub = 3; e.t2 = 0; e.x2 = true; Sonido.golpe(); pop(e.x, e.y, 0.5, 0.3); }
                else { e.x = nx; e.y = ny; }
                if (e.a2 == 0 && d < 0.6) { danio(10); e.a2 = 1; }
                break;
            }
            default:
                e.t2 += dt; e.x2 = true;
                if (e.t2 >= 2.0) { e.sub = 0; e.x2 = false; e.cd = 1.5; }
        }
    }

    private void gelatina(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        double vel = e.tam == 2 ? 1.0 : e.tam == 1 ? 1.25 : 1.5, r = e.tam == 2 ? 0.35 : e.tam == 1 ? 0.27 : 0.2;
        if (d > 0.9) mover(e, ux * vel * dt, uy * vel * dt, r);
        else if (e.cd <= 0) { danio(5); e.cd = 1.0; }
    }

    private void sombra(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        e.t2 += dt;
        switch (e.sub) {
            case 0:
                if (d > 1.4) mover(e, ux * 1.0 * dt, uy * 1.0 * dt, 0.28);
                if (e.t2 >= 3.5) { e.sub = 1; e.t2 = 0; Sonido.tono(1100, 250, 450, 0.5); }
                break;
            case 1:
                if (e.t2 >= 0.5) {
                    for (int k = 0; k < 7; k++) {
                        double off = (k % 2 == 0 ? 1 : -1) * 0.45 * ((k + 1) / 2);
                        double a = ang + Math.PI + off, bx = px + Math.cos(a) * 1.2, by = py + Math.sin(a) * 1.2;
                        if (!chocaCon(bx, by, 0.28)) { e.x = bx; e.y = by; break; }
                    }
                    e.sub = 2; e.t2 = 0;
                }
                break;
            default:
                if (e.t2 >= 0.8) {
                    if (Math.hypot(px - e.x, py - e.y) < 1.8) { danio(7); shakeT = 0.25; }
                    e.sub = 0; e.t2 = 0;
                }
        }
    }

    private void moho(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        if (d > 0.9) mover(e, ux * 0.6 * dt, uy * 0.6 * dt, 0.3);
        else if (e.cd <= 0) { danio(3); e.cd = 1.0; }
        e.t2 += dt;
        if (e.t2 >= 1.4) { e.t2 = 0; charco(e.x, e.y); }
    }

    private void peluson(Cosa e, double dt, double d, double ux, double uy) {
        double r = 0.28;
        if (e.estado == QUIETO) {
            if (d < 3.0) {
                e.estado = PERSIGUE; e.sub = 1; e.t2 = 0; e.a1 = Math.atan2(py - e.y, px - e.x);
                Sonido.tono(300, 650, 140, 0.4);
            } else {
                if (d < 9) { e.t3 -= dt; if (e.t3 <= 0) { Sonido.tono(160, 110, 80, 0.3); e.t3 = 1.1; } }
                return;
            }
        }
        if (e.sub == 1) {
            e.t2 += dt;
            mover(e, Math.cos(e.a1) * 4.5 * dt, Math.sin(e.a1) * 4.5 * dt, r);
            if (e.t2 >= 0.45) e.sub = 2;
        } else if (d > 0.7) mover(e, ux * 1.3 * dt, uy * 1.3 * dt, r);
        if (d < 0.7 && e.cd <= 0) { danio(4); shakeT = 1.0; e.cd = 1.2; Sonido.tono(500, 300, 100, 0.4); mensaje("¡Achís! Te llenó de pelusa"); }
    }

    private void telaranin(Cosa e, double dt, double d, double ux, double uy) {
        switch (e.sub) {
            case 0:
                if (Math.abs(px - e.x) < 1.3 && Math.abs(py - e.y) < 1.3) { e.sub = 1; e.t2 = 0; e.estado = PERSIGUE; Sonido.tono(500, 900, 150, 0.3); }
                break;
            case 1:
                e.t2 += dt;
                if (e.t2 >= 0.5) { e.sub = 2; e.alto = false; Sonido.boing(); }
                break;
            default: {
                double r = 0.28;
                if (d > 3.5) mover(e, ux * 1.6 * dt, uy * 1.6 * dt, r);
                else if (d < 2.2) mover(e, -ux * 1.2 * dt, -uy * 1.2 * dt, r);
                else mover(e, -uy * 0.9 * dt, ux * 0.9 * dt, r);
                if (e.cd <= 0 && d < 9 && veLinea(e.x, e.y, px, py)) {
                    lanzar(RED, e.x, e.y, Math.atan2(py - e.y, px - e.x), 3.4, 3, 4, 0.3, 0.3);
                    e.cd = 2.4; e.disp = 0.3;
                }
            }
        }
    }

    private void fijarObjetivo(Cosa e) {
        for (int i = 0; i < 12; i++) {
            double a = Math.random() * Math.PI * 2, rr = Math.random() * 2.2;
            double x = px + Math.cos(a) * rr, y = py + Math.sin(a) * rr;
            if (!chocaCon(x, y, 0.3)) { e.x = x; e.y = y; return; }
        }
        e.x = px; e.y = py;
    }

    private void gotera(Cosa e, double dt, double d) {
        if (e.estado == QUIETO) {
            if (d < 10) { e.estado = PERSIGUE; e.sub = 0; e.t2 = 0; fijarObjetivo(e); } else return;
        }
        e.t2 += dt;
        switch (e.sub) {
            case 0:
                e.alto = true;
                if (e.t2 >= 1.0) { e.sub = 1; e.t2 = 0; }
                break;
            case 1:
                if (e.t2 >= 0.15) { e.sub = 2; e.t2 = 0; e.alto = false; splash(e.x, e.y); }
                break;
            default:
                if (e.t2 >= 1.85) { e.sub = 0; e.t2 = 0; e.alto = true; fijarObjetivo(e); }
        }
    }

    private void capullo(Cosa e, double dt) {
        if (e.inerte) return;
        e.t3 -= dt;
        if (e.t3 <= 0) {
            e.t3 = 10;
            Cosa a = invocar(TELARANIN, e.x, e.y + 0.4);
            if (solido(a.x, a.y)) { a.x = e.x; a.y = e.y; }
            Sonido.tono(300, 700, 200, 0.4);
        }
    }

    // ------------------------------------------------------------------ jefes

    private void grason(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        double r = 0.55;
        if (e.fase == 1) {
            e.t2 += dt;
            if (e.sub == 0) {
                e.inmune = true;
                if (e.t2 >= 2) { e.sub = 1; e.t2 = 0; e.inmune = false; Sonido.tono(500, 300, 100, 0.4); }
            } else {
                e.inmune = false;
                if (e.cd <= 0 && veLinea(e.x, e.y, px, py)) {
                    lanzar(TOMATE, e.x, e.y, Math.atan2(py - e.y, px - e.x) + (Math.random() - 0.5) * 0.12, 3.6, 8, 4, 0.26, 0.3);
                    e.cd = 1.2; e.disp = 0.3;
                }
                if (e.t2 >= 3) { e.sub = 0; e.t2 = 0; Sonido.tono(1400, 1800, 80, 0.4); }
            }
            if (d > 4.5) mover(e, ux * 0.8 * dt, uy * 0.8 * dt, r);
            else mover(e, -uy * 0.7 * dt, ux * 0.7 * dt, r);
            if (e.hp <= 22.5) {
                e.fase = 2; e.sub = 0; e.t2 = 0; e.t3 = 10; e.inmune = false;
                mensaje("¡Grasón perdió la sartén y se enojó!"); Sonido.perder();
            }
            return;
        }
        double v = 0.9 * 1.4;
        switch (e.sub) {
            case 0:
                e.x2 = false;
                if (d > 1.3) mover(e, ux * v * dt, uy * v * dt, r);
                if (d < 1.2 && e.cd <= 0) { danio(6); e.cd = 1.2; }
                e.t3 += dt;
                if (e.t3 >= 12) {
                    e.t3 = 0; invocarMigajines(e);
                    e.sub = 1; e.t2 = 0; mensaje("¡Cuidado con el Rodillo Veloz!");
                }
                break;
            case 1:
                e.t2 += dt;
                if (e.t2 >= 0.8) {
                    double a = Math.atan2(py - e.y, px - e.x);
                    e.vx = Math.cos(a) * 7; e.vy = Math.sin(a) * 7; e.sub = 2; e.t2 = 0; e.a2 = 0;
                    Sonido.tono(200, 500, 300, 0.5);
                }
                break;
            case 2: {
                e.t2 += dt;
                double nx = e.x + e.vx * dt, ny = e.y + e.vy * dt;
                if (chocaCon(nx, ny, r) || e.t2 >= 0.9) { e.sub = 3; e.t2 = 0; Sonido.golpe(); }
                else { e.x = nx; e.y = ny; }
                if (e.a2 == 0 && d < 1.0) { danio(12); e.a2 = 1; }
                break;
            }
            default:
                e.x2 = true; e.t2 += dt;
                if (e.t2 >= 2) { e.sub = 0; e.x2 = false; }
        }
    }

    private void invocarMigajines(Cosa e) {
        int n = 0;
        for (int[] h : hornallas) {
            int[][] vec = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] v : vec) {
                int cx = h[0] + v[0], cy = h[1] + v[1];
                if (cy >= 0 && cx >= 0 && cy < mapa.length && cx < mapa[0].length && mapa[cy][cx] == 0) {
                    invocar(MIGAJIN, cx + 0.5, cy + 0.5); n++; break;
                }
            }
        }
        for (; n < 4; n++) invocar(MIGAJIN, e.x + (Math.random() - 0.5), e.y + (Math.random() - 0.5));
        Sonido.tono(300, 900, 300, 0.5);
        mensaje("¡Salen Migajines del horno!");
    }

    private void tirarRedes(Cosa e, double dmg) {
        double a = Math.atan2(py - e.y, px - e.x);
        for (int i = -1; i <= 1; i++) lanzar(RED, e.x, e.y, a + i * 0.26, 3.3, dmg, 4, 0.3, 0.3);
        e.disp = 0.4;
        Sonido.tono(600, 900, 120, 0.4);
    }

    private void madame(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        double r = 0.5;
        e.t2 += dt;
        if (e.fase == 1) {
            switch (e.sub) {
                case 0:
                    e.alto = true;
                    if (d > 2.5) mover(e, ux * 1.6 * dt, uy * 1.6 * dt, 0.3);
                    else mover(e, -uy * 1.2 * dt, ux * 1.2 * dt, 0.3);
                    if (e.t2 >= 8) { e.sub = 1; e.t2 = 0; Sonido.tono(500, 200, 400, 0.4); }
                    break;
                case 1:
                    e.alto = true;
                    if (e.t2 >= 0.6) { e.sub = 2; e.t2 = 0; e.alto = false; e.n = 0; Sonido.boing(); }
                    break;
                case 2:
                    e.alto = false;
                    mover(e, -uy * 0.8 * dt, ux * 0.8 * dt, r);
                    if (e.n == 0 && e.t2 >= 0.4) { e.n = 1; tirarRedes(e, 4); }
                    if (e.n == 1 && e.t2 >= 2.3) { e.n = 2; tirarRedes(e, 4); }
                    if (e.t2 >= 4) { e.sub = 3; e.t2 = 0; }
                    break;
                default:
                    e.alto = true;
                    if (e.t2 >= 0.6) { e.sub = 0; e.t2 = 0; }
            }
            if (e.hp <= 30) {
                e.fase = 2; e.sub = 12; e.alto = false; e.t2 = 0; e.cd = 1.5; e.n = 0;
                apagonOn = true;
                for (Cosa c : cosas) if (c.tipo == CAPULLO) { c.inerte = false; c.t3 = 3 + Math.random() * 4; }
                mensaje("¡Madame apagó las luces! Reventá los capullos (3 burbujas cada uno)");
                Sonido.perder();
            }
            return;
        }
        // fase 2: apagon
        e.alto = false;
        if (e.sub == 12) {
            if (d > 4.5) mover(e, ux * 1.1 * dt, uy * 1.1 * dt, r);
            else mover(e, -uy * 0.9 * dt, ux * 0.9 * dt, r);
            if (e.cd <= 0 && veLinea(e.x, e.y, px, py)) { tirarRedes(e, 4); e.cd = 2.6; }
            if (e.hp <= 8 && e.n != 9) { e.sub = 10; e.t2 = 0; mensaje("¡Madame intenta escapar por la ventanita!"); }
        } else if (e.sub == 10) {
            double tx = ventX + 0.5, ty = ventY + 0.5, dd = Math.hypot(tx - e.x, ty - e.y);
            if (dd > 0.01) mover(e, (tx - e.x) / dd * 2.4 * dt, (ty - e.y) / dd * 2.4 * dt, 0.3);
            if (dd < 0.9 || e.t2 > 6) {
                e.sub = 11; e.t2 = 0; e.x2 = true; e.n = 9;
                mensaje("¡Se enredó en su propia tela! 5 segundos con daño doble"); Sonido.magia();
            }
        } else {
            e.x2 = true;
            if (e.t2 >= 5) { e.sub = 12; e.x2 = false; e.cd = 1.0; }
        }
    }

    private void onda(Cosa e, double off) {
        for (int i = 0; i < 12; i++) lanzar(ONDA, e.x, e.y, off + i * Math.PI * 2 / 12, 3.0, 8, 3.6, 0.3, 0.15);
        e.disp = 0.5;
        Sonido.tono(150, 80, 300, 0.6);
    }

    private void capitan(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        double r = 0.6;
        e.inmune = valvulas < 3;
        if (e.fase == 1 && valvulas >= 3) {
            e.fase = 2; e.inmune = false; e.t3 = 4; e.a1 = 8;
            mensaje("¡La sala está seca! ¡El Capitán ya no tiene escudo!"); Sonido.magia();
        }
        double vel = e.fase == 1 ? 0.6 : 0.45;
        if (d > 4.5) mover(e, ux * vel * dt, uy * vel * dt, r);
        else mover(e, -uy * vel * 0.8 * dt, ux * vel * 0.8 * dt, r);
        if (e.cd <= 0 && veLinea(e.x, e.y, px, py)) {
            lanzar(ANCLA, e.x, e.y, Math.atan2(py - e.y, px - e.x), 2.6, 10, 5, 0.4, 0.3);
            e.cd = e.fase == 1 ? 2.8 : 2.4; e.disp = 0.4;
        }
        boolean furia = e.fase == 2 && e.hp <= e.maxHp * 0.5;
        e.t2 += dt;
        if (e.t2 >= (furia ? 5.0 : 6.5)) {
            e.t2 = 0; onda(e, 0);
            if (furia) e.a2 = 0.5;
        }
        if (e.a2 > 0) { e.a2 -= dt; if (e.a2 <= 0) onda(e, Math.PI / 12); }
        if (e.fase == 2) {
            e.t3 -= dt;
            if (e.t3 <= 0) {
                e.t3 = 5;
                for (int i = 0; i < 10; i++) {
                    double a = Math.random() * Math.PI * 2, rr = 1.5 + Math.random() * 3;
                    double x = px + Math.cos(a) * rr, y = py + Math.sin(a) * rr;
                    if (!solido(x, y)) { nube(x, y, 6, 1.2); break; }
                }
            }
            e.a1 += dt;
            if (e.a1 >= 15) {
                e.a1 = 0;
                for (int i = 0; i < 2; i++) {
                    double x = e.x + (i == 0 ? -1 : 1) * 1.0, y = e.y + 0.6;
                    if (solido(x, y)) { x = e.x; y = e.y; }
                    invocar(MOHO, x, y);
                }
                mensaje("¡El Capitán invoca Mohos Pegajosos!");
            }
        }
    }

    private void lluvia(double x, double y) {
        if (solido(x, y)) return;
        Cosa l = new Cosa();
        l.tipo = LLUVIA; l.x = x; l.y = y; l.esc = 0.3;
        cosas.add(l);
    }

    private void madre(Cosa e, double dt, double d, double ux, double uy) {
        if (!despierto(e, d)) return;
        e.t3 = Math.max(0, e.t3 - dt);
        switch (e.fase) {
            case 1: {
                if (e.n == 0) { e.n = 1; e.t2 = 7; }
                if (d > 3.5) mover(e, ux * 0.7 * dt, uy * 0.7 * dt, 0.5);
                else mover(e, -uy * 0.5 * dt, ux * 0.5 * dt, 0.5);
                e.t2 += dt;
                if (e.t2 >= 10) {
                    e.t2 = 0;
                    for (int i = 0; i < 2; i++) {
                        double x = e.x + (i == 0 ? -1 : 1) * 1.1, y = e.y;
                        if (solido(x, y)) { x = e.x; y = e.y; }
                        invocar(GELATINA, x, y);
                    }
                    double a0 = Math.random() * Math.PI;
                    for (int i = 0; i < 8; i++) lanzar(MUGRE, e.x, e.y, a0 + i * Math.PI / 4, 3.0, 6, 5, 0.22, 0.3);
                    e.disp = 0.5; Sonido.tono(150, 400, 300, 0.6);
                    mensaje("¡La Mancha Madre larga gelatinas y mugre!");
                }
                if (e.hp <= 67) {
                    e.fase = 2; e.sub = 0; e.t2 = 0; e.n = 2;
                    for (int i = 0; i < 3; i++) {
                        Cosa o = new Cosa();
                        o.tipo = ORBE; o.n = i; o.a1 = i * Math.PI * 2 / 3; o.hp = 6; o.maxHp = 6; o.estado = PERSIGUE; o.cd = 1 + i * 0.7;
                        o.x = e.x; o.y = e.y; cosas.add(o);
                    }
                    mensaje("¡La Mancha se alzó! Reventá los 3 pedazos de corona que giran a su alrededor");
                    Sonido.perder();
                }
                break;
            }
            case 2:
                if (d > 4) mover(e, ux * 0.55 * dt, uy * 0.55 * dt, 0.5);
                else mover(e, -uy * 0.45 * dt, ux * 0.45 * dt, 0.5);
                if (e.n == 2 && orbesVivos() == 0) { e.n = 3; mensaje("¡Los orbes estallaron! ¡Ahora sí se la puede limpiar!"); Sonido.magia(); }
                if (e.hp <= 34) {
                    e.fase = 3; e.sub = 0; e.t2 = 0; e.a1 = 0; e.t3 = 0;
                    mensaje("¡Ducha Maestra! Atraela al centro y tirá de la cadena (E)"); Sonido.perder();
                }
                break;
            default: {
                e.t2 += dt; e.a1 += dt;
                if (e.a1 >= 1.5) {
                    e.a1 = 0;
                    for (int i = 0; i < 2; i++) {
                        double a = Math.random() * Math.PI * 2, rr = 0.5 + Math.random() * 2.5;
                        lluvia(px + Math.cos(a) * rr, py + Math.sin(a) * rr);
                    }
                }
                switch (e.sub) {
                    case 0: {
                        double mx = duX - (px - duX), my = duY - (py - duY);
                        double ml = Math.hypot(mx - duX, my - duY);
                        if (ml > 4.5) { mx = duX + (mx - duX) / ml * 4.5; my = duY + (my - duY) / ml * 4.5; }
                        double dd = Math.hypot(mx - e.x, my - e.y);
                        if (dd > 0.4) mover(e, (mx - e.x) / dd * 1.9 * dt, (my - e.y) / dd * 1.9 * dt, 0.5);
                        if (d < 1.3 && e.cd <= 0) { danio(8); e.cd = 1.2; }
                        if (e.t2 >= 3.2 || (dd <= 0.4 && e.t2 >= 1.2)) { e.sub = 1; e.t2 = 0; Sonido.tono(150, 600, 500, 0.6); }
                        break;
                    }
                    case 1:
                        if (e.t2 >= 0.9) {
                            double a = Math.atan2(py - e.y, px - e.x);
                            e.vx = Math.cos(a) * 6.5; e.vy = Math.sin(a) * 6.5; e.sub = 2; e.t2 = 0; e.a2 = 0;
                        }
                        break;
                    case 2: {
                        double nx = e.x + e.vx * dt, ny = e.y + e.vy * dt;
                        if (chocaCon(nx, ny, 0.5) || e.t2 >= 1.3) { e.sub = 3; e.t2 = 0; Sonido.golpe(); }
                        else { e.x = nx; e.y = ny; }
                        if (e.a2 == 0 && d < 1.1) { danio(10); e.a2 = 1; }
                        break;
                    }
                    default:
                        if (e.t2 >= 1.3) { e.sub = 0; e.t2 = 0; }
                }
            }
        }
    }

    private void orbe(Cosa o, double dt) {
        Cosa m = jefe;
        if (m == null || !m.activa || m.estado == MURIENDO || m.fase != 2) { o.activa = false; return; }
        o.a1 += dt * 1.5;
        o.x = m.x; o.y = m.y;
        for (double rr : new double[]{1.7, 1.3, 0.9, 0.5}) {
            double nx = m.x + Math.cos(o.a1) * rr, ny = m.y + Math.sin(o.a1) * rr;
            if (!solido(nx, ny)) { o.x = nx; o.y = ny; break; }
        }
        double a = Math.atan2(py - o.y, px - o.x);
        if (o.cd > 0 || !veLinea(o.x, o.y, px, py)) return;
        switch (o.n) {
            case 0: lanzar(TOMATE, o.x, o.y, a, 3.6, 8, 4, 0.26, 0.4); o.cd = 2.2; break;
            case 1: for (int i = -1; i <= 1; i++) lanzar(RED, o.x, o.y, a + i * 0.26, 3.3, 4, 4, 0.3, 0.4); o.cd = 3.2; break;
            default: lanzar(ESPORABOLA, o.x, o.y, a, 3.0, 0, Math.min(2.5, Math.hypot(px - o.x, py - o.y) / 3.0), 0.25, 0.4); o.cd = 3.6;
        }
        Sonido.tono(500, 800, 100, 0.3);
    }

    // ================================================================== fin del capitulo

    private void ganar() {
        estado = Estado.FIN;
        finT = 0; finPag = 0;
        soltarMouse();
        disparando = false;
        pEn = totEn == 0 ? 100 : Math.min(100, muertes * 100 / totEn);
        pObj = totObj == 0 ? 100 : Math.min(100, objetos * 100 / totObj);
        pSec = totSec == 0 ? 100 : Math.min(100, secretos * 100 / totSec);
        int prom = (pEn + pObj + pSec) / 3;
        if (pEn == 100 && pObj == 100 && pSec == 100 && tiempo <= niv.par) rango = "S";
        else if (prom >= 90) rango = "A";
        else if (prom >= 70) rango = "B";
        else if (prom >= 50) rango = "C";
        else rango = "D";
        if (!monedasDadas) {
            monedasDadas = true;
            origen.sumarMonedas(niv.monedas);
            monedasSesion += niv.monedas;
            capSesion = Math.max(capSesion, cap);
        }
        guardarRango(cap, rango);
        dorado = trajeDorado();
        Sonido.ganar();
        for (int i = 0; i < 5; i++) fx.estrellas(120 + i * 180, 140, 8);
    }

    private int ultimoTick = -1;
    private void tickFin() {
        int fase = (int) (finT * 6);
        if (finT < 4.4 && fase != ultimoTick) { ultimoTick = fase; if (fase % 2 == 0) Sonido.tono(700, 760, 25, 0.25); }
        if (Math.random() < 0.15 && finT > 3.6) fx.estrellas(60 + Math.random() * 840, 60 + Math.random() * 120, 1);
    }

    // ================================================================== dibujo 3D

    private static int sombra(int col, int s) {
        if (s > 256) s = 256;
        return ((((col >> 16) & 255) * s >> 8) << 16) | ((((col >> 8) & 255) * s >> 8) << 8) | ((col & 255) * s >> 8);
    }

    private static int mezclar(int a, int b, int t) {
        if (t <= 0) return a;
        if (t >= 256) return b;
        int r = (((a >> 16) & 255) * (256 - t) + ((b >> 16) & 255) * t) >> 8;
        int g = (((a >> 8) & 255) * (256 - t) + ((b >> 8) & 255) * t) >> 8;
        int bl = ((a & 255) * (256 - t) + (b & 255) * t) >> 8;
        return (r << 16) | (g << 8) | bl;
    }

    private int luz(double dist) {
        double base = niv.luzBase, kk = niv.luzK, mn = niv.luzMin;
        if (niv.niebla && linterna) kk *= 0.55;
        double l1 = Math.max(mn, Math.min(1, base - dist * kk));
        if (apagon > 0) {
            double lb = linterna ? 1.0 : 0.6, lk = linterna ? 0.15 : 0.32;
            double l2 = Math.max(0.03, lb - dist * lk);
            l1 = l1 * (1 - apagon) + l2 * apagon;
        }
        return (int) (Math.max(0, Math.min(1, l1)) * 256);
    }

    private static final int COLOR_NIEBLA = 0x6E625A;

    private int niebla(double dist, boolean iluminado) {
        if (!niv.niebla) return 0;
        double f = Math.max(0, Math.min(1, (dist - 2.5) / 12.0)) * 0.78;
        if (linterna) f *= 0.7;
        if (iluminado) f *= 0.3;
        f *= (1 - apagon * 0.8);
        return (int) (f * 256);
    }

    private int texPared(int v, int mx, int my) {
        if (v == niv.texExitOff && jefeMuerto) return niv.texExitOn;
        switch (v) {
            case T_HORN_DIN: return hornallaEncendida(mx, my) ? T_HORN_ON : T_HORN_OFF;
            case T_CADENA: return cadenaCd > 0 ? T_CADENA_REC : T_CADENA;
            case T_P3_0: return T_P3_0 + locksPlaced;
            case T_VENTANA: return ventanaRota ? T_VENTANA_ROTA : T_VENTANA;
            default: return v;
        }
    }

    private boolean hornallaEncendida(int mx, int my) {
        if (jefe == null || jefeMuerto) return false;
        int enc = (int) Math.ceil(jefe.hp / (jefe.maxHp / 4.0));
        for (int i = 0; i < hornallas.size(); i++) if (hornallas.get(i)[0] == mx && hornallas.get(i)[1] == my) return i < enc;
        return true;
    }

    private void renderizar() {
        int mw = mapa[0].length, mh = mapa.length;
        double rd0x = dirX - plX, rd0y = dirY - plY, rd1x = dirX + plX, rd1y = dirY + plY;
        int hor = VH / 2;
        double agua = niv.agua == null ? 0 : (valvulas >= 3 ? 0 : 1 - valvulas / 3.0);
        int fondoCentro = mezclar(0xEDE7FF, 0x201C28, (int) (apagon * 230));
        for (int y = 0; y < VH; y++) {
            int p = y - hor;
            if (p == 0) { for (int x = 0; x < VW; x++) pix[y * VW + x] = fondoCentro; continue; }
            boolean suelo = p > 0;
            double rowDist = 0.5 * VH / Math.abs(p);
            int s = luz(rowDist);
            int fg = niebla(rowDist, false);
            double stX = rowDist * (rd1x - rd0x) / VW, stY = rowDist * (rd1y - rd0y) / VW;
            double fx0 = px + rowDist * rd0x, fy0 = py + rowDist * rd0y;
            int row = y * VW;
            for (int x = 0; x < VW; x++) {
                int cx = (int) Math.floor(fx0), cy = (int) Math.floor(fy0);
                int tx = (int) (64 * (fx0 - cx)) & 63, ty = (int) (64 * (fy0 - cy)) & 63;
                int gx = Math.max(0, Math.min(mw - 1, cx)), gy = Math.max(0, Math.min(mh - 1, cy));
                int col;
                boolean lit = luzCelda[gy][gx];
                if (suelo) col = PISO[pisoG[gy][gx]][ty * 64 + tx]; else col = TECHOS[techoG[gy][gx]][ty * 64 + tx];
                int ss = (suelo && lit) ? Math.min(256, s + 90) : s;
                col = sombra(col, ss);
                if (fg > 0) col = mezclar(col, sombra(COLOR_NIEBLA, Math.max(40, ss)), lit ? fg * 3 / 10 : fg);
                if (suelo && agua > 0 && cx >= niv.agua[0] && cx <= niv.agua[2] && cy >= niv.agua[1] && cy <= niv.agua[3]) {
                    int ond = (int) (Math.sin((fx0 + fy0) * 5 + t * 2.5) * 14);
                    col = mezclar(col, 0x2E86C1, (int) (agua * 120) + ond);
                }
                pix[row + x] = col;
                fx0 += stX; fy0 += stY;
            }
        }

        for (int x = 0; x < VW; x++) {
            double cam = 2.0 * x / VW - 1;
            double rx = dirX + plX * cam, ry = dirY + plY * cam;
            int mx = (int) px, my = (int) py;
            double ddx = rx == 0 ? 1e30 : Math.abs(1 / rx), ddy = ry == 0 ? 1e30 : Math.abs(1 / ry);
            int sx, sy; double sdx, sdy;
            if (rx < 0) { sx = -1; sdx = (px - mx) * ddx; } else { sx = 1; sdx = (mx + 1.0 - px) * ddx; }
            if (ry < 0) { sy = -1; sdy = (py - my) * ddy; } else { sy = 1; sdy = (my + 1.0 - py) * ddy; }
            int lado = 0, v = 0;
            for (int i = 0; i < 90; i++) {
                if (sdx < sdy) { sdx += ddx; mx += sx; lado = 0; } else { sdy += ddy; my += sy; lado = 1; }
                if (mx < 0 || my < 0 || mx >= mw || my >= mh) { v = 5; break; }
                v = mapa[my][mx];
                if (v > 0) break;
            }
            double perp = lado == 0 ? sdx - ddx : sdy - ddy;
            if (perp < 0.05) perp = 0.05;
            zbuf[x] = perp;
            int linea = (int) (VH / perp);
            int ini = hor - linea / 2, fin = hor + linea / 2;
            double wx = lado == 0 ? py + perp * ry : px + perp * rx;
            wx -= Math.floor(wx);
            int tx = (int) (wx * 64);
            if ((lado == 0 && rx > 0) || (lado == 1 && ry < 0)) tx = 63 - tx;
            int[] tex = TEX[texPared(v, mx, my)];
            if (tex == null) tex = TEX[1];
            int s = luz(perp);
            if (lado == 1) s = s * 200 >> 8;
            int fg = niebla(perp, false);
            int fogCol = fg > 0 ? sombra(COLOR_NIEBLA, Math.max(40, s)) : 0;
            double paso = 64.0 / linea;
            double tpos = (Math.max(0, ini) - ini) * paso;
            int y0 = Math.max(0, ini), y1 = Math.min(VH - 1, fin);
            for (int y = y0; y <= y1; y++) {
                int ty = (int) tpos & 63; tpos += paso;
                int col = sombra(tex[ty * 64 + tx], s);
                if (fg > 0) col = mezclar(col, fogCol, fg);
                pix[y * VW + x] = col;
            }
        }
        dibujarCosas();
    }

    // ---- vista de las cosas

    private double vEsc, vAlt;
    private int vAlpha;
    private boolean vBrillo;

    private double escBase(Cosa c) {
        switch (c.tipo) {
            case MUGRITO: case TIRADOR: case TOMATON: case MOHO: return 0.7;
            case REY: case GRASON: return 1.4;
            case MADAME: return 1.3;
            case CAPITAN: return 1.45;
            case MIGAJIN: return 0.45;
            case PELUSON: return 0.6;
            case GELATINA: return c.tam == 2 ? 0.95 : c.tam == 1 ? 0.7 : 0.45;
            case SOMBRA: return 0.85;
            case MADRE: return 1.4;
            default: return 0.6;
        }
    }

    private Spr limpioDe(Cosa c) {
        switch (c.tipo) {
            case MUGRITO: return MUG_L;
            case TIRADOR: return TIR_L;
            case REY: return REY_L;
            case MIGAJIN: return MIGA_L;
            case TOMATON: return TOMA_L;
            case GRASON: return GRAS_L;
            case PELUSON: return PELU_L;
            case TELARANIN: return ARA_L;
            case MADAME: return MAD_L;
            case GOTERA: return GOTA_L;
            case MOHO: return MOHO_L;
            case CAPITAN: return CAP_L;
            case GELATINA: return GELA_L;
            case SOMBRA: return SOMB_L;
            default: return null;
        }
    }

    private Spr vista(Cosa c) {
        int f = ((int) (c.t * 4)) & 1;
        vAlpha = 256; vBrillo = c.tipo >= BALA; vAlt = 0; vEsc = escBase(c);
        switch (c.tipo) {
            case MUGRITO: return (niv.disfraz ? MUG_COC : MUG)[f];
            case TIRADOR: return c.disp > 0 ? TIR_A : (niv.disfraz ? TIR_COC : TIR)[f];
            case REY: return REYS[((int) (c.t * 3)) & 1];
            case MIGAJIN: return MIGA[f];
            case TOMATON: return c.sub == 1 ? TOMA_AV[f] : c.sub == 3 ? TOMA_MAR : TOMA[f];
            case GRASON:
                if (c.fase == 1) return c.sub == 0 ? GRAS_G : GRAS_A;
                return c.sub == 3 ? GRAS_X : GRAS_E[f];
            case PELUSON: {
                double dd = Math.hypot(c.x - px, c.y - py);
                vAlpha = c.estado == QUIETO ? (dd > 3.4 ? 34 : dd < 2.6 ? 256 : 34 + (int) ((3.4 - dd) / 0.8 * 222)) : 256;
                return PELU[f];
            }
            case TELARANIN:
                if (c.sub >= 2) { return ARA[f]; }
                vAlt = c.sub == 0 ? 0.42 : 0.42 * (1 - Math.min(1, c.t2 / 0.5));
                vEsc = Math.max(0.5, 1.0 - vAlt);
                return ARA_COLG;
            case MADAME:
                vBrillo = apagon > 0.5;
                if (c.fase == 1 && c.sub == 0) { vAlt = 0.45 + 0.02 * Math.sin(c.t * 3); vEsc = 0.55; return MAD[f]; }
                if (c.fase == 1 && (c.sub == 1 || c.sub == 3)) {
                    double p = Math.min(1, c.t2 / 0.6); if (c.sub == 3) p = 1 - p;
                    vAlt = 0.45 * (1 - p); vEsc = 0.55 + 0.75 * p; return MAD_HILO;
                }
                vEsc = 1.3;
                return c.fase == 2 && c.sub == 11 ? MAD_ENRED : MAD[f];
            case GOTERA:
                if (c.sub == 0) { vAlt = 0.45; vEsc = 0.6; vAlpha = ((int) (c.t * 8)) % 2 == 0 ? 256 : 70; return GOTA[0]; }
                if (c.sub == 1) { vAlt = 0.45 * (1 - Math.min(1, c.t2 / 0.15)); vEsc = 0.6; return GOTA_CAE; }
                vEsc = 0.6; return GOTA_CHARCO;
            case MOHO: return MOHO_S[f];
            case CAPITAN: return c.inmune ? CAP_ESC : (c.fase == 2 && c.hp <= c.maxHp * 0.5 ? CAP_ENFU : CAP[((int) (c.t * 2)) & 1]);
            case GELATINA: return GELA[f];
            case SOMBRA:
                vAlpha = c.sub == 1 ? 256 : (int) (140 + 90 * Math.sin(c.t * 9));
                return c.sub == 1 ? SOMB_AV : SOMB[f];
            case MADRE:
                vBrillo = false;
                if (c.fase == 1) { vEsc = 1.1; return MADRE_C[((int) (c.t * 2)) & 1]; }
                if (c.fase == 2) { vEsc = 1.4; return MADRE_T[((int) (c.t * 2)) & 1]; }
                vEsc = 1.5; return MADRE_G[((int) (c.t * 3)) & 1];
            case ORBE: vBrillo = true; vEsc = 0.5; vAlt = 0.3 + 0.05 * Math.sin(c.t * 4); return ORBE_S[c.n % 3];
            case CAPULLO: vEsc = 0.7; vAlt = 0.3; return CAPULLO_S[((int) (c.t * 6)) & 1];
            case JABON: case BURBUJAS: case ESTRELLA: case LLAVE: case BGRANDE: case SUPERJ: case ESCUDO: case LINTERNA: case TURBO:
                vEsc = 0.42; vAlt = 0.04 + 0.03 * Math.sin(c.t * 3);
                switch (c.tipo) {
                    case JABON: return SP_JAB;
                    case BURBUJAS: return SP_BUR;
                    case ESTRELLA: return SP_EST;
                    case LLAVE: return LLAVES[Math.max(0, niv.llave)];
                    case BGRANDE: return SP_BGRANDE;
                    case SUPERJ: return SP_SUPERJ;
                    case ESCUDO: return SP_ESCUDO;
                    case LINTERNA: return SP_LINTERNA;
                    default: return SP_TURBO;
                }
            case BALA: vEsc = c.esc; vAlt = c.alt; return SP_BALA;
            case MUGRE: vEsc = c.esc; vAlt = c.alt; return SP_MUGRE;
            case TOMATE: vEsc = 0.3; vAlt = c.alt; return SP_TOMATE;
            case RED: vEsc = 0.34; vAlt = c.alt; return SP_RED;
            case ANCLA: vEsc = 0.45; vAlt = c.alt; return SP_ANCLA;
            case ONDA: vEsc = 0.4; vAlt = c.alt; return SP_ONDA;
            case ESPORABOLA: vEsc = 0.3; vAlt = c.alt; return SP_ESPORA;
            case GOTAFIJA: {
                double ph = c.t % 3.2;
                if (ph >= 0.8 && ph < 1.0) { vEsc = 0.5; vAlt = 0.9 * (1 - (ph - 0.8) / 0.2); return GOTA_CAE; }
                if (ph >= 1.0 && ph < 1.7) { vEsc = 0.6; return GOTA_CHARCO; }
                return null;
            }
            case CHARCO: vEsc = 0.6; return CHARCO_MOHO;
            case NUBE: vEsc = c.esc; vAlt = 0; vBrillo = true; return ESPORAS[((int) (c.t * 6)) & 1];
            case DUCHA: vEsc = 1.0; vAlt = 0; vBrillo = false; return SP_DUCHA;
            case LLUVIA: vEsc = 0.4; vAlt = Math.max(0, 1.0 - c.t / 0.9); return SP_MUGRE;
            case SOMBRAGOTA: vEsc = c.esc; vAlt = 0; return SOMBRA_GOTA;
            default: vEsc = 0.8; vAlt = 0.15; return POPS[Math.min(2, (int) (c.t / 0.1))];
        }
    }

    private final List<Cosa> orden = new ArrayList<>(), poolSombras = new ArrayList<>();
    private int usoSombras;

    private Cosa sombraTmp(Cosa de, double esc) {
        if (usoSombras >= poolSombras.size()) poolSombras.add(new Cosa());
        Cosa s = poolSombras.get(usoSombras++);
        s.tipo = SOMBRAGOTA; s.x = de.x; s.y = de.y; s.esc = esc; s.activa = true; s.estado = 0;
        return s;
    }

    private void dibujarCosas() {
        orden.clear(); usoSombras = 0;
        for (Cosa c : cosas) {
            if (!c.activa) continue;
            if (c.tipo == GOTAFIJA && c.t % 3.2 < 1.0) { Cosa s = sombraTmp(c, 0.35 + 0.3 * ((c.t % 3.2))); addOrden(s); }
            else if (c.tipo == GOTERA && c.estado == PERSIGUE && c.sub == 0) { addOrden(sombraTmp(c, 0.35 + 0.3 * Math.min(1, c.t2))); }
            else if (c.tipo == LLUVIA && c.t < 0.9) { addOrden(sombraTmp(c, 0.25 + 0.4 * (c.t / 0.9))); }
            addOrden(c);
        }
        orden.sort(Comparator.comparingDouble((Cosa c) -> c.dist).reversed());
        for (Cosa c : orden) {
            Spr s;
            double esc, alt;
            boolean muriendo = esEnemigo(c.tipo) && c.estado == MURIENDO && c.tipo != MADRE;
            vAlpha = 256; vBrillo = false;
            boolean jefeGrande = c.tipo == REY || c.tipo == GRASON || c.tipo == MADAME || c.tipo == CAPITAN;
            if (muriendo) {
                Spr l = limpioDe(c);
                if (l != null && c.t < 0.4) {
                    s = l; esc = escBase(c) * (1 + c.t * 0.4); alt = c.t * 0.5;
                } else {
                    s = POPS[Math.min(2, (int) (Math.max(0, c.t - (l != null ? 0.4 : 0)) / 0.1))];
                    esc = jefeGrande ? 1.3 : 0.8; alt = 0.15;
                }
            } else if (c.tipo == MADRE && c.estado == MURIENDO) {
                s = MADRE_L;
                double p = Math.min(1, c.t / 4.5);
                esc = 1.3 * (1 - p * 0.75); alt = Math.min(1.0, c.t * 0.18); vAlpha = 256; vBrillo = true;
            } else {
                s = vista(c);
                if (s == null) continue;
                esc = vEsc; alt = vAlt;
            }
            double sx = c.x - px, sy = c.y - py;
            double tY = c.dist;
            double tX = (dirX * sy - dirY * sx) / PLANO;
            int pantX = (int) (VW / 2.0 * (1 + tX / tY));
            int top = (int) (VH / 2.0 + (0.5 - alt - esc) * VH / tY);
            int bot = (int) (VH / 2.0 + (0.5 - alt) * VH / tY);
            int h = bot - top;
            if (h < 1) continue;
            int x0 = pantX - h / 2;
            if (c.tipo == TOMATON && c.sub == 1 && !muriendo) x0 += (int) (Math.sin(t * 70) * h * 0.04);
            boolean brillante = vBrillo || (c.tipo >= BALA && c.tipo != CHARCO && c.tipo != DUCHA);
            int sh = brillante ? 256 : Math.max(niv.niebla ? 70 : 150, luz(tY));
            if (!brillante && niv.niebla) sh = Math.min(256, luz(tY) + 40);
            boolean blanco = c.flash > 0;
            boolean oro = c.guardia && !muriendo;
            int alpha = vAlpha;
            int fgs = (!brillante) ? niebla(tY, false) : 0;
            for (int x = Math.max(0, x0); x < Math.min(VW, x0 + h); x++) {
                if (tY >= zbuf[x]) continue;
                int tx = (x - x0) * s.w / h;
                for (int y = Math.max(0, top); y < Math.min(VH, bot); y++) {
                    int ty = (y - top) * s.h / h;
                    int argb = s.px[ty * s.w + tx];
                    if ((argb >>> 24) < 128) continue;
                    int col = argb & 0xFFFFFF;
                    if (blanco) col = mezclaBlanco(col);
                    if (oro) col = mezclar(col, 0xFFD54F, 70);
                    col = sh == 256 ? col : sombra(col, sh);
                    if (fgs > 0) col = mezclar(col, sombra(COLOR_NIEBLA, Math.max(40, sh)), fgs);
                    if (alpha < 256) col = mezclar(pix[y * VW + x], col, alpha);
                    pix[y * VW + x] = col;
                }
            }
            // brillos
            if (c.tipo == MADAME && apagon > 0.4 && !muriendo && pantX >= 0 && pantX < VW && tY < zbuf[pantX]) halo(pantX, (top + bot) / 2, (int) (h * 0.75), 0xFF9AD5, 120, tY);
            else if (c.tipo == ORBE && pantX >= 0 && pantX < VW && tY < zbuf[pantX]) halo(pantX, (top + bot) / 2, (int) (h * 0.9), c.n == 0 ? 0xFF6E40 : c.n == 1 ? 0xCE93D8 : 0xAED581, 110, tY);
            else if (oro && pantX >= 0 && pantX < VW && tY < zbuf[pantX]) halo(pantX, (top + bot) / 2, (int) (h * 0.7), 0xFFD54F, 50, tY);
            else if (c.tipo == MADRE && c.estado == MURIENDO && pantX >= 0 && pantX < VW) halo(pantX, (top + bot) / 2, (int) (h * 1.2), 0xB3E5FC, 100, tY);
        }
    }

    private void addOrden(Cosa c) {
        double sx = c.x - px, sy = c.y - py;
        c.dist = sx * dirX + sy * dirY;
        if (c.dist > 0.15) orden.add(c);
    }

    private void halo(int cx, int cy, int r, int rgb, int fuerza, double tY) {
        r = Math.min(r, 130);
        if (r < 2) return;
        int cr = (rgb >> 16) & 255, cg = (rgb >> 8) & 255, cb = rgb & 255;
        for (int y = Math.max(0, cy - r); y < Math.min(VH, cy + r); y++) {
            for (int x = Math.max(0, cx - r); x < Math.min(VW, cx + r); x++) {
                if (tY >= zbuf[x]) continue;
                double dd = Math.hypot(x - cx, y - cy) / r;
                if (dd >= 1) continue;
                int a = (int) ((1 - dd) * (1 - dd) * fuerza);
                int p = pix[y * VW + x];
                int nr = Math.min(255, ((p >> 16) & 255) + (cr * a >> 8)), ng = Math.min(255, ((p >> 8) & 255) + (cg * a >> 8)), nb = Math.min(255, (p & 255) + (cb * a >> 8));
                pix[y * VW + x] = (nr << 16) | (ng << 8) | nb;
            }
        }
    }

    private static int mezclaBlanco(int c) {
        int r = ((c >> 16) & 255), g = ((c >> 8) & 255), b = c & 255;
        r = r + (255 - r) * 4 / 10; g = g + (255 - g) * 4 / 10; b = b + (255 - b) * 4 / 10;
        return (r << 16) | (g << 8) | b;
    }

    // ================================================================== pantalla

    private final java.util.Map<Spr, BufferedImage> cacheImg = new java.util.HashMap<>();

    private BufferedImage img(Spr s) {
        BufferedImage b = cacheImg.get(s);
        if (b == null) {
            b = new BufferedImage(s.w, s.h, BufferedImage.TYPE_INT_ARGB);
            b.setRGB(0, 0, s.w, s.h, s.px, 0, s.w);
            cacheImg.put(s, b);
        }
        return b;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (estado == Estado.SELECCION) { seleccion(g); g.dispose(); return; }
        if (estado == Estado.CAIDA) { caida(g); g.dispose(); return; }

        if (estado != Estado.FIN) {
            renderizar();
            int ox = 0, oy = 0;
            if (shakeT > 0) { double a = Math.min(1, shakeT); ox = (int) ((Math.random() - 0.5) * 16 * a); oy = (int) ((Math.random() - 0.5) * 10 * a); }
            g.setClip(0, 0, W, HUDY);
            g.drawImage(buf, ox, oy, W, HUDY, null);
            if (golpeT > 0) { g.setColor(new Color(141, 110, 99, (int) (110 * golpeT / 0.3))); g.fillRect(0, 0, W, HUDY); }
            if (brilloT > 0) { g.setColor(new Color(255, 240, 150, (int) (80 * brilloT / 0.25))); g.fillRect(0, 0, W, HUDY); }
            if (shakeT > 0) { g.setColor(new Color(170, 170, 170, (int) (70 * Math.min(1, shakeT)))); g.fillRect(0, 0, W, HUDY); }
            if (niv.agua != null && valvulas < 3 && px >= niv.agua[0] && px <= niv.agua[2] + 1 && py >= niv.agua[1] && py <= niv.agua[3] + 1) {
                g.setPaint(new GradientPaint(0, HUDY - 150, new Color(40, 130, 210, 0), 0, HUDY, new Color(40, 130, 210, (int) (70 * (1 - valvulas / 3.0)))));
                g.fillRect(0, HUDY - 150, W, 150);
            }
            if (finalT > 3.5) { g.setColor(new Color(255, 255, 255, (int) Math.min(200, (finalT - 3.5) / 1.8 * 200))); g.fillRect(0, 0, W, HUDY); }
            g.setClip(null);
            // mira
            g.setColor(new Color(255, 255, 255, 220)); g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(W / 2.0 - 8, HUDY / 2.0 - 8, 16, 16));
            g.fill(new Ellipse2D.Double(W / 2.0 - 2, HUDY / 2.0 - 2, 4, 4));
            arma(g);
            hud(g);
            if (estado == Estado.JUGANDO || estado == Estado.PAUSA) { estados(g); barraJefe(g); }
            if (msgT > 0 && estado == Estado.JUGANDO) {
                int a = (int) Math.min(255, msgT * 400);
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                int w = Math.min(W - 40, g.getFontMetrics().stringWidth(msg));
                g.setColor(new Color(30, 20, 60, a * 3 / 4)); g.fill(new RoundRectangle2D.Double(W / 2.0 - w / 2.0 - 16, 108, w + 32, 40, 24, 24));
                g.setColor(new Color(255, 255, 255, a)); g.drawString(msg, (float) (W / 2.0 - w / 2.0), 135);
            }
            g.setComposite(AlphaComposite.SrcOver);
            fx.dibujar(g);
        }
        switch (estado) {
            case INTRO: intro(g); break;
            case PAUSA:
                velo(g, 150);
                texto(g, "PAUSA", W / 2, 280, 80, Color.WHITE);
                texto(g, "P / Enter: seguir     Esc: elegir capítulo", W / 2, 335, 22, new Color(0xE0D8FF));
                break;
            case MUERTO:
                velo(g, 170);
                texto(g, "¡Pou quedó todo sucio!", W / 2, 250, 58, new Color(0xFFD9A0));
                texto(g, "Se va a bañar y vuelve a intentarlo...", W / 2, 305, 26, Color.WHITE);
                texto(g, "Enter: reintentar     Esc: elegir capítulo", W / 2, 370, 24, new Color(0xE0D8FF));
                break;
            case FIN: if (cap == ShooterNiveles.TOTAL && finPag == 1) finalGeneral(g); else fin(g); fx.dibujar(g); break;
            default: break;
        }
        g.dispose();
    }

    private void velo(Graphics2D g, int a) { g.setColor(new Color(10, 5, 30, a)); g.fillRect(0, 0, W, H); }

    private void arma(Graphics2D g) {
        double bob = Math.sin(bobT) * 6, bob2 = Math.abs(Math.cos(bobT)) * 5;
        double rec = Math.max(0, disparoT) / 0.18 * 24;
        double cx = W / 2.0 + bob * 1.4, by = HUDY + 46 + bob2 + rec;
        Shape clip = g.getClip();
        g.setClip(0, 0, W, HUDY + 4);
        g.setColor(c(0xFF8FB1)); g.fill(new RoundRectangle2D.Double(cx - 26, by - 100, 52, 110, 22, 22));
        g.setColor(c(0xE0577F)); g.setStroke(new BasicStroke(4f)); g.draw(new RoundRectangle2D.Double(cx - 26, by - 100, 52, 110, 22, 22));
        Path2D cu = new Path2D.Double();
        cu.moveTo(cx - 70, by); cu.lineTo(cx + 70, by); cu.lineTo(cx + 36, by - 130); cu.lineTo(cx - 36, by - 130); cu.closePath();
        g.setColor(turbo ? c(0xFF7043) : c(0x4FC3F7)); g.fill(cu);
        g.setColor(turbo ? c(0xFFAB91) : c(0x9BE1FF)); g.fill(new Rectangle2D.Double(cx - 22, by - 120, 12, 100));
        g.setColor(turbo ? c(0xD84315) : c(0x0288D1)); g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(cu);
        g.setColor(c(0xFFE082)); g.fill(new RoundRectangle2D.Double(cx - 17, by - 200, 34, 80, 10, 10));
        g.setColor(c(0xE0A800)); g.setStroke(new BasicStroke(4f)); g.draw(new RoundRectangle2D.Double(cx - 17, by - 200, 34, 80, 10, 10));
        g.setColor(new Color(190, 235, 255, 120)); g.fill(new Ellipse2D.Double(cx - 46, by - 262, 92, 72));
        g.setColor(bgrande > 0 ? c(0x00B0FF) : c(0xFF8FB1)); g.setStroke(new BasicStroke(bgrande > 0 ? 13f : 9f)); g.draw(new Ellipse2D.Double(cx - 46, by - 262, 92, 72));
        g.setColor(new Color(255, 255, 255, 230)); g.fill(new Ellipse2D.Double(cx - 28, by - 250, 18, 10));
        if (disparoT > 0) {
            double p = 1 - disparoT / 0.18;
            for (int i = 0; i < 6; i++) {
                double a = -Math.PI / 2 + (i - 2.5) * 0.35, d = 40 + p * 150;
                double bx = cx + Math.cos(a) * d, byy = by - 226 + Math.sin(a) * d - p * 20, r = 8 + (i % 3) * 5;
                g.setColor(new Color(190, 235, 255, (int) (190 * (1 - p)))); g.fill(new Ellipse2D.Double(bx - r, byy - r, r * 2, r * 2));
                g.setColor(new Color(255, 255, 255, (int) (230 * (1 - p)))); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(bx - r, byy - r, r * 2, r * 2));
            }
        }
        g.setClip(clip);
    }

    private void hud(Graphics2D g) {
        g.setPaint(new GradientPaint(0, HUDY, c(0x4B3A94), 0, H, c(0x2A1B5E))); g.fillRect(0, HUDY, W, H - HUDY);
        g.setColor(c(0xFFE066)); g.fillRect(0, HUDY, W, 4);
        etiqueta(g, "BURBUJAS", 40, HUDY + 28);
        numero(g, String.valueOf(municion), 40, HUDY + 80, municion > 10 ? Color.WHITE : (municion > 0 ? c(0xFFB74D) : c(0xEF5350)));
        if (municion <= 0 && (((int) (t * 3)) & 1) == 0) { g.setFont(new Font("SansSerif", Font.BOLD, 13)); g.setColor(c(0xFF8A80)); g.drawString("¡SIN BURBUJAS!", 40, HUDY + 94); }
        etiqueta(g, "LIMPIEZA", 200, HUDY + 28);
        numero(g, (int) Math.ceil(limpieza) + "%", 200, HUDY + 80, limpieza > 30 ? Color.WHITE : c(0xFFB74D));
        g.setColor(new Color(255, 255, 255, 50)); g.fill(new RoundRectangle2D.Double(202, HUDY + 86, 130, 8, 8, 8));
        g.setColor(limpieza > 30 ? c(0x4FC3F7) : c(0xFFB74D)); g.fill(new RoundRectangle2D.Double(202, HUDY + 86, Math.max(8, 130 * limpieza / 100), 8, 8, 8));
        if (escudoEsp > 0) { g.setColor(c(0xE1F5FE)); g.fill(new RoundRectangle2D.Double(202, HUDY + 86, 130 * Math.min(30, escudoEsp) / 30.0, 4, 4, 4)); }

        // cara de Pou
        g.setColor(new Color(20, 10, 50, 150)); g.fill(new RoundRectangle2D.Double(W / 2.0 - 56, HUDY + 10, 112, 80, 22, 22));
        if (dorado) { g.setColor(c(0xFFD54F)); g.setStroke(new BasicStroke(3f)); g.draw(new RoundRectangle2D.Double(W / 2.0 - 56, HUDY + 10, 112, 80, 22, 22)); }
        PouArte.Cara cara = estado == Estado.MUERTO ? PouArte.Cara.ENFERMO
                : golpeT > 0 ? PouArte.Cara.SORPRESA
                : disparoT > 0 && limpieza > 30 ? PouArte.Cara.RISA
                : limpieza > 60 ? PouArte.Cara.FELIZ : (limpieza > 30 ? PouArte.Cara.NORMAL : PouArte.Cara.TRISTE);
        rostro.cara = cara;
        rostro.miraX = Math.sin(t * 0.8) * 0.4;
        rostro.parpadeo = (t % 3.4) > 3.3 ? 1 : 0;
        Shape cl = g.getClip();
        g.setClip(new Rectangle2D.Double(W / 2.0 - 56, HUDY + 10, 112, 80));
        PouArte.dibujar(g, W / 2.0, H - 6, 0.40, ropa, rostro);
        g.setClip(cl);

        // llave, germenes y corona
        if (niv.llave >= 0) {
            etiqueta(g, "LLAVE", 560, HUDY + 28);
            if (tieneLlave) g.drawImage(img(LLAVES[niv.llave]), 560, HUDY + 36, 52, 52, null);
            else { g.setColor(new Color(255, 255, 255, 60)); g.setStroke(new BasicStroke(2f)); g.draw(new RoundRectangle2D.Double(564, HUDY + 46, 44, 32, 10, 10)); }
        }
        etiqueta(g, "GÉRMENES", 660, HUDY + 28);
        int quedan = 0;
        for (Cosa c : cosas) if (c.activa && esEnemigo(c.tipo) && c.cuenta && c.estado != MURIENDO) quedan++;
        numero(g, String.valueOf(quedan), 660, HUDY + 80, Color.WHITE);
        if (cap >= 2) {
            etiqueta(g, "CORONA", 790, HUDY + 28);
            int rec = coronas();
            for (int i = 0; i < 3; i++) corona(g, 790 + i * 30, HUDY + 52, i < rec);
            g.setFont(new Font("SansSerif", Font.BOLD, 15)); g.setColor(new Color(255, 255, 255, 200));
            g.drawString(rec + "/3", 886, HUDY + 58);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 13)); g.setColor(new Color(255, 255, 255, 170));
        g.drawString("Cap. " + cap + " - " + mmss(tiempo), 790, HUDY + 84);
    }

    private void corona(Graphics2D g, double x, double y, boolean tiene) {
        Path2D p = new Path2D.Double();
        p.moveTo(x - 11, y + 10); p.lineTo(x - 10, y - 8); p.lineTo(x - 4, y + 1); p.lineTo(x, y - 11); p.lineTo(x + 4, y + 1); p.lineTo(x + 10, y - 8); p.lineTo(x + 11, y + 10); p.closePath();
        g.setColor(tiene ? c(0xFFD54F) : new Color(255, 255, 255, 40)); g.fill(p);
        g.setColor(tiene ? c(0xB8860B) : new Color(255, 255, 255, 90)); g.setStroke(new BasicStroke(2f)); g.draw(p);
    }

    private void chip(Graphics2D g, int y, String s, Color col) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        int w = g.getFontMetrics().stringWidth(s) + 20;
        g.setColor(new Color(20, 10, 50, 170)); g.fill(new RoundRectangle2D.Double(12, y, w, 24, 14, 14));
        g.setColor(col); g.draw(new RoundRectangle2D.Double(12, y, w, 24, 14, 14));
        g.setColor(Color.WHITE); g.drawString(s, 22, y + 17);
    }

    private void estados(Graphics2D g) {
        int y = 12;
        g.setStroke(new BasicStroke(2f));
        if (escudoEsp > 0) { chip(g, y, "ESCUDO DE ESPUMA " + (int) Math.ceil(escudoEsp), c(0x81D4FA)); y += 30; }
        if (bgrande > 0) { chip(g, y, "BURBUJA GRANDE x" + bgrande, c(0x00B0FF)); y += 30; }
        if (turbo) { chip(g, y, "BURBUJERO TURBO", c(0xFF7043)); y += 30; }
        if (linterna) { chip(g, y, "LINTERNA", c(0xFFEE58)); y += 30; }
        if (lentoT > 0) { chip(g, y, "TELARAÑA: LENTO " + (int) Math.ceil(lentoT), c(0xB39DDB)); y += 30; }
        if (niv.num == 4) { chip(g, y, "VÁLVULAS " + valvulas + "/3", valvulas >= 3 ? c(0x66BB6A) : c(0xEF5350)); y += 30; }
        if (niv.num == 5 && jefe != null && jefe.fase == 3 && !jefeMuerto) {
            if (cadenaCd > 0) chip(g, y, "DUCHA: " + (int) Math.ceil(cadenaCd) + " s", c(0x90A4AE));
            else chip(g, y, "DUCHA LISTA", c(0x66BB6A));
        }
        if (girando) {
            double w = 220, x = W / 2.0 - w / 2, yy = HUDY - 70;
            g.setColor(new Color(20, 10, 50, 190)); g.fill(new RoundRectangle2D.Double(x - 10, yy - 26, w + 20, 52, 18, 18));
            g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.setColor(Color.WHITE); g.drawString("Girando la válvula...", (float) x + 40, (float) yy - 6);
            g.setColor(new Color(255, 255, 255, 60)); g.fill(new RoundRectangle2D.Double(x, yy, w, 12, 10, 10));
            g.setColor(c(0xFFE066)); g.fill(new RoundRectangle2D.Double(x, yy, Math.max(8, w * valvProg), 12, 10, 10));
        }
        if (niv.num == 5 && jefe != null && jefe.fase == 3 && cadenaCd <= 0 && !jefeMuerto && Math.hypot(jefe.x - duX, jefe.y - duY) <= 2.1) {
            double p = 0.5 + 0.5 * Math.sin(t * 12);
            texto(g, "¡AHORA! Tirá de la cadena (E)", W / 2, 190, 30, new Color(255, (int) (200 + 55 * p), 80));
        }
        if (apagon > 0.5 && !linterna) texto(g, "¡Está muy oscuro! Se ve muy poco...", W / 2, HUDY - 20, 18, new Color(255, 255, 255, 170));
    }

    private void barraJefe(Graphics2D g) {
        Cosa j = jefe;
        if (j == null || !j.activa || j.estado != PERSIGUE || estado != Estado.JUGANDO) return;
        int bw = 460, bx = (W - bw) / 2, by = 84;
        boolean inm = inmuneAhora(j) || (j.tipo == MADAME && j.fase == 1 && j.sub != 2);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String nombre = niv.jefe + (inm ? "  (¡INMUNE!)" : "");
        int sw = g.getFontMetrics().stringWidth(nombre);
        g.setColor(new Color(0, 0, 0, 120)); g.drawString(nombre, W / 2 - sw / 2 + 2, by - 6);
        g.setColor(inm ? c(0xB0BEC5) : Color.WHITE); g.drawString(nombre, W / 2 - sw / 2, by - 8);
        g.setColor(new Color(20, 10, 50, 190)); g.fill(new RoundRectangle2D.Double(bx - 4, by - 2, bw + 8, 22, 14, 14));
        double p = Math.max(0, Math.min(1, j.hp / j.maxHp));
        Color col = inm ? c(0x90A4AE) : p > 0.5 ? c(0x66BB6A) : p > 0.25 ? c(0xFFCA28) : c(0xEF5350);
        g.setColor(col); g.fill(new RoundRectangle2D.Double(bx, by, Math.max(6, bw * p), 18, 12, 12));
        g.setColor(new Color(255, 255, 255, 110)); g.setStroke(new BasicStroke(2f));
        double[] marcas = j.tipo == MADRE ? new double[]{0.67, 0.34} : (j.tipo == GRASON || j.tipo == MADAME) ? new double[]{0.5} : new double[0];
        for (double m : marcas) g.draw(new Line2D.Double(bx + bw * m, by, bx + bw * m, by + 18));
        if (j.tipo == CAPITAN && valvulas < 3) {
            for (int i = 0; i < 3 - valvulas; i++) { g.setColor(c(0x4FC3F7)); g.fill(new RoundRectangle2D.Double(bx + 6 + i * 50, by + 24, 44, 8, 6, 6)); }
            g.setFont(new Font("SansSerif", Font.BOLD, 12)); g.setColor(Color.WHITE); g.drawString("ESCUDO DE AGUA", bx + bw - 110, by + 32);
        }
        if (j.tipo == MADRE) {
            String f = j.fase == 1 ? "CHARCO" : j.fase == 2 ? "TORRE: " + orbesVivos() + " orbes" : "LA GRAN MANCHA";
            g.setFont(new Font("SansSerif", Font.BOLD, 12)); g.setColor(Color.WHITE); g.drawString(f, bx + 6, by + 32);
        }
    }

    private BufferedImage cacheLlave;

    private void etiqueta(Graphics2D g, String s, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.setColor(c(0xFFE066)); g.drawString(s, x, y);
    }

    private void numero(Graphics2D g, String s, int x, int y, Color col) {
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.setColor(new Color(0, 0, 0, 110)); g.drawString(s, x + 2, y + 3);
        g.setColor(col); g.drawString(s, x, y);
    }

    private void texto(Graphics2D g, String s, int cx, int y, int tam, Color col) {
        g.setFont(new Font("SansSerif", Font.BOLD, tam));
        int w = g.getFontMetrics().stringWidth(s);
        g.setColor(new Color(0, 0, 0, 110)); g.drawString(s, cx - w / 2 + 3, y + 4);
        g.setColor(col); g.drawString(s, cx - w / 2, y);
    }

    private boolean dorado;

    // ------------------------------------------------------------------ selector de capitulos

    private static final int[] COLORES_CAP = {0xFF9CC4, 0xFFB74D, 0xB39DDB, 0x4DD0E1, 0x90A4AE};

    private Spr spriteJefe(int n) {
        switch (n) { case 1: return REYS[0]; case 2: return GRAS_G; case 3: return MAD[0]; case 4: return CAP[0]; default: return MADRE_G[0]; }
    }

    private void insignia(Graphics2D g, double cx, double cy, double r, String rg) {
        Color rc = rg.equals("S") ? c(0xFFD54F) : rg.equals("A") ? c(0x66BB6A) : rg.equals("B") ? c(0x4FC3F7) : rg.equals("C") ? c(0xFFB300) : c(0xEF5350);
        g.setColor(new Color(0, 0, 0, 80)); g.fill(new Ellipse2D.Double(cx - r + 2, cy - r + 3, r * 2, r * 2));
        g.setColor(rc); g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke((float) Math.max(2, r / 9))); g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (r * 1.3)));
        int rw = g.getFontMetrics().stringWidth(rg);
        g.drawString(rg, (int) (cx - rw / 2.0), (int) (cy + r * 0.45));
    }

    private void seleccion(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, c(0x3B2A78), 0, H, c(0x7E63D8))); g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 255, 255, 22));
        for (int i = 0; i < 26; i++) g.fill(new Ellipse2D.Double((i * 151.7 + t * 10) % (W + 80) - 40, (i * 89.3 + t * 6) % H, 30 + (i % 4) * 12, 30 + (i % 4) * 12));
        texto(g, "OPERACIÓN MUGRE", W / 2, 85, 56, c(0xFFE066));
        texto(g, "Elegí un capítulo", W / 2, 130, 24, Color.WHITE);
        String gen = rangoGeneral();
        boolean alguno = false;
        for (int n = 1; n <= ShooterNiveles.TOTAL; n++) if (!rangoGuardado(n).isEmpty()) alguno = true;
        if (alguno) { texto(g, "RANGO GENERAL", W - 120, 60, 14, Color.WHITE); insignia(g, W - 120, 105, 26, gen); }
        if (dorado) texto(g, "★ Traje dorado de limpieza desbloqueado ★", W / 2, 165, 18, c(0xFFD54F));
        for (int i = 0; i < ShooterNiveles.TOTAL; i++) {
            ShooterNiveles.Nivel nv = ShooterNiveles.get(i + 1);
            Rectangle2D r = rCarta(i);
            boolean abierto = desbloqueado(i + 1), es = i == sel;
            double up = es ? -10 + Math.sin(t * 4) * 3 : 0;
            double x = r.getX(), y = r.getY() + up, w = r.getWidth(), h = r.getHeight();
            g.setColor(new Color(0, 0, 0, 70)); g.fill(new RoundRectangle2D.Double(x + 3, y + 7, w, h, 26, 26));
            g.setColor(abierto ? c(0xFFF8E1) : c(0x2B2255)); g.fill(new RoundRectangle2D.Double(x, y, w, h, 26, 26));
            g.setColor(es ? c(0xFFE066) : c(COLORES_CAP[i])); g.setStroke(new BasicStroke(es ? 6f : 4f)); g.draw(new RoundRectangle2D.Double(x, y, w, h, 26, 26));
            g.setColor(c(COLORES_CAP[i])); g.fill(new RoundRectangle2D.Double(x + 8, y + 8, w - 16, 30, 18, 18));
            g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.setColor(Color.WHITE);
            String ct = "CAPÍTULO " + (i + 1); g.drawString(ct, (int) (x + w / 2 - g.getFontMetrics().stringWidth(ct) / 2), (int) (y + 30));
            Spr sp = spriteJefe(i + 1);
            g.drawImage(img(sp), (int) (x + w / 2 - 55), (int) (y + 44), 110, 110, null);
            if (!abierto) { g.setColor(new Color(20, 14, 50, 205)); g.fill(new RoundRectangle2D.Double(x + 4, y + 40, w - 8, 120, 20, 20)); candadoGrande(g, x + w / 2, y + 100); }
            String[] pal = nv.titulo.split(" ");
            String l1 = "", l2 = "";
            for (String p : pal) { if (l1.length() + p.length() < 14 && l2.isEmpty()) l1 += (l1.isEmpty() ? "" : " ") + p; else l2 += (l2.isEmpty() ? "" : " ") + p; }
            g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.setColor(abierto ? c(0x5A3E8E) : c(0x9C8FCB));
            g.drawString(l1, (int) (x + w / 2 - g.getFontMetrics().stringWidth(l1) / 2), (int) (y + 182));
            g.drawString(l2, (int) (x + w / 2 - g.getFontMetrics().stringWidth(l2) / 2), (int) (y + 202));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(abierto ? c(0x3B2A58) : c(0x8A7FBF));
            String jf = "Jefe: " + capitalizar(nv.jefe);
            g.drawString(jf, (int) (x + w / 2 - g.getFontMetrics().stringWidth(jf) / 2), (int) (y + 228));
            String mon = "+" + nv.monedas + " monedas";
            g.drawString(mon, (int) (x + w / 2 - g.getFontMetrics().stringWidth(mon) / 2), (int) (y + 247));
            String rg = rangoGuardado(i + 1);
            if (!rg.isEmpty()) insignia(g, x + w - 30, y + 260, 20, rg);
            else if (abierto) { g.setFont(new Font("SansSerif", Font.BOLD, 13)); g.setColor(c(0xE0577F)); g.drawString("¡Nuevo!", (int) (x + 14), (int) (y + 272)); }
        }
        ShooterNiveles.Nivel s = ShooterNiveles.get(sel + 1);
        texto(g, s.titulo + " · " + s.sub, W / 2, 530, 22, Color.WHITE);
        texto(g, "Rango S en " + mmss(s.par) + "   ·   Jefe: " + s.jefe + (desbloqueado(sel + 1) ? "" : "   ·   (terminá el capítulo anterior)"), W / 2, 560, 16, new Color(0xE0D8FF));
        double p = 0.5 + 0.5 * Math.sin(t * 4);
        texto(g, "← →: elegir     Enter: jugar     Esc: volver", W / 2, 610, 20, new Color(255, (int) (170 + 60 * p), 90));
    }

    private static String capitalizar(String s) {
        StringBuilder sb = new StringBuilder();
        for (String p : s.toLowerCase().split(" ")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private void candadoGrande(Graphics2D g, double cx, double cy) {
        g.setColor(c(0xFFD54F)); g.setStroke(new BasicStroke(6f)); g.draw(new Arc2D.Double(cx - 15, cy - 38, 30, 34, 0, 180, Arc2D.OPEN));
        g.fill(new RoundRectangle2D.Double(cx - 24, cy - 10, 48, 36, 10, 10));
        g.setColor(c(0x7A5B00)); g.fill(new Ellipse2D.Double(cx - 5, cy - 1, 10, 10)); g.fillRect((int) cx - 2, (int) cy + 6, 4, 12);
    }

    // ------------------------------------------------------------------ intro, caida y fin

    private void intro(Graphics2D g) {
        velo(g, 200);
        g.setColor(new Color(0xFFF8E1)); g.fill(new RoundRectangle2D.Double(120, 70, 720, 500, 40, 40));
        g.setColor(c(COLORES_CAP[cap - 1])); g.setStroke(new BasicStroke(6f)); g.draw(new RoundRectangle2D.Double(120, 70, 720, 500, 40, 40));
        texto(g, "OPERACIÓN MUGRE", W / 2, 130, 42, c(0xE0577F));
        g.setFont(new Font("SansSerif", Font.BOLD, 22)); g.setColor(c(0x5A3E8E));
        String t1 = "Capítulo " + cap + " - " + niv.titulo;
        g.drawString(t1, W / 2 - g.getFontMetrics().stringWidth(t1) / 2, 165);
        g.setFont(new Font("SansSerif", Font.PLAIN, 19)); g.setColor(c(0x3B2A58));
        for (int i = 0; i < niv.intro.length; i++) g.drawString(niv.intro[i], W / 2 - g.getFontMetrics().stringWidth(niv.intro[i]) / 2, 215 + i * 30);
        g.setFont(new Font("SansSerif", Font.BOLD, 17)); g.setColor(c(0xE0577F));
        String c1 = "CONTROLES";
        g.drawString(c1, W / 2 - g.getFontMetrics().stringWidth(c1) / 2, 395);
        String[] ctl = {
                "W A S D (o flechas arriba/abajo): moverse      Mouse (o flechas izq/der): girar la cámara",
                "Click / Espacio / Ctrl: disparar burbujas      E o Enter: usar puertas, válvulas y cadenas",
                "Shift: correr     Tab: mouse sí/no     P: pausa     M: sonido     Esc: salir",
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.setColor(c(0x3B2A58));
        for (int i = 0; i < ctl.length; i++) g.drawString(ctl[i], W / 2 - g.getFontMetrics().stringWidth(ctl[i]) / 2, 425 + i * 26);
        double p = 0.5 + 0.5 * Math.sin(t * 4);
        texto(g, "Enter / click para empezar", W / 2, 535, 26, new Color(255, (int) (150 + 60 * p), 60));
    }

    private void caida(Graphics2D g) {
        double k = Math.min(1, caidaT / 3.4);
        g.setPaint(new GradientPaint(0, 0, c(0x0D1B24), 0, H, c(0x2C4A3E))); g.fillRect(0, 0, W, H);
        double cx = W / 2.0 + Math.sin(caidaT * 2.3) * 60, cy = H / 2.0 - 40 + Math.cos(caidaT * 1.7) * 30;
        for (int i = 13; i >= 0; i--) {
            double z = ((i / 14.0) + caidaT * 0.55) % 1.0;
            double r = 14 + Math.pow(z, 2.4) * 900;
            double ox = Math.sin(caidaT * 1.3 + i) * 40 * (1 - z), oy = Math.cos(caidaT * 1.1 + i * 0.7) * 30 * (1 - z);
            int shade = (int) (40 + 170 * z);
            g.setColor(new Color(Math.min(255, shade + (i % 3) * 20), Math.min(255, shade * 3 / 4 + 30), Math.min(255, shade / 2 + 40)));
            g.setStroke(new BasicStroke((float) (3 + z * 70)));
            g.draw(new Ellipse2D.Double(cx + ox - r, cy + oy - r * 0.8, r * 2, r * 1.6));
        }
        g.setColor(new Color(255, 255, 255, 120)); g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 24; i++) {
            double bx = (i * 83.7) % W, by = H - ((caidaT * (120 + i * 7) + i * 55) % (H + 60));
            g.draw(new Ellipse2D.Double(bx, by, 14 + i % 3 * 8, 14 + i % 3 * 8));
        }
        rostro.cara = PouArte.Cara.SORPRESA; rostro.miraX = 0; rostro.parpadeo = 0;
        PouArte.dibujar(g, W / 2.0 + Math.sin(caidaT * 5) * 12, H + 40 - 30 * k, 0.85, ropa, rostro);
        texto(g, "¡Un tobogán de caños!", W / 2, 90, 44, Color.WHITE);
        texto(g, "Cap. 5 - El Gran Desagüe", W / 2, 128, 22, c(0xFFE066));
        if (k > 0.85) { g.setColor(new Color(255, 255, 255, (int) ((k - 0.85) / 0.15 * 255))); g.fillRect(0, 0, W, H); }
        texto(g, "Enter: saltear", W - 100, H - 16, 14, new Color(255, 255, 255, 150));
    }

    private void estrella(Graphics2D g, double cx, double cy, double r, Color col) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double rr = i % 2 == 0 ? r : r * 0.42, a = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        g.setColor(col); g.fill(p);
        g.setColor(col.darker()); g.setStroke(new BasicStroke(2f)); g.draw(p);
    }

    private static String mmss(double s) { return String.format("%d:%02d", (int) s / 60, (int) s % 60); }

    private void fin(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, c(0x3B2A78), 0, H, c(0x7E63D8))); g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 255, 255, 22));
        for (int i = 0; i < 26; i++) g.fill(new Ellipse2D.Double((i * 151.7 + t * 10) % (W + 80) - 40, (i * 89.3) % H, 30 + (i % 4) * 12, 30 + (i % 4) * 12));
        texto(g, "¡CAPÍTULO " + cap + " COMPLETO!", W / 2, 72, 52, c(0xFFE066));
        texto(g, niv.titulo, W / 2, 108, 26, Color.WHITE);

        g.setColor(new Color(20, 10, 50, 150)); g.fill(new RoundRectangle2D.Double(150, 130, 660, 260, 36, 36));
        String[] et = {"GÉRMENES LIMPIADOS", "OBJETOS", "SECRETOS"};
        int[] fin = {pEn, pObj, pSec};
        double[] ini = {0.4, 1.2, 2.0};
        for (int i = 0; i < 3; i++) {
            double p = Math.max(0, Math.min(1, (finT - ini[i]) / 0.7));
            if (finT < ini[i]) continue;
            int v = (int) Math.round(fin[i] * p);
            g.setFont(new Font("SansSerif", Font.BOLD, 30)); g.setColor(Color.WHITE);
            g.drawString(et[i], 190, 184 + i * 50);
            String vs = v + "%";
            g.setColor(v == 100 ? c(0xFFE066) : Color.WHITE);
            g.drawString(vs, 770 - g.getFontMetrics().stringWidth(vs), 184 + i * 50);
        }
        if (finT > 2.8) {
            g.setFont(new Font("SansSerif", Font.BOLD, 26)); g.setColor(new Color(255, 255, 255, 230));
            g.drawString("TIEMPO  " + mmss(tiempo), 190, 365);
            String par = "PAR  " + mmss(niv.par);
            g.drawString(par, 770 - g.getFontMetrics().stringWidth(par), 365);
        }
        if (finT > 3.6) {
            double e = Math.min(1, (finT - 3.6) / 0.4);
            double rad = 46 * (0.6 + 0.4 * e) * (1 + (finT < 4.0 ? 0.15 : 0));
            double cy = 450;
            g.setColor(new Color(0, 0, 0, 80)); g.fill(new Ellipse2D.Double(W / 2.0 - rad + 3, cy - rad + 6, rad * 2, rad * 2));
            Color rc = rango.equals("S") ? c(0xFFD54F) : rango.equals("A") ? c(0x66BB6A) : rango.equals("B") ? c(0x4FC3F7) : rango.equals("C") ? c(0xFFB300) : c(0xEF5350);
            g.setColor(rc); g.fill(new Ellipse2D.Double(W / 2.0 - rad, cy - rad, rad * 2, rad * 2));
            g.setColor(Color.WHITE); g.setStroke(new BasicStroke(5f)); g.draw(new Ellipse2D.Double(W / 2.0 - rad, cy - rad, rad * 2, rad * 2));
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (62 * e)));
            int rw = g.getFontMetrics().stringWidth(rango);
            g.setColor(Color.WHITE); g.drawString(rango, (int) (W / 2.0 - rw / 2.0), (int) (cy + 21 * e));
            texto(g, "CALIFICACIÓN", W / 2 - 190, (int) cy + 12, 22, Color.WHITE);
            int estrellas = rango.equals("S") ? 5 : rango.equals("A") ? 4 : rango.equals("B") ? 3 : rango.equals("C") ? 2 : 1;
            for (int i = 0; i < 5; i++) estrella(g, W / 2.0 + 110 + i * 34, cy, 15, i < estrellas ? c(0xFFE066) : new Color(255, 255, 255, 70));
        }
        if (finT > 4.4) {
            for (int i = 0; i < Math.min(12, niv.monedas); i++) moneda(g, W / 2.0 - 280 + i * 28, 520, t * 5 + i * 0.7);
            texto(g, "+" + niv.monedas + " monedas", W / 2 + 160, 530, 28, c(0xFFE066));
        }
        if (finT > 4.6) {
            g.setFont(new Font("SansSerif", Font.ITALIC, 17)); g.setColor(new Color(0xFFF3C4));
            int gw = g.getFontMetrics().stringWidth(niv.gancho);
            if (gw > W - 60) { g.setFont(new Font("SansSerif", Font.ITALIC, 14)); gw = g.getFontMetrics().stringWidth(niv.gancho); }
            g.drawString(niv.gancho, W / 2 - gw / 2, 573);
            if (!niv.proximo.isEmpty()) texto(g, niv.proximo, W / 2, 603, 18, new Color(0xE0D8FF));
            double p = 0.5 + 0.5 * Math.sin(t * 4);
            texto(g, cap == ShooterNiveles.TOTAL ? "Enter: ver el final" : "Enter: continuar", W - 120, 628, 16, new Color(255, (int) (170 + 60 * p), 90));
        }
    }

    private void finalGeneral(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, c(0x2A1B5E), 0, H, c(0xE0577F))); g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 255, 255, 30));
        for (int i = 0; i < 30; i++) g.fill(new Ellipse2D.Double((i * 131.7 + t * 14) % (W + 80) - 40, H - ((i * 71.3 + t * 30) % (H + 60)), 26 + (i % 4) * 12, 26 + (i % 4) * 12));
        texto(g, "¡OPERACIÓN MUGRE COMPLETA!", W / 2, 70, 48, c(0xFFE066));
        texto(g, "La casa quedó brillando", W / 2, 108, 24, Color.WHITE);
        g.setColor(new Color(20, 10, 50, 150)); g.fill(new RoundRectangle2D.Double(170, 130, 620, 262, 30, 30));
        for (int i = 1; i <= ShooterNiveles.TOTAL; i++) {
            ShooterNiveles.Nivel nv = ShooterNiveles.get(i);
            double y = 166 + (i - 1) * 48;
            g.setFont(new Font("SansSerif", Font.BOLD, 22)); g.setColor(Color.WHITE);
            g.drawString("Cap. " + i + " - " + nv.titulo, 200, (int) y + 8);
            String rg = rangoGuardado(i); if (i == cap && rg.isEmpty()) rg = rango;
            insignia(g, 740, y, 19, rg.isEmpty() ? "D" : rg);
        }
        double e = Math.min(1, Math.max(0, (finT - 5.2) / 0.5));
        String gen = rangoGeneral();
        double rad = 44 * (0.6 + 0.4 * e);
        insignia(g, W / 2.0 + 130, 455, rad, gen);
        texto(g, "RANGO GENERAL", W / 2 - 120, 466, 26, Color.WHITE);
        if (dorado) texto(g, "★ ¡Traje dorado de limpieza desbloqueado! ★", W / 2, 532, 22, c(0xFFD54F));
        else texto(g, "Sacá rango S en los cinco capítulos para ganar el traje dorado de limpieza", W / 2, 532, 16, new Color(0xE0D8FF));
        g.setFont(new Font("SansSerif", Font.ITALIC, 18)); g.setColor(new Color(0xFFF3C4));
        String ep = "Un germencito se ríe bajo la cama: “Continuará… Operación Mugre 2”";
        g.drawString(ep, W / 2 - g.getFontMetrics().stringWidth(ep) / 2, 580);
        double p = 0.5 + 0.5 * Math.sin(t * 4);
        texto(g, "Enter: volver al menú de capítulos", W / 2, 620, 18, new Color(255, (int) (170 + 60 * p), 90));
    }

    private void moneda(Graphics2D g, double x, double y, double fase) {
        double sx = Math.max(0.15, Math.abs(Math.cos(fase)));
        AffineTransform at = g.getTransform();
        g.translate(x, y); g.scale(sx, 1);
        g.setColor(c(0xB8860B)); g.fill(new Ellipse2D.Double(-15, -15, 30, 30));
        g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(-12, -12, 24, 24));
        g.setColor(c(0xFFF59D)); g.fill(new Ellipse2D.Double(-8, -9, 7, 7));
        g.setTransform(at);
    }
}
