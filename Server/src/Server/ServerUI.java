package Server;

import PublishSubscribeSystem.PubSub;
import Utils.ImageResizer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ServerUI {

	private static final String DEFAULT_POOLSIZE = "20";
	private static final String DEFAULT_HOST = "localhost";
	private static final String DEFAULT_PORT = "8080";

	private static JFrame frame;
	private static Dimension screenSize = new Dimension(1500,800);
	private static JPanel homePanel;
	public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");  
	
	public static JTextPane logPane;
	public static JScrollPane logScrollPane ;
	
	public static void main(String[] args) {		
		EventQueue.invokeLater(() -> {
            try {
                new ServerUI();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
	}
	
	private ServerUI() {
		initialize();
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
	
	private void initHomePanel() {

		homePanel = new JPanel();
		homePanel.setLayout(new BoxLayout(homePanel, BoxLayout.Y_AXIS));
		JPanel boardInfoPanel = new JPanel();
		boardInfoPanel.setLayout(null);
		
		JLabel background = new JLabel();
		background.setBounds(0, 0, (int) (screenSize.width), (int) (screenSize.height));
		background.setIcon(ImageResizer.reSizeForLabel(new ImageIcon(getClass().getResource("home.png")), background));
		homePanel.add(background);
		
		Font font = new Font("TimesRoman", Font.BOLD, 20);

		JTextArea poolSize= new JTextArea(DEFAULT_POOLSIZE);
		JTextArea ipInput= new JTextArea(DEFAULT_HOST);
		JTextArea portInput= new JTextArea(DEFAULT_PORT);
		

		JLabel poolSizeLabel = new JLabel("인원수: ");
		JLabel ipInputLabel = new JLabel("IP: ");
		JLabel portInputLabel = new JLabel("포트: ");
		

		poolSize.setFont(font);
		ipInput.setFont(font);
		portInput.setFont(font);

		poolSizeLabel.setFont(font);
		ipInputLabel.setFont(font);
		portInputLabel.setFont(font);

		poolSize.setBackground(Color.black);
		ipInput.setBackground(Color.black);
		portInput.setBackground(Color.black);

		poolSize.setForeground(Color.white);
		ipInput.setForeground(Color.white);
		portInput.setForeground(Color.white);
		poolSize.setBounds((int) (screenSize.width*0.25), (int) (screenSize.height*0.3), (int) (screenSize.height*0.2), 25);
		ipInput.setBounds((int) (screenSize.width*0.25), (int) (screenSize.height*0.4), (int) (screenSize.height*0.2), 25);
		portInput.setBounds((int) (screenSize.width*0.25), (int) (screenSize.height*0.5), (int) (screenSize.height*0.2), 25);
		poolSizeLabel.setBounds((int) (screenSize.width*0.15), (int) (screenSize.height*0.3), (int) (screenSize.height*0.2), 25);
		ipInputLabel.setBounds((int) (screenSize.width*0.15), (int) (screenSize.height*0.4), (int) (screenSize.height*0.2), 25);
		portInputLabel.setBounds((int) (screenSize.width*0.15), (int) (screenSize.height*0.5), (int) (screenSize.height*0.2), 25);


		boardInfoPanel.add(poolSize);
		boardInfoPanel.add(ipInput);
		boardInfoPanel.add(portInput);
		boardInfoPanel.add(poolSizeLabel);
		boardInfoPanel.add(ipInputLabel);
		boardInfoPanel.add(portInputLabel);
		
		logPane = new JTextPane();
		logPane.setBounds((int) (screenSize.width*0.4), (int) (screenSize.height*0.1), (int) (screenSize.width*0.5), (int) (screenSize.height*0.7));
		logPane.setBackground(Color.BLACK);
		logScrollPane = new JScrollPane(logPane);
		logScrollPane.setBounds((int) (screenSize.width*0.4), (int) (screenSize.height*0.1), (int) (screenSize.width*0.5), (int) (screenSize.height*0.7));
		boardInfoPanel.add(logScrollPane);
		
		JButton startButton = new JButton();
		startButton.setBounds((int) (screenSize.width*0.15), (int) (screenSize.height*0.6), (int) (screenSize.height*0.1), (int) (screenSize.height*0.1));
		startButton.setLabel("서버시작");
		startButton.addActionListener(e -> {


            String ip = ipInput.getText();
            int port = Integer.parseInt(portInput.getText());

            PubSub.getInstance().setRoomSize(3);

            try {
                Server newserver = new Server(port, ip);
                Thread t = new Thread(newserver);
                t.start();
            }
            catch (IOException ex){
                ex.printStackTrace();
            }



        });
		boardInfoPanel.add(startButton);
		
		JButton closeButton = new JButton();
		closeButton.setToolTipText("Close Server");
		closeButton.setBounds((int) (screenSize.width*0.25), (int) (screenSize.height*0.6), (int) (screenSize.height*0.1), (int) (screenSize.height*0.1));
		closeButton.setLabel("서버끄기");
		closeButton.addActionListener(e -> {
            try {
                PubSub.getInstance().disconnectServer();
            }
            catch(IOException ex){

                ex.printStackTrace();

            }
        });
		boardInfoPanel.add(closeButton);
		
		homePanel.add(boardInfoPanel);
		frame.getContentPane().add(homePanel);
		boardInfoPanel.setComponentZOrder(background, 9);
	}
	

}
