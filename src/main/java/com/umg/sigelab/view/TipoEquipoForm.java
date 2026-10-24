package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.umg.sigelab.controller.TipoEquipoController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formulario del catalogo de tipos de equipo, disenado con el GUI Designer: TipoEquipoForm.form.
 * Los campos de abajo deben llamarse EXACTAMENTE igual que el "field name" de cada componente
 * en el diseñador. Aqui solo hay logica de interfaz: las reglas viven en TipoEquipoService.
 */
public class TipoEquipoForm {

    // ---- Componentes del .form (field name en el diseñador) ----
    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JPanel panelBotones;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JTextField txtBuscar;
    private JTable tblTiposEquipo;

    private final TipoEquipoController controller = new TipoEquipoController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Descripción"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

    private List<TipoEquipo> tipos = new ArrayList<>();
    private TipoEquipo seleccionado; // null = se esta creando un tipo nuevo

    public TipoEquipoForm() {
        configurarTabla();

        txtId.setEditable(false);

        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
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

        cargarTipos();
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
        tblTiposEquipo.setModel(modelo);
        tblTiposEquipo.setRowSorter(sorter);
        tblTiposEquipo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTiposEquipo.getTableHeader().setReorderingAllowed(false);
        tblTiposEquipo.setRowHeight(24);
        tblTiposEquipo.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblTiposEquipo.getColumnModel().getColumn(0).setMaxWidth(60);
        tblTiposEquipo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void cargarTipos() {
        ejecutar(() -> {
            tipos = controller.listar();
            modelo.setRowCount(0);
            for (TipoEquipo t : tipos) {
                modelo.addRow(new Object[]{t.getId(), t.getNombre(),
                        t.getDescripcion() == null ? "" : t.getDescripcion()});
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
        int fila = tblTiposEquipo.getSelectedRow();
        if (fila < 0) {
            return;
        }
        mostrarEnFormulario(tipos.get(tblTiposEquipo.convertRowIndexToModel(fila)));
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------
    private void mostrarEnFormulario(TipoEquipo t) {
        seleccionado = t;
        txtId.setText(String.valueOf(t.getId()));
        txtNombre.setText(t.getNombre());
        txtDescripcion.setText(t.getDescripcion() == null ? "" : t.getDescripcion());
    }

    private void nuevo() {
        tblTiposEquipo.clearSelection();
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        seleccionado = null;
        txtId.setText("");
        txtNombre.setText("");
        txtDescripcion.setText("");
        txtNombre.requestFocusInWindow();
    }

    private TipoEquipo leerFormulario() {
        TipoEquipo datos = new TipoEquipo();
        datos.setNombre(txtNombre.getText());
        datos.setDescripcion(txtDescripcion.getText());
        return datos;
    }

    private void recargar() {
        tblTiposEquipo.clearSelection();
        cargarTipos();
        limpiarFormulario();
    }

    // ------------------------------------------------------------------
    // Acciones de los botones
    // ------------------------------------------------------------------

    /**
     * Guardar = crear un tipo nuevo.
     */
    private void guardar() {
        if (seleccionado != null) {
            advertir("Hay un tipo de equipo seleccionado. Use «Actualizar» para modificarlo "
                    + "o «Nuevo» para crear otro.");
            return;
        }
        ejecutar(() -> {
            controller.crear(leerFormulario());
            informar("Tipo de equipo creado correctamente.");
            recargar();
        });
    }

    /**
     * Actualizar = modificar el tipo seleccionado en la tabla.
     */
    private void actualizar() {
        if (seleccionado == null) {
            advertir("Seleccione un tipo de equipo de la tabla para actualizarlo.");
            return;
        }
        final int id = seleccionado.getId();
        ejecutar(() -> {
            TipoEquipo datos = leerFormulario();
            datos.setId(id);
            controller.actualizar(datos);
            informar("Tipo de equipo actualizado correctamente.");
            recargar();
        });
    }

    // ------------------------------------------------------------------
    // Mensajes y manejo de errores
    // ------------------------------------------------------------------
    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Tipos de equipo", JOptionPane.INFORMATION_MESSAGE);
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
        panelPrincipal.setLayout(new GridLayoutManager(6, 2, new Insets(8, 8, 8, 8), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("ID");
        panelPrincipal.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setEnabled(false);
        panelPrincipal.add(txtId, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Nombre");
        panelPrincipal.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        panelPrincipal.add(txtNombre, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Descripción");
        panelPrincipal.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDescripcion = new JTextField();
        panelPrincipal.add(txtDescripcion, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        panelBotones = new JPanel();
        panelBotones.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panelPrincipal.add(panelBotones, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelBotones.add(btnNuevo);
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelBotones.add(btnGuardar);
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelBotones.add(btnActualizar);
        final JLabel label4 = new JLabel();
        label4.setText("Buscar tipo de equipo");
        panelPrincipal.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtBuscar = new JTextField();
        panelPrincipal.add(txtBuscar, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblTiposEquipo = new JTable();
        scrollPane1.setViewportView(tblTiposEquipo);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}