package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.umg.sigelab.controller.PrestamoController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de registro de prestamos (RN-05, RN-06, RN-07, RN-08, RN-09, RN-14, RN-19).
 * No contiene reglas de negocio ni SQL: solo captura datos y llama al Controller.
 */
public class PrestamoForm {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Componentes enlazados al archivo PrestamoForm.form
    private JPanel panelPrincipal;
    private JTextField txtSolicitante;
    private JTextField txtFechaEsperada;
    private JTextField txtObservaciones;
    private JComboBox<Equipo> cmbEquipo;
    private JButton btnAgregar;
    private JButton btnQuitar;
    private JButton btnLeerQr;
    private JTable tblEquipos;
    private JLabel lblTotal;
    private JButton btnRegistrar;
    private JButton btnLimpiar;

    private final PrestamoController controller = new PrestamoController();
    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"C\u00f3digo", "Nombre"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final List<Equipo> seleccionados = new ArrayList<>();

    public PrestamoForm() {
        configurarTabla();
        configurarCombo();
        configurarEventos();
        cargarCombo();
        actualizarTotal();
    }

    private void configurarTabla() {
        tblEquipos.setModel(modeloTabla);
    }

    private void configurarCombo() {
        cmbEquipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor,
                                                          int indice, boolean seleccionado, boolean foco) {
                Object texto = valor;
                if (valor instanceof Equipo) {
                    Equipo e = (Equipo) valor;
                    texto = e.getCodigoInventario() + " - " + e.getNombre();
                }
                return super.getListCellRendererComponent(lista, texto, indice, seleccionado, foco);
            }
        });
    }

    private void configurarEventos() {
        btnAgregar.addActionListener(e -> agregarEquipo());
        btnQuitar.addActionListener(e -> quitarEquipo());
        btnLeerQr.addActionListener(e -> ejecutar(this::leerQr));
        btnRegistrar.addActionListener(e -> registrarPrestamo());
        btnLimpiar.addActionListener(e -> limpiar());
    }

    private void cargarCombo() {
        ejecutar(() -> {
            List<Equipo> disponibles = controller.listarEquiposDisponibles();
            DefaultComboBoxModel<Equipo> modelo = new DefaultComboBoxModel<>();
            for (Equipo equipo : disponibles) {
                modelo.addElement(equipo);
            }
            cmbEquipo.setModel(modelo);
            cmbEquipo.setSelectedIndex(-1);
        });
    }

    private void agregarEquipo() {
        Equipo elegido = (Equipo) cmbEquipo.getSelectedItem();
        if (elegido == null) {
            aviso("Seleccione un equipo de la lista.");
            return;
        }
        if (agregarALista(elegido)) {
            cmbEquipo.setSelectedIndex(-1);
        }
    }

    /**
     * Agrega un equipo a la tabla del prestamo (RN-06: sin repetidos).
     * Lo usan el boton Agregar y el boton Leer QR.
     *
     * @return true si se agrego, false si ya estaba en la lista
     */
    private boolean agregarALista(Equipo equipo) {
        for (Equipo e : seleccionados) {
            if (e.getId() == equipo.getId()) {
                aviso("El equipo " + equipo.getCodigoInventario()
                        + " ya fue agregado a este pr\u00e9stamo.");
                return false;
            }
        }
        seleccionados.add(equipo);
        modeloTabla.addRow(new Object[]{equipo.getCodigoInventario(), equipo.getNombre()});
        actualizarTotal();
        return true;
    }

    /**
     * RN-19: leer el QR solo identifica el equipo; no cambia ningun estado.
     * CP-09: DISPONIBLE se agrega a la lista. CP-10: cualquier otro estado se
     * identifica pero queda bloqueado. El rechazo definitivo (RN-20) sigue
     * en PrestamoService.crearPrestamo.
     */
    private void leerQr() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccione la imagen del c\u00f3digo QR");
        selector.setFileFilter(new FileNameExtensionFilter("Im\u00e1genes (PNG, JPG)", "png", "jpg", "jpeg"));
        if (selector.showOpenDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path imagen = selector.getSelectedFile().toPath();
        Equipo equipo = controller.identificarPorQr(imagen);

        if (equipo.getEstado() == EstadoEquipo.DISPONIBLE) {
            if (agregarALista(equipo)) {
                info("Equipo " + equipo.getCodigoInventario() + " (" + equipo.getNombre()
                        + ") identificado y agregado al pr\u00e9stamo.");
            }
        } else {
            aviso("Equipo " + equipo.getCodigoInventario() + " (" + equipo.getNombre()
                    + ") identificado, pero est\u00e1 en estado " + equipo.getEstado()
                    + " y no puede prestarse.");
        }
    }

    private void quitarEquipo() {
        int fila = tblEquipos.getSelectedRow();
        if (fila < 0) {
            aviso("Seleccione en la tabla el equipo que desea quitar.");
            return;
        }
        seleccionados.remove(fila);
        modeloTabla.removeRow(fila);
        actualizarTotal();
    }

    private void registrarPrestamo() {
        String textoFecha = txtFechaEsperada.getText().trim();
        if (textoFecha.isEmpty()) {
            aviso("Ingrese la fecha esperada de devoluci\u00f3n (dd/MM/aaaa).");
            return;
        }
        LocalDate fechaEsperada;
        try {
            fechaEsperada = LocalDate.parse(textoFecha, FORMATO_FECHA);
        } catch (DateTimeParseException ex) {
            aviso("La fecha esperada no es v\u00e1lida. Use el formato dd/MM/aaaa, por ejemplo 15/11/2026.");
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (Equipo e : seleccionados) {
            ids.add(e.getId());
        }

        String solicitante = txtSolicitante.getText();
        String observaciones = txtObservaciones.getText();
        LocalDate fecha = fechaEsperada;

        ejecutar(() -> {
            var idPrestamo = controller.crearPrestamo(solicitante, fecha, observaciones, ids);
            info("Pr\u00e9stamo registrado correctamente. N\u00famero de pr\u00e9stamo: " + idPrestamo);
            limpiar();
        });
    }

    private void limpiar() {
        txtSolicitante.setText("");
        txtFechaEsperada.setText("");
        txtObservaciones.setText("");
        seleccionados.clear();
        modeloTabla.setRowCount(0);
        actualizarTotal();
        cargarCombo();
    }

    private void actualizarTotal() {
        lblTotal.setText("Equipos en el pr\u00e9stamo: " + seleccionados.size());
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

    private void info(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Informaci\u00f3n", JOptionPane.INFORMATION_MESSAGE);
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
        panelPrincipal.setLayout(new GridLayoutManager(7, 4, new Insets(10, 10, 10, 10), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Solicitante:");
        panelPrincipal.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtSolicitante = new JTextField();
        panelPrincipal.add(txtSolicitante, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Fecha esperada (dd/MM/aaaa):");
        panelPrincipal.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtFechaEsperada = new JTextField();
        panelPrincipal.add(txtFechaEsperada, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Observaciones:");
        panelPrincipal.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtObservaciones = new JTextField();
        panelPrincipal.add(txtObservaciones, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Equipo disponible:");
        panelPrincipal.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbEquipo = new JComboBox();
        panelPrincipal.add(cmbEquipo, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnAgregar = new JButton();
        btnAgregar.setText("Agregar");
        panelPrincipal.add(btnAgregar, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnQuitar = new JButton();
        btnQuitar.setText("Quitar seleccionado");
        panelPrincipal.add(btnQuitar, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Identificar por QR:");
        panelPrincipal.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLeerQr = new JButton();
        btnLeerQr.setText("Leer QR");
        panelPrincipal.add(btnLeerQr, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(5, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblEquipos = new JTable();
        scrollPane1.setViewportView(tblEquipos);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel1, new GridConstraints(6, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        lblTotal = new JLabel();
        lblTotal.setText("Equipos en el préstamo: 0");
        panel1.add(lblTotal, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRegistrar = new JButton();
        btnRegistrar.setText("Registrar préstamo");
        panel1.add(btnRegistrar, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLimpiar = new JButton();
        btnLimpiar.setText("Limpiar");
        panel1.add(btnLimpiar, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}