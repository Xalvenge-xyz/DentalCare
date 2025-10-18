package services;

import config.config;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuthService {

    public static Map<String, Object> authenticate(Scanner sc, config conf) {
        System.out.print("Enter Email: ");
        String email = sc.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = sc.nextLine().trim();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = conf.getConnection();

            String sql = "SELECT * FROM tbl_accounts WHERE acc_email = ? AND acc_pass = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("acc_status");
                if (!"Approved".equalsIgnoreCase(status)) {
                    System.out.println("❌ Account is not approved yet.");
                    return null;
                }

                Map<String, Object> user = new HashMap<>();
                user.put("acc_id", rs.getInt("acc_id"));
                user.put("acc_name", rs.getString("acc_name"));
                user.put("acc_email", rs.getString("acc_email"));
                user.put("acc_role", rs.getString("acc_role"));
                user.put("acc_contact", rs.getString("acc_contact"));
                user.put("acc_status", status);

                return user;
            } else {
                System.out.println("❌ Invalid credentials.");
                return null;
            }

        } catch (SQLException e) {
            System.out.println("❌ Database Error: " + e.getMessage());
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                System.out.println("❌ Error closing resources: " + ex.getMessage());
            }
        }
    }
}