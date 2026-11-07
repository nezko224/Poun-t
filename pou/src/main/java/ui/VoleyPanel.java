package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Minijuego de voley de playa: tu Pou contra un Pou rival. Gana el primero en llegar a 7 puntos.
 * Teclado (flechas / A D, saltar con arriba, W o espacio) o mouse (moverlo y click para saltar).
 * No toca las reglas del juego principal: es solo diversion (y suma cariño al volver).
 */
public class VoleyPanel extends JPanel {

    private static final int W = 960, H = 640, SUELO = 520, RED_X = 480, RED_ALTO = 150;
    private static final double R_POU = 56, R_BOLA = 18, ESC = 0.64;
    private static final double G_BOLA = 1050, G_POU = 2300, SALTO = 780, VEL_J = 410, VEL_IA = 300;
    private static final int PUNTOS_META = 7;

    private static final class Jugador {
        double x, h, vx, vy, sq, sqv, miraX, miraY, caraT;
        double xMin, xMax;
        PouArte.Cara cara = PouArte.Cara.NORMAL;
        final PouArte.Pose pose = new PouArte.Pose();
        PouArte.Ropa ropa;
        String nombre;
        double centroY() { return SUELO - h - 52; }
    }

    private final Navegador nav;
    private final PanelJuego origen;
    private final Jugador yo = new Jugador(), rival = new Jugador();
    private final Particulas fx = new Particulas();
    private final javax.swing.Timer timer;
    private long ultimoNanos;
    private double t;

    private double bx, by, bvx, bvy, brot, bsq = 1;
    private boolean bolaViva;
    private int puntosYo, puntosRival;
    private int puntosTotales;
    private boolean ganoAlguna;
    private double cuenta = 1.6;      // cuenta regresiva antes del saque
    private boolean sirvoYo = true;
    private double bannerT;
    private String banner = "";
    private boolean finPartida, ganeYo;
    private double finT;
    private boolean izq, der, salto;
    private boolean usaMouse;
    private double mouseX = 240;
    private double iaObjetivo = 720, iaError;
    private int toquesRival;
    private double sacudida;

    public VoleyPanel(Navegador nav, PanelJuego origen, String nombre, PouArte.Ropa ropa) {
        this.nav = nav;
        this.origen = origen;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);

