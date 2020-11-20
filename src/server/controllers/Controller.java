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
    private Map<String,Info> players;//status 0=login fail, 1= waiting ,2= playing
    
    public Controller(){
        con = getConnection("localhost", "btl", "root", "");
        players = new HashMap<>();
        open();
        while(true){
            try {
                Socket socket = myServer.accept();
                Info info = null;
                //System.out.println(socket.getInetAddress().getHostAddress());
                if(!players.containsKey(socket.getInetAddress().getHostAddress())){
                    info = new Info(socket,0);
                    players.put(socket.getInetAddress().getHostAddress(),info);
                }
                else{
                    info = players.get(socket.getInetAddress().getHostAddress());
                    info.setSocket(socket);
                    //System.out.println(info.getUser().getUserName());
                   
                }
                new Listening(info).start();
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class Listening extends Thread{
        private Socket clientSocket;
        private DataInputStream dis;
        private BufferedWriter bw;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private Info info;
        public Listening(Info info){
            try {
                
                this.info = info;
                
                //System.out.println(info.getUser().getUserName());
                //System.out.println("ket noi thanh cong");
                this.clientSocket = info.getSocket();
                ois = new ObjectInputStream(clientSocket.getInputStream());
                oos = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        @Override
        public void run() {
            try {
                Object o;
                while( clientSocket.isConnected() &&(o = ois.readObject())!=null ){
                   
                    //Object o  = ois.readObject();
                    Request request = null;
                    if(o instanceof Request){
                        request = (Request)o;
                        //System.out.println(request.getRequestName());
                        if(request.getRequestName().equals("login")){
                            proceedLogin(request);
                        }
                        else if(request.getRequestName().equals("getUserOnline")){
                            System.out.println("hello");
                            for(Map.Entry<String, Info> player : players.entrySet()){
                                if(player.getValue().getStatus()!=0) System.out.println(player.getKey() +" "+ player.getValue().getStatus() +" "+player.getValue().getUser().getUserName());
                            }
                        }
                    }
                }
                
            } catch (Exception ex) {
                try {
                    clientSocket.close();
                    //ex.printStackTrace();
                } catch (IOException ex1) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
        public void proceedLogin(Request request){
            User user = (User) request.getObject();
            String datasend = "Thất bại";
            if(checkUser(user)){
                datasend = "thanh cong";
                info.setUserName(user);
                info.setStatus(1);
                //System.out.println(info.getStatus());
                //System.out.println(info.getUser().getUserName());
            }
            try {
                oos.writeObject(datasend);
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public void send(String s){
        try {
            DataOutputStream dis = new DataOutputStream(clientSocket.getOutputStream());
            dis.writeUTF(s);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void open(){
        try {
            myServer = new ServerSocket(port);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public User receiveUser(){
        User res =null;
        try {
            clientSocket = myServer.accept();
            System.out.println(clientSocket.getInetAddress().getHostAddress());
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            res=(User)ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return res;
    }
    public boolean checkUser(User user){
        String sql = "select * from tbl_user where userName=? and passWord=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1,user.getUserName());
            ps.setString(2,user.getPassWord());
            ResultSet rs = ps.executeQuery();
            if(rs.next()) return true;
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
