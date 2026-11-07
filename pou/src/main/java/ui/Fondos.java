package ui;

import java.awt.*;
import java.awt.geom.*;

/** Fondos de cada habitacion, dibujados con Java2D. Pantalla logica de 960 x 640. */
public final class Fondos {
    public static final int W = 960, H = 640;
    public static final int PISO = 440;      // linea donde empieza el piso
    public static final double CAMA_X = 745; // donde se acuesta el Pou

    private Fondos() { }

    static Color c(int rgb) { return new Color(rgb); }

    static void rr(Graphics2D g, double x, double y, double w, double h, double arc, Color c) {
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    static void rrb(Graphics2D g, double x, double y, double w, double h, double arc, Color c, Color borde, float grosor) {
        RoundRectangle2D s = new RoundRectangle2D.Double(x, y, w, h, arc, arc);
        g.setColor(c);
        g.fill(s);
        g.setColor(borde);
        g.setStroke(new BasicStroke(grosor));
        g.draw(s);
    }

    static void pisoMadera(Graphics2D g, int y0, Color a, Color b) {
        g.setPaint(new GradientPaint(0, y0, a, 0, 640, b));
        g.fillRect(0, y0, W, 640 - y0);
        g.setColor(new Color(0, 0, 0, 28));
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 6; i++) g.drawLine(0, y0 + 18 + i * 24, W, y0 + 18 + i * 24);
        for (int r = 0; r < 6; r++) {
            for (int x = (r % 2) * 70; x < W; x += 140) g.drawLine(x, y0 + r * 24, x, y0 + r * 24 + 24);
        }
        g.setColor(new Color(0, 0, 0, 45));
        g.fillRect(0, y0, W, 6);
    }

    static void zocalo(Graphics2D g, int y, Color col) {
        g.setColor(col);
        g.fillRect(0, y - 12, W, 12);
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(0, y - 2, W, 2);
    }

    // ------------------------------------------------------------------ SALA

    public static void sala(Graphics2D g, double t, double noche) {
        Color d1 = c(0xFFE9B5), d2 = c(0xF6CB86), n1 = c(0x1E2448), n2 = c(0x353C6E);
        g.setPaint(new GradientPaint(0, 0, PouArte.mezclar(d1, n1, noche), 0, PISO, PouArte.mezclar(d2, n2, noche)));
        g.fillRect(0, 0, W, PISO);
        // papel tapiz: rombos suaves
        g.setColor(new Color(255, 255, 255, 26));
        for (int x = 20; x < W; x += 70) for (int y = 100; y < PISO - 20; y += 70) {
            Path2D p = new Path2D.Double();
            p.moveTo(x, y - 12); p.lineTo(x + 12, y); p.lineTo(x, y + 12); p.lineTo(x - 12, y); p.closePath();
            g.fill(p);
        }
        zocalo(g, PISO, PouArte.mezclar(c(0xFFFFFF), c(0x8C93C9), noche));
        pisoMadera(g, PISO, PouArte.mezclar(c(0xC98A4B), c(0x6A5070), noche * 0.7), PouArte.mezclar(c(0xA86D35), c(0x4A3860), noche * 0.7));

        // alfombra
        g.setColor(PouArte.mezclar(c(0xE46A6A), c(0x7A4A7A), noche * 0.6));
        g.fill(new Ellipse2D.Double(190, 492, 400, 62));
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(206, 498, 368, 50));

        ventana(g, t, noche);

