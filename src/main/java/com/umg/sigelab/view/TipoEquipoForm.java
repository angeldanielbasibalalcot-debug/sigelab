package com.umg.sigelab.view;

import com.umg.sigelab.controller.TipoEquipoController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

public class TipoEquipoForm {

    private final TipoEquipoController controller =
            new TipoEquipoController();

    private final JPanel panelPrincipal =
            new JPanel(new BorderLayout(10, 10));

    private final JTextField txtId = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextArea txtDescripcion = new JTextArea(3, 30);
    private final JTextField txtBuscar = new JTextField();

    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnRecargar = new JButton("Recargar");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(
                    new Object[]{"ID", "Nombre", "Descripción"}, 0) {

                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columna) {
                    return columna == 0 ? Integer.class : String.class;
                }
            };

    private final JTable tabla = new JTable(modeloTabla);

    private final TableRowSorter<DefaultTableModel> ordenador =
            new TableRowSorter<>(modeloTabla);

    public TipoEquipoForm() {
        construirFormulario();
        configurarEventos();
        nuevo();
        cargarTabla();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    private void construirFormulario() {
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12));

        txtId.setEditable(false);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);

        JPanel campos = new JPanel(new GridBagLayout());

        agregarCampo(campos, 0, "ID:", txtId);
        agregarCampo(campos, 1, "Nombre:", txtNombre);
        agregarCampo(campos, 2, "Descripción:",
                new JScrollPane(txtDescripcion));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnRecargar);

        JPanel superior = new JPanel(new BorderLayout(8, 8));
        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        JPanel busqueda = new JPanel(new BorderLayout(8, 8));
        busqueda.add(new JLabel("Buscar tipo de equipo:"),
                BorderLayout.WEST);
        busqueda.add(txtBuscar, BorderLayout.CENTER);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowSorter(ordenador);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(450);

        JPanel listado = new JPanel(new BorderLayout(8, 8));
        listado.add(busqueda, BorderLayout.NORTH);
        listado.add(new JScrollPane(tabla), BorderLayout.CENTER);

        panelPrincipal.add(superior, BorderLayout.NORTH);
        panelPrincipal.add(listado, BorderLayout.CENTER);
    }

    private void agregarCampo(
            JPanel panel, int fila, String etiqueta, JComponent campo) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fila;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 12);
        panel.add(new JLabel(etiqueta), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, c);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar(false));
        btnActualizar.addActionListener(e -> guardar(true));
        btnRecargar.addActionListener(e -> cargarTabla());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarFila();
            }
        });

        txtBuscar.getDocument().addDocumentListener(
                new DocumentListener() {
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
    }

    private void nuevo() {
        tabla.clearSelection();
        txtId.setText("");
        txtNombre.setText("");
        txtDescripcion.setText("");
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        txtNombre.requestFocusInWindow();
    }

    private void seleccionarFila() {
        int filaVista = tabla.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int fila = tabla.convertRowIndexToModel(filaVista);

        txtId.setText(modeloTabla.getValueAt(fila, 0).toString());
        txtNombre.setText((String) modeloTabla.getValueAt(fila, 1));
        txtDescripcion.setText((String) modeloTabla.getValueAt(fila, 2));

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
    }

    private void cargarTabla() {
        try {
            List<TipoEquipo> tipos = controller.listar();

            modeloTabla.setRowCount(0);

            for (TipoEquipo tipo : tipos) {
                modeloTabla.addRow(new Object[]{
                        tipo.getId(),
                        tipo.getNombre(),
                        tipo.getDescripcion() == null
                                ? "" : tipo.getDescripcion()
                });
            }

            nuevo();
        } catch (BusinessException ex) {
            mostrarValidacion(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "No se pudo cargar la lista de tipos de equipo.\n"
                            + "Revise la conexión y pulse Recargar.",
                    "Error al consultar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar(boolean actualizar) {
        if (actualizar && txtId.getText().isBlank()) {
            mostrarValidacion("Seleccione un tipo de equipo en la tabla.");
            return;
        }

        try {
            int id = actualizar
                    ? Integer.parseInt(txtId.getText()) : 0;

            TipoEquipo datos = new TipoEquipo(
                    id,
                    txtNombre.getText(),
                    txtDescripcion.getText());

            if (actualizar) {
                controller.actualizar(datos);
            } else {
                controller.crear(datos);
            }
        } catch (BusinessException ex) {
            mostrarValidacion(ex.getMessage());
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "No se pudo confirmar la operación.\n"
                            + "Recargue la lista antes de volver a intentarlo.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(
                panelPrincipal,
                actualizar
                        ? "Tipo de equipo actualizado correctamente."
                        : "Tipo de equipo creado correctamente.",
                "Operación realizada",
                JOptionPane.INFORMATION_MESSAGE);

        nuevo();
        txtBuscar.setText("");
        cargarTabla();
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim();

        ordenador.setRowFilter(
                texto.isEmpty()
                        ? null
                        : RowFilter.regexFilter(
                        "(?iu)" + Pattern.quote(texto), 1, 2));
    }

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "Revise los datos",
                JOptionPane.WARNING_MESSAGE);
    }
}