package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.umg.sigelab.controller.AuditoriaController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.RegistroAuditoria;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/**
 * Pantalla de consulta de auditoria (solo ADMIN).
 * Solo captura filtros y muestra resultados; las reglas viven en el Service.
 */
public class AuditoriaForm {

    private JPanel panelPrincipal;
    private JComboBox<String> cmbAccion;
    private JTextField txtDesde;
    private JTextField txtHasta;
    private JButton btnConsultar;
    private JButton btnLimpiar;
    private JLabel lblTotal;
    private JTable tblAuditoria;

    private static final String TODAS = "(Todas las acciones)";
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditoriaController controller = new AuditoriaController();
    private DefaultTableModel modelo;

    public AuditoriaForm() {
        configurarTabla();
        configurarEventos();
        cargarAcciones();
        consultar();
    }

    private void configurarTabla() {
        modelo = new DefaultTableModel(
                new Object[]{"Fecha y hora", "Usuario", "Acci\u00f3n", "Entidad", "ID registro", "Descripci\u00f3n"},
                0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tblAuditoria.setModel(modelo);
        tblAuditoria.getColumnModel().getColumn(0).setPreferredWidth(140);
        tblAuditoria.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblAuditoria.getColumnModel().getColumn(2).setPreferredWidth(190);
        tblAuditoria.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblAuditoria.getColumnModel().getColumn(4).setPreferredWidth(80);
        tblAuditoria.getColumnModel().getColumn(5).setPreferredWidth(400);
    }

    private void cargarAcciones() {
        ejecutar(() -> {
            cmbAccion.removeAllItems();
            cmbAccion.addItem(TODAS);
            for (String accion : controller.listarAcciones()) {
                cmbAccion.addItem(accion);
            }
        });
    }

    private void configurarEventos() {
        btnConsultar.addActionListener(e -> consultar());
        btnLimpiar.addActionListener(e -> limpiar());
        txtDesde.addActionListener(e -> consultar());
        txtHasta.addActionListener(e -> consultar());
    }

    private void consultar() {
        ejecutar(() -> {
            String seleccion = (String) cmbAccion.getSelectedItem();
            String accion = (seleccion == null || TODAS.equals(seleccion)) ? null : seleccion;
            LocalDate desde = leerFecha(txtDesde.getText(), "Desde");
            LocalDate hasta = leerFecha(txtHasta.getText(), "Hasta");

            List<RegistroAuditoria> registros = controller.consultar(accion, desde, hasta);
            llenarTabla(registros);
        });
    }

    private void limpiar() {
        if (cmbAccion.getItemCount() > 0) {
            cmbAccion.setSelectedIndex(0);
        }
        txtDesde.setText("");
        txtHasta.setText("");
        consultar();
    }

    private LocalDate leerFecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new BusinessException("La fecha '" + campo
                    + "' no es v\u00e1lida. Use el formato dd/MM/yyyy, por ejemplo 27/10/2026.");
        }
    }

    private void llenarTabla(List<RegistroAuditoria> registros) {
        modelo.setRowCount(0);
        for (RegistroAuditoria r : registros) {
            modelo.addRow(new Object[]{
                    r.getFechaHora() == null ? "" : r.getFechaHora().format(FECHA_HORA),
                    r.getNombreUsuario() == null ? "(sin usuario)" : r.getNombreUsuario(),
                    r.getAccion(),
                    r.getEntidad() == null ? "" : r.getEntidad(),
                    r.getIdRegistro() == null ? "" : r.getIdRegistro(),
                    r.getDescripcion() == null ? "" : r.getDescripcion()
            });
        }
        lblTotal.setText("Registros: " + registros.size());
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException e) {
            aviso(e.getMessage());
        } catch (RuntimeException e) {
            aviso("Ocurri\u00f3 un problema al consultar la auditor\u00eda. Intente de nuevo.");
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
        panelPrincipal.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 9, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Acción:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbAccion = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        cmbAccion.setModel(defaultComboBoxModel1);
        panel1.add(cmbAccion, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Desde (dd/MM/yyyy):");
        panel1.add(label2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDesde = new JTextField();
        panel1.add(txtDesde, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Hasta (dd/MM/yyyy):");
        panel1.add(label3, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtHasta = new JTextField();
        panel1.add(txtHasta, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        btnConsultar = new JButton();
        btnConsultar.setText("Consultar");
        panel1.add(btnConsultar, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLimpiar = new JButton();
        btnLimpiar.setText("Limpiar");
        panel1.add(btnLimpiar, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        lblTotal = new JLabel();
        lblTotal.setText("Registros: 0");
        panelPrincipal.add(lblTotal, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblAuditoria = new JTable();
        scrollPane1.setViewportView(tblAuditoria);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}