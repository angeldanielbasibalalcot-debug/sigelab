package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.umg.sigelab.controller.MantenimientoController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.Mantenimiento;
import com.umg.sigelab.model.ResultadoMantenimiento;
import com.umg.sigelab.model.TipoMantenimiento;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Mantenimiento (formulario del GUI Designer: MantenimientoForm.form).
 * Solo captura y muestra datos. Las reglas viven en MantenimientoService.
 */
public class MantenimientoForm {

    private JPanel panelPrincipal;
    private JComboBox<Equipo> cmbEquipo;
    private JComboBox<TipoMantenimiento> cmbTipo;
    private JTextField txtDescripcion;
    private JTextField txtResponsable;
    private JTextField txtObservaciones;
    private JButton btnEnviar;
    private JComboBox<ResultadoMantenimiento> cmbResultado;
    private JButton btnFinalizar;
    private JButton btnLimpiar;
    private JButton btnRecargar;
    private JCheckBox chkSoloEnProceso;
    private JTable tblMantenimientos;

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MantenimientoController controller =
            new MantenimientoController();

    private DefaultTableModel modelo;
    private final List<Mantenimiento> visibles = new ArrayList<>();

    public MantenimientoForm() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        ejecutar(this::recargar);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ---------- Configuracion ----------

    private void configurarTabla() {
        modelo = new DefaultTableModel(new String[]{
                "ID", "Equipo", "Tipo", "Ingreso", "Responsable",
                "Estado", "Resultado", "Salida"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tblMantenimientos.setModel(modelo);
        tblMantenimientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMantenimientos.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarCombos() {
        cmbEquipo.setRenderer(new ItemRenderer());
        cmbTipo.setRenderer(new ItemRenderer());
        cmbResultado.setRenderer(new ItemRenderer());

        cmbTipo.addItem(null);
        for (TipoMantenimiento t : TipoMantenimiento.values()) {
            cmbTipo.addItem(t);
        }

        cmbResultado.addItem(null);
        for (ResultadoMantenimiento r : ResultadoMantenimiento.values()) {
            cmbResultado.addItem(r);
        }
    }

    private void configurarEventos() {
        btnEnviar.addActionListener(ev -> ejecutar(this::enviar));
        btnFinalizar.addActionListener(ev -> ejecutar(this::finalizar));
        btnLimpiar.addActionListener(ev -> limpiar());
        btnRecargar.addActionListener(ev -> ejecutar(this::recargar));
        chkSoloEnProceso.addActionListener(ev -> ejecutar(this::cargarTabla));
    }

    // ---------- Acciones ----------

    private void enviar() {
        Equipo equipo = (Equipo) cmbEquipo.getSelectedItem();
        int equipoId = equipo == null ? 0 : equipo.getId();

        controller.enviar(
                equipoId,
                (TipoMantenimiento) cmbTipo.getSelectedItem(),
                txtDescripcion.getText(),
                txtResponsable.getText(),
                txtObservaciones.getText());

        info("El equipo fue enviado a mantenimiento.");
        limpiar();
        recargar();
    }

    private void finalizar() {
        int fila = tblMantenimientos.getSelectedRow();
        if (fila < 0 || fila >= visibles.size()) {
            throw new BusinessException(
                    "Seleccione un mantenimiento en la tabla.");
        }

        Mantenimiento seleccionado = visibles.get(fila);
        ResultadoMantenimiento resultado =
                (ResultadoMantenimiento) cmbResultado.getSelectedItem();

        if (resultado == null) {
            throw new BusinessException(
                    "Seleccione el resultado del mantenimiento.");
        }

        String destino = resultado == ResultadoMantenimiento.REPARADO
                ? "DISPONIBLE" : "BAJA";

        int opcion = JOptionPane.showConfirmDialog(panelPrincipal,
                "El equipo " + seleccionado.getEquipo().getCodigoInventario()
                        + " quedar\u00e1 en estado " + destino
                        + ".\n\u00bfDesea finalizar el mantenimiento?",
                "Confirmar", JOptionPane.YES_NO_OPTION);

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        controller.finalizar(seleccionado.getId(), resultado,
                txtObservaciones.getText());

        info("El mantenimiento fue finalizado.");
        limpiar();
        recargar();
    }

    private void recargar() {
        cmbEquipo.removeAllItems();
        cmbEquipo.addItem(null);
        for (Equipo e : controller.listarEquiposElegibles()) {
            cmbEquipo.addItem(e);
        }
        cargarTabla();
    }

    private void cargarTabla() {
        List<Mantenimiento> datos = chkSoloEnProceso.isSelected()
                ? controller.listarEnProceso()
                : controller.listar();

        visibles.clear();
        visibles.addAll(datos);
        modelo.setRowCount(0);

        for (Mantenimiento m : datos) {
            modelo.addRow(new Object[]{
                    m.getId(),
                    m.getEquipo().getCodigoInventario() + " - "
                            + m.getEquipo().getNombre(),
                    m.getTipo(),
                    formatear(m.getFechaIngreso()),
                    m.getResponsable(),
                    m.getEstado(),
                    m.getResultado() == null ? "" : m.getResultado(),
                    formatear(m.getFechaSalida())
            });
        }
    }

    private void limpiar() {
        cmbEquipo.setSelectedIndex(-1);
        cmbTipo.setSelectedIndex(0);
        cmbResultado.setSelectedIndex(0);
        txtDescripcion.setText("");
        txtResponsable.setText("");
        txtObservaciones.setText("");
        tblMantenimientos.clearSelection();
    }

    // ---------- Utilidades ----------

    private static String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO);
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException ex) {
            aviso(ex.getMessage());
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panelPrincipal,
                    "No se pudo completar la operaci\u00f3n. Intente de nuevo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje,
                "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje,
                "Mantenimiento", JOptionPane.INFORMATION_MESSAGE);
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
        panelPrincipal.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Equipo:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbEquipo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        cmbEquipo.setModel(defaultComboBoxModel1);
        panel1.add(cmbEquipo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Tipo de mantenimiento:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbTipo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        cmbTipo.setModel(defaultComboBoxModel2);
        panel1.add(cmbTipo, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Descripción:");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDescripcion = new JTextField();
        panel1.add(txtDescripcion, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Responsable:");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtResponsable = new JTextField();
        panel1.add(txtResponsable, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Observaciones:");
        panel1.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtObservaciones = new JTextField();
        panel1.add(txtObservaciones, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        btnEnviar = new JButton();
        btnEnviar.setText("Enviar a mantenimiento");
        panel2.add(btnEnviar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Resultado:");
        panel2.add(label6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbResultado = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        cmbResultado.setModel(defaultComboBoxModel3);
        panel2.add(cmbResultado, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnFinalizar = new JButton();
        btnFinalizar.setText("Finalizar mantenimiento");
        panel2.add(btnFinalizar, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLimpiar = new JButton();
        btnLimpiar.setText("Limpiar");
        panel2.add(btnLimpiar, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRecargar = new JButton();
        btnRecargar.setText("Recargar");
        panel2.add(btnRecargar, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        chkSoloEnProceso = new JCheckBox();
        chkSoloEnProceso.setText("Mostrar solo mantenimientos en proceso");
        panelPrincipal.add(chkSoloEnProceso, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblMantenimientos = new JTable();
        scrollPane1.setViewportView(tblMantenimientos);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

    // Muestra texto legible en los combos (y "Seleccione..." si no hay valor).
    private static class ItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value, int index, boolean seleccionado, boolean foco) {

            String texto;
            if (value == null) {
                texto = "-- Seleccione --";
            } else if (value instanceof Equipo e) {
                texto = e.getCodigoInventario() + " - " + e.getNombre()
                        + " (" + e.getEstado().name() + ")";
            } else {
                texto = value.toString();
            }

            return super.getListCellRendererComponent(
                    list, texto, index, seleccionado, foco);
        }
    }
}