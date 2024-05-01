package client;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shape.Rectangle;
import shape.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;

public class ClientRunner {

    private static JFrame frame;
    private static JPanel drawPanel;
    private JPanel drawControlPanel;
    private String[] options = {"펜", "직선", "사각형", "원", "지우개"};
    private final static String SOURCE = "Source";
    private final static String COMMAND = "Command";
    private final static String SERVER = "Server";
    private Color color;
    private Color[] colors = {Color.GRAY, Color.LIGHT_GRAY, Color.darkGray, Color.black, Color.orange, Color.green,
            Color.red, Color.pink, Color.blue, Color.cyan, Color.magenta, Color.YELLOW,
            new Color(125, 55, 237), new Color(255, 99, 71), new Color(240, 230, 140),
            new Color(0, 250, 154), new Color(0, 206, 209), new Color(238, 130, 238),
            Color.WHITE};
    private String shape = "펜";
    private static State state = new State(new ArrayList<>());
    private ArrayList<CustomShape> shapesPreview = new ArrayList<>();
    private int x1, y1, x2, y2;
    private static BasicStroke stroke;
    private JComboBox<Integer> thicknessSelector;
    private JCheckBox fillSelector;
    private Boolean fill;
    private String username = "default";
    private int time = 60000;
    private static Client client;

    public static void main(String[] args) throws IOException, ConnectException, UnknownHostException {
        client = new Client();
        try {
            String host = "localhost";
            int port = 1000;
            client.initiate(host, port);
            EventQueue.invokeLater(() -> {
                try {
                    ClientRunner window = new ClientRunner();
                    window.frame.setVisible(true);
                } catch (java.lang.Exception e) {
                    e.printStackTrace();
                }
            });
            startListeningServer();
        } catch (IOException e1) {
            JOptionPane.showConfirmDialog(null, e1.getMessage(), e1.getMessage(), JOptionPane.YES_NO_OPTION);
        }
    }

