package ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Sistema de particulas: corazones, burbujas, migas, brillos, textos flotantes, etc. */
public final class Particulas {

    public enum Tipo { CORAZON, BURBUJA, MIGA, BRILLO, GOTA, ZZZ, TEXTO, ESTRELLA, ESPUMA, SUDOR, NOTA, VAPOR, POLVO, ARCOIRIS }

    private static final class P {
        Tipo t; double x, y, vx, vy, vida, max, tam, rot, vrot, grav; Color c; String txt;
    }

    private final List<P> lista = new ArrayList<>();
    private static final Color[] ARCO = {
            new Color(0xFF4D4D), new Color(0xFF9F43), new Color(0xFFE14D),
            new Color(0x4DDB6D), new Color(0x4DB5FF), new Color(0xA66BFF)};

    private static double r(double a, double b) { return ThreadLocalRandom.current().nextDouble(a, b); }

    public void add(Tipo t, double x, double y, double vx, double vy, double vida, double tam, double grav, Color c, String txt) {
        P p = new P();
        p.t = t; p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.vida = vida; p.max = vida; p.tam = tam;
        p.grav = grav; p.c = c; p.txt = txt; p.rot = r(-0.5, 0.5); p.vrot = r(-3, 3);
        if (lista.size() < 900) lista.add(p);
    }

    public void corazones(double x, double y, int n) {
        for (int i = 0; i < n; i++)
            add(Tipo.CORAZON, x + r(-30, 30), y + r(-10, 10), r(-25, 25), r(-90, -50), r(1.0, 1.7), r(10, 20), 0, null, null);
    }
    public void brillos(double x, double y, int n, Color c) {
        for (int i = 0; i < n; i++) {
            double a = r(0, Math.PI * 2), v = r(40, 220);
            add(Tipo.BRILLO, x, y, Math.cos(a) * v, Math.sin(a) * v, r(0.5, 1.1), r(4, 10), 120, c, null);
        }
    }
    public void estrellas(double x, double y, int n) {
        for (int i = 0; i < n; i++) {
            double a = r(0, Math.PI * 2), v = r(80, 320);
            add(Tipo.ESTRELLA, x, y, Math.cos(a) * v, Math.sin(a) * v, r(0.7, 1.4), r(8, 16), 200, ARCO[i % ARCO.length], null);
        }
    }
    public void migas(double x, double y, int n, Color c) {
        for (int i = 0; i < n; i++)
            add(Tipo.MIGA, x + r(-12, 12), y, r(-120, 120), r(-200, -60), r(0.5, 0.9), r(3, 6), 700, c, null);
    }
    public void gotas(double x, double y, int n) {
        for (int i = 0; i < n; i++)
            add(Tipo.GOTA, x + r(-60, 60), y, r(-20, 20), r(300, 460), r(0.5, 0.8), r(3, 5), 500, null, null);
    }
    public void burbujas(double x, double y, int n) {
        for (int i = 0; i < n; i++)
            add(Tipo.BURBUJA, x + r(-70, 70), y + r(-60, 40), r(-15, 15), r(-70, -25), r(1.2, 2.4), r(6, 18), 0, null, null);
    }
    public void polvo(double x, double y, int n) {
        for (int i = 0; i < n; i++)
            add(Tipo.POLVO, x + r(-30, 30), y, r(-110, 110), r(-40, -5), r(0.35, 0.6), r(5, 11), 0, null, null);
    }
    public void texto(double x, double y, String txt, Color c) {
        add(Tipo.TEXTO, x, y, 0, -55, 1.6, 20, 0, c, txt);
    }
    public void zzz(double x, double y) {
        add(Tipo.ZZZ, x, y, r(10, 25), -35, 2.2, r(16, 26), 0, null, "Z");
    }
    public void nota(double x, double y) {
        add(Tipo.NOTA, x, y, r(-40, 40), -70, 1.6, r(14, 20), 0, ARCO[ThreadLocalRandom.current().nextInt(ARCO.length)], null);
    }
    public void vapor(double x, double y) {
        add(Tipo.VAPOR, x + r(-60, 60), y, r(-10, 10), r(-50, -25), r(1.0, 1.8), r(20, 40), 0, null, null);
    }
    public void sudor(double x, double y) {
        add(Tipo.SUDOR, x, y, r(-10, 10), -40, 0.9, 9, 400, null, null);
    }
    public void arcoiris(double x, double y) {
        add(Tipo.ARCOIRIS, x, y, 0, 0, 0.7, 14, 0, ARCO[(int) (System.nanoTime() / 40_000_000L) % ARCO.length], null);
    }

    public void limpiar() { lista.clear(); }

    public void actualizar(double dt) {
        for (Iterator<P> it = lista.iterator(); it.hasNext(); ) {
            P p = it.next();
            p.vida -= dt;
            if (p.vida <= 0) { it.remove(); continue; }
            p.vy += p.grav * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += p.vrot * dt;
            if (p.t == Tipo.ZZZ || p.t == Tipo.BURBUJA) p.x += Math.sin(p.vida * 4) * 0.4;
        }
    }

    public void dibujar(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (P p : lista) {
            double f = Math.max(0, Math.min(1, p.vida / p.max));
            float a = (float) Math.min(1, f * 2.2);
            Graphics2D h = (Graphics2D) g.create();
            h.setComposite(AlphaComposite.SrcOver.derive(a));
            h.translate(p.x, p.y);
            switch (p.t) {
                case CORAZON: {
                    h.rotate(p.rot * 0.3);
                    double s = p.tam * (0.7 + 0.5 * (1 - f));
                    h.setColor(new Color(0xFF4F7B));
                    h.fill(corazon(s));
                    h.setColor(new Color(255, 255, 255, 140));
                    h.fill(new Ellipse2D.Double(-s * 0.45, -s * 0.5, s * 0.3, s * 0.2));
                    break;
                }
                case BURBUJA:
                    h.setColor(new Color(200, 230, 255, 110));
                    h.fill(new Ellipse2D.Double(-p.tam, -p.tam, p.tam * 2, p.tam * 2));
                    h.setColor(new Color(255, 255, 255, 220));
                    h.setStroke(new BasicStroke(1.6f));
                    h.draw(new Ellipse2D.Double(-p.tam, -p.tam, p.tam * 2, p.tam * 2));
                    h.fill(new Ellipse2D.Double(-p.tam * 0.55, -p.tam * 0.6, p.tam * 0.5, p.tam * 0.4));
                    break;
                case MIGA:
                    h.rotate(p.rot);
                    h.setColor(p.c);
                    h.fill(new Rectangle2D.Double(-p.tam / 2, -p.tam / 2, p.tam, p.tam));
                    break;
                case BRILLO:
                    h.rotate(p.rot);
                    h.setColor(p.c);
                    h.fill(estrella(p.tam, 4, 0.3));
                    break;
                case ESTRELLA:
                    h.rotate(p.rot);
                    h.setColor(p.c);
                    h.fill(estrella(p.tam, 5, 0.45));
                    h.setColor(new Color(255, 255, 255, 170));
                    h.fill(estrella(p.tam * 0.45, 5, 0.45));
                    break;
                case GOTA:
                    h.setColor(new Color(120, 190, 255, 210));
                    h.fill(new Ellipse2D.Double(-p.tam * 0.5, -p.tam * 1.6, p.tam, p.tam * 3.2));
                    break;
                case ESPUMA:
                    h.setColor(new Color(255, 255, 255, 230));
                    h.fill(new Ellipse2D.Double(-p.tam, -p.tam, p.tam * 2, p.tam * 2));
                    break;
                case ZZZ:
                    h.setFont(new Font("SansSerif", Font.BOLD, (int) p.tam));
                    h.setColor(new Color(0xE9F0FF));
                    h.drawString(p.txt, 0, 0);
                    break;
                case TEXTO:
                    h.setFont(new Font("SansSerif", Font.BOLD, (int) p.tam));
                    h.setColor(new Color(0, 0, 0, 120));
                    h.drawString(p.txt, 2, 2);
                    h.setColor(p.c);
                    h.drawString(p.txt, 0, 0);
                    break;
                case SUDOR:
                    h.setColor(new Color(150, 210, 255));
                    h.fill(new Ellipse2D.Double(-p.tam * 0.4, -p.tam * 0.6, p.tam * 0.8, p.tam * 1.2));
                    break;
                case NOTA:
                    h.setColor(p.c);
                    h.fill(new Ellipse2D.Double(-p.tam * 0.4, 0, p.tam * 0.8, p.tam * 0.55));
                    h.setStroke(new BasicStroke(2.4f));
                    h.draw(new Line2D.Double(p.tam * 0.38, 4, p.tam * 0.38, -p.tam));
                    h.draw(new Line2D.Double(p.tam * 0.38, -p.tam, p.tam * 0.8, -p.tam * 0.6));
                    break;
                case VAPOR:
                    h.setColor(new Color(255, 255, 255, 90));
                    h.fill(new Ellipse2D.Double(-p.tam, -p.tam, p.tam * 2, p.tam * 2));
                    break;
                case POLVO:
                    h.setColor(new Color(235, 225, 205, 170));
                    h.fill(new Ellipse2D.Double(-p.tam, -p.tam, p.tam * 2, p.tam * 2));
                    break;
                case ARCOIRIS:
                    h.setColor(p.c);
                    double s = p.tam * f;
                    h.fill(new Ellipse2D.Double(-s, -s, s * 2, s * 2));
                    break;
            }
            h.dispose();
        }
        g.dispose();
    }

    public static Shape corazon(double s) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, s * 0.9);
        p.curveTo(-s * 1.4, -s * 0.1, -s * 0.7, -s * 1.0, 0, -s * 0.3);
        p.curveTo(s * 0.7, -s * 1.0, s * 1.4, -s * 0.1, 0, s * 0.9);
        p.closePath();
        return p;
    }

    public static Shape estrella(double r, int puntas, double interior) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < puntas * 2; i++) {
            double rad = (i % 2 == 0) ? r : r * interior;
            double a = -Math.PI / 2 + i * Math.PI / puntas;
            double x = Math.cos(a) * rad, y = Math.sin(a) * rad;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }
}
