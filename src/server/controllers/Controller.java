/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import server.controllers.*;
import Models.com.*;
import com.mysql.fabric.Server;
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
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

/**
 *
 * @author cuongnv
 */
public class Controller {

    private int port = 8888;
    private int portRmi = 8889;
    private ServerSocket myServer;
    private Socket clientSocket;

    private Connection con;
    private Map<String, Info> listPlayerSocket;//status 0=login fail, 1= waiting ,2= playing
    private Map<String, Pair<User, Integer>> players;
    private ArrayList<User> ranks;
    private ArrayList<CouplePlayer> listPlaySession = new ArrayList<>(); // Lưu trạng thái nộp bài của mỗi cặp đấu 

    public Controller() throws RemoteException {
        con = getConnection("localhost", "btl", "root", "");
        players = new HashMap<>();
        listPlayerSocket = new HashMap<>();
        RMIController rmi = new RMIController();
        
        open();
        updatePlayerOnline threadUpdate = new updatePlayerOnline(this.listPlayerSocket);
        threadUpdate.setDaemon(true);
        threadUpdate.start();

        while (true) {
            try {
                Socket socket = myServer.accept();
                Info info = null;
                //System.out.println(socket.getInetAddress().getHostAddress());
                if (!listPlayerSocket.containsKey(socket.getInetAddress().getHostAddress())) {
                    info = new Info(socket, 0);
                    info.oos = new ObjectOutputStream(socket.getOutputStream());
                    listPlayerSocket.put(socket.getInetAddress().getHostAddress(), info);
                } else {
                    info = listPlayerSocket.get(socket.getInetAddress().getHostAddress());
                    // Khi dang nhap sai socket se bi dong => set socket moi
                    info.setSocket(socket);
                    info.oos = new ObjectOutputStream(socket.getOutputStream());
                    //System.out.println(info.getUser().getUserName());
                }
                new ThreadServerListen(info, con, listPlayerSocket, players, listPlaySession).start();
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public class RMIController extends UnicastRemoteObject implements RMIinterface{
        private Registry registry;
        private String rmiService ="rmi";
        public RMIController() throws RemoteException{
            try {
                registry = LocateRegistry.createRegistry(portRmi);
                registry.rebind(rmiService, this);
            } catch (RemoteException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        @Override
        public ArrayList<User> getUsers() {
            ArrayList<User> ans = new ArrayList<User>();
            try {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                String sql = "select * from tbl_user where 1";
                
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    ans.add(new User(rs.getString("userName"),rs.getFloat("score"),rs.getFloat("averageTimeWin"),rs.getFloat("averageCompetitor")));
                }
            } catch (SQLException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ans;
        }
    }
    //gui thong tin nguoi dung khi dang nhap thanh cong
    public void open() {
        try {
            myServer = new ServerSocket(port);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void close() {
        try {
            myServer.close();
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public Connection getConnection(String host, String dbName, String username, String password) {
        Connection res = null;
        try {
            String dbUrl = "jdbc:mysql://" + host + "/" + dbName;
            String classUrl = "com.mysql.jdbc.Driver";
            Class.forName(classUrl);
            res = DriverManager.getConnection(dbUrl, username, password);

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

}
