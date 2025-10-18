package main;

import config.config;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSetup {

    private final config conf;

    public DatabaseSetup(config conf) {
        this.conf = conf;
    }

    public void createTables() {
        try (Connection conn = config.connectDB(); Statement stmt = conn.createStatement()) {

            // ===================== Accounts Table (Users, Staff, Admin, Dentist) =====================
            String createAccountsTable = "CREATE TABLE IF NOT EXISTS tbl_accounts ("
                    + "acc_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "acc_name TEXT NOT NULL, "
                    + "acc_email TEXT UNIQUE NOT NULL, "
                    + "acc_pass TEXT NOT NULL, "
                    + "acc_contact TEXT, "
                    + "acc_role TEXT NOT NULL CHECK(acc_role IN ('Staff', 'Admin', 'Dentist', 'Super Admin')), "
                    + "acc_status TEXT NOT NULL DEFAULT 'Pending Approval' "
                    + "CHECK(acc_status IN ('Pending Approval', 'Approved', 'Rejected', 'Deactivated'))"
                    + ")";

            // ===================== Patients Table =====================
            String createPatientsTable = "CREATE TABLE IF NOT EXISTS tbl_patients ("
                    + "pat_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "pat_name TEXT NOT NULL, "
                    + "pat_age INTEGER NOT NULL, "
                    + "pat_sex TEXT CHECK(pat_sex IN ('M', 'F')), "
                    + "pat_contact TEXT, "
                    + "pat_address TEXT"
                    + ")";

            // ===================== Dentists Additional Info Table =====================
            String createDentistsTable = "CREATE TABLE IF NOT EXISTS tbl_dentists ("
                    + "dentist_id INTEGER PRIMARY KEY, "
                    + "specialty TEXT, "
                    + "work_start TEXT DEFAULT '08:00', "
                    + "work_end TEXT DEFAULT '17:00', "
                    + "work_days TEXT DEFAULT 'Monday,Tuesday,Wednesday,Thursday,Friday', "
                    + "FOREIGN KEY (dentist_id) REFERENCES tbl_accounts(acc_id) ON DELETE CASCADE"
                    + ")";

            // ===================== Appointments Table =====================
            String createAppointmentsTable = "CREATE TABLE IF NOT EXISTS tbl_appointments ("
                    + "app_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "pat_id INTEGER NOT NULL, "
                    + "dentist_id INTEGER NOT NULL, "
                    + "app_date TEXT NOT NULL, "
                    + "app_time TEXT NOT NULL, "
                    + "app_service TEXT, "
                    + "app_status TEXT NOT NULL DEFAULT 'Scheduled' "
                    + "CHECK(app_status IN ('Scheduled', 'Completed', 'Cancelled')), "
                    + "app_notes TEXT, "
                    + "FOREIGN KEY (pat_id) REFERENCES tbl_patients(pat_id), "
                    + "FOREIGN KEY (dentist_id) REFERENCES tbl_accounts(acc_id)"
                    + ")";

            // ===================== Execute Table Creations =====================
            conf.updateRecord(createAccountsTable);
            conf.updateRecord(createPatientsTable);
            conf.updateRecord(createDentistsTable);
            conf.updateRecord(createAppointmentsTable);

            System.out.println("✅ All database tables are ready.");
        } catch (Exception e) {
            System.out.println("❌ Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
