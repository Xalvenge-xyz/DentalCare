package main;

import services.MenuController;
import config.config;
import java.util.Scanner;

public class main {

    public static void main(String[] args) {
       
        config conf = new config();
        Scanner sc = new Scanner(System.in);
        
     
        DatabaseSetup dbSetup = new DatabaseSetup(conf);
        dbSetup.createTables();

       
        MenuController menu = new MenuController(conf, sc);
        menu.start();

        sc.close();
        //System.out.println("System exited successfully.");
    }
}