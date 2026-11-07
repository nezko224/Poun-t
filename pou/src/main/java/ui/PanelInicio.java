package ui;

import model.Pou;
import service.PouService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/** Menu inicial: Nuevo Pou / Cargar Pou guardado / Salir (igual que elegirPou() de la consola). */
public class PanelInicio extends JPanel {

    private static final int W = 960, H = 640;

    private final Navegador nav;
    private final PouService service;
    private final javax.swing.Timer timer;
    private long ultimoNanos;
    private double t, saltoT, risaT, mensajeT = 7;
    private double sq, sqv, alt, vy;
    private double mx = 480, my = 300;
    private String mensaje;
    private boolean modoCargar;
    private List<Pou> vivos = new ArrayList<>();
    private final Particulas fx = new Particulas();
    private int hover = -1;
    private String aviso;
    private double avisoT;

    private static final String[] BTN = {"Nuevo Pou", "Cargar Pou guardado", "Salir"};

    public PanelInicio(Navegador nav, PouService service, String mensaje) {
        this.nav = nav;
        this.service = service;
        this.mensaje = mensaje;
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
                if (!modoCargar) {
                    if (k == KeyEvent.VK_1 || k == KeyEvent.VK_N) nuevo();
                    else if (k == KeyEvent.VK_2 || k == KeyEvent.VK_C) cargar();
                    else if (k == KeyEvent.VK_0 || k == KeyEvent.VK_ESCAPE) System.exit(0);
                } else {
                    if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_0) modoCargar = false;
                    else if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) elegir(k - KeyEvent.VK_1);
                }
            }
        });
        ultimoNanos = System.nanoTime();
        timer = new javax.swing.Timer(16, e -> {
            long ahora = System.nanoTime();
            double dt = Math.min(0.05, (ahora - ultimoNanos) / 1e9);
            ultimoNanos = ahora;
            actualizar(dt);
            repaint();
        });
        timer.start();
    }

    @Override public void addNotify() { super.addNotify(); SwingUtilities.invokeLater(this::requestFocusInWindow); }

    public void detener() { timer.stop(); }

    private Rectangle2D rBtn(int i) { return new Rectangle2D.Double(W / 2.0 - 190, 372 + i * 72, 380, 58); }
    private Rectangle2D rCard(int i) { return new Rectangle2D.Double(60 + (i % 2) * 430, 130 + (i / 2) * 105, 410, 92); }
    private Rectangle2D rVolver() { return new Rectangle2D.Double(W / 2.0 - 90, 560, 180, 46); }

    private void actualizarHover() {
        hover = -1;
        if (!modoCargar) {
            for (int i = 0; i < 3; i++) if (rBtn(i).contains(mx, my)) hover = i;
        } else {
            for (int i = 0; i < Math.min(8, vivos.size()); i++) if (rCard(i).contains(mx, my)) hover = i;
            if (rVolver().contains(mx, my)) hover = 99;
        }
        setCursor(Cursor.getPredefinedCursor(hover >= 0 || pouPulsable(mx, my) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private boolean pouPulsable(double x, double y) {
        return !modoCargar && Math.hypot(x - 480, y - 265) < 90;
    }

    private void click(int x, int y) {
        if (!modoCargar) {
            for (int i = 0; i < 3; i++) if (rBtn(i).contains(x, y)) {
                Sonido.pop();
                if (i == 0) nuevo(); else if (i == 1) cargar(); else System.exit(0);
                return;
            }
            if (pouPulsable(x, y)) {
                sqv += -8; vy = 520; risaT = 1.0;
                fx.corazones(480, 150, 3);
                Sonido.boing();
            }
        } else {
            if (rVolver().contains(x, y)) { modoCargar = false; Sonido.pop(); return; }
            for (int i = 0; i < Math.min(8, vivos.size()); i++) if (rCard(i).contains(x, y)) { elegir(i); return; }
        }
    }

    private void nuevo() {
        String nombre = JOptionPane.showInputDialog(this, "Como se llama tu Pou?", "Nuevo Pou", JOptionPane.QUESTION_MESSAGE);
        if (nombre == null) return;
        nombre = nombre.trim();
        if (nombre.equals("0") || nombre.isBlank()) nombre = "Rufus";
        Pou pou = service.nuevoPou(nombre);
        detener();
        nav.jugar(pou);
    }

    private void cargar() {
        vivos = service.listarPousVivos();
        if (vivos.isEmpty()) {
            aviso = "No hay Pous guardados con vida.";
            avisoT = 3;
            Sonido.error();
            return;
        }
        modoCargar = true;
    }

    private void elegir(int i) {
        if (i < 0 || i >= vivos.size() || i >= 8) return;
        Pou p = service.cargarPou(vivos.get(i));
        detener();
        nav.jugar(p);
    }

    private void actualizar(double dt) {
        t += dt;
        mensajeT = Math.max(0, mensajeT - dt);
        avisoT = Math.max(0, avisoT - dt);
        risaT = Math.max(0, risaT - dt);
        sqv += (-190 * sq - 14 * sqv) * dt;
        sq += sqv * dt;
        vy -= 1900 * dt;
        alt += vy * dt;
        if (alt <= 0) { if (vy < -250) { sqv -= Math.min(8, -vy / 140); } alt = 0; vy = 0; }
        fx.actualizar(dt);
        if (Math.random() < dt * 0.8) fx.add(Particulas.Tipo.BURBUJA, Math.random() * W, H + 10, 0, -60 - Math.random() * 40, 9, 8 + Math.random() * 14, 0, null, null);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(0x3B2A78), 0, H, new Color(0xF58F6B)));
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < 40; i++) {
            double br = 0.5 + 0.5 * Math.sin(t * 2 + i);
            g.setColor(new Color(255, 255, 255, (int) (40 + 150 * br * (1 - (i * 13 % 300) / 400.0))));
            g.fill(new Ellipse2D.Double((i * 151) % W, (i * 71) % 300, 2.5, 2.5));
        }
        g.setColor(new Color(0x2A1B5A));
        Path2D col = new Path2D.Double();
        col.moveTo(0, 520);
        col.curveTo(200, 440, 300, 540, 520, 480);
        col.curveTo(700, 430, 820, 520, W, 470);
        col.lineTo(W, H); col.lineTo(0, H); col.closePath();
        g.fill(col);
        fx.dibujar(g);

        // titulo con letras saltarinas
        String titulo = "Poun't";
        g.setFont(new Font("SansSerif", Font.BOLD, 84));
        FontMetrics fm = g.getFontMetrics();
        double x = W / 2.0 - fm.stringWidth(titulo) / 2.0;
        Color[] cs = {new Color(0xFFE066), new Color(0xFF8FB1), new Color(0x6BD6FF), new Color(0x9DFFB0), new Color(0xFFB347), new Color(0xC59BFF)};
        for (int i = 0; i < titulo.length(); i++) {
            String ch = String.valueOf(titulo.charAt(i));
            double y = 100 + Math.sin(t * 3 + i * 0.7) * 8;
            g.setColor(new Color(0, 0, 0, 90));
            g.drawString(ch, (float) x + 4, (float) y + 5);
            g.setColor(cs[i % cs.length]);
            g.drawString(ch, (float) x, (float) y);
            x += fm.stringWidth(ch);
        }

        if (!modoCargar) menu(g); else cargarPantalla(g);

        if (mensaje != null && mensajeT > 0) pastilla(g, mensaje, 585, new Color(40, 30, 70, 235), Math.min(1, mensajeT));
        if (aviso != null && avisoT > 0) pastilla(g, "Ojo: " + aviso, 330, new Color(150, 40, 50, 240), Math.min(1, avisoT));
        g.dispose();
    }

    private void pastilla(Graphics2D g, String txt, int y, Color bg, double a) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        int w = g.getFontMetrics().stringWidth(txt) + 34;
        if (w > W - 40) { txt = txt.substring(0, 100) + "..."; w = g.getFontMetrics().stringWidth(txt) + 34; }
        g.setComposite(AlphaComposite.SrcOver.derive((float) a));
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Double(W / 2.0 - w / 2.0, y, w, 32, 32, 32));
        g.setColor(Color.WHITE);
        g.drawString(txt, (float) (W / 2.0 - w / 2.0 + 17), y + 21);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void menu(Graphics2D g) {
        PouArte.Pose p = new PouArte.Pose();
        p.cara = risaT > 0 ? PouArte.Cara.RISA : PouArte.Cara.FELIZ;
        p.sy = 1 + sq + Math.sin(t * 2.3) * 0.016;
        p.sx = 1 - sq * 0.7;
        p.miraX = Math.max(-1, Math.min(1, (mx - 480) / 300));
        p.miraY = Math.max(-1, Math.min(1, (my - 245) / 200));
        p.giro = Math.sin(t * 1.3) * 0.03;
        double bx = 480, by = 348 - alt;
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(bx - 80, 344, 160, 16));
        PouArte.Ropa r = new PouArte.Ropa();
        PouArte.dibujar(g, bx, by, 1.0, r, p);

        for (int i = 0; i < 3; i++) {
            Rectangle2D b = rBtn(i);
            boolean h = hover == i;
            double dy = h ? -3 : 0;
            g.setColor(new Color(0, 0, 0, 80));
            g.fill(new RoundRectangle2D.Double(b.getX() + 3, b.getY() + 6, b.getWidth(), b.getHeight(), 30, 30));
            g.setPaint(new GradientPaint(0, (float) b.getY(), h ? new Color(0xFFD866) : new Color(0xFFC247), 0, (float) (b.getY() + 58), new Color(0xF2A02E)));
            if (i == 2) g.setPaint(new GradientPaint(0, (float) b.getY(), h ? new Color(0x8D75E8) : new Color(0x7E63D8), 0, (float) (b.getY() + 58), new Color(0x5B43B0)));
            g.fill(new RoundRectangle2D.Double(b.getX(), b.getY() + dy, b.getWidth(), b.getHeight(), 30, 30));
            g.setColor(new Color(255, 255, 255, 80));
            g.fill(new RoundRectangle2D.Double(b.getX() + 10, b.getY() + dy + 4, b.getWidth() - 20, 16, 16, 16));
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.setColor(i == 2 ? Color.WHITE : new Color(0x4A2C12));
            String s = BTN[i];
            g.drawString(s, (float) (b.getCenterX() - g.getFontMetrics().stringWidth(s) / 2.0), (float) (b.getY() + dy + 38));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(i == 2 ? "0" : String.valueOf(i + 1), (float) (b.getX() + 20), (float) (b.getY() + dy + 36));
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(255, 255, 255, 190));
        String s = "Tocá al Pou para jugar con él";
        g.drawString(s, (float) (W / 2.0 - g.getFontMetrics().stringWidth(s) / 2.0), 150);
    }

    private void cargarPantalla(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        String s = "Elegí tu Pou";
        g.drawString(s, (float) (W / 2.0 - g.getFontMetrics().stringWidth(s) / 2.0), 118);
        for (int i = 0; i < Math.min(8, vivos.size()); i++) {
            Pou p = vivos.get(i);
            Rectangle2D r = rCard(i);
            boolean h = hover == i;
            double dy = h ? -3 : 0;
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new RoundRectangle2D.Double(r.getX() + 3, r.getY() + 5, r.getWidth(), r.getHeight(), 24, 24));
            g.setColor(h ? new Color(0xFFFFFF) : new Color(0xFFF3DD));
            g.fill(new RoundRectangle2D.Double(r.getX(), r.getY() + dy, r.getWidth(), r.getHeight(), 24, 24));
            PouArte.Pose pp = new PouArte.Pose();
            int min = Math.min(p.getHambre(), Math.min(p.getHigiene(), p.getSueño()));
            pp.cara = p.getEstado() == Pou.EstadoPou.ENFERMO ? PouArte.Cara.ENFERMO : min <= 20 ? PouArte.Cara.TRISTE : min >= 70 ? PouArte.Cara.FELIZ : PouArte.Cara.NORMAL;
            pp.enfermo = p.getEstado() == Pou.EstadoPou.ENFERMO ? 1 : 0;
            pp.sucio = Math.max(0, (70 - p.getHigiene()) / 70.0);
            pp.sy = 1 + Math.sin(t * 2.3 + i) * 0.016;
            pp.miraX = Math.sin(t + i) * 0.5;
            PouArte.dibujar(g, r.getX() + 52, r.getY() + dy + 82, 0.38, new PouArte.Ropa(), pp);
            g.setColor(new Color(0x4A2C12));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString((i + 1) + ") " + p.getNombre(), (float) (r.getX() + 100), (float) (r.getY() + dy + 30));
            mini(g, r.getX() + 100, r.getY() + dy + 42, "Hambre", p.getHambre(), new Color(0xFFB347));
            mini(g, r.getX() + 190, r.getY() + dy + 42, "Higiene", p.getHigiene(), new Color(0x4DB5E8));
            mini(g, r.getX() + 280, r.getY() + dy + 42, "Sueño", p.getSueño(), new Color(0x8E6BEF));
            Iconos.moneda(g, r.getX() + 110, r.getY() + dy + 80, 9, false, 0);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(0xB8860B));
            g.drawString(String.valueOf((int) p.getSaldo()) + " monedas", (float) (r.getX() + 126), (float) (r.getY() + dy + 85));
        }
        Rectangle2D v = rVolver();
        g.setColor(hover == 99 ? new Color(0x8D75E8) : new Color(0x7E63D8));
        g.fill(new RoundRectangle2D.Double(v.getX(), v.getY(), v.getWidth(), v.getHeight(), 28, 28));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String b = "0) Volver";
        g.drawString(b, (float) (v.getCenterX() - g.getFontMetrics().stringWidth(b) / 2.0), (float) (v.getY() + 30));
    }

    private void mini(Graphics2D g, double x, double y, String et, int val, Color c) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(0x7A5A3A));
        g.drawString(et + " " + val, (float) x, (float) y);
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new RoundRectangle2D.Double(x, y + 4, 80, 8, 8, 8));
        g.setColor(c);
        if (val > 0) g.fill(new RoundRectangle2D.Double(x, y + 4, Math.max(8, 80 * val / 100.0), 8, 8, 8));
    }
}
