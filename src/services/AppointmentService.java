package services;

import config.config;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentService {

    private int updated;

    // ===================== Date/Time Helpers =====================
    private String convertDate(String input) throws ParseException {
        SimpleDateFormat inFmt = new SimpleDateFormat("MM/dd/yy");
        SimpleDateFormat outFmt = new SimpleDateFormat("yyyy-MM-dd");
        return outFmt.format(inFmt.parse(input));
    }

    private String convertTime(String input) throws ParseException {
        SimpleDateFormat inFmt = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        SimpleDateFormat outFmt = new SimpleDateFormat("HH:mm");
        return outFmt.format(inFmt.parse(input));
    }

public void scheduleAppointment(config conf, Scanner sc) {
    try {
        System.out.println("\n==================================================");
        System.out.println("         🗓️ SCHEDULE NEW APPOINTMENT");
        System.out.println("==================================================");

        // Step 1: Show available patients
        List<Map<String, Object>> patients = conf.fetchRecords(
            "SELECT pat_id, pat_name FROM tbl_patients ORDER BY pat_name"
        );
        if (patients == null || patients.isEmpty()) {
            System.out.println("No patients found.");
            return;
        }

        System.out.println("\nAvailable Patients:");
        System.out.printf("%-5s %-25s%n", "ID", "Name");
        System.out.println("-------------------------------");
        for (Map<String, Object> p : patients) {
            System.out.printf("%-5s %-25s%n", p.get("pat_id"), p.get("pat_name"));
        }
        System.out.println("-------------------------------");
        System.out.print("Enter Patient ID from the list above: ");
        int patientId = Integer.parseInt(sc.nextLine().trim());

        // Step 2: Show approved dentists
        List<Map<String, Object>> dentists = conf.fetchRecords(
            "SELECT a.acc_id, a.acc_name, d.specialty FROM tbl_accounts a " +
            "LEFT JOIN tbl_dentists d ON a.acc_id = d.dentist_id " +
            "WHERE a.acc_role = 'Dentist' AND a.acc_status = 'Approved' " +
            "ORDER BY a.acc_name"
        );
        if (dentists == null || dentists.isEmpty()) {
            System.out.println("⚠️ No approved dentists available.");
            return;
        }

        System.out.println("\nAvailable Dentists:");
        System.out.printf("%-5s %-20s %-25s%n", "ID", "Name", "Specialty");
        System.out.println("--------------------------------------------------");
        for (Map<String, Object> d : dentists) {
            System.out.printf("%-5s %-20s %-25s%n",
                d.get("acc_id"),
                d.get("acc_name"),
                d.get("specialty") != null ? d.get("specialty") : "Not set"
            );
        }
        System.out.println("--------------------------------------------------");
        System.out.print("Enter Dentist ID from the list above: ");
        int dentistId = Integer.parseInt(sc.nextLine().trim());

        // Step 3: Fetch dentist info
        Map<String, Object> dentist = conf.fetchSingleRecord(
            "SELECT specialty, work_start, work_end, work_days FROM tbl_dentists WHERE dentist_id = ?",
            dentistId
        );
        if (dentist == null) {
            System.out.println("❌ Dentist not found.");
            return;
        }

        String specialty = dentist.get("specialty") != null ? dentist.get("specialty").toString() : "General Dentistry";
        String workStartStr = dentist.get("work_start").toString();
        String workEndStr = dentist.get("work_end").toString();
        String workDays = dentist.get("work_days").toString();

        LocalTime workStart = LocalTime.parse(workStartStr);
        LocalTime workEnd = LocalTime.parse(workEndStr);

        System.out.println("Selected Dentist Specialty: " + specialty);
        System.out.println("Working Hours: " + workStart + " - " + workEnd);
        System.out.println("Working Days: " + workDays);

        // Step 4: Enter appointment date
        System.out.print("Enter Appointment Date (YYYY-MM-DD): ");
        String appDate = sc.nextLine().trim();

        LocalDate appointmentDate;
        try {
            appointmentDate = LocalDate.parse(appDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            System.out.println("❌ Invalid date format. Use YYYY-MM-DD.");
            return;
        }

        // Step 5: Check working day
        String dayOfWeek = appointmentDate.getDayOfWeek().toString();
        dayOfWeek = dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase();

        if (!workDays.contains(dayOfWeek)) {
            System.out.println("\n❌ Dentist does not work on " + dayOfWeek + ".");
            System.out.println("Available days: " + workDays);
            return;
        }

        // Step 6: Check existing booked slots
        List<Map<String, Object>> booked = conf.fetchRecords(
            "SELECT app_time FROM tbl_appointments WHERE dentist_id = ? AND app_date = ? AND app_status = 'Scheduled'",
            dentistId, appointmentDate.toString()
        );

        List<String> bookedTimes = new ArrayList<>();
        for (Map<String, Object> b : booked) {
            bookedTimes.add(LocalTime.parse(b.get("app_time").toString()).truncatedTo(ChronoUnit.MINUTES).toString());
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mm a");
        System.out.println("\nAvailable Time Slots:");

        List<LocalTime> availableSlots = new ArrayList<>();
        int index = 1;

        for (LocalTime t = workStart; t.isBefore(workEnd); t = t.plusMinutes(30)) {
            String timeStr = t.truncatedTo(ChronoUnit.MINUTES).toString();
            if (!bookedTimes.contains(timeStr)) {
                availableSlots.add(t);
                System.out.printf("%d. %s%n", index++, t.format(timeFmt));
            }
        }

        if (availableSlots.isEmpty()) {
            System.out.println("❌ No available slots for this day.");
            return;
        }

        // ✅ Step 7: Select appointment time instead of typing
        System.out.print("\nSelect a Time Slot (1-" + availableSlots.size() + "): ");
        int slotChoice;
        try {
            slotChoice = Integer.parseInt(sc.nextLine().trim());
            if (slotChoice < 1 || slotChoice > availableSlots.size()) {
                System.out.println("❌ Invalid selection. Try again.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input. Please enter a number.");
            return;
        }

        LocalTime appointmentTime = availableSlots.get(slotChoice - 1);

        // Step 8: Specialty-based service selection
        Map<String, List<String>> specialtyServices = new HashMap<>();
        specialtyServices.put("General Dentistry", Arrays.asList("Dental Cleaning", "Dental Checkup", "Oral Examination", "Dental Filling"));
        specialtyServices.put("Orthodontics", Arrays.asList("Braces Consultation", "Retainer Checkup", "Orthodontic Adjustment"));
        specialtyServices.put("Endodontics", Arrays.asList("Root Canal Treatment", "Pulp Therapy"));
        specialtyServices.put("Periodontics", Arrays.asList("Gum Treatment", "Deep Cleaning", "Periodontal Maintenance"));
        specialtyServices.put("Pediatric Dentistry", Arrays.asList("Pediatric Checkup", "Fluoride Treatment", "Sealants"));
        specialtyServices.put("Prosthodontics", Arrays.asList("Crown or Bridge", "Dentures", "Dental Implants"));
        specialtyServices.put("Oral Surgery", Arrays.asList("Tooth Extraction", "Wisdom Tooth Removal", "Minor Oral Surgery"));
        specialtyServices.put("Cosmetic Dentistry", Arrays.asList("Teeth Whitening", "Veneers Consultation", "Smile Design"));

        List<String> services = specialtyServices.getOrDefault(specialty, Arrays.asList("Dental Checkup"));

        System.out.println("\nSelect Service for Appointment (" + specialty + "):");
        for (int i = 0; i < services.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, services.get(i));
        }

        System.out.print("Enter choice (1-" + services.size() + "): ");
        int serviceChoice;
        try {
            serviceChoice = Integer.parseInt(sc.nextLine().trim());
            if (serviceChoice < 1 || serviceChoice > services.size()) {
                System.out.println("⚠️ Invalid selection. Please try again.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Invalid input. Please enter a number.");
            return;
        }

        String service = services.get(serviceChoice - 1);

        // Step 9: Insert appointment
        conf.updateRecord(
            "INSERT INTO tbl_appointments (pat_id, dentist_id, app_date, app_time, app_service, app_status) " +
            "VALUES (?, ?, ?, ?, ?, 'Scheduled')",
            patientId, dentistId, appointmentDate.toString(), appointmentTime.toString(), service
        );

        System.out.println("\n✅ Appointment scheduled successfully!");
        System.out.println("📅 Date: " + appointmentDate);
        System.out.println("⏰ Time: " + appointmentTime.format(timeFmt));
        System.out.println("🦷 Service: " + service);

    } catch (Exception e) {
        System.out.println("❌ Error scheduling appointment: " + e.getMessage());
    }
}


   // ===================== View Scheduled Appointments =====================
public void viewScheduledAppointments(config conf, Scanner sc) {
    System.out.println("\n=== 📅 All Scheduled Appointments ===");

    String sql = 
        "SELECT a.app_id, p.pat_name, d.acc_name, a.app_date, a.app_time " +
        "FROM tbl_appointments a " +
        "JOIN tbl_patients p ON a.pat_id = p.pat_id " +
        "JOIN tbl_accounts d ON a.dentist_id = d.acc_id " +
        // Only include appointments with 'Scheduled' status
        "WHERE a.app_status = 'Scheduled' " + 
        // Always order by date and time
        "ORDER BY a.app_date, a.app_time"; 

    List<Map<String, Object>> apps = new ArrayList<>();

    try {
        // --- Execute the query for "View All" ---
        apps = conf.fetchRecords(sql);
        // ------------------------------------------

        if (apps.isEmpty()) {
            System.out.println("❌ No scheduled appointments found.");
            return;
        }

        SimpleDateFormat inFmt = new SimpleDateFormat("HH:mm");
        SimpleDateFormat outFmt = new SimpleDateFormat("h:mm a");

        // Print header
        System.out.printf("%-5s %-12s %-10s %-20s %-20s%n", "ID", "Date", "Time", "Patient", "Dentist");
        
        // Print appointments
        for (Map<String, Object> a : apps) {
            String formattedTime = outFmt.format(inFmt.parse(a.get("app_time").toString()));
            System.out.printf("%-5s %-12s %-10s %-20s %-20s%n",
                a.get("app_id"),
                a.get("app_date"),
                formattedTime,
                a.get("pat_name"),
                a.get("acc_name")
            );
        }

    } catch (Exception e) {
        System.out.println("❌ Error: " + e.getMessage());
    }
}


// ===================== View All Appointments =====================
public void viewAllAppointments(config conf, Scanner sc) {
    List<Map<String, Object>> apps = conf.fetchRecords(
        "SELECT a.app_id, p.pat_name, d.acc_name, a.app_date, a.app_time, a.app_status " +
        "FROM tbl_appointments a " +
        "JOIN tbl_patients p ON a.pat_id = p.pat_id " +
        "JOIN tbl_accounts d ON a.dentist_id = d.acc_id " +
        "ORDER BY a.app_date DESC, a.app_time DESC"
    );

    if (apps.isEmpty()) {
        System.out.println("\n────────────────────────────");
        System.out.println(" APPOINTMENT SUMMARY REPORT");
        System.out.println("────────────────────────────");
        System.out.println("No appointments recorded yet.");
        System.out.println("────────────────────────────");
        return;
    }

    SimpleDateFormat inFmt = new SimpleDateFormat("HH:mm");
    SimpleDateFormat outFmt = new SimpleDateFormat("h:mm a");

    int scheduledCount = 0, completedCount = 0, cancelledCount = 0;
    String currentStatus = "";

    for (Map<String, Object> a : apps) {
        String status = a.get("app_status").toString();

        // Group header when status changes
        if (!status.equalsIgnoreCase(currentStatus)) {
            currentStatus = status;
            System.out.println("\n=== " + currentStatus.toUpperCase() + " APPOINTMENTS ===");
            System.out.printf("%-5s %-12s %-10s %-20s %-20s%n", "ID", "Date", "Time", "Patient", "Dentist");
        }

        String formattedTime;
        try {
            formattedTime = outFmt.format(inFmt.parse(a.get("app_time").toString()));
        } catch (Exception e) {
            formattedTime = a.get("app_time").toString(); // fallback if parsing fails
        }

        System.out.printf("%-5s %-12s %-10s %-20s %-20s%n",
            a.get("app_id"),
            a.get("app_date"),
            formattedTime,
            a.get("pat_name"),
            a.get("acc_name")
        );

        // Count by status
        switch (status.toLowerCase()) {
            case "scheduled": scheduledCount++; break;
            case "completed": completedCount++; break;
            case "cancelled": cancelledCount++; break;
        }
    }

    // === Professional Summary Report ===
    System.out.println("\n────────────────────────────");
    System.out.println(" APPOINTMENT SUMMARY REPORT");
    System.out.println("────────────────────────────");

    int total = scheduledCount + completedCount + cancelledCount;
    System.out.printf("• Total Appointments      : %d%n", total);
    System.out.printf("   - Scheduled            : %d%n", scheduledCount);
    System.out.printf("   - Completed            : %d%n", completedCount);
    System.out.printf("   - Cancelled            : %d%n", cancelledCount);

    if (scheduledCount == 0) {
        System.out.println("\n(No scheduled appointments pending review.)");
    }

    System.out.println("────────────────────────────");
}


    // ===================== Cancel Appointment =====================
   public void cancelAppointment(config conf, Scanner sc) {
    try {
        // === 1. Display all scheduled appointments first ===
        List<Map<String, Object>> appointments = conf.fetchRecords(
            "SELECT a.app_id, p.pat_name, a.app_date, a.app_time, a.app_status " +
            "FROM tbl_appointments a " +
            "JOIN tbl_patients p ON a.pat_id = p.pat_id " +
            "WHERE a.app_status = 'Scheduled'"
        );

        if (appointments.isEmpty()) {
            System.out.println("\n📋 No scheduled appointments found.\n");
            return;
        }

        System.out.println("\n===== Scheduled Appointments =====");
        for (Map<String, Object> row : appointments) {
            System.out.println("ID: " + row.get("app_id")
                + " | Name: " + row.get("pat_name")
                + " | Date: " + row.get("app_date")
                + " | Time: " + row.get("app_time")
                + " | Status: " + row.get("app_status"));
        }
        System.out.println("=================================\n");

        // === 2. Ask user for the appointment to cancel ===
        System.out.print("Enter Appointment ID to cancel: ");
        int appId = Integer.parseInt(sc.nextLine().trim());

        // === 3. Verify the appointment exists and is cancellable ===
        List<Map<String, Object>> app = conf.fetchRecords(
            "SELECT * FROM tbl_appointments WHERE app_id = ? AND app_status = 'Scheduled'", appId
        );

        if (app.isEmpty()) {
            System.out.println("❌ Appointment not found or already cancelled/completed.");
            return;
        }

        // === 4. Confirm cancellation ===
        System.out.print("Are you sure you want to cancel this appointment? (Y/N): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (!confirm.equals("Y")) {
            System.out.println("❎ Cancellation aborted.");
            return;
        }

        // === 5. Update status to 'Cancelled' ===
        conf.updateRecord(
            "UPDATE tbl_appointments SET app_status = 'Cancelled' WHERE app_id = ?", appId
        );

        System.out.println("✅ Appointment cancelled successfully.");

    } catch (Exception e) {
        System.out.println("❌ Error: " + e.getMessage());
    }
}

// ===================== View Upcoming Appointments =====================
public void viewDentistAppointments(config conf, Scanner sc, int dentistId) {
    List<Map<String, Object>> appointments = conf.fetchRecords(
        "SELECT a.app_id, p.pat_name, a.app_date, a.app_time, a.app_service, a.app_status " +
        "FROM tbl_appointments a " +
        "JOIN tbl_patients p ON a.pat_id = p.pat_id " +
        "WHERE a.dentist_id = ? AND a.app_status IN ('Scheduled', 'Confirmed') " +
        "ORDER BY a.app_date, a.app_time",
        dentistId
    );

    if (appointments == null || appointments.isEmpty()) {
        System.out.println("⚠️ No appointments found for your account.");
        return;
    }

    System.out.println("\n== Your Upcoming Appointments ==");
    System.out.printf("%-5s %-20s %-20s %-10s %-25s %-12s%n", "ID", "Patient", "Date", "Time", "Service", "Status");
    System.out.println("----------------------------------------------------------------------------------------");

    for (Map<String, Object> a : appointments) {
        LocalDate date = LocalDate.parse(a.get("app_date").toString());
        LocalTime time = LocalTime.parse(a.get("app_time").toString());

        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)", Locale.ENGLISH));
        String formattedTime = time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));

        System.out.printf("%-5s %-20s %-20s %-10s %-25s %-12s%n",
            a.get("app_id"),
            a.get("pat_name"),
            formattedDate,
            formattedTime,
            a.get("app_service"),
            a.get("app_status")
        );
    }
}

// ===================== Complete Appointment =====================
public void completeAppointment(config conf, Scanner sc, int dentistId) {

    // Show only Scheduled/Confirmed appointments to complete
    List<Map<String, Object>> pending = conf.fetchRecords(
        "SELECT a.app_id, p.pat_name, a.app_service, a.app_date, a.app_time " +
        "FROM tbl_appointments a " +
        "JOIN tbl_patients p ON a.pat_id = p.pat_id " +
        "WHERE a.dentist_id = ? AND a.app_status IN ('Scheduled', 'Confirmed') " +
        "ORDER BY a.app_date, a.app_time",
        dentistId
    );

    if (pending == null || pending.isEmpty()) {
        System.out.println("⚠️ No pending appointments to complete.");
        return;
    }

    System.out.println("\n== Complete an Appointment ==");
    System.out.printf("%-5s %-20s %-20s %-15s %-20s%n", "ID", "Patient", "Service", "Date", "Time");
    for (Map<String, Object> a : pending) {
        System.out.printf("%-5s %-20s %-20s %-15s %-20s%n",
            a.get("app_id"),
            a.get("pat_name"),
            a.get("app_service"),
            a.get("app_date"),
            a.get("app_time")
        );
    }

    System.out.print("\nEnter Appointment ID to mark as Completed: ");
    int appId;
    try {
        appId = Integer.parseInt(sc.nextLine().trim());
    } catch (NumberFormatException e) {
        System.out.println("❌ Invalid input. Please enter a valid Appointment ID.");
        return;
    }

    // Check if appointment exists and belongs to this dentist
    Map<String, Object> appointment = conf.fetchSingleRecord(
        "SELECT app_id FROM tbl_appointments WHERE app_id = ? AND dentist_id = ? AND app_status IN ('Scheduled', 'Confirmed')",
        appId, dentistId
    );

    if (appointment == null) {
        System.out.println("❌ Invalid Appointment ID or it’s not assigned to you.");
        return;
    }

    System.out.print("Enter Notes/Diagnosis: ");
    String notes = sc.nextLine().trim();
    String notesApp = notes.isEmpty() ? "N/A" : notes;

    try {
        // Update appointment to completed
        String updateAppointment = 
            "UPDATE tbl_appointments SET app_status = 'Completed', app_notes = ? WHERE app_id = ? AND dentist_id = ?";
        conf.updateRecord(updateAppointment, notesApp, appId, dentistId);

        // ✅ Alternative validation — check if update actually applied
        Map<String, Object> verify = conf.fetchSingleRecord(
            "SELECT app_status FROM tbl_appointments WHERE app_id = ? AND dentist_id = ?",
            appId, dentistId
        );

        if (verify != null && "Completed".equalsIgnoreCase(verify.get("app_status").toString())) {
            System.out.println("✅ Appointment marked as COMPLETED successfully!");
        } else {
            System.out.println("❌ Failed to mark appointment as completed. Please check the database.");
        }

    } catch (Exception e) {
        System.out.println("❌ Error updating appointment: " + e.getMessage());
    }
}


// ===================== View Completed Appointments =====================
public void viewCompletedAppointments(config conf, Scanner sc, int dentistId) {
    List<Map<String, Object>> completed = conf.fetchRecords(
        "SELECT a.app_id, p.pat_name, a.app_date, a.app_time, a.app_service, a.app_notes " +
        "FROM tbl_appointments a " +
        "JOIN tbl_patients p ON a.pat_id = p.pat_id " +
        "WHERE a.dentist_id = ? AND a.app_status = 'Completed' " +
        "ORDER BY a.app_date DESC, a.app_time DESC",
        dentistId
    );

    if (completed == null || completed.isEmpty()) {
        System.out.println("⚠️ No completed appointments found.");
        return;
    }

    System.out.println("\n== Completed Appointments ==");
    System.out.printf("%-5s %-20s %-25s %-12s %-25s %-30s%n", "ID", "Patient", "Date", "Time", "Service", "Notes");
    System.out.println("--------------------------------------------------------------------------------------------------");

    for (Map<String, Object> a : completed) {
        LocalDate date = LocalDate.parse(a.get("app_date").toString());
        LocalTime time = LocalTime.parse(a.get("app_time").toString());

        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)", Locale.ENGLISH));
        String formattedTime = time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));

        System.out.printf("%-5s %-20s %-25s %-12s %-25s %-30s%n",
            a.get("app_id"),
            a.get("pat_name"),
            formattedDate,
            formattedTime,
            a.get("app_service"),
            a.getOrDefault("app_notes", "N/A")
        );
    }
}
}