package config;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class config {

    // Connection method to SQLITE
    public static Connection connectDB() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:dentalcare.db");
            //System.out.println("Connection Successful");
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e);
        }
        return con;
    }

    public void addRecord(String sql, Object... values) {
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setPreparedStatementValues(pstmt, values);
            pstmt.executeUpdate();
            //System.out.println("Record added successfully!");

        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
        }
    }

    public void updateRecord(String sql, Object... values) {
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setPreparedStatementValues(pstmt, values);
            pstmt.executeUpdate();
            //System.out.println("Record updated successfully!");

        } catch (SQLException e) {
            System.out.println("Error updating record: " + e.getMessage());
        }
    }

    public void deleteRecord(String sql, Object... values) {
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setPreparedStatementValues(pstmt, values);
            pstmt.executeUpdate();
            System.out.println("Record deleted successfully!");

        } catch (SQLException e) {
            System.out.println("Error deleting record: " + e.getMessage());
        }
    }

    public java.util.List<java.util.Map<String, Object>> fetchRecords(String sqlQuery, Object... values) {
        java.util.List<java.util.Map<String, Object>> records = new java.util.ArrayList<>();

        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {

            setPreparedStatementValues(pstmt, values);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                records.add(row);
            }

        } catch (SQLException e) {
            System.out.println("Error fetching records: " + e.getMessage());
        }

        return records;
    }

    private void setPreparedStatementValues(PreparedStatement pstmt, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            pstmt.setObject(i + 1, values[i]);
        }
    }

    public Connection getConnection() {
        return connectDB();  
    }

   public Map<String, Object> fetchSingleRecord(String sql, Object... params) {
    Map<String, Object> record = null;

    try (PreparedStatement ps = connectDB().prepareStatement(sql)) {
        // set parameters safely
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }

        ResultSet rs = ps.executeQuery();
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();

        if (rs.next()) {
            record = new HashMap<>();
            for (int i = 1; i <= columns; i++) {
                record.put(md.getColumnLabel(i), rs.getObject(i));
            }
        }

        rs.close();
    } catch (SQLException e) {
        System.out.println("❌ SQL Error: " + e.getMessage());
    }

    return record;
}
public int getCount(String query, Object... params) throws Exception {
    List<Map<String, Object>> result = fetchRecords(query, params);
    if (result.isEmpty()) return 0;
    return ((Number) result.get(0).values().toArray()[0]).intValue();
}

}