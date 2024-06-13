package Client;

import Exceptions.CustomException;
import Shape.*;
import Utils.EncryptDecrypt;
import Utils.ImageResizer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

public class ClientUI {

    private static final String DEFAULT_USERNAME = "username";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "8080";

    private static Thread clientThread;
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
    private static int time = 6000000;
    private static Client client;
    private static int timeout = 20;

    private static Dimension screenSize = new Dimension(1500, 800);
    private static ArrayList<String> tempUserList;
    private static JList<Object> userList;
    private static DefaultListModel<Object> users = new DefaultListModel<>();
    private static BoardState state = new BoardState(new ArrayList<>());
    private String[] options = {"free draw", "line", "rectangle", "circle", "text", "eraser"};
    protected static Color color;
    private Color[] colors = {Color.GRAY, Color.LIGHT_GRAY, Color.darkGray, Color.black, Color.orange, Color.green,
            Color.red, Color.pink, Color.blue, Color.cyan, Color.magenta, Color.YELLOW,
            new Color(125, 55, 237), new Color(255, 99, 71), new Color(240, 230, 140),
            new Color(0, 250, 154), new Color(0, 206, 209), new Color(238, 130, 238),
            Color.WHITE};
    private String shape = "free draw";

    private int x1, y1, x2, y2;
    private static BasicStroke strock;
    private JSpinner thicknessSelector;
    private JCheckBox fillSelector;
    private boolean fill;
    private static String username = "";

    private volatile static boolean boardOwner = false;
    private volatile static boolean enterBoard = false;
    private volatile static boolean pending = false;
    private volatile static boolean connected = false;

    protected volatile static boolean error;
    protected volatile static String errorMsg;

    private static JFrame frame;
    private static JPanel mainPanel;
    private static JPanel homePanel;
    private JPanel drawPanelHeader;
    private JPanel boardInfoPanel;
    private JPanel drawControlPanel;
    private JPanel drawPanelBoard;

    private static JTextPane messageShowPanel;
    private static Graphics2D g;

