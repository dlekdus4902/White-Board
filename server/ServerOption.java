package server;


import java.util.ArrayList;


public class ServerOption {

    private static ServerOption state;
    private ArrayList<ClientThread> connectedClients;

    private ServerOption(){
        connectedClients = new ArrayList<>();
    }

    public static synchronized ServerOption getState(){
        if(state == null){
            state = new ServerOption();
        }
        return state;
    }

    public synchronized void clientConnected(ClientThread client){
        connectedClients.add(client);
    }

    public synchronized void remove(ClientThread client){
        connectedClients.remove(client);
    }


}