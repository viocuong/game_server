/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controllers;

import Models.com.Info;
import static java.lang.Thread.sleep;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cuongnv
 */
public class updatePlayerOnline extends Thread {

    public Map<String, Info> listPlayer;

    public updatePlayerOnline(Map<String, Info> lp) {
        this.listPlayer = lp;
    }

    @Override
    public void run() {
        while (true) {
            try {

                sleep(2000);
                updateOnline();
            } catch (InterruptedException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public synchronized void updateOnline() {
        for (Map.Entry<String, Info> p : listPlayer.entrySet()) {
            if (p.getValue().getSocket().isClosed()) {
                listPlayer.remove(p.getKey());
                break;
            }
        }
        System.out.println(listPlayer.size());
    }

}