    private JButton openBtn;
    private JButton saveBtn;
    private JButton saveAsBtn;
    private JButton newBtn;
    private JButton withDrawBtn;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                new ClientUI();
                frame.setVisible(true);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (connected) {

                            if (boardOwner) {
                                try {
                                    client.requestClose(time);
                                } catch (CustomException | IOException e2) {
                                    e2.printStackTrace();
                                }
                            } else {
                                try {
                                    client.requestLeave(username, time);
                                    ;
                                } catch (CustomException | IOException e3) {
                                    e3.printStackTrace();
                                }
                            }

                            try {
                                connected = false;
                                client.disconnect();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }

                        e.getWindow().dispose();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

		client = new Client();

		Runnable listeningServer = new Runnable() {
			 @Override
	            public void run() {
				 	String content;
					try {
						while(true) {
							if (!connected || !client.getBufferReader().ready()) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

							if(connected && client.getBufferReader().ready()) {
								  content = EncryptDecrypt.decrypt(client.getBufferReader().readLine());
								  System.out.println(content.toString());
							  	  JSONParser parser = new JSONParser();
							      JSONObject temp = (JSONObject) parser.parse(content);

							      if (error == true) {
							    	  System.out.println("Alert: " + errorMsg);
							    	  error = false;
							      }

							      if (!pending && enterBoard && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Info")) {
							    	  String obj = temp.get("ObjectString").toString();
							    	  String type = temp.get("Class").toString();
							    	  byte[] bytes= Base64.getDecoder().decode(obj);
							    	  Object object;

							    	  switch(type) {
										case "Shape.MyLine":
                                          case "Shape.MyEllipse":
                                          case "Shape.MyRectangle":
                                              object = client.deserialize(bytes);
											state.addShapes((MyShape) object);
											draw((MyShape) object);
											break;
                                          case "Shape.MyText":
											object = client.deserialize(bytes);
											state.addShapes((MyText) object);
											draw((MyShape) object);
											break;
										default:
											break;
							    	  }
							      }


							      else if (!pending && enterBoard && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("New")) {
							    	  state.New();
							    	  clearBoard((int) (screenSize.getWidth()), (int) (screenSize.getHeight()));
							      }


							      else if (!pending && enterBoard && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Load")) {
							    	  String obj = temp.get("ObjectString").toString();

							    	  byte[] bytes= Base64.getDecoder().decode(obj);
							    	  state = (BoardState)client.deserialize(bytes);
							    	  rePaint(g);
							      }




							      else if (boardOwner && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Authorize")) {
							    	  String name = temp.get("username").toString();

							    	  final JOptionPane userEnter = new JOptionPane(name, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
							    	  final JDialog dlg = userEnter.createDialog("Allow following user to join?");
							    	  dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

							    	  dlg.addComponentListener(new ComponentAdapter() {
							              @Override
							              public void componentShown(ComponentEvent e) {
							                  super.componentShown(e);
							                  final Timer t = new Timer(8000,new ActionListener() {
							                      @Override
							                      public void actionPerformed(ActionEvent e) {
							                          dlg.setVisible(false);
							                      }
							                  });
							                  t.start();
							              }
							          });

							    	  dlg.setVisible(true);
							    	  Object reply = userEnter.getValue();

							    	  if (reply.equals(JOptionPane.YES_OPTION)) {
							              try {
											  client.requestAccept(name, time);
										  } catch (CustomException e) {
											  e.printStackTrace();
										  }
							          } else {
							        	  try {
											  client.requestDecline(name, time);
										  } catch (CustomException e) {
											  e.printStackTrace();
										  }
							          }
							      }


							      else if (pending && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Accept")) {
							    	  String status = temp.get("Status").toString();
							    	  if (status.equals("In_Queue")) {
							    		  Runnable panel = () -> JOptionPane.showMessageDialog(null, "You are now entered", "In queue, please wait", JOptionPane.NO_OPTION);
							    		  Thread panelThread = new Thread(panel);
							    		  panelThread.start();
							    		  timeout += 30;
							    	  } else {
								    	  tempUserList = (ArrayList<String>) temp.get("UserList");
								    	  String boardStateStr = temp.get("BoardState").toString();

								    	  enterBoard = true;
								    	  pending = false;

								    	  byte[] boardStateByte= Base64.getDecoder().decode(boardStateStr);
								    	  state = (BoardState)client.deserialize(boardStateByte);
							    	  }

							      }


							      else if (pending && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Decline")) {
							          pending = false;
							          enterBoard = false;
							      }



							      else if (pending && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Create")) {
							    	  String msg = temp.get("ObjectString").toString();

							    	  if (msg.equals("Success")) {
							    		  pending = false;
								    	  enterBoard = true;
								    	  boardOwner = true;
							    	  } else {
								    	  pending = false;
								    	  enterBoard = false;
							    	  }
							      }



							      else if (!pending && enterBoard && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Remove")) {
							    	  resetBoardState();
							    	  JOptionPane.showMessageDialog(null, "You have been kicked by board owner", "Alert", JOptionPane.WARNING_MESSAGE);
							      }


							      else if (!pending && enterBoard && temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Close")) {
							    	  resetBoardState();
							    	  JOptionPane.showMessageDialog(null, "Board owner has closed the connection", "Alert", JOptionPane.WARNING_MESSAGE);
							      }


							      else if (temp.get("Source").toString().equals("Server") && temp.get("Goal").toString().equals("Reply")){
							    	  if(pending) {
							    		  pending = false;
							    		  enterBoard = false;
							    	  } else {
							    		  System.out.println("Success");
							    	  }
							      } else {
							    	  continue;
							      }
							}
						}

                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, e1.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
                } catch (ParseException e1) {
                    JOptionPane.showMessageDialog(null, e1.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
                } catch (ClassNotFoundException e1) {
                    JOptionPane.showMessageDialog(null, e1.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
                } finally {
                    resetBoardState();
                }

            }
        };

        clientThread = new Thread(listeningServer);
        clientThread.start();
    }


    private ClientUI() {
        initialize();
    }


    private void connectToServer(String host, int port) {
        try {
            client.initiate(host, port);
            connected = true;
        } catch (ConnectException e1) {
            connected = false;
            JOptionPane.showMessageDialog(null, e1.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
        } catch (UnknownHostException e1) {
            connected = false;
            JOptionPane.showMessageDialog(null, e1.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e1) {
            connected = false;
            JOptionPane.showMessageDialog(null, e1.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
        } finally {
            if (!connected) {
                pending = false;
                enterBoard = false;
            }
        }
    }


    private void initialize() {
        frame = new JFrame();
        frame.setBounds(0, 0, screenSize.width, screenSize.height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        initHomePanel();
        frame.getContentPane().add(homePanel);
        frame.setVisible(true);

    }


    private void initMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(null);

        initDrawControlPanel();

        initDrawPanelBoard();
        initDrawPanelHeader();
        mainPanel.setVisible(true);
        frame.getContentPane().add(mainPanel);
        drawPanelBoard.addMouseListener(ma);
        drawPanelBoard.addMouseMotionListener(ma);
        frame.setVisible(true);
        g = (Graphics2D) drawPanelBoard.getGraphics();
    }


    private void initHomePanel() {
        homePanel = new JPanel();
        homePanel.setLayout(new BoxLayout(homePanel, BoxLayout.Y_AXIS));
        boardInfoPanel = new JPanel();
        boardInfoPanel.setLayout(null);

        JLabel background = new JLabel();
        background.setBounds(0, 0, (int) (screenSize.width), (int) (screenSize.height));
        background.setIcon(ImageResizer.reSizeForLabel(new ImageIcon(getClass().getResource("home.png")), background));
        homePanel.add(background);

        JTextArea userNameInput = new JTextArea(DEFAULT_USERNAME);
        JTextArea ipInput = new JTextArea(DEFAULT_HOST);
        JTextArea portInput = new JTextArea(DEFAULT_PORT);

        JLabel userNameInputLabel = new JLabel("유저명 : ");
        JLabel ipInputLabel = new JLabel("IP : ");
        JLabel portInputLabel = new JLabel("포트 : ");

        Font font = new Font("TimesRoman", Font.BOLD, 20);

        userNameInput.setFont(font);
        ipInput.setFont(font);
        portInput.setFont(font);
        userNameInputLabel.setFont(font);
        ipInputLabel.setFont(font);
        portInputLabel.setFont(font);

        userNameInput.setBackground(Color.black);
        ipInput.setBackground(Color.black);
        portInput.setBackground(Color.black);

        userNameInput.setForeground(Color.white);
        ipInput.setForeground(Color.white);
        portInput.setForeground(Color.white);

        userNameInput.setBounds((int) (screenSize.width * 0.45), (int) (screenSize.height * 0.3), (int) (screenSize.height * 0.2), 25);
        ipInput.setBounds((int) (screenSize.width * 0.45), (int) (screenSize.height * 0.4), (int) (screenSize.height * 0.2), 25);
        portInput.setBounds((int) (screenSize.width * 0.45), (int) (screenSize.height * 0.5), (int) (screenSize.height * 0.2), 25);

        userNameInputLabel.setBounds((int) (screenSize.width * 0.35), (int) (screenSize.height * 0.3), (int) (screenSize.height * 0.2), 25);
        ipInputLabel.setBounds((int) (screenSize.width * 0.35), (int) (screenSize.height * 0.4), (int) (screenSize.height * 0.2), 25);
        portInputLabel.setBounds((int) (screenSize.width * 0.35), (int) (screenSize.height * 0.5), (int) (screenSize.height * 0.2), 25);

        boardInfoPanel.add(userNameInput);
        boardInfoPanel.add(ipInput);
        boardInfoPanel.add(portInput);
        boardInfoPanel.add(userNameInputLabel);
        boardInfoPanel.add(ipInputLabel);
        boardInfoPanel.add(portInputLabel);

        JButton enterBtn = new JButton();
        enterBtn.setBounds((int) (screenSize.width * 0.6), (int) (screenSize.height * 0.3), (int) (screenSize.height * 0.1), (int) (screenSize.height * 0.1));
        enterBtn.setLabel("방 입장하기");
        enterBtn.addActionListener(arg0 -> {
            username = userNameInput.getText();


            pending = true;
            enterBoard = false;


            connectToServer(ipInput.getText(), Integer.parseInt(portInput.getText()));

            try {
                client.requestEnter(username, time);
            } catch (CustomException | IOException e1) {
                e1.printStackTrace();
            }


            Date start = new Date();
            Date end = new Date();

            while (pending && (int) ((end.getTime() - start.getTime()) / 1000) < timeout) {
                end = new Date();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (enterBoard) {
                homePanel.setVisible(false);
                initMainPanel();

                withDrawBtn.setVisible(false);
                openBtn.setVisible(false);
                newBtn.setVisible(false);
                saveBtn.setVisible(true);
                saveAsBtn.setVisible(true);

                rePaint(g);

                tempUserList = null;
            } else if (pending) {

                if (timeout > 20) {
                    try {
                        client.requestTimeOut(username, time);
                    } catch (CustomException | IOException e1) {
                        e1.printStackTrace();
                    }
                    timeout = 20;
                }

                JOptionPane.showMessageDialog(null, "시간 초과");
                if (connected) {
                    try {
                        connected = false;
                        client.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (connected) {
                    JOptionPane.showMessageDialog(null, "해당 주소에 방이 없거나 같은 이름이 존재하거나 방장이 입장을 거부했습니다");
                    try {
                        connected = false;
                        client.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        JButton createBtn = new JButton();
        createBtn.setToolTipText("Create Board");
        createBtn.setBounds((int) (screenSize.width * 0.6), (int) (screenSize.height * 0.4) + 25, (int) (screenSize.height * 0.1), (int) (screenSize.height * 0.1));
        createBtn.setLabel("방 생성하기");
        createBtn.addActionListener(arg0 -> {
            System.out.println("Create board");
            username = userNameInput.getText();

            pending = true;
            enterBoard = false;

            connectToServer(ipInput.getText(), Integer.parseInt(portInput.getText()));

            try {
                client.requestCreate(username, time);
            } catch (CustomException | IOException e1) {
                e1.printStackTrace();
            }

            Date start = new Date();
            Date end = new Date();

            while (pending && (int) ((end.getTime() - start.getTime()) / 1000) < timeout) {
                end = new Date();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (enterBoard) {
                homePanel.setVisible(false);
                initMainPanel();
                withDrawBtn.setVisible(true);
                openBtn.setVisible(true);
                newBtn.setVisible(true);
                saveBtn.setVisible(true);
                saveAsBtn.setVisible(true);
                updateUserList(username, "add");
            } else if (pending) {
                JOptionPane.showMessageDialog(null, "연결 시간 초과");
                if (connected) {
                    try {
                        connected = false;
                        client.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (connected) {
                    JOptionPane.showMessageDialog(null, "방이 이미 존재합니다");
                    try {
                        connected = false;
                        client.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        boardInfoPanel.add(enterBtn);
        boardInfoPanel.add(createBtn);

        homePanel.add(boardInfoPanel);
        frame.getContentPane().add(homePanel);
        boardInfoPanel.setComponentZOrder(background, 8);
    }

    private void initDrawPanelHeader() {
        drawPanelHeader = new JPanel();
        drawPanelHeader.setBounds(0, 0, (int) (screenSize.width * 0.8), (int) (screenSize.height * 0.05));
        drawPanelHeader.setPreferredSize(new Dimension(0, 20));
        mainPanel.add(drawPanelHeader);
        drawPanelHeader.setLayout(null);


        withDrawBtn = new JButton();
        withDrawBtn.setBounds((int) (screenSize.width * 0.52), 2, (int) (screenSize.height * 0.05), (int) (screenSize.height * 0.05));
        withDrawBtn.setSize(new Dimension(80,40));
        withDrawBtn.setLabel("작업취소");
        withDrawBtn.addActionListener(arg0 -> {

            try {
                client.requestWithdraw(time);
            } catch (CustomException | IOException e) {
                e.printStackTrace();
            }

        });
        drawPanelHeader.add(withDrawBtn);

        openBtn = new JButton();

        openBtn.setBounds((int) (screenSize.width * 0.64), 2, (int) (screenSize.height * 0.05), (int) (screenSize.height * 0.05));
        openBtn.setSize(new Dimension(50,40));
        openBtn.setLabel("열기");
        openBtn.setEnabled(true);
        openBtn.addActionListener(arg0 -> {
            JFileChooser chooser = new JFileChooser();
            String path = Paths.get("").toAbsolutePath().toString();
            chooser.setCurrentDirectory(new File(path));
            chooser.setFileFilter(new FileNameExtensionFilter("ser", "SER"));
            int value = chooser.showOpenDialog(null);
            if (value == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                String filename = f.getAbsolutePath();
                try {
                    state = state.Open(filename);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                rePaint(g);


                try {
                    client.requestLoad(state, time);
                } catch (CustomException | IOException e) {
                    e.printStackTrace();
                }
            }

        });
        drawPanelHeader.add(openBtn);

        saveBtn = new JButton();

        saveBtn.setBounds((int) (screenSize.width * 0.76), 2, (int) (screenSize.height * 0.05), (int) (screenSize.height * 0.05));
        saveBtn.setSize(new Dimension(50,40));
        saveBtn.setLabel("저장");
        saveBtn.addActionListener(e -> state.Save());
        drawPanelHeader.add(saveBtn);

        saveAsBtn = new JButton();
        saveAsBtn.setBounds((int) (screenSize.width * 0.68), 2, (int) (screenSize.height * 0.05), (int) (screenSize.height * 0.05));
        saveAsBtn.setSize(new Dimension(110,40));
        saveAsBtn.setLabel("다른이름으로 저장");
        saveAsBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            String path = Paths.get("").toAbsolutePath().toString();
            chooser.setCurrentDirectory(new File(path));
            chooser.setFileFilter(new FileNameExtensionFilter("ser", "SER"));
            chooser.setApproveButtonText("Save As");
            chooser.setDialogTitle("Save As");
            int value = chooser.showOpenDialog(null);
            if (value == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                String filename = f.getAbsolutePath();
                state.SaveAs(filename);
            } else {
                System.out.println("Save command cancelled by user.");
            }

        });
        drawPanelHeader.add(saveAsBtn);

        newBtn = new JButton();
        newBtn.setBounds((int) (screenSize.width * 0.58), 2, (int) (screenSize.height * 0.05), (int) (screenSize.height * 0.05));
        newBtn.setSize(new Dimension(50,40));
        newBtn.setLabel("초기화");
        newBtn.addActionListener(arg0 -> {
            state.New();
            clearBoard((int) (screenSize.getWidth()), (int) (screenSize.getHeight()));


            try {
                client.requestNew(time);
            } catch (CustomException | IOException e) {
                e.printStackTrace();
            }
        });
        drawPanelHeader.add(newBtn);
    }


    private void initDrawControlPanel() {
        drawControlPanel = new JPanel();
        drawControlPanel.setBounds(0, (int) (screenSize.height * 0.87), (int) (screenSize.width * 1), (int) (screenSize.height * 0.13));
        drawControlPanel.setLayout(null);
        drawControlPanel.setBackground(SystemColor.controlHighlight);
        drawControlPanel.setPreferredSize(new Dimension(0, 60));
        for (int i = 0; i < colors.length; i++) {
            JButton btn = new JButton();
            btn.setBackground(colors[i]);
            btn.setBorderPainted(false);
            btn.setOpaque(true);
            btn.addActionListener(colorSelectAL);
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
            btn.setBounds(20 + i * (int) (screenSize.height * 0.04), 5, (int) (screenSize.height * 0.03), (int) (screenSize.height * 0.03));
            drawControlPanel.add(btn);
        }

        JComboBox<String> shapeSelector = new JComboBox<>(options);
        shapeSelector.setBounds(20, (int) (screenSize.height * 0.03), (int) (screenSize.height * 0.04), (int) (screenSize.height * 0.04));

        // ItemListener 추가
        shapeSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                shape = (String) e.getItem();
            }
        });

        drawControlPanel.add(shapeSelector);

        SpinnerNumberModel thicknessModel = new SpinnerNumberModel(1, 1, 15, 1);
        thicknessSelector = new JSpinner(thicknessModel);
        thicknessSelector.setBounds((int) (screenSize.width * 0.75), 5, (int) (screenSize.height * 0.05), (int) (screenSize.height * 0.025));
        drawControlPanel.add(thicknessSelector);

        fillSelector = new JCheckBox("채우기");
        fillSelector.setBackground(Color.LIGHT_GRAY);
        fillSelector.setBounds((int) (screenSize.width * 0.75), (int) (screenSize.height * 0.03) + 5, (int) (screenSize.height * 0.1), (int) (screenSize.height * 0.025));
        drawControlPanel.add(fillSelector);
// 두께 선택기에 라벨 추가
        JLabel thicknessLabel = new JLabel("선두께: ");
        thicknessLabel.setBounds((int) (screenSize.width * 0.7), 5, (int) (screenSize.height * 1), (int) (screenSize.height * 0.025));
        drawControlPanel.add(thicknessLabel);

// 옵션 선택기에 라벨 추가
        JLabel optionsLabel = new JLabel("그리기 옵션: ");
        optionsLabel.setBounds(1, (int) (screenSize.height * 0.03), (int) (screenSize.height * 0.04), (int) (screenSize.height * 0.04));
        drawControlPanel.add(optionsLabel);

        mainPanel.add(drawControlPanel);
    }



    private void initDrawPanelBoard() {
        drawPanelBoard = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                rePaint(g2d);
            }

            ;
        };
        drawPanelBoard.setBounds(0, (int) (screenSize.height * 0.05), (int) (screenSize.width), (int) (screenSize.height * 0.82));


        drawPanelBoard.setBackground(Color.WHITE);
        mainPanel.add(drawPanelBoard);
    }

    ActionListener colorSelectAL = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton bt = (JButton) e.getSource();
            color = bt.getBackground();
        }
    };

    MouseAdapter ma = new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
            x1 = e.getX();
            y1 = e.getY();
            switch (shape) {
                case "text":
                    String text = JOptionPane.showInputDialog(JOptionPane.getRootFrame(),
                            "Input your text", "");
                    int size = (int) thicknessSelector.getValue() * 10;
                    MyText mytext = new MyText(text, (float) x1, (float) y1, color, size, username);
                    state.addShapes(mytext);
                    sendDrawRequest(mytext);
                    draw(mytext);
                default:
            }
        }

        public void mouseEntered(MouseEvent e) {
            if (color == null) {
                color = Color.black;
            }
            g.setColor(color);
        }

        public void mouseDragged(MouseEvent e) {
            int thickness = (int) thicknessSelector.getValue();
            strock = new BasicStroke(thickness);
            g.setStroke(strock);
            fill = fillSelector.isSelected();
            x2 = e.getX();
            y2 = e.getY();

            switch (shape) {
                case "free draw":
                    Shape line = new Line2D.Double(x1, y1, x2, y2);
                    MyLine myline = new MyLine(line, color, username, thickness, fill);
                    state.addShapes(myline);
                    draw(myline);
                    sendDrawRequest(myline);
                    x1 = x2;
                    y1 = y2;
                    break;

                case "text":
                    break;

                case "eraser":
                    Shape eraser = new Line2D.Double(x1, y1, x2, y2);
                    MyLine myEraser = new MyLine(eraser, Color.white, username, thickness * 10, fill);
                    state.addShapes(myEraser);
                    draw(myEraser);
                    sendDrawRequest(myEraser);
                    x1 = x2;
                    y1 = y2;
                    break;

                default:
                    break;
            }

		}

		public void mouseReleased(MouseEvent e) {
			Shape s;

			switch(shape) {
				case "free draw":
					break;

				case "line":
					s =  new Line2D.Double(x1, y1, e.getX(), e.getY());
					MyLine myline = new MyLine(s, color, username, (int)strock.getLineWidth(), fill);
					state.addShapes(myline);
					draw(myline);
					sendDrawRequest(myline);
					break;

				case "rectangle":
					s = ShapeMaker.makeRectangle(x1, y1, e.getX(), e.getY());
					MyRectangle myRectangle = new MyRectangle(s, color, username, (int)strock.getLineWidth(), fill);
					state.addShapes(myRectangle);
					draw(myRectangle);
					sendDrawRequest(myRectangle);
					break;

				case "circle":
					s = ShapeMaker.makeCircle(x1, y1, e.getX(), e.getY());
					MyEllipse myCircle = new MyEllipse(s, color, username, (int)strock.getLineWidth(), fill);
					state.addShapes(myCircle);
					draw(myCircle);
					sendDrawRequest(myCircle);
					break;

				case "eraser":
					break;

				default:
					System.out.println("Unsupported Shape");
			}
	    }

	};


    private synchronized static void draw(MyShape s) {
        if (s.getClass().toString().equals(MyText.class.toString())) {
            MyText t = (MyText) s;
            g.setFont(new Font("TimesRoman", Font.PLAIN, t.getThickness()));
            g.setPaint(t.getColor());
            g.drawString(t.getText(), t.getX(), t.getY());
        } else {
            strock = new BasicStroke(s.getThickness());
            g.setStroke(strock);
            g.setPaint(s.getColor());
            g.draw(s.getShape());
            if (s.getFill()) {
                g.fill(s.getShape());
            }
        }
    }


    private synchronized static void rePaint(Graphics2D g2d) {
        clearBoard((int) (screenSize.getWidth()), (int) (screenSize.getHeight()));
        for (MyShape s : state.getShapes()) {
            if (s.getClass().toString().equals(MyText.class.toString())) {
                MyText t = (MyText) s;
                g2d.setFont(new Font("TimesRoman", Font.PLAIN, t.getThickness()));
                g2d.setPaint(t.getColor());
                g2d.drawString(t.getText(), t.getX(), t.getY());
            } else {
                strock = new BasicStroke(s.getThickness());
                g2d.setStroke(strock);
                g2d.setPaint(s.getColor());
                g2d.draw(s.getShape());
                if (s.getFill()) {
                    g2d.fill(s.getShape());
                }
            }
        }
    }

    private synchronized static void clearBoard(int width, int height) {
        g.setPaint(Color.WHITE);
        Shape s = ShapeMaker.makeRectangle(0, 0, width, height);
        g.draw(s);
        g.fill(s);
    }


    private synchronized static void resetBoardState() {
        state.New();
        clearBoard((int) (screenSize.getWidth()), (int) (screenSize.getHeight()));
        pending = false;
        enterBoard = false;
        username = null;
        users.clear();
        messageShowPanel.setText("");
        mainPanel.removeAll();
        frame.getContentPane().remove(mainPanel);
        homePanel.setVisible(true);
        frame.setVisible(true);
    }


    private synchronized static void updateUserList(String name, String option) {
        if (option.equals("add")) {
            users.addElement(name);
        } else {
            users.removeElement(name);
        }
    }


    private void sendDrawRequest(Object obj) {
        try {
            client.requestDraw(obj, time);
        } catch (CustomException | IOException e) {
            e.printStackTrace();
        }
    }

}