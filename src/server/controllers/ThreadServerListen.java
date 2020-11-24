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
        private Info info;
        private Connection con;
        private Map<String, Info> listPlayerSocket;
        private Map<String , Pair<User, Integer>> players;
    public ThreadServerListen(Info info, Connection con, Map<String, Info> l, Map<String, Pair<User, Integer>> players){
        try {
            this.players = players;
            this.listPlayerSocket = l;
            this.con = con;
            this.info = info;
            this.clientSocket = info.getSocket();
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
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
        User user = (User) respond.getObject();
        String datasend = "fail";
        Request req = null;
        if(checkUser(user)){
            System.out.println(user.getScore());
            datasend = "success";
            info.setUser(user);
            info.setStatus(1);
            //System.out.println(info.getStatus());
            //System.out.println(info.getUser().getUserName());
            req = new Request("login", (Object)user);
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
