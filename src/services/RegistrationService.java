package services;

import config.config;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RegistrationService {

    public static void registerAccount(Scanner sc, config conf) {
        System.out.println("\n==============================");
        System.out.println("       ACCOUNT REGISTRATION    ");
        System.out.println("==============================");

        System.out.println("Please select your role:");
        System.out.println("1. Staff (requires Admin approval)");
        System.out.println("2. Admin (requires Super Admin approval)");
        System.out.println("3. Dentist (requires Admin approval)");
        System.out.print("Enter choice (1-3): ");

        int roleChoice;
        try {
            roleChoice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input. Registration canceled.");
            return;
        }

        String role;
        String status = "Pending Approval";

        switch (roleChoice) {
            case 1: role = "Staff"; break;
            case 2: role = "Admin"; break;
            case 3: role = "Dentist"; break;
            default:
                System.out.println("❌ Invalid choice. Registration canceled.");
                return;
        }

        System.out.println("\n--- Enter Your Details ---");
        System.out.print("Full Name       : ");
        String name = sc.nextLine();

        System.out.print("Enter Email  : ");
        String email = sc.nextLine();

        System.out.print("Password        : ");
        String password = sc.nextLine();

        System.out.print("Contact Number  : ");
        String contact = sc.nextLine();

        String query = "INSERT INTO tbl_accounts (acc_name, acc_email, acc_pass, acc_contact, acc_role, acc_status) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            conf.updateRecord(query, name, email, password, contact, role, status);

            // ✅ If Dentist, also add entry to tbl_dentists
            if ("Dentist".equals(role)) {
                System.out.print("Specialty       : ");
                String specialty = sc.nextLine();

                // Fetch dentist acc_id
                List<Map<String, Object>> result = conf.fetchRecords(
                        "SELECT acc_id FROM tbl_accounts WHERE acc_email = ?", email);

                if (!result.isEmpty()) {
                    int dentistId = (Integer) result.get(0).get("acc_id");
                    String dentistQuery = "INSERT INTO tbl_dentists (dentist_id, specialty) VALUES (?, ?)";
                    conf.updateRecord(dentistQuery, dentistId, specialty);
                } else {
                    System.out.println("❌ Error fetching dentist ID.");
                }
            }

            System.out.println("\n✅ Account created successfully!");
            System.out.println("Role    : " + role);
            System.out.println("Status  : " + status);
            System.out.println("Note    : Please wait for approval before logging in.\n");

        } catch (Exception e) {
            System.out.println("❌ Registration failed: " + e.getMessage());
        }
    }

    public static void staffRegister(Scanner sc, config conf) {
        registerAccount(sc, conf);
    }

    public static void adminRegister(Scanner sc, config conf) {
        registerAccount(sc, conf);
    }

    public static void dentistRegister(Scanner sc, config conf) {
        registerAccount(sc, conf);
    }
}
