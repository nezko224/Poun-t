package service;

import dao.AlimentoDao;
import dao.ConexionBD;
import dao.MedicamentoDao;
import dao.MonedaDao;
import dao.PouDao;
import dao.PrendaDao;
import dao.impl.AlimentoDaoImpl;
import dao.impl.MedicamentoDaoImpl;
import dao.impl.MonedaDaoImpl;
import dao.impl.PouDaoImpl;
import dao.impl.PrendaDaoImpl;
import exception.AccionNoPermitidaException;
import exception.SaldoInsuficienteException;
import model.Alimento;
import model.ComidaChatarra;
import model.Fruta;
import model.Medicamento;
import model.Moneda;
import model.MonedaComun;
import model.MonedaEspecial;
import model.Pou;
import model.Prenda;
import model.Vegetal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PouService {

    public static final double SALDO_INICIAL = 15;
    public static final double PROBABILIDAD_ESPECIAL = 0.30;


    public static final long TIEMPO_ENFERMO_MAX_MS = 2 * 60 * 1000L;
    private static final int BONUS_BANO = 40;

    private final PouDao pouDao = new PouDaoImpl();
    private final AlimentoDao alimentoDao = new AlimentoDaoImpl();
    private final MedicamentoDao medicamentoDao = new MedicamentoDaoImpl();
    private final PrendaDao prendaDao = new PrendaDaoImpl();
    private final MonedaDao monedaDao = new MonedaDaoImpl();

    private boolean dormidoForzado;
    private long enfermoDesde;        
    private boolean avisoMitadDado;



    public boolean hayConexion() {
        try (Connection con = ConexionBD.conexionBd()) {
            return true;
        } catch (SQLException e) {
            System.out.println("Detalle: " + e.getMessage());
            return false;
        }
    }


    public void cargarCatalogoInicial() {
        if (alimentoDao.listarTodo().isEmpty()) {
            alimentoDao.crear(new Fruta("Manzana", 2, 10, 5));
            alimentoDao.crear(new Fruta("Banana", 3, 15, 3));
            alimentoDao.crear(new Vegetal("Zanahoria", 3, 10, 2.0));
            alimentoDao.crear(new Vegetal("Brocoli", 4, 12, 2.5));
            alimentoDao.crear(new ComidaChatarra("Papas fritas", 1, 20, 8));
            alimentoDao.crear(new ComidaChatarra("Hamburguesa", 2, 30, 12));
        }
        if (medicamentoDao.listarTodo().isEmpty()) {
            medicamentoDao.crear(new Medicamento("Jarabe", 6, 50));
            medicamentoDao.crear(new Medicamento("Pastilla", 10, 100));
        }
    }


    public Pou nuevoPou(String nombre) {
        Pou pou = new Pou(nombre);
        pou.setSaldo(SALDO_INICIAL);
        pouDao.crear(pou);
        reiniciarEstadoDeSesion();
        return pou;
    }

    public List<Pou> listarPousVivos() {
        List<Pou> vivos = new ArrayList<>();
        for (Pou p : pouDao.listarTodo()) {
            if (p.getEstado() != Pou.EstadoPou.MUERTO) {
                vivos.add(p);
            }
        }
        return vivos;
    }

    public Pou cargarPou(Pou pou) {
        reiniciarEstadoDeSesion();
        if (pou.getEstado() == Pou.EstadoPou.DORMIDO) {
            pou.setEstado(Pou.EstadoPou.SANO);
        }
        if (pou.getEstado() == Pou.EstadoPou.ENFERMO) {
            iniciarEnfermedad(); 
        }
        return pou;
    }

    public void guardar(Pou pou) {
        pouDao.actualizar(pou);
    }

    private void reiniciarEstadoDeSesion() {
        dormidoForzado = false;
        enfermoDesde = 0;
        avisoMitadDado = false;
    }


    private void iniciarEnfermedad() {
        enfermoDesde = System.currentTimeMillis();
        avisoMitadDado = false;
    }

    private void terminarEnfermedad() {
        enfermoDesde = 0;
        avisoMitadDado = false;
    }

   
    public long segundosRestantesEnfermo(Pou pou) {
        if (pou.getEstado() != Pou.EstadoPou.ENFERMO || enfermoDesde == 0) {
            return 0;
        }
        long restante = TIEMPO_ENFERMO_MAX_MS - (System.currentTimeMillis() - enfermoDesde);
        return Math.max(0, restante / 1000);
    }



    public List<Alimento> listarAlimentos() {
        return alimentoDao.listarTodo();
    }

    public List<Medicamento> listarMedicamentos() {
        return medicamentoDao.listarTodo();
    }

    public List<Prenda> catalogoPrendas() {
        List<Prenda> catalogo = new ArrayList<>();
        catalogo.add(new Prenda("Gorra", Prenda.TipoConjunto.CONJUNTO_1, 15));
        catalogo.add(new Prenda("Remera", Prenda.TipoConjunto.CONJUNTO_2, 25));
        catalogo.add(new Prenda("Zapatillas", Prenda.TipoConjunto.CONJUNTO_3, 40));
        catalogo.add(new Prenda("Anteojos", Prenda.TipoConjunto.CONJUNTO_1, 20));
        return catalogo;
    }

    public List<Prenda> listarPrendas(Pou pou) {
        return prendaDao.listarPorPou(pou.getId());
    }

    
    public String comprarAlimento(Pou pou, Alimento alimento) {
        validarDespierto(pou);
        if (pou.getHambre() >= 100) {
            throw new AccionNoPermitidaException(pou.getNombre() + " esta lleno, no quiere comer.");
        }
        cobrar(pou, alimento.getPrecio());
        int hambreAntes = pou.getHambre();
        alimento.aplicarEfecto(pou);
        String msg = pou.getNombre() + " comio " + alimento.getNombre() + " (hambre "
                + hambreAntes + " -> " + pou.getHambre() + ").";
        msg += revisarSuciedad(pou);
        guardar(pou);
        return msg;
    }

    public String banar(Pou pou) {
        validarDespierto(pou);
        if (pou.getEstado() == Pou.EstadoPou.ENFERMO) {
            throw new AccionNoPermitidaException(pou.getNombre() + " esta enfermo: necesita un medicamento, un baño no alcanza.");
        }
        if (pou.getHigiene() >= 100) {
            throw new AccionNoPermitidaException(pou.getNombre() + " ya esta limpito.");
        }
        int antes = pou.getHigiene();
        pou.setHigiene(antes + BONUS_BANO);
        guardar(pou);
        return pou.getNombre() + " se dio un baño (higiene " + antes + " -> " + pou.getHigiene() + ").";
    }

    public String dormir(Pou pou) {
        validarDespierto(pou);
        if (pou.getEstado() == Pou.EstadoPou.ENFERMO) {
            throw new AccionNoPermitidaException(pou.getNombre() + " esta muy enfermo para dormir.");
        }
        if (pou.getSueño() >= 90) {
            throw new AccionNoPermitidaException(pou.getNombre() + " no tiene sueño.");
        }
        pou.setEstado(Pou.EstadoPou.DORMIDO);
        dormidoForzado = false;
        guardar(pou);
        return pou.getNombre() + " se fue a dormir. Zzz...";
    }

    public String despertar(Pou pou) {
        if (pou.getEstado() != Pou.EstadoPou.DORMIDO) {
            throw new AccionNoPermitidaException(pou.getNombre() + " no esta durmiendo.");
        }
        if (dormidoForzado && pou.getSueño() < 50) {
            throw new AccionNoPermitidaException(pou.getNombre() + " se durmio de cansancio, todavia no lo podes despertar.");
        }
        pou.setEstado(Pou.EstadoPou.SANO);
        dormidoForzado = false;
        guardar(pou);
        return pou.getNombre() + " se desperto.";
    }

    public String comprarMedicamento(Pou pou, Medicamento medicamento) {
        validarDespierto(pou);
        if (pou.getEstado() != Pou.EstadoPou.ENFERMO) {
            throw new AccionNoPermitidaException(pou.getNombre() + " no esta enfermo, no necesita medicamentos.");
        }
        if (pou.getHambre() == 0) {
            throw new AccionNoPermitidaException(pou.getNombre() + " tiene hambre 0: primero dale de comer, el medicamento solo no alcanza.");
        }
        cobrar(pou, medicamento.getPrecio());
        medicamento.aplicarEfecto(pou);

        String msg;
        if (pou.getEstado() == Pou.EstadoPou.ENFERMO && pou.getHigiene() >= 50) {
            pou.setEstado(Pou.EstadoPou.SANO);
        }
        if (pou.getEstado() == Pou.EstadoPou.SANO) {
            terminarEnfermedad();
            msg = pou.getNombre() + " se curo con " + medicamento.getNombre() + "!";
        } else {
            msg = medicamento.getNombre() + " no alcanzo, " + pou.getNombre() + " sigue enfermo.";
        }
        guardar(pou);
        return msg;
    }

    public String comprarPrenda(Pou pou, Prenda prenda) {
        validarDespierto(pou);
        for (Prenda tiene : listarPrendas(pou)) {
            if (tiene.getNombre().equals(prenda.getNombre())) {
                throw new AccionNoPermitidaException("Ya tenes " + prenda.getNombre() + ".");
            }
        }
        cobrar(pou, prenda.getPrecio());
        prendaDao.crear(prenda, pou.getId());
        guardar(pou);
        return "Compraste " + prenda.getNombre() + "! Fijate en 'Mis prendas' para ponerla.";
    }


    public String alternarPrenda(Pou pou, Prenda prenda) {
        validarDespierto(pou);
        if (prenda.isEstaPuesta()) {
            prenda.setEstaPuesta(false);
            prendaDao.actualizar(prenda);
            return "Le sacaste " + prenda.getNombre() + ".";
        }
        for (Prenda otra : listarPrendas(pou)) {
            if (otra.getTipo() == prenda.getTipo() && otra.isEstaPuesta()) {
                otra.setEstaPuesta(false);
                prendaDao.actualizar(otra);
            }
        }
        prenda.aplicarEfecto(pou);
        prendaDao.actualizar(prenda);
        return "Le pusiste " + prenda.getNombre() + ".";
    }



    public Moneda generarMoneda(Pou pou) {
        int x = ThreadLocalRandom.current().nextInt(0, 100);
        int y = ThreadLocalRandom.current().nextInt(0, 100);

        Moneda moneda;
        if (ThreadLocalRandom.current().nextDouble() < PROBABILIDAD_ESPECIAL) {
            moneda = new MonedaEspecial(x, y, PROBABILIDAD_ESPECIAL);
        } else {
            moneda = new MonedaComun(x, y);
        }
        monedaDao.crear(moneda, pou.getId());
        return moneda;
    }

    public void recolectarMoneda(Pou pou, Moneda moneda) {
        moneda.setRecolectada(true);
        pou.setSaldo(pou.getSaldo() + moneda.getValor());
        monedaDao.marcarRecolectada(moneda.getId());
        guardar(pou);
    }


    public List<String> pasarTiempo(Pou pou) {
        List<String> avisos = new ArrayList<>();
        if (pou.getEstado() == Pou.EstadoPou.MUERTO) {
            return avisos;
        }

        String nombre = pou.getNombre();
        int hambreAntes = pou.getHambre();
        int higieneAntes = pou.getHigiene();
        int suenoAntes = pou.getSueño();

        if (pou.getEstado() == Pou.EstadoPou.DORMIDO) {
            pou.setHambre(pou.getHambre() - 1);
            pou.setSueño(pou.getSueño() + 6);
            boolean descansado = pou.getSueño() >= 100 || (dormidoForzado && pou.getSueño() >= 50);
            if (descansado) {
                pou.setEstado(Pou.EstadoPou.SANO);
                dormidoForzado = false;
                avisos.add(nombre + " se desperto descansado.");
            }
        } else {
            pou.setHambre(pou.getHambre() - 2);
            pou.setHigiene(pou.getHigiene() - 1);
            pou.setSueño(pou.getSueño() - 1);
        }

        if (pou.getEstado() == Pou.EstadoPou.SANO && (pou.getHambre() == 0 || pou.getHigiene() == 0)) {
            pou.setEstado(Pou.EstadoPou.ENFERMO);
            iniciarEnfermedad();
            if (pou.getHambre() == 0) {
                avisos.add(nombre + " se enfermo por hambre! Tenes 2 minutos: dale de comer y compra un medicamento en la farmacia.");
            } else {
                avisos.add(nombre + " se enfermo por falta de higiene! Tenes 2 minutos para comprar un medicamento en la farmacia.");
            }
        } else if (pou.getEstado() == Pou.EstadoPou.ENFERMO) {
            if (enfermoDesde == 0) {
                iniciarEnfermedad();   
            }
            long transcurrido = System.currentTimeMillis() - enfermoDesde;
            if (transcurrido >= TIEMPO_ENFERMO_MAX_MS) {
                pou.setEstado(Pou.EstadoPou.MUERTO);
                avisos.add(nombre + " murio por no recibir medicamento a tiempo.");
                return avisos;
            }
            if (!avisoMitadDado && transcurrido >= TIEMPO_ENFERMO_MAX_MS / 2) {
                avisoMitadDado = true;
                avisos.add(nombre + " esta empeorando! Le queda 1 minuto, necesita un medicamento YA.");
            }
        }


        if (pou.getEstado() == Pou.EstadoPou.SANO && pou.getSueño() == 0) {
            pou.setEstado(Pou.EstadoPou.DORMIDO);
            dormidoForzado = true;
            avisos.add(nombre + " se durmio de cansancio!");
        }

  
        if (hambreAntes > 20 && pou.getHambre() <= 20) {
            avisos.add(nombre + " tiene mucha hambre!");
        }
        if (higieneAntes > 20 && pou.getHigiene() <= 20) {
            avisos.add(nombre + " esta muy sucio!");
        }
        if (suenoAntes > 20 && pou.getSueño() <= 20 && pou.getEstado() != Pou.EstadoPou.DORMIDO) {
            avisos.add(nombre + " tiene mucho sueño!");
        }

        return avisos;
    }

    private void validarDespierto(Pou pou) {
        if (pou.getEstado() == Pou.EstadoPou.MUERTO) {
            throw new AccionNoPermitidaException(pou.getNombre() + " esta muerto.");
        }
        if (pou.getEstado() == Pou.EstadoPou.DORMIDO) {
            throw new AccionNoPermitidaException(pou.getNombre() + " esta durmiendo.");
        }
    }

    private void cobrar(Pou pou, double precio) {
        if (pou.getSaldo() < precio) {
            throw new SaldoInsuficienteException(precio, pou.getSaldo());
        }
        pou.setSaldo(pou.getSaldo() - precio);
    }


    private String revisarSuciedad(Pou pou) {
        if (pou.getEstado() == Pou.EstadoPou.SANO && pou.getHigiene() == 0) {
            pou.setEstado(Pou.EstadoPou.ENFERMO);
            iniciarEnfermedad();
            return " Pero quedo con higiene 0 y se enfermo! Tenes 2 minutos para comprar un medicamento.";
        }
        return "";
    }
}
