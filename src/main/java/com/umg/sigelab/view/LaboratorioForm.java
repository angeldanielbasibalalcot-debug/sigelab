package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.umg.sigelab.controller.LaboratorioController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formulario de gestion de laboratorios, disenado con el GUI Designer: LaboratorioForm.form.
 * Los campos de abajo deben llamarse EXACTAMENTE igual que el "field name" de cada componente
 * en el diseñador. Aqui solo hay logica de interfaz: las reglas viven en LaboratorioService.
 */
public class LaboratorioForm {

    private static final int COL_ESTADO = 5;

    // ---- Componentes del .form (field name en el diseñador) ----
    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JTextField txtCapacidad;
    private JCheckBox chkActivo;
    private JPanel panelBotones;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnReactivar;
    private JTextField txtBuscar;
    private JTable tblLaboratorios;

    private final LaboratorioController controller = new LaboratorioController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Código", "Nombre", "Ubicación", "Capacidad", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

    private List<Laboratorio> laboratorios = new ArrayList<>();
    private Laboratorio seleccionado; // null = se esta creando un laboratorio nuevo

    public LaboratorioForm() {
        configurarTabla();

        txtId.setEditable(false);
        chkActivo.setEnabled(false);
        chkActivo.setToolTipText("El estado se cambia con el botón Desactivar / Reactivar.");

        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnReactivar.addActionListener(e -> reactivar());
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });

        cargarLaboratorios();
        limpiarFormulario();
    }

    /**
     * Panel que MainFrame muestra en su area de contenido.
     */
    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ------------------------------------------------------------------
    // Tabla
    // ------------------------------------------------------------------
    private void configurarTabla() {
        tblLaboratorios.setModel(modelo);
        tblLaboratorios.setRowSorter(sorter);
        tblLaboratorios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLaboratorios.getTableHeader().setReorderingAllowed(false);
        tblLaboratorios.setRowHeight(24);
        tblLaboratorios.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblLaboratorios.getColumnModel().getColumn(0).setMaxWidth(60);
        tblLaboratorios.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                int filaModelo = t.convertRowIndexToModel(fila);
                boolean inactivo = EstadoLaboratorio.INACTIVO.name()
                        .equals(String.valueOf(modelo.getValueAt(filaModelo, COL_ESTADO)));
                if (!seleccionada) {
                    c.setForeground(inactivo ? Color.GRAY : t.getForeground());
                }
                return c;
            }
        });
        tblLaboratorios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void cargarLaboratorios() {
        ejecutar(() -> {
            laboratorios = controller.listar();
            modelo.setRowCount(0);
            for (Laboratorio l : laboratorios) {
                modelo.addRow(new Object[]{l.getId(), l.getCodigo(), l.getNombre(), l.getUbicacion(),
                        l.getCapacidad(), l.getEstado()});
            }
        });
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        }
    }

    private void alSeleccionarFila() {
        int fila = tblLaboratorios.getSelectedRow();
        if (fila < 0) {
            return;
        }
        mostrarEnFormulario(laboratorios.get(tblLaboratorios.convertRowIndexToModel(fila)));
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------
    private void mostrarEnFormulario(Laboratorio l) {
        seleccionado = l;
        boolean activo = l.getEstado() == EstadoLaboratorio.ACTIVO;
        txtId.setText(String.valueOf(l.getId()));
        txtCodigo.setText(l.getCodigo());
        txtNombre.setText(l.getNombre());
        txtUbicacion.setText(l.getUbicacion());
        txtCapacidad.setText(String.valueOf(l.getCapacidad()));
        chkActivo.setSelected(activo);
        btnDesactivar.setEnabled(activo);
        btnReactivar.setEnabled(!activo);
    }

    private void nuevo() {
        tblLaboratorios.clearSelection();
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        seleccionado = null;
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtUbicacion.setText("");
        txtCapacidad.setText("");
        chkActivo.setSelected(false);
        btnDesactivar.setEnabled(false);
        btnReactivar.setEnabled(false);
        txtCodigo.requestFocusInWindow();
    }

    /**
     * Lee los campos. Solo convierte la capacidad a numero; las demas reglas las valida el Service.
     */
    private Laboratorio leerFormulario() {
        String textoCapacidad = txtCapacidad.getText().trim();
        int capacidad;
        try {
            capacidad = Integer.parseInt(textoCapacidad);
        } catch (NumberFormatException e) {
            throw new BusinessException("Ingrese la capacidad como un número entero, por ejemplo 30.");
        }
        Laboratorio datos = new Laboratorio();
        datos.setCodigo(txtCodigo.getText());
        datos.setNombre(txtNombre.getText());
        datos.setUbicacion(txtUbicacion.getText());
        datos.setCapacidad(capacidad);
        return datos;
    }

    private void recargar() {
        tblLaboratorios.clearSelection();
        cargarLaboratorios();
        limpiarFormulario();
    }

    // ------------------------------------------------------------------
    // Acciones de los botones
    // ------------------------------------------------------------------

    /**
     * Guardar = crear un laboratorio nuevo.
     */
    private void guardar() {
        if (seleccionado != null) {
            advertir("Hay un laboratorio seleccionado. Use «Actualizar» para modificarlo "
                    + "o «Nuevo» para crear otro.");
            return;
        }
        ejecutar(() -> {
            controller.crear(leerFormulario());
            informar("Laboratorio creado correctamente.");
            recargar();
        });
    }

    /**
     * Actualizar = modificar el laboratorio seleccionado en la tabla.
     */
    private void actualizar() {
        if (seleccionado == null) {
            advertir("Seleccione un laboratorio de la tabla para actualizarlo.");
            return;
        }
        final int id = seleccionado.getId();
        ejecutar(() -> {
            Laboratorio datos = leerFormulario();
            datos.setId(id);
            controller.actualizar(datos);
            informar("Laboratorio actualizado correctamente.");
            recargar();
        });
    }

    private void desactivar() {
        if (seleccionado == null) {
            advertir("Seleccione un laboratorio de la tabla.");
            return;
        }
        final Laboratorio lab = seleccionado;
        ejecutar(() -> {
            int respuesta = JOptionPane.showConfirmDialog(panelPrincipal,
                    "¿Desactivar el laboratorio " + lab.getCodigo() + "?\n"
                            + "Se conserva su historial, pero ya no debe usarse para equipos nuevos.",
                    "Confirmar desactivación", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
            controller.desactivar(lab.getId());
            informar("Laboratorio desactivado correctamente.");
            recargar();
        });
    }

    private void reactivar() {
        if (seleccionado == null) {
            advertir("Seleccione un laboratorio de la tabla.");
            return;
        }
        final Laboratorio lab = seleccionado;
        ejecutar(() -> {
            controller.reactivar(lab.getId());
            informar("Laboratorio reactivado correctamente.");
            recargar();
        });
    }

    // ------------------------------------------------------------------
    // Mensajes y manejo de errores
    // ------------------------------------------------------------------
    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Laboratorios", JOptionPane.INFORMATION_MESSAGE);
    }

    private void advertir(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Ejecuta una accion y convierte las excepciones en mensajes comprensibles.
     */
    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(panelPrincipal, e.getMessage(),
                    "Revise los datos", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(panelPrincipal, e.getMessage(),
                    "Error de datos", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(panelPrincipal, "Ocurrió un error inesperado. Intente nuevamente.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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
        panelPrincipal.setLayout(new GridLayoutManager(9, 2, new Insets(8, 8, 8, 8), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("ID");
        panelPrincipal.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setEnabled(false);
        panelPrincipal.add(txtId, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Código");
        panelPrincipal.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        panelPrincipal.add(txtCodigo, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Nombre");
        panelPrincipal.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        panelPrincipal.add(txtNombre, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Ubicación");
        panelPrincipal.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtUbicacion = new JTextField();
        panelPrincipal.add(txtUbicacion, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Capacidad");
        panelPrincipal.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCapacidad = new JTextField();
        panelPrincipal.add(txtCapacidad, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        chkActivo = new JCheckBox();
        chkActivo.setEnabled(false);
        chkActivo.setText("Activo");
        panelPrincipal.add(chkActivo, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panelBotones = new JPanel();
        panelBotones.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panelPrincipal.add(panelBotones, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelBotones.add(btnNuevo);
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelBotones.add(btnGuardar);
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelBotones.add(btnActualizar);
        btnDesactivar = new JButton();
        btnDesactivar.setText("Desactivar");
        panelBotones.add(btnDesactivar);
        btnReactivar = new JButton();
        btnReactivar.setText("Reactivar");
        panelBotones.add(btnReactivar);
        final JLabel label6 = new JLabel();
        label6.setText("Buscar laboratorio");
        panelPrincipal.add(label6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtBuscar = new JTextField();
        panelPrincipal.add(txtBuscar, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblLaboratorios = new JTable();
        scrollPane1.setViewportView(tblLaboratorios);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}