        yo.nombre = nombre;
        yo.ropa = ropa;
        yo.xMin = R_POU + 6; yo.xMax = RED_X - 8 - R_POU;
        rival.nombre = "Rival";
        rival.ropa = new PouArte.Ropa();
        rival.ropa.gorra = true; rival.ropa.anteojos = true;
        rival.pose.color = new Color(0xB57AE8);
        rival.xMin = RED_X + 8 + R_POU; rival.xMax = W - R_POU - 6;
        reiniciarPartido();

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); usaMouse = true; }
            @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); usaMouse = true; }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (finPartida) { if (e.getY() > 400 && e.getY() < 460) { if (e.getX() < W / 2) reiniciarPartido(); else salir(); } return; }
                salto = true; usaMouse = true; mouseX = e.getX();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { tecla(e.getKeyCode(), true); }
            @Override public void keyReleased(KeyEvent e) { tecla(e.getKeyCode(), false); }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        ultimoNanos = System.nanoTime();
        timer = new javax.swing.Timer(16, e -> {
            long ahora = System.nanoTime();
            double dt = Math.min(0.05, (ahora - ultimoNanos) / 1e9);
            ultimoNanos = ahora;
            actualizar(dt);
            repaint();
        });
    }

    public void iniciar() { ultimoNanos = System.nanoTime(); timer.start(); requestFocusInWindow(); }
    public void detener() { timer.stop(); }

    private void salir() {
        timer.stop();
        nav.volverDelVoley(origen, puntosTotales, ganoAlguna);
    }

    private void tecla(int k, boolean abajo) {
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) { izq = abajo; if (abajo) usaMouse = false; }
        else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) { der = abajo; if (abajo) usaMouse = false; }
        else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W || k == KeyEvent.VK_SPACE) { salto = abajo; if (abajo) usaMouse = false; }
        else if (abajo && k == KeyEvent.VK_ESCAPE) salir();
        else if (abajo && (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_R) && finPartida) reiniciarPartido();
        else if (abajo && k == KeyEvent.VK_M) Sonido.setActivo(!Sonido.isActivo());
    }

    // ===================================================================== reglas

    private void reiniciarPartido() {
        puntosYo = puntosRival = 0;
        finPartida = false;
        sirvoYo = true;
        yo.x = 240; rival.x = 720; yo.h = rival.h = 0; yo.vy = rival.vy = 0;
        toquesRival = 0;
        fx.limpiar();
        prepararSaque();
    }

    private void prepararSaque() {
        bolaViva = false;
        cuenta = 1.4;
        bvx = bvy = 0;
        bx = sirvoYo ? 240 : 720;
        by = SUELO - 330;
        toquesRival = 0;
    }

    private void punto(boolean paraYo, double x) {
        if (paraYo) puntosYo++; else puntosRival++;
        if (paraYo) puntosTotales++;
        bolaViva = false;
        sacudida = 0.25;
        fx.polvo(x, SUELO, 10);
        fx.estrellas(x, SUELO - 20, paraYo ? 14 : 4);
        Sonido.punto();
        Jugador gana = paraYo ? yo : rival, pierde = paraYo ? rival : yo;
        gana.cara = PouArte.Cara.RISA; gana.caraT = 1.6; gana.sqv += 8;
        pierde.cara = PouArte.Cara.TRISTE; pierde.caraT = 1.6;
        banner = paraYo ? "¡Punto para " + yo.nombre + "!" : "Punto para el Rival";
        bannerT = 1.4;
        sirvoYo = paraYo;
        if (puntosYo >= PUNTOS_META || puntosRival >= PUNTOS_META) {
            finPartida = true;
            ganeYo = puntosYo >= PUNTOS_META;
            finT = 0;
            if (ganeYo) { ganoAlguna = true; Sonido.ganar(); for (int i = 0; i < 6; i++) fx.estrellas(200 + i * 110, 200, 10); }
            else Sonido.perder();
            yo.cara = ganeYo ? PouArte.Cara.RISA : PouArte.Cara.TRISTE; yo.caraT = 99;
            rival.cara = ganeYo ? PouArte.Cara.TRISTE : PouArte.Cara.RISA; rival.caraT = 99;
        } else {
            prepararSaque();
            cuenta = 2.0;
        }
    }

    // ===================================================================== fisica

    private void actualizar(double dt) {
        t += dt;
        sacudida = Math.max(0, sacudida - dt);
        bannerT = Math.max(0, bannerT - dt);
        fx.actualizar(dt);
        if (finPartida) finT += dt;

        int pasos = 2;
        for (int i = 0; i < pasos; i++) paso(dt / pasos);

        animar(yo, dt);
        animar(rival, dt);
        brot += bvx * dt * 0.02;
        bsq += (1 - bsq) * Math.min(1, dt * 14);
        if (bolaViva && Math.hypot(bvx, bvy) > 620 && Math.random() < 0.6)
            fx.add(Particulas.Tipo.BRILLO, bx, by, 0, 0, 0.35, 6, 0, new Color(255, 255, 255, 200), null);
    }

    private void paso(double dt) {
        if (!finPartida) moverYo(dt);
        moverIA(dt);
        for (Jugador j : new Jugador[]{yo, rival}) {
            j.vy += G_POU * dt;
            j.h -= j.vy * dt;
            if (j.h <= 0) {
                if (j.vy > 600) { j.sqv -= Math.min(9, j.vy / 120); fx.polvo(j.x, SUELO, 3); }
                j.h = 0; j.vy = 0;
            }
            j.x = Math.max(j.xMin, Math.min(j.xMax, j.x + j.vx * dt));
        }
        if (!bolaViva) {
            if (!finPartida && cuenta > 0) {
                cuenta -= dt;
                if (cuenta <= 0) { bolaViva = true; bvx = (sirvoYo ? 1 : -1) * 40; bvy = -250; Sonido.pop(); }
            }
            return;
        }
        bvy += G_BOLA * dt;
        bx += bvx * dt;
        by += bvy * dt;
        // paredes
        if (bx < R_BOLA) { bx = R_BOLA; bvx = Math.abs(bvx) * 0.9; }
        if (bx > W - R_BOLA) { bx = W - R_BOLA; bvx = -Math.abs(bvx) * 0.9; }
        if (by < R_BOLA) { by = R_BOLA; bvy = Math.abs(bvy) * 0.6; }
        // red
        double topeRed = SUELO - RED_ALTO;
        if (by > topeRed && Math.abs(bx - RED_X) < R_BOLA + 4) {
            if (bx < RED_X) { bx = RED_X - R_BOLA - 4; bvx = -Math.abs(bvx) * 0.6; }
            else { bx = RED_X + R_BOLA + 4; bvx = Math.abs(bvx) * 0.6; }
            bsq = 0.8; Sonido.golpe();
        } else {
            double dx = bx - RED_X, dy = by - topeRed;
            double d = Math.hypot(dx, dy);
            if (d < R_BOLA + 5 && by <= topeRed) {
                double nx = dx / d, ny = dy / d;
                double dot = bvx * nx + bvy * ny;
                if (dot < 0) { bvx -= 1.7 * dot * nx; bvy -= 1.7 * dot * ny; }
                bx = RED_X + nx * (R_BOLA + 5); by = topeRed + ny * (R_BOLA + 5);
                bsq = 0.8; Sonido.golpe();
            }
        }
        golpearCon(yo, false);
        golpearCon(rival, true);
        double v = Math.hypot(bvx, bvy);
        if (v > 950) { bvx *= 950 / v; bvy *= 950 / v; }
        // suelo
        if (by >= SUELO - R_BOLA) {
            punto(bx > RED_X, bx);
        }
    }

    private void golpearCon(Jugador j, boolean esRival) {
        double cx = j.x, cy = j.centroY();
        double dx = bx - cx, dy = by - cy;
        double d = Math.hypot(dx, dy);
        double suma = R_POU + R_BOLA;
        if (d >= suma || d < 1e-6) return;
        double nx = dx / d, ny = dy / d;
        double rvx = bvx - j.vx, rvy = bvy + j.vy;
        double dot = rvx * nx + rvy * ny;
        bx = cx + nx * suma; by = cy + ny * suma;
        if (dot < 0) {
            bvx -= 1.9 * dot * nx;
            bvy -= 1.9 * dot * ny;
        }
        bvx += j.vx * 0.55;
        bvy -= j.vy * 0.35;
        double v = Math.hypot(bvx, bvy);
        if (v < 430) { double k = 430 / Math.max(v, 1); bvx *= k; bvy *= k; }
        if (bvy > -120 && ny < 0) bvy = -320;
        bsq = 0.7;
        j.sqv -= 6;
        Sonido.golpe();
        fx.estrellas(bx, by, 5);
        fx.brillos(bx, by, 8, new Color(0xFFF2A8));
        sacudida = Math.max(sacudida, 0.06);
        j.cara = PouArte.Cara.SORPRESA; j.caraT = 0.35;
        if (esRival) { toquesRival++; iaError = (Math.random() - 0.5) * 120; }
    }

    private void moverYo(double dt) {
        double dir = 0;
        if (usaMouse) {
            double dxm = mouseX - yo.x;
            dir = Math.abs(dxm) < 8 ? 0 : Math.signum(dxm) * Math.min(1, Math.abs(dxm) / 60);
        } else {
            if (izq) dir -= 1;
            if (der) dir += 1;
        }
        yo.vx = dir * VEL_J;
        if (salto && yo.h <= 0.01) {
            yo.vy = -SALTO; yo.sqv += 7; Sonido.boing();
            if (!usaMouse && !finPartida) { /* mantener apretado no repite hasta tocar suelo */ }
            if (usaMouse) salto = false;
        }
    }

    private void moverIA(double dt) {
        if (finPartida) { rival.vx = 0; return; }
        double objetivo = 720;
        if (bolaViva) {
            boolean viene = bx > RED_X - 40 || bvx > 80;
            if (viene) {
                double aterriza = predecir();
                objetivo = aterriza + 24 + iaError;
            }
        } else if (!sirvoYo) {
            objetivo = 700;
        }
        objetivo = Math.max(rival.xMin, Math.min(rival.xMax, objetivo));
        iaObjetivo += (objetivo - iaObjetivo) * Math.min(1, dt * 9);
        double dx = iaObjetivo - rival.x;
        rival.vx = Math.abs(dx) < 6 ? 0 : Math.signum(dx) * VEL_IA * Math.min(1, Math.abs(dx) / 40);
        if (bolaViva && rival.h <= 0.01) {
            double ddx = Math.abs(bx - rival.x);
            if (ddx < 85 && by > SUELO - 250 && by < SUELO - 95 && bvy > -150 && bx > RED_X) {
                rival.vy = -SALTO * 0.97; rival.sqv += 7;
            }
        }
    }

    /** Donde va a caer la pelota (balistica con rebote en paredes). */
    private double predecir() {
        double x = bx, y = by, vx = bvx, vy = bvy;
        for (int i = 0; i < 400; i++) {
            vy += G_BOLA * 0.016;
            x += vx * 0.016; y += vy * 0.016;
            if (x < R_BOLA || x > W - R_BOLA) vx = -vx;
            if (y >= SUELO - 150) return Math.max(R_BOLA, Math.min(W - R_BOLA, x));
        }
        return x;
    }

    private void animar(Jugador j, double dt) {
        j.sqv += (-200 * j.sq - 14 * j.sqv) * dt;
        j.sq += j.sqv * dt;
        j.sq = Math.max(-0.3, Math.min(0.4, j.sq));
        j.caraT = Math.max(0, j.caraT - dt);
        if (j.caraT <= 0) j.cara = PouArte.Cara.NORMAL;
        PouArte.Pose p = j.pose;
        p.sy = 1 + j.sq;
        p.sx = 1 - j.sq * 0.7;
        p.giro += (-Math.max(-0.3, Math.min(0.3, j.vx * 0.0006)) - p.giro) * Math.min(1, dt * 8);
        double tx = Math.max(-1, Math.min(1, (bx - j.x) / 180));
        double ty = Math.max(-1, Math.min(1, (by - j.centroY() + 40) / 160));
        j.miraX += (tx - j.miraX) * Math.min(1, dt * 12);
        j.miraY += (ty - j.miraY) * Math.min(1, dt * 12);
        p.miraX = j.miraX; p.miraY = j.miraY;
        p.cara = j.cara == PouArte.Cara.NORMAL
                ? (bolaViva && Math.hypot(bx - j.x, by - j.centroY()) < 160 ? PouArte.Cara.SORPRESA : PouArte.Cara.FELIZ)
                : j.cara;
        p.parpadeo = ((int) (t * 1.7 + (j == yo ? 0 : 1)) % 5 == 0 && (t * 1.7) % 1 < 0.12) ? 1 : 0;
    }

    // ===================================================================== dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (sacudida > 0) g.translate((Math.random() - 0.5) * 8 * sacudida * 4, (Math.random() - 0.5) * 8 * sacudida * 4);

        fondo(g);
        // sombras
        sombra(g, yo.x, 1 - yo.h / 300);
        sombra(g, rival.x, 1 - rival.h / 300);
        if (bolaViva || cuenta > 0) sombra(g, bx, Math.max(0.15, 1 - (SUELO - by) / 500) * 0.45);

        PouArte.dibujar(g, rival.x, SUELO - rival.h, ESC, rival.ropa, rival.pose);
        red(g);
        PouArte.dibujar(g, yo.x, SUELO - yo.h, ESC, yo.ropa, yo.pose);
        pelota(g);
        fx.dibujar(g);
        marcador(g);

        if (!bolaViva && !finPartida && cuenta > 0) {
            int n = (int) Math.ceil(cuenta);
            if (cuenta < 1.4 && n <= 3) texto(g, n == 0 ? "¡YA!" : String.valueOf(n), W / 2, 250, 90, Color.WHITE);
        }
        if (bannerT > 0 && !finPartida) texto(g, banner, W / 2, 200, 40, new Color(0xFFE066));
        g.dispose();

        Graphics2D h = (Graphics2D) g0.create();
        h.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        h.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ayuda(h);
        if (finPartida && finT > 0.8) fin(h);
        h.dispose();
    }

    private void texto(Graphics2D g, String s, int cx, int y, int tam, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, tam));
        int w = g.getFontMetrics().stringWidth(s);
        g.setColor(new Color(0, 0, 0, 110));
        g.drawString(s, cx - w / 2 + 3, y + 4);
        g.setColor(c);
        g.drawString(s, cx - w / 2, y);
    }

    private void sombra(Graphics2D g, double x, double k) {
        k = Math.max(0.1, Math.min(1, k));
        g.setColor(new Color(0, 0, 0, (int) (60 * k)));
        g.fill(new Ellipse2D.Double(x - 55 * k, SUELO - 2, 110 * k, 14 * k));
    }

    private void fondo(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x4FB6F0), 0, 330, new Color(0xCFF0FF)));
        g.fillRect(0, 0, W, 330);
        // sol
        RadialGradientPaint sol = new RadialGradientPaint(new Point2D.Double(780, 90), 120,
                new float[]{0f, 1f}, new Color[]{new Color(255, 240, 160, 200), new Color(255, 240, 160, 0)});
        g.setPaint(sol);
        g.fill(new Ellipse2D.Double(660, -30, 240, 240));
        g.setColor(new Color(0xFFD54A));
        g.fill(new Ellipse2D.Double(752, 62, 56, 56));
        // nubes
        g.setColor(new Color(255, 255, 255, 235));
        for (int k = 0; k < 4; k++) {
            double cx = ((t * (8 + k * 3) + k * 260) % (W + 200)) - 100, cy = 60 + k * 38;
            g.fill(new Ellipse2D.Double(cx, cy, 90, 30));
            g.fill(new Ellipse2D.Double(cx + 20, cy - 16, 52, 34));
            g.fill(new Ellipse2D.Double(cx + 44, cy - 8, 50, 30));
        }
        // gaviotas
        g.setColor(new Color(40, 50, 70));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 3; k++) {
            double gx = ((t * 40 + k * 330) % (W + 100)) - 50, gy = 150 + k * 22 + Math.sin(t * 2 + k) * 8;
            double al = Math.sin(t * 9 + k) * 5;
            Path2D p = new Path2D.Double();
            p.moveTo(gx - 12, gy - al); p.quadTo(gx - 5, gy - 6 - al, gx, gy);
            p.quadTo(gx + 5, gy - 6 + al, gx + 12, gy + al);
            g.draw(p);
        }
        // mar
        g.setPaint(new GradientPaint(0, 300, new Color(0x2AA6D8), 0, 400, new Color(0x6FD0E8)));
        g.fillRect(0, 300, W, 110);
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2f));
        for (int k = 0; k < 7; k++) {
            Path2D ola = new Path2D.Double();
            double y0 = 316 + k * 13;
            ola.moveTo(0, y0);
            for (int x = 0; x <= W; x += 20) ola.lineTo(x, y0 + Math.sin(t * 1.6 + x * 0.04 + k) * 3);
            g.draw(ola);
        }
        // palmera
        g.setColor(new Color(0x8A5A2B));
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tr = new Path2D.Double();
        tr.moveTo(70, 470); tr.quadTo(100, 380, 78, 290);
        g.draw(tr);
        g.setColor(new Color(0x2E9B3E));
        for (int k = 0; k < 6; k++) {
            double a = -Math.PI / 2 + (k - 2.5) * 0.55 + Math.sin(t * 1.5 + k) * 0.04;
            AffineTransform at = AffineTransform.getTranslateInstance(78, 290);
            at.rotate(a);
            Path2D hoja = new Path2D.Double();
            hoja.moveTo(0, 0); hoja.quadTo(40, -30, 90, -6); hoja.quadTo(40, -10, 0, 0);
            g.fill(at.createTransformedShape(hoja));
        }
        // arena
        g.setPaint(new GradientPaint(0, 405, new Color(0xF6E2A8), 0, H, new Color(0xE3C47A)));
        g.fillRect(0, 405, W, H - 405);
        g.setColor(new Color(0xFFFFFF + 0, true));
        g.setColor(new Color(255, 255, 255, 80));
        g.setStroke(new BasicStroke(3f));
        Path2D esp = new Path2D.Double();
        esp.moveTo(0, 405);
        for (int x = 0; x <= W; x += 20) esp.lineTo(x, 405 + Math.sin(t * 1.6 + x * 0.04) * 3 + 2);
        g.draw(esp);
        g.setColor(new Color(0xB8944A));
        for (int k = 0; k < 90; k++) g.fill(new Ellipse2D.Double((k * 97) % W, 420 + (k * 53) % 210, 3, 2));
        // cancha
        g.setColor(new Color(255, 255, 255, 190));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(30, SUELO + 6, W - 30, SUELO + 6));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(30, SUELO + 6, 60, SUELO + 40));
        g.draw(new Line2D.Double(W - 30, SUELO + 6, W - 60, SUELO + 40));
    }

    private void red(Graphics2D g) {
        double top = SUELO - RED_ALTO;
        g.setColor(new Color(0x7A5A3A));
        g.fill(new RoundRectangle2D.Double(RED_X - 6, top - 6, 12, RED_ALTO + 8, 6, 6));
        // malla
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(new Rectangle2D.Double(RED_X - 3, top, 6, RED_ALTO));
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(1.5f));
        for (int y = 0; y < RED_ALTO; y += 12) g.draw(new Line2D.Double(RED_X - 4, top + y, RED_X + 4, top + y + 6));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(RED_X - 8, top - 6, 16, 14, 8, 8));
        g.setColor(new Color(0xD94F4F));
        g.fill(new Rectangle2D.Double(RED_X - 8, top + 2, 16, 4));
    }

    private void pelota(Graphics2D g) {
        Graphics2D h = (Graphics2D) g.create();
        h.translate(bx, by);
        h.scale(1 + (1 - bsq) * 0.6, bsq);
        h.rotate(brot);
        h.setColor(Color.WHITE);
        h.fill(new Ellipse2D.Double(-R_BOLA, -R_BOLA, R_BOLA * 2, R_BOLA * 2));
        h.setColor(new Color(0xFFC247));
        h.fill(new Arc2D.Double(-R_BOLA, -R_BOLA, R_BOLA * 2, R_BOLA * 2, 20, 60, Arc2D.PIE));
        h.setColor(new Color(0x3C8DE8));
        h.fill(new Arc2D.Double(-R_BOLA, -R_BOLA, R_BOLA * 2, R_BOLA * 2, 200, 60, Arc2D.PIE));
        h.setColor(Color.WHITE);
        h.fill(new Ellipse2D.Double(-R_BOLA * 0.55, -R_BOLA * 0.55, R_BOLA * 1.1, R_BOLA * 1.1));
        h.setColor(new Color(0x7A5A3A));
        h.setStroke(new BasicStroke(1.8f));
        h.draw(new Ellipse2D.Double(-R_BOLA, -R_BOLA, R_BOLA * 2, R_BOLA * 2));
        h.draw(new Arc2D.Double(-R_BOLA, -R_BOLA * 1.9, R_BOLA * 2, R_BOLA * 2, 200, 140, Arc2D.OPEN));
        h.dispose();
        g.setColor(new Color(255, 255, 255, 140));
        g.fill(new Ellipse2D.Double(bx - 9, by - 12, 7, 5));
        // indicador cuando la pelota sale por arriba
        if (by < -4) {
            g.setColor(new Color(255, 255, 255, 220));
            g.fill(AffineTransform.getTranslateInstance(bx, 20).createTransformedShape(Particulas.estrella(10, 3, 0.5)));
        }
    }

    private void marcador(Graphics2D g) {
        double cx = W / 2.0;
        g.setColor(new Color(30, 20, 56, 215));
        g.fill(new RoundRectangle2D.Double(cx - 230, 10, 460, 58, 28, 28));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString(yo.nombre, (float) (cx - 215), 44);
        String r = rival.nombre;
        g.drawString(r, (float) (cx + 215 - g.getFontMetrics().stringWidth(r)), 44);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.setColor(new Color(0xFFE066));
        String s = puntosYo + "  -  " + puntosRival;
        g.drawString(s, (float) (cx - g.getFontMetrics().stringWidth(s) / 2.0), 52);
        // puntitos hacia la meta
        for (int i = 0; i < PUNTOS_META; i++) {
            g.setColor(i < puntosYo ? new Color(0xFF8FB1) : new Color(255, 255, 255, 50));
            g.fill(new Ellipse2D.Double(cx - 215 + i * 11, 54, 7, 7));
            g.setColor(i < puntosRival ? new Color(0xC59BFF) : new Color(255, 255, 255, 50));
            g.fill(new Ellipse2D.Double(cx + 215 - 7 - i * 11, 54, 7, 7));
        }
    }

    private void ayuda(Graphics2D g) {
        g.setColor(new Color(30, 20, 56, 190));
        g.fill(new RoundRectangle2D.Double(W / 2.0 - 330, H - 44, 660, 32, 24, 24));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        String s = "← → / A D mover   ↑ / W / Espacio saltar   Mouse: mover + click saltar   Gana el primero en llegar a " + PUNTOS_META + "   Esc: volver";
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(s, (float) (W / 2.0 - g.getFontMetrics().stringWidth(s) / 2.0), H - 23);
    }

    private void fin(Graphics2D g) {
        double a = Math.min(1, (finT - 0.8) * 2);
        g.setComposite(AlphaComposite.SrcOver.derive((float) a));
        g.setColor(new Color(10, 5, 30, 170));
        g.fillRect(0, 0, W, H);
        String s = ganeYo ? "¡GANASTE!" : "Perdiste...";
        double bounce = ganeYo ? Math.sin(t * 6) * 6 : 0;
        texto(g, s, W / 2, (int) (290 + bounce), 80, ganeYo ? new Color(0xFFE066) : new Color(0xB9C7FF));
        texto(g, puntosYo + " - " + puntosRival, W / 2, 345, 36, Color.WHITE);
        boolean[] izqBtn = {true, false};
        String[] txt = {"Jugar de nuevo (Enter)", "Volver (Esc)"};
        for (int i = 0; i < 2; i++) {
            double x = i == 0 ? W / 2.0 - 250 : W / 2.0 + 20;
            g.setColor(i == 0 ? new Color(0xFFC247) : new Color(0x7E63D8));
            g.fill(new RoundRectangle2D.Double(x, 400, 230, 56, 28, 28));
            g.setColor(i == 0 ? new Color(0x4A2C12) : Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(txt[i], (float) (x + 115 - g.getFontMetrics().stringWidth(txt[i]) / 2.0), (float) 435);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }
}
