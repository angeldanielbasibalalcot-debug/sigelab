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
        JPanel centro = new JPanel();

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

        centro.add(
                new JLabel(
                        "Equipo Prestado:"));

        centro.add(cboDetalle);

        centro.add(
                new JLabel(
                        "Condicion:"));

        centro.add(cboCondicion);

        centro.add(
                new JLabel(
                        "Observacion:"));

        centro.add(
                new JScrollPane(
                        txtObservacion));

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
                        "Devolucion registrada correctamente");

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
}