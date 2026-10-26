package com.umg.sigelab.view;

import javax.swing.*;
import java.awt.*;

public class PrestamoForm {

    private JPanel panelPrincipal;

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

        JTextArea area =
                new JTextArea();

        area.setEditable(false);

        area.setText(
                "Modulo de prestamos en desarrollo");

        panelPrincipal.add(
                area,
                BorderLayout.CENTER);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}