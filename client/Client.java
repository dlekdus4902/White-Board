package client;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.Base64;

import org.json.simple.JSONObject;


public class Client {

    private Socket socket;
    private OutputStreamWriter osw;
    private BufferedReader br;
    private long time;

    public OutputStreamWriter getOsw() {
        return osw;
    }

    public void initiate(String address, int port) throws IOException {

        System.out.println(address);
        System.out.println(port);
        this.socket = new Socket(address, port);
        this.time = System.currentTimeMillis();


        try {
            OutputStream os = socket.getOutputStream();
            this.osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            this.br = new BufferedReader(isr);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bao);
        os.writeObject(obj);
        return bao.toByteArray();
    }

    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public JSONObject request(Object obj, int threshold) throws IOException {

        JSONObject response = new JSONObject();
        try {

            if ((System.currentTimeMillis() - this.time) <= threshold) {

                String str = Base64.getEncoder().encodeToString(serialize(obj));

                this.time = System.currentTimeMillis();
                JSONObject request = new JSONObject();
                request.put("Source", "Client");
                request.put("Command", "Draw");
                request.put("ObjectString", str);
                request.put("Class", obj.getClass().getName());
                osw.write(request + "\n");
                osw.flush();
            } else {
                response.put("Timeout", String.valueOf(System.currentTimeMillis() - this.time));
                throw new RuntimeException("Timout 발생");
            }

        } catch (SocketException e) {
            response.put("Status", "실패: 연결이 끊어졌거나 서버가 다운되었습니다.");
            response.put("Action", "클라이언트를 종료하고 나중에 다시 시작해주세요");
        } catch (IOException e) {
            response.put("Status", "실패: IO 예외, 입력 또는 출력 스트림을 확인하세요.");
            response.put("Action", "클라이언트를 종료하고 나중에 다시 시작해주세요");

        } finally {
            return response;
        }
    }


    public BufferedReader getBufferReader() {
        return this.br;
    }

    public void disconnect() throws IOException {
        this.osw.close();
        this.br.close();
        this.socket.close();
    }


}