package Server;

import PublishSubscribeSystem.ClientInfo;
import PublishSubscribeSystem.PubSub;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class Server implements Runnable {

    private String roomowner;
    private String hostname = "localhost";
    private int portnumber = 8002;
    private ServerSocket listeningSocket;
    private static int poolLimited = 20;


    public Server(int portnumber, String hostname) throws IOException {
        try {
            this.hostname = hostname;
            this.portnumber = portnumber;
            this.listeningSocket = new ServerSocket(portnumber);
        } catch (IOException ex) {
            throw new IOException("problem with Server Creating");
        }


    }

    public void run() {
        PubSub.getInstance().registerServer(listeningSocket);


        ExecutorService threadpool_receive = Executors.newFixedThreadPool(poolLimited);
        int clientnumber = 0;
        try {

            while (true) {


                Socket clientsocket = listeningSocket.accept();


                clientnumber++;

                Client_thread client = new Client_thread(clientsocket, clientnumber);

                Thread t = new Thread(client);

                threadpool_receive.execute(t);

            }
        } catch (SocketException ex) {

            if (!(listeningSocket.isClosed())) {
                try {
                    listeningSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                ConcurrentHashMap<String, Socket> connectedClient = PubSub.getInstance().getUsermap();

                if (!connectedClient.isEmpty()) {

                    for (Map.Entry<String, Socket> eachUser : connectedClient.entrySet()) {
                        Socket socket = eachUser.getValue();

                        if (!socket.isClosed()) {
                            OutputStream out = socket.getOutputStream();
                            OutputStreamWriter oos = new OutputStreamWriter(out, "UTF8");
                            oos.write("Manager leaving , session closed");
                            oos.flush();
                        }
                    }
                }


                LinkedBlockingQueue<ClientInfo> queue = PubSub.getInstance().getQueue();

                if (!queue.isEmpty()) {
                    Iterator<ClientInfo> listOfClients = queue.iterator();
                    while (listOfClients.hasNext()) {
                        ClientInfo current = listOfClients.next();
                        Socket socket = current.getClient();
                        if (!socket.isClosed()) {
                            OutputStream out = socket.getOutputStream();
                            OutputStreamWriter oos = new OutputStreamWriter(out, "UTF8");
                            oos.write("Manager leaving , session closed");
                            oos.flush();

                        }


                    }
                }

                threadpool_receive.shutdown();
                listeningSocket.close();


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


}
