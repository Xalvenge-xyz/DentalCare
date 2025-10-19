package services;

import config.config;
import services.ApprovalService;
import services.AuthService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SuperAdminPortal {

    private Scanner sc;
    private config conf;

    public SuperAdminPortal(Scanner sc, config conf) {
        this.sc = sc;
        this.conf = conf;
    }

    public void run() {
        System.out.println("\n== Super Admin Login ==");

        Map<String, Object> user = AuthService.authenticate(sc, conf);

        if (user == null || !"Super Admin".equals(user.get("acc_role"))) {
            System.out.println("❌ Invalid Super Admin credentials or account not approved.");
            return;
        }

        System.out.println("Login successful! Welcome, " + user.get("acc_name") + " (Super Admin).");

        boolean loop = true;
        while (loop) {
            System.out.println("\n== Super Admin Dashboard ==");
            System.out.println("1. Approve Admin Accounts");
            System.out.println("2. Approve Staff Accounts");
            System.out.println("3. Approve Dentist Accounts");
            System.out.println("4. View All Accounts");
            System.out.println("5. Deactivate / Reactivate Account");
            System.out.println("6. Delete Account");
            System.out.println("7. View all Data");
            System.out.println("8. Logout");
            System.out.print("Select an option: ");

            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    ApprovalService.approveAccounts(sc, conf, "Admin");
                    break;
                case "2":
                    ApprovalService.approveAccounts(sc, conf, "Staff");
                    break;
                case "3":
                    ApprovalService.approveAccounts(sc, conf, "Dentist");
                    break;
                case "4":
                    viewAllAccounts();
                    break;
                case "5":
                    toggleAccountStatus();
                    break;
                case "6":
                    deleteAccount();
                    break;
                case "7":
                    showDatabaseSummary();
                    break;
                case "8":
		    loop = false;
                    System.out.println("🔒 Logged out.");
                    break;
                default:
                    System.out.println("❌ Invalid option. Try again.");
                    break;
            }
        }
    }

    // ===================== VIEW ALL ACCOUNTS =====================
    private void viewAllAccounts() {
        try {
            System.out.print("Filter by role (All/Admin/Staff/Dentist/Super Admin): ");
            String role = sc.nextLine().trim();

            String query = "SELECT acc_id, acc_name, acc_email, acc_role, acc_status FROM tbl_accounts";
            if (!role.equalsIgnoreCase("All") && !role.isEmpty()) {
                query += " WHERE acc_role = '" + role + "'";
            }

            List<Map<String, Object>> accounts = conf.fetchRecords(query);

            if (accounts.isEmpty()) {
                System.out.println("⚠️ No accounts found.");
                return;
            }

            System.out.println("\n--- Accounts List ---");
            for (Map<String, Object> acc : accounts) {
                System.out.println("ID: " + acc.get("acc_id") +
                        " | Name: " + acc.get("acc_name") +
                        " | Email: " + acc.get("acc_email") +
                        " | Role: " + acc.get("acc_role") +
                        " | Status: " + acc.get("acc_status"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching accounts: " + e.getMessage());
        }
    }

    // ===================== DEACTIVATE / REACTIVATE ACCOUNT =====================
    private void toggleAccountStatus() {
        try {
            System.out.print("Enter Account ID to change status: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            List<Map<String, Object>> acc = conf.fetchRecords("SELECT * FROM tbl_accounts WHERE acc_id = ?", id);

            if (acc.isEmpty()) {
                System.out.println("❌ Account not found.");
                return;
            }

            String currentStatus = (String) acc.get(0).get("acc_status");
            String newStatus = currentStatus.equals("Deactivated") ? "Approved" : "Deactivated";

            conf.updateRecord("UPDATE tbl_accounts SET acc_status = ? WHERE acc_id = ?", newStatus, id);
            System.out.println("✅ Account status changed to: " + newStatus);
        } catch (Exception e) {
            System.out.println("❌ Error updating account: " + e.getMessage());
        }
    }

    // ===================== DELETE ACCOUNT =====================
    private void deleteAccount() {
        try {
            System.out.print("Enter Account ID to delete: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Are you sure you want to permanently delete this account? (yes/no): ");
            String confirm = sc.nextLine().trim().toLowerCase();

            if (!confirm.equals("yes")) {
                System.out.println("❌ Deletion cancelled.");
                return;
            }

            conf.updateRecord("DELETE FROM tbl_accounts WHERE acc_id = ?", id);
            System.out.println("🗑️ Account deleted successfully.");
        } catch (Exception e) {
            System.out.println("❌ Error deleting account: " + e.getMessage());
        }
    }

    // ===================== VIEW DATABASE SUMMARY =====================
    private void showDatabaseSummary() {
        try {
            int accounts = conf.getCount("SELECT COUNT(*) FROM tbl_accounts");
            int patients = conf.getCount("SELECT COUNT(*) FROM tbl_patients");
            int dentists = conf.getCount("SELECT COUNT(*) FROM tbl_dentists");
            int appointments = conf.getCount("SELECT COUNT(*) FROM tbl_appointments");

            System.out.println("\nDATA SUMMARY");
            System.out.println("Accounts: " + accounts);
            System.out.println("Patients: " + patients);
            System.out.println("Dentists: " + dentists);
            System.out.println("Appointments: " + appointments);
        } catch (Exception e) {
            System.out.println("❌ Error showing summary: " + e.getMessage());
        }
    }
}
