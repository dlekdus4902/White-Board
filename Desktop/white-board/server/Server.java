package server;

import shape.CustomShape;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private static ArrayList<Socket> connectedClient = new ArrayList<>();
    private static ArrayList<CustomShape> customShapes = new ArrayList<>();

    private static String HOSTNAME = "localhost";
    private static int PORT_NUMBER = 1000;

    public static void main(String[] args) throws Exception {

        try {
            if (args.length == 1) {
                PORT_NUMBER = Integer.parseInt(args[0]);
            } else if (args.length == 0) {
                System.out.println("기본 포트 지정  " + PORT_NUMBER);

            } else {
                System.out.println("이름과 호스트가 사용중입니다");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        ServerSocket listeningSocket = new ServerSocket(PORT_NUMBER);
        ExecutorService threadPoolListen = Executors.newCachedThreadPool();

        try {
            while (true) {
                Socket clientsocket = listeningSocket.accept();
                connectedClient.add(clientsocket);
                broadcastClientUpdate("새로운 접속자: " + clientsocket);

                for (Socket client : connectedClient) {
                    if (!client.isClosed()) {
                        System.out.println(client);
                    } else {
                        connectedClient.remove(client);
                        client.close();
                    }
                }


                ClientThread client = new ClientThread(clientsocket);
                threadPoolListen.execute(new Thread(client));
                ServerOption.getState().clientConnected(client);
            }
        } catch (SocketException ex) {
            if (!(listeningSocket.isClosed())) {
                try {
                    listeningSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    broadcastClientUpdate("클라이언트 종료");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                for (Socket connectedClient1 : connectedClient) {
                    if (!connectedClient1.isClosed()) {
                        OutputStream out = connectedClient1.getOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(out);
                        oos.writeUTF("세션 종료");
                    } else {
                        connectedClient.remove(connectedClient1);
                    }
                }
                threadPoolListen.shutdown();
                listeningSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    static synchronized ArrayList<Socket> getConnectedClient() {
        return (ArrayList<Socket>) connectedClient.clone();
    }


    static synchronized void addShape(CustomShape customShape) {
        customShapes.add(customShape);
    }


    static synchronized void broadcast(CustomShape item) throws IOException {

        String shapestr = Base64.getEncoder().encodeToString(serialize(item));

        JSONObject reply = new JSONObject();

        reply.put("Source", "Server");
        reply.put("Command", "Info");
        reply.put("ObjectString", shapestr);
        reply.put("Class", item.getClass().getName());


        for (Socket connectedClient : connectedClient) {
            OutputStream out = connectedClient.getOutputStream();
            OutputStreamWriter oos = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            oos.write(reply.toJSONString() + "\n");
            oos.flush();
        }

        System.out.println("done");

    }


    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bao);
        os.writeObject(obj);
        return bao.toByteArray();
    }


    public static synchronized void broadcastClientUpdate(String message) throws IOException {
        JSONObject reply = new JSONObject();
        reply.put("Source", "Server");
        reply.put("Command", "ClientUpdate");
        reply.put("Message", message);

        for (Socket connectedClient : connectedClient) {
            OutputStream out = connectedClient.getOutputStream();
            OutputStreamWriter oos = new OutputStreamWriter(out, "UTF8");
            oos.write(reply.toJSONString() + "\n");
            oos.flush();
        }
    }


    public static void removeClient(Socket clientsocket) {
        connectedClient.remove(clientsocket);
        try {
            clientsocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
