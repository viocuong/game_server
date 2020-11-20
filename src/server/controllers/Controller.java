/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import Models.com.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
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
    public Controller(){
        con = getConnection("localhost", "btl", "root", "");
        open();
        while(true){
            try {
                new Listening(myServer.accept()).start();
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class Listening extends Thread{
        private Socket clientSocket;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        public Listening(Socket s){
            try {
                System.out.println("ket noi thanh cong");
                this.clientSocket = s;
                ois = new ObjectInputStream(s.getInputStream());
                oos = new ObjectOutputStream(s.getOutputStream());
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        @Override
        public void run() {
            try {
               
                Request request =(Request) ois.readObject();
                
                if(request.getRequestName().equals("login")){
                    User user = (User) request.getObject();
                    String datasend = "Thất bại";
                    if(checkUser(user)) datasend = "thanh cong";
                    oos.writeObject(datasend);
                }
                if(request.getRequestName().equals("getUserOnline")){
                    System.out.println("10 thang dang online");
                }
                
            } catch (Exception ex) {
               ex.printStackTrace();
            }finally{
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("client "+clientSocket.getInetAddress().getHostAddress()+" da dong");
            }
        }
        
    }
    public void listening(){
        User user = receiveUser();
        String ans = "that bai";
        if(checkUser(user)) ans = "thanh cong";
        send(ans);
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