        // cuadro
        rrb(g, 470, 110, 124, 88, 8, c(0x7BC4E8), c(0x8A5A2B), 6f);
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Double(473, 113, 118, 82, 6, 6));
        g.setColor(c(0x6BC46B));
        g.fill(new Ellipse2D.Double(450, 165, 130, 80));
        g.setColor(c(0xFFD54A));
        g.fill(new Ellipse2D.Double(550, 120, 22, 22));
        g.setClip(clip);

        // lampara colgante
        double luz = 1 - noche;
        g.setColor(c(0x5A4630));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(380, 0, 380, 140);
        Path2D pant = new Path2D.Double();
        pant.moveTo(352, 190); pant.lineTo(366, 140); pant.lineTo(394, 140); pant.lineTo(408, 190); pant.closePath();
        g.setColor(PouArte.mezclar(c(0xFFD54A), c(0x8A7A4A), noche));
        g.fill(pant);
        g.setColor(c(0x8A6A1A));
        g.draw(pant);
        if (luz > 0.05) {
            RadialGradientPaint rg = new RadialGradientPaint(new Point2D.Double(380, 195), 150,
                    new float[]{0f, 1f}, new Color[]{new Color(255, 240, 170, (int) (95 * luz)), new Color(255, 240, 170, 0)});
            g.setPaint(rg);
            g.fill(new Ellipse2D.Double(230, 140, 300, 300));
        }
    }

    private static void ventana(Graphics2D g, double t, double noche) {
        int x = 90, y = 96, w = 190, h = 190;
        Color cielo1 = PouArte.mezclar(c(0x8FD3FF), c(0x0C1233), noche), cielo2 = PouArte.mezclar(c(0xD7F0FF), c(0x28306B), noche);
        Shape clip = g.getClip();
        g.clip(new Rectangle2D.Double(x, y, w, h));
        g.setPaint(new GradientPaint(0, y, cielo1, 0, y + h, cielo2));
        g.fillRect(x, y, w, h);
        if (noche < 0.6) {
            double a = 1 - noche;
            g.setColor(new Color(255, 214, 74, (int) (255 * a)));
            g.fill(new Ellipse2D.Double(x + 100, y + 30, 46, 46));
            g.setStroke(new BasicStroke(3f));
            for (int i = 0; i < 10; i++) {
                double an = t * 0.4 + i * Math.PI / 5;
                g.draw(new Line2D.Double(x + 123 + Math.cos(an) * 30, y + 53 + Math.sin(an) * 30,
                        x + 123 + Math.cos(an) * 40, y + 53 + Math.sin(an) * 40));
            }
            g.setColor(new Color(255, 255, 255, (int) (230 * a)));
            for (int k = 0; k < 2; k++) {
                double cx = x + ((t * 12 + k * 120) % (w + 90)) - 45, cy = y + 40 + k * 55;
                g.fill(new Ellipse2D.Double(cx, cy, 54, 22));
                g.fill(new Ellipse2D.Double(cx + 14, cy - 12, 32, 26));
            }
        }
        if (noche > 0.3) {
            for (int i = 0; i < 16; i++) {
                double sx = x + 8 + (i * 37) % (w - 16), sy = y + 8 + (i * 53) % (h - 60);
                double br = 0.5 + 0.5 * Math.sin(t * 2 + i);
                g.setColor(new Color(255, 255, 255, (int) (200 * noche * br)));
                g.fill(Particulas.estrella(2.6 + br * 1.4, 4, 0.35).getBounds2D());
                AffineTransform at = AffineTransform.getTranslateInstance(sx, sy);
                g.fill(at.createTransformedShape(Particulas.estrella(3 + br * 2, 4, 0.35)));
            }
            g.setColor(new Color(250, 246, 220, (int) (255 * noche)));
            Area luna = new Area(new Ellipse2D.Double(x + 110, y + 28, 44, 44));
            luna.subtract(new Area(new Ellipse2D.Double(x + 124, y + 22, 40, 40)));
            g.fill(luna);
        }
        g.setClip(clip);
        g.setColor(c(0x8A5A2B));
        g.setStroke(new BasicStroke(8f));
        g.draw(new Rectangle2D.Double(x, y, w, h));
        g.setStroke(new BasicStroke(5f));
        g.drawLine(x + w / 2, y, x + w / 2, y + h);
        g.drawLine(x, y + h / 2, x + w, y + h / 2);
        rr(g, x - 12, y + h, w + 24, 12, 6, c(0x9A6A38));
        // cortinas
        Color cor = PouArte.mezclar(c(0xF28BA6), c(0x6A3A66), noche * 0.7);
        Path2D izq = new Path2D.Double();
        izq.moveTo(x - 14, y - 10); izq.lineTo(x + 40, y - 10); izq.quadTo(x + 18, y + 90, x + 38, y + h + 8); izq.lineTo(x - 14, y + h + 8); izq.closePath();
        g.setColor(cor);
        g.fill(izq);
        AffineTransform esp = AffineTransform.getTranslateInstance(2 * x + w, 0);
        esp.scale(-1, 1);
        g.fill(esp.createTransformedShape(izq));
        g.setColor(new Color(0, 0, 0, 30));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(x + 6, y, x + 14, y + h));
        g.draw(esp.createTransformedShape(new Line2D.Double(x + 6, y, x + 14, y + h)));
        rr(g, x - 18, y - 18, w + 36, 10, 6, c(0x7A4A20));
    }

    /** Cama detras del Pou. */
    public static void cama(Graphics2D g, double noche) {
        rr(g, 876, 340, 30, 168, 12, c(0x8A5A2B));
        rr(g, 880, 346, 22, 8, 4, c(0xA8743A));
        rr(g, 604, 468, 296, 38, 8, c(0x9A6A38));
        rr(g, 612, 500, 18, 22, 4, c(0x7A4A20));
        rr(g, 874, 500, 18, 22, 4, c(0x7A4A20));
        rr(g, 612, 438, 266, 38, 16, PouArte.mezclar(c(0x6FA8E8), c(0x2F4A8A), noche * 0.5));
        rr(g, 800, 420, 72, 30, 18, PouArte.mezclar(Color.WHITE, c(0xB0B8E0), noche * 0.6));
        g.setColor(new Color(0, 0, 0, 25));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(800, 420, 72, 30, 18, 18));
    }

    /** Manta que tapa al Pou dormido (delante). */
    public static void manta(Graphics2D g, double cobertura) {
        if (cobertura < 0.02) return;
        double top = 476 - 74 * cobertura;
        Path2D p = new Path2D.Double();
        p.moveTo(634, 480);
        p.lineTo(634, top + 10);
        for (int i = 0; i <= 10; i++) {
            double x = 634 + i * 24.4;
            p.quadTo(x - 6, top - 6 + (i % 2) * 5, x + 12, top + 5);
        }
        p.lineTo(878, 480);
        p.closePath();
        g.setColor(c(0xF2A33A));
        g.fill(p);
        Shape clip = g.getClip();
        g.clip(p);
        g.setColor(new Color(255, 255, 255, 80));
        for (int x = 640; x < 880; x += 34) g.fill(new Rectangle2D.Double(x, top - 10, 14, 120));
        g.setClip(clip);
        g.setColor(c(0xB8741A));
        g.setStroke(new BasicStroke(3f));
        g.draw(p);
    }

    // ------------------------------------------------------------------ COCINA

    public static void cocina(Graphics2D g, double t) {
        g.setPaint(new GradientPaint(0, 0, c(0xDDF3EA), 0, PISO, c(0xBFE3D4)));
        g.fillRect(0, 0, W, PISO);
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x < W; x += 48) g.drawLine(x, 0, x, PISO);
        for (int y = 0; y < PISO; y += 48) g.drawLine(0, y, W, y);
        // piso a cuadros
        for (int y = PISO; y < 640; y += 40) for (int x = 0; x < W; x += 40) {
            boolean par = ((x / 40) + ((y - PISO) / 40)) % 2 == 0;
            g.setColor(par ? c(0xF4EAD5) : c(0xD8A86B));
            g.fillRect(x, y, 40, 40);
        }
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(0, PISO, W, 6);
        // alacenas
        for (int i = 0; i < 3; i++) {
            double x = 210 + i * 134;
            rrb(g, x, 92, 124, 96, 8, c(0xC98A4B), c(0x8A5A2B), 3f);
            rr(g, x + 56, 160, 12, 12, 4, c(0xF2E2B3));
        }
        // mesada
        rr(g, 190, 394, 440, 46, 6, c(0xE9D8B8));
        rr(g, 190, 388, 440, 12, 6, c(0x8F9AA6));
        // olla con vapor
        rr(g, 260, 352, 78, 40, 10, c(0xB8BEC8));
        rr(g, 250, 346, 98, 10, 4, c(0x8F96A2));
        rr(g, 342, 360, 22, 6, 3, c(0x6A707A));
        for (int i = 0; i < 3; i++) {
            double ph = (t * 0.9 + i * 0.33) % 1.0;
            g.setColor(new Color(255, 255, 255, (int) (150 * (1 - ph))));
            g.fill(new Ellipse2D.Double(284 + Math.sin(t * 3 + i) * 8 + i * 6, 336 - ph * 70, 18 + ph * 14, 18 + ph * 14));
        }
        // frutero
        g.setColor(c(0xA0703C));
        g.fill(new Arc2D.Double(470, 360, 90, 60, 180, 180, Arc2D.CHORD));
        g.setColor(c(0xE63946));
        g.fill(new Ellipse2D.Double(486, 350, 28, 28));
        g.setColor(c(0xFFD93D));
        g.fill(new Ellipse2D.Double(514, 346, 28, 30));
        g.setColor(c(0x7CC46B));
        g.fill(new Ellipse2D.Double(500, 340, 26, 26));
        // heladera
        rrb(g, 30, 110, 150, 370, 14, c(0xF1F5F8), c(0x9AA6B2), 4f);
        g.setColor(c(0x9AA6B2));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(32, 270, 178, 270);
        rr(g, 150, 150, 8, 60, 4, c(0x7C8896));
        rr(g, 150, 290, 8, 90, 4, c(0x7C8896));
        // imanes
        g.setColor(c(0xE63946)); g.fill(new Ellipse2D.Double(60, 140, 22, 22));
        g.setColor(c(0x4DB5FF)); g.fill(new Ellipse2D.Double(92, 152, 18, 18));
        // estante
        estante(g, 630, 120, 310, new double[]{262, 392});
        rrb(g, 700, 82, 170, 32, 10, c(0xD94F4F), c(0x8A2A2A), 3f);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("COCINA", 738, 105);
    }

    static void estante(Graphics2D g, int x, int y0, int w, double[] tablas) {
        double fondoH = tablas[tablas.length - 1] + 18 - y0;
        rr(g, x, y0, w, fondoH, 10, c(0xA8743A));
        rr(g, x + 8, y0 + 8, w - 16, fondoH - 16, 8, c(0x8A5A2B));
        for (double yb : tablas) {
            rr(g, x - 6, yb, w + 12, 18, 6, c(0xC98A4B));
            g.setColor(new Color(0, 0, 0, 40));
            g.fill(new Rectangle2D.Double(x - 6, yb + 14, w + 12, 4));
        }
    }

    // ------------------------------------------------------------------ BANO

    public static void banoFondo(Graphics2D g, double t, double ducha) {
        g.setPaint(new GradientPaint(0, 0, c(0xCDEBFA), 0, PISO, c(0xA8D8F0)));
        g.fillRect(0, 0, W, PISO);
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x < W; x += 44) g.drawLine(x, 0, x, PISO);
        for (int y = 0; y < PISO; y += 44) g.drawLine(0, y, W, y);
        for (int y = PISO; y < 640; y += 40) for (int x = 0; x < W; x += 80) {
            boolean par = ((x / 80) + ((y - PISO) / 40)) % 2 == 0;
            g.setColor(par ? c(0xF5FAFD) : c(0xBFE0F0));
            g.fillRect(x, y, 80, 40);
        }
        g.setColor(new Color(0, 0, 0, 35));
        g.fillRect(0, PISO, W, 6);
        // espejo
        rrb(g, 90, 120, 130, 170, 20, c(0xEAF6FF), c(0x9AB6C8), 6f);
        double gl = ((t * 0.5) % 1.6) * 200 - 40;
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Double(94, 124, 122, 162, 16, 16));
        g.setColor(new Color(255, 255, 255, 150));
        g.fill(new Parallelogram(94 + gl - 120, 124, 26, 162));
        g.setClip(clip);
        // toallero
        g.setColor(c(0x8F9AA6));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(110, 330, 210, 330);
        rr(g, 120, 330, 80, 70, 8, c(0xFFB74D));
        g.setColor(new Color(255, 255, 255, 120));
        g.fillRect(120, 360, 80, 8);
        // caño y ducha
        rr(g, 442, 0, 16, 96, 4, c(0xB8C4CF));
        g.setPaint(new GradientPaint(0, 90, c(0xE3EAF0), 0, 120, c(0xA7B4C0)));
        g.fill(new Ellipse2D.Double(396, 92, 108, 34));
        g.setColor(c(0x7C8896));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(396, 92, 108, 34));
        g.setColor(new Color(60, 70, 80, 120));
        for (int i = 0; i < 6; i++) g.fill(new Ellipse2D.Double(412 + i * 14, 112, 5, 5));
        // canilla
        double gira = ducha > 0 ? t * 6 : 0;
        g.setColor(c(0xB8C4CF));
        g.fill(new Ellipse2D.Double(684, 214, 72, 72));
        g.setColor(c(0x7C8896));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(684, 214, 72, 72));
        AffineTransform at = AffineTransform.getRotateInstance(gira, 720, 250);
        g.setColor(c(0xE63946));
        g.fill(at.createTransformedShape(new RoundRectangle2D.Double(692, 244, 56, 12, 8, 8)));
        g.fill(at.createTransformedShape(new RoundRectangle2D.Double(714, 222, 12, 56, 8, 8)));
        g.setColor(c(0x5A6A78));
        g.fill(new Ellipse2D.Double(712, 242, 16, 16));
        g.setColor(c(0x2D4A5A));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("DUCHA", 693, 306);
        // estante
        rr(g, 690, 372, 230, 14, 6, c(0xC98A4B));
        rr(g, 700, 386, 10, 20, 3, c(0x8A5A2B));
        rr(g, 900, 386, 10, 20, 3, c(0x8A5A2B));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(c(0x2D4A5A));
        g.drawString("Esponja: arrastrala sobre el Pou", 690, 420);
        // banera (parte de atras)
        rrb(g, 292, 424, 316, 100, 26, c(0xD8EEF8), c(0x8FB6CC), 4f);
    }

    /** Parte delantera de la banera + agua (se dibuja sobre el Pou). */
    public static void banoFrente(Graphics2D g, double t, double nivelAgua, double bounce) {
        double yAgua = 506 - 36 * nivelAgua;
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Double(298, 428, 304, 94, 26, 26));
        Path2D ola = new Path2D.Double();
        ola.moveTo(290, 520);
        ola.lineTo(290, yAgua);
        for (int x = 290; x <= 610; x += 20) ola.lineTo(x, yAgua + Math.sin(t * 3 + x * 0.08) * 3);
        ola.lineTo(610, 520);
        ola.closePath();
        g.setColor(new Color(110, 190, 245, 130));
        g.fill(ola);
        g.setClip(clip);
        // frente
        Path2D f = new Path2D.Double();
        f.moveTo(280, 468);
        f.lineTo(620, 468);
        f.curveTo(630, 528, 600, 558, 570, 560);
        f.lineTo(330, 560);
        f.curveTo(300, 558, 270, 528, 280, 468);
        f.closePath();
        g.setPaint(new GradientPaint(0, 468, c(0xFFFFFF), 0, 560, c(0xCFE6F2)));
        g.fill(f);
        g.setColor(c(0x8FB6CC));
        g.setStroke(new BasicStroke(4f));
        g.draw(f);
        rr(g, 270, 458, 360, 16, 10, c(0xFFFFFF));
        g.setColor(c(0x8FB6CC));
        g.draw(new RoundRectangle2D.Double(270, 458, 360, 16, 10, 10));
        // patito
        double py = 452 + Math.sin(t * 2) * 2;
        g.setColor(c(0xFFD93D));
        g.fill(new Ellipse2D.Double(560, py, 34, 24));
        g.fill(new Ellipse2D.Double(580, py - 14, 20, 20));
        g.setColor(c(0xFF8A1F));
        g.fill(new Ellipse2D.Double(594, py - 6, 12, 7));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(590, py - 9, 4, 4));
    }

    // ------------------------------------------------------------------ ROPERO

    public static void ropero(Graphics2D g, double t) {
        g.setPaint(new GradientPaint(0, 0, c(0xF1DDC4), 0, PISO, c(0xE0C3A0)));
        g.fillRect(0, 0, W, PISO);
        g.setColor(new Color(120, 80, 40, 35));
        g.setStroke(new BasicStroke(3f));
        for (int x = 0; x < W; x += 60) g.drawLine(x, 0, x, PISO);
        zocalo(g, PISO, c(0x8A5A2B));
        pisoMadera(g, PISO, c(0xB87C45), c(0x8F5A2B));
        g.setColor(c(0x6A8FD8));
        g.fill(new Ellipse2D.Double(140, 500, 330, 56));
        // espejo de pie
        rrb(g, 60, 110, 150, 350, 70, c(0xEAF6FF), c(0x8A5A2B), 10f);
        double gl = ((t * 0.45) % 1.8) * 260 - 80;
        Shape clip = g.getClip();
        g.clip(new RoundRectangle2D.Double(70, 120, 130, 330, 60, 60));
        g.setColor(new Color(255, 255, 255, 130));
        g.fill(new Parallelogram(70 + gl - 150, 120, 30, 330));
        g.setClip(clip);
        rr(g, 120, 458, 30, 40, 4, c(0x8A5A2B));
        // placard
        rrb(g, 575, 84, 360, 446, 16, c(0xA8743A), c(0x6A4420), 6f);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Rectangle2D.Double(581, 90, 348, 434));
    }

    // ------------------------------------------------------------------ FARMACIA

    public static void farmacia(Graphics2D g, double t) {
        g.setPaint(new GradientPaint(0, 0, c(0xF6FFFB), 0, PISO, c(0xD6F2E8)));
        g.fillRect(0, 0, W, PISO);
        g.setColor(new Color(120, 200, 170, 60));
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x < W; x += 56) g.drawLine(x, 0, x, PISO);
        for (int y = 0; y < PISO; y += 56) g.drawLine(0, y, W, y);
        for (int y = PISO; y < 640; y += 40) for (int x = 0; x < W; x += 80) {
            boolean par = ((x / 80) + ((y - PISO) / 40)) % 2 == 0;
            g.setColor(par ? c(0xFFFFFF) : c(0xCDEBDD));
            g.fillRect(x, y, 80, 40);
        }
        g.setColor(new Color(0, 0, 0, 30));
        g.fillRect(0, PISO, W, 6);
        // cruz luminosa
        double pulso = 0.6 + 0.4 * Math.sin(t * 2.5);
        g.setColor(new Color(60, 200, 120, (int) (60 * pulso)));
        g.fill(new Ellipse2D.Double(190, 80, 220, 220));
        rr(g, 270, 100, 60, 180, 14, c(0x2FBF71));
        rr(g, 210, 160, 180, 60, 14, c(0x2FBF71));
        // reloj
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(450, 96, 70, 70));
        g.setColor(c(0x4A5A66));
        g.setStroke(new BasicStroke(4f));
        g.draw(new Ellipse2D.Double(450, 96, 70, 70));
        double sec = (System.currentTimeMillis() / 1000.0) % 60;
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(485, 131, 485 + Math.cos(sec / 60 * 6.283 - 1.5708) * 26, 131 + Math.sin(sec / 60 * 6.283 - 1.5708) * 26));
        g.draw(new Line2D.Double(485, 131, 485 + 14, 131));
        // mostrador
        rrb(g, 200, 392, 410, 48, 8, c(0xE9E4D6), c(0x9A8F78), 3f);
        rr(g, 200, 384, 410, 12, 6, c(0x7C8896));
        // estante con frascos
        estante(g, 640, 120, 300, new double[]{300});
        rrb(g, 700, 82, 190, 32, 10, c(0x2FBF71), c(0x1B7A47), 3f);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("FARMACIA", 726, 105);
        for (int i = 0; i < 5; i++) {
            Color[] cs = {c(0x9AD0F5), c(0xF5B5C8), c(0xF8E08A), c(0xB7E3A8), c(0xD6B8F0)};
            rr(g, 660 + i * 54, 140, 28, 40, 6, cs[i]);
            rr(g, 666 + i * 54, 130, 16, 14, 4, Color.WHITE);
        }
    }

    /** Paralelogramo simple para los brillos. */
    private static final class Parallelogram extends Path2D.Double {
        Parallelogram(double x, double y, double w, double h) {
            moveTo(x + 40, y); lineTo(x + 40 + w, y); lineTo(x + w, y + h); lineTo(x, y + h); closePath();
        }
    }
}
