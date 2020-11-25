/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import Models.com.Info;
import Models.com.Pair;
import Models.com.Request;
import Models.com.User;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cuongnv
 */
public class ThreadServerListen extends Thread{
    private Socket clientSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    public Info info;
    private Connection con;
    private Map<String, Info> listPlayerSocket;
    private Map<String , Pair<User, Integer>> players;
    private User user;
    public ThreadServerListen(Info info, Connection con, Map<String, Info> l, Map<String, Pair<User, Integer>> players){
        try {
            this.players = players;
            this.listPlayerSocket = l;
            this.con = con;
            this.info = info;
            this.clientSocket = info.getSocket();
            //oos = new ObjectOutputStream(clientSocket.getOutputStream());
            this.oos = info.oos;
            ois = new ObjectInputStream(this.clientSocket.getInputStream());
            
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void run(){
        try {
            Object o =null;
            while(clientSocket.isConnected() && (o = (Request)ois.readObject())!=null){
                Request respond = (Request) o;
                switch(respond.getRequestName()){
                    case "login":
                        handleLogin(respond);
                        break;
                    case "getListPlayer":
                        sendListPlayer();
                        break;
                    case "match":
                        
                        forwardInvite(respond);
                        break;
                    case "refuse":
                        sendRefuse(respond);
                        break;
                    
                    
              }
            }
        } catch (IOException ex) {
            try {
                clientSocket.close();
            } catch (IOException ex1) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //trả lại thông báo từ chối cho người mời
    public void sendRefuse(Request res){
        try {
            String ip = (String)res.getObject();
            
            Request req = new Request("refuse",(Object)this.info.getUser().getUserName());
            listPlayerSocket.get(ip).oos.writeObject(req);
        } catch (IOException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //Chuyển l ờithách đấu của user1 gửi đến User2
    public void forwardInvite(Request res){
        try {
            String ip = (String) res.getObject();
            System.out.println("nhan loi thach dau");
            Request req = new Request("challange",(Object)ip);
            //System.out.println("send to "+s.getValue().getSocket().getInetAddress().getHostAddress());
            listPlayerSocket.get(ip).oos.writeObject(req);
            //s.getValue().oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
                   
            
        
    }
    
    public void handleLogin(Request respond){
        User user = (User) respond.getObject();
        String datasend = "fail";
        Request req = null;
        if(checkUser(user)){
            //System.out.println(user.getScore());
            datasend = "success";

            user.setIp(info.getSocket().getInetAddress().getHostAddress());
            this.user = user;
            info.setUser(user);
            info.setStatus(1);
            req = new Request("login", (Object)this.user);
        }
        else req = new Request("login",(Object)"fail");
        sendRequest(req);
    }
    public void sendAccount(User user){
        try {
            
            oos.writeObject(user);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void sendRequest(Request request){
        try {
            oos.writeObject(request);
            //oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void sendListPlayer(){
        players.clear();
        players= new HashMap<>();
        for(Map.Entry<String,Info> player : listPlayerSocket.entrySet()){
            if(player.getValue().getSocket().isConnected()){
                players.put(player.getKey(),new Pair(player.getValue().getUser(),player.getValue().getStatus()));
            }
        }
        Map<String, Pair<User, Integer>> listPlayer = players;

        sendRequest(new Request("sendListPlayer",(Object)listPlayer));
        //oos.writeObject(listPlayer);
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
}
