package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Minijuego Snake (el de toda la vida). Se mueve con flechas o WASD, se come las manzanas
 * y se gana al llegar a la meta del nivel: ganar da 2 monedas. Hay 3 dificultades
 * (mas rapida y con mas manzanas que juntar). No toca model / dao / service.
 */
public class SnakePanel extends JPanel {

    public static final int MONEDAS_POR_VICTORIA = 2;

    private static final int W = 960, H = 640;
    private static final int COLS = 30, ROWS = 20, CELL = 28, BX = 60, BY = 66;

    private enum Estado { ELEGIR, JUGANDO, PAUSA, FIN }

    private enum Nivel {
        FACIL("Fácil", 0.150, 8, new Color(0x66BB6A)),
        NORMAL("Normal", 0.110, 12, new Color(0xFFB300)),
        DIFICIL("Difícil", 0.075, 16, new Color(0xEF5350));

        final String nombre;
        final double paso;
        final int meta;
        final Color color;
        Nivel(String n, double p, int m, Color c) { nombre = n; paso = p; meta = m; color = c; }
    }

    /** Mejor puntaje por dificultad (dura mientras la aplicacion este abierta). */
    private static final int[] MEJOR = new int[3];

    private final Navegador nav;
    private final PanelJuego origen;
    private final Particulas fx = new Particulas();
    private final javax.swing.Timer timer;
    private long ultimoNanos;
    private double t;

    private Estado estado = Estado.ELEGIR;
    private Nivel nivel = Nivel.NORMAL;
    private int hover = -1;
    private double mx, my;

    private LinkedList<int[]> cuerpo = new LinkedList<>();
    private List<int[]> ant = new ArrayList<>();
    private final ArrayDeque<int[]> cola = new ArrayDeque<>();
    private int[] dir = {1, 0};
    private int[] man = {20, 10};
    private double manT;                 // animacion de aparicion de la manzana
    private double acc;
    private double cuenta;               // cuenta antes de arrancar
    private int comidas;
    private boolean gano;
    private double finT;
    private double sacudida;
    private double lenguaT;

    // lo que se informa al volver a la casa
    private int victorias;
    private int monedasGanadas;

    public SnakePanel(Navegador nav, PanelJuego origen) {
        this.nav = nav;
        this.origen = origen;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); actualizarHover(); }
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); mx = e.getX(); my = e.getY(); click(); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { tecla(e.getKeyCode()); }
        });

        reiniciarTablero();
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
    public void detener() { timer.stop(); }

    private void salir() {
        timer.stop();
        nav.volverDelSnake(origen, victorias, monedasGanadas);
    }

    // ===================================================================== reglas

    private void reiniciarTablero() {
        cuerpo.clear();
        int cx = COLS / 2 - 3, cy = ROWS / 2;
        for (int i = 0; i < 3; i++) cuerpo.add(new int[]{cx - i, cy});
        ant = copiar(cuerpo);
        dir = new int[]{1, 0};
        cola.clear();
        comidas = 0;
        acc = 0;
        gano = false;
        finT = 0;
        fx.limpiar();
        ponerManzana();
    }

    private static List<int[]> copiar(List<int[]> l) {
        List<int[]> r = new ArrayList<>();
        for (int[] s : l) r.add(new int[]{s[0], s[1]});
        return r;
    }

    private void empezar(Nivel n) {
        nivel = n;
        reiniciarTablero();
        estado = Estado.JUGANDO;
        cuenta = 1.2;
        Sonido.pop();
    }

    private double paso() {
        // se va poniendo un poquito mas rapido con cada manzana
        return Math.max(nivel.paso * 0.6, nivel.paso * (1 - 0.02 * comidas));
    }

    private void ponerManzana() {
        List<int[]> libres = new ArrayList<>();
        boolean[][] ocupado = new boolean[COLS][ROWS];
        for (int[] s : cuerpo) ocupado[s[0]][s[1]] = true;
        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++)
                if (!ocupado[x][y]) libres.add(new int[]{x, y});
        if (libres.isEmpty()) return;
        man = libres.get((int) (Math.random() * libres.size()));
        manT = 0;
    }

    private void mover() {
        if (!cola.isEmpty()) dir = cola.poll();
        int[] cab = cuerpo.getFirst();
        int nx = cab[0] + dir[0], ny = cab[1] + dir[1];
        boolean come = nx == man[0] && ny == man[1];
        boolean choque = nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS;
        if (!choque) {
            int limite = come ? cuerpo.size() : cuerpo.size() - 1;   // si no come, la cola se corre
            int k = 0;
            for (int[] s : cuerpo) {
                if (k++ >= limite) break;
                if (s[0] == nx && s[1] == ny) { choque = true; break; }
            }
        }
        if (choque) { perder(); return; }

        ant = copiar(cuerpo);
        if (come) ant.add(new int[]{cuerpo.getLast()[0], cuerpo.getLast()[1]});
        cuerpo.addFirst(new int[]{nx, ny});
        if (!come) { cuerpo.removeLast(); return; }

        comidas++;
        double px = centroX(nx), py = centroY(ny);
        fx.estrellas(px, py, 6);
        fx.texto(px, py - 18, comidas + "/" + nivel.meta, new Color(0xFFFFFF));
        Sonido.comer();
        if (comidas >= nivel.meta) ganar(); else ponerManzana();
    }

    private void ganar() {
        estado = Estado.FIN;
        gano = true;
        finT = 0;
        victorias++;
        monedasGanadas += MONEDAS_POR_VICTORIA;
        origen.sumarMonedas(MONEDAS_POR_VICTORIA);       // se guarda ya, por si cierra la ventana
        MEJOR[nivel.ordinal()] = Math.max(MEJOR[nivel.ordinal()], comidas);
        for (int i = 0; i < 6; i++) fx.estrellas(150 + i * 130, 220, 10);
        Sonido.ganar();
        Sonido.moneda();
    }

    private void perder() {
        estado = Estado.FIN;
        gano = false;
        finT = 0;
        acc = 0;
        sacudida = 0.35;
        MEJOR[nivel.ordinal()] = Math.max(MEJOR[nivel.ordinal()], comidas);
        int[] c = cuerpo.getFirst();
        fx.polvo(centroX(c[0]), centroY(c[1]), 10);
        Sonido.perder();
    }

    private double centroX(double cx) { return BX + (cx + 0.5) * CELL; }
    private double centroY(double cy) { return BY + (cy + 0.5) * CELL; }

    // ===================================================================== entrada

    private void girar(int dx, int dy) {
        int[] base = cola.isEmpty() ? dir : cola.peekLast();
        if (base[0] == dx && base[1] == dy) return;
        if (base[0] == -dx && base[1] == -dy) return;     // no se puede dar la vuelta en U
        if (cola.size() < 3) cola.add(new int[]{dx, dy});
    }

    private void tecla(int k) {
        if (k == KeyEvent.VK_M) { Sonido.setActivo(!Sonido.isActivo()); return; }
        switch (estado) {
            case ELEGIR:
                if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_0) salir();
                else if (k == KeyEvent.VK_1) empezar(Nivel.FACIL);
                else if (k == KeyEvent.VK_2) empezar(Nivel.NORMAL);
                else if (k == KeyEvent.VK_3) empezar(Nivel.DIFICIL);
                else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) { nivel = Nivel.values()[Math.max(0, nivel.ordinal() - 1)]; Sonido.pop(); }
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) { nivel = Nivel.values()[Math.min(2, nivel.ordinal() + 1)]; Sonido.pop(); }
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) empezar(nivel);
                break;
            case JUGANDO:
                if (k == KeyEvent.VK_ESCAPE) salir();
                else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) girar(0, -1);
                else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) girar(0, 1);
                else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) girar(-1, 0);
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) girar(1, 0);
                else if (k == KeyEvent.VK_P || k == KeyEvent.VK_SPACE) estado = Estado.PAUSA;
                break;
            case PAUSA:
                if (k == KeyEvent.VK_ESCAPE) salir();
                else if (k == KeyEvent.VK_P || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) estado = Estado.JUGANDO;
                break;
            case FIN:
                if (k == KeyEvent.VK_ESCAPE) salir();
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_R) empezar(nivel);
                else if (k == KeyEvent.VK_D) { estado = Estado.ELEGIR; reiniciarTablero(); }
                break;
        }
    }

    private Rectangle2D rNivel(int i) { return new Rectangle2D.Double(55 + i * 290, 240, 270, 225); }
    private Rectangle2D rFin(int i) { return new Rectangle2D.Double(W / 2.0 - 395 + i * 270, 410, 250, 56); }

    private void actualizarHover() {
        hover = -1;
        if (estado == Estado.ELEGIR) {
            for (int i = 0; i < 3; i++) if (rNivel(i).contains(mx, my)) hover = i;
        } else if (estado == Estado.FIN && finT > 0.8) {
            for (int i = 0; i < 3; i++) if (rFin(i).contains(mx, my)) hover = i;
        }
        setCursor(Cursor.getPredefinedCursor(hover >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void click() {
        actualizarHover();
        if (estado == Estado.ELEGIR && hover >= 0) empezar(Nivel.values()[hover]);
        else if (estado == Estado.FIN && hover >= 0) {
            if (hover == 0) empezar(nivel);
            else if (hover == 1) { estado = Estado.ELEGIR; reiniciarTablero(); Sonido.pop(); }
            else salir();
        } else if (estado == Estado.PAUSA) estado = Estado.JUGANDO;
    }

    // ===================================================================== animacion

    private void actualizar(double dt) {
        t += dt;
        lenguaT += dt;
        manT = Math.min(1, manT + dt * 4);
        sacudida = Math.max(0, sacudida - dt);
        fx.actualizar(dt);
        if (estado == Estado.JUGANDO) {
            if (cuenta > 0) { cuenta -= dt; return; }
            acc += dt;
            int seguridad = 0;
            while (estado == Estado.JUGANDO && acc >= paso() && seguridad++ < 4) {
                acc -= paso();
                mover();
            }
        } else if (estado == Estado.FIN) {
            finT += dt;
            if (gano && Math.random() < dt * 10) fx.estrellas(80 + Math.random() * 800, 120 + Math.random() * 120, 1);
        }
    }

    // ===================================================================== dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x3B2A78), 0, H, new Color(0x7E63D8)));
        g.fillRect(0, 0, W, H);

        Graphics2D b = (Graphics2D) g.create();
        if (sacudida > 0) b.translate((Math.random() - 0.5) * 22 * sacudida, (Math.random() - 0.5) * 22 * sacudida);
        tablero(b);
        manzana(b);
        serpiente(b);
        fx.dibujar(b);
        b.dispose();

        if (estado != Estado.ELEGIR) hud(g);

        if (estado == Estado.JUGANDO && cuenta > 0) {
            texto(g, cuenta > 0.5 ? "¡Listo!" : "¡Ya!", W / 2, 330, 80, Color.WHITE);
        }
        if (estado == Estado.PAUSA) {
            velo(g, 150);
            texto(g, "PAUSA", W / 2, 320, 80, Color.WHITE);
            texto(g, "P / Espacio / click para seguir   -   Esc: volver", W / 2, 370, 20, new Color(0xE0D8FF));
        }
        if (estado == Estado.ELEGIR) elegir(g);
        if (estado == Estado.FIN) fin(g);
        g.dispose();
    }

    private void velo(Graphics2D g, int alfa) {
        g.setColor(new Color(10, 5, 30, alfa));
        g.fillRect(0, 0, W, H);
    }

    private void tablero(Graphics2D g) {
        g.setColor(new Color(0x2A1B5E));
        g.fill(new RoundRectangle2D.Double(BX - 8, BY - 8, COLS * CELL + 16, ROWS * CELL + 16, 26, 26));
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Double(BX, BY, COLS * CELL, ROWS * CELL, 18, 18));
        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++) {
                g.setColor(((x + y) & 1) == 0 ? new Color(0xA8E063) : new Color(0x9AD655));
                g.fillRect(BX + x * CELL, BY + y * CELL, CELL, CELL);
            }
        g.setClip(clip);
    }

    private void manzana(Graphics2D g) {
        if (estado == Estado.FIN && gano) return;
        double cx = centroX(man[0]), cy = centroY(man[1]);
        double e = easeOutBack(manT) * (1 + 0.05 * Math.sin(t * 6));
        AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        g.scale(e, e);
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(-10, 8, 22, 8));
        g.setColor(new Color(0xE53935));
        g.fill(new Ellipse2D.Double(-12, -11, 24, 23));
        g.setColor(new Color(0xFF8A80));
        g.fill(new Ellipse2D.Double(-7, -7, 7, 6));
        g.setColor(new Color(0x5D4037));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0, -10, 2, -16));
        g.setColor(new Color(0x2E7D32));
        g.fill(new Ellipse2D.Double(2, -18, 10, 6));
        g.setTransform(at);
    }

    private static double easeOutBack(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    private void serpiente(Graphics2D g) {
        int n = cuerpo.size();
        double a = estado == Estado.JUGANDO && cuenta <= 0 ? Math.min(1, acc / paso()) : (estado == Estado.PAUSA ? Math.min(1, acc / paso()) : 0);
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            int[] c = cuerpo.get(i);
            int[] p = i < ant.size() ? ant.get(i) : c;
            xs[i] = centroX(p[0] + (c[0] - p[0]) * a);
            ys[i] = centroY(p[1] + (c[1] - p[1]) * a);
        }
        Path2D ruta = new Path2D.Double();
        ruta.moveTo(xs[n - 1], ys[n - 1]);
        for (int i = n - 2; i >= 0; i--) ruta.lineTo(xs[i], ys[i]);
        if (n == 1) ruta.lineTo(xs[0] + 0.01, ys[0]);

        g.setColor(new Color(0, 0, 0, 45));
        g.setStroke(new BasicStroke(CELL * 0.86f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.translate(2, 4);
        g.draw(ruta);
        g.translate(-2, -4);
        g.setColor(new Color(0x5B3519));
        g.setStroke(new BasicStroke(CELL * 0.86f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(ruta);
        g.setColor(new Color(0xA8703F));
        g.setStroke(new BasicStroke(CELL * 0.70f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(ruta);
        g.setColor(new Color(0xC98F5A));
        g.setStroke(new BasicStroke(CELL * 0.26f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.translate(0, -3);
        g.draw(ruta);
        g.translate(0, 3);

        // cabeza
        double hx = xs[0], hy = ys[0];
        int dx = dir[0], dy = dir[1];
        double px = -dy, py = dx;
        if ((lenguaT % 1.6) < 0.35 && estado != Estado.FIN) {
            g.setColor(new Color(0xE53935));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(hx + dx * 12, hy + dy * 12, hx + dx * 20, hy + dy * 20));
            g.draw(new Line2D.Double(hx + dx * 20, hy + dy * 20, hx + dx * 25 + px * 4, hy + dy * 25 + py * 4));
            g.draw(new Line2D.Double(hx + dx * 20, hy + dy * 20, hx + dx * 25 - px * 4, hy + dy * 25 - py * 4));
        }
        g.setColor(new Color(0x5B3519));
        g.fill(new Ellipse2D.Double(hx - 17, hy - 17, 34, 34));
        g.setColor(new Color(0xB77B45));
        g.fill(new Ellipse2D.Double(hx - 15, hy - 15, 30, 30));
        boolean muerta = estado == Estado.FIN && !gano;
        for (int lado = -1; lado <= 1; lado += 2) {
            double ex = hx + dx * 5 + px * lado * 7, ey = hy + dy * 5 + py * lado * 7;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(ex - 5.5, ey - 5.5, 11, 11));
            g.setColor(new Color(0x2A1608));
            if (muerta) {
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(ex - 3, ey - 3, ex + 3, ey + 3));
                g.draw(new Line2D.Double(ex - 3, ey + 3, ex + 3, ey - 3));
            } else {
                g.fill(new Ellipse2D.Double(ex + dx * 1.5 - 2.8, ey + dy * 1.5 - 2.8, 5.6, 5.6));
            }
        }
        if (estado == Estado.FIN && gano) {
            g.setColor(new Color(0x2A1608));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(hx + dx * 7 - 5, hy + dy * 7 - 3, 10, 7, 200, 140, Arc2D.OPEN));
        }
    }

    private void hud(Graphics2D g) {
        g.setColor(new Color(30, 20, 56, 215));
        g.fill(new RoundRectangle2D.Double(BX - 8, 10, COLS * CELL + 16, 44, 26, 26));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("Manzanas: " + comidas + " / " + nivel.meta, BX + 14, 40);
        // barra de progreso
        double bw = 260, bx = W / 2.0 - bw / 2;
        g.setColor(new Color(255, 255, 255, 50));
        g.fill(new RoundRectangle2D.Double(bx, 24, bw, 14, 14, 14));
        g.setColor(nivel.color);
        g.fill(new RoundRectangle2D.Double(bx, 24, Math.max(14, bw * comidas / nivel.meta), 14, 14, 14));
        String r = nivel.nombre + "   Mejor: " + MEJOR[nivel.ordinal()];
        g.setColor(new Color(0xFFE066));
        g.drawString(r, BX + COLS * CELL - 14 - g.getFontMetrics().stringWidth(r), 40);
    }

    private void elegir(Graphics2D g) {
        velo(g, 170);
        texto(g, "SNAKE", W / 2, 105, 72, new Color(0x9CE36B));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(255, 255, 255, 230));
        String[] intro = {"Juntá las manzanas sin chocar las paredes ni a vos mismo.",
                "¡Si llegás a la meta ganás " + MONEDAS_POR_VICTORIA + " monedas!"};
        for (int k = 0; k < intro.length; k++)
            g.drawString(intro[k], (float) (W / 2.0 - g.getFontMetrics().stringWidth(intro[k]) / 2.0), 150 + k * 26);
        g.setColor(new Color(0xFFE066));
        String s2 = "Elegí la dificultad:";
        g.drawString(s2, (float) (W / 2.0 - g.getFontMetrics().stringWidth(s2) / 2.0), 222);
        String[] vel = {"Lenta", "Media", "Rápida"};
        for (int i = 0; i < 3; i++) {
            Nivel n = Nivel.values()[i];
            Rectangle2D r = rNivel(i);
            boolean sel = (hover >= 0 ? hover : nivel.ordinal()) == i;
            double up = sel ? 8 : 0;
            double x = r.getX(), y = r.getY() - up, w = r.getWidth(), h = r.getHeight();
            g.setColor(new Color(0, 0, 0, 80));
            g.fill(new RoundRectangle2D.Double(x + 3, y + 8 + up, w, h, 32, 32));
            RoundRectangle2D forma = new RoundRectangle2D.Double(x, y, w, h, 32, 32);
            g.setPaint(new GradientPaint(0, (float) y, n.color, 0, (float) (y + h), n.color.darker().darker()));
            g.fill(forma);
            g.setColor(new Color(255, 255, 255, sel ? 255 : 110));
            g.setStroke(new BasicStroke(sel ? 5f : 3f));
            g.draw(forma);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(Color.WHITE);
            String t1 = (i + 1) + ". " + n.nombre;
            g.drawString(t1, (float) (x + w / 2 - g.getFontMetrics().stringWidth(t1) / 2.0), (float) (y + 58));
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            String[] lin = {"Velocidad: " + vel[i], "Meta: " + n.meta + " manzanas", "Premio: +" + MONEDAS_POR_VICTORIA + " monedas",
                    "Tu mejor: " + MEJOR[i]};
            for (int k = 0; k < lin.length; k++)
                g.drawString(lin[k], (float) (x + w / 2 - g.getFontMetrics().stringWidth(lin[k]) / 2.0), (float) (y + 105 + k * 28));
            if (sel) {
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                String jg = "▶ Click o Enter";
                g.drawString(jg, (float) (x + w / 2 - g.getFontMetrics().stringWidth(jg) / 2.0), (float) (y + h - 14));
            }
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(255, 255, 255, 210));
        String ayuda = "← → elegir   1 / 2 / 3 o Enter jugar   Flechas / WASD mover   P pausa   Esc volver";
        g.drawString(ayuda, (float) (W / 2.0 - g.getFontMetrics().stringWidth(ayuda) / 2.0), 520);
    }

    private void fin(Graphics2D g) {
        double a = Math.min(1, Math.max(0, (finT - 0.5) * 2));
        g.setComposite(AlphaComposite.SrcOver.derive((float) a));
        velo(g, 175);
        double bounce = gano ? Math.sin(t * 6) * 6 : 0;
        texto(g, gano ? "¡GANASTE!" : "Perdiste...", W / 2, (int) (255 + bounce), 80, gano ? new Color(0xFFE066) : new Color(0xB9C7FF));
        texto(g, "Manzanas: " + comidas + " / " + nivel.meta + "   (" + nivel.nombre + ")", W / 2, 305, 28, Color.WHITE);
        if (gano) {
            double cx = W / 2.0 - 140;
            for (int i = 0; i < MONEDAS_POR_VICTORIA; i++) moneda(g, cx + i * 42, 350, t * 5 + i);
            texto(g, "+" + MONEDAS_POR_VICTORIA + " monedas", W / 2 + 50, 362, 34, new Color(0xFFE066));
        } else {
            texto(g, "Llegá a " + nivel.meta + " para ganar " + MONEDAS_POR_VICTORIA + " monedas", W / 2, 360, 22, new Color(0xE0D8FF));
        }
        String[] txt = {"Jugar de nuevo (Enter)", "Dificultad (D)", "Volver (Esc)"};
        Color[] col = {new Color(0xFFC247), new Color(0x4FC3F7), new Color(0x7E63D8)};
        for (int i = 0; i < 3; i++) {
            Rectangle2D r = rFin(i);
            double up = hover == i ? 4 : 0;
            g.setColor(col[i]);
            g.fill(new RoundRectangle2D.Double(r.getX(), r.getY() - up, r.getWidth(), r.getHeight(), 28, 28));
            g.setColor(i == 0 ? new Color(0x4A2C12) : (i == 1 ? new Color(0x0D3B5A) : Color.WHITE));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(txt[i], (float) (r.getCenterX() - g.getFontMetrics().stringWidth(txt[i]) / 2.0), (float) (r.getY() + 35 - up));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void moneda(Graphics2D g, double x, double y, double fase) {
        double sx = Math.max(0.15, Math.abs(Math.cos(fase)));
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.scale(sx, 1);
        g.setColor(new Color(0xB8860B));
        g.fill(new Ellipse2D.Double(-17, -17, 34, 34));
        g.setColor(new Color(0xFFD54F));
        g.fill(new Ellipse2D.Double(-14, -14, 28, 28));
        g.setColor(new Color(0xFFF59D));
        g.fill(new Ellipse2D.Double(-9, -10, 8, 8));
        g.setTransform(at);
    }

    private void texto(Graphics2D g, String s, int cx, int y, int tam, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, tam));
        int w = g.getFontMetrics().stringWidth(s);
        g.setColor(new Color(0, 0, 0, 110));
        g.drawString(s, cx - w / 2 + 3, y + 4);
        g.setColor(c);
        g.drawString(s, cx - w / 2, y);
    }
}
