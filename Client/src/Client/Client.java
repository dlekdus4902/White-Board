package Client;

import java.net.*;
import java.io.*;
import java.util.Base64;
import org.json.simple.JSONObject;
import Exceptions.CustomException;
import Utils.EncryptDecrypt;

public class Client {

	    private Socket socket;
	    private OutputStreamWriter osw;
	    private BufferedReader br;
	    private long time;

	    public Client() {
	    }

	    public void initiate(String address,int port) throws ConnectException, UnknownHostException, IOException{
	        try{
	        	
	            this.socket = new Socket(address,port);
	            this.time = System.currentTimeMillis();
	            
	        }catch(ConnectException e) {
	            throw new ConnectException("연결이 실패했거나 시간 초과되었습니다. 주소와 포트를 확인하세요.");
	        }catch (UnknownHostException e){
	            throw new UnknownHostException("알 수 없는 호스트입니다. 호스트 주소를 확인하세요.");
	        }catch(IOException e){
	            e.printStackTrace();
	        }catch(IllegalArgumentException e) {
	            throw new IllegalArgumentException("메소드의 인수가 유효하지 않습니다.");
	        }
	        
	        try {
	            OutputStream os = socket.getOutputStream();
	            this.osw = new OutputStreamWriter(os, "UTF8");
	            InputStream is = socket.getInputStream();
	            InputStreamReader isr = new InputStreamReader(is);
	            this.br = new BufferedReader(isr);

	        	
	        }catch(IOException e) {
	        	throw new IOException("InputStream 또는 OutputStream에서 문제가 발생했습니다.");
	        }
	    }

	    public synchronized byte[] serialize(Object obj) throws IOException {
	    	ByteArrayOutputStream bao = new ByteArrayOutputStream();
	    	ObjectOutputStream os = new ObjectOutputStream(bao);
	    	os.writeObject(obj);
	    	return bao.toByteArray();	
	    }
	    
	    public synchronized Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    	ByteArrayInputStream in = new ByteArrayInputStream(data);
	    	ObjectInputStream is = new ObjectInputStream(in);
	    	return is.readObject();
	    }
	    
	    public synchronized void requestDraw(Object obj, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	            	  String str = Base64.getEncoder().encodeToString(serialize(obj));
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Draw");
	                  request.put("ObjectString", str);
	                  request.put("Class", obj.getClass().getName());
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + (System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestLoad(Object obj, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	            	  String str = Base64.getEncoder().encodeToString(serialize(obj));
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Load");
	                  request.put("ObjectString", str);
	                  request.put("Class", obj.getClass().getName());
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {

	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "실패: 연결이 끊어졌거나 서버가 다운되었습니다 | 클라이언트를 종료하고 나중에 다시 시작 해주세요";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "실패: IO 예외, 입력 또는 출력 스트림 확인 | 클라이언트를 종료하고 나중에 다시 시작 해주세요";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestNew(int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "New");
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "실패: 연결이 끊어졌거나 서버가 다운되었습니다 | 클라이언트를 종료하고 나중에 다시 시작하세요";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "실패: IO 예외, 입력 또는 출력 스트림 확인 | 클라이언트를 종료하고 나중에 다시 시작";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestClose(int threshod) throws CustomException, IOException{
	          try {
	        	  
                  JSONObject request = new JSONObject();
                  request.put("Source", "Client");
                  request.put("Goal", "Close");
                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
                  osw.flush();

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestAccept(String username, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Accept");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestDecline(String username, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Decline");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestRemove(String username, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Remove");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestChat(String username, String message, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Chat");
	                  request.put("Username", username);
	                  request.put("Message", message);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestLeave(String username, int threshod) throws CustomException, IOException{
	          try {
	            	  
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Leave");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestEnter(String username, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Enter");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestCreate(String username, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Create");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestTimeOut(String username, int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Timeout");
	                  request.put("Username", username);
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized void requestWithdraw(int threshod) throws CustomException, IOException{
	          try {
	              if((System.currentTimeMillis() - this.time)<= threshod) {
	            	  
	                  this.time = System.currentTimeMillis();
	                  JSONObject request = new JSONObject();
	                  request.put("Source", "Client");
	                  request.put("Goal", "Withdraw");
	                  osw.write(EncryptDecrypt.encrypt(request.toJSONString())+"\n");
	                  osw.flush();
					
	              }else {
	            	  ClientUI.errorMsg = "Timeout" + String.valueOf(System.currentTimeMillis() - this.time) + " Request timeout, check the connection";
	            	  ClientUI.error = true;
	              }

	          }catch(SocketException e){
	        	  ClientUI.errorMsg = "Failure: Connection is lost or Server is down | Terminate the client and restart later";
	        	  ClientUI.error = true;
	        	  
	          }catch (IOException e) {
	        	  ClientUI.errorMsg = "Failure: IO Exception, check input or output streams | Terminate the client and restart later";
	        	  ClientUI.error = true;
	          }
	    }
	    
	    public synchronized BufferedReader getBufferReader() {
	    	return this.br;
	    }
	    
	    public synchronized void disconnect() throws IOException {
	    	try{
	        	this.osw.close();
	        	this.br.close();
	    	this.socket.close();
	    	}catch(IOException e) {
	    		throw new IOException("Cannot disconnect the client properly");
	    	}
	    }


	
}