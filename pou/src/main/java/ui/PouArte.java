package ui;

import java.awt.*;
import java.awt.geom.*;

/**
 * Dibuja al Pou con Java2D (sin imagenes externas).
 * Todo se dibuja en coordenadas locales: el origen es el centro de los pies
 * y el cuerpo sube hacia y negativo (mide ~172 px de alto y ~180 de ancho).
 */
public final class PouArte {

    public enum Cara { NORMAL, FELIZ, TRISTE, COMIENDO, SORPRESA, DORMIDO, ENFERMO, MUERTO, RISA, ASCO }

    /** Prendas que lleva puestas (se arma segun las Prenda con estaPuesta). */
    public static class Ropa {
        public boolean gorra, remera, zapatillas, anteojos;
    }

    /** Todo lo que cambia cuadro a cuadro. */
    public static class Pose {
        public double sx = 1, sy = 1;
        public double giro = 0;
        public double miraX = 0, miraY = 0;
        public double parpadeo = 0;
        public double boca = 0;
        public double enfermo = 0;
        public double sucio = 0;
        public double espuma = 0;
        public double fantasma = 0;
        public double moco = 0;
        public Cara cara = Cara.NORMAL;
        public Color color = MARRON;
    }

    public static final Color MARRON = new Color(0xA8703F);
    private static final Color CONTORNO = new Color(0x5B3519);
    private static final Color OSCURO = new Color(0x2A1608);

    private static final Path2D CUERPO = crearCuerpo();

    private static Path2D crearCuerpo() {
        Path2D p = new Path2D.Double();
        p.moveTo(0, -172);
        p.curveTo(20, -172, 30, -150, 42, -124);
        p.curveTo(78, -96, 94, -56, 89, -26);
        p.curveTo(86, -6, 60, 0, 0, 0);
        p.curveTo(-60, 0, -86, -6, -89, -26);
        p.curveTo(-94, -56, -78, -96, -42, -124);
        p.curveTo(-30, -150, -20, -172, 0, -172);
        p.closePath();
        return p;
    }

    private static final double[][] MANCHAS = {
            {-50, -40, 9}, {40, -70, 7}, {-15, -25, 11}, {55, -30, 8}, {-60, -80, 6},
            {12, -112, 7}, {26, -45, 9}, {-35, -62, 6}, {2, -14, 8}, {-70, -30, 7}
    };
    private static final double[][] ESPUMA = {
            {-45, -35, 17}, {35, -60, 15}, {-10, -20, 19}, {52, -28, 14}, {-58, -78, 13},
            {8, -105, 16}, {-30, -70, 12}, {20, -38, 16}, {-5, -135, 11}, {60, -90, 10}
    };

    private PouArte() { }

    // ------------------------------------------------------------------ utilidades

