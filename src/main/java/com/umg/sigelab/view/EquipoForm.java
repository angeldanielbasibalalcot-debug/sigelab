package com.umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.umg.sigelab.controller.EquipoController;
import com.umg.sigelab.controller.QrController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.EstadoEquipo;
import com.umg.sigelab.model.EstadoLaboratorio;
import com.umg.sigelab.model.Laboratorio;
import com.umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Equipos (formulario del GUI Designer: EquipoForm.form).
 * Solo captura y muestra datos. Las reglas viven en EquipoService.
 */
public class EquipoForm {

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
    private JButton btnRecargar;
    private JButton btnGenerarQr;
    private JButton btnLeerQr;
    private JTextField txtBuscar;
    private JTable tblEquipos;

    private final EquipoController controller = new EquipoController();
    private final QrController qr = new QrController();

    private DefaultTableModel modelo;
    private List<Equipo> todos = new ArrayList<>();
    private final List<Equipo> visibles = new ArrayList<>();
    private Equipo seleccionado;

    public EquipoForm() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        ejecutar(this::recargar);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ---------- Configuración ----------

    private void configurarTabla() {
        modelo = new DefaultTableModel(new String[]{
                "ID", "Código", "Nombre", "Tipo", "Marca", "Modelo",
                "Serie", "Laboratorio", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tblEquipos.setModel(modelo);
        tblEquipos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEquipos.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarCombos() {
        cmbTipo.setRenderer(new ItemRenderer());
        cmbLaboratorio.setRenderer(new ItemRenderer());
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(ev -> nuevo());
        btnGuardar.addActionListener(ev -> ejecutar(this::guardar));
        btnActualizar.addActionListener(ev -> ejecutar(this::actualizar));
        btnRecargar.addActionListener(ev -> ejecutar(this::recargar));
        btnGenerarQr.addActionListener(ev -> ejecutar(this::generarQr));
        btnLeerQr.addActionListener(ev -> ejecutar(this::leerQr));

        tblEquipos.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) {
                return;
            }
            int fila = tblEquipos.getSelectedRow();
            if (fila >= 0 && fila < visibles.size()) {
                cargarEnFormulario(visibles.get(fila));
            }
        });

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent ev) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent ev) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent ev) {
                filtrar();
            }
        });
    }

    // ---------- Acciones de botones ----------

    private void nuevo() {
        tblEquipos.clearSelection();
        limpiarFormulario();
        txtCodigo.requestFocus();
    }

    private void guardar() {
        if (seleccionado != null) {
            aviso("Ese equipo ya existe. Pulse Nuevo para registrar otro "
                    + "o Actualizar para guardar cambios.");
            return;
        }
        controller.crear(leerFormulario());
        info("Equipo registrado correctamente.");
        recargar();
    }

    private void actualizar() {
        if (seleccionado == null) {
            aviso("Seleccione un equipo en la tabla.");
            return;
        }
        controller.actualizar(leerFormulario());
        info("Equipo actualizado correctamente.");
        recargar();
    }

    private void recargar() {
        cargarCombos();
        todos = controller.listar();
        filtrar();
        limpiarFormulario();
    }

    private void generarQr() {
        if (seleccionado == null) {
            aviso("Seleccione un equipo de la tabla para generar su QR.");
            return;
        }
        String codigo = seleccionado.getCodigoInventario();
        BufferedImage imagen = qr.generar(codigo);
        Object[] opciones = {"Guardar imagen", "Cerrar"};
        int resp = JOptionPane.showOptionDialog(panelPrincipal,
                new JLabel(new ImageIcon(imagen)), "QR de " + codigo,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, opciones, opciones[1]);
        if (resp == 0) {
            guardarImagen(imagen, codigo);
        }
    }

    private void guardarImagen(BufferedImage imagen, String codigo) {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File(codigo + ".png"));
        if (selector.showSaveDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = selector.getSelectedFile().toPath();
        String nombre = destino.getFileName().toString();
        if (!nombre.toLowerCase().endsWith(".png")) {
            destino = destino.resolveSibling(nombre + ".png");
        }
        qr.guardarPng(imagen, destino);
        info("Imagen guardada en: " + destino);
    }

    /**
     * RN-19: leer el QR solo identifica y muestra; no cambia ningún estado.
     */
    private void leerQr() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (png, jpg)", "png", "jpg", "jpeg"));
        if (selector.showOpenDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Equipo equipo = qr.leer(selector.getSelectedFile().toPath());
        if (equipo == null) {
            aviso("El código leído no corresponde a ningún equipo registrado.");
            return;
        }
        tblEquipos.clearSelection();
        cargarEnFormulario(equipo);

        String mensaje = "Equipo identificado: " + equipo.getCodigoInventario()
                + " - " + equipo.getNombre()
                + "\nEstado: " + equipo.getEstado();
        if (equipo.getEstado() == EstadoEquipo.DISPONIBLE) {
            mensaje += "\nDisponible para agregar a un préstamo.";
        } else {
            mensaje += "\nBloqueado: no se puede prestar en este estado.";
        }
        info(mensaje);
    }

    // ---------- Datos y tabla ----------

    private void cargarCombos() {
        cmbTipo.removeAllItems();
        TipoEquipo marcadorTipo = new TipoEquipo();
        marcadorTipo.setId(0);
        cmbTipo.addItem(marcadorTipo);
        for (TipoEquipo tipo : controller.listarTipos()) {
            cmbTipo.addItem(tipo);
        }

        cmbLaboratorio.removeAllItems();
        Laboratorio marcadorLab = new Laboratorio();
        marcadorLab.setId(0);
        cmbLaboratorio.addItem(marcadorLab);
        for (Laboratorio lab : controller.listarLaboratorios()) {
            if (lab.getEstado() == EstadoLaboratorio.ACTIVO) {
                cmbLaboratorio.addItem(lab);
            }
        }
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim().toLowerCase();
        visibles.clear();
        modelo.setRowCount(0);
        for (Equipo e : todos) {
            if (!texto.isEmpty() && !coincide(e, texto)) {
                continue;
            }
            visibles.add(e);
            modelo.addRow(new Object[]{
                    e.getId(),
                    e.getCodigoInventario(),
                    e.getNombre(),
                    e.getTipoEquipo() == null ? "" : e.getTipoEquipo().getNombre(),
                    nz(e.getMarca()),
                    nz(e.getModelo()),
                    nz(e.getNumeroSerie()),
                    e.getLaboratorio() == null ? "" : e.getLaboratorio().getNombre(),
                    e.getEstado() == null ? "" : e.getEstado().name()
            });
        }
    }

    private boolean coincide(Equipo e, String texto) {
        return nz(e.getCodigoInventario()).toLowerCase().contains(texto)
                || nz(e.getNombre()).toLowerCase().contains(texto)
                || nz(e.getMarca()).toLowerCase().contains(texto)
                || nz(e.getModelo()).toLowerCase().contains(texto)
                || nz(e.getNumeroSerie()).toLowerCase().contains(texto);
    }

    // ---------- Formulario ----------

    private Equipo leerFormulario() {
        Equipo e = new Equipo();
        if (seleccionado != null) {
            e.setId(seleccionado.getId());
        }
        e.setCodigoInventario(txtCodigo.getText());
        e.setNombre(txtNombre.getText());
        e.setTipoEquipo((TipoEquipo) cmbTipo.getSelectedItem());
        e.setMarca(txtMarca.getText());
        e.setModelo(txtModelo.getText());
        e.setNumeroSerie(txtSerie.getText());
        e.setLaboratorio((Laboratorio) cmbLaboratorio.getSelectedItem());
        e.setObservaciones(txtObservaciones.getText());
        return e;
    }

    private void cargarEnFormulario(Equipo e) {
        seleccionado = e;
        txtId.setText(String.valueOf(e.getId()));
        txtCodigo.setText(nz(e.getCodigoInventario()));
        txtNombre.setText(nz(e.getNombre()));
        txtMarca.setText(nz(e.getMarca()));
        txtModelo.setText(nz(e.getModelo()));
        txtSerie.setText(nz(e.getNumeroSerie()));
        txtObservaciones.setText(nz(e.getObservaciones()));
        txtEstado.setText(e.getEstado() == null ? "" : e.getEstado().name());
        seleccionarTipo(e.getTipoEquipo());
        seleccionarLaboratorio(e.getLaboratorio());
    }

    private void limpiarFormulario() {
        seleccionado = null;
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtSerie.setText("");
        txtObservaciones.setText("");
        txtEstado.setText("");
        if (cmbTipo.getItemCount() > 0) {
            cmbTipo.setSelectedIndex(0);
        }
        if (cmbLaboratorio.getItemCount() > 0) {
            cmbLaboratorio.setSelectedIndex(0);
        }
    }

    private void seleccionarTipo(TipoEquipo tipo) {
        if (tipo == null) {
            cmbTipo.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < cmbTipo.getItemCount(); i++) {
            if (cmbTipo.getItemAt(i).getId() == tipo.getId()) {
                cmbTipo.setSelectedIndex(i);
                return;
            }
        }
        cmbTipo.addItem(tipo);
        cmbTipo.setSelectedItem(tipo);
    }

    private void seleccionarLaboratorio(Laboratorio lab) {
        if (lab == null) {
            cmbLaboratorio.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < cmbLaboratorio.getItemCount(); i++) {
            if (cmbLaboratorio.getItemAt(i).getId() == lab.getId()) {
                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }
        // Equipo de un laboratorio inactivo: se muestra, pero no aparece en nuevas listas.
        cmbLaboratorio.addItem(lab);
        cmbLaboratorio.setSelectedItem(lab);
    }

    // ---------- Utilidades ----------

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException ex) {
            aviso(ex.getMessage());
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panelPrincipal,
                    "No se pudo completar la operación. Intente de nuevo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje,
                "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje,
                "Equipos", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String nz(String texto) {
        return texto == null ? "" : texto;
    }

    private static String textoItem(Object valor) {
        if (valor instanceof TipoEquipo t) {
            return t.getId() == 0 ? "-- Seleccione --" : t.getNombre();
        }
        if (valor instanceof Laboratorio l) {
            return l.getId() == 0 ? "-- Seleccione --"
                    : l.getCodigo() + " - " + l.getNombre();
        }
        return "";
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
        panelPrincipal.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(10, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("ID:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setEnabled(false);
        panel1.add(txtId, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Código de inventario:");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        panel1.add(txtCodigo, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Nombre:");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        panel1.add(txtNombre, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Tipo de equipo:");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbTipo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        cmbTipo.setModel(defaultComboBoxModel1);
        panel1.add(cmbTipo, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Marca:");
        panel1.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtMarca = new JTextField();
        panel1.add(txtMarca, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Modelo:");
        panel1.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtModelo = new JTextField();
        panel1.add(txtModelo, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Número de serie:");
        panel1.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtSerie = new JTextField();
        panel1.add(txtSerie, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Laboratorio:");
        panel1.add(label8, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbLaboratorio = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        cmbLaboratorio.setModel(defaultComboBoxModel2);
        panel1.add(cmbLaboratorio, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Estado:");
        panel1.add(label9, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtEstado = new JTextField();
        txtEstado.setEnabled(false);
        panel1.add(txtEstado, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Observaciones:");
        panel1.add(label10, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtObservaciones = new JTextField();
        panel1.add(txtObservaciones, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panelBotones, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelBotones.add(btnNuevo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelBotones.add(btnGuardar, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelBotones.add(btnActualizar, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRecargar = new JButton();
        btnRecargar.setText("Recargar");
        panelBotones.add(btnRecargar, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGenerarQr = new JButton();
        btnGenerarQr.setText("Generar QR");
        panelBotones.add(btnGenerarQr, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLeerQr = new JButton();
        btnLeerQr.setText("Leer QR");
        panelBotones.add(btnLeerQr, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panelBotones.add(spacer1, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Buscar:");
        panel2.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtBuscar = new JTextField();
        panel2.add(txtBuscar, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblEquipos = new JTable();
        scrollPane1.setViewportView(tblEquipos);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

    private static class ItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor,
                                                      int indice, boolean seleccionado, boolean foco) {
            super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
            setText(textoItem(valor));
            return this;
        }
    }
}