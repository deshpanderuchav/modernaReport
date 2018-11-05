package com.liconic.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBKIWIConnection {

    private String DBdriver;
    private String DBpath;
    private String DBuser;
    private String DBpassword;
    
    public DBKIWIConnection(String DBDrv, String Pth, String Usr, String Pwd) throws Exception {

        DBpath = Pth;
        DBuser = Usr;
        DBpassword = Pwd;
        DBdriver = DBDrv;

        System.out.println("DB=" + DBpath);

        try {
            Class.forName(DBdriver);
        } catch (Exception E) {
            System.err.println(E.getMessage());
        }

    }

    public synchronized Connection getConnection() throws SQLException {

        return DriverManager.getConnection(DBpath, DBuser, DBpassword);

    }

}