    public static Color mezclar(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static AffineTransform transformacion(double x, double y, double escala, Pose p) {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.rotate(p.giro);
        at.scale(escala * p.sx, escala * p.sy);
        return at;
    }

    /** Convierte un punto local del Pou (ej. la boca en 0,-58) a coordenadas de pantalla. */
    public static Point2D aPantalla(double x, double y, double escala, Pose p, double lx, double ly) {
        return transformacion(x, y, escala, p).transform(new Point2D.Double(lx, ly), null);
    }

    public static boolean contiene(double x, double y, double escala, Pose p, double px, double py) {
        Shape s = transformacion(x, y, escala, p).createTransformedShape(CUERPO);
        return s.contains(px, py);
    }

    // ------------------------------------------------------------------ dibujo

    public static void dibujar(Graphics2D g0, double x, double y, double escala, Ropa ropa, Pose p) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.rotate(p.giro);
        g.scale(escala * p.sx, escala * p.sy);
        if (p.fantasma > 0) {
            g.setComposite(AlphaComposite.SrcOver.derive((float) (1 - 0.45 * p.fantasma)));
        }

        Color base = p.color;
        if (p.enfermo > 0) base = mezclar(base, new Color(0x8DB44E), p.enfermo * 0.65);
        if (p.fantasma > 0) base = mezclar(base, new Color(0xDCE8FF), p.fantasma * 0.8);
        Color claro = mezclar(base, Color.WHITE, 0.22);
        Color oscuro = mezclar(base, Color.BLACK, 0.30);

        // cuerpo
        g.setPaint(new GradientPaint(0, -172, claro, 0, 0, oscuro));
        g.fill(CUERPO);

        Shape clipViejo = g.getClip();
        g.clip(CUERPO);

        // remera
        if (ropa != null && ropa.remera) dibujarRemera(g);

        // manchas de suciedad
        int manchas = (int) Math.round(p.sucio * MANCHAS.length);
        for (int i = 0; i < manchas; i++) {
            double[] m = MANCHAS[i];
            g.setColor(i % 2 == 0 ? new Color(60, 38, 14, 150) : new Color(80, 100, 30, 120));
            g.fill(new Ellipse2D.Double(m[0] - m[2], m[1] - m[2] * 0.8, m[2] * 2, m[2] * 1.6));
        }
        // espuma
        int burbujas = (int) Math.round(p.espuma * ESPUMA.length);
        for (int i = 0; i < burbujas; i++) {
            double[] m = ESPUMA[i];
            g.setColor(new Color(255, 255, 255, 235));
            g.fill(new Ellipse2D.Double(m[0] - m[2], m[1] - m[2], m[2] * 2, m[2] * 2));
            g.setColor(new Color(170, 210, 245, 200));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(m[0] - m[2], m[1] - m[2], m[2] * 2, m[2] * 2));
            g.setColor(new Color(255, 255, 255, 255));
            g.fill(new Ellipse2D.Double(m[0] - m[2] * 0.5, m[1] - m[2] * 0.6, m[2] * 0.4, m[2] * 0.4));
        }
        // brillo
        g.setColor(new Color(255, 255, 255, 60));
        AffineTransform previo = g.getTransform();
        g.rotate(Math.toRadians(-22), -40, -118);
        g.fill(new Ellipse2D.Double(-54, -146, 30, 54));
        g.setTransform(previo);
        g.setClip(clipViejo);

        // contorno
        g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(mezclar(CONTORNO, base, p.fantasma * 0.5));
        g.draw(CUERPO);

        dibujarCara(g, p, base);

        if (ropa != null) {
            if (ropa.anteojos) dibujarAnteojos(g);
            if (ropa.gorra) dibujarGorra(g);
            if (ropa.zapatillas) dibujarZapatillas(g);
        }
        g.dispose();
    }

    private static void dibujarRemera(Graphics2D g) {
        g.setColor(new Color(0xD9453B));
        g.fill(new Rectangle2D.Double(-100, -40, 200, 54));
        g.setColor(new Color(0xF2F2F2));
        g.fill(new Rectangle2D.Double(-100, -28, 200, 7));
        g.fill(new Rectangle2D.Double(-100, -12, 200, 7));
        g.setColor(new Color(0x9E2A22));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(-100, -40, 100, -40));
    }

    private static void dibujarGorra(Graphics2D g) {
        Path2D c = new Path2D.Double();
        c.moveTo(-40, -130);
        c.curveTo(-42, -170, -22, -190, 0, -190);
        c.curveTo(22, -190, 42, -170, 40, -130);
        c.curveTo(20, -124, -20, -124, -40, -130);
        c.closePath();
        g.setColor(new Color(0x2E6FD8));
        g.fill(c);
        g.setColor(new Color(0x1B3F85));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(c);
        // visera
        Path2D v = new Path2D.Double();
        v.moveTo(8, -131);
        v.curveTo(40, -140, 76, -134, 82, -124);
        v.curveTo(60, -116, 20, -118, 8, -124);
        v.closePath();
        g.setColor(new Color(0x1F55B0));
        g.fill(v);
        g.setColor(new Color(0x1B3F85));
        g.draw(v);
        // boton y brillo
        g.setColor(new Color(0xF4D03F));
        g.fill(new Ellipse2D.Double(-5, -194, 10, 10));
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new Ellipse2D.Double(-26, -176, 14, 28));
    }

    private static void dibujarZapatillas(Graphics2D g) {
        for (int lado = -1; lado <= 1; lado += 2) {
            double cx = lado * 46;
            Shape zapa = new RoundRectangle2D.Double(cx - 28, -8, 56, 24, 22, 22);
            g.setColor(Color.WHITE);
            g.fill(zapa);
            g.setColor(new Color(0xE23D3D));
            g.fill(new RoundRectangle2D.Double(cx - 28, 6, 56, 10, 10, 10));
            g.setColor(new Color(0x444444));
            g.setStroke(new BasicStroke(2.6f));
            g.draw(zapa);
            g.setColor(new Color(0xE23D3D));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx - 8, -4, cx + 10, -4));
        }
    }

    private static void dibujarAnteojos(Graphics2D g) {
        g.setColor(new Color(20, 20, 20));
        g.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int lado = -1; lado <= 1; lado += 2) {
            Ellipse2D lente = new Ellipse2D.Double(lado * 30 - 25, -100 - 26, 50, 52);
            g.setColor(new Color(150, 210, 255, 55));
            g.fill(lente);
            g.setColor(new Color(20, 20, 20));
            g.draw(lente);
        }
        g.draw(new Arc2D.Double(-9, -126, 18, 14, 0, 180, Arc2D.OPEN));
        g.draw(new Line2D.Double(-55, -105, -88, -112));
        g.draw(new Line2D.Double(55, -105, 88, -112));
    }

    // ------------------------------------------------------------------ cara

    private static void dibujarCara(Graphics2D g, Pose p, Color base) {
        Cara c = p.cara;
        double cejaTriste = (c == Cara.TRISTE || c == Cara.ENFERMO || c == Cara.ASCO) ? 1 : 0;
        boolean cerrado = c == Cara.DORMIDO;
        boolean risa = c == Cara.RISA;

        // mejillas
        if (c == Cara.FELIZ || c == Cara.RISA) {
            g.setColor(new Color(255, 120, 120, 90));
            g.fill(new Ellipse2D.Double(-68, -80, 24, 14));
            g.fill(new Ellipse2D.Double(44, -80, 24, 14));
        }

        for (int lado = -1; lado <= 1; lado += 2) {
            double cx = lado * 30, cy = -100;
            if (c == Cara.MUERTO) {
                g.setColor(OSCURO);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(cx - 11, cy - 11, cx + 11, cy + 11));
                g.draw(new Line2D.Double(cx - 11, cy + 11, cx + 11, cy - 11));
                continue;
            }
            if (cerrado) {
                g.setColor(OSCURO);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D ojo = new Path2D.Double();
                ojo.moveTo(cx - 14, cy);
                ojo.quadTo(cx, cy + 12, cx + 14, cy);
                g.draw(ojo);
                continue;
            }
            if (risa) {
                g.setColor(OSCURO);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D ojo = new Path2D.Double();
                ojo.moveTo(cx - 14, cy + 4);
                ojo.quadTo(cx, cy - 14, cx + 14, cy + 4);
                g.draw(ojo);
                continue;
            }
            double ancho = 36, alto = 42;
            if (c == Cara.SORPRESA) { ancho = 40; alto = 48; }
            double abierto = 1 - Math.max(0, Math.min(1, p.parpadeo));
            double h = Math.max(3, alto * abierto);
            Ellipse2D ojo = new Ellipse2D.Double(cx - ancho / 2, cy - h / 2, ancho, h);
            g.setColor(Color.WHITE);
            g.fill(ojo);
            // pupila
            if (abierto > 0.35) {
                double rad = (c == Cara.SORPRESA) ? 5.5 : 8;
                double px = cx + p.miraX * 8, py = cy + p.miraY * 9;
                g.setColor(OSCURO);
                g.fill(new Ellipse2D.Double(px - rad, py - rad, rad * 2, rad * 2));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(px - rad * 0.2, py - rad * 0.75, rad * 0.7, rad * 0.7));
            }
            // parpado de enfermo / cansado
            if (c == Cara.ENFERMO || c == Cara.ASCO) {
                Shape clip = g.getClip();
                g.clip(ojo);
                g.setColor(mezclar(base, Color.BLACK, 0.05));
                g.fill(new Rectangle2D.Double(cx - 30, cy - 30, 60, 30 + (c == Cara.ENFERMO ? 6 : 1)));
                g.setClip(clip);
            }
            g.setColor(CONTORNO);
            g.setStroke(new BasicStroke(2.6f));
            g.draw(ojo);

            if (cejaTriste > 0) {
                g.setColor(OSCURO);
                g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double iX = cx - lado * 12, oX = cx + lado * 14;
                g.draw(new Line2D.Double(oX, cy - 25, iX, cy - 36));
            }
        }

        // boca
        g.setColor(OSCURO);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (c) {
            case FELIZ:
            case RISA: {
                Path2D b = new Path2D.Double();
                b.moveTo(-24, -66);
                b.quadTo(0, -34, 24, -66);
                b.closePath();
                g.fill(b);
                g.setColor(new Color(0xE8707A));
                Shape clip = g.getClip();
                g.clip(b);
                g.fill(new Ellipse2D.Double(-11, -54, 22, 16));
                g.setClip(clip);
                break;
            }
            case COMIENDO: {
                double h = 8 + p.boca * 30;
                Ellipse2D b = new Ellipse2D.Double(-19, -58 - h / 2, 38, h);
                g.fill(b);
                g.setColor(new Color(0xE8707A));
                Shape clip = g.getClip();
                g.clip(b);
                g.fill(new Ellipse2D.Double(-10, -58 + h / 2 - 9, 20, 14));
                g.setClip(clip);
                break;
            }
            case SORPRESA:
                g.fill(new Ellipse2D.Double(-9, -66, 18, 22));
                break;
            case TRISTE: {
                Path2D b = new Path2D.Double();
                b.moveTo(-15, -50);
                b.quadTo(0, -64, 15, -50);
                g.draw(b);
                break;
            }
            case DORMIDO: {
                Path2D b = new Path2D.Double();
                b.moveTo(-8, -58);
                b.quadTo(0, -53, 8, -58);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(b);
                break;
            }
            case ENFERMO:
            case ASCO: {
                Path2D b = new Path2D.Double();
                b.moveTo(-19, -55);
                b.curveTo(-13, -65, -7, -47, 0, -55);
                b.curveTo(7, -63, 13, -47, 19, -55);
                g.draw(b);
                break;
            }
            case MUERTO: {
                g.draw(new Line2D.Double(-14, -58, 14, -58));
                g.setColor(new Color(0xE8707A));
                g.fill(new RoundRectangle2D.Double(3, -58, 12, 17, 8, 8));
                break;
            }
            default: {
                Path2D b = new Path2D.Double();
                b.moveTo(-17, -59);
                b.quadTo(0, -47, 17, -59);
                g.draw(b);
            }
        }

        // burbuja de moco al dormir
        if (p.moco > 0.02) {
            double r = 4 + 15 * p.moco;
            g.setColor(new Color(220, 240, 255, 150));
            g.fill(new Ellipse2D.Double(26 - r / 2, -70 - r / 2, r, r));
            g.setColor(new Color(255, 255, 255, 220));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(26 - r / 2, -70 - r / 2, r, r));
        }

        // termometro
        if (c == Cara.ENFERMO) {
            AffineTransform previo = g.getTransform();
            g.rotate(Math.toRadians(-18), 10, -58);
            g.setColor(new Color(255, 255, 255, 240));
            g.fill(new RoundRectangle2D.Double(10, -62, 64, 9, 9, 9));
            g.setColor(new Color(0xD8302C));
            g.fill(new RoundRectangle2D.Double(10, -62, 28, 9, 9, 9));
            g.setColor(CONTORNO);
            g.setStroke(new BasicStroke(1.8f));
            g.draw(new RoundRectangle2D.Double(10, -62, 64, 9, 9, 9));
            g.setTransform(previo);
        }
    }
}
