package com.umg.sigelab.view;

import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.model.Usuario;
import com.umg.sigelab.util.SesionActual;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Ventana principal. El menu se arma segun el rol del usuario (6.2).

 * COMO AGREGAR UN MODULO (para el equipo):
 *   1. Crear su panel en el paquete view (ej. EquiposPanel extends JPanel).
 *   2. En el metodo crearModulo(...) de abajo, descomentar/agregar la linea "case MOD_XXX -> new XxxPanel();".
 *   El menu y los permisos por rol ya estan listos. Hagan "Pull" antes de editar este archivo.

 * Recordatorio: ocultar un menu no es seguridad. Cada Service debe llamar a
 * AutorizacionService.exigirRol(...) (RN-20).
 */
public class MainFrame extends JFrame {

    public static final String MOD_USUARIOS = "Usuarios";
    public static final String MOD_LABORATORIOS = "Laboratorios";
    public static final String MOD_TIPOS = "Tipos de equipo";
    public static final String MOD_EQUIPOS = "Equipos";
    public static final String MOD_PRESTAMOS = "Préstamos";
    public static final String MOD_DEVOLUCIONES = "Devoluciones";
    public static final String MOD_MANTENIMIENTO = "Mantenimiento";
    public static final String MOD_REPORTES = "Reportes";
    public static final String MOD_AUDITORIA = "Auditoría";

    private static final NombreRol[] SOLO_ADMIN = {NombreRol.ADMIN};
    private static final NombreRol[] ADMIN_ENCARGADO = {NombreRol.ADMIN, NombreRol.ENCARGADO};
    private static final NombreRol[] TODOS = {NombreRol.ADMIN, NombreRol.ENCARGADO, NombreRol.DOCENTE};

    private final Usuario usuario = SesionActual.getUsuario();
    private final JPanel contenido = new JPanel(new BorderLayout());

    public MainFrame() {
        super("SIGELAB - Sistema de Gestión de Laboratorios");
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        setJMenuBar(construirMenu());
        add(construirEncabezado(), BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
        mostrarBienvenida();
    }

    // ---------------------------------------------------------------
    // PUNTO DE INTEGRACION: aqui se conectan los paneles de cada modulo
    // ---------------------------------------------------------------

    private JComponent crearModulo(String modulo) {

        return switch (modulo) {

            case MOD_USUARIOS ->
                    new UsuarioForm()
                            .getPanelPrincipal();

            case MOD_LABORATORIOS ->
                    new LaboratorioForm()
                            .getPanelPrincipal();

            case MOD_TIPOS ->
                    new TipoEquipoForm()
                            .getPanelPrincipal();

            case MOD_EQUIPOS ->
                    new EquipoForm()
                            .getPanelPrincipal();

            case MOD_PRESTAMOS ->
                    new PrestamoForm()
                            .getPanelPrincipal();

            case MOD_DEVOLUCIONES ->
                    new DevolucionForm()
                            .getPanelPrincipal();

            case MOD_MANTENIMIENTO ->
                    new MantenimientoForm()
                            .getPanelPrincipal();

            default ->
                    panelEnConstruccion(modulo);
        };
    }

    // ---------------------------------------------------------------
    // Menu por rol
    // ---------------------------------------------------------------
    private JMenuBar construirMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem cerrar = new JMenuItem("Cerrar sesión");
        cerrar.addActionListener(e -> cerrarSesion());
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(e -> System.exit(0));
        archivo.add(cerrar);
        archivo.add(salir);
        barra.add(archivo);

        JMenu catalogos = new JMenu("Catálogos");
        agregarModulo(catalogos, MOD_USUARIOS, SOLO_ADMIN);
        agregarModulo(catalogos, MOD_LABORATORIOS, SOLO_ADMIN);
        agregarModulo(catalogos, MOD_TIPOS, SOLO_ADMIN);
        agregarModulo(catalogos, MOD_EQUIPOS, ADMIN_ENCARGADO);
        agregarSiTieneOpciones(barra, catalogos);

        JMenu operaciones = new JMenu("Operaciones");
        agregarModulo(operaciones, MOD_PRESTAMOS, ADMIN_ENCARGADO);
        agregarModulo(operaciones, MOD_DEVOLUCIONES, ADMIN_ENCARGADO);
        agregarModulo(operaciones, MOD_MANTENIMIENTO, ADMIN_ENCARGADO);
        agregarSiTieneOpciones(barra, operaciones);

        JMenu consultas = new JMenu("Consultas");
        agregarModulo(consultas, MOD_REPORTES, TODOS);
        agregarModulo(consultas, MOD_AUDITORIA, SOLO_ADMIN);
        agregarSiTieneOpciones(barra, consultas);

        return barra;
    }

    private void agregarModulo(JMenu menu, String modulo, NombreRol... rolesPermitidos) {
        if (!usuario.tieneRol(rolesPermitidos)) {
            return;
        }
        JMenuItem item = new JMenuItem(modulo);
        item.addActionListener(e -> mostrar(crearModulo(modulo)));
        menu.add(item);
    }

    private void agregarSiTieneOpciones(JMenuBar barra, JMenu menu) {
        if (menu.getItemCount() > 0) {
            barra.add(menu);
        }
    }

    // ---------------------------------------------------------------
    // Contenido
    // ---------------------------------------------------------------
    private JPanel construirEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JLabel lblUsuario = new JLabel("Usuario: " + usuario.getNombreCompleto()
                + "  |  Rol: " + usuario.getRol().getNombre(), SwingConstants.RIGHT);
        encabezado.add(lblUsuario, BorderLayout.EAST);
        JLabel lblTitulo = new JLabel("SIGELAB");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 16f));
        encabezado.add(lblTitulo, BorderLayout.WEST);
        return encabezado;
    }

    private void mostrarBienvenida() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel("Bienvenido, " + usuario.getNombre()
                + ". Seleccione una opción del menú.", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(16f));
        p.add(l, BorderLayout.CENTER);
        mostrar(p);
    }

    private void mostrar(JComponent panel) {
        contenido.removeAll();
        contenido.add(panel, BorderLayout.CENTER);
        contenido.revalidate();
        contenido.repaint();
    }

    private JComponent panelEnConstruccion(String modulo) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel("Módulo \"" + modulo + "\" en construcción.", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 15f));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private void cerrarSesion() {
        SesionActual.cerrar();
        Inicio_de_sesion.abrir();
        dispose();
    }
}
