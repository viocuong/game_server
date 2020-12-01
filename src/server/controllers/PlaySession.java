/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import Models.com.Info;
import Models.com.Question;
import Models.com.Request;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cuongnv
 */
public class PlaySession extends Thread {

    public boolean is_createQuestion = false;
    private Info info1;
    private Info info2;
    private Connection con;
    private ArrayList<Question> questions;

    public PlaySession(Info i1, Info i2, Connection con) {
        this.info1 = i1;
        this.info2 = i2;
        this.con = con;
        updateStatusPlaying();
    }

    public void run() {
        //tao cau 10 cau hoi ngau nhien gui cho user1 va user2

        createQuestion();
//        while(true){
//            
//        }
    }

    //Cập nhật trạng thái online
    public void updateStatusOnline() {
        this.info1.setStatus(1);
        this.info2.setStatus(1);
    }

    // cập nhật trạng thái bận cho 2 người chơi;
    public void updateStatusPlaying() {
        this.info1.setStatus(2);
        this.info2.setStatus(2);
    }

    public void createQuestion() {
        ArrayList<Question> questions = new ArrayList<>();
        try {
            String sql = "select * from tbl_question ORDER BY RAND() LIMIT 10";
            PreparedStatement s = this.con.prepareStatement(sql);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.setQuestion(rs.getString("question"));
                ArrayList<String> listans = new ArrayList<>();
                listans.add(rs.getString("ans1"));
                listans.add(rs.getString("ans2"));
                listans.add(rs.getString("ans3"));
                listans.add(rs.getString("ans4"));
                q.setAns(listans);
                q.setCorrect(rs.getInt("correct"));
                questions.add(q);
                //questions.add(new Question(rs.getString(""), listans, NORM_PRIORITY))
            }
            info1.oos.writeObject(new Request("sendListQuestion", (Object) questions, (Object) info2.getUser()));
            info2.oos.writeObject(new Request("sendListQuestion", (Object) questions, (Object) info1.getUser()));
            this.is_createQuestion = true;
            this.questions = questions;
        } catch (SQLException ex) {
            Logger.getLogger(PlaySession.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PlaySession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<Question> getQuestions() {
        return this.questions;
    }
}
