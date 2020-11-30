/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import Models.com.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cuongnv
 */
public class ThreadServerListen extends Thread {

    private Socket clientSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    public Info info;
    private ArrayList<Question> questions;
    private ArrayList<Pair<String, Thread>> listThread;
    private Connection con;
    private Map<String, Info> listPlayerSocket;
    private Map<String, Pair<User, Integer>> players;
    private User user;
    private ArrayList<CouplePlayer> listPlaySession;

    public ThreadServerListen(Info info, Connection con, Map<String, Info> l, Map<String, Pair<User, Integer>> players, ArrayList<CouplePlayer> playsessions) {
        try {
            this.listPlaySession = playsessions;
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

    public synchronized void run() {
        try {
            Object o = null;
            while (clientSocket.isConnected() && (o = (Request) ois.readObject()) != null) {
                Request respond = (Request) o;
                switch (respond.getRequestName()) {
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
                    case "acceptChallange":
                        createSession((String) respond.getObject());
                        break;
                    case "submit":
                        handleSubmit(respond);
                        break;
                    case "getRank":
                        handleGetRank();
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

    public void handleGetRank() {
        ArrayList<User> ans = new ArrayList<User>();
        try {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            String sql = "select * from tbl_user where 1";

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ans.add(new User(rs.getString("userName"), rs.getFloat("score"), rs.getFloat("averageTimeWin"), rs.getFloat("averageCompetitor")));
            }
            Request req = new Request("sendRank",(Object)ans);
            sendRequest(req);
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void handleSubmit(Request res) {
        PostSubmitted post = (PostSubmitted) res.getObject();
        int[] ans = post.getAns();
        User user = post.getUser();    //usr doi thu
        int time = post.getTime();
        ArrayList<Question> questions = post.getQuestions();
        System.out.println(listPlaySession.size());
        for (CouplePlayer c : listPlaySession) {
            System.out.println(c.is_submit1 + " " + c.is_submit2);
            if (this.info.getUser().getUserName().equals(c.getUser1().getUser().getUserName())) {
                c.setUser1(getNumCorrect(ans, questions), time);
                if (c.is_submit2 == true) {
                    sendResult(c);
                }
                break;
            } else if (this.info.getUser().getUserName().equals(c.getUser2().getUser().getUserName())) {
                c.setUser2(getNumCorrect(ans, questions), time);
                if (c.is_submit1 == true) {
                    sendResult(c);
                }
                break;
            }
        }
    }

    public void resetCouple(User u1, User u2) {

        for (CouplePlayer cp : listPlaySession) {
            if (cp.getUser1().getUser().getUserName().equals(u1.getUserName()) && cp.getUser2().getUser().getUserName().equals(u2.getUserName())) {
                listPlaySession.remove(cp);
                break;
            }
        }
    }

    //luu ket qua vao DB va send ket qua cho nguoi choi
    public void sendResult(CouplePlayer c) {
        try {
            Result user1 = new Result(c.getUser1().getUser(), c.getNumCorrect1(), c.getTime1(), c.getUser2().getUser());
            Result user2 = new Result(c.getUser2().getUser(), c.getNumcorrect2(), c.getTime2(), c.getUser1().getUser());
            boolean u1 = false, u2 = false;
            if (user1.getNumCorrect() > user2.getNumCorrect()) {
                u1 = true;
            } else if (user1.getNumCorrect() < user2.getNumCorrect()) {
                u2 = true;
            } else {
                // Bang diem nhau nhau xet theo time
                if (user1.getTime() < user2.getTime()) {
                    u1 = true;
                } else if (user1.getTime() > user2.getTime()) {
                    u2 = true;
                }
            }
            user1.setWin(u1);
            user2.setWin(u2);
            boolean hoa = false; // hòa, do ngu tiếng anh : )
            if (u1 == u2) {
                hoa = true;
            }
            user1.setIsEquals(hoa);
            user2.setIsEquals(hoa);
            updateResultIntoDatabase(user1, user2.getUser().getScore());
            updateResultIntoDatabase(user2, user1.getUser().getScore());
            //Ghi log, A thắng B
            if (hoa) {
                updateLog(user1.getUser(), user2.getUser(), user1.getTime(), user1.getNumCorrect(), 1);
            } else if (u1) {
                updateLog(user1.getUser(), user2.getUser(), user1.getTime(), user1.getNumCorrect(), 0);
            } else {
                updateLog(user2.getUser(), user1.getUser(), user2.getTime(), user2.getNumCorrect(), 0);
            }
            //Gửi kết quả cho người chơi
            Request req1 = new Request("result", (Object) user1);
            c.getUser1().oos.writeObject(req1);

            Request req2 = new Request("result", (Object) user2);
            c.getUser2().oos.writeObject(req2);
            updateStatusOnline(user1.getUser(), user2.getUser());
            resetCouple(user1.getUser(), user2.getUser());// reset lai phien choi, khi choi xong

        } catch (IOException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateStatusOnline(User user1, User user2) {
        for (Map.Entry<String, Info> player : listPlayerSocket.entrySet()) {
            if (user1.getIp().equals(player.getKey()) || user2.getIp().equals(player.getKey())) {
                player.getValue().setStatus(1);
                //break;
            }
        }
    }

    public void updateLog(User win, User lose, int time, int num, int is_equals) {
        try {
            String sql = "insert into tbl_log(userNameA, userNameB, time, numCorrect, is_equals) values(?,?,?,?,?)";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, win.getUserName());
            ps.setString(2, lose.getUserName());
            ps.setInt(3, time);
            ps.setInt(4, num);
            ps.setInt(5, is_equals);
            ps.execute();
        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateResultIntoDatabase(Result user, float totalScore2) {
        try {
            float score = user.getUser().getScore();
            int totalWinTime = getTotalWinTime(user.getUser());
            int numWin = getNumWin(user.getUser());
            float totalCompetitorScore = getTotalCompetitorScore(user.getUser()) + totalScore2;// tong diem cua cac doi thu da choi
            int numMatches = getNumMatches(user.getUser()) + 1;
            float averageTimeWin = getAverageTimeWin(user.getUser());
            float averageCompetitor = 0;
            if (user.getIsEquals()) {
                score += 0.5; // neu hoa +0.5
            } else if (user.getIsWin()) {
                score += 1;
                // Thời gian kết thúc trong các trận thắng
                totalWinTime += user.getTime();
                numWin++;
                averageTimeWin = (float) (totalWinTime * 1.0 / numWin);
            }
            averageCompetitor = totalCompetitorScore / numMatches;
            String sql = "UPDATE tbl_user SET score=?,win=?,averageTimeWin=?,numMatches=?,averageCompetitor=?,totalWinTime=?,totalCompetitorScore=? WHERE userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setFloat(1, score);
            ps.setInt(2, numWin);
            ps.setFloat(3, averageTimeWin);
            ps.setInt(4, numMatches);
            ps.setFloat(5, averageCompetitor);
            ps.setInt(6, totalWinTime);
            ps.setFloat(7, totalCompetitorScore);
            ps.setString(8, user.getUser().getUserName());
            ps.execute();
        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getTotalWinTime(User user) {
        int ans = 0;
        try {
            String sql = "select totalWinTime from tbl_user where userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ans = rs.getInt("totalWinTime");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ans;
    }

    public float getTotalCompetitorScore(User user) {
        float ans = 0;
        try {
            String sql = "select totalCompetitorScore from tbl_user where userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ans = rs.getFloat("totalCompetitorScore");
            }

        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ans;
    }

    public int getNumWin(User user) {
        int ans = 0;
        try {
            String sql = "select win from tbl_user where userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ans = rs.getInt("win");
            }

        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ans;
    }

    public int getNumMatches(User user) {
        int ans = 0;
        try {
            String sql = "select numMatches from tbl_user where userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ans = rs.getInt("numMatches");
            }

        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ans;
    }

    public float getAverageTimeWin(User user) {
        float ans = 0;
        try {
            String sql = "select averageTimeWin from tbl_user where userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ans = rs.getFloat("averageTimeWin");
            }

        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ans;
    }

    public float getAverageCompetitor(User user) {
        float ans = 0;
        try {
            String sql = "select averageCompetitor from tbl_user where userName=?";
            PreparedStatement ps = this.con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ans = rs.getFloat("averageCompetitor");
            }

        } catch (SQLException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ans;
    }

    public int getNumCorrect(int[] ans, ArrayList<Question> questions) {
        int sum = 0;
        for (int i = 0; i < ans.length; i++) {
            if (ans[i] == questions.get(i).getCorrectAns()) {
                sum++;
            }
        }
        return sum;
    }

    public void createSession(String ip) {
        Info user1 = listPlayerSocket.get(ip);
        addSession(user1, this.info);
        PlaySession playS = new PlaySession(user1, this.info, this.con);
        playS.start();
    }

    public void addSession(Info i1, Info i2) {
        // thêm phiên làm bài của 2 user, = false => chưa nộp bài
        listPlaySession.add(new CouplePlayer(i1, i2));
    }

    //trả lại thông báo từ chối cho người mời
    public void sendRefuse(Request res) {
        try {
            String ip = (String) res.getObject();

            Request req = new Request("refuse", (Object) this.info.getUser().getUserName());
            listPlayerSocket.get(ip).oos.writeObject(req);
        } catch (IOException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Chuyển l ờithách đấu của user1 gửi đến User2
    public void forwardInvite(Request res) {
        try {
            String ip = (String) res.getObject();
            System.out.println("nhan loi thach dau");

            Request req = new Request("challange", (Object) user);
            //System.out.println("send to "+s.getValue().getSocket().getInetAddress().getHostAddress());
            listPlayerSocket.get(ip).oos.writeObject(req);
            //s.getValue().oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(ThreadServerListen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void handleLogin(Request respond) {
        User user = (User) respond.getObject();
        String datasend = "fail";
        Request req = null;
        if (checkUser(user)) {
            //System.out.println(user.getScore());
            datasend = "success";
            user.setIp(info.getSocket().getInetAddress().getHostAddress());
            this.user = user;
            info.setUser(user);
            info.setStatus(1);
            req = new Request("login", (Object) this.user);
        } else {
            req = new Request("login", (Object) "fail");
        }
        sendRequest(req);
    }

    public void sendAccount(User user) {
        try {

            oos.writeObject(user);
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendRequest(Request request) {
        try {
            oos.writeObject(request);
            //oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendListPlayer() {
        players.clear();
        players = new HashMap<>();
        for (Map.Entry<String, Info> player : listPlayerSocket.entrySet()) {
            if (player.getValue().getSocket().isConnected()) {
                players.put(player.getKey(), new Pair(player.getValue().getUser(), player.getValue().getStatus()));
            }
        }
        Map<String, Pair<User, Integer>> listPlayer = players;

        sendRequest(new Request("sendListPlayer", (Object) listPlayer));
        //oos.writeObject(listPlayer);
    }

    public boolean checkUser(User user) {
        String sql = "select * from tbl_user where userName=? and passWord=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, user.getUserName());
            ps.setString(2, user.getPassWord());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user.setScore(rs.getFloat("score"));
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
}
