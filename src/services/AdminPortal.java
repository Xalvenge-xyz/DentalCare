package services;

import config.config;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AdminPortal {

    private final Scanner sc;
    private final config conf;

    public AdminPortal(Scanner sc, config conf) {
        this.sc = sc;
        this.conf = conf;
    }

    // ===================== RUN DASHBOARD =====================
    public void run() {
        System.out.println("\n== Admin Login ==");

        Map<String, Object> user = AuthService.authenticate(sc, conf);

        if (user == null || !"Admin".equals(user.get("acc_role")) || !"Approved".equals(user.get("acc_status"))) {
            System.out.println("❌ Access denied: Invalid credentials or unapproved account.");
            return;
        }

        System.out.println("✅ Login successful! Welcome, " + user.get("acc_name") + " (Admin).");

        boolean loop = true;

        while (loop) {
            System.out.println("\n================== ADMIN DASHBOARD ==================");
            System.out.println("Welcome, " + user.get("acc_name") + "!\n");

            System.out.println("1. Approve Pending Staff Accounts");
            System.out.println("2. Approve Pending Dentist Accounts");
            System.out.println("3. View All User Accounts");
            System.out.println("4. Update User Account Status");
            System.out.println("5. Search User Accounts");
            System.out.println("6. View All Appointments");
            System.out.println("7. Logout");

            System.out.print("\nSelect an option [1-7]: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    ApprovalService.approveAccounts(sc, conf, "Staff");
                    break;
                case "2":
                    ApprovalService.approveAccounts(sc, conf, "Dentist");
                    break;
                case "3":
                    viewAllUsers();
                    break;
                case "4":
                    updateUserStatus(user);
                    break;
                case "5":
                    searchUsers();
                    break;
                case "6":
                    viewAllAppointments();
                    break;
                case "7":
                    loop = false;
                    System.out.println("Logged out. Returning to main menu...");
                    break;
                default:
                    System.out.println("❌ Invalid option. Try again.");
                    break;
            }
        }
    }

    // ===================== VIEW ALL USERS =====================
 private void viewAllUsers() {
    try {
        List<Map<String, Object>> users = conf.fetchRecords(
                "SELECT acc_id, acc_name, acc_role, acc_email, acc_status " +
                        "FROM tbl_accounts " +
                        "WHERE acc_role != 'Super Admin' " +  // hide superadmin
                        "ORDER BY acc_role, acc_name"
        );

        if (users == null || users.isEmpty()) {
            System.out.println("⚠️ No users found in the system.");
            return;
        }

        System.out.println("\n================== ALL USER ACCOUNTS ==================");
        System.out.printf("%-5s %-20s %-12s %-25s %-12s%n", "ID", "Name", "Role", "Email", "Status");
        System.out.println("---------------------------------------------------------------");

        for (Map<String, Object> u : users) {
            System.out.printf("%-5s %-20s %-12s %-25s %-12s%n",
                    u.get("acc_id"),
                    u.get("acc_name"),
                    u.get("acc_role"),
                    u.get("acc_email"),
                    u.get("acc_status")
            );
        }
        System.out.println("---------------------------------------------------------------");

    } catch (Exception e) {
        System.out.println("❌ Error fetching users: " + e.getMessage());
    }
}
    // ===================== UPDATE USER STATUS =====================
    private void updateUserStatus(Map<String, Object> currentAdmin) {
        try {
            System.out.print("\nEnter the User ID to update: ");
            int userId = Integer.parseInt(sc.nextLine().trim());

            if ((int) currentAdmin.get("acc_id") == userId) {
                System.out.println("❌ You cannot change your own account status.");
                return;
            }

            Map<String, Object> userRecord = conf.fetchSingleRecord(
                    "SELECT acc_id, acc_name, acc_role, acc_status FROM tbl_accounts WHERE acc_id = ? AND acc_status != 'Pending Approval' AND acc_role != 'SuperAdmin'",
                    userId
            );

            if (userRecord == null) {
                System.out.println("❌ User not found or still pending approval.");
                return;
            }

            String currentStatus = (String) userRecord.get("acc_status");
            System.out.println("\n================== UPDATE USER STATUS ==================");
            System.out.printf("ID      : %s%n", userRecord.get("acc_id"));
            System.out.printf("Name    : %s%n", userRecord.get("acc_name"));
            System.out.printf("Role    : %s%n", userRecord.get("acc_role"));
            System.out.printf("Current : %s%n", currentStatus);
            System.out.println("--------------------------------------------------------");

            Map<Integer, String> options = new java.util.LinkedHashMap<>();
            int optNumber = 1;

            switch (currentStatus) {
                case "Approved":
                    options.put(optNumber++, "Deactivate");
                    break;
                case "Deactivated":
                case "Rejected":
                    options.put(optNumber++, "Approve");
                    break;
            }

            if (options.isEmpty()) {
                System.out.println("⚠️ No valid actions for this user.");
                return;
            }

            System.out.println("Select the new status for this user:");
            for (Map.Entry<Integer, String> entry : options.entrySet()) {
                System.out.printf("%d. %s%n", entry.getKey(), entry.getValue());
            }
            System.out.print("Enter your choice [1-" + (optNumber - 1) + "]: ");
            int choice = Integer.parseInt(sc.nextLine().trim());

            if (!options.containsKey(choice)) {
                System.out.println("❌ Invalid choice. Operation cancelled.");
                return;
            }

            String newStatus = options.get(choice);

            System.out.printf("You are about to change %s's status from %s → %s. Confirm? (y/n): ",
                    userRecord.get("acc_name"), currentStatus, newStatus);
            String confirm = sc.nextLine().trim();
            if (!confirm.equalsIgnoreCase("y")) {
                System.out.println("❌ Operation cancelled.");
                return;
            }

            conf.updateRecord(
                    "UPDATE tbl_accounts SET acc_status = ? WHERE acc_id = ?",
                    newStatus, userId
            );

            System.out.println("✅ User status successfully updated to " + newStatus);

        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid ID entered. Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    // ===================== SEARCH USERS =====================
    private void searchUsers() {
    try {
        System.out.print("\nEnter search keyword (name, email, or role): ");
        String keyword = sc.nextLine().trim();

        if (keyword.isEmpty()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        // Only search name, email, or roles that are NOT 'Super Admin'
        List<Map<String, Object>> results = conf.fetchRecords(
            "SELECT acc_id, acc_name, acc_role, acc_email, acc_status " +
            "FROM tbl_accounts " +
            "WHERE (acc_name LIKE ? OR acc_email LIKE ? OR (acc_role LIKE ? AND acc_role != 'Super Admin')) " +
            "ORDER BY acc_role, acc_name",
            "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%"
        );

        if (results == null || results.isEmpty()) {
            System.out.println("⚠️ No users match your search.");
            return;
        }

        System.out.println("\n================== SEARCH RESULTS ==================");
        System.out.printf("%-5s %-20s %-12s %-25s %-12s%n", "ID", "Name", "Role", "Email", "Status");
        System.out.println("---------------------------------------------------------------");

        for (Map<String, Object> u : results) {
            System.out.printf("%-5s %-20s %-12s %-25s %-12s%n",
                    u.get("acc_id"),
                    u.get("acc_name"),
                    u.get("acc_role"),
                    u.get("acc_email"),
                    u.get("acc_status")
            );
        }
        System.out.println("---------------------------------------------------------------");

    } catch (Exception e) {
        System.out.println("❌ Error during search: " + e.getMessage());
    }
}

    // ===================== VIEW ALL APPOINTMENTS =====================
    private void viewAllAppointments() {
        try {
            List<Map<String, Object>> appointments = conf.fetchRecords(
                "SELECT a.app_id, p.pat_name, d.acc_name AS dentist_name, " +
                "a.app_date, a.app_time, a.app_status " +
                "FROM tbl_appointments a " +
                "LEFT JOIN tbl_patients p ON a.pat_id = p.pat_id " +
                "LEFT JOIN tbl_accounts d ON a.dentist_id = d.acc_id " +
                "ORDER BY a.app_date, a.app_time"
            );

            if (appointments == null || appointments.isEmpty()) {
                System.out.println("⚠️ No appointments found.");
                return;
            }

            System.out.println("\n================== ALL APPOINTMENTS ==================");
            System.out.printf("%-5s %-20s %-20s %-12s %-8s %-12s%n", "ID", "Patient", "Dentist", "Date", "Time", "Status");
            System.out.println("--------------------------------------------------------------------------");

            for (Map<String, Object> a : appointments) {
                System.out.printf("%-5s %-20s %-20s %-12s %-8s %-12s%n",
                        a.get("app_id"),
                        a.get("pat_name"),
                        a.get("dentist_name") != null ? a.get("dentist_name") : "Unassigned",
                        a.get("app_date"),
                        a.get("app_time"),
                        a.get("app_status")
                );
            }
            System.out.println("--------------------------------------------------------------------------");

        } catch (Exception e) {
            System.out.println("❌ Error fetching appointments: " + e.getMessage());
        }
    }
}
