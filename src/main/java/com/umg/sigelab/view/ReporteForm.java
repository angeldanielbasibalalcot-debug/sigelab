package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.umg.sigelab.controller.ReporteController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.ConteoEstado;
import com.umg.sigelab.model.FilaHistorialEquipo;
import com.umg.sigelab.model.FilaHistorialMantenimiento;
import com.umg.sigelab.model.FilaPrestamo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de reportes (6.11, RN-17, CP-18).
 * No contiene reglas de negocio ni SQL: solo captura filtros y muestra
 * lo que devuelve el Controller.
 */
public class ReporteForm {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String R_POR_ESTADO = "Equipos por estado";
    private static final String R_ACTIVOS = "Pr\u00e9stamos activos (no vencidos)";
    private static final String R_FINALIZADOS = "Pr\u00e9stamos finalizados";
    private static final String R_VENCIDOS = "Pr\u00e9stamos vencidos";
    private static final String R_ENTRE_FECHAS = "Pr\u00e9stamos entre fechas";
    private static final String R_HIST_EQUIPO = "Historial de pr\u00e9stamos de un equipo";
    private static final String R_HIST_MANT = "Historial de mantenimiento";

    // Componentes enlazados al archivo ReporteForm.form
    private JPanel panelPrincipal;
    private JComboBox<String> cmbReporte;
    private JTextField txtDesde;
    private JTextField txtHasta;
    private JTextField txtCodigo;
    private JButton btnConsultar;
    private JButton btnLimpiar;
    private JTable tblResultados;
    private JLabel lblTotal;

    private final ReporteController controller = new ReporteController();

    public ReporteForm() {
        configurarCombo();
        configurarEventos();
        actualizarCampos();
    }

    private void configurarCombo() {
        cmbReporte.setModel(new DefaultComboBoxModel<>(new String[]{
                R_POR_ESTADO, R_ACTIVOS, R_FINALIZADOS, R_VENCIDOS,
                R_ENTRE_FECHAS, R_HIST_EQUIPO, R_HIST_MANT}));
        cmbReporte.setSelectedIndex(0);
    }

    private void configurarEventos() {
        cmbReporte.addActionListener(e -> actualizarCampos());
        btnConsultar.addActionListener(e -> ejecutar(this::consultar));
        btnLimpiar.addActionListener(e -> limpiar());
    }

    /**
     * Solo comodidad visual: habilita los filtros que usa cada reporte.
     */
    private void actualizarCampos() {
        String reporte = (String) cmbReporte.getSelectedItem();
        boolean usaFechas = R_ENTRE_FECHAS.equals(reporte);
        boolean usaCodigo = R_HIST_EQUIPO.equals(reporte) || R_HIST_MANT.equals(reporte);
        txtDesde.setEnabled(usaFechas);
        txtHasta.setEnabled(usaFechas);
        txtCodigo.setEnabled(usaCodigo);
    }

    private void consultar() {
        String reporte = (String) cmbReporte.getSelectedItem();
        if (reporte == null) {
            aviso("Seleccione un reporte.");
            return;
        }
        switch (reporte) {
            case R_POR_ESTADO -> mostrarEquiposPorEstado();
            case R_ACTIVOS -> mostrarPrestamos(controller.listarPrestamos("ACTIVO"));
            case R_FINALIZADOS -> mostrarPrestamos(controller.listarPrestamos("FINALIZADO"));
            case R_VENCIDOS -> mostrarPrestamos(controller.listarPrestamos("VENCIDO"));
            case R_ENTRE_FECHAS -> mostrarPrestamos(controller.prestamosEntreFechas(
                    leerFecha(txtDesde, "inicial"), leerFecha(txtHasta, "final")));
            case R_HIST_EQUIPO -> mostrarHistorialEquipo(
                    controller.historialDeEquipo(txtCodigo.getText()));
            case R_HIST_MANT -> mostrarHistorialMantenimiento(
                    controller.historialMantenimiento(txtCodigo.getText()));
            default -> aviso("Seleccione un reporte.");
        }
    }

    /**
     * Devuelve null si el campo esta vacio (el Service avisa); error si el formato es invalido.
     */
    private LocalDate leerFecha(JTextField campo, String nombre) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(texto, FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("La fecha " + nombre
                    + " no es v\u00e1lida. Use el formato dd/MM/aaaa, por ejemplo 15/11/2026.");
        }
    }

    private void mostrarEquiposPorEstado() {
        List<Object[]> filas = new ArrayList<>();
        for (ConteoEstado c : controller.equiposPorEstado()) {
            filas.add(new Object[]{c.estado(), c.total()});
        }
        mostrar(new String[]{"Estado", "Total"}, filas);
    }

    private void mostrarPrestamos(List<FilaPrestamo> prestamos) {
        List<Object[]> filas = new ArrayList<>();
        for (FilaPrestamo p : prestamos) {
            filas.add(new Object[]{p.id(), p.solicitante(), texto(p.fechaPrestamo()),
                    texto(p.fechaEsperada()), p.registradoPor(), p.totalEquipos(),
                    p.pendientes(), p.situacion()});
        }
        mostrar(new String[]{"N\u00ba", "Solicitante", "Fecha pr\u00e9stamo", "Fecha esperada",
                "Registrado por", "Equipos", "Pendientes", "Situaci\u00f3n"}, filas);
    }

    private void mostrarHistorialEquipo(List<FilaHistorialEquipo> historial) {
        List<Object[]> filas = new ArrayList<>();
        for (FilaHistorialEquipo h : historial) {
            filas.add(new Object[]{h.prestamoId(), h.solicitante(), texto(h.fechaPrestamo()),
                    texto(h.fechaEsperada()), texto(h.fechaDevolucion()),
                    h.condicion() == null ? "PENDIENTE" : h.condicion(),
                    h.observacion() == null ? "" : h.observacion()});
        }
        mostrar(new String[]{"Pr\u00e9stamo", "Solicitante", "Fecha pr\u00e9stamo", "Fecha esperada",
                "Fecha devoluci\u00f3n", "Condici\u00f3n", "Observaci\u00f3n"}, filas);
    }

    private void mostrarHistorialMantenimiento(List<FilaHistorialMantenimiento> historial) {
        List<Object[]> filas = new ArrayList<>();
        for (FilaHistorialMantenimiento m : historial) {
            filas.add(new Object[]{m.id(), m.codigoEquipo(), m.nombreEquipo(),
                    texto(m.fechaIngreso()), m.tipo(), m.descripcion(), m.responsable(),
                    texto(m.fechaSalida()), m.resultado() == null ? "" : m.resultado(),
                    m.estado()});
        }
        mostrar(new String[]{"N\u00ba", "C\u00f3digo", "Equipo", "Ingreso", "Tipo",
                "Descripci\u00f3n", "Responsable", "Salida", "Resultado", "Estado"}, filas);
    }

    private void mostrar(String[] columnas, List<Object[]> filas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        for (Object[] fila : filas) {
            modelo.addRow(fila);
        }
        tblResultados.setModel(modelo);
        lblTotal.setText("Registros: " + filas.size());
    }

    private String texto(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA_HORA);
    }

    private void limpiar() {
        txtDesde.setText("");
        txtHasta.setText("");
        txtCodigo.setText("");
        cmbReporte.setSelectedIndex(0);
        tblResultados.setModel(new DefaultTableModel());
        lblTotal.setText("Registros: 0");
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException ex) {
            aviso(ex.getMessage());
        } catch (RuntimeException ex) {
            aviso("Ocurri\u00f3 un problema al realizar la operaci\u00f3n. Intente de nuevo.");
        }
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new GridLayoutManager(5, 4, new Insets(10, 10, 10, 10), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Reporte:");
        panelPrincipal.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbReporte = new JComboBox();
        panelPrincipal.add(cmbReporte, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Desde (dd/MM/aaaa):");
        panelPrincipal.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDesde = new JTextField();
        panelPrincipal.add(txtDesde, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Hasta (dd/MM/aaaa):");
        panelPrincipal.add(label3, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtHasta = new JTextField();
        panelPrincipal.add(txtHasta, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Código de equipo:");
        panelPrincipal.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        panelPrincipal.add(txtCodigo, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));
        btnConsultar = new JButton();
        btnConsultar.setText("Consultar");
        panelPrincipal.add(btnConsultar, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLimpiar = new JButton();
        btnLimpiar.setText("Limpiar");
        panelPrincipal.add(btnLimpiar, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblResultados = new JTable();
        scrollPane1.setViewportView(tblResultados);
        lblTotal = new JLabel();
        lblTotal.setText("Registros: 0");
        panelPrincipal.add(lblTotal, new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}
