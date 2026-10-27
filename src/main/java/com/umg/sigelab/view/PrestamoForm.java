package com.umg.sigelab.view;

import javax.swing.*;
import java.awt.*;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.service.PrestamoService;

import javax.swing.JComboBox;
import java.util.List;

import com.umg.sigelab.model.Prestamo;
import com.umg.sigelab.model.EstadoPrestamo;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.model.DetallePrestamo;

import java.util.Date;

public class PrestamoForm {

    private JPanel panelPrincipal;

    private PrestamoService service =
            new PrestamoService();

    public PrestamoForm() {

        panelPrincipal = new JPanel(new BorderLayout());

        JLabel titulo = new JLabel(
                "Modulo de Prestamos",
                SwingConstants.CENTER);

        titulo.setFont(
                titulo.getFont().deriveFont(
                        Font.BOLD,
                        20f));

        panelPrincipal.add(
                titulo,
                BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(
                centro,
                BoxLayout.Y_AXIS));

        JComboBox<Equipo> cboEquipos =
                new JComboBox<>();

        List<Equipo> equipos =
                service.listarEquiposDisponibles();

        for (Equipo equipo : equipos) {
            cboEquipos.addItem(equipo);
        }

        centro.add(new JLabel("Equipo:"));
        centro.add(cboEquipos);

        JTextField txtSolicitante =
                new JTextField(20);

        JTextField txtFechaEsperada =
                new JTextField(20);

        JButton btnAgregar =
                new JButton("Agregar Equipo");

        DefaultListModel<Equipo> modeloLista =
                new DefaultListModel<>();

        JList<Equipo> listaEquipos =
                new JList<>(modeloLista);

        JButton btnRegistrar =
                new JButton("Registrar Prestamo");

        centro.add(new JLabel("Solicitante:"));
        centro.add(txtSolicitante);

        centro.add(
                new JLabel(
                        "Fecha Esperada (YYYY-MM-DD):"));
        centro.add(txtFechaEsperada);

        centro.add(btnAgregar);

        centro.add(
                new JLabel(
                        "Equipos Seleccionados:"));

        centro.add(
                new JScrollPane(listaEquipos));

        centro.add(btnRegistrar);
        btnRegistrar.addActionListener(e -> {

            try {

                Prestamo prestamo =
                        new Prestamo();

                prestamo.setSolicitante(
                        txtSolicitante.getText());

                prestamo.setFechaPrestamo(
                        new Date());

                prestamo.setFechaEsperadaDevolucion(
                        new Date());

                prestamo.setUsuarioRegistraId(
                        SesionActual.getUsuario().getId());

                prestamo.setEstado(
                        EstadoPrestamo.ACTIVO);

                prestamo.setObservaciones(
                        "Prestamo generado desde formulario");

                int idGenerado =
                        service.guardarPrestamo(prestamo);
                for (int i = 0; i < modeloLista.size(); i++) {

                    Equipo equipo =
                            modeloLista.getElementAt(i);

                    DetallePrestamo detalle =
                            new DetallePrestamo();

                    detalle.setPrestamoId(
                            idGenerado);

                    detalle.setEquipoId(
                            equipo.getId());

                    service.guardarDetallePrestamo(
                            detalle);

                    service.actualizarEstadoEquipo(
                            equipo.getId());
                }

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Prestamo registrado. ID: "
                                + idGenerado);

            } catch (Exception ex) {

                ex.printStackTrace();

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        ex.getMessage());

            }

        });

        btnAgregar.addActionListener(e -> {

            Equipo seleccionado =
                    (Equipo) cboEquipos.getSelectedItem();

            if (seleccionado == null) {
                return;
            }

            boolean existe = false;

            for (int i = 0; i < modeloLista.size(); i++) {

                if (modeloLista.get(i).getId()
                        == seleccionado.getId()) {

                    existe = true;
                    break;
                }
            }

            if (!existe) {

                modeloLista.addElement(seleccionado);

            } else {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Ese equipo ya fue agregado.");
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