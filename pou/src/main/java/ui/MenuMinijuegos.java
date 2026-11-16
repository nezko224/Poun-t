package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/** Menu de minijuegos: elegis a que jugar (Voley, Snake) o volves a la casa del Pou. */
public class MenuMinijuegos extends JPanel {

    private static final int W = 960, H = 640;

    private final Navegador nav;
    private final PanelJuego origen;
    private final javax.swing.Timer timer;
    private long ultimoNanos;
    private double t;
    private int hover = -1;          // 0 voley, 1 snake, 2 operacion mugre, 3 volver
    private final Particulas fx = new Particulas();
    private final PouArte.Pose pose = new PouArte.Pose();
    private final PouArte.Ropa ropa;
    private final String nombre;
    private double mx, my;
    private double[] anim = {0, 0, 0, 0};   // 0..1 de "levantado" por tarjeta

    private final Runnable alVoley, alSnake, alShooter;

    public MenuMinijuegos(Navegador nav, PanelJuego origen, String nombre, PouArte.Ropa ropa,
                          Runnable alVoley, Runnable alSnake, Runnable alShooter) {
        this.nav = nav;
        this.origen = origen;
        this.nombre = nombre;
        this.ropa = ropa;
        this.alVoley = alVoley;
        this.alSnake = alSnake;
        this.alShooter = alShooter;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); actualizarHover(); }
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); click(e.getX(), e.getY()); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_1 || k == KeyEvent.VK_V) elegir(0);
                else if (k == KeyEvent.VK_2 || k == KeyEvent.VK_S) elegir(1);
                else if (k == KeyEvent.VK_3 || k == KeyEvent.VK_O) elegir(2);
                else if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_0) elegir(3);
                else if (k == KeyEvent.VK_M) Sonido.setActivo(!Sonido.isActivo());
                else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) { hover = hover <= 0 ? 2 : Math.min(2, hover) - 1; Sonido.pop(); }
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) { hover = hover < 0 || hover >= 2 ? 0 : hover + 1; Sonido.pop(); }
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) { if (hover >= 0) elegir(hover); }
            }
        });

        pose.cara = PouArte.Cara.FELIZ;
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

    private Rectangle2D rCard(int i) { return new Rectangle2D.Double(55 + i * 290, 170, 270, 290); }
    private Rectangle2D rVolver() { return new Rectangle2D.Double(W / 2.0 - 110, 548, 220, 50); }

    private void actualizarHover() {
        hover = -1;
        for (int i = 0; i < 3; i++) if (rCard(i).contains(mx, my)) hover = i;
        if (rVolver().contains(mx, my)) hover = 3;
        setCursor(Cursor.getPredefinedCursor(hover >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void click(int x, int y) {
        mx = x; my = y;
        actualizarHover();
        if (hover >= 0) elegir(hover);
    }

    private void elegir(int i) {
        Sonido.pop();
        timer.stop();
        if (i == 0) alVoley.run();
        else if (i == 1) alSnake.run();
        else if (i == 2) alShooter.run();
        else nav.volverDelMenu(origen);
    }

    private void actualizar(double dt) {
        t += dt;
        fx.actualizar(dt);
        for (int i = 0; i < 4; i++) anim[i] += ((hover == i ? 1 : 0) - anim[i]) * Math.min(1, dt * 12);
        // Pou saltarin en el medio de arriba
        double ciclo = (t % 1.6) / 1.6;
        double salto = Math.abs(Math.sin(ciclo * Math.PI));
        pose.sy = 1 + 0.06 * Math.cos(ciclo * Math.PI * 2);
        pose.sx = 1 / pose.sy;
        pose.miraX = Math.sin(t * 0.8) * 0.5;
        pose.parpadeo = (t % 3.2) > 3.05 ? 1 : 0;
        if (Math.random() < dt * 0.7) fx.estrellas(60 + Math.random() * 840, 40 + Math.random() * 100, 1);
        hoyY = 128 - salto * 26;
    }

    private double hoyY = 128;

    // ===================================================================== dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x3B2A78), 0, H, new Color(0x7E63D8)));
        g.fillRect(0, 0, W, H);
        // puntitos de fondo
        g.setColor(new Color(255, 255, 255, 22));
        for (int i = 0; i < 28; i++) {
            double x = (i * 137.5 + t * 8) % (W + 80) - 40;
            double y = (i * 83.3) % H;
            g.fill(new Ellipse2D.Double(x, y, 26 + (i % 4) * 10, 26 + (i % 4) * 10));
        }

        texto(g, "MINIJUEGOS", W / 2, 66, 48, new Color(0xFFE066));
        PouArte.dibujar(g, 160, hoyY + 480, 0.42, ropa, pose);
        PouArte.dibujar(g, 800, hoyY + 480, 0.42, ropa, pose);
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        String sub = "¿A qué quiere jugar " + nombre + "?";
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString(sub, (float) (W / 2.0 - g.getFontMetrics().stringWidth(sub) / 2.0), 108);

        tarjeta(g, 0, "1. Vóley", "Tu Pou contra un rival.\nGana el primero en llegar a 7.",
                new Color(0x4FC3F7), new Color(0x1E88E5));
        tarjeta(g, 1, "2. Snake", "El clásico: comé manzanas.\nGanás 2 monedas. 3 dificultades.",
                new Color(0x9CE36B), new Color(0x43A047));
        tarjeta(g, 2, "3. Operación Mugre", "5 capítulos con jefes.\nCada capítulo da monedas (5 a 12)",
                new Color(0xFF9CC4), new Color(0xC2478B));

        // volver
        Rectangle2D rv = rVolver();
        double up = anim[3] * 4;
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(rv.getX() + 2, rv.getY() + 5 - up, rv.getWidth(), rv.getHeight(), 28, 28));
        g.setColor(hover == 3 ? new Color(0xFFC247) : new Color(0xFFB02E));
        g.fill(new RoundRectangle2D.Double(rv.getX(), rv.getY() - up, rv.getWidth(), rv.getHeight(), 28, 28));
        g.setColor(new Color(0x4A2C12));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String v = "Volver (Esc)";
        g.drawString(v, (float) (rv.getCenterX() - g.getFontMetrics().stringWidth(v) / 2.0), (float) (rv.getY() + 32 - up));

        fx.dibujar(g);
        g.dispose();
    }

    private void tarjeta(Graphics2D g, int i, String titulo, String desc, Color c1, Color c2) {
        Rectangle2D r = rCard(i);
        double up = anim[i] * 10;
        double x = r.getX(), y = r.getY() - up, w = r.getWidth(), h = r.getHeight();
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 10 + up, w, h, 36, 36));
        RoundRectangle2D forma = new RoundRectangle2D.Double(x, y, w, h, 36, 36);
        g.setPaint(new GradientPaint(0, (float) y, c1, 0, (float) (y + h), c2));
        g.fill(forma);
        g.setColor(new Color(255, 255, 255, hover == i ? 255 : 120));
        g.setStroke(new BasicStroke(hover == i ? 5f : 3f));
        g.draw(forma);

        // ilustracion
        Graphics2D d = (Graphics2D) g.create();
        d.setClip(new RoundRectangle2D.Double(x + 4, y + 4, w - 8, h - 8, 32, 32));
        d.translate(x + w / 2, y + 100);
        if (i == 0) dibujarMiniVoley(d); else if (i == 1) dibujarMiniSnake(d); else dibujarMiniShooter(d);
        d.dispose();

        g.setFont(new Font("SansSerif", Font.BOLD, titulo.length() > 12 ? 23 : 30));
        g.setColor(Color.WHITE);
        g.drawString(titulo, (float) (x + w / 2 - g.getFontMetrics().stringWidth(titulo) / 2.0), (float) (y + 192));
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(255, 255, 255, 235));
        String[] lineas = desc.split("\n");
        for (int k = 0; k < lineas.length; k++) {
            g.drawString(lineas[k], (float) (x + w / 2 - g.getFontMetrics().stringWidth(lineas[k]) / 2.0), (float) (y + 222 + k * 20));
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        String jug = hover == i ? "▶ Click o Enter" : "Click para jugar";
        g.setColor(new Color(255, 255, 255, hover == i ? 255 : 160));
        g.drawString(jug, (float) (x + w / 2 - g.getFontMetrics().stringWidth(jug) / 2.0), (float) (y + h - 16));
    }

    private void dibujarMiniVoley(Graphics2D g) {
        // arena
        g.setColor(new Color(0xF6DFA4));
        g.fillRect(-210, 36, 420, 24);
        // red
        g.setColor(new Color(0x5B3519));
        g.fill(new RoundRectangle2D.Double(-4, -30, 8, 70, 4, 4));
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(1.5f));
        for (int k = -26; k < 44; k += 10) g.draw(new Line2D.Double(-30, k, 30, k));
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(-30, -34, 60, 5));
        // pelota rebotando
        double by = -50 - Math.abs(Math.sin(t * 3)) * 28;
        double bx = Math.sin(t * 1.5) * 90;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(bx - 17, by - 17, 34, 34));
        g.setColor(new Color(0xFFC247));
        g.fill(new Arc2D.Double(bx - 17, by - 17, 34, 34, 20, 60, Arc2D.PIE));
        g.setColor(new Color(0x3C8DE8));
        g.fill(new Arc2D.Double(bx - 17, by - 17, 34, 34, 200, 60, Arc2D.PIE));
        g.setColor(new Color(0x7A5A3A));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(bx - 17, by - 17, 34, 34));
    }

    private void dibujarMiniSnake(Graphics2D g) {
        g.setColor(new Color(0xA8E063));
        g.fillRect(-210, -60, 420, 120);
        g.setColor(new Color(0x9AD655));
        for (int cx = 0; cx < 14; cx++) for (int cy = 0; cy < 4; cy++)
            if ((cx + cy) % 2 == 0) g.fillRect(-210 + cx * 30, -60 + cy * 30, 30, 30);
        // serpiente que ondula
        int n = 11;
        for (int k = n - 1; k >= 0; k--) {
            double fase = t * 3 - k * 0.55;
            double x = -120 + (n - 1 - k) * 22;
            double y = 20 + Math.sin(fase) * 22;
            double rad = k == 0 ? 15 : 12.5 - k * 0.2;
            Color c = new Color(0xA8703F);
            if (k % 2 == 1) c = new Color(0xB98250);
            g.setColor(c);
            g.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
            if (k == 0) {
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x + 1, y - 10, 9, 9));
                g.fill(new Ellipse2D.Double(x + 1, y + 1, 9, 9));
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Double(x + 5, y - 7, 4, 4));
                g.fill(new Ellipse2D.Double(x + 5, y + 4, 4, 4));
            }
        }
        // manzana
        double ax = 105, ay = 14 + Math.sin(t * 4) * 4;
        g.setColor(new Color(0xE53935));
        g.fill(new Ellipse2D.Double(ax - 14, ay - 12, 28, 26));
        g.setColor(new Color(0x5D4037));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(ax, ay - 12, ax + 2, ay - 20));
        g.setColor(new Color(0x43A047));
        g.fill(new Ellipse2D.Double(ax + 2, ay - 22, 12, 7));
    }

    private void dibujarMiniShooter(Graphics2D g) {
        // pasillo en perspectiva
        g.setColor(new Color(0xEDE7FF)); g.fillRect(-135, -60, 270, 60);
        g.setColor(new Color(0xF8C8DC)); g.fillRect(-135, 0, 270, 70);
        g.setColor(new Color(0xFFD9E8));
        Polygon izq = new Polygon(new int[]{-135, -52, -52, -135}, new int[]{-60, -28, 28, 70}, 4);
        Polygon der = new Polygon(new int[]{135, 52, 52, 135}, new int[]{-60, -28, 28, 70}, 4);
        g.fillPolygon(izq); g.fillPolygon(der);
        g.setColor(new Color(0xE79BB8)); g.fillRect(-52, -28, 104, 56);
        g.setColor(new Color(255, 255, 255, 120));
        for (int i = -2; i <= 2; i++) g.drawLine(i * 12, 28, i * 40, 70);
        // puerta del fondo
        g.setColor(new Color(0x9C6B3C)); g.fillRect(-14, -8, 28, 36);
        g.setColor(new Color(0xFFD54F)); g.fillOval(6, 10, 4, 4);
        // germen
        double bz = Math.sin(t * 4) * 2;
        g.setColor(new Color(0x7CB342)); g.fill(new Ellipse2D.Double(-46, 4 + bz, 30, 26));
        g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(-40, 10 + bz, 8, 9)); g.fill(new Ellipse2D.Double(-30, 10 + bz, 8, 9));
        g.setColor(Color.BLACK); g.fill(new Ellipse2D.Double(-37, 13 + bz, 4, 5)); g.fill(new Ellipse2D.Double(-28, 13 + bz, 4, 5));
        // burbujas
        double ph = (t * 1.3) % 1.0;
        for (int i = 0; i < 3; i++) {
            double q = (ph + i / 3.0) % 1.0, r = 3 + q * 9;
            g.setColor(new Color(190, 235, 255, (int) (200 * (1 - q * 0.5)))); g.fill(new Ellipse2D.Double(40 - q * 70 - r, 70 - q * 70 - r, r * 2, r * 2));
            g.setColor(Color.WHITE); g.draw(new Ellipse2D.Double(40 - q * 70 - r, 70 - q * 70 - r, r * 2, r * 2));
        }
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
