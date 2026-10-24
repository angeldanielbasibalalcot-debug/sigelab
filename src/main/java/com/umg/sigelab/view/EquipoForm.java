package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.umg.sigelab.controller.EquipoController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.TipoEquipo;

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
 * Formulario de gestion de equipos, disenado con el GUI Designer: EquipoForm.form.
 * Los campos de abajo deben llamarse EXACTAMENTE igual que el "field name" de cada componente
 * en el diseñador. Aqui solo hay logica de interfaz: las reglas viven en EquipoService.
 * El estado del equipo es de solo lectura: lo cambian los prestamos, devoluciones y mantenimiento.
 */
public class EquipoForm {

    private static final int COL_ESTADO = 8;

    // ---- Componentes del .form (field name en el diseñador) ----
    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JComboBox<TipoEquipo> cmbTipo;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtSerie;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JTextField txtEstado;
    private JTextField txtObservaciones;
    private JPanel panelBotones;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnVerQr;
    private JTextField txtBuscar;
    private JTable tblEquipos;

    private final EquipoController controller = new EquipoController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Código", "Nombre", "Tipo", "Marca", "Modelo", "Serie", "Laboratorio", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

    private List<Equipo> equipos = new ArrayList<>();
    private Equipo seleccionado; // null = se esta creando un equipo nuevo

    public EquipoForm() {
        configurarTabla();

        txtId.setEditable(false);
        txtEstado.setEditable(false);
        txtEstado.setToolTipText("El estado lo cambian los préstamos, devoluciones y mantenimientos.");
        // El codigo QR se integra en el siguiente paso (libreria ZXing).
        btnVerQr.setEnabled(false);
        btnVerQr.setToolTipText("Disponible cuando se integre el código QR.");

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

        cargarCombos();
        cargarEquipos();
        limpiarFormulario();
    }

