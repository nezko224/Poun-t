package ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Arte procedural de "Operacion Mugre" (texturas, pisos, techos y sprites).  Todo se dibuja por codigo, sin archivos.
 */
final class ShooterArte {
    private ShooterArte() {}

    // ---- ids de texturas de pared
    static final int T_PUERTA = 10, T_CANDADO_ORO = 11, T_DUCHA_OFF = 12, T_DUCHA_ON = 13,
            T_ESCARCHA = 14, T_DESPENSA = 15, T_CANDADO_PLATA = 16, T_ALACENA_OFF = 17, T_ALACENA_ON = 18,
            T_HORN_ON = 19, T_HORN_OFF = 20, T_HELADERITA = 21, T_BAUL = 22, T_DESVAN = 23, T_TELA = 24,
            T_BIBLIO = 25, T_DISFRAZ = 26, T_COSTURA = 27, T_CANDADO_HILO = 28, T_TRAMP_OFF = 29, T_TRAMP_ON = 30,
            T_LADRILLO = 31, T_LAVARROPAS = 32, T_LAVAR_ABIERTO = 33, T_ROPA = 34, T_TUBOS = 35, T_COLUMNA = 36,
            T_VALV_OFF = 37, T_VALV_ON = 38, T_CALDERA = 39, T_CANDADO_LLAVE = 40, T_DESAGUE_OFF = 41,
            T_DESAGUE_ON = 42, T_CANO_SUELTO = 43, T_OXIDO = 44, T_REJA = 45, T_ESPUMA = 46,
            T_P3_0 = 47, T_P3_1 = 48, T_P3_2 = 49, T_P3_3 = 50, T_TRONO = 51, T_CADENA = 52, T_CADENA_REC = 53,
            T_CANO_ROTO = 54, T_HORN_DIN = 55, T_MESADA_PUERTITA = 56, T_LIBRO = 57, T_MUNECA = 58,
            T_VENTANA = 59, T_VENTANA_ROTA = 60, T_TRONO_ATRAS = 61;
    // ---- ids de pisos
    static final int P_DESPENSA = 6, P_HIELO = 7, P_HORNO = 8, P_DESVAN = 9, P_ALFOMBRA = 10, P_BIBLIO = 11,
            P_COSTURA = 12, P_VERDE = 13, P_LAVADERO = 14, P_CALDERA = 15, P_REJA = 16, P_ESPUMA = 17, P_TRONO = 18;

    static final class Spr { final int w, h; final int[] px; Spr(int w, int h, int[] px) { this.w = w; this.h = h; this.px = px; } }

    static final int[][] TEX = new int[64][];
    static final int[][] PISO = new int[24][];
    static final int[][] TECHOS = new int[6][];
    static final Spr[] MUG = new Spr[2], TIR = new Spr[2], REYS = new Spr[2], POPS = new Spr[3];
    static final Spr MUG_L, TIR_L, REY_L, TIR_A, SP_JAB, SP_BUR, SP_EST, SP_LLA, SP_BALA, SP_MUGRE;

    static Spr spr(int w, int h, Consumer<Graphics2D> f) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        f.accept(g);
        g.dispose();
        return new Spr(w, h, im.getRGB(0, 0, w, h, null, 0, w));
    }

    static int[] tex(Consumer<Graphics2D> f) {
        Spr s = spr(64, 64, f);
        for (int i = 0; i < s.px.length; i++) s.px[i] &= 0xFFFFFF;
        return s.px;
    }

    static Color c(int rgb) { return new Color(rgb); }

    static void rect(Graphics2D g, int col, int x, int y, int w, int h) { g.setColor(c(col)); g.fillRect(x, y, w, h); }

    static {
        TEX[1] = tex(g -> { // sala: papel rosa con rayas y corazones
            rect(g, 0xF8C8DC, 0, 0, 64, 64);
            for (int i = 0; i < 4; i++) rect(g, 0xFFDDEA, i * 16 + 4, 0, 8, 48);
            g.setColor(c(0xE57399));
            for (int i = 0; i < 4; i++) for (int j = 0; j < 2; j++) {
                int cx = i * 16 + 8, cy = 14 + j * 18;
                g.fill(new Ellipse2D.Double(cx - 4, cy - 3, 5, 5)); g.fill(new Ellipse2D.Double(cx, cy - 3, 5, 5));
                g.fillPolygon(new int[]{cx - 4, cx + 5, cx}, new int[]{cy, cy, cy + 6}, 3);
            }
            rect(g, 0xE79BB8, 0, 48, 64, 16); rect(g, 0xFFFFFF, 0, 47, 64, 3);
            g.setColor(c(0xD4789B)); for (int i = 0; i < 64; i += 16) g.drawLine(i, 50, i, 63);
        });
        TEX[2] = tex(g -> { // cocina: azulejos amarillos
            for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) rect(g, ((x + y) & 1) == 0 ? 0xFFF3B0 : 0xFFFFFF, x * 16, y * 16, 16, 16);
            g.setColor(c(0xE6D690)); for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
            g.setColor(c(0xFFFFFF)); g.drawLine(2, 2, 12, 2);
        });
        TEX[3] = tex(g -> { // bano: azulejos celestes con burbujas
            rect(g, 0x8FE3F0, 0, 0, 64, 64);
            g.setColor(c(0x5BC0D6)); for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
            g.setStroke(new BasicStroke(1.6f)); g.setColor(c(0xFFFFFF));
            int[][] b = {{10, 10, 6}, {40, 22, 9}, {22, 44, 5}, {50, 52, 7}, {6, 34, 4}};
            for (int[] q : b) g.draw(new Ellipse2D.Double(q[0] - q[2], q[1] - q[2], q[2] * 2, q[2] * 2));
        });
        TEX[4] = tex(g -> { // dormitorio: cielo nocturno con estrellas
            g.setPaint(new GradientPaint(0, 0, c(0x4A3FA5), 0, 64, c(0x2B2470))); g.fillRect(0, 0, 64, 64);
            g.setColor(c(0xFFE066));
            int[][] s = {{8, 8}, {30, 14}, {52, 6}, {18, 30}, {44, 34}, {10, 52}, {36, 52}, {56, 48}};
            for (int[] q : s) { g.fillRect(q[0] - 1, q[1] - 3, 2, 6); g.fillRect(q[0] - 3, q[1] - 1, 6, 2); }
            g.setColor(c(0xFFF3B0)); g.fill(new Ellipse2D.Double(40, 16, 14, 14));
            g.setColor(c(0x3A2F94)); g.fill(new Ellipse2D.Double(45, 14, 13, 13));
            rect(g, 0x1E1850, 0, 56, 64, 8);
        });
        TEX[5] = tex(g -> { // pasillo: madera clara
            rect(g, 0xFFF1D6, 0, 0, 64, 64);
            for (int i = 0; i < 64; i += 8) rect(g, 0xE9CFA3, i, 0, 1, 64);
            rect(g, 0xD9B98A, 0, 30, 64, 4); rect(g, 0xC9A06A, 0, 56, 64, 8);
        });
        TEX[6] = tex(g -> { // sillon
            rect(g, 0xD94C6B, 0, 0, 64, 64);
            for (int i = 0; i < 3; i++) { g.setColor(c(0xEC6A88)); g.fill(new RoundRectangle2D.Double(i * 21 + 2, 6, 18, 40, 10, 10)); }
            rect(g, 0xA93550, 0, 48, 64, 16); rect(g, 0x7A2238, 0, 62, 64, 2);
        });
        TEX[7] = tex(g -> { // madera / mesada
            rect(g, 0xC8894F, 0, 0, 64, 64);
            g.setColor(c(0xAE7339)); for (int i = 6; i < 64; i += 10) g.drawLine(0, i, 63, i + 2);
            rect(g, 0xF2C48A, 0, 0, 64, 6); rect(g, 0x8F5A2B, 0, 58, 64, 6);
        });
        TEX[8] = tex(g -> { // cama
            rect(g, 0x5B8DEF, 0, 0, 64, 64);
            rect(g, 0xFFFFFF, 4, 4, 56, 16); rect(g, 0xDDE8FF, 4, 18, 56, 3);
            g.setColor(c(0x3C6BD0)); for (int i = 28; i < 64; i += 12) g.fillRect(0, i, 64, 3);
        });
        TEX[9] = tex(g -> { // porcelana
            rect(g, 0xF4FBFF, 0, 0, 64, 64);
            rect(g, 0xCDEBFA, 0, 40, 64, 24); rect(g, 0xFFFFFF, 6, 8, 52, 6);
            rect(g, 0x9AD4EE, 0, 62, 64, 2);
        });
        TEX[10] = tex(g -> { // puerta
            rect(g, 0xFFF1D6, 0, 0, 64, 64); rect(g, 0x9C6B3C, 6, 0, 52, 64);
            rect(g, 0xB98250, 11, 6, 20, 24); rect(g, 0xB98250, 33, 6, 20, 24);
            rect(g, 0xB98250, 11, 34, 20, 24); rect(g, 0xB98250, 33, 34, 20, 24);
            g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(46, 30, 7, 7));
        });
        TEX[11] = tex(g -> { // puerta con candado
            rect(g, 0xFFF1D6, 0, 0, 64, 64); rect(g, 0xE5B21F, 6, 0, 52, 64);
            rect(g, 0xF7D250, 11, 6, 42, 52);
            g.setColor(c(0x7A5B00)); g.fill(new Ellipse2D.Double(26, 22, 12, 12)); g.fillRect(30, 30, 4, 14);
            rect(g, 0xB8860B, 22, 36, 20, 4);
        });
        TEX[12] = tex(g -> { // ducha apagada
            rect(g, 0xA9B8C2, 0, 0, 64, 64);
            g.setColor(c(0x8A99A3)); for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
            g.setColor(c(0x6B7A84)); g.fill(new Arc2D.Double(14, 8, 36, 24, 0, 180, Arc2D.CHORD)); g.fillRect(30, 28, 4, 10);
        });
        TEX[13] = tex(g -> { // ducha encendida
            rect(g, 0x7BE0F5, 0, 0, 64, 64);
            g.setColor(c(0x4CC3DB)); for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
            g.setColor(c(0xFFFFFF)); g.fill(new Arc2D.Double(14, 6, 36, 24, 0, 180, Arc2D.CHORD));
            g.setColor(c(0xFFE066)); for (int i = 0; i < 7; i++) g.fillRect(16 + i * 5, 34 + (i * 7) % 12, 3, 8);
            g.setColor(c(0xFFFFFF)); for (int i = 0; i < 5; i++) g.fill(new Ellipse2D.Double(10 + i * 10, 46 + (i * 5) % 8, 6, 6));
        });

        PISO[1] = tex(g -> { // madera
            rect(g, 0xD9A066, 0, 0, 64, 64);
            g.setColor(c(0xB9814A)); for (int i = 0; i < 64; i += 16) g.drawLine(0, i, 63, i);
            for (int i = 0; i < 4; i++) g.drawLine((i * 23 + 9) % 64, i * 16, (i * 23 + 9) % 64, i * 16 + 15);
        });
        PISO[2] = tex(g -> { for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) rect(g, ((x + y) & 1) == 0 ? 0xFFFFFF : 0xFFE082, x * 32, y * 32, 32, 32); });
        PISO[3] = tex(g -> { for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) rect(g, ((x + y) & 1) == 0 ? 0xB2EBF2 : 0x80DEEA, x * 32, y * 32, 32, 32); });
        PISO[4] = tex(g -> {
            rect(g, 0x7E57C2, 0, 0, 64, 64); g.setColor(c(0x9575CD));
            for (int i = 0; i < 8; i++) g.fillRect((i * 29) % 60, (i * 17) % 60, 3, 3);
        });
        PISO[5] = tex(g -> { for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) rect(g, ((x + y) & 1) == 0 ? 0xFFF1D6 : 0xF8C8DC, x * 32, y * 32, 32, 32); });
        TECHOS[0] = tex(g -> {
            rect(g, 0xEDE7FF, 0, 0, 64, 64); g.setColor(c(0xE2D9FA));
            g.fillRect(8, 8, 48, 48);
        });

        MUG_L = spr(64, 64, g -> dGermen(g, 0, 0, true, false));
        TIR_L = spr(64, 64, g -> dGermen(g, 1, 0, true, false));
        REY_L = spr(96, 96, g -> { g.scale(1.5, 1.5); dGermen(g, 2, 0, true, false); });
        for (int f = 0; f < 2; f++) {
            final int ff = f;
            MUG[f] = spr(64, 64, g -> dGermen(g, 0, ff, false, false));
            TIR[f] = spr(64, 64, g -> dGermen(g, 1, ff, false, false));
            REYS[f] = spr(96, 96, g -> { g.scale(1.5, 1.5); dGermen(g, 2, ff, false, false); });
        }
        TIR_A = spr(64, 64, g -> dGermen(g, 1, 0, false, true));

        SP_JAB = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(8, 50, 48, 9));
            g.setColor(c(0xFF8FB1)); g.fill(new RoundRectangle2D.Double(8, 22, 48, 30, 14, 14));
            g.setColor(c(0xFFC1D5)); g.fill(new RoundRectangle2D.Double(12, 25, 34, 8, 8, 8));
            g.setColor(c(0xE0577F)); g.setStroke(new BasicStroke(2f)); g.draw(new RoundRectangle2D.Double(8, 22, 48, 30, 14, 14));
            g.setColor(new Color(190, 235, 255, 210));
            g.fill(new Ellipse2D.Double(36, 6, 12, 12)); g.fill(new Ellipse2D.Double(18, 10, 8, 8)); g.fill(new Ellipse2D.Double(48, 18, 7, 7));
            g.setColor(Color.WHITE); g.draw(new Ellipse2D.Double(36, 6, 12, 12));
        });
        SP_BUR = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(14, 54, 36, 8));
            g.setColor(c(0x4FC3F7)); g.fill(new RoundRectangle2D.Double(16, 24, 32, 34, 12, 12));
            g.setColor(c(0xFFE082)); g.fill(new RoundRectangle2D.Double(22, 12, 20, 14, 5, 5));
            g.setColor(Color.WHITE); g.fill(new RoundRectangle2D.Double(20, 34, 24, 14, 6, 6));
            g.setColor(c(0x0288D1)); g.setStroke(new BasicStroke(2f)); g.draw(new RoundRectangle2D.Double(16, 24, 32, 34, 12, 12));
            g.setColor(new Color(190, 235, 255, 220)); g.fill(new Ellipse2D.Double(40, 2, 10, 10)); g.fill(new Ellipse2D.Double(12, 6, 7, 7));
        });
        SP_EST = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 40)); g.fill(new Ellipse2D.Double(14, 54, 36, 7));
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double r = (i % 2 == 0) ? 26 : 11, a = -Math.PI / 2 + i * Math.PI / 5;
                double x = 32 + Math.cos(a) * r, y = 32 + Math.sin(a) * r;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            g.setColor(c(0xFFD54F)); g.fill(p); g.setColor(c(0xF29D00)); g.setStroke(new BasicStroke(3f)); g.draw(p);
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(26, 20, 6, 6));
        });
        SP_LLA = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 40)); g.fill(new Ellipse2D.Double(10, 54, 44, 7));
            g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(8, 12, 26, 26));
            g.setColor(c(0x00000000, true)); g.setComposite(AlphaComposite.Clear); g.fill(new Ellipse2D.Double(15, 19, 12, 12));
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(c(0xFFD54F)); g.fillRect(30, 22, 28, 7); g.fillRect(46, 28, 6, 12); g.fillRect(54, 28, 5, 9);
            g.setColor(c(0xB8860B)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(8, 12, 26, 26));
        });
        SP_BALA = spr(32, 32, g -> {
            g.setColor(new Color(150, 220, 255, 120)); g.fill(new Ellipse2D.Double(2, 2, 28, 28));
            g.setColor(new Color(255, 255, 255, 230)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(2, 2, 28, 28));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(8, 7, 8, 6));
        });
        SP_MUGRE = spr(32, 32, g -> {
            g.setColor(c(0x6D4C41)); for (int i = 0; i < 9; i++) { double a = i * Math.PI * 2 / 9; g.fill(new Ellipse2D.Double(16 + Math.cos(a) * 10 - 4, 16 + Math.sin(a) * 10 - 4, 8, 8)); }
            g.fill(new Ellipse2D.Double(5, 5, 22, 22));
            g.setColor(c(0x8D6E63)); g.fill(new Ellipse2D.Double(10, 9, 7, 6));
            g.setColor(c(0xFFD54F)); g.fillRect(20, 18, 3, 3);
        });
        for (int f = 0; f < 3; f++) {
            final int ff = f;
            POPS[f] = spr(64, 64, g -> {
                double r = 10 + ff * 9;
                g.setColor(new Color(190, 235, 255, 200 - ff * 50)); g.setStroke(new BasicStroke(3f - ff * 0.6f));
                g.draw(new Ellipse2D.Double(32 - r, 32 - r, r * 2, r * 2));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4 + 0.3, d = r + 4 + ff * 3;
                    g.setColor(i % 2 == 0 ? c(0xFFE066) : Color.WHITE);
                    g.fill(new Ellipse2D.Double(32 + Math.cos(a) * d - 3, 32 + Math.sin(a) * d - 3, 6 - ff, 6 - ff));
                }
            });
        }
    }

    static Color c(int rgb, boolean ignorar) { return new Color(0, 0, 0, 0); }

    /** Germen simpatico (nada de sangre: al "limpiarlo" se pone feliz dentro de una burbuja). */
    static void dGermen(Graphics2D g, int tipo, int frame, boolean limpio, boolean ataque) {
        AffineTransform base = g.getTransform();
        double sq = frame == 0 ? 1.0 : 0.93, sw = frame == 0 ? 1.0 : 1.05;
        g.translate(32, 60); g.scale(sw, sq); g.translate(-32, -60);
        Color col, dark;
        if (limpio) { col = c(0xB3E5FC); dark = c(0x81D4FA); }
        else if (tipo == 1) { col = c(0x9C6BD6); dark = c(0x7345B0); }
        else if (tipo == 2) { col = c(0x8A6340); dark = c(0x5D4037); }
        else { col = c(0x7CB342); dark = c(0x558B2F); }
        g.setColor(new Color(0, 0, 0, 50)); g.fill(new Ellipse2D.Double(10, 55, 44, 8));
        g.setColor(dark); g.fill(new Ellipse2D.Double(14, 54, 16, 9)); g.fill(new Ellipse2D.Double(34, 54, 16, 9));
        g.setColor(col);
        if (tipo != 1) for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            g.fill(new Ellipse2D.Double(32 + Math.cos(a) * 21 - 5, 38 + Math.sin(a) * 19 - 5, 10, 10));
        }
        g.fill(new Ellipse2D.Double(11, 19, 42, 38));
        g.setColor(dark);
        g.fill(new Ellipse2D.Double(18, 46, 6, 5)); g.fill(new Ellipse2D.Double(42, 44, 7, 6)); g.fill(new Ellipse2D.Double(30, 20, 5, 4));
        if (tipo == 1) {
            g.setStroke(new BasicStroke(2.5f)); g.drawLine(24, 22, 18, 9); g.drawLine(40, 22, 46, 9);
            g.fill(new Ellipse2D.Double(14, 4, 9, 9)); g.fill(new Ellipse2D.Double(41, 4, 9, 9));
        }
        for (int e = 0; e < 2; e++) {
            double cx = e == 0 ? 24 : 40;
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 7, 26, 14, 16));
            g.setColor(c(0x1F1A12)); g.fill(new Ellipse2D.Double(cx - 3 + (e == 0 ? 1 : -1), 31, 7, 8));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 2 + (e == 0 ? 1 : -1), 32, 2.5, 2.5));
        }
        g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (!limpio) { g.drawLine(15, 25, 29, 30); g.drawLine(49, 25, 35, 30); }
        if (limpio) g.draw(new Arc2D.Double(24, 42, 16, 10, 200, 140, Arc2D.OPEN));
        else if (ataque) g.fill(new Ellipse2D.Double(28, 44, 9, 9));
        else {
            Path2D z = new Path2D.Double(); z.moveTo(24, 48); z.lineTo(28, 44); z.lineTo(32, 48); z.lineTo(36, 44); z.lineTo(40, 48);
            g.setStroke(new BasicStroke(2.2f)); g.draw(z);
        }
        if (tipo == 2) {
            Path2D k = new Path2D.Double();
            k.moveTo(18, 22); k.lineTo(19, 6); k.lineTo(26, 15); k.lineTo(32, 3); k.lineTo(38, 15); k.lineTo(45, 6); k.lineTo(46, 22); k.closePath();
            g.setColor(c(0xFFD54F)); g.fill(k); g.setColor(c(0xB8860B)); g.setStroke(new BasicStroke(2f)); g.draw(k);
            g.setColor(c(0xE53935)); g.fill(new Ellipse2D.Double(29, 13, 6, 6));
            g.setColor(c(0x42A5F5)); g.fill(new Ellipse2D.Double(20, 15, 4, 4)); g.fill(new Ellipse2D.Double(40, 15, 4, 4));
        }
        if (limpio) {
            g.setColor(new Color(150, 220, 255, 80)); g.fill(new Ellipse2D.Double(1, 1, 62, 62));
            g.setColor(new Color(255, 255, 255, 230)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(1, 1, 62, 62));
            g.fill(new Ellipse2D.Double(10, 8, 9, 6));
        }
        g.setTransform(base);
    }

    // ================================================================== texturas nuevas (capitulos 2 a 5)

    static void ladrillos(Graphics2D g, int base, int junta, int alto, int ancho) {
        rect(g, junta, 0, 0, 64, 64);
        for (int y = 0, f = 0; y < 64; y += alto, f++)
            for (int x = -(f % 2) * ancho / 2; x < 64; x += ancho) {
                int v = ((x * 7 + y * 13) & 15) - 8;
                g.setColor(c(mezcla(base, v * 2)));
                g.fillRect(x + 1, y + 1, ancho - 2, alto - 2);
            }
    }

    static int mezcla(int col, int d) {
        int r = Math.max(0, Math.min(255, ((col >> 16) & 255) + d)), gg = Math.max(0, Math.min(255, ((col >> 8) & 255) + d)),
                b = Math.max(0, Math.min(255, (col & 255) + d));
        return (r << 16) | (gg << 8) | b;
    }

    static void tablones(Graphics2D g, int base, int linea, int ancho) {
        rect(g, base, 0, 0, 64, 64);
        g.setColor(c(linea));
        for (int x = 0; x < 64; x += ancho) g.fillRect(x, 0, 1, 64);
        for (int i = 0; i < 12; i++) g.fillRect((i * 37 + 5) % 60, (i * 23) % 60, 3, 1);
    }

    static void tuerca(Graphics2D g, int x, int y, int col) { g.setColor(c(col)); g.fill(new Ellipse2D.Double(x - 2, y - 2, 5, 5)); }

    static void puertaBase(Graphics2D g, int marco, int hoja, int panel) {
        rect(g, 0xFFF1D6, 0, 0, 64, 64); rect(g, marco, 6, 0, 52, 64); rect(g, hoja, 11, 6, 42, 52);
        rect(g, panel, 15, 10, 34, 44);
    }

    static void candado(Graphics2D g, int col, int cx, int cy) {
        g.setColor(c(col)); g.setStroke(new BasicStroke(3f)); g.draw(new Arc2D.Double(cx - 6, cy - 12, 12, 14, 0, 180, Arc2D.OPEN));
        g.fill(new RoundRectangle2D.Double(cx - 9, cy - 2, 18, 14, 4, 4));
        g.setColor(c(0x333333)); g.fill(new Ellipse2D.Double(cx - 2, cy + 2, 4, 4)); g.fillRect(cx - 1, cy + 5, 2, 4);
    }

    static {
        TEX[T_ESCARCHA] = tex(g -> {
            rect(g, 0xCDEEFF, 0, 0, 64, 64);
            g.setColor(c(0x9BD3F0)); g.drawLine(0, 32, 63, 32); g.drawLine(32, 0, 32, 63);
            g.setColor(c(0xFFFFFF));
            for (int i = 0; i < 8; i++) { int x = i * 9 + 2; g.fillPolygon(new int[]{x, x + 5, x + 9}, new int[]{0, 12 + (i * 5) % 9, 0}, 3); }
            for (int i = 0; i < 7; i++) { int x = i * 10 + 4; g.fillPolygon(new int[]{x, x + 5, x + 10}, new int[]{63, 50 - (i * 7) % 10, 63}, 3); }
            g.setStroke(new BasicStroke(1.6f)); g.setColor(c(0x7EC8EE));
            for (int[] s : new int[][]{{18, 22}, {46, 44}}) for (int k = 0; k < 3; k++) {
                double a = k * Math.PI / 3; g.draw(new Line2D.Double(s[0] - Math.cos(a) * 7, s[1] - Math.sin(a) * 7, s[0] + Math.cos(a) * 7, s[1] + Math.sin(a) * 7));
            }
        });
        TEX[T_DESPENSA] = tex(g -> {
            rect(g, 0x7A4E26, 0, 0, 64, 64);
            for (int s = 0; s < 3; s++) {
                int y = 20 + s * 21;
                rect(g, 0x4E3118, 0, y, 64, 4); rect(g, 0x946236, 0, y, 64, 1);
            }
            int[] cl = {0xE57373, 0x81C784, 0x64B5F6, 0xFFD54F, 0xBA68C8};
            for (int i = 0; i < 5; i++) { rect(g, cl[i], 4 + i * 12, 6, 9, 14); rect(g, 0xFFFFFF, 4 + i * 12, 11, 9, 3); }
            for (int i = 0; i < 4; i++) { rect(g, cl[(i + 2) % 5], 6 + i * 15, 28, 10, 13); rect(g, 0xCFD8DC, 6 + i * 15, 26, 10, 3); }
            for (int i = 0; i < 3; i++) { g.setColor(c(0xFFE0B2)); g.fill(new RoundRectangle2D.Double(5 + i * 20, 47, 14, 15, 6, 6)); rect(g, 0xBF360C, 7 + i * 20, 45, 10, 4); }
        });
        TEX[T_CANDADO_PLATA] = tex(g -> {
            rect(g, 0xFFF1D6, 0, 0, 64, 64); rect(g, 0x90A4AE, 4, 0, 56, 64); rect(g, 0xECEFF1, 8, 4, 48, 56);
            g.setColor(c(0xB0BEC5)); g.fillRect(8, 30, 48, 2);
            rect(g, 0x546E7A, 46, 10, 5, 18); rect(g, 0x546E7A, 46, 36, 5, 18);
            g.setColor(c(0xFFFFFF)); g.fillRect(10, 6, 30, 3);
            candado(g, 0xFFD54F, 28, 36);
            g.setColor(c(0xBBDEFB)); g.fillPolygon(new int[]{8, 18, 8}, new int[]{4, 4, 14}, 3);
        });
        TEX[T_ALACENA_OFF] = tex(g -> {
            rect(g, 0xF4E4C1, 0, 0, 64, 64); rect(g, 0xA9733E, 4, 2, 27, 60); rect(g, 0xA9733E, 33, 2, 27, 60);
            rect(g, 0xC48E55, 8, 6, 19, 24); rect(g, 0xC48E55, 37, 6, 19, 24); rect(g, 0xC48E55, 8, 34, 19, 24); rect(g, 0xC48E55, 37, 34, 19, 24);
            g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(24, 30, 5, 5)); g.fill(new Ellipse2D.Double(35, 30, 5, 5));
        });
        TEX[T_ALACENA_ON] = tex(g -> {
            rect(g, 0xF4E4C1, 0, 0, 64, 64); rect(g, 0x1B1208, 10, 2, 44, 60);
            for (int i = 0; i < 6; i++) rect(g, mezcla(0x6D4C41, i * 12), 12 + i * 3, 56 - i * 9, 40 - i * 6, 7);
            rect(g, 0xA9733E, 0, 2, 9, 60); rect(g, 0xA9733E, 55, 2, 9, 60);
            g.setColor(c(0xFFE066)); g.fill(new Ellipse2D.Double(28, 6, 8, 8));
        });
        for (int on = 0; on < 2; on++) {
            final boolean enc = on == 0;
            TEX[enc ? T_HORN_ON : T_HORN_OFF] = tex(g -> {
                rect(g, 0x37474F, 0, 0, 64, 64);
                g.setColor(c(0x263238)); for (int i = 0; i < 64; i += 16) g.drawLine(i, 0, i, 63);
                if (enc) { g.setColor(new Color(255, 152, 0, 90)); g.fill(new Ellipse2D.Double(4, 10, 56, 48)); }
                g.setColor(c(enc ? 0xFF5722 : 0x455A64)); g.fill(new Ellipse2D.Double(14, 20, 36, 28));
                g.setColor(c(enc ? 0xFF9800 : 0x37474F)); g.fill(new Ellipse2D.Double(20, 25, 24, 18));
                g.setColor(c(enc ? 0xFFEB3B : 0x263238)); g.fill(new Ellipse2D.Double(27, 30, 10, 8));
                g.setColor(c(0x1C1C1C)); g.setStroke(new BasicStroke(2f));
                for (int i = 0; i < 4; i++) g.drawLine(12 + i * 14, 14, 12 + i * 14, 54);
                g.drawLine(10, 34, 54, 34);
            });
        }
        TEX[T_HORN_DIN] = TEX[T_HORN_ON];
        TEX[T_HELADERITA] = tex(g -> {
            rect(g, 0xF4E4C1, 0, 0, 64, 64); rect(g, 0xF5F5F5, 10, 3, 44, 60); rect(g, 0xCFD8DC, 10, 3, 44, 3);
            rect(g, 0xB0BEC5, 10, 24, 44, 2); rect(g, 0x78909C, 46, 10, 3, 10); rect(g, 0x78909C, 46, 30, 3, 14);
            g.setColor(c(0xEF5350)); g.fill(new Ellipse2D.Double(16, 32, 8, 8)); g.setColor(c(0x42A5F5)); g.fillRect(26, 34, 8, 8);
        });
        TEX[T_BAUL] = tex(g -> {
            tablones(g, 0x8D6E63, 0x6D4C41, 10);
            rect(g, 0x4E342E, 0, 0, 64, 6); rect(g, 0x4E342E, 0, 58, 64, 6); rect(g, 0x4E342E, 6, 0, 6, 64); rect(g, 0x4E342E, 52, 0, 6, 64);
            rect(g, 0xFFC107, 26, 26, 12, 14); rect(g, 0x5D4037, 30, 30, 4, 6);
            tuerca(g, 9, 10, 0xFFC107); tuerca(g, 55, 10, 0xFFC107); tuerca(g, 9, 54, 0xFFC107); tuerca(g, 55, 54, 0xFFC107);
        });
        TEX[T_DESVAN] = tex(g -> {
            tablones(g, 0x4E342E, 0x33221B, 8);
            g.setColor(c(0x6D5446)); for (int i = 0; i < 8; i++) g.fillRect(i * 8 + 2, 10 + (i * 11) % 40, 2, 10);
            rect(g, 0x2E1F18, 0, 56, 64, 8);
        });
        TEX[T_TELA] = tex(g -> {
            tablones(g, 0x3E2B22, 0x2A1D17, 16);
            g.setStroke(new BasicStroke(1.4f)); g.setColor(c(0xECECF5));
            for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; g.draw(new Line2D.Double(32, 32, 32 + Math.cos(a) * 34, 32 + Math.sin(a) * 34)); }
            for (int r = 7; r <= 31; r += 8) {
                Path2D p = new Path2D.Double();
                for (int i = 0; i <= 8; i++) {
                    double a = i * Math.PI / 4, rr = r * (i % 2 == 0 ? 1.0 : 0.82);
                    double x = 32 + Math.cos(a) * rr, y = 32 + Math.sin(a) * rr;
                    if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                g.draw(p);
            }
        });
        TEX[T_BIBLIO] = tex(g -> {
            rect(g, 0x4A2F1B, 0, 0, 64, 64);
            int[] cl = {0xC62828, 0x1565C0, 0x2E7D32, 0xF9A825, 0x6A1B9A, 0xEF6C00, 0x00838F};
            for (int s = 0; s < 3; s++) {
                int y = s * 21 + 3, x = 2;
                for (int i = 0; x < 62; i++) {
                    int w = 4 + (i * 3 + s) % 4, h = 12 + (i * 5 + s * 3) % 6;
                    rect(g, cl[(i + s * 2) % 7], x, y + 17 - h, w, h);
                    rect(g, 0xFFFFFF, x + 1, y + 20 - h - 3 + 6, w - 2, 1);
                    x += w + 1;
                }
                rect(g, 0x2E1B0E, 0, y + 17, 64, 4);
            }
        });
        TEX[T_DISFRAZ] = tex(g -> {
            rect(g, 0x6D5A7A, 0, 0, 64, 64); rect(g, 0x57476A, 0, 56, 64, 8);
            rect(g, 0x3E2D4D, 0, 12, 64, 3);
            g.setColor(c(0xFFD54F)); g.fillPolygon(new int[]{10, 18, 26}, new int[]{40, 18, 40}, 3);
            g.setColor(c(0xE57373)); g.fill(new Ellipse2D.Double(15, 14, 6, 6));
            rect(g, 0x212121, 36, 22, 16, 16); rect(g, 0x212121, 32, 38, 24, 4); rect(g, 0xC62828, 36, 33, 16, 3);
        });
        TEX[T_COSTURA] = tex(g -> {
            rect(g, 0xC98BA1, 0, 0, 64, 64);
            g.setColor(c(0xE8B4C8)); for (int i = 0; i < 64; i += 8) g.fillRect(i, 0, 4, 64);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4, 3}, 0)); g.setColor(c(0xFFFFFF));
            g.drawRect(3, 3, 57, 57);
            int[] cl = {0xE53935, 0x1E88E5, 0xFDD835};
            for (int i = 0; i < 3; i++) {
                rect(g, 0xD7B98A, 8 + i * 18, 22, 12, 3); rect(g, 0xD7B98A, 8 + i * 18, 38, 12, 3); rect(g, cl[i], 9 + i * 18, 25, 10, 13);
            }
        });
        TEX[T_CANDADO_HILO] = tex(g -> {
            puertaBase(g, 0x6A1B9A, 0x8E24AA, 0xAB47BC);
            g.setStroke(new BasicStroke(2f)); g.setColor(c(0xF3E5F5));
            for (int i = 0; i < 6; i++) g.draw(new Line2D.Double(16, 14 + i * 7, 48, 22 + i * 5));
            rect(g, 0xD7B98A, 24, 6, 16, 4); rect(g, 0xD7B98A, 24, 54, 16, 4);
            candado(g, 0xFFD54F, 32, 32);
        });
        TEX[T_TRAMP_OFF] = tex(g -> {
            tablones(g, 0x5D4037, 0x3E2723, 12);
            rect(g, 0x3E2723, 8, 8, 48, 48); rect(g, 0x6D4C41, 11, 11, 42, 42);
            g.setColor(c(0x9E9E9E)); g.setStroke(new BasicStroke(4f)); g.draw(new Ellipse2D.Double(24, 24, 16, 16));
        });
        TEX[T_TRAMP_ON] = tex(g -> {
            tablones(g, 0x5D4037, 0x3E2723, 12);
            rect(g, 0x0E1A2B, 8, 8, 48, 48);
            g.setPaint(new GradientPaint(0, 8, c(0x4FC3F7), 0, 56, c(0x0E1A2B))); g.fillRect(8, 8, 48, 48);
            g.setColor(c(0x8D6E63)); for (int i = 0; i < 4; i++) g.fillRect(14, 14 + i * 10, 36, 4);
            rect(g, 0x6D4C41, 12, 8, 4, 48); rect(g, 0x6D4C41, 48, 8, 4, 48);
        });
        TEX[T_LADRILLO] = tex(g -> {
            ladrillos(g, 0x5E7F5A, 0x3E5A3B, 10, 21);
            g.setColor(new Color(124, 179, 66, 120)); for (int i = 0; i < 6; i++) g.fillRect(i * 11 + 3, 0, 3, 20 + (i * 17) % 36);
            rect(g, 0x2F4B3B, 0, 60, 64, 4);
        });
        TEX[T_LAVARROPAS] = tex(g -> {
            rect(g, 0xCFD8DC, 0, 0, 64, 64); rect(g, 0xECEFF1, 3, 3, 58, 58);
            rect(g, 0x90A4AE, 3, 12, 58, 2);
            g.setColor(c(0xE53935)); g.fill(new Ellipse2D.Double(8, 5, 5, 5)); g.setColor(c(0x42A5F5)); g.fill(new Ellipse2D.Double(16, 5, 5, 5));
            g.setColor(c(0xB0BEC5)); g.fill(new Ellipse2D.Double(11, 17, 42, 42));
            g.setColor(c(0x64B5F6)); g.fill(new Ellipse2D.Double(15, 21, 34, 34));
            g.setColor(c(0xE57373)); g.fill(new Arc2D.Double(18, 24, 28, 28, 20, 150, Arc2D.PIE));
            g.setColor(c(0xFFF176)); g.fill(new Arc2D.Double(18, 24, 28, 28, 200, 110, Arc2D.PIE));
            g.setColor(new Color(255, 255, 255, 140)); g.fill(new Ellipse2D.Double(19, 25, 10, 8));
        });
        TEX[T_LAVAR_ABIERTO] = tex(g -> {
            rect(g, 0xCFD8DC, 0, 0, 64, 64); rect(g, 0xECEFF1, 3, 3, 58, 58); rect(g, 0x90A4AE, 3, 12, 58, 2);
            g.setColor(c(0xB0BEC5)); g.fill(new Ellipse2D.Double(11, 17, 42, 42));
            g.setColor(c(0x1B2A33)); g.fill(new Ellipse2D.Double(15, 21, 34, 34));
            g.setColor(c(0xECEFF1)); g.fill(new Ellipse2D.Double(4, 20, 14, 36));
            g.setColor(c(0xFFB74D)); g.fill(new Ellipse2D.Double(30, 38, 10, 8));
        });
        TEX[T_ROPA] = tex(g -> {
            rect(g, 0x78909C, 0, 0, 64, 64); rect(g, 0x546E7A, 0, 5, 64, 2);
            int[] cl = {0xEF5350, 0xFFEE58, 0x66BB6A, 0x42A5F5, 0xAB47BC, 0xFFA726};
            for (int i = 0; i < 6; i++) { rect(g, cl[i], 2 + i * 10, 8, 9, 38 + (i * 7) % 14); rect(g, 0xFFFFFF, 5 + i * 10, 6, 3, 5); }
            rect(g, 0x37474F, 0, 58, 64, 6);
        });
        TEX[T_TUBOS] = tex(g -> {
            rect(g, 0x455A64, 0, 0, 64, 64);
            int[] cl = {0xE53935, 0xFDD835, 0x1E88E5, 0x43A047};
            for (int i = 0; i < 4; i++) {
                int y = 2 + i * 16; rect(g, cl[i], 0, y, 64, 11); rect(g, mezcla(cl[i], 60), 0, y + 1, 64, 3); rect(g, mezcla(cl[i], -50), 0, y + 8, 64, 3);
                rect(g, 0x263238, 14 + i * 12, y - 1, 4, 13);
            }
        });
        TEX[T_COLUMNA] = tex(g -> {
            ladrillos(g, 0xA1553A, 0x6D3A28, 8, 16);
            rect(g, 0xC47A5B, 0, 0, 3, 64); rect(g, 0x5A2E20, 61, 0, 3, 64);
            rect(g, 0x7A4130, 0, 0, 64, 4); rect(g, 0x7A4130, 0, 60, 64, 4);
        });
        for (int on = 0; on < 2; on++) {
            final boolean hecha = on == 1;
            TEX[hecha ? T_VALV_ON : T_VALV_OFF] = tex(g -> {
                rect(g, 0x546E7A, 0, 0, 64, 64);
                g.setColor(c(0x455A64)); for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
                rect(g, 0x78909C, 28, 40, 8, 24);
                int col = hecha ? 0x43A047 : 0xE53935;
                g.setColor(c(col)); g.setStroke(new BasicStroke(5f)); g.draw(new Ellipse2D.Double(14, 10, 36, 36));
                g.setStroke(new BasicStroke(4f));
                double rot = hecha ? Math.PI / 4 : 0;
                for (int i = 0; i < 4; i++) { double a = rot + i * Math.PI / 2; g.draw(new Line2D.Double(32, 28, 32 + Math.cos(a) * 18, 28 + Math.sin(a) * 18)); }
                g.fill(new Ellipse2D.Double(27, 23, 10, 10));
                if (hecha) { g.setColor(c(0xFFFFFF)); g.setStroke(new BasicStroke(3f)); g.drawLine(24, 54, 29, 59); g.drawLine(29, 59, 40, 48); }
            });
        }
        TEX[T_CALDERA] = tex(g -> {
            rect(g, 0x4E5B63, 0, 0, 64, 64); rect(g, 0x37444B, 0, 0, 64, 6); rect(g, 0x37444B, 0, 58, 64, 6);
            g.setColor(c(0x6B7B84)); g.fill(new RoundRectangle2D.Double(8, 10, 48, 44, 14, 14));
            for (int i = 0; i < 6; i++) { tuerca(g, 12 + i * 8, 14, 0x9AA7AE); tuerca(g, 12 + i * 8, 50, 0x9AA7AE); }
            g.setColor(c(0xECEFF1)); g.fill(new Ellipse2D.Double(20, 20, 24, 24));
            g.setColor(c(0xE53935)); g.setStroke(new BasicStroke(2f)); g.drawLine(32, 32, 40, 25);
            g.setColor(c(0x263238)); g.draw(new Ellipse2D.Double(20, 20, 24, 24));
            g.setColor(c(0x8D6E63)); g.fillRect(0, 40, 64, 4);
        });
        TEX[T_CANDADO_LLAVE] = tex(g -> {
            puertaBase(g, 0x455A64, 0x78909C, 0x90A4AE);
            g.setColor(c(0x37474F)); g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(24, 44, 42, 20)); g.draw(new Arc2D.Double(36, 10, 14, 14, 200, 280, Arc2D.OPEN));
            g.draw(new Arc2D.Double(18, 40, 12, 12, 20, 280, Arc2D.OPEN));
            candado(g, 0xFFD54F, 32, 36);
        });
        TEX[T_DESAGUE_OFF] = tex(g -> {
            rect(g, 0x5E7F5A, 0, 0, 64, 64); rect(g, 0x263238, 8, 8, 48, 48);
            g.setColor(c(0x78909C)); for (int i = 0; i < 6; i++) g.fillRect(12 + i * 8, 8, 3, 48);
            rect(g, 0x546E7A, 8, 8, 48, 4); rect(g, 0x546E7A, 8, 52, 48, 4);
        });
        TEX[T_DESAGUE_ON] = tex(g -> {
            rect(g, 0x5E7F5A, 0, 0, 64, 64); rect(g, 0x0B2A3A, 8, 8, 48, 48);
            g.setStroke(new BasicStroke(3f)); 
            for (int i = 0; i < 5; i++) { g.setColor(new Color(79 + i * 25, 195, 247, 255)); g.draw(new Arc2D.Double(32 - 20 + i * 4, 32 - 20 + i * 4, 40 - i * 8, 40 - i * 8, i * 60, 270, Arc2D.OPEN)); }
            g.setColor(c(0xFFFFFF)); g.fill(new Ellipse2D.Double(28, 28, 8, 8));
        });
        TEX[T_CANO_SUELTO] = tex(g -> {
            rect(g, 0x455A64, 0, 0, 64, 64);
            rect(g, 0x78909C, 0, 8, 64, 11); rect(g, 0xB0BEC5, 0, 9, 64, 3);
            rect(g, 0x78909C, 0, 44, 64, 11);
            g.setColor(c(0x90A4AE)); g.fill(new RoundRectangle2D.Double(26, 14, 12, 40, 4, 4)); g.setColor(c(0xCFD8DC)); g.fillRect(28, 16, 3, 36);
            g.setColor(c(0x546E7A)); g.fillRect(22, 30, 20, 4);
        });
        TEX[T_OXIDO] = tex(g -> {
            rect(g, 0x8D5B3A, 0, 0, 64, 64);
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
                g.setColor(c(((x + y) & 1) == 0 ? 0x9A6743 : 0x7E5033)); g.fillRect(x * 32 + 1, y * 32 + 1, 30, 30);
            }
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) { tuerca(g, x * 32 + 5, y * 32 + 5, 0x4E342E); tuerca(g, x * 32 + 27, y * 32 + 27, 0x4E342E); }
            g.setColor(new Color(60, 90, 100, 130)); for (int i = 0; i < 5; i++) g.fillRect(i * 13 + 4, 0, 3, 18 + (i * 13) % 40);
        });
        TEX[T_REJA] = tex(g -> {
            rect(g, 0x1B2A33, 0, 0, 64, 64);
            g.setColor(c(0x8D5B3A)); for (int i = 0; i < 7; i++) g.fillRect(4 + i * 9, 0, 5, 64);
            rect(g, 0x6B4226, 0, 12, 64, 5); rect(g, 0x6B4226, 0, 46, 64, 5);
            g.setColor(c(0xB5835A)); for (int i = 0; i < 7; i++) g.fillRect(5 + i * 9, 0, 1, 64);
        });
        TEX[T_ESPUMA] = tex(g -> {
            rect(g, 0xE1F5FE, 0, 0, 64, 64);
            g.setStroke(new BasicStroke(1.5f));
            int[][] b = {{10, 12, 8}, {30, 8, 6}, {50, 16, 9}, {18, 32, 10}, {44, 38, 8}, {8, 52, 7}, {30, 54, 9}, {56, 56, 6}};
            for (int[] q : b) {
                g.setColor(new Color(255, 255, 255, 255)); g.fill(new Ellipse2D.Double(q[0] - q[2], q[1] - q[2], q[2] * 2, q[2] * 2));
                g.setColor(c(0x81D4FA)); g.draw(new Ellipse2D.Double(q[0] - q[2], q[1] - q[2], q[2] * 2, q[2] * 2));
                g.setColor(c(0xB3E5FC)); g.fill(new Ellipse2D.Double(q[0] - q[2] * 0.5, q[1] - q[2] * 0.6, q[2] * 0.5, q[2] * 0.4));
            }
        });
        for (int n = 0; n <= 3; n++) {
            final int llenas = n;
            TEX[T_P3_0 + n] = tex(g -> {
                rect(g, 0x37474F, 0, 0, 64, 64); rect(g, 0x5D4037, 4, 0, 56, 64); rect(g, 0x6D4C41, 8, 4, 48, 56);
                g.setColor(c(0x3E2723)); g.setStroke(new BasicStroke(2f)); g.drawRect(8, 4, 47, 55);
                for (int i = 0; i < 3; i++) {
                    int cy = 14 + i * 18;
                    g.setColor(c(0x212121)); g.fill(new Ellipse2D.Double(24, cy - 7, 16, 16));
                    if (i < llenas) {
                        g.setColor(new Color(255, 224, 102, 120)); g.fill(new Ellipse2D.Double(18, cy - 13, 28, 28));
                        g.setColor(c(0xFFD54F)); g.fillPolygon(new int[]{26, 28, 32, 36, 38, 38, 26}, new int[]{cy + 4, cy - 4, cy, cy - 4, cy + 4, cy + 6, cy + 6}, 7);
                    } else { g.setColor(c(0x5D4037)); g.fillRect(31, cy - 3, 2, 8); g.fill(new Ellipse2D.Double(29, cy - 4, 6, 6)); }
                }
            });
        }
        TEX[T_TRONO] = tex(g -> {
            rect(g, 0x4A3F4F, 0, 0, 64, 64);
            int[] cl = {0x6D4C41, 0x78909C, 0xA1887F, 0x607D8B, 0x8D6E63, 0xB0A090};
            for (int i = 0; i < 18; i++) {
                g.setColor(c(cl[i % 6])); g.fill(new RoundRectangle2D.Double((i * 19) % 56, (i * 13) % 56, 10 + (i * 3) % 12, 8 + (i * 5) % 10, 5, 5));
            }
            g.setColor(c(0x2B2230)); g.fillRect(0, 56, 64, 8);
        });
        TEX[T_TRONO_ATRAS] = tex(g -> {
            rect(g, 0x4A3F4F, 0, 0, 64, 64);
            int[] cl = {0x6D4C41, 0x78909C, 0xA1887F, 0x607D8B, 0x8D6E63, 0xB0A090};
            for (int i = 0; i < 18; i++) {
                g.setColor(c(cl[(i + 1) % 6])); g.fill(new RoundRectangle2D.Double((i * 17 + 3) % 56, (i * 11) % 56, 10 + (i * 3) % 12, 8 + (i * 5) % 10, 5, 5));
            }
            g.setColor(c(0x2B2230)); g.fillRect(0, 56, 64, 8);
        });
        for (int r = 0; r < 2; r++) {
            final boolean rec = r == 1;
            TEX[rec ? T_CADENA_REC : T_CADENA] = tex(g -> {
                rect(g, 0x455A64, 0, 0, 64, 64); rect(g, 0x37474F, 0, 56, 64, 8);
                int col = rec ? 0x90A4AE : 0xFFD54F;
                g.setColor(c(col)); g.setStroke(new BasicStroke(3.5f));
                for (int i = 0; i < 6; i++) g.draw(new RoundRectangle2D.Double(26 + (i % 2) * 5, 2 + i * 8, 12, 12, 6, 6));
                g.setColor(c(rec ? 0x607D8B : 0xE53935)); g.fill(new Ellipse2D.Double(22, 46, 20, 14));
                g.setColor(c(0xFFFFFF)); g.fill(new Ellipse2D.Double(26, 48, 6, 4));
            });
        }
        TEX[T_CANO_ROTO] = tex(g -> {
            rect(g, 0x455A64, 0, 0, 64, 64);
            rect(g, 0x78909C, 0, 6, 64, 14); rect(g, 0xB0BEC5, 0, 8, 64, 3);
            rect(g, 0x78909C, 0, 40, 64, 14);
            g.setColor(c(0x263238)); g.fill(new Polygon(new int[]{20, 44, 34, 40, 24, 28}, new int[]{6, 6, 14, 20, 20, 12}, 6));
            g.setColor(c(0x4FC3F7)); g.fillRect(30, 20, 5, 22);
            g.setColor(c(0x81D4FA)); g.fill(new Ellipse2D.Double(24, 42, 18, 6));
        });
        TEX[T_MESADA_PUERTITA] = tex(g -> {
            rect(g, 0xC8894F, 0, 0, 64, 64);
            g.setColor(c(0xAE7339)); for (int i = 6; i < 64; i += 10) g.drawLine(0, i, 63, i + 2);
            rect(g, 0xF2C48A, 0, 0, 64, 6); rect(g, 0x8F5A2B, 0, 58, 64, 6);
            rect(g, 0x8F5A2B, 22, 30, 20, 20); rect(g, 0xB98250, 24, 32, 16, 16);
            g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(36, 38, 3, 3));
        });
        TEX[T_LIBRO] = tex(g -> {
            rect(g, 0x4A2F1B, 0, 0, 64, 64);
            int[] cl = {0xC62828, 0x1565C0, 0x2E7D32, 0xF9A825, 0x6A1B9A, 0xEF6C00, 0x00838F};
            for (int s = 0; s < 3; s++) {
                int y = s * 21 + 3, x = 2;
                for (int i = 0; x < 62; i++) {
                    int w = 4 + (i * 3 + s) % 4, h = 12 + (i * 5 + s * 3) % 6;
                    rect(g, cl[(i + s * 2) % 7], x, y + 17 - h, w, h);
                    x += w + 1;
                }
                rect(g, 0x2E1B0E, 0, y + 17, 64, 4);
            }
            rect(g, 0xFFEE58, 28, 24, 9, 17); rect(g, 0xFBC02D, 28, 24, 2, 17);
            rect(g, 0xFFF59D, 36, 26, 3, 13);
        });
        TEX[T_MUNECA] = tex(g -> {
            rect(g, 0x6D5A7A, 0, 0, 64, 64); rect(g, 0x3E2D4D, 6, 6, 52, 52);
            g.setColor(c(0xFFE0D0)); g.fill(new Ellipse2D.Double(20, 12, 24, 26));
            g.setColor(c(0xF8BBD0)); g.fill(new Ellipse2D.Double(22, 28, 7, 5)); g.fill(new Ellipse2D.Double(35, 28, 7, 5));
            g.setColor(c(0x212121)); g.fill(new Ellipse2D.Double(25, 21, 4, 5)); g.fill(new Ellipse2D.Double(35, 21, 4, 5));
            g.setColor(c(0xC62828)); g.fill(new Ellipse2D.Double(29, 31, 6, 3));
            g.setColor(c(0x5D4037)); g.fill(new Arc2D.Double(18, 8, 28, 20, 0, 180, Arc2D.CHORD));
            g.setColor(c(0xF48FB1)); g.fillPolygon(new int[]{20, 44, 52, 12}, new int[]{38, 38, 56, 56}, 4);
            g.setColor(new Color(255, 255, 255, 255)); g.setStroke(new BasicStroke(1.5f)); g.drawRect(6, 6, 51, 51);
        });
        TEX[T_VENTANA] = tex(g -> {
            rect(g, 0x8D6E63, 0, 0, 64, 64); rect(g, 0x6D4C41, 0, 0, 64, 5); rect(g, 0x6D4C41, 0, 59, 64, 5);
            g.setColor(c(0x4E342E)); g.fill(new Ellipse2D.Double(8, 8, 48, 48));
            g.setPaint(new GradientPaint(0, 12, c(0x1A237E), 0, 52, c(0x5C6BC0))); g.fill(new Ellipse2D.Double(12, 12, 40, 40));
            g.setColor(c(0xFFF59D)); g.fill(new Ellipse2D.Double(34, 18, 10, 10));
            g.setColor(c(0x5C6BC0)); g.fill(new Ellipse2D.Double(37, 17, 9, 9));
            g.setColor(c(0x4E342E)); g.setStroke(new BasicStroke(3f)); g.drawLine(32, 12, 32, 52); g.drawLine(12, 32, 52, 32);
        });
        TEX[T_VENTANA_ROTA] = tex(g -> {
            rect(g, 0x8D6E63, 0, 0, 64, 64); rect(g, 0x6D4C41, 0, 0, 64, 5); rect(g, 0x6D4C41, 0, 59, 64, 5);
            g.setColor(c(0x4E342E)); g.fill(new Ellipse2D.Double(8, 8, 48, 48));
            g.setColor(c(0xB3E5FC)); g.fill(new Ellipse2D.Double(12, 12, 40, 40));
            g.setColor(c(0xFFFFFF)); g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 7; i++) { double a = i * Math.PI / 3.5; g.draw(new Line2D.Double(32, 32, 32 + Math.cos(a) * 20, 32 + Math.sin(a) * 20)); }
            g.setColor(c(0xFFFFFF)); g.fillPolygon(new int[]{12, 24, 18}, new int[]{20, 14, 30}, 3);
        });

        // ---- pisos nuevos
        PISO[P_DESPENSA] = tex(g -> tablones(g, 0x6D4C41, 0x4E342E, 16));
        PISO[P_HIELO] = tex(g -> {
            rect(g, 0xDFF3FA, 0, 0, 64, 64); g.setColor(c(0xB5E0F2)); g.setStroke(new BasicStroke(1.2f));
            g.drawLine(0, 20, 30, 28); g.drawLine(30, 28, 64, 18); g.drawLine(12, 64, 28, 40); g.drawLine(40, 0, 46, 30);
            g.setColor(new Color(255, 255, 255, 255)); g.fillRect(6, 8, 14, 3); g.fillRect(40, 44, 14, 3);
        });
        PISO[P_HORNO] = tex(g -> { for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) rect(g, ((x + y) & 1) == 0 ? 0xFFFFFF : 0xEF9A9A, x * 16, y * 16, 16, 16); });
        PISO[P_DESVAN] = tex(g -> tablones(g, 0x5D4037, 0x3E2723, 16));
        PISO[P_ALFOMBRA] = tex(g -> {
            rect(g, 0x8E6B8F, 0, 0, 64, 64); g.setColor(c(0xA98BAA));
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) g.fillPolygon(new int[]{i * 16 + 8, i * 16 + 14, i * 16 + 8, i * 16 + 2}, new int[]{j * 16 + 2, j * 16 + 8, j * 16 + 14, j * 16 + 8}, 4);
        });
        PISO[P_BIBLIO] = tex(g -> {
            rect(g, 0x7B2D3B, 0, 0, 64, 64); g.setColor(c(0xC9A24A)); g.setStroke(new BasicStroke(2f));
            g.drawRect(4, 4, 55, 55); g.drawLine(4, 4, 59, 59); g.drawLine(59, 4, 4, 59);
        });
        PISO[P_COSTURA] = tex(g -> {
            rect(g, 0x9C7B5C, 0, 0, 64, 64); g.setColor(c(0x7E6048)); g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(8, 8, 48, 48)); g.draw(new Ellipse2D.Double(20, 20, 24, 24));
            for (int i = 0; i < 64; i += 16) g.drawLine(0, i, 63, i);
        });
        PISO[P_VERDE] = tex(g -> {
            rect(g, 0x5C7A63, 0, 0, 64, 64); g.setColor(c(0x4A6651));
            for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
            g.setColor(c(0x7FA3C2)); g.fill(new Ellipse2D.Double(10, 38, 26, 10)); g.fill(new Ellipse2D.Double(40, 10, 16, 8));
        });
        PISO[P_LAVADERO] = tex(g -> { for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) rect(g, ((x + y) & 1) == 0 ? 0xB0BEC5 : 0x90A4AE, x * 32, y * 32, 32, 32); });
        PISO[P_CALDERA] = tex(g -> {
            rect(g, 0x607D8B, 0, 0, 64, 64); g.setColor(c(0x546E7A)); g.drawLine(0, 32, 63, 32); g.drawLine(32, 0, 32, 63);
            g.setColor(c(0x8D5B3A)); g.fillRect(8, 8, 6, 3); g.fillRect(44, 40, 8, 3);
        });
        PISO[P_REJA] = tex(g -> {
            rect(g, 0x546E7A, 0, 0, 64, 64); g.setColor(c(0x1B2A33));
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) g.fill(new RoundRectangle2D.Double(i * 16 + 3, j * 16 + 3, 10, 10, 3, 3));
        });
        PISO[P_ESPUMA] = tex(g -> {
            rect(g, 0xE8F7FF, 0, 0, 64, 64); g.setColor(c(0xB3E5FC)); g.setStroke(new BasicStroke(1.5f));
            int[][] b = {{12, 14, 7}, {40, 10, 5}, {52, 36, 8}, {22, 44, 6}, {8, 56, 5}, {44, 56, 6}};
            for (int[] q : b) g.draw(new Ellipse2D.Double(q[0] - q[2], q[1] - q[2], q[2] * 2, q[2] * 2));
        });
        PISO[P_TRONO] = tex(g -> {
            rect(g, 0x3B2F4A, 0, 0, 64, 64); g.setColor(c(0x4A3D5C));
            for (int i = 0; i < 64; i += 16) { g.drawLine(i, 0, i, 63); g.drawLine(0, i, 63, i); }
            g.setColor(c(0x2A1F36)); g.fill(new Ellipse2D.Double(10, 12, 22, 12)); g.fill(new Ellipse2D.Double(36, 40, 18, 10));
        });

        // ---- techos
        TECHOS[1] = tex(g -> { rect(g, 0xFFF3D6, 0, 0, 64, 64); g.setColor(c(0xEBD9A6)); g.drawRect(0, 0, 63, 63); rect(g, 0xFFFBEA, 20, 20, 24, 24); });
        TECHOS[2] = tex(g -> { tablones(g, 0x3B2A22, 0x2A1D17, 16); rect(g, 0x2A1D17, 0, 28, 64, 8); });
        TECHOS[3] = tex(g -> ladrillos(g, 0x3E4F55, 0x2B373C, 10, 21));
        TECHOS[4] = tex(g -> {
            rect(g, 0x37464F, 0, 0, 64, 64);
            rect(g, 0x6B7B84, 0, 10, 64, 9); rect(g, 0x8A99A3, 0, 11, 64, 2); rect(g, 0x6B7B84, 0, 40, 64, 9);
            rect(g, 0x263238, 20, 8, 5, 13); rect(g, 0x263238, 44, 38, 5, 13);
        });
        TECHOS[5] = tex(g -> {
            rect(g, 0x2A2230, 0, 0, 64, 64);
            g.setStroke(new BasicStroke(1.3f)); g.setColor(c(0xD7D7E8));
            for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; g.draw(new Line2D.Double(32, 32, 32 + Math.cos(a) * 46, 32 + Math.sin(a) * 46)); }
            for (int r = 8; r <= 40; r += 10) g.draw(new Ellipse2D.Double(32 - r, 32 - r, r * 2, r * 2));
        });
    }

    // ================================================================== sprites nuevos

    static final Spr[] MIGA = new Spr[2], TOMA = new Spr[2], TOMA_AV = new Spr[2], PELU = new Spr[2], ARA = new Spr[2],
            MOHO_S = new Spr[2], GELA = new Spr[2], SOMB = new Spr[2], GRAS_E = new Spr[2], MAD = new Spr[2],
            CAPULLO_S = new Spr[2], GOTA = new Spr[2], CAP = new Spr[2], ORBE_S = new Spr[3], LLAVES = new Spr[4],
            ESPORAS = new Spr[2], MADRE_C = new Spr[2], MADRE_T = new Spr[2], MADRE_G = new Spr[2];
    static final Spr MIGA_L, TOMA_MAR, TOMA_L, GRAS_G, GRAS_A, GRAS_X, GRAS_L, PELU_L, ARA_COLG, ARA_L,
            MAD_HILO, MAD_ENRED, MAD_L, GOTA_CAE, GOTA_CHARCO, GOTA_L, SOMBRA_GOTA, MOHO_L, CHARCO_MOHO, CAP_ESC,
            CAP_ENFU, CAP_L, GELA_L, SOMB_AV, SOMB_L, MADRE_L, SP_TOMATE, SP_RED, SP_ANCLA, SP_ONDA, SP_ESPORA,
            SP_BGRANDE, SP_SUPERJ, SP_ESCUDO, SP_LINTERNA, SP_TURBO, SP_DUCHA;

    static void suelo(Graphics2D g, double w) { g.setColor(new Color(0, 0, 0, 50)); g.fill(new Ellipse2D.Double(32 - w / 2, 55, w, 8)); }

    static void ojos(Graphics2D g, double cx, double cy, double sep, double r, double mx, boolean enojado) {
        for (int e = 0; e < 2; e++) {
            double x = cx + (e == 0 ? -sep : sep);
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(x - r, cy - r * 1.15, r * 2, r * 2.3));
            g.setColor(c(0x1F1A12)); g.fill(new Ellipse2D.Double(x - r * 0.5 + mx * (e == 0 ? 1 : -1) * 0.5, cy - r * 0.45, r, r * 1.2));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(x - r * 0.3, cy - r * 0.35, r * 0.4, r * 0.4));
        }
        if (enojado) {
            g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx - sep - r * 1.2, cy - r * 1.5, cx - sep + r * 1.1, cy - r * 0.9));
            g.draw(new Line2D.Double(cx + sep + r * 1.2, cy - r * 1.5, cx + sep - r * 1.1, cy - r * 0.9));
        }
    }

    static void sonrisa(Graphics2D g, double cx, double cy, double w) {
        g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(cx - w / 2, cy - w / 4, w, w / 2, 200, 140, Arc2D.OPEN));
    }

    static void bocaMala(Graphics2D g, double cx, double cy, double w) {
        Path2D z = new Path2D.Double(); z.moveTo(cx - w / 2, cy + 2); z.lineTo(cx - w / 4, cy - 2); z.lineTo(cx, cy + 2); z.lineTo(cx + w / 4, cy - 2); z.lineTo(cx + w / 2, cy + 2);
        g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(2.2f)); g.draw(z);
    }

    static void burbujaL(Graphics2D g) {
        g.setColor(new Color(150, 220, 255, 80)); g.fill(new Ellipse2D.Double(1, 1, 62, 62));
        g.setColor(new Color(255, 255, 255, 230)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(1, 1, 62, 62));
        g.fill(new Ellipse2D.Double(10, 8, 9, 6));
    }

    static void patas(Graphics2D g, double cx, double cy, double largo, int col, double f, float ancho) {
        g.setColor(c(col)); g.setStroke(new BasicStroke(ancho, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int s = -1; s <= 1; s += 2) for (int i = 0; i < 4; i++) {
            double a = Math.PI * (0.12 + i * 0.2) + (f > 0 && i % 2 == 0 ? 0.1 : 0), dx = Math.cos(a) * s;
            double kx = cx + dx * largo * 0.8, ky = cy - Math.sin(a) * largo * 0.9 - 4;
            Path2D p = new Path2D.Double(); p.moveTo(cx + s * 4, cy); p.quadTo(kx, ky, cx + dx * largo * 1.15, cy + 14 - i * 1.5);
            g.draw(p);
        }
    }

    static void dMiga(Graphics2D g, int f, boolean limpio) {
        double o = f == 0 ? 0 : -3;
        suelo(g, 30);
        Color col = limpio ? c(0xB3E5FC) : c(0xE2B070), dk = limpio ? c(0x81D4FA) : c(0xB57F3B);
        g.setColor(dk); g.fill(new Ellipse2D.Double(13, 36 + o, 38, 22));
        g.setColor(col); g.fill(new Ellipse2D.Double(14, 33 + o, 22, 20)); g.fill(new Ellipse2D.Double(26, 37 + o, 22, 18)); g.fill(new Ellipse2D.Double(18, 42 + o, 24, 14));
        g.setColor(dk); g.fill(new Ellipse2D.Double(8, 53, 5, 4)); g.fill(new Ellipse2D.Double(50, 52, 4, 4)); g.fill(new Ellipse2D.Double(24, 38 + o, 3, 3));
        ojos(g, 32, 45 + o, 6, 3.6, 0.5, !limpio);
        if (limpio) { sonrisa(g, 32, 53 + o, 10); burbujaL(g); } else bocaMala(g, 32, 53 + o, 9);
    }

    /** modo: 0 normal, 1 aviso (rojo y temblando), 2 mareado, 3 limpio */
    static void dTomate(Graphics2D g, int f, int modo) {
        double o = f == 0 ? 0 : -2;
        suelo(g, 44);
        Color col = modo == 3 ? c(0xFFCDD2) : modo == 1 ? c(0xFF3B30) : c(0xA33A2E), dk = modo == 3 ? c(0xEF9A9A) : modo == 1 ? c(0xC62828) : c(0x7A241C);
        g.setColor(dk); g.fill(new Ellipse2D.Double(8, 20 + o, 48, 38));
        g.setColor(col); g.fill(new Ellipse2D.Double(9, 19 + o, 46, 34));
        g.setColor(modo == 3 ? c(0xFFEBEE) : c(0xD9675A)); g.fill(new Ellipse2D.Double(15, 24 + o, 12, 8));
        if (modo == 0 || modo == 2) { g.setColor(c(0x5A1A14)); g.fill(new Ellipse2D.Double(40, 44 + o, 6, 5)); g.fill(new Ellipse2D.Double(18, 46 + o, 5, 4)); }
        g.setColor(c(0x43A047));
        g.fillPolygon(new int[]{32, 38, 46, 38, 40, 32, 24, 26, 18, 26}, new int[]{20, 12, 14, 20, 26, 22, 26, 20, 14, 12}, 10);
        g.setColor(c(0x2E7D32)); g.fillRect(31, 10, 3, 8);
        if (modo == 2) {
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(18, 31 + o, 12, 12)); g.fill(new Ellipse2D.Double(36, 31 + o, 12, 12));
            g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(1.8f));
            for (int e = 0; e < 2; e++) { double cx = e == 0 ? 24 : 42; for (int k = 1; k <= 2; k++) g.draw(new Ellipse2D.Double(cx - k * 2, 37 + o - k * 2, k * 4, k * 4)); }
            g.setColor(c(0xFFD54F));
            for (int s = 0; s < 3; s++) { double a = s * 2.1 + 0.4; g.fill(new Ellipse2D.Double(32 + Math.cos(a) * 18 - 3, 10 + Math.sin(a) * 4 - 3, 6, 6)); }
            g.setColor(c(0x1F1A12)); g.draw(new Arc2D.Double(24, 46 + o, 16, 8, 0, 180, Arc2D.OPEN));
        } else {
            ojos(g, 32, 37 + o, 8, modo == 1 ? 5.2 : 4.4, 0, modo != 3);
            if (modo == 3) sonrisa(g, 32, 47 + o, 14); else bocaMala(g, 32, 47 + o, 14);
            if (modo == 1) { g.setColor(c(0x81D4FA)); g.fill(new Ellipse2D.Double(51, 22, 5, 8)); }
        }
        if (modo == 3) burbujaL(g);
    }

    /** modo: 0 guardia, 1 ataque, 2 enojado (sin sarten), 3 agotado, 4 limpio */
    static void dGrason(Graphics2D g, int modo, int f) {
        AffineTransform base = g.getTransform();
        double sq = modo == 3 ? 0.88 : (f == 0 ? 1 : 0.95);
        g.translate(32, 60); g.scale(1, sq); g.translate(-32, -60);
        suelo(g, 46);
        boolean limpio = modo == 4;
        Color col = limpio ? c(0xB3E5FC) : modo == 2 ? c(0xE8A079) : c(0xE6D08A), dk = limpio ? c(0x81D4FA) : modo == 2 ? c(0xC4714F) : c(0xBFA550);
        g.setColor(dk); g.fill(new Ellipse2D.Double(10, 26, 44, 33)); g.setColor(col); g.fill(new Ellipse2D.Double(10, 24, 44, 32));
        g.setColor(limpio ? c(0xE1F5FE) : c(0xFFF3B0)); g.fill(new Ellipse2D.Double(16, 29, 10, 7));
        if (!limpio) { g.setColor(c(0xA6882F)); g.fill(new Ellipse2D.Double(40, 46, 5, 6)); g.fill(new Ellipse2D.Double(18, 49, 4, 5)); }
        // gorro
        g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(14, 8, 18, 16)); g.fill(new Ellipse2D.Double(26, 2, 20, 18)); g.fill(new Ellipse2D.Double(34, 8, 18, 16));
        g.setColor(c(0xE0E0E0)); g.fillRect(17, 20, 30, 6); g.setColor(c(0xBDBDBD)); g.drawRect(17, 20, 30, 6);
        if (!limpio) {
            g.setColor(new Color(255, 224, 102, 120)); g.fill(new Ellipse2D.Double(24, 6, 16, 16));
            g.setColor(c(0xFFD54F)); g.fillPolygon(new int[]{27, 28, 32, 36, 37, 37, 27}, new int[]{20, 11, 15, 11, 20, 22, 22}, 7);
            g.setColor(c(0xFFFFFF)); g.fill(new Ellipse2D.Double(31, 14, 3, 3));
        }
        // cara
        if (modo == 3) {
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(17, 30, 12, 12)); g.fill(new Ellipse2D.Double(35, 30, 12, 12));
            g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(1.8f));
            for (int e = 0; e < 2; e++) { double cx = e == 0 ? 23 : 41; g.draw(new Ellipse2D.Double(cx - 4, 33, 8, 8)); g.draw(new Ellipse2D.Double(cx - 1.5, 35.5, 3, 3)); }
            g.setColor(c(0xE57373)); g.fill(new Ellipse2D.Double(28, 47, 9, 8));
            g.setColor(c(0x81D4FA)); g.fill(new Ellipse2D.Double(52, 32, 4, 7));
        } else ojos(g, 32, 37, 9, 5, 0, !limpio);
        g.setColor(limpio ? c(0x4FC3F7) : c(0x5D4037));
        if (modo != 3) { g.fill(new Arc2D.Double(18, 41, 14, 8, 0, 180, Arc2D.CHORD)); g.fill(new Arc2D.Double(32, 41, 14, 8, 0, 180, Arc2D.CHORD)); }
        if (limpio) sonrisa(g, 32, 50, 12);
        else if (modo == 1) { g.setColor(c(0x1F1A12)); g.fill(new Ellipse2D.Double(28, 48, 9, 7)); }
        else if (modo == 2) { g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(2.4f)); g.draw(new Arc2D.Double(25, 49, 14, 8, 20, 140, Arc2D.OPEN)); }
        // sarten / rodillo
        if (modo == 0) {
            g.setColor(c(0x424242)); g.fill(new Ellipse2D.Double(4, 24, 38, 34)); g.setColor(c(0x616161)); g.fill(new Ellipse2D.Double(8, 28, 30, 26));
            g.setColor(new Color(255, 255, 255, 255)); g.fill(new Ellipse2D.Double(13, 32, 10, 6));
            g.setColor(c(0x5D4037)); g.fillRect(36, 50, 24, 5);
            g.setColor(c(0xFFFFFF)); g.fillPolygon(new int[]{44, 46, 48, 46}, new int[]{20, 15, 20, 25}, 4);
        } else if (modo == 1) {
            g.setColor(c(0x424242)); g.fill(new Ellipse2D.Double(38, 44, 24, 16)); g.setColor(c(0x5D4037)); g.fillRect(48, 36, 4, 12);
        } else if (modo == 2) {
            g.setColor(c(0xD7B98A)); g.fill(new RoundRectangle2D.Double(46, 28, 8, 28, 4, 4)); g.setColor(c(0x8D6E63)); g.fillRect(48, 20, 4, 10); g.fillRect(48, 54, 4, 8);
        } else if (modo == 3) {
            g.setColor(c(0x424242)); g.fill(new Ellipse2D.Double(2, 50, 24, 10));
        }
        if (limpio) burbujaL(g);
        g.setTransform(base);
    }

    static void dPelu(Graphics2D g, int f, boolean limpio) {
        suelo(g, 40);
        double o = f == 0 ? 0 : 1;
        int a = limpio ? 0xB3E5FC : 0x9E9E9E, b = limpio ? 0x81D4FA : 0x757575, cc = limpio ? 0xE1F5FE : 0xBDBDBD;
        int[] cols = {a, b, cc};
        for (int i = 0; i < 34; i++) {
            double an = i * 2.399, rr = 6 + (i * 7 % 14);
            double x = 32 + Math.cos(an) * rr * 1.5 + ((i % 3) - 1) * o, y = 40 + Math.sin(an) * rr * 1.1;
            g.setColor(c(cols[i % 3])); g.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
        }
        for (int i = 0; i < 12; i++) {
            double an = i * Math.PI / 6;
            g.setColor(c(b)); g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(32 + Math.cos(an) * 24, 40 + Math.sin(an) * 18, 32 + Math.cos(an) * 29 + o, 40 + Math.sin(an) * 22));
        }
        ojos(g, 32, 40, 7, 4, 0, !limpio);
        if (limpio) { sonrisa(g, 32, 50, 10); burbujaL(g); }
    }

    /** modo: 0 suelo, 1 colgado (hilo), 2 limpio */
    static void dAra(Graphics2D g, int f, int modo) {
        double cy = modo == 1 ? 46 : 42;
        boolean limpio = modo == 2;
        if (modo == 1) { g.setColor(c(0xF0F0F8)); g.setStroke(new BasicStroke(1.6f)); g.drawLine(32, 0, 32, 36); }
        else suelo(g, 34);
        int body = limpio ? 0xB3E5FC : 0x7E57C2, dk = limpio ? 0x81D4FA : 0x4527A0;
        patas(g, 32, cy, 24, dk, f, 2.6f);
        g.setColor(c(dk)); g.fill(new Ellipse2D.Double(18, cy - 10, 28, 22));
        g.setColor(c(body)); g.fill(new Ellipse2D.Double(19, cy - 12, 26, 20));
        g.setColor(c(limpio ? 0xE1F5FE : 0xB39DDB)); g.fill(new Ellipse2D.Double(24, cy - 10, 8, 5));
        if (!limpio) { g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(30, cy + 1, 4, 4)); }
        ojos(g, 32, cy - 3, 6, 3.8, 0, !limpio);
        if (limpio) { sonrisa(g, 32, cy + 5, 8); burbujaL(g); }
        else { g.setColor(c(0xF8BBD0)); g.fill(new Ellipse2D.Double(17, cy, 5, 3)); g.fill(new Ellipse2D.Double(42, cy, 5, 3)); }
    }

    /** modo: 0 camina, 1 con hilo, 2 enredada, 3 limpia */
    static void dMadame(Graphics2D g, int modo, int f) {
        boolean limpio = modo == 3;
        double cy = 40;
        if (modo == 1) { g.setColor(c(0xF0F0F8)); g.setStroke(new BasicStroke(1.8f)); g.drawLine(32, 0, 32, 12); }
        else suelo(g, 46);
        patas(g, 32, cy + 4, 30, limpio ? 0x81D4FA : 0x311B5C, f, 3f);
        int body = limpio ? 0xB3E5FC : 0x5E35B1, dk = limpio ? 0x81D4FA : 0x311B5C;
        g.setColor(c(dk)); g.fill(new Ellipse2D.Double(14, cy - 6, 36, 28)); g.setColor(c(body)); g.fill(new Ellipse2D.Double(15, cy - 8, 34, 26));
        if (!limpio) {
            g.setColor(c(0xFFD54F)); g.setStroke(new BasicStroke(1.8f)); g.draw(new Arc2D.Double(20, cy - 2, 24, 18, 200, 140, Arc2D.OPEN)); g.draw(new Arc2D.Double(24, cy + 2, 16, 12, 200, 140, Arc2D.OPEN));
        }
        g.setColor(c(limpio ? 0xE1F5FE : 0x7E57C2)); g.fill(new Ellipse2D.Double(20, cy - 24, 24, 22));
        g.setColor(c(limpio ? 0xB3E5FC : 0x9575CD)); g.fill(new Ellipse2D.Double(22, cy - 24, 20, 14));
        // sombrerito
        g.setColor(c(0x212121)); g.fill(new Ellipse2D.Double(18, cy - 26, 28, 7)); g.fill(new RoundRectangle2D.Double(24, cy - 36, 16, 12, 5, 5));
        g.setColor(c(0xF06292)); g.fillRect(24, cy > 0 ? (int) cy - 28 : 0, 16, 3);
        g.setColor(c(0xF8BBD0)); g.setStroke(new BasicStroke(2.4f)); g.draw(new Arc2D.Double(34, cy - 44, 20, 18, 200, 120, Arc2D.OPEN));
        if (modo == 2) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(23, cy - 18, 8, 8)); g.fill(new Ellipse2D.Double(33, cy - 18, 8, 8));
            g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(25, cy - 16, 4, 4)); g.draw(new Ellipse2D.Double(35, cy - 16, 4, 4));
        } else ojos(g, 32, cy - 14, 5, 3.2, 0, !limpio);
        g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(1.6f));
        if (!limpio) { g.drawLine(24, cy > 0 ? (int) cy - 21 : 0, 22, (int) cy - 24); g.drawLine(40, (int) cy - 21, 42, (int) cy - 24); }
        if (limpio) sonrisa(g, 32, cy - 6, 9); else { g.setColor(c(0xC62828)); g.fill(new Ellipse2D.Double(29, cy - 9, 6, 3)); }
        // broche
        if (!limpio) {
            g.setColor(new Color(255, 120, 190, 110)); g.fill(new Ellipse2D.Double(24, cy + 1, 16, 16));
            g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(27, cy + 4, 10, 10)); g.setColor(c(0xFF4081)); g.fill(new Ellipse2D.Double(29, cy + 6, 6, 6));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(30, cy + 7, 2, 2));
        }
        if (modo == 2) {
            g.setColor(c(0xF5F5FA)); g.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 9; i++) g.draw(new Line2D.Double(8 + i * 6, 6, 56 - i * 6, 62));
            for (int i = 0; i < 5; i++) g.draw(new Line2D.Double(6, 14 + i * 10, 58, 22 + i * 8));
        }
        if (limpio) burbujaL(g);
    }

    static void dCapullo(Graphics2D g, int f) {
        g.setColor(c(0xF0F0F8)); g.setStroke(new BasicStroke(1.8f)); g.drawLine(32, 0, 32, 12 + f);
        g.setColor(c(0xB8B8C8)); g.fill(new Ellipse2D.Double(17 + f, 10, 30, 52));
        g.setColor(c(0xE6E6F0)); g.fill(new Ellipse2D.Double(18 + f, 9, 28, 50));
        g.setColor(c(0xFFFFFF)); g.fill(new Ellipse2D.Double(22 + f, 14, 8, 18));
        g.setColor(c(0xA5A5B8)); g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 6; i++) g.draw(new Arc2D.Double(18 + f, 12 + i * 8, 28, 14, 200, 140, Arc2D.OPEN));
        g.setColor(c(0x7E57C2)); g.fill(new Ellipse2D.Double(28 + f, 36, 3, 3)); g.fill(new Ellipse2D.Double(36 + f, 40, 3, 3));
    }

    /** modo: 0 gota con cara, 1 cayendo, 2 charco, 3 limpia */
    static void dGota(Graphics2D g, int modo) {
        if (modo == 2) {
            g.setColor(c(0x29B6F6)); g.fill(new Ellipse2D.Double(8, 48, 48, 12)); g.setColor(c(0x81D4FA)); g.fill(new Ellipse2D.Double(14, 50, 22, 5));
            g.setColor(c(0xE1F5FE)); g.setStroke(new BasicStroke(1.5f)); g.draw(new Ellipse2D.Double(18, 46, 28, 8));
            return;
        }
        boolean limpio = modo == 3;
        double top = modo == 1 ? 0 : 6, cy = modo == 1 ? 38 : 40;
        Path2D p = new Path2D.Double(); p.moveTo(32, top); p.curveTo(40, top + 16, 52, cy - 10, 50, cy + 2); p.curveTo(50, cy + 20, 14, cy + 20, 14, cy + 2); p.curveTo(12, cy - 10, 24, top + 16, 32, top); p.closePath();
        g.setColor(c(limpio ? 0xB3E5FC : 0x29B6F6)); g.fill(p);
        g.setColor(c(limpio ? 0xE1F5FE : 0x4FC3F7)); g.fill(new Ellipse2D.Double(20, cy - 6, 9, 14));
        g.setColor(c(limpio ? 0x81D4FA : 0x0288D1)); g.setStroke(new BasicStroke(2f)); g.draw(p);
        ojos(g, 32, cy + 2, 7, 4.2, 0, !limpio);
        if (limpio) { sonrisa(g, 32, cy + 12, 10); burbujaL(g); } else bocaMala(g, 32, cy + 12, 10);
    }

    static void dMoho(Graphics2D g, int f, boolean limpio) {
        double o = f == 0 ? 0 : 2;
        suelo(g, 52);
        Color col = limpio ? c(0xB3E5FC) : c(0x7CB342), dk = limpio ? c(0x81D4FA) : c(0x4E7A28);
        g.setColor(dk); g.fill(new Ellipse2D.Double(5, 24 + o, 54, 36 - o));
        g.setColor(col); g.fill(new Ellipse2D.Double(6, 22 + o, 52, 34 - o));
        g.fill(new Ellipse2D.Double(12, 15 + o, 18, 16)); g.fill(new Ellipse2D.Double(32, 17 + o, 20, 16));
        g.setColor(limpio ? c(0xE1F5FE) : c(0xAED581)); g.fill(new Ellipse2D.Double(16, 20 + o, 8, 6));
        if (!limpio) { g.setColor(c(0x33691E)); g.fill(new Ellipse2D.Double(40, 42, 7, 5)); g.fill(new Ellipse2D.Double(14, 46, 6, 5)); g.fill(new Ellipse2D.Double(28, 50, 5, 4)); }
        ojos(g, 32, 36 + o, 8, 4.4, 0, false);
        if (!limpio) { g.setColor(col); g.fill(new Rectangle2D.Double(23, 30 + o, 18, 4.5)); g.setColor(dk); g.setStroke(new BasicStroke(1.6f)); g.drawLine(23, 35 + (int) o, 41, 35 + (int) o); }
        if (limpio) { sonrisa(g, 32, 48, 12); burbujaL(g); } else { g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(2.2f)); g.draw(new Arc2D.Double(25, 47, 14, 7, 20, 140, Arc2D.OPEN)); }
    }

    /** modo: 0 normal, 1 con escudo de agua, 2 enfurecido, 3 limpio */
    static void dCapitan(Graphics2D g, int modo, int f) {
        double o = f == 0 ? 0 : 1.5;
        boolean limpio = modo == 3;
        suelo(g, 54);
        Color col = limpio ? c(0xB3E5FC) : modo == 2 ? c(0xA8732F) : c(0x6A9A2F), dk = limpio ? c(0x81D4FA) : modo == 2 ? c(0x7A4A1A) : c(0x3F6B1E);
        g.setColor(dk); g.fill(new Ellipse2D.Double(5, 26 + o, 54, 34 - o)); g.setColor(col); g.fill(new Ellipse2D.Double(6, 24 + o, 52, 32 - o));
        g.fill(new Ellipse2D.Double(10, 18 + o, 20, 16)); g.fill(new Ellipse2D.Double(34, 20 + o, 20, 14));
        g.setColor(limpio ? c(0xE1F5FE) : c(0xAED581)); g.fill(new Ellipse2D.Double(14, 24 + o, 9, 6));
        if (!limpio) { g.setColor(c(0x2E5016)); g.fill(new Ellipse2D.Double(42, 44, 7, 5)); g.fill(new Ellipse2D.Double(12, 48, 6, 4)); }
        // gorro de capitan
        g.setColor(c(0x1A237E)); g.fill(new Arc2D.Double(14, 4 + o, 36, 24, 0, 180, Arc2D.CHORD)); g.setColor(c(0xFFFFFF)); g.fillRect(14, (int) (16 + o), 36, 4);
        g.setColor(c(0x111B5E)); g.fill(new Ellipse2D.Double(12, 17 + o, 40, 6));
        g.setColor(c(0xFFD54F)); g.fill(new Ellipse2D.Double(28, 9 + o, 8, 8));
        ojos(g, 32, 34 + o, 9, 4.6, 0, !limpio);
        if (!limpio) { g.setColor(modo == 2 ? c(0x8E1B1B) : c(0x2E5016)); g.fill(new Ellipse2D.Double(18, 42 + o, 28, 13)); g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(2.4f)); g.draw(new Arc2D.Double(24, 44 + o, 16, 7, 20, 140, Arc2D.OPEN)); }
        else sonrisa(g, 32, 50, 12);
        // ancla
        if (!limpio) {
            g.setColor(c(0x78909C)); g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(54, 24, 54, 56); g.drawLine(48, 30, 60, 30); g.draw(new Arc2D.Double(46, 44, 16, 16, 180, 180, Arc2D.OPEN));
            g.draw(new Ellipse2D.Double(51, 18, 6, 6));
        }
        if (modo == 1) {
            g.setColor(new Color(129, 212, 247, 255)); g.setStroke(new BasicStroke(3f)); g.draw(new Ellipse2D.Double(1, 1, 62, 62));
            g.setColor(new Color(225, 245, 254, 255)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(5, 5, 54, 54));
            g.fill(new Ellipse2D.Double(10, 8, 9, 6)); g.fill(new Ellipse2D.Double(50, 44, 5, 5));
        }
        if (limpio) burbujaL(g);
    }

    static void dGela(Graphics2D g, int f, boolean limpio) {
        double sy = f == 0 ? 1 : 0.9, sx = f == 0 ? 1 : 1.07;
        AffineTransform base = g.getTransform(); g.translate(32, 60); g.scale(sx, sy); g.translate(-32, -60);
        suelo(g, 50);
        Color col = limpio ? c(0xB3E5FC) : c(0x5E3FA3), dk = limpio ? c(0x81D4FA) : c(0x2E1F5C);
        Path2D p = new Path2D.Double(); p.moveTo(6, 58); p.curveTo(2, 34, 16, 18, 32, 18); p.curveTo(48, 18, 62, 34, 58, 58); p.closePath();
        g.setColor(dk); g.fill(p); g.setColor(col); g.translate(0, -2); g.fill(p); g.translate(0, 2);
        g.setColor(limpio ? c(0xE1F5FE) : c(0x9A7BE0)); g.fill(new Ellipse2D.Double(14, 24, 12, 8));
        ojos(g, 32, 38, 9, 5.2, 0, !limpio);
        if (limpio) sonrisa(g, 32, 50, 12); else bocaMala(g, 32, 50, 14);
        g.setTransform(base);
        if (limpio) burbujaL(g);
    }

    /** modo: 0 normal, 1 aviso, 2 limpio */
    static void dSombra(Graphics2D g, int f, int modo) {
        boolean limpio = modo == 2;
        double w = f == 0 ? 0 : 2;
        Color col = limpio ? c(0xB3E5FC) : c(0x3A444C), dk = limpio ? c(0x81D4FA) : c(0x232A30);
        g.setColor(dk); g.fill(new Ellipse2D.Double(12, 10, 40, 38));
        Path2D t = new Path2D.Double(); t.moveTo(12, 34); t.curveTo(8 + w, 54, 16, 60, 20 - w, 62); t.curveTo(26, 56, 30, 62, 34, 58); t.curveTo(38, 62, 44 + w, 58, 48, 62); t.curveTo(54, 56, 56, 46, 52, 34); t.closePath();
        g.fill(t);
        g.setColor(col); g.fill(new Ellipse2D.Double(14, 9, 36, 34)); g.fill(new Ellipse2D.Double(11, 26 + w, 14, 18)); g.fill(new Ellipse2D.Double(39, 26 - w, 14, 18));
        if (modo == 1) { g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2.5f)); g.draw(new Ellipse2D.Double(12, 8, 40, 40)); }
        g.setColor(limpio ? Color.WHITE : modo == 1 ? c(0xFFEB3B) : c(0xFFFFFF));
        g.fill(new Ellipse2D.Double(20, 22, 9, 13)); g.fill(new Ellipse2D.Double(36, 22, 9, 13));
        g.setColor(c(limpio ? 0x1F1A12 : 0x111111)); g.fill(new Ellipse2D.Double(23, 26, 4, 7)); g.fill(new Ellipse2D.Double(38, 26, 4, 7));
        if (limpio) { sonrisa(g, 32, 40, 12); burbujaL(g); }
        else { g.setColor(c(0x111111)); g.setStroke(new BasicStroke(2.6f)); g.drawLine(18, 18, 28, 22); g.drawLine(46, 18, 36, 22); }
    }

    /** modo: 0 charco, 1 torre, 2 gran mancha, 3 limpia (burbujita) */
    static void dMadre(Graphics2D g, int modo, int f) {
        boolean limpio = modo == 3;
        Color col = limpio ? c(0xB3E5FC) : c(0x2E2240), dk = limpio ? c(0x81D4FA) : c(0x1A1326), br = limpio ? c(0xE1F5FE) : c(0x5B4580);
        double w = f == 0 ? 0 : 1.5;
        if (modo == 0) {
            g.setColor(new Color(0, 0, 0, 60)); g.fill(new Ellipse2D.Double(0, 54, 64, 8));
            g.setColor(dk); g.fill(new Ellipse2D.Double(1, 44, 62, 18)); g.setColor(col); g.fill(new Ellipse2D.Double(2, 42 + w, 60, 16));
            g.setColor(br); g.fill(new Ellipse2D.Double(8, 44, 16, 5));
            g.setColor(col); g.fill(new Ellipse2D.Double(14, 32, 14, 16)); g.fill(new Ellipse2D.Double(36, 32, 14, 16));
            for (int e = 0; e < 2; e++) {
                double cx = e == 0 ? 21 : 43;
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 8, 22, 16, 20)); g.setColor(c(0x1F1A12)); g.fill(new Ellipse2D.Double(cx - 4 + (e == 0 ? 1 : -1), 28, 8, 11));
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 2, 29, 3, 3));
            }
            g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(12, 18, 28, 25)); g.draw(new Line2D.Double(36, 25, 52, 18));
            g.setColor(c(0x7F6AA6)); g.draw(new Line2D.Double(36, 21, 52, 25));
            return;
        }
        if (limpio) {
            g.setColor(c(0xB3E5FC)); g.fill(new Ellipse2D.Double(8, 8, 48, 48));
            g.setColor(c(0xE1F5FE)); g.fill(new Ellipse2D.Double(14, 12, 14, 9));
            ojos(g, 32, 32, 9, 4.8, 0, false); sonrisa(g, 32, 44, 14); burbujaL(g);
            return;
        }
        double top = modo == 1 ? 2 : 6, left = modo == 1 ? 14 : 3, wid = modo == 1 ? 36 : 58, hei = modo == 1 ? 58 : 54;
        g.setColor(new Color(0, 0, 0, 60)); g.fill(new Ellipse2D.Double(left - 2, 56, wid + 4, 8));
        g.setColor(dk);
        for (int i = 0; i < 7; i++) { double x = left + 2 + i * (wid - 8) / 6.0; g.fill(new Ellipse2D.Double(x, 50 + (i % 2) * 3 + w, 8, 12)); }
        g.fill(new Ellipse2D.Double(left, top + 2, wid, hei));
        g.setColor(col); g.fill(new Ellipse2D.Double(left + 1, top, wid - 2, hei - 2));
        g.setColor(br); g.fill(new Ellipse2D.Double(left + 6, top + 6, 14, 8));
        for (int e = 0; e < 2; e++) {
            double cx = 32 + (e == 0 ? -11 : 11), cy = modo == 1 ? 22 : 24;
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 8, cy - 11, 16, 22)); g.setColor(c(0x1F1A12)); g.fill(new Ellipse2D.Double(cx - 4 + (e == 0 ? 1 : -1), cy - 4, 8, 12));
            g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(cx - 2, cy - 3, 3, 3));
        }
        g.setColor(c(0x1F1A12)); g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double by = modo == 1 ? 8 : 10;
        g.draw(new Line2D.Double(14, by, 30, by + 7)); g.draw(new Line2D.Double(34, by + 4, 50, by + 11));
        g.draw(new Arc2D.Double(24, (modo == 1 ? 40 : 42), 16, 10, 20, 140, Arc2D.OPEN));
        g.setColor(c(0x4FC3F7)); g.fill(new Ellipse2D.Double(45, by + 16, 3, 6));
    }

    static void dOrbe(Graphics2D g, int aura) {
        Color a = c(aura);
        g.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 255)); g.fill(new Ellipse2D.Double(4, 4, 56, 56));
        g.setColor(new Color(Math.min(255, a.getRed() + 70), Math.min(255, a.getGreen() + 70), Math.min(255, a.getBlue() + 70), 255)); g.fill(new Ellipse2D.Double(9, 9, 46, 46));
        g.setColor(c(0xFFD54F)); g.fillPolygon(new int[]{18, 20, 28, 32, 36, 44, 46, 46, 18}, new int[]{44, 22, 34, 16, 34, 22, 44, 48, 48}, 9);
        g.setColor(c(0xB8860B)); g.setStroke(new BasicStroke(2f)); g.drawPolygon(new int[]{18, 20, 28, 32, 36, 44, 46, 46, 18}, new int[]{44, 22, 34, 16, 34, 22, 44, 48, 48}, 9);
        g.setColor(c(0xE53935)); g.fill(new Ellipse2D.Double(29, 36, 6, 6));
        g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(12, 10, 8, 6));
    }

    static void dLlave(Graphics2D g, int col, int dk, int tipo) {
        g.setColor(new Color(0, 0, 0, 40)); g.fill(new Ellipse2D.Double(10, 54, 44, 7));
        if (tipo == 3) { // llave inglesa
            g.setColor(c(dk)); g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(new Line2D.Double(14, 50, 40, 22));
            g.setColor(c(col)); g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(new Line2D.Double(14, 50, 40, 22));
            Area a = new Area(new Ellipse2D.Double(34, 6, 24, 24)); a.subtract(new Area(new Rectangle2D.Double(41, 4, 10, 14))); g.setColor(c(col)); g.fill(a);
            g.setColor(c(dk)); g.setStroke(new BasicStroke(2f)); g.draw(a);
            return;
        }
        Area a = new Area(new Ellipse2D.Double(8, 12, 26, 26)); a.subtract(new Area(new Ellipse2D.Double(15, 19, 12, 12)));
        g.setColor(c(col)); g.fill(a); g.fillRect(30, 22, 28, 7); g.fillRect(46, 28, 6, 12); g.fillRect(54, 28, 5, 9);
        g.setColor(c(dk)); g.setStroke(new BasicStroke(2f)); g.draw(a);
        if (tipo == 2) {
            g.setColor(c(0xF3E5F5)); g.setStroke(new BasicStroke(1.6f));
            for (int i = 0; i < 5; i++) g.draw(new Arc2D.Double(6 + i, 10 + i, 30 - 2 * i, 30 - 2 * i, 20 + i * 40, 200, Arc2D.OPEN));
            g.setColor(c(0xFFFFFF)); g.fill(new Ellipse2D.Double(48, 12, 6, 6));
        }
    }

    static {
        for (int f = 0; f < 2; f++) {
            final int ff = f;
            MIGA[f] = spr(64, 64, g -> dMiga(g, ff, false));
            TOMA[f] = spr(64, 64, g -> dTomate(g, ff, 0));
            TOMA_AV[f] = spr(64, 64, g -> dTomate(g, ff, 1));
            GRAS_E[f] = spr(96, 96, g -> { g.scale(1.5, 1.5); dGrason(g, 2, ff); });
            PELU[f] = spr(64, 64, g -> dPelu(g, ff, false));
            ARA[f] = spr(64, 64, g -> dAra(g, ff, 0));
            MAD[f] = spr(96, 96, g -> { g.scale(1.5, 1.5); dMadame(g, 0, ff); });
            CAPULLO_S[f] = spr(64, 64, g -> dCapullo(g, ff));
            MOHO_S[f] = spr(64, 64, g -> dMoho(g, ff, false));
            CAP[f] = spr(96, 96, g -> { g.scale(1.5, 1.5); dCapitan(g, 0, ff); });
            GELA[f] = spr(64, 64, g -> dGela(g, ff, false));
            SOMB[f] = spr(64, 64, g -> dSombra(g, ff, 0));
            MADRE_C[f] = spr(128, 128, g -> { g.scale(2, 2); dMadre(g, 0, ff); });
            MADRE_T[f] = spr(128, 128, g -> { g.scale(2, 2); dMadre(g, 1, ff); });
            MADRE_G[f] = spr(128, 128, g -> { g.scale(2, 2); dMadre(g, 2, ff); });
        }
        MIGA_L = spr(64, 64, g -> dMiga(g, 0, true));
        TOMA_MAR = spr(64, 64, g -> dTomate(g, 0, 2));
        TOMA_L = spr(64, 64, g -> dTomate(g, 0, 3));
        GRAS_G = spr(96, 96, g -> { g.scale(1.5, 1.5); dGrason(g, 0, 0); });
        GRAS_A = spr(96, 96, g -> { g.scale(1.5, 1.5); dGrason(g, 1, 0); });
        GRAS_X = spr(96, 96, g -> { g.scale(1.5, 1.5); dGrason(g, 3, 0); });
        GRAS_L = spr(96, 96, g -> { g.scale(1.5, 1.5); dGrason(g, 4, 0); });
        PELU_L = spr(64, 64, g -> dPelu(g, 0, true));
        ARA_COLG = spr(64, 64, g -> dAra(g, 0, 1));
        ARA_L = spr(64, 64, g -> dAra(g, 0, 2));
        MAD_HILO = spr(96, 96, g -> { g.scale(1.5, 1.5); dMadame(g, 1, 0); });
        MAD_ENRED = spr(96, 96, g -> { g.scale(1.5, 1.5); dMadame(g, 2, 0); });
        MAD_L = spr(96, 96, g -> { g.scale(1.5, 1.5); dMadame(g, 3, 0); });
        GOTA[0] = spr(64, 64, g -> dGota(g, 0));
        GOTA[1] = spr(64, 64, g -> dGota(g, 0));
        GOTA_CAE = spr(64, 64, g -> dGota(g, 1));
        GOTA_CHARCO = spr(64, 64, g -> dGota(g, 2));
        GOTA_L = spr(64, 64, g -> dGota(g, 3));
        SOMBRA_GOTA = spr(64, 64, g -> {
            g.setColor(c(0x24334D)); g.fill(new Ellipse2D.Double(10, 50, 44, 12));
            g.setColor(c(0x3C5278)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(14, 52, 36, 8));
        });
        MOHO_L = spr(64, 64, g -> dMoho(g, 0, true));
        CHARCO_MOHO = spr(64, 64, g -> {
            g.setColor(c(0x4E7A28)); g.fill(new Ellipse2D.Double(4, 48, 56, 14)); g.setColor(c(0x7CB342)); g.fill(new Ellipse2D.Double(8, 49, 48, 10));
            g.setColor(c(0xAED581)); g.fill(new Ellipse2D.Double(14, 51, 16, 4));
        });
        for (int f = 0; f < 2; f++) {
            final int ff = f;
            ESPORAS[f] = spr(64, 64, g -> {
                int[] cl = {0xC5E1A5, 0xDCE775, 0xAED581, 0xF0F4C3};
                for (int i = 0; i < 22; i++) {
                    double a = i * 2.4 + ff * 0.9, r = 6 + (i * 5) % 22;
                    double x = 32 + Math.cos(a) * r, y = 36 + Math.sin(a) * r * 0.7, s = 4 + (i % 4) * 1.6;
                    g.setColor(c(cl[i % 4])); g.fill(new Ellipse2D.Double(x - s, y - s, s * 2, s * 2));
                }
            });
        }
        CAP_ESC = spr(96, 96, g -> { g.scale(1.5, 1.5); dCapitan(g, 1, 0); });
        CAP_ENFU = spr(96, 96, g -> { g.scale(1.5, 1.5); dCapitan(g, 2, 0); });
        CAP_L = spr(96, 96, g -> { g.scale(1.5, 1.5); dCapitan(g, 3, 0); });
        GELA_L = spr(64, 64, g -> dGela(g, 0, true));
        SOMB_AV = spr(64, 64, g -> dSombra(g, 0, 1));
        SOMB_L = spr(64, 64, g -> dSombra(g, 0, 2));
        MADRE_L = spr(128, 128, g -> { g.scale(2, 2); dMadre(g, 3, 0); });
        ORBE_S[0] = spr(64, 64, g -> dOrbe(g, 0xE53935));
        ORBE_S[1] = spr(64, 64, g -> dOrbe(g, 0x8E24AA));
        ORBE_S[2] = spr(64, 64, g -> dOrbe(g, 0x7CB342));

        SP_TOMATE = spr(32, 32, g -> {
            g.setColor(c(0x7A241C)); g.fill(new Ellipse2D.Double(2, 6, 28, 24)); g.setColor(c(0xC0392B)); g.fill(new Ellipse2D.Double(3, 5, 26, 22));
            g.setColor(c(0xE57373)); g.fill(new Ellipse2D.Double(7, 8, 8, 5)); g.setColor(c(0x43A047)); g.fillPolygon(new int[]{16, 21, 25, 19, 16, 13, 7, 12}, new int[]{4, 1, 4, 7, 8, 7, 4, 1}, 8);
        });
        SP_RED = spr(32, 32, g -> {
            g.setColor(new Color(255, 255, 255, 255)); g.setStroke(new BasicStroke(1.6f));
            for (int i = 0; i < 6; i++) { double a = i * Math.PI / 3; g.draw(new Line2D.Double(16, 16, 16 + Math.cos(a) * 14, 16 + Math.sin(a) * 14)); }
            for (int r = 5; r <= 13; r += 4) g.draw(new Ellipse2D.Double(16 - r, 16 - r, r * 2, r * 2));
            g.setColor(c(0xB39DDB)); g.fill(new Ellipse2D.Double(12, 12, 8, 8));
        });
        SP_ANCLA = spr(32, 32, g -> {
            g.setColor(c(0x546E7A)); g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(16, 6, 16, 26); g.drawLine(10, 11, 22, 11); g.draw(new Arc2D.Double(6, 14, 20, 16, 180, 180, Arc2D.OPEN)); g.draw(new Ellipse2D.Double(13, 2, 6, 6));
            g.setColor(c(0xB0BEC5)); g.setStroke(new BasicStroke(1.4f)); g.drawLine(15, 8, 15, 24);
        });
        SP_ONDA = spr(32, 32, g -> {
            g.setColor(c(0x29B6F6)); g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(2, 6, 28, 22, 20, 140, Arc2D.OPEN));
            g.setColor(c(0xE1F5FE)); g.setStroke(new BasicStroke(2f)); g.draw(new Arc2D.Double(4, 7, 24, 18, 30, 120, Arc2D.OPEN));
        });
        SP_ESPORA = spr(32, 32, g -> {
            g.setColor(c(0x689F38)); g.fill(new Ellipse2D.Double(4, 4, 24, 24)); g.setColor(c(0xAED581)); g.fill(new Ellipse2D.Double(8, 8, 14, 14));
            g.setColor(c(0xF0F4C3)); g.fill(new Ellipse2D.Double(10, 9, 5, 4));
        });
        SP_BGRANDE = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(10, 55, 44, 8));
            g.setColor(c(0x81D4FA)); g.fill(new Ellipse2D.Double(5, 5, 54, 54)); g.setColor(c(0xB3E5FC)); g.fill(new Ellipse2D.Double(8, 8, 48, 48));
            g.setColor(Color.WHITE); g.setStroke(new BasicStroke(3f)); g.draw(new Ellipse2D.Double(5, 5, 54, 54)); g.fill(new Ellipse2D.Double(14, 12, 14, 9));
            g.setColor(c(0x0277BD)); g.setFont(new Font("SansSerif", Font.BOLD, 22)); g.drawString("x2", 20, 41);
        });
        SP_SUPERJ = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(8, 50, 48, 9));
            g.setColor(c(0xFFB300)); g.fill(new RoundRectangle2D.Double(8, 22, 48, 30, 14, 14));
            g.setColor(c(0xFFE082)); g.fill(new RoundRectangle2D.Double(12, 25, 34, 8, 8, 8));
            g.setColor(c(0xE65100)); g.setStroke(new BasicStroke(2f)); g.draw(new RoundRectangle2D.Double(8, 22, 48, 30, 14, 14));
            g.setColor(Color.WHITE);
            for (int[] s : new int[][]{{14, 12}, {48, 14}, {32, 6}}) { g.fillRect(s[0] - 1, s[1] - 4, 2, 8); g.fillRect(s[0] - 4, s[1] - 1, 8, 2); }
        });
        SP_ESCUDO = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(10, 55, 44, 8));
            g.setColor(c(0x4FC3F7)); g.fill(new Ellipse2D.Double(8, 8, 48, 48)); g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(12, 12, 40, 40));
            g.setColor(c(0xB3E5FC)); g.setStroke(new BasicStroke(2f));
            for (int[] b : new int[][]{{22, 24, 7}, {38, 22, 5}, {30, 38, 9}, {44, 40, 5}, {20, 44, 4}}) g.draw(new Ellipse2D.Double(b[0] - b[2], b[1] - b[2], b[2] * 2, b[2] * 2));
            g.setColor(c(0x0288D1)); g.draw(new Ellipse2D.Double(8, 8, 48, 48));
        });
        SP_LINTERNA = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(14, 55, 36, 7));
            g.setColor(new Color(255, 241, 118, 255)); g.fill(new Ellipse2D.Double(2, 14, 20, 20)); 
            g.setColor(c(0x546E7A)); g.fill(new RoundRectangle2D.Double(18, 18, 30, 34, 8, 8)); g.setColor(c(0x78909C)); g.fillRect(21, 21, 6, 28);
            g.setColor(c(0x37474F)); g.fill(new RoundRectangle2D.Double(14, 12, 20, 12, 5, 5));
            g.setColor(c(0xFFEE58)); g.fill(new Ellipse2D.Double(8, 18, 14, 14)); g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(10, 20, 5, 5));
            g.setColor(c(0xB3E5FC)); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(44, 6, 12, 12)); g.draw(new Ellipse2D.Double(50, 22, 7, 7));
            g.setColor(c(0xE53935)); g.fill(new Ellipse2D.Double(30, 30, 10, 10));
        });
        SP_TURBO = spr(64, 64, g -> {
            g.setColor(new Color(0, 0, 0, 45)); g.fill(new Ellipse2D.Double(12, 55, 40, 7));
            g.setColor(c(0xFF7043)); g.fillPolygon(new int[]{20, 32, 44, 32}, new int[]{50, 62, 50, 56}, 4);
            g.setColor(c(0x00E5FF)); g.fill(new RoundRectangle2D.Double(16, 20, 32, 36, 12, 12));
            g.setColor(c(0xFFEE58)); g.fill(new RoundRectangle2D.Double(22, 8, 20, 16, 5, 5));
            g.setColor(c(0xE53935)); g.fillPolygon(new int[]{10, 18, 18}, new int[]{54, 38, 54}, 3); g.fillPolygon(new int[]{54, 46, 46}, new int[]{54, 38, 54}, 3);
            g.setColor(Color.WHITE); g.fill(new RoundRectangle2D.Double(20, 30, 24, 12, 5, 5));
            g.setColor(c(0x006064)); g.setFont(new Font("SansSerif", Font.BOLD, 9)); g.drawString("5000", 21, 40);
            g.setStroke(new BasicStroke(2f)); g.draw(new RoundRectangle2D.Double(16, 20, 32, 36, 12, 12));
        });
        LLAVES[0] = SP_LLA;
        LLAVES[1] = spr(64, 64, g -> dLlave(g, 0xCFD8DC, 0x607D8B, 1));
        LLAVES[2] = spr(64, 64, g -> dLlave(g, 0xAB47BC, 0x6A1B9A, 2));
        LLAVES[3] = spr(64, 64, g -> dLlave(g, 0xB0BEC5, 0x37474F, 3));
        SP_DUCHA = spr(64, 64, g -> {
            g.setColor(c(0x78909C)); g.fillRect(29, 0, 6, 22);
            g.setColor(c(0x546E7A)); g.fillRect(27, 18, 10, 5);
            Path2D p = new Path2D.Double(); p.moveTo(10, 36); p.curveTo(10, 18, 54, 18, 54, 36); p.closePath();
            g.setColor(c(0x90A4AE)); g.fill(p); g.setColor(c(0xCFD8DC)); g.fill(new Ellipse2D.Double(16, 22, 14, 6));
            g.setColor(c(0x546E7A)); g.setStroke(new BasicStroke(2f)); g.draw(p);
            g.setColor(c(0x37474F)); for (int i = 0; i < 5; i++) g.fill(new Ellipse2D.Double(14 + i * 8, 33, 3, 3));
            g.setColor(c(0x4FC3F7)); for (int i = 0; i < 5; i++) g.fillRect(15 + i * 8, 40 + (i * 7) % 8, 2, 6);
        });
    }

    static final Spr[] MUG_COC = new Spr[2], TIR_COC = new Spr[2];
    static {
        for (int f = 0; f < 2; f++) {
            final int ff = f;
            MUG_COC[f] = spr(64, 64, g -> {
                dGermen(g, 0, ff, false, false);
                g.setColor(c(0x8D6E63)); g.fillRect(50, 36, 3, 20); g.setColor(c(0xCFD8DC)); g.fill(new Ellipse2D.Double(46, 24, 11, 15));
            });
            TIR_COC[f] = spr(64, 64, g -> {
                dGermen(g, 1, ff, false, false);
                g.setColor(c(0x78909C)); g.fill(new Ellipse2D.Double(12, 3, 40, 11)); g.setColor(c(0xB0BEC5)); g.fill(new Ellipse2D.Double(14, 3, 36, 7));
                g.setColor(c(0x455A64)); g.fill(new Ellipse2D.Double(28, 0, 8, 5));
            });
        }
    }
}
