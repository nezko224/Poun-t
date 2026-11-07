package ui;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Efectos de sonido sintetizados (sin archivos). Si no hay audio, no pasa nada. */
public final class Sonido {
    private static volatile boolean activo = true;
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sonido");
        t.setDaemon(true);
        return t;
    });

    private Sonido() { }

    public static boolean isActivo() { return activo; }
    public static void setActivo(boolean a) { activo = a; }

    public static void tono(double f0, double f1, int ms, double vol) {
        if (!activo) return;
        HILO.execute(() -> {
            try {
                float rate = 22050f;
                int n = (int) (rate * ms / 1000);
                byte[] buf = new byte[n * 2];
                double fase = 0;
                for (int i = 0; i < n; i++) {
                    double t = (double) i / n;
                    double f = f0 + (f1 - f0) * t;
                    fase += 2 * Math.PI * f / rate;
                    double env = Math.min(1, t * 20) * Math.pow(1 - t, 1.5);
                    short v = (short) (Math.sin(fase) * env * vol * 12000);
                    buf[2 * i] = (byte) (v & 0xff);
                    buf[2 * i + 1] = (byte) ((v >> 8) & 0xff);
                }
                AudioFormat fmt = new AudioFormat(rate, 16, 1, true, false);
                Clip clip = AudioSystem.getClip();
                clip.open(fmt, buf, 0, buf.length);
                clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.start();
            } catch (Throwable ignorado) {
                activo = false; // sin placa de audio: se apaga solo
            }
        });
    }

    public static void moneda() { tono(880, 1320, 140, 0.6); }
    public static void comer() { tono(300, 180, 110, 0.7); }
    public static void boing() { tono(180, 420, 160, 0.6); }
    public static void error() { tono(220, 140, 200, 0.6); }
    public static void pop() { tono(500, 900, 70, 0.5); }
    public static void golpe() { tono(330, 200, 80, 0.7); }
    public static void punto() { tono(520, 780, 220, 0.6); }
    public static void ganar() { tono(500, 1000, 450, 0.6); }
    public static void perder() { tono(400, 120, 450, 0.6); }
    public static void curar() { tono(600, 1200, 300, 0.6); }
    public static void magia() { tono(400, 1600, 500, 0.5); }
}