    public ClientRunner() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.NORMAL);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                try {
                    PrintWriter out = new PrintWriter(client.getOsw(), true);
                    out.println("exit");
                    client.disconnect();

                    System.out.println("소켓 연결 끊김");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                e.getWindow().dispose();
            }
        });
        frame.setSize(2300, 1000);

        drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                rePaint(g2d);
            }
        };
        drawPanel.setPreferredSize(new Dimension(2300, 1000));
        drawPanel.setBackground(Color.WHITE);
        drawPanel.addMouseListener(ma);
        drawPanel.addMouseMotionListener(ma);

        initDrawControlPanel();

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(drawPanel, BorderLayout.CENTER);
        frame.getContentPane().add(drawControlPanel, BorderLayout.NORTH);
    }

    private void initDrawControlPanel() {
        drawControlPanel = new JPanel();
        drawControlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        drawControlPanel.setBackground(Color.PINK);

        for (int i = 0; i < colors.length; i++) {
            JButton btn = new JButton();
            btn.setBackground(colors[i]);
            btn.setContentAreaFilled(false);
            btn.setOpaque(true);
            btn.addActionListener(colorSelectAL);
            drawControlPanel.add(btn);
        }

        for (int i = 0; i < options.length; i++) {
            JButton btn = new JButton(options[i]);
            btn.addActionListener(shapeSelectAL);
            btn.setPreferredSize(new Dimension(100, 25));
            drawControlPanel.add(btn);
        }

        JLabel label = new JLabel("선굵기");
        label.setForeground(Color.BLACK);
        drawControlPanel.add(label);
        thicknessSelector = new JComboBox<>();
        drawControlPanel.add(thicknessSelector);
        for (int i = 0; i < 10; i++) {
            thicknessSelector.addItem(i + 1);
        }

        fillSelector = new JCheckBox("채우기");
        fillSelector.setBackground(Color.LIGHT_GRAY);
        drawControlPanel.add(fillSelector);
    }

    ActionListener colorSelectAL = e -> {
        JButton bt = (JButton) e.getSource();
        color = bt.getBackground();
    };

    ActionListener shapeSelectAL = e -> {
        JButton bt = (JButton) e.getSource();
        shape = bt.getText();
    };

    MouseAdapter ma = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            x1 = e.getX();
            y1 = e.getY();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (color == null) {
                color = Color.black;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int thickness = (int) thicknessSelector.getSelectedItem();
            stroke = new BasicStroke(thickness);
            fill = fillSelector.isSelected();
            x2 = e.getX();
            y2 = e.getY();

            switch (shape) {
                case "펜":
                    Shape line = new Line2D.Double(x1, y1, x2, y2);
                    Line myline = new Line(line, color, username, thickness, fill);
                    state.getShapes().add(myline);
                    shapesPreview.add(myline);
                    draw();

                    try {
                        client.request(myline, time);
                    } catch (IOException | RuntimeException ex) {
                        JOptionPane.showConfirmDialog(null, ex.getMessage(), ex.getMessage(), JOptionPane.YES_NO_OPTION);
                    }

                    x1 = x2;
                    y1 = y2;
                    break;

                case "지우개":
                    Shape eraser = new Line2D.Double(x1, y1, x2, y2);
                    Line myEraser = new Line(eraser, Color.white, username, thickness * 10, fill);
                    state.getShapes().add(myEraser);
                    shapesPreview.add(myEraser);
                    draw();

                    try {
                        client.request(myEraser, time);
                    } catch (IOException | RuntimeException ex) {
                        JOptionPane.showConfirmDialog(null, ex.getMessage(), ex.getMessage(), JOptionPane.YES_NO_OPTION);
                    }

                    x1 = x2;
                    y1 = y2;
                    break;

                default:
                    Shape lineY = new Line2D.Double(x1, y1, x1, y2);
                    Shape lineX = new Line2D.Double(x1, y1, x2, y1);
                    shapesPreview.add(new Line(lineY, color, username, thickness, fill));
                    shapesPreview.add(new Line(lineX, color, username, thickness, fill));
                    draw();
                    break;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Shape s;
            shapesPreview.clear();
            drawPanel.repaint();

            if (shape.equals("펜")) {
                shapesPreview.clear();
            } else if (shape.equals("직선")) {
                s = new Line2D.Double(x1, y1, e.getX(), e.getY());
                Line myline = new Line(s, color, username, (int) stroke.getLineWidth(), fill);
                state.getShapes().add(myline);

                try {
                    client.request(myline, time);
                } catch (IOException | RuntimeException ex) {
                    JOptionPane.showConfirmDialog(null, ex.getMessage(), ex.getMessage(), JOptionPane.YES_NO_OPTION);
                }
            } else if (shape.equals("사각형")) {
                s = ShapeFactory.createRectangle(x1, y1, e.getX(), e.getY());
                Rectangle myRectangle = new Rectangle(s, color, username, (int) stroke.getLineWidth(), fill);
                state.getShapes().add(myRectangle);

                try {
                    client.request(myRectangle, time);
                } catch (IOException | RuntimeException ex) {
                    JOptionPane.showConfirmDialog(null, ex.getMessage(), ex.getMessage(), JOptionPane.YES_NO_OPTION);
                }
            } else if (shape.equals("원")) {
                s = ShapeFactory.createCircle(x1, y1, e.getX(), e.getY());
                Circle myCircle = new Circle(s, color, username, (int) stroke.getLineWidth(), fill);
                state.getShapes().add(myCircle);

                try {
                    client.request(myCircle, time);
                } catch (IOException | RuntimeException ex) {
                    JOptionPane.showConfirmDialog(null, ex.getMessage(), ex.getMessage(), JOptionPane.YES_NO_OPTION);
                }
            } else if (shape.equals("지우개")) {
                shapesPreview.clear();
            }
        }
    };


    private void rePaint(Graphics2D g2d) {
        for (CustomShape s : state.getShapes()) {
            stroke = new BasicStroke(s.getThickness());
            g2d.setStroke(stroke);
            g2d.setPaint(s.getColor());
            g2d.draw(s.getShape());
            if (s.getFill()) {
                g2d.fill(s.getShape());
            }
        }
    }

    private void draw() {
        for (CustomShape s : shapesPreview) {
            stroke = new BasicStroke(s.getThickness());
            Graphics2D g = (Graphics2D) drawPanel.getGraphics();
            g.setStroke(stroke);
            g.setPaint(s.getColor());
            g.draw(s.getShape());
            if (s.getFill()) {
                g.fill(s.getShape());
            }
        }
    }

    private static void startListeningServer() {
        Runnable listeningServer = () -> {
            String content;
            try {
                while (true) {
                    if (client.getBufferReader().ready()) {
                        content = client.getBufferReader().readLine();
                        JSONParser parser = new JSONParser();
                        JSONObject temp = (JSONObject) parser.parse(content);

                        if (temp.get(SOURCE).toString().equals(SERVER) && temp.get(COMMAND).toString().equals("ClientUpdate")) {
                            String message = temp.get("Message").toString();
                            handleClientUpdate(message);
                        }

                        if (!temp.get(SOURCE).toString().equals(SERVER) || !temp.get(COMMAND).toString().equals("Info")) {
                            if (temp.get(SOURCE).toString().equals(SERVER) && temp.get(COMMAND).toString().equals("Reply")) {
                                System.out.println("success");
                            }
                        } else {
                            String obj = temp.get("ObjectString").toString();
                            String type = temp.get("Class").toString();
                            byte[] bytes = Base64.getDecoder().decode(obj);
                            Object object;

                            switch (type) {
                                case "shape.Line":
                                case "shape.Circle":
                                case "shape.Rectangle":
                                    object = client.deserialize(bytes);
                                    state.getShapes().add((CustomShape) object);
                                    break;
                                default:
                                    break;
                            }
                            drawPanel.repaint();
                        }
                    }
                }
            } catch (IOException | ParseException | ClassNotFoundException e) {
                JOptionPane.showConfirmDialog(null, e.getMessage(), e.getMessage(), JOptionPane.YES_NO_OPTION);
            } finally {
                try {
                    client.disconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Thread clientThread = new Thread(listeningServer);
        clientThread.start();
    }

    private static void handleClientUpdate(String message) {
        JOptionPane.showMessageDialog(null, message);
    }
}