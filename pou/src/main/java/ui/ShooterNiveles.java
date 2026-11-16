package ui;

import static ui.ShooterArte.*;

/** Datos de los 5 capitulos de "Operacion Mugre": mapas, texturas, textos y parametros. */
final class ShooterNiveles {
    private ShooterNiveles() {}

    static final class Nivel {
        int num;
        String titulo, sub, jefe;
        String[] mapa, intro;
        int[] paredes = new int[10];
        int texPuerta = T_PUERTA, texCandado, texExitOff, texExitOn;
        int[] secretTex = new int[3];
        int[][] regiones;           // x0, y0, x1, y1, piso, techo
        int pisoDef, techoDef;
        double luzBase = 1.2, luzK = 0.055, luzMin = 0.42;
        boolean niebla, requiereLinterna, disfraz;
        int guardiaX = -1;
        double angIni = 0;
        int[] agua;                 // x0, y0, x1, y1 (sala inundada) o null
        int par, monedas, llave = -1, coronaBase;
        String msgLlave = "", msgLlaveOk = "", msgPuertaOk = "", msgSalidaOff = "", msgJefe = "", msgOscuro = "";
        String gancho = "", proximo = "";
    }

    static final int TOTAL = 5;
    private static final Nivel[] NIVELES = new Nivel[TOTAL + 1];

    static Nivel get(int n) { return NIVELES[n]; }

    private static int[] paredes(int... v) { int[] p = new int[10]; for (int i = 0; i < v.length; i++) p[i + 1] = v[i]; return p; }

