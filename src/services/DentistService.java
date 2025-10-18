package services;

import config.config;
import java.util.*;

public class DentistService {

    public static void updateDentistSchedule(Scanner sc, config conf) {
        try {
            System.out.print("Enter Dentist ID (same as account ID): ");
            int dentistId = Integer.parseInt(sc.nextLine().trim());

            // Check if dentist account exists
            List<Map<String, Object>> accountCheck = conf.fetchRecords(
                "SELECT * FROM tbl_accounts WHERE acc_id = ? AND acc_role = 'Dentist'", dentistId
            );
            if (accountCheck.isEmpty()) {
                System.out.println("❌ No dentist account found with ID " + dentistId);
                return;
            }

            // Check if dentist already has a record
            List<Map<String, Object>> check = conf.fetchRecords(
                "SELECT * FROM tbl_dentists WHERE dentist_id = ?", dentistId
            );
            boolean exists = !check.isEmpty();

            System.out.print("Enter Specialty: ");
            String specialty = sc.nextLine().trim();

            System.out.print("Enter Work Start Time (HH:mm): ");
            String start = sc.nextLine().trim();

            System.out.print("Enter Work End Time (HH:mm): ");
            String end = sc.nextLine().trim();

            System.out.print("Enter Work Days (e.g., Monday,Tuesday,Wednesday): ");
            String days = sc.nextLine().trim();

            // Validate time format
            try {
                java.time.LocalTime.parse(start);
                java.time.LocalTime.parse(end);
            } catch (Exception e) {
                System.out.println("❌ Invalid time format. Use HH:mm (e.g., 09:00).");
                return;
            }

            // Validate day names
            List<String> validDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
            for (String day : days.split(",")) {
                if (!validDays.contains(day.trim())) {
                    System.out.println("❌ Invalid day: " + day + ". Use full day names.");
                    return;
                }
            }

            if (exists) {
                conf.updateRecord(
                    "UPDATE tbl_dentists SET specialty = ?, work_start = ?, work_end = ?, work_days = ? WHERE dentist_id = ?",
                    specialty, start, end, days, dentistId
                );
                System.out.println("✅ Dentist schedule updated.");
            } else {
                conf.updateRecord(
                    "INSERT INTO tbl_dentists (dentist_id, specialty, work_start, work_end, work_days) VALUES (?, ?, ?, ?, ?)",
                    dentistId, specialty, start, end, days
                );
                System.out.println("✅ Dentist schedule created.");
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    public static void viewAllDentistSchedules(Scanner sc, config conf) {
        List<Map<String, Object>> dentists = conf.fetchRecords(
            "SELECT a.acc_id, a.acc_name, d.specialty, d.work_start, d.work_end, d.work_days " +
            "FROM tbl_accounts a " +
            "LEFT JOIN tbl_dentists d ON a.acc_id = d.dentist_id " +
            "WHERE a.acc_role = 'Dentist' " +
            "ORDER BY a.acc_name"
        );

        if (dentists == null || dentists.isEmpty()) {
            System.out.println("⚠️ No dentist records found.");
            return;
        }

        System.out.println("\n🦷 All Dentist Schedules:");
        System.out.printf("%-5s %-20s %-20s %-10s %-10s %-30s%n",
            "ID", "Name", "Specialty", "Start", "End", "Work Days");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Map<String, Object> d : dentists) {
            System.out.printf("%-5s %-20s %-20s %-10s %-10s %-30s%n",
                d.get("acc_id"),
                d.get("acc_name"),
                d.get("specialty") != null ? d.get("specialty") : "Not set",
                d.get("work_start") != null ? d.get("work_start") : "-",
                d.get("work_end") != null ? d.get("work_end") : "-",
                d.get("work_days") != null ? d.get("work_days") : "-"
            );
        }
    }
}
