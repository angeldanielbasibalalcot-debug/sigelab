package com.umg.sigelab.view;

import javax.swing.*;
import java.awt.*;
import com.umg.sigelab.model.CondicionDevolucion;
import com.umg.sigelab.service.DevolucionService;
import com.umg.sigelab.util.SesionActual;

import com.umg.sigelab.model.DetallePrestamoCombo;
import java.util.List;

public class DevolucionForm {

    private JPanel panelPrincipal;

    private DevolucionService service =
            new DevolucionService();


    public DevolucionForm() {

        panelPrincipal =
                new JPanel(
                        new BorderLayout());
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 15, 15, 15));

        JLabel titulo =
                new JLabel(
                        "Modulo de Devoluciones",
                        SwingConstants.CENTER);

        titulo.setFont(
                titulo.getFont()
                        .deriveFont(
                                Font.BOLD,
                                20f));

        panelPrincipal.add(
                titulo,
                BorderLayout.NORTH);

        JPanel centro =
                new JPanel(
                        new GridBagLayout());

        centro.setBorder(
                BorderFactory.createTitledBorder(
                        "Registrar Devolucion"));

        JComboBox<DetallePrestamoCombo> cboDetalle =
                new JComboBox<>();

        List<DetallePrestamoCombo> pendientes =
                service.listarPendientes();

        for (DetallePrestamoCombo item : pendientes) {

            cboDetalle.addItem(item);

        }

        JComboBox<CondicionDevolucion> cboCondicion =
                new JComboBox<>(
                        CondicionDevolucion.values());

        JTextArea txtObservacion =
                new JTextArea(5, 20);

        JButton btnDevolver =
                new JButton(
                        "Registrar Devolucion");

        agregarCampo(
                centro,
                0,
                "Equipo Prestado:",
                cboDetalle);

        agregarCampo(
                centro,
                1,
                "Condicion:",
                cboCondicion);

        agregarCampo(
                centro,
                2,
                "Observacion:",
                new JScrollPane(
                        txtObservacion));
        GridBagConstraints boton =
                new GridBagConstraints();

        boton.gridx = 0;
        boton.gridy = 3;
        boton.gridwidth = 2;
        boton.insets =
                new Insets(
                        10, 10, 10, 10);

        centro.add(
                btnDevolver,
                boton);

        centro.add(btnDevolver);
        btnDevolver.addActionListener(e -> {

            try {

                DetallePrestamoCombo seleccionado =
                        (DetallePrestamoCombo)
                                cboDetalle.getSelectedItem();

                if (seleccionado == null) {

                    JOptionPane.showMessageDialog(
                            panelPrincipal,
                            "No existen equipos pendientes.");

                    return;
                }

                int detalleId =
                        seleccionado.getDetalleId();


                service.devolverEquipo(
                        detalleId,
                        cboCondicion.getSelectedItem()
                                .toString(),
                        txtObservacion.getText(),
                        SesionActual
                                .getUsuario()
                                .getId());

                service.actualizarEstadoDisponible(
                        detalleId);

                service.finalizarPrestamo(
                        detalleId);

                txtObservacion.setText("");

                cboCondicion.setSelectedIndex(0);

                cboDetalle.removeItem(
                        seleccionado);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "\"Devolucion registrada correctamente.\\nEl equipo volvió a estado DISPONIBLE.\"");

            } catch (Exception ex) {

                ex.printStackTrace();

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        ex.getMessage());
            }

        });

        panelPrincipal.add(
                centro,
                BorderLayout.CENTER);


    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    private void agregarCampo(
            JPanel panel,
            int fila,
            String etiqueta,
            JComponent componente) {

        GridBagConstraints c =
                new GridBagConstraints();

        c.gridx = 0;
        c.gridy = fila;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(
                5, 5, 5, 5);

        panel.add(
                new JLabel(etiqueta),
                c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        panel.add(
                componente,
                c);
    }
}