package com.umg.sigelab.view;

import com.umg.sigelab.controller.EquipoController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import com.umg.sigelab.controller.QrController;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class EquipoForm {

    private final EquipoController controller = new EquipoController();

    private final QrController qrController = new QrController();

    private static final Path CARPETA_QR = Path.of("D:\\SIGELAB\\QR");

    private final JButton btnGenerarQr = new JButton("Generar QR");
    private final JButton btnLeerQr = new JButton("Leer QR");

    private final JPanel panelPrincipal =
            new JPanel(new BorderLayout(10, 10));

    private final JTextField txtId = new JTextField();
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtMarca = new JTextField();
    private final JTextField txtModelo = new JTextField();
    private final JTextField txtSerie = new JTextField();
    private final JTextField txtEstado = new JTextField();
    private final JTextField txtBuscar = new JTextField();

    private final JTextArea txtObservaciones = new JTextArea(3, 30);

    private final JComboBox<TipoEquipo> cmbTipo = new JComboBox<>();
    private final JComboBox<Laboratorio> cmbLaboratorio = new JComboBox<>();

    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnRecargar = new JButton("Recargar");

    private final JLabel lblEstado = new JLabel(" ");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(new Object[]{
                    "ID", "Código", "Nombre", "Tipo", "Marca",
                    "Modelo", "Serie", "Laboratorio", "Estado",
                    "Observaciones"
            }, 0) {
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

    private List<Equipo> equipos = List.of();
    private Equipo seleccionado;
    private boolean ocupado;
    private boolean datosCargados;

    public EquipoForm() {
        construirPantalla();
        configurarEventos();
        nuevo();
        recargar();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    private void construirPantalla() {
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12));

        txtId.setEditable(false);
        txtEstado.setEditable(false);

        cmbTipo.setEditable(false);
        cmbLaboratorio.setEditable(false);

        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);

        JPanel campos = new JPanel(new GridBagLayout());

        agregarCampo(campos, 0, 0, "ID:", txtId);
        agregarCampo(campos, 0, 2, "Código de inventario:", txtCodigo);

        agregarCampo(campos, 1, 0, "Nombre:", txtNombre);
        agregarCampo(campos, 1, 2, "Tipo de equipo:", cmbTipo);

        agregarCampo(campos, 2, 0, "Marca:", txtMarca);
        agregarCampo(campos, 2, 2, "Modelo:", txtModelo);

        agregarCampo(campos, 3, 0, "Número de serie:", txtSerie);
        agregarCampo(campos, 3, 2, "Laboratorio:", cmbLaboratorio);

        agregarCampo(campos, 4, 0, "Estado:", txtEstado);
        agregarCampo(campos, 4, 2, "Observaciones:",
                new JScrollPane(txtObservaciones));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnRecargar);
        botones.add(btnGenerarQr);
        botones.add(btnLeerQr);

        JPanel superior = new JPanel(new BorderLayout(8, 8));
        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        JPanel busqueda = new JPanel(new BorderLayout(8, 8));
        busqueda.add(new JLabel("Buscar equipo:"), BorderLayout.WEST);
        busqueda.add(txtBuscar, BorderLayout.CENTER);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowSorter(ordenador);
        tabla.setRowHeight(25);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] anchos = {60, 130, 220, 130, 120, 140, 140, 260, 150, 280};

        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JPanel listado = new JPanel(new BorderLayout(8, 8));
        listado.add(busqueda, BorderLayout.NORTH);
        listado.add(new JScrollPane(tabla), BorderLayout.CENTER);
        listado.add(lblEstado, BorderLayout.SOUTH);

        panelPrincipal.add(superior, BorderLayout.NORTH);
        panelPrincipal.add(listado, BorderLayout.CENTER);
    }

    private void agregarCampo(
            JPanel panel, int fila, int columna,
            String etiqueta, JComponent componente) {

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = columna;
        c.gridy = fila;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 10);
        panel.add(new JLabel(etiqueta), c);

        c.gridx = columna + 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(componente, c);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar(false));
        btnActualizar.addActionListener(e -> guardar(true));
        btnRecargar.addActionListener(e -> recargar());
        btnGenerarQr.addActionListener(e -> generarQr());
        btnLeerQr.addActionListener(e -> leerQr());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !ocupado) {
                seleccionarFila();
            }
        });

        txtBuscar.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        filtrar();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        filtrar();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        filtrar();
                    }
                });
    }

    private void nuevo() {
        seleccionado = null;
        tabla.clearSelection();

        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtSerie.setText("");
        txtObservaciones.setText("");
        txtEstado.setText(EstadoEquipo.DISPONIBLE.name());

        cmbTipo.setSelectedIndex(-1);
        cmbLaboratorio.setSelectedIndex(-1);

        actualizarControles();
        txtCodigo.requestFocusInWindow();
    }

    private void seleccionarFila() {
        int filaVista = tabla.getSelectedRow();

        if (filaVista < 0) {
            nuevo();
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        seleccionado = equipos.get(filaModelo);

        txtId.setText(String.valueOf(seleccionado.getId()));
        txtCodigo.setText(seleccionado.getCodigoInventario());
        txtNombre.setText(seleccionado.getNombre());
        txtMarca.setText(texto(seleccionado.getMarca()));
        txtModelo.setText(texto(seleccionado.getModelo()));
        txtSerie.setText(texto(seleccionado.getNumeroSerie()));
        txtObservaciones.setText(texto(seleccionado.getObservaciones()));
        txtEstado.setText(seleccionado.getEstado().name());

        cmbTipo.setSelectedIndex(-1);
        for (int i = 0; i < cmbTipo.getItemCount(); i++) {
            if (cmbTipo.getItemAt(i).getId()
                    == seleccionado.getTipoEquipo().getId()) {
                cmbTipo.setSelectedIndex(i);
                break;
            }
        }

        cmbLaboratorio.setSelectedIndex(-1);
        for (int i = 0; i < cmbLaboratorio.getItemCount(); i++) {
            if (cmbLaboratorio.getItemAt(i).getId()
                    == seleccionado.getLaboratorio().getId()) {
                cmbLaboratorio.setSelectedIndex(i);
                break;
            }
        }

        actualizarControles();
    }

    private void recargar() {
        if (ocupado) {
            return;
        }

        ocupado = true;
        actualizarControles();
        lblEstado.setText("Cargando equipos, tipos y laboratorios...");

        new SwingWorker<Void, Void>() {
            private List<Equipo> nuevosEquipos;
            private List<TipoEquipo> tipos;
            private List<Laboratorio> laboratorios;

            @Override
            protected Void doInBackground() {
                tipos = controller.listarTipos();
                laboratorios = controller.listarLaboratorios();
                nuevosEquipos = controller.listar();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    cmbTipo.removeAllItems();
                    for (TipoEquipo tipo : tipos) {
                        cmbTipo.addItem(tipo);
                    }

                    cmbLaboratorio.removeAllItems();
                    for (Laboratorio laboratorio : laboratorios) {
                        cmbLaboratorio.addItem(laboratorio);
                    }

                    equipos = nuevosEquipos;
                    modeloTabla.setRowCount(0);

                    for (Equipo equipo : equipos) {
                        modeloTabla.addRow(new Object[]{
                                equipo.getId(),
                                equipo.getCodigoInventario(),
                                equipo.getNombre(),
                                equipo.getTipoEquipo().getNombre(),
                                texto(equipo.getMarca()),
                                texto(equipo.getModelo()),
                                texto(equipo.getNumeroSerie()),
                                equipo.getLaboratorio().toString(),
                                equipo.getEstado().name(),
                                texto(equipo.getObservaciones())
                        });
                    }

                    datosCargados = true;
                    nuevo();
                    lblEstado.setText("Equipos cargados: " + equipos.size());

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    datosCargados = false;
                    lblEstado.setText("Carga interrumpida. Pulse Recargar.");
                } catch (ExecutionException ex) {
                    datosCargados = false;
                    mostrarError(ex.getCause(),
                            "No se pudieron cargar los datos. Pulse Recargar.");
                    lblEstado.setText("No se completó la carga.");
                } finally {
                    ocupado = false;
                    actualizarControles();
                }
            }
        }.execute();
    }

    private void guardar(boolean actualizar) {
        if (ocupado || !datosCargados) {
            return;
        }

        if (actualizar && seleccionado == null) {
            mostrarError(new BusinessException(
                    "Seleccione un equipo en la tabla."), "");
            return;
        }

        Equipo datos = new Equipo(
                actualizar ? seleccionado.getId() : 0,
                txtCodigo.getText(),
                txtNombre.getText(),
                (TipoEquipo) cmbTipo.getSelectedItem(),
                txtMarca.getText(),
                txtModelo.getText(),
                txtSerie.getText(),
                (Laboratorio) cmbLaboratorio.getSelectedItem(),
                actualizar
                        ? seleccionado.getEstado()
                        : EstadoEquipo.DISPONIBLE,
                txtObservaciones.getText()
        );

        ocupado = true;
        actualizarControles();
        lblEstado.setText("Guardando equipo...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (actualizar) {
                    controller.actualizar(datos);
                } else {
                    controller.crear(datos);
                }
                return null;
            }

            @Override
            protected void done() {
                boolean correcto = false;

                try {
                    get();
                    correcto = true;

                    JOptionPane.showMessageDialog(
                            panelPrincipal,
                            actualizar
                                    ? "Equipo actualizado correctamente."
                                    : "Equipo creado correctamente.",
                            "Operación realizada",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    datosCargados = false;
                    lblEstado.setText(
                            "Operación interrumpida. Recargue antes de continuar.");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();

                    if (!(causa instanceof BusinessException)) {
                        datosCargados = false;
                    }

                    mostrarError(causa,
                            "No se pudo confirmar la operación.\n"
                                    + "Pulse Recargar antes de volver a intentarlo.");

                    lblEstado.setText("Revise los datos o recargue la lista.");
                } finally {
                    ocupado = false;
                    actualizarControles();
                }

                if (correcto) {
                    nuevo();
                    txtBuscar.setText("");
                    recargar();
                }
            }
        }.execute();
    }

    private void actualizarControles() {
        boolean disponible = !ocupado && datosCargados;

        btnNuevo.setEnabled(disponible);
        btnGuardar.setEnabled(disponible && seleccionado == null);
        btnActualizar.setEnabled(disponible && seleccionado != null);
        btnRecargar.setEnabled(!ocupado);
        btnGenerarQr.setEnabled(disponible && seleccionado != null);
        btnLeerQr.setEnabled(disponible);

        tabla.setEnabled(disponible);
        txtBuscar.setEnabled(disponible);

        txtCodigo.setEnabled(disponible);
        txtNombre.setEnabled(disponible);
        txtMarca.setEnabled(disponible);
        txtModelo.setEnabled(disponible);
        txtSerie.setEnabled(disponible);
        txtObservaciones.setEnabled(disponible);
        cmbTipo.setEnabled(disponible);
        cmbLaboratorio.setEnabled(disponible);
    }

    private void filtrar() {
        String busqueda = txtBuscar.getText().trim();

        ordenador.setRowFilter(busqueda.isEmpty()
                ? null
                : RowFilter.regexFilter(
                "(?iu)" + Pattern.quote(busqueda),
                1, 2, 3, 4, 5, 6, 7, 8));

        if (!ocupado) {
            nuevo();
        }
    }

    private void mostrarError(Throwable error, String mensajeTecnico) {
        boolean negocio = error instanceof BusinessException;

        if (!negocio) {
            error.printStackTrace();
        }

        JOptionPane.showMessageDialog(
                panelPrincipal,
                negocio ? error.getMessage() : mensajeTecnico,
                negocio ? "Revise los datos" : "Error",
                negocio
                        ? JOptionPane.WARNING_MESSAGE
                        : JOptionPane.ERROR_MESSAGE);
    }

    private void generarQr() {
        if (ocupado || seleccionado == null) {
            return;
        }

        // Usamos el código del registro seleccionado, no cambios sin guardar.
        String codigo = seleccionado.getCodigoInventario();
        int id = seleccionado.getId();

        ejecutarTareaQr(
                "Generando QR del equipo...",
                () -> qrController.generar(codigo),
                imagen -> {
                    JPanel vista = new JPanel(new BorderLayout(8, 8));
                    vista.add(
                            new JLabel(new ImageIcon(imagen)),
                            BorderLayout.CENTER
                    );
                    vista.add(
                            new JLabel(
                                    "Código: " + codigo,
                                    SwingConstants.CENTER
                            ),
                            BorderLayout.SOUTH
                    );

                    int opcion = JOptionPane.showOptionDialog(
                            panelPrincipal,
                            vista,
                            "QR del equipo",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            new Object[]{"Guardar PNG", "Cerrar"},
                            "Guardar PNG"
                    );

                    if (opcion == 0) {
                        guardarImagenQr(imagen, id);
                    }
                }
        );
    }

    private JFileChooser crearSelectorQr() throws java.io.IOException {
        Files.createDirectories(CARPETA_QR);

        JFileChooser selector = new JFileChooser(CARPETA_QR.toFile());
        selector.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selector.setAcceptAllFileFilterUsed(false);

        return selector;
    }

    private void guardarImagenQr(BufferedImage imagen, int id) {
        if (ocupado) {
            return;
        }

        try {
            JFileChooser selector = crearSelectorQr();
            selector.setDialogTitle("Guardar QR en el disco D");
            selector.setFileFilter(
                    new FileNameExtensionFilter("Imagen PNG (*.png)", "png")
            );

            // El ID evita usar caracteres inválidos del código como nombre.
            selector.setSelectedFile(
                    CARPETA_QR.resolve("equipo-" + id + ".png").toFile()
            );

            if (selector.showSaveDialog(panelPrincipal)
                    != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path elegido = selector.getSelectedFile()
                    .toPath().toAbsolutePath().normalize();

            if (!elegido.getFileName().toString()
                    .toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                elegido = elegido.resolveSibling(
                        elegido.getFileName().toString() + ".png"
                );
            }

            // Respetamos tu configuración: guardar únicamente en D.
            if (!elegido.startsWith(Path.of("D:\\"))) {
                throw new BusinessException(
                        "Seleccione una carpeta del disco D para guardar el QR."
                );
            }

            if (Files.exists(elegido)) {
                int respuesta = JOptionPane.showConfirmDialog(
                        panelPrincipal,
                        "El archivo ya existe.\n¿Desea reemplazarlo?",
                        "Confirmar reemplazo",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            final Path destino = elegido;

            ejecutarTareaQr(
                    "Guardando imagen QR...",
                    () -> {
                        qrController.guardarPng(imagen, destino);
                        return destino;
                    },
                    archivo -> JOptionPane.showMessageDialog(
                            panelPrincipal,
                            "QR guardado en:\n" + archivo,
                            "Imagen guardada",
                            JOptionPane.INFORMATION_MESSAGE
                    )
            );

        } catch (BusinessException ex) {
            mostrarError(ex, "");
        } catch (Exception ex) {
            mostrarError(
                    ex,
                    "No se pudo preparar el guardado en D:\\SIGELAB\\QR."
            );
        }
    }

    private void leerQr() {
        if (ocupado || !datosCargados) {
            return;
        }

        try {
            JFileChooser selector = crearSelectorQr();
            selector.setDialogTitle("Seleccionar imagen con código QR");
            selector.setFileFilter(
                    new FileNameExtensionFilter(
                            "Imágenes PNG y JPG",
                            "png", "jpg", "jpeg"
                    )
            );

            if (selector.showOpenDialog(panelPrincipal)
                    != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path archivo = selector.getSelectedFile().toPath();

            ejecutarTareaQr(
                    "Leyendo QR y consultando el equipo...",
                    () -> qrController.leer(archivo),
                    this::mostrarEquipoQr
            );

        } catch (Exception ex) {
            mostrarError(
                    ex,
                    "No se pudo abrir el selector de imágenes en D:\\SIGELAB\\QR."
            );
        }
    }

    private void mostrarEquipoQr(Equipo equipo) {
        String informacion =
                "ID: " + equipo.getId()
                        + "\nCódigo: " + equipo.getCodigoInventario()
                        + "\nNombre: " + equipo.getNombre()
                        + "\nTipo: " + equipo.getTipoEquipo().getNombre()
                        + "\nMarca: " + texto(equipo.getMarca())
                        + "\nModelo: " + texto(equipo.getModelo())
                        + "\nSerie: " + texto(equipo.getNumeroSerie())
                        + "\nLaboratorio: " + equipo.getLaboratorio()
                        + "\nEstado actual: " + equipo.getEstado().name()
                        + "\nObservaciones: " + texto(equipo.getObservaciones())
                        + "\n\nEsta consulta no modifica el estado del equipo.";

        JTextArea detalle = new JTextArea(informacion, 15, 45);
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setCaretPosition(0);

        JOptionPane.showMessageDialog(
                panelPrincipal,
                new JScrollPane(detalle),
                "Equipo identificado por QR",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Ejecuta la lectura, generación o guardado fuera del hilo de la pantalla.
     * La actualización de la interfaz ocurre en done().
     */
    private <T> void ejecutarTareaQr(
            String mensaje,
            Callable<T> tarea,
            Consumer<T> alCompletar) {

        if (ocupado) {
            return;
        }

        ocupado = true;
        actualizarControles();
        lblEstado.setText(mensaje);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                T resultado = null;
                boolean correcto = false;

                try {
                    resultado = get();
                    correcto = true;
                    lblEstado.setText("Operación QR completada.");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lblEstado.setText("Operación QR interrumpida.");

                } catch (ExecutionException ex) {
                    mostrarError(
                            ex.getCause(),
                            "No se pudo completar la operación QR."
                    );
                    lblEstado.setText("No se completó la operación QR.");

                } finally {
                    ocupado = false;
                    actualizarControles();
                }

                if (correcto) {
                    alCompletar.accept(resultado);
                }
            }
        }.execute();
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
