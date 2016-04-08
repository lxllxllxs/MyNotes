package cn.lxl.notes.db;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Administrator on 2016/3/27.
 */
public class sqlhelper {
    private String url = "jdbc:mysql://192.168.56.1:3306/mynote";//192.168.56.1为VBOX虚拟机的ip地址
    private String tableName = "account";
    private String username = "root";
    private String password = "lxl774785161";
    private ResultSet rs;

    public Boolean openMynote(String sql) {
        Connection connection=null;
        Statement stmt=null;
        Boolean hasresult=false;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);
            stmt = connection.createStatement();
            hasresult = stmt.execute(sql);
            Log.d("openMyNote",hasresult+"");
            if (hasresult) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    Log.d("OPENMYSQL", rs.getString("username") + "" + rs.getString("apassword"));
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
                stmt.close();
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return hasresult;
    }
}