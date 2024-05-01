package server;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import shape.CustomShape;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

import static server.Server.broadcastClientUpdate;


public class ClientThread implements Runnable {


    private Socket clientsocket;


    ClientThread(Socket client) throws IOException {
        this.clientsocket = client;
    }

    @Override
    public void run() {
        try (Socket socket = clientsocket) {

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader ois = new BufferedReader(isr);
            OutputStreamWriter oos = new OutputStreamWriter(socket.getOutputStream());
            JSONParser parser = new JSONParser();

            String result;


            while (!socket.isClosed()) {

                if ((result = ois.readLine()) != null) {

                    if (result.equals("exit")) {
                        oos.close();
                        socket.close();
                        ServerOption.getState().remove(this);
                        Server.getConnectedClient().remove(socket);
                        return;
                    }

                    JSONObject command = (JSONObject) parser.parse(result);

                    if (command.get("Source").toString().equals("Client") && command.get("Command").toString().equals("Draw")) {
                        String obj = command.get("ObjectString").toString();
                        String type = command.get("Class").toString();
                        byte[] bytes = Base64.getDecoder().decode(obj);
                        Object object;

                        JSONObject reply = new JSONObject();
                        reply.put("Source", "Server");
                        reply.put("Command", "Reply");
                        reply.put("ObjectString", "연결 성공");

                        oos.write(reply.toJSONString() + "\n");
                        oos.flush();

                        if (type.equals("shape.Line") || type.equals("shape.Circle") || type.equals("shape.Rectangle")) {
                            object = deserialize(bytes);
                            Server.addShape((CustomShape) object);
                            Server.broadcast((CustomShape) object);
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientsocket.close();
                Server.removeClient(this.clientsocket);
                broadcastClientUpdate("클라이언트 종료 : " + this.clientsocket);
                ServerOption.getState().remove(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("exit");
        }
    }

    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}