    static {
        Nivel n = new Nivel();
        n.num = 1; n.titulo = "La Casa Invadida"; n.sub = "Sala, pasillo, dormitorio y baño"; n.jefe = "REY MUGRE";
        n.mapa = new String[]{
            "2222222222222555533333333333335555",
            "2...........2...5............35555",
            "2.777777777.2...5..99999999.j35555",
            "2...........2.e.5..99999999..35555",
            "2..g........2...5..........t.35555",
            "2.....e..g..D...K..t.........X5555",
            "2...77777...2...5...9..R..9..35555",
            "2...77777...2...5............35555",
            "2...........2.g.5..g.......g.35555",
            "2.te.g......2...5.b.9.....9..35555",
            "2.b.......j.2...5............35555",
            "222222D222222...533333333333335555",
            "1...........5.b.5............45555",
            "1.j.....g...1...5.8888......e45555",
            "1..66666....1...5.8888....g..45555",
            "1...........1.g.5.8888.......44444",
            "1........g..1...5............4.e.4",
            "1........e..1...5.....g....t.S.j.4",
            "1......77...D...D............4..e4",
            "1......77...1...5..g..e......44444",
            "1..P......g.1..e5............45555",
            "1......e...b1...5.b.....t..k.45555",
            "1...........1...5...........j45555",
            "1111111111111555544444444444445555"
        };
        n.paredes = paredes(1, 2, 3, 4, 5, 6, 7, 8, 9);
        n.texCandado = T_CANDADO_ORO; n.texExitOff = T_DUCHA_OFF; n.texExitOn = T_DUCHA_ON;
        n.secretTex = new int[]{4, 4, 4};
        n.pisoDef = 4; n.techoDef = 0;
        n.regiones = new int[][]{{0, 0, 12, 11, 2, 0}, {0, 12, 12, 99, 1, 0}, {30, 0, 99, 99, 4, 0}, {13, 0, 15, 99, 5, 0}, {16, 0, 29, 11, 3, 0}};
        n.par = 270; n.monedas = 5; n.llave = 0; n.coronaBase = -1;
        n.intro = new String[]{
            "¡Pou volvió a casa y está todo cubierto de mugre!",
            "El Rey Mugre y sus gérmenes se metieron mientras nadie limpiaba.",
            "Con el Burbujero 3000 los gérmenes se limpian y se van flotando:",
            "¡nadie sale lastimado! Buscá la llave dorada, abrí la puerta del baño",
            "y encendé la ducha mágica (tecla E) para terminar el capítulo."};
        n.msgLlave = "Está cerrada con candado. Buscá la llave dorada.";
        n.msgLlaveOk = "¡Conseguiste la llave dorada!";
        n.msgPuertaOk = "¡Abriste la puerta del baño!";
        n.msgSalidaOff = "La ducha mágica está apagada. ¡Limpiá al Rey Mugre primero!";
        n.msgJefe = "¡Limpiaste al Rey Mugre! La ducha mágica se encendió: andá y apretá E";
        n.gancho = "La corona del Rey se partió en tres... y desde la cocina se oye un estruendo de ollas.";
        n.proximo = "Próximamente: Capítulo 2 - La Cocina Caótica";
        NIVELES[1] = n;

        n = new Nivel();
        n.num = 2; n.titulo = "La Cocina Caótica"; n.sub = "Comedor, cocina y despensa"; n.jefe = "CHEF GRASÓN";
        n.mapa = new String[]{
            "22222222222222222222222222222222222222222222222222222222",
            "22222222222222222222222222222222222222222222222222222222",
            "22222222222222111111111111111111222222222222222222222222",
            "22..........g.1.j2222eee22222.b1222222222222222222222222",
            "22e..2222..2..1..22222Z222222..1222222222222222222222222",
            "22...2222.....1.e.........o....5222222222222222222222222",
            "22.........m..D....t.......t...5222222222222222222222222",
            "22.....b......1......2222......1222222222222222222222222",
            "22..2..2..2...1.............m..52222222222221F11111F1222",
            "22..........t.1...g...bo.....g.12222222222211.....j.1122",
            "22j...e..g...j1...............j1222222222211.........112",
            "222D2222S2222233333333D33333331122222222211b........b.11",
            "22....22j222223.e...........b3222222222221.............1",
            "22....22j222223..............3222222222221.............1",
            "22..g.222222223.3333333333...3222222222221.....22......1",
            "22....222222223.............t3222222222221.....22..G...X",
            "22.P..222222eBY.....m.....g..4444444444444.....22......1",
            "22...b222222223...3333333333.4...b...g..B4.............1",
            "222222222222223..g........e..D.....o.....K.............1",
            "222222222222223k.....j......b4........e..41b........b.11",
            "22222222222222333333333333333444444444444411.........112",
            "222222222222222222222222222222222222222222211.j.....1122",
            "222222222222222222222222222222222222222222221F11111F1222",
            "22222222222222222222222222222222222222222222222222222222",
            "22222222222222222222222222222222222222222222222222222222"
        };
        n.paredes = paredes(2, 7, T_DESPENSA, T_ESCARCHA, T_HORN_ON);
        n.texCandado = T_CANDADO_PLATA; n.texExitOff = T_ALACENA_OFF; n.texExitOn = T_ALACENA_ON;
        n.secretTex = new int[]{T_HELADERITA, T_DESPENSA, T_MESADA_PUERTITA};
        n.pisoDef = 2; n.techoDef = 1; n.disfraz = true;
        n.regiones = new int[][]{{0, 0, 14, 24, 1, 1}, {15, 0, 30, 11, 2, 1}, {15, 12, 29, 24, P_DESPENSA, 1}, {30, 12, 41, 24, P_HIELO, 1}, {42, 0, 80, 24, P_HORNO, 1}};
        n.par = 300; n.monedas = 5; n.llave = 1; n.coronaBase = 0; n.angIni = -Math.PI / 2;
        n.intro = new String[]{
            "¡Un pedazo de la Corona Mugre se escapó a la cocina!",
            "El Chef Grasón se lo puso de gorro y llenó todo de grasa y migajas.",
            "Buscá la llave plateada de la despensa para abrir la heladera gigante.",
            "Ojo con los tomates rodantes: ¡se ponen rojos antes de embestir!",
            "Limpiá al Chef Grasón y recuperá el pedazo de corona."};
        n.msgLlave = "La heladera gigante tiene candado. Buscá la llave plateada en la despensa.";
        n.msgLlaveOk = "¡Conseguiste la llave plateada!";
        n.msgPuertaOk = "¡Abriste la heladera gigante!";
        n.msgSalidaOff = "La alacena está cerrada. ¡Limpiá al Chef Grasón primero!";
        n.msgJefe = "¡Limpiaste al Chef Grasón! Corona 1/3. La alacena se abrió: andá y apretá E";
        n.gancho = "“Qué desorden… subí, querido, que te estaba esperando.”";
        n.proximo = "Próximamente: Capítulo 3 - El Desván Polvoriento";
        NIVELES[2] = n;

        n = new Nivel();
        n.num = 3; n.titulo = "El Desván Polvoriento"; n.sub = "Baúles, disfraces y biblioteca"; n.jefe = "MADAME TELARAÑA";
        n.mapa = new String[]{
            "11111111111111111111111111111111111111111111111111111111",
            "11111111111111111111111111111111111111111111111111111111",
            "11111111111111111111111111111111111111111111111111111111",
            "11111111111111111111111111111111111111111111111111111111",
            "111111111111111111111111111111111111J1111111111111111111",
            "111114444444444444444444433333333333S3333333333311111111",
            "111114......e............3.b..**............b..311111111",
            "111114.g......4.........j3....**.t....e..t....k311111111",
            "111114..4...t.......4....3..333333..333333333..311111111",
            "111114.....4...l.4....4..3b...l...........a....311111111",
            "111eeYb..l**..........4..D........a............311111111",
            "111114....**..4......l...3..3333333333..33333..311111111",
            "111114..4.......a...4....3.g....e...**.l.....g.311111111",
            "111114.b...4.....4.....g.3....a....t**........j311111111",
            "111114.............e.....3.....................311111111",
            "11111444444441..144444444333333333333333333331.111111111",
            "11111111111111..111111111111111111111111111111w111111111",
            "11111111111111ww111111111111111111111111111111.111111111",
            "11111111111111..111111111111111111111111111111.111111111",
            "11111111111111..111111111111111111111111111555K555111111",
            "11111222222221..1222222222111111111111111155.....5511111",
            "111112...................211111111111111155.......551111",
            "111112b....2..e..22...t..21111111111111155b...b...b55111",
            "111112..22.......22......2111111111111155...........5511",
            "11...2..22...a..l.....22.211111111111115..c.......c..511",
            "11.b.2*...L........g.....ZBB111111111115......M......N11",
            "11...2*.....22....**22...211111111111115.............511",
            "11...D......22....**22...2111111111111155...........5511",
            "11.P.2...e.g...22......j.21111111111111155b...c...b55111",
            "11...2...................211111111111111155.......551111",
            "11111222222222222222222222111111111111111155.....5511111",
            "1111111111111111111111111111111111111111111555X555111111",
            "11111111111111111111111111111111111111111111111111111111",
            "11111111111111111111111111111111111111111111111111111111"
        };
        n.paredes = paredes(T_DESVAN, T_BAUL, T_BIBLIO, T_DISFRAZ, T_COSTURA);
        n.texCandado = T_CANDADO_HILO; n.texExitOff = T_TRAMP_OFF; n.texExitOn = T_TRAMP_ON;
        n.secretTex = new int[]{T_LIBRO, T_MUNECA, T_BAUL};
        n.pisoDef = P_DESVAN; n.techoDef = 2;
        n.regiones = new int[][]{{0, 0, 24, 15, P_ALFOMBRA, 2}, {26, 0, 47, 15, P_BIBLIO, 2}, {39, 16, 55, 33, P_COSTURA, 5}};
        n.luzBase = 0.95; n.luzK = 0.07; n.luzMin = 0.25; n.niebla = true; n.requiereLinterna = true;
        n.par = 330; n.monedas = 6; n.llave = 2; n.coronaBase = 1;
        n.intro = new String[]{
            "El segundo pedazo de la Corona está en el desván.",
            "Madame Telaraña lo usa de broche y apagó todas las luces.",
            "Buscá la llave de hilo en la biblioteca polvorienta.",
            "Las telarañas bloquean el paso: ¡reventalas con burbujas!",
            "Y no te asustes si algo se mueve en la oscuridad…"};
        n.msgLlave = "El Cuarto de Costura está atado con hilo. Buscá la llave de hilo en la biblioteca.";
        n.msgLlaveOk = "¡Conseguiste la llave de hilo!";
        n.msgPuertaOk = "¡Abriste el Cuarto de Costura!";
        n.msgOscuro = "Ahí dentro está oscuro como boca de lobo... buscá la Linterna de Burbujas en el cuarto de los baúles.";
        n.msgSalidaOff = "La trampilla está cerrada. ¡Limpiá a Madame Telaraña primero!";
        n.msgJefe = "¡Limpiaste a Madame Telaraña! Corona 2/3. La trampilla se abrió: apretá E";
        n.gancho = "“¿Ya terminaron? Yo no tengo apuro, tengo toda el agua del mundo.”";
        n.proximo = "Próximamente: Capítulo 4 - El Sótano Húmedo";
        NIVELES[3] = n;

        n = new Nivel();
        n.num = 4; n.titulo = "El Sótano Húmedo"; n.sub = "Lavadero y sala de calderas"; n.jefe = "CAPITÁN MOHO";
        n.mapa = new String[]{
            "1111111111111111111111111111111111111111111111111111",
            "1111111111111111111111111111111111111111111111111111",
            "1111111111111111111111111111111111111111111111111111",
            "1111111111111111111111111111111111111111111111111111",
            "1111111111111111111111111111111111111111V11111111111",
            "1111111111111111111111111111111111.............11111",
            "11111111111111111111e1111111111111.b....j....b.11111",
            "11111111111111111111e1111111111111.............X1111",
            "11111111111111111111e1111111111111...55...55...11111",
            "11111444444444444444Z4444444444441E..55...55...11111",
            "111114b.....t...j.q...b..E.a.d.k.1.............11111",
            "111114..dq4..d.hd....t..4d..q.b..K..........C..V1111",
            "111114..g....a...4.ld..h..g..4h..1.............11111",
            "111114.....e.....................1b..55...55...11111",
            "11111444444444D44444444444Y4444441...55...55...11111",
            "111112.................211B1111111.............11111",
            "111112.22..22.e.22..22.211B1111111.b....j....b.11111",
            "111112.22..22...22..22.21111111111.............11111",
            "111112e........q......b21111111111111111V11111111111",
            "111112...g...........t.21111111111111111111111111111",
            "111112.b..333333...g...21111111111111111111111111111",
            "111112.................21111111111111111111111111111",
            "11...2.333..l..........SJ.11111111111111111111111111",
            "11...2....q......33333.21111111111111111111111111111",
            "11.g.D.................21111111111111111111111111111",
            "11...2.22....t..b...22.21111111111111111111111111111",
            "11.P.2.22.........h.22j21111111111111111111111111111",
            "11...2.................21111111111111111111111111111",
            "1111122222222222222222221111111111111111111111111111",
            "1111111111111111111111111111111111111111111111111111"
        };
        n.paredes = paredes(T_LADRILLO, T_LAVARROPAS, T_ROPA, T_TUBOS, T_COLUMNA);
        n.texCandado = T_CANDADO_LLAVE; n.texExitOff = T_DESAGUE_OFF; n.texExitOn = T_DESAGUE_ON;
        n.secretTex = new int[]{T_LAVAR_ABIERTO, T_CALDERA, T_CANO_SUELTO};
        n.pisoDef = P_VERDE; n.techoDef = 3;
        n.regiones = new int[][]{{6, 15, 23, 28, P_LAVADERO, 3}, {33, 0, 50, 18, P_CALDERA, 3}};
        n.agua = new int[]{34, 5, 46, 17};
        n.luzBase = 1.05; n.luzK = 0.06; n.luzMin = 0.35;
        n.par = 360; n.monedas = 8; n.llave = 3; n.coronaBase = 2;
        n.intro = new String[]{
            "El último pedazo de la Corona está en el sótano.",
            "El Capitán Moho inundó la sala de calderas para que nadie lo toque.",
            "Buscá la llave inglesa en el pasillo de tuberías.",
            "Cuidado con las goteras: ¡mirá la sombra en el piso!",
            "Secá la sala girando las tres válvulas (tecla E)."};
        n.msgLlave = "La sala de calderas está trabada. Buscá la llave inglesa en el pasillo de tuberías.";
        n.msgLlaveOk = "¡Conseguiste la llave inglesa!";
        n.msgPuertaOk = "¡Abriste la sala de calderas!";
        n.msgSalidaOff = "El desagüe está cerrado. ¡Limpiá al Capitán Moho primero!";
        n.msgJefe = "¡Limpiaste al Capitán Moho! Corona 3/3. El desagüe se abrió: apretá E";
        n.gancho = "La corona armada vibra... el desagüe se abre con un “¡GLUP!” y una voz profunda dice: “Gracias por traerme mi corona…”";
        n.proximo = "Próximamente: Capítulo 5 - El Gran Desagüe";
        NIVELES[4] = n;

        n = new Nivel();
        n.num = 5; n.titulo = "El Gran Desagüe"; n.sub = "Túnel, laguna y trono final"; n.jefe = "LA MANCHA MADRE";
        n.mapa = new String[]{
            "111111111111111111111111111111111111111111111111111111111111111111111111111111",
            "1111111111111111111111111111111111111111Q1111111111111111111111111111111111111",
            "1111111111111111111111111111111111111111J1111111111111111111111111111111111111",
            "1111111111111111111111111111222222222222S2222222222211111111111111111111111111",
            "11111111111111111111111111112......................211111111111111111111111111",
            "11111111111111111111111111112.b......e.....2.....b.211111111113333333331111111",
            "11111111111111111111111111112.g.....t........222...211111111133.......33111111",
            "11111111111111111111111111112...222.....n....222...21111111133....b....3311111",
            "11111111111111111111111111112...222.......j..222...2111111133.b.......b.331111",
            "111111e1111111111111111111112...222...2222.......a.211111133.............33111",
            "111111e1111111111111111111112....l....2222...q....e21111133...............3311",
            "111111e1111111111111111111112.22......2222.........4444444......b..........311",
            "144444Z4444444444444444444442.................222..4.J.J.4...............33311",
            "14..b....j.r.....u..r..b.......e....b......o.m222........4.b.............33311",
            "14P.....n..r...n....r...b.E........g....a........e...b.b.A........U...W..33311",
            "14...u.....r.b....e.r....j......b...........b............4.b..............YBB1",
            "14444444444444444444444444442.....u............t...4..E..4.................311",
            "11111111111111111111111111112........l...2222....h.444444H..........b......311",
            "11111111111111111111111111112....222....b2222......21111133...............3311",
            "11111111111111111111111111112....222...e.2222......211111133.............33111",
            "11111111111111111111111111112..o.222............22.2111111133.b.......b.331111",
            "11111111111111111111111111112....e..22..........22.21111111133....b....3311111",
            "11111111111111111111111111112......j22h.....g.u.22.211111111133.......33111111",
            "11111111111111111111111111112.b.............e....b.211111111113333333331111111",
            "11111111111111111111111111112......................211111111111111111111111111",
            "111111111111111111111111111122222222222222222222222211111111111111111111111111",
            "111111111111111111111111111111111111111111111111111111111111111111111111111111",
            "111111111111111111111111111111111111111111111111111111111111111111111111111111",
            "111111111111111111111111111111111111111111111111111111111111111111111111111111",
            "111111111111111111111111111111111111111111111111111111111111111111111111111111"
        };
        n.paredes = paredes(T_OXIDO, T_ESPUMA, T_TRONO, T_LADRILLO, T_TUBOS);
        n.secretTex = new int[]{T_ESPUMA, T_TRONO_ATRAS, T_CANO_ROTO};
        n.pisoDef = P_REJA; n.techoDef = 4;
        n.regiones = new int[][]{{29, 0, 51, 29, P_ESPUMA, 4}, {52, 0, 57, 29, P_CALDERA, 4}, {58, 0, 90, 29, P_TRONO, 4}};
        n.luzBase = 1.1; n.luzK = 0.06; n.luzMin = 0.35; n.guardiaX = 29;
        n.par = 420; n.monedas = 12; n.llave = -1; n.coronaBase = 3;
        n.intro = new String[]{
            "La corona armada despertó a la Mancha Madre.",
            "Todos los gérmenes de la casa volvieron a su llamado.",
            "Cruzá el desagüe y abrí la puerta de las tres cerraduras.",
            "Usá la Ducha Maestra (tecla E) cuando la Mancha esté justo abajo.",
            "Es la última limpieza: ¡dejá la casa brillando!"};
        n.msgJefe = "¡La Mancha Madre quedó limpia!";
        n.gancho = "“…solo quería que alguien me limpiara con cariño.”";
        n.proximo = "";
        NIVELES[5] = n;
    }
}
