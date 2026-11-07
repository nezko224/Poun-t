package ui;

import java.awt.*;
import java.awt.geom.*;
import java.text.Normalizer;

/** Iconos dibujados con formas (comida, remedios, ropa, monedas y botones). */
public final class Iconos {
    private Iconos() { }

    private static final Color CONT = new Color(0x4A2C12);

    private static String limpio(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
    }

    private static void relleno(Graphics2D g, Shape s, Color c, float borde) {
        g.setColor(c);
        g.fill(s);
        g.setColor(CONT);
        g.setStroke(new BasicStroke(borde, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    /** tipo: 0 fruta, 1 vegetal, 2 chatarra (solo si el nombre no se reconoce). */
    public static void alimento(Graphics2D g0, String nombre, int tipo, double cx, double cy, double s) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.scale(s / 60.0, s / 60.0);
        String n = limpio(nombre);
        float b = 2.6f;
        if (n.contains("manzana")) {
            relleno(g, new Ellipse2D.Double(-26, -24, 52, 50), new Color(0xE63946), b);
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(new Ellipse2D.Double(-16, -16, 12, 18));
            g.setColor(new Color(0x5B3A1A));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(0, -22, 4, -34));
            relleno(g, new Ellipse2D.Double(4, -38, 20, 11), new Color(0x4CAF50), b);
        } else if (n.contains("banana")) {
            Path2D p = new Path2D.Double();
            p.moveTo(-26, -20);
            p.curveTo(-30, 14, 0, 34, 30, 12);
            p.curveTo(34, 8, 30, 2, 24, 6);
            p.curveTo(4, 16, -14, 4, -14, -24);
            p.closePath();
            relleno(g, p, new Color(0xFFD93D), b);
            relleno(g, new Rectangle2D.Double(-24, -30, 12, 10), new Color(0x7A5A2A), b);
        } else if (n.contains("zanahoria")) {
            Path2D p = new Path2D.Double();
            p.moveTo(-14, -18);
            p.curveTo(-6, -26, 10, -22, 16, -14);
            p.lineTo(-18, 32);
            p.closePath();
            AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(25));
            Shape rot = at.createTransformedShape(p);
            relleno(g, rot, new Color(0xFF8A1F), b);
            g.setColor(new Color(0xD66A0C));
            g.setStroke(new BasicStroke(2f));
            g.draw(at.createTransformedShape(new Line2D.Double(-8, -10, 2, -8)));
            g.draw(at.createTransformedShape(new Line2D.Double(-8, 4, 0, 6)));
            relleno(g, new Ellipse2D.Double(-6, -42, 12, 24), new Color(0x4CAF50), b);
            relleno(g, new Ellipse2D.Double(8, -38, 12, 22), new Color(0x66BB6A), b);
        } else if (n.contains("brocoli")) {
            relleno(g, new RoundRectangle2D.Double(-8, 4, 16, 28, 8, 8), new Color(0x8BC34A), b);
            Color v = new Color(0x2E9B3E);
            relleno(g, new Ellipse2D.Double(-30, -14, 28, 28), v, b);
            relleno(g, new Ellipse2D.Double(2, -14, 28, 28), v, b);
            relleno(g, new Ellipse2D.Double(-17, -30, 34, 32), new Color(0x38AD4A), b);
        } else if (n.contains("papas")) {
            g.setColor(new Color(0xFFD23F));
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -2; i <= 2; i++) g.draw(new Line2D.Double(i * 8, -8, i * 8 + i * 2, -30 - (i % 2 == 0 ? 4 : 0)));
            Path2D c = new Path2D.Double();
            c.moveTo(-24, -10); c.lineTo(24, -10); c.lineTo(18, 32); c.lineTo(-18, 32); c.closePath();
            relleno(g, c, new Color(0xD62828), b);
            g.setColor(new Color(255, 255, 255, 230));
            g.fill(new Ellipse2D.Double(-8, 4, 16, 16));
        } else if (n.contains("hamburguesa")) {
            relleno(g, new Arc2D.Double(-30, -30, 60, 52, 0, 180, Arc2D.CHORD), new Color(0xE0993A), b);
            g.setColor(new Color(0xFFF1C9));
            for (int i = -2; i <= 2; i++) g.fill(new Ellipse2D.Double(i * 10 - 2, -20 + Math.abs(i) * 2, 5, 3));
            relleno(g, new RoundRectangle2D.Double(-32, -4, 64, 9, 9, 9), new Color(0x5BC236), b);
            relleno(g, new RoundRectangle2D.Double(-30, 4, 60, 12, 10, 10), new Color(0x6B3A1E), b);
            relleno(g, new RoundRectangle2D.Double(-28, 14, 56, 14, 14, 14), new Color(0xE0993A), b);
        } else {
            Color c = tipo == 0 ? new Color(0xFF7043) : tipo == 1 ? new Color(0x66BB6A) : new Color(0xFFCA28);
            relleno(g, new Ellipse2D.Double(-26, -26, 52, 52), c, b);
        }
        g.dispose();
    }

    public static void medicina(Graphics2D g0, String nombre, double cx, double cy, double s) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.scale(s / 60.0, s / 60.0);
        String n = limpio(nombre);
        if (n.contains("jarabe")) {
            relleno(g, new RoundRectangle2D.Double(-18, -14, 36, 46, 14, 14), new Color(0xE84A7F), 2.6f);
            relleno(g, new RoundRectangle2D.Double(-11, -30, 22, 18, 6, 6), new Color(0xF2F2F2), 2.6f);
            relleno(g, new RoundRectangle2D.Double(-13, 2, 26, 18, 4, 4), Color.WHITE, 2f);
            g.setColor(new Color(0xD62828));
            g.fill(new Rectangle2D.Double(-2.5, 5, 5, 12));
            g.fill(new Rectangle2D.Double(-6, 8.5, 12, 5));
        } else {
            AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(-35));
            Shape mitad1 = at.createTransformedShape(new RoundRectangle2D.Double(-30, -12, 32, 24, 24, 24));
            Shape mitad2 = at.createTransformedShape(new RoundRectangle2D.Double(-2, -12, 32, 24, 24, 24));
            relleno(g, mitad1, new Color(0xE63946), 2.6f);
            relleno(g, mitad2, Color.WHITE, 2.6f);
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(at.createTransformedShape(new Ellipse2D.Double(-24, -8, 18, 6)));
        }
        g.dispose();
    }

    public static void prenda(Graphics2D g0, String nombre, double cx, double cy, double s) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.scale(s / 60.0, s / 60.0);
        String n = limpio(nombre);
        if (n.contains("gorra")) {
            Path2D c = new Path2D.Double();
            c.moveTo(-26, 12); c.curveTo(-28, -22, -6, -30, 6, -30); c.curveTo(22, -30, 28, -10, 26, 12); c.closePath();
            relleno(g, c, new Color(0x2E6FD8), 2.6f);
            Path2D v = new Path2D.Double();
            v.moveTo(10, 8); v.curveTo(34, 2, 50, 8, 52, 16); v.curveTo(34, 20, 14, 20, 8, 14); v.closePath();
            relleno(g, v, new Color(0x1F55B0), 2.6f);
            g.setColor(new Color(0xF4D03F));
            g.fill(new Ellipse2D.Double(1, -34, 9, 9));
        } else if (n.contains("remera")) {
            Path2D c = new Path2D.Double();
            c.moveTo(-14, -28); c.lineTo(-34, -16); c.lineTo(-26, -2); c.lineTo(-18, -8); c.lineTo(-18, 28);
            c.lineTo(18, 28); c.lineTo(18, -8); c.lineTo(26, -2); c.lineTo(34, -16); c.lineTo(14, -28);
            c.curveTo(8, -20, -8, -20, -14, -28);
            c.closePath();
            relleno(g, c, new Color(0xD9453B), 2.6f);
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(-18, 2, 36, 6));
            g.fill(new Rectangle2D.Double(-18, 14, 36, 6));
        } else if (n.contains("zapatilla")) {
            for (int i = 0; i < 2; i++) {
                double dx = -22 + i * 26, dy = i * 6;
                relleno(g, new RoundRectangle2D.Double(dx - 4, -6 + dy - 6, 38, 24, 18, 18), Color.WHITE, 2.4f);
                relleno(g, new RoundRectangle2D.Double(dx - 4, 8 + dy - 6, 38, 10, 8, 8), new Color(0xE23D3D), 2.4f);
            }
        } else {
            g.setColor(new Color(20, 20, 20));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(150, 210, 255, 90));
            g.fill(new Ellipse2D.Double(-34, -14, 30, 30));
            g.fill(new Ellipse2D.Double(4, -14, 30, 30));
            g.setColor(new Color(20, 20, 20));
            g.draw(new Ellipse2D.Double(-34, -14, 30, 30));
            g.draw(new Ellipse2D.Double(4, -14, 30, 30));
            g.draw(new Arc2D.Double(-6, -10, 12, 10, 0, 180, Arc2D.OPEN));
        }
        g.dispose();
    }

    public static void moneda(Graphics2D g0, double cx, double cy, double r, boolean especial, double giro) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        double ancho = Math.max(0.12, Math.abs(Math.cos(giro)));
        g.scale(ancho, 1);
        Color a = especial ? new Color(0xFFF08A) : new Color(0xFFD23F);
        Color b = especial ? new Color(0xF2A900) : new Color(0xE8A800);
        g.setPaint(new GradientPaint(0, (float) -r, a, 0, (float) r, b));
        g.fill(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
        g.setColor(new Color(0x9A6A00));
        g.setStroke(new BasicStroke(Math.max(1.6f, (float) r / 7), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
        g.draw(new Ellipse2D.Double(-r * 0.68, -r * 0.68, r * 1.36, r * 1.36));
        if (especial) {
            g.setColor(new Color(0xB8860B));
            g.fill(Particulas.estrella(r * 0.55, 5, 0.45));
        } else {
            g.setColor(new Color(0xB8860B));
            g.fill(new Rectangle2D.Double(-r * 0.09, -r * 0.4, r * 0.18, r * 0.8));
        }
        g.dispose();
    }

    /**
     * Iconos de la barra inferior.
     * 0 sala, 1 cocina, 2 bano, 3 dormir, 4 despertar, 5 ropero, 6 farmacia, 7 jugar, 8 estado, 9 salir
     */
    public static void nav(Graphics2D g0, int idx, double cx, double cy, double s, Color c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.scale(s / 40.0, s / 40.0);
        g.setColor(c);
        g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (idx) {
            case 0: { // casa
                Path2D p = new Path2D.Double();
                p.moveTo(-16, 0); p.lineTo(0, -16); p.lineTo(16, 0);
                g.draw(p);
                g.draw(new Rectangle2D.Double(-11, 0, 22, 15));
                g.fill(new Rectangle2D.Double(-3, 6, 6, 9));
                break;
            }
            case 1: { // manzana
                g.fill(new Ellipse2D.Double(-13, -9, 26, 26));
                g.draw(new Line2D.Double(0, -9, 3, -17));
                g.fill(new Ellipse2D.Double(3, -19, 11, 6));
                break;
            }
            case 2: { // gota
                Path2D p = new Path2D.Double();
                p.moveTo(0, -17); p.curveTo(14, 2, 14, 16, 0, 16); p.curveTo(-14, 16, -14, 2, 0, -17);
                g.fill(p);
                break;
            }
            case 3: { // luna
                Area a = new Area(new Ellipse2D.Double(-14, -14, 28, 28));
                a.subtract(new Area(new Ellipse2D.Double(-4, -18, 26, 26)));
                g.fill(a);
                break;
            }
            case 4: { // sol
                g.fill(new Ellipse2D.Double(-8, -8, 16, 16));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.draw(new Line2D.Double(Math.cos(a) * 13, Math.sin(a) * 13, Math.cos(a) * 18, Math.sin(a) * 18));
                }
                break;
            }
            case 5: { // remera
                Path2D p = new Path2D.Double();
                p.moveTo(-6, -15); p.lineTo(-17, -8); p.lineTo(-12, -1); p.lineTo(-8, -4); p.lineTo(-8, 15);
                p.lineTo(8, 15); p.lineTo(8, -4); p.lineTo(12, -1); p.lineTo(17, -8); p.lineTo(6, -15);
                p.curveTo(3, -10, -3, -10, -6, -15);
                g.fill(p);
                break;
            }
            case 6: { // cruz
                g.fill(new RoundRectangle2D.Double(-5, -16, 10, 32, 3, 3));
                g.fill(new RoundRectangle2D.Double(-16, -5, 32, 10, 3, 3));
                break;
            }
            case 7: { // pelota de voley
                g.draw(new Ellipse2D.Double(-15, -15, 30, 30));
                g.draw(new Arc2D.Double(-15, -30, 30, 30, 200, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(-30, -8, 30, 30, 300, 90, Arc2D.OPEN));
                g.draw(new Line2D.Double(0, -15, 0, 0));
                break;
            }
            case 8: { // corazon
                g.fill(Particulas.corazon(15));
                break;
            }
            default: { // salir
                g.draw(new Rectangle2D.Double(-14, -15, 16, 30));
                g.draw(new Line2D.Double(-2, 0, 17, 0));
                g.draw(new Line2D.Double(10, -7, 17, 0));
                g.draw(new Line2D.Double(10, 7, 17, 0));
            }
        }
        g.dispose();
    }
}