    /**
     * Panel que MainFrame muestra en su area de contenido.
     */
    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ------------------------------------------------------------------
    // Tabla y combos
    // ------------------------------------------------------------------
    private void configurarTabla() {
        tblEquipos.setModel(modelo);
        tblEquipos.setRowSorter(sorter);
        tblEquipos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEquipos.getTableHeader().setReorderingAllowed(false);
        tblEquipos.setRowHeight(24);
        tblEquipos.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblEquipos.getColumnModel().getColumn(0).setMaxWidth(60);
        tblEquipos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                int filaModelo = t.convertRowIndexToModel(fila);
                boolean baja = EstadoEquipo.BAJA.name()
                        .equals(String.valueOf(modelo.getValueAt(filaModelo, COL_ESTADO)));
                if (!seleccionada) {
                    c.setForeground(baja ? Color.GRAY : t.getForeground());
                }
                return c;
            }
        });
        tblEquipos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    /**
     * Llena los combos: todos los tipos y solo los laboratorios ACTIVOS.
     */
    private void cargarCombos() {
        ejecutar(() -> {
            cmbTipo.removeAllItems();
            controller.listarTipos().forEach(cmbTipo::addItem);
            cmbLaboratorio.removeAllItems();
            controller.listarLaboratoriosActivos().forEach(cmbLaboratorio::addItem);
        });
    }

    private void cargarEquipos() {
        ejecutar(() -> {
            equipos = controller.listar();
            modelo.setRowCount(0);
            for (Equipo e : equipos) {
                modelo.addRow(new Object[]{e.getId(), e.getCodigoInventario(), e.getNombre(),
                        e.getTipo().getNombre(), vacioSiNulo(e.getMarca()), vacioSiNulo(e.getModelo()),
                        vacioSiNulo(e.getNumeroSerie()), e.getLaboratorio().getCodigo(), e.getEstado()});
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
        int fila = tblEquipos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        mostrarEnFormulario(equipos.get(tblEquipos.convertRowIndexToModel(fila)));
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------
    private void mostrarEnFormulario(Equipo e) {
        seleccionado = e;
        txtId.setText(String.valueOf(e.getId()));
        txtCodigo.setText(e.getCodigoInventario());
        txtCodigo.setEditable(false); // el codigo no cambia: el QR ya impreso depende de el
        txtNombre.setText(e.getNombre());
        txtMarca.setText(vacioSiNulo(e.getMarca()));
        txtModelo.setText(vacioSiNulo(e.getModelo()));
        txtSerie.setText(vacioSiNulo(e.getNumeroSerie()));
        txtEstado.setText(e.getEstado().name());
        txtObservaciones.setText(vacioSiNulo(e.getObservaciones()));
        seleccionarTipo(e.getTipo());
        seleccionarLaboratorio(e.getLaboratorio());
    }

    private void seleccionarTipo(TipoEquipo tipo) {
        for (int i = 0; i < cmbTipo.getItemCount(); i++) {
            if (cmbTipo.getItemAt(i).getId() == tipo.getId()) {
                cmbTipo.setSelectedIndex(i);
                return;
            }
        }
        cmbTipo.setSelectedIndex(-1);
    }

    /**
     * Si el laboratorio del equipo esta INACTIVO no esta en el combo: se agrega solo para mostrarlo.
     */
    private void seleccionarLaboratorio(Laboratorio lab) {
        for (int i = 0; i < cmbLaboratorio.getItemCount(); i++) {
            if (cmbLaboratorio.getItemAt(i).getId() == lab.getId()) {
                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }
        cmbLaboratorio.addItem(lab);
        cmbLaboratorio.setSelectedItem(lab);
    }

    private void nuevo() {
        tblEquipos.clearSelection();
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        seleccionado = null;
        txtId.setText("");
        txtCodigo.setText("");
        txtCodigo.setEditable(true);
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtSerie.setText("");
        txtObservaciones.setText("");
        txtEstado.setText(EstadoEquipo.DISPONIBLE.name()); // todo equipo nuevo nace DISPONIBLE
        cmbTipo.setSelectedIndex(-1);
        cmbLaboratorio.setSelectedIndex(-1);
        txtCodigo.requestFocusInWindow();
    }

    private Equipo leerFormulario() {
        Equipo datos = new Equipo();
        datos.setCodigoInventario(txtCodigo.getText());
        datos.setNombre(txtNombre.getText());
        datos.setTipo((TipoEquipo) cmbTipo.getSelectedItem());
        datos.setMarca(txtMarca.getText());
        datos.setModelo(txtModelo.getText());
        datos.setNumeroSerie(txtSerie.getText());
        datos.setLaboratorio((Laboratorio) cmbLaboratorio.getSelectedItem());
        datos.setObservaciones(txtObservaciones.getText());
        return datos;
    }

    private void recargar() {
        tblEquipos.clearSelection();
        cargarCombos();
        cargarEquipos();
        limpiarFormulario();
    }

    private String vacioSiNulo(String valor) {
        return valor == null ? "" : valor;
    }

    // ------------------------------------------------------------------
    // Acciones de los botones
    // ------------------------------------------------------------------

    /**
     * Guardar = crear un equipo nuevo (nace DISPONIBLE).
     */
    private void guardar() {
        if (seleccionado != null) {
            advertir("Hay un equipo seleccionado. Use «Actualizar» para modificarlo "
                    + "o «Nuevo» para crear otro.");
            return;
        }
        ejecutar(() -> {
            controller.crear(leerFormulario());
            informar("Equipo creado correctamente.");
            recargar();
        });
    }

    /**
     * Actualizar = modificar los datos del equipo seleccionado (no su codigo ni su estado).
     */
    private void actualizar() {
        if (seleccionado == null) {
            advertir("Seleccione un equipo de la tabla para actualizarlo.");
            return;
        }
        final int id = seleccionado.getId();
        ejecutar(() -> {
            Equipo datos = leerFormulario();
            datos.setId(id);
            controller.actualizar(datos);
            informar("Equipo actualizado correctamente.");
            recargar();
        });
    }

    // ------------------------------------------------------------------
    // Mensajes y manejo de errores
    // ------------------------------------------------------------------
    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Equipos", JOptionPane.INFORMATION_MESSAGE);
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
        panelPrincipal.setLayout(new GridLayoutManager(13, 2, new Insets(8, 8, 8, 8), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("ID");
        panelPrincipal.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setEnabled(false);
        panelPrincipal.add(txtId, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Código de inventario");
        panelPrincipal.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        panelPrincipal.add(txtCodigo, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Nombre");
        panelPrincipal.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        panelPrincipal.add(txtNombre, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Tipo de equipo");
        panelPrincipal.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbTipo = new JComboBox();
        panelPrincipal.add(cmbTipo, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Marca");
        panelPrincipal.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtMarca = new JTextField();
        panelPrincipal.add(txtMarca, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Modelo");
        panelPrincipal.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtModelo = new JTextField();
        panelPrincipal.add(txtModelo, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Número de serie");
        panelPrincipal.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtSerie = new JTextField();
        panelPrincipal.add(txtSerie, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Laboratorio");
        panelPrincipal.add(label8, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbLaboratorio = new JComboBox();
        panelPrincipal.add(cmbLaboratorio, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Estado");
        panelPrincipal.add(label9, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtEstado = new JTextField();
        txtEstado.setEnabled(false);
        panelPrincipal.add(txtEstado, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Observaciones");
        panelPrincipal.add(label10, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtObservaciones = new JTextField();
        panelPrincipal.add(txtObservaciones, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        panelBotones = new JPanel();
        panelBotones.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panelPrincipal.add(panelBotones, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelBotones.add(btnNuevo);
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelBotones.add(btnGuardar);
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelBotones.add(btnActualizar);
        btnVerQr = new JButton();
        btnVerQr.setText("Ver código QR");
        panelBotones.add(btnVerQr);
        final JLabel label11 = new JLabel();
        label11.setText("Buscar equipo");
        panelPrincipal.add(label11, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtBuscar = new JTextField();
        panelPrincipal.add(txtBuscar, new GridConstraints(11, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(12, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblEquipos = new JTable();
        scrollPane1.setViewportView(tblEquipos);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}