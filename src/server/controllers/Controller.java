/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import Models.com.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author cuongnv
 */
public class Controller {
    private int port = 8888;
    private ServerSocket myServer;
    private Socket clientSocket;
    private Connection con;
    private Map<String,Info> listPlayerSocket;//status 0=login fail, 1= waiting ,2= playing
    private Map<String, Pair<User,Integer>> players;
    private ArrayList<User> ranks;
    public Controller(){
        con = getConnection("localhost", "btl", "root", "");
        players = new HashMap<>();
        listPlayerSocket = new HashMap<>();
        open();
        //new updatePlayerOnline().start(); 
        while(true){
            try {
                Socket socket = myServer.accept();
                Info info = null;
                //System.out.println(socket.getInetAddress().getHostAddress());
                if(!listPlayerSocket.containsKey(socket.getInetAddress().getHostAddress())){
                    info = new Info(socket,0);
                    listPlayerSocket.put(socket.getInetAddress().getHostAddress(),info);
                }
                else{
                    info = listPlayerSocket.get(socket.getInetAddress().getHostAddress());
                    // Khi dang nhap sai socket se bi dong => set socket moi
                    info.setSocket(socket);
                    //System.out.println(info.getUser().getUserName());
                }
                new Listening(info).start();
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    class updatePlayerOnline extends Thread{
        public void run(){
            while(true){
                try {
                    sleep(2000);
                    updateOnline();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        public void updateOnline(){
            for(Map.Entry<String, Info> p: listPlayerSocket.entrySet()){
                if(p.getValue().getSocket().isClosed()){
                    listPlayerSocket.remove(p.getKey());
                    break;
                }
            }
            System.out.println(listPlayerSocket.size());
        }
    }
    private class Listening extends Thread{
        private Socket clientSocket;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private Info info;
        public Listening(Info info){
            try {
                this.info = info;
                this.clientSocket = info.getSocket();
                oos = new ObjectOutputStream(clientSocket.getOutputStream());
                ois = new ObjectInputStream(this.clientSocket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        @Override
        public void run() {
            try {
                Object o =null;
                while(clientSocket.isConnected() && (o = (Request)ois.readObject())!=null){
                    Request respond = (Request) o;
                    switch(respond.getRequestName()){
                        case "login":
                            handleLogin(respond);
                            break;
                        case "getListPlayerOnline":
                            System.out.println("nhan get online");
                            sendListPlayer();
                            break;               
                    }
                }
            } catch (IOException ex) {
                try {
                    clientSocket.close();
//Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex1) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        public void handleLogin(Request respond){
            Request request = null;
            User user = (User) respond.getObject();
            String datasend = "fail";
            if(checkUser(user)){
                datasend = "success";
                info.setUser(user);
                info.setStatus(1);
                request = new Request("login",(Object)user);
            }
            else request = new Request("login",(Object)"fail");
            //oos.writeObject(datasend);
            sendRequest(request);
            //sendAccount(user);
        }
        public void sendRequest(Request request){
            try {
                oos.writeObject(request);
                oos.flush();
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        public void sendListPlayer(){
            for(Map.Entry<String,Info> player : listPlayerSocket.entrySet()){
                if(!players.containsKey(player.getKey())){
                    players.put(player.getKey(), new Pair(player.getValue().getUser(),player.getValue().getStatus()));
                    System.out.println(player.getKey()+ " "+player.getValue().getStatus());
                }
            }
            Map<String, Pair<User, Integer>> listPlayer = players;
            sendRequest(new Request("sendListPlayerOnline",(Object)listPlayer));
            //oos.writeObject(listPlayer);
        }  
    }
    
    //gui thong tin nguoi dung khi dang nhap thanh cong
    public void open(){
        try {
            myServer = new ServerSocket(port);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public boolean checkUser(User user){
        
        String sql = "select * from tbl_user where userName=? and passWord=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1,user.getUserName());
            ps.setString(2,user.getPassWord());
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                user.setScore(rs.getInt("score"));
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
        
    }
    public void close(){
        try {
            myServer.close();
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public Connection getConnection(String host,String dbName, String username, String password){
        Connection res = null;
        try {
            String dbUrl = "jdbc:mysql://"+host+"/"+dbName;
            String classUrl = "com.mysql.jdbc.Driver";
            Class.forName(classUrl);
            res = DriverManager.getConnection(dbUrl,username,password);
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
    
    
}
