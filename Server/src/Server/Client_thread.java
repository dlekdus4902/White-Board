package Server;


import PublishSubscribeSystem.ClientInfo;
import PublishSubscribeSystem.PubSub;
import Shape.BoardState;
import Shape.MyShape;
import Utils.EncryptDecrypt;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;


public class Client_thread implements Runnable {



    private Socket clientsocket;
    private int clientnumber;
    private String username;
    private boolean isManager = false;


    Client_thread (Socket client,int clientnumber) throws IOException{
        this.clientsocket = client;
        this.clientnumber = clientnumber;

    }

    @Override
    public void run() {
        try(Socket socket = clientsocket){

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader ois = new BufferedReader(isr);
            OutputStreamWriter oos = new OutputStreamWriter(socket.getOutputStream());
            JSONParser parser = new JSONParser();

            String result;


            while(!socket.isClosed()){




                while((result = ois.readLine()) != null){

                        result = EncryptDecrypt.decrypt(result);

                        JSONObject command = (JSONObject) parser.parse(result);

                        if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Draw"))
                        {
                            String obj = command.get("ObjectString").toString();
                            String type = command.get("Class").toString();
                            byte[] bytes= Base64.getDecoder().decode(obj);
                            Object object;

                            JSONObject reply = new JSONObject();
                            reply.put("Source","Server");
                            reply.put("Goal","Reply");
                            reply.put("ObjectString","Successfully received!");

                            String acknowledgement = EncryptDecrypt.encrypt(reply.toJSONString());

                            oos.write(acknowledgement+"\n");
                            oos.flush();

                            switch(type) {
                                case "Shape.MyLine":
                                case "Shape.MyEllipse":
                                case "Shape.MyRectangle":
                                case "Shape.MyText":
                                    object = deserialize(bytes);
                                    PubSub.getInstance().getBoardState().addShapes((MyShape) object);
                                    PubSub.getInstance().broadcastShapes((MyShape) object,username);
                                    break;
                                default:
                                    break;
                            }
                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Create")) {
                            String username = command.get("Username").toString();
                            this.username = username;
                            JSONObject reply = new JSONObject();
                            reply.put("Source","Server");
                            reply.put("Goal","Create");

                            boolean res = false;

                            synchronized(PubSub.getInstance()) {
                            if (!PubSub.getInstance().hasManger()) {
                            	res = true;
                            	PubSub.getInstance().setManager(command.get("Username").toString());
                            }
                            }

                            if(res) {
                            PubSub.getInstance().registerClient(username, socket);
                            reply.put("ObjectString","Success");
                            this.isManager = true;
                            String acknowledgement = EncryptDecrypt.encrypt(reply.toJSONString());
                            oos.write(acknowledgement+"\n");
                            oos.flush();
                            }
                            else {
                            reply.put("ObjectString","Failure");
                            String acknowledgement = EncryptDecrypt.encrypt(reply.toJSONString());
                            oos.write(acknowledgement+"\n");
                            oos.flush();
                            oos.close();
                            }



                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Chat")) {
                            String username = command.get("Username").toString();
                            String message = command.get("Message").toString();
                            JSONObject reply = new JSONObject();


                                reply.put("Source","Server");
                                reply.put("Goal","Chat");
                                reply.put("message", message);
                                reply.put("username", username);
                            PubSub.getInstance().broadcastJSON(reply,this.username);




                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Leave")) {
                            String username = command.get("Username").toString();
                            this.username = username;
                            JSONObject reply = new JSONObject();
                            PubSub.getInstance().deregisterClient(username);
                            reply.put("Source","Server");
                            reply.put("Goal","Leave");
                            reply.put("username", username);

                            PubSub.getInstance().broadcastJSON(reply,this.username);

                            LinkedBlockingQueue<ClientInfo> q = PubSub.getInstance().getQueue();

                            synchronized(q) {
                                if(q.size()!=0 && PubSub.getInstance().getUsermap().size() < PubSub.getInstance().getRoomSize()) {

                                    ClientInfo clientinfo;

                                    clientinfo = q.poll();

                                    String name = clientinfo.getName();
                                    Socket s = clientinfo.getClient();
                                    PubSub.getInstance().getUsermap().put(name, s);

                                    JSONObject updateUserList = new JSONObject();
                                    updateUserList.put("Source","Server");
                                    updateUserList.put("Goal","Enter");
                                    updateUserList.put("username",name);
                                    PubSub.getInstance().broadcastJSON(updateUserList);

                                    JSONObject endwaiting = new JSONObject();
                                    endwaiting.put("Source","Server");
                                    endwaiting.put("Goal","Accept");
                                    endwaiting.put("Status","In_Room");
                                    BoardState obj1 = PubSub.getInstance().getBoardState();
                                    String boarddstr = Base64.getEncoder().encodeToString(serialize(obj1));
                                    ArrayList<String> obj2 = PubSub.getInstance().getUserList();

                                    endwaiting.put("BoardState", boarddstr);
                                    endwaiting.put("UserList",obj2);

                                    String endwaitingstr = EncryptDecrypt.encrypt(endwaiting.toJSONString());

                                    OutputStream aout = s.getOutputStream();
                                    OutputStreamWriter aoos =new OutputStreamWriter(aout, "UTF8");
                                    aoos.write(endwaitingstr+"\n");
                                    aoos.flush();




                                }
                            }




                        }

                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Close")) {

                            JSONObject reply = new JSONObject();

                            	reply.put("Source","Server");
                                reply.put("Goal","Close");
                                reply.put("ObjectString", "Manager " + username + " is closing the board");

                                PubSub.getInstance().resetManager();

                                PubSub.getInstance().broadcastJSON(reply,this.username);


                                LinkedBlockingQueue<ClientInfo> queue = PubSub.getInstance().getQueue();


                                Iterator<ClientInfo> listOfClients = queue.iterator();
                                while (listOfClients.hasNext()) {
                                    ClientInfo current = listOfClients.next();
                                    Socket wait = current.getClient();
                                    if(!wait.isClosed()){
                                        OutputStream out = wait.getOutputStream();
                                        OutputStreamWriter woos =new OutputStreamWriter(out, "UTF8");
                                        woos.write(reply.toJSONString()+"\n");
                                        woos.flush();

                                    }
                                }

                                PubSub.getInstance().disconnectServer();


                        }


                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Remove")) {
                            String removename = command.get("Username").toString();
                            JSONObject reply = new JSONObject();

                            	boolean addsocket = false;
                            	if(PubSub.getInstance().getUsermap().containsKey(removename)) {
                            		addsocket = true;
                            	}
                            	reply.put("Source","Server");
                                reply.put("Goal","Remove");
                                reply.put("ObjectString", "User " + removename + " has been kicked out");

                                PubSub.getInstance().sendtoSpecificUser(reply,removename);

                                PubSub.getInstance().deregisterClient(removename);



                                JSONObject broadcasttheremove = new JSONObject();

                                broadcasttheremove.put("Source","Server");
                                broadcasttheremove.put("Goal","Leave");
                                broadcasttheremove.put("message","User " + removename + " has been kicked out");
                                broadcasttheremove.put("username",removename);

                                PubSub.getInstance().broadcastJSON(broadcasttheremove,this.username);

                                LinkedBlockingQueue<ClientInfo> q = PubSub.getInstance().getQueue();

                                synchronized(q) {
                                	if(q.size()!=0 && addsocket && PubSub.getInstance().getUsermap().size() < PubSub.getInstance().getRoomSize()) {

                                		ClientInfo clientinfo;

                                		clientinfo = q.poll();

                                		String name = clientinfo.getName();
                                		Socket s = clientinfo.getClient();
                                		PubSub.getInstance().getUsermap().put(name, s);

                                		JSONObject updateUserList = new JSONObject();
                                		updateUserList.put("Source","Server");
                                		updateUserList.put("Goal","Enter");
                                		updateUserList.put("username",name);
                                		PubSub.getInstance().broadcastJSON(updateUserList);

                                        JSONObject endwaiting = new JSONObject();
                                        endwaiting.put("Source","Server");
                                        endwaiting.put("Goal","Accept");
                                        endwaiting.put("Status","In_Room");
                                        BoardState obj1 = PubSub.getInstance().getBoardState();
                                        String boarddstr = Base64.getEncoder().encodeToString(serialize(obj1));
                                        ArrayList<String> obj2 = PubSub.getInstance().getUserList();

                                        endwaiting.put("BoardState", boarddstr);
                                        endwaiting.put("UserList",obj2);

                                        String endwaitingstr = EncryptDecrypt.encrypt(endwaiting.toJSONString());

                                        OutputStream aout = s.getOutputStream();
                                        OutputStreamWriter aoos =new OutputStreamWriter(aout, "UTF8");
                                        aoos.write(endwaitingstr+"\n");
                                        aoos.flush();

                                }
                              }



                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("New")) {


                            JSONObject reply = new JSONObject();

                            	reply.put("Source","Server");
                                reply.put("Goal","New");
                                reply.put("ObjectString", "Manager " + username + " has cleaned the board");

                                PubSub.getInstance().resetBoardState();

                                PubSub.getInstance().broadcastJSON(reply,this.username);
                                
                                JSONObject msg = new JSONObject();
                                msg.put("Source","Server");
                                msg.put("Goal","Chat");
                                msg.put("message", "The Board Owner clears the board!");
                                msg.put("username", "Board_Owner");
                                PubSub.getInstance().broadcastJSON(msg);



                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Enter")) {
                            String username = command.get("Username").toString();
                            this.username = username;

                            boolean hasBoard = false;
                            boolean norepeatName =true;
                            
                            
                            synchronized(PubSub.getInstance()) {
                                if (PubSub.getInstance().hasManger()) {
                                    hasBoard = true;
                                }
                            }
                            
                            synchronized(PubSub.getInstance()) {
                                if (PubSub.getInstance().hasrepeatedName(username)) {
                                    norepeatName = false;
                                }
                            }



                            PubSub.getInstance().getApplicants().put(username, socket);

                            if(hasBoard && norepeatName) {

                               
                                JSONObject reply = new JSONObject();
                                reply.put("Source", "Server");
                                reply.put("Goal", "Authorize");
                                reply.put("ObjectString", "Need to authorize the applicant");
                                reply.put("username", username);

                                PubSub.getInstance().sendToManger(reply);


                            }
                            else if(!norepeatName){
                                JSONObject reply = new JSONObject();

                                reply.put("Source","Server");
                                reply.put("Goal","Reply");
                                reply.put("ObjectString","repeated Name, double check");

                                String message = EncryptDecrypt.encrypt(reply.toJSONString());

                                oos.write(message+"\n");
                                oos.flush();
                                PubSub.getInstance().getApplicants().remove(username);
                                oos.close();


                            }
                            else{
                                JSONObject reply = new JSONObject();

                                reply.put("Source","Server");
                                reply.put("Goal","Reply");
                                reply.put("ObjectString","No board yet, try to create one");

                                String message = EncryptDecrypt.encrypt(reply.toJSONString());


                                oos.write(message+"\n");
                                oos.flush();
                                PubSub.getInstance().getApplicants().remove(username);
                                oos.close();



                            }

                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Accept")) {
                        	String applicant = command.get("Username").toString();

                            JSONObject reply = new JSONObject();

                            	Socket client  = PubSub.getInstance().getApplicants().get(applicant);

                            	if(!client.isClosed()) {
                            	boolean res = PubSub.getInstance().registerClient(applicant, client);

                            	reply.put("Source","Server");
                                reply.put("Goal","Accept");
                                
                                PubSub.getInstance().getApplicants().remove(applicant);
                                
                                if (res)
                                {
                                    BoardState obj1 = PubSub.getInstance().getBoardState();
                                    String boarddstr = Base64.getEncoder().encodeToString(serialize(obj1));
                                    ArrayList<String> obj2 = PubSub.getInstance().getUserList();

                                reply.put("BoardState", boarddstr);
                                reply.put("UserList",obj2);
                                reply.put("Status","In_Room");
                              

                                JSONObject updateUserList = new JSONObject();
                                updateUserList.put("Source","Server");
                                updateUserList.put("Goal","Enter");
                                updateUserList.put("username",applicant);
                                PubSub.getInstance().broadcastJSON(updateUserList);

                                }
                                
                                else {
                                reply.put("message","the room is full, you are in the waiting list");
                                reply.put("Status","In_Queue");

                                }

                                String message = EncryptDecrypt.encrypt(reply.toJSONString());

                                OutputStream aout = client.getOutputStream();
                                OutputStreamWriter aoos =new OutputStreamWriter(aout, "UTF8");
                                aoos.write(message+"\n");
                                aoos.flush();

                            	}

                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Decline")) {
                        	String applicant = command.get("Username").toString();
                            JSONObject reply = new JSONObject();

                            	Socket client  = PubSub.getInstance().getApplicants().get(applicant);
                            	PubSub.getInstance().getApplicants().remove(applicant);
                            	
                            	if(!client.isClosed()) {                     
                            	reply.put("Source","Server");
                                reply.put("Goal","Decline");

                                OutputStream aout = client.getOutputStream();
                                OutputStreamWriter aoos =new OutputStreamWriter(aout, "UTF8");
                                String message = EncryptDecrypt.encrypt(reply.toJSONString());
                                aoos.write(message+"\n");
                                aoos.flush();
                                aoos.close();

                            	}
                        }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Load")) {

                            JSONObject reply = new JSONObject();
                            String boardstate = command.get("ObjectString").toString();

                            	reply.put("Source","Server");
                                reply.put("Goal","Load");
                                reply.put("ObjectString", boardstate);
                                PubSub.getInstance().broadcastJSON(reply,this.username);

                                byte[] bytes = Base64.getDecoder().decode(boardstate);
                                BoardState bs = (BoardState) PubSub.getInstance().deserialize(bytes);
                                PubSub.getInstance().updateBoardState(bs);
                                JSONObject msg = new JSONObject();
                                msg.put("Source","Server");
                                msg.put("Goal","Chat");
                                msg.put("message", "The Board Owner load new shapes!");
                                msg.put("username", "Board_Owner");
                                PubSub.getInstance().broadcastJSON(msg);
                                
                           }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Timeout")) {

                        	String user = command.get("Username").toString();
                        	PubSub.getInstance().deregisterClient(user);
                        	if(PubSub.getInstance().getApplicants().contains(user)) {
                        		PubSub.getInstance().getApplicants().remove(user);
                        	}
                                
                           }
                        else if(command.get("Source").toString().equals("Client") && command.get("Goal").toString().equals("Withdraw")) {
                        	if(PubSub.getInstance().getBoardState().getShapes().size()!=0) {
                        	PubSub.getInstance().getBoardState().getShapes().remove(PubSub.getInstance().getBoardState().getShapes().size()-1);
                        	JSONObject reply = new JSONObject();
                        	reply.put("Source","Server");
                            reply.put("Goal","Load");
                            BoardState bs = PubSub.getInstance().getBoardState();
                            byte[] bytes = PubSub.getInstance().serialize(bs);
                            String boardstate = Base64.getEncoder().encodeToString(bytes);
                            reply.put("ObjectString", boardstate);
                            PubSub.getInstance().broadcastJSON(reply);

                        	}  
                           }
                        
                        }
            }

        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();

        }
        catch (IOException e)
        {

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        finally {


        try {

            if(this.isManager){

            JSONObject reply = new JSONObject();

            reply.put("Source","Server");
            reply.put("Goal","Close");
            reply.put("ObjectString", "There is something wrong in Manager " + username + " thread");

            PubSub.getInstance().resetManager();

            PubSub.getInstance().broadcastJSON(reply,this.username);


            LinkedBlockingQueue<ClientInfo> queue = PubSub.getInstance().getQueue();


            Iterator<ClientInfo> listOfClients = queue.iterator();
            while (listOfClients.hasNext()) {
                ClientInfo current = listOfClients.next();
                Socket wait = current.getClient();
                if(!wait.isClosed()){
                    OutputStream out = wait.getOutputStream();
                    OutputStreamWriter woos =new OutputStreamWriter(out, "UTF8");
                    woos.write(reply.toJSONString()+"\n");
                    woos.flush();

                }
            }

            PubSub.getInstance().disconnectServer();



        }
            if(!clientsocket.isClosed())
                clientsocket.close();
        }
        catch(IOException ex){

            System.out.println("incorrectly end thread");
        }

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
}
