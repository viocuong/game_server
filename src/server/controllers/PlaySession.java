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
public class PlaySession extends Thread{
    private Info info1;
    private Info info2;
    private Connection con;
    public PlaySession(Info i1, Info i2, Connection con){
        this.info1 = i1;
        this.info2 = i2;
        this.con = con;
        
    }
    public void run(){
        createQuestion(info1);
        createQuestion(info2);
//        while(true){
//            
//        }
    }
    public void createQuestion(Info info1){
        ArrayList<Question> questions = new ArrayList<>();
        try {
            String sql ="select * from tbl_question ORDER BY RAND() LIMIT 10";
            PreparedStatement s = this.con.prepareStatement(sql);
            ResultSet rs = s.executeQuery();
            while(rs.next()){
                Question q = new Question();
                q.setQuestion(rs.getString("question"));
                ArrayList<String> listans= new ArrayList<>();
                listans.add(rs.getString("ans1"));
                listans.add(rs.getString("ans2"));
                listans.add(rs.getString("ans3"));
                listans.add(rs.getString("ans4"));
                q.setAns(listans);
                q.setCorrect(rs.getInt("correct"));
                questions.add(q);
                //questions.add(new Question(rs.getString(""), listans, NORM_PRIORITY))
            }
            info1.oos.writeObject(new Request("sendListQuestion",(Object)questions));
            
            
        } catch (SQLException ex) {
            Logger.getLogger(PlaySession.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PlaySession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
