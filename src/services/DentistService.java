package services;

import config.config;
import java.util.*;
import java.time.LocalTime;

public class DentistService {

    public static void updateDentistSchedule(Scanner sc, config conf, int dentistId) {
    try {
        System.out.println("\n== Update Work Schedule ==");

        // ✅ Check if dentist already has a schedule
        List<Map<String, Object>> existingSchedule = conf.fetchRecords(
            "SELECT * FROM tbl_dentists WHERE dentist_id = ?", dentistId
        );
        boolean exists = !existingSchedule.isEmpty();

        if (exists) {
            Map<String, Object> current = existingSchedule.get(0);
            System.out.println("\nCurrent Schedule:");
            System.out.println("Work Days : " + current.get("work_days"));
            System.out.println("Start Time: " + current.get("work_start"));
            System.out.println("End Time  : " + current.get("work_end"));
        }

        // ✅ Input Work Hours
        System.out.print("\nEnter New Work Start Time (HH:mm): ");
        String start = sc.nextLine().trim();

        System.out.print("Enter New Work End Time (HH:mm): ");
        String end = sc.nextLine().trim();

        try {
            LocalTime.parse(start);
            LocalTime.parse(end);
        } catch (Exception e) {
            System.out.println("❌ Invalid time format. Use HH:mm (e.g., 09:00).");
            return;
        }

        // ✅ Select Work Days (Multiple Choice)
        List<String> validDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        List<String> selectedDays = new ArrayList<>();

        System.out.println("\nSelect Work Days (choose multiple, separated by commas):");
        System.out.println("1. Monday");
        System.out.println("2. Tuesday");
        System.out.println("3. Wednesday");
        System.out.println("4. Thursday");
        System.out.println("5. Friday");
        System.out.print("Enter numbers (e.g., 1,3,5): ");
        String input = sc.nextLine().trim();

        if (input.isEmpty()) {
            System.out.println("❌ No days selected. Update canceled.");
            return;
        }

        for (String num : input.split(",")) {
            try {
                int day = Integer.parseInt(num.trim());
                if (day >= 1 && day <= 5) {
                    selectedDays.add(validDays.get(day - 1));
                } else {
                    System.out.println("⚠️ Skipping invalid number: " + num);
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Skipping invalid input: " + num);
            }
        }

        if (selectedDays.isEmpty()) {
            System.out.println("❌ No valid days selected. Update canceled.");
            return;
        }

        String days = String.join(",", selectedDays);

        // ✅ Insert or Update Record
        if (exists) {
            conf.updateRecord(
                "UPDATE tbl_dentists SET work_start = ?, work_end = ?, work_days = ? WHERE dentist_id = ?",
                start, end, days, dentistId
            );
            System.out.println("\n✅ Work schedule updated successfully!");
        } else {
            conf.updateRecord(
                "INSERT INTO tbl_dentists (dentist_id, work_start, work_end, work_days) VALUES (?, ?, ?, ?)",
                dentistId, start, end, days
            );
            System.out.println("\n✅ Work schedule created successfully!");
        }

    } catch (Exception e) {
        System.out.println("❌ Error updating schedule: " + e.getMessage());
    }
}


    // ✅ View All Dentist Schedules
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
