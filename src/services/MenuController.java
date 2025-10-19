package services;

import config.config;
import java.util.Map;
import java.util.Scanner;

public class MenuController {

    private final config conf;
    private final Scanner sc;
    private final AppointmentService appointmentService = new AppointmentService();

    public MenuController(config conf, Scanner sc) {
        this.conf = conf;
        this.sc = sc;
    }

    // ===================== Main Menu =====================
    public void start() {
        boolean mainLoop = true;

        while (mainLoop) {
            System.out.println("\n===== WELCOME TO DENTALCARE APPOINTMENT SYSTEM =====");
            System.out.println("== Select Account ==");
            System.out.println("1. Staff / Dentist Portal");
            System.out.println("2. Admin Portal");
            System.out.println("3. Super Admin Portal");
            System.out.println("4. Register Account");
            System.out.println("5. Exit");
            System.out.print("Choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Enter a number.");
                continue;
            }

            switch (choice) {
                case 1: staffPortal(); break;
                case 2: adminPortal(); break;
                case 3: superAdminPortal(); break;
                case 4: RegistrationService.registerAccount(sc, conf); break;
                case 5:
                    mainLoop = false;
                    System.out.println("Exiting system. Goodbye!");
                    break;
                default: System.out.println("❌ Invalid option. Try again."); break;
            }
        }
    }
    // ===================== Staff / Dentist Portal =====================
    private void staffPortal() {
        System.out.println("\n== Login ==");
        Map<String, Object> user = AuthService.authenticate(sc, conf);

        if (user != null) {
            String role = (String) user.get("acc_role");
            switch (role) {
                case "Staff":
                    System.out.println("✅ Welcome, " + user.get("acc_name") + " (Staff)");
                    staffDashboard();
                    break;
                case "Dentist":
                    System.out.println("✅ Welcome, " + user.get("acc_name") + " (Dentist)");
                    dentistDashboard((int) user.get("acc_id"));
                    break;
                default:
                    System.out.println("❌ Access Denied.");
                    break;
            }
        } else {
            System.out.println("❌ Invalid credentials.");
        }
    }

   private void staffDashboard() {
    boolean loop = true;
    while (loop) {
        System.out.println("\n== Staff Dashboard ==");
        System.out.println("1. Manage Patients");
        System.out.println("2. Manage Appointments");
        System.out.println("3. View Dentist Schedules");
        System.out.println("4. Logout");
        System.out.print("Choice: ");

        String choice = sc.nextLine();
        switch (choice) {
            case "1": managePatientsMenu(); break;
            case "2": manageAppointmentsMenu(); break;
            case "3": DentistService.viewAllDentistSchedules(sc, conf); break;
            case "4": loop = false; break;
            default: System.out.println("❌ Invalid option."); break;
        }
    }
}


    // ===================== Manage Patients Menu =====================
    private void managePatientsMenu() {
        boolean loop = true;
        while (loop) {
            System.out.println("\n== Manage Patients ==");
            System.out.println("1. Add Patient");
            System.out.println("2. View Patients");
            System.out.println("3. Delete Patient");
            System.out.println("4. Back");
            System.out.print("Choice: ");

            String choice = sc.nextLine();
            switch (choice) {
                case "1": PatientService.addPatient(sc, conf); break;
                case "2": PatientService.viewPatients(sc, conf); break;
                case "3": PatientService.deletePatient(sc, conf); break;
                case "4": loop = false; break;
                default: System.out.println("❌ Invalid option."); break;
            }
        }
    }

    // ===================== Manage Appointments Menu =====================
    private void manageAppointmentsMenu() {
        boolean loop = true;
        while (loop) {
            System.out.println("\n== Manage Appointments ==");
            System.out.println("1. Schedule Appointment");
            System.out.println("2. View Scheduled Appointments");
            System.out.println("3. View All Appointments");
            System.out.println("4. Cancel Appointment");
            System.out.println("5. Back");
            System.out.print("Choice: ");

            String choice = sc.nextLine();
            switch (choice) {
                case "1": appointmentService.scheduleAppointment(conf, sc); break;
                case "2": appointmentService.viewScheduledAppointments(conf, sc); break;
                case "3": appointmentService.viewAllAppointments(conf, sc); break;
                case "4": appointmentService.cancelAppointment(conf, sc); break;
                case "5": loop = false; break;
                default: System.out.println("❌ Invalid option."); break;
            }
        }
    }
// ===================== Dentist Menu =====================
private void dentistDashboard(int dentistId) {
    boolean loop = true;
    while (loop) {
        Map<String, Object> dentistInfo = conf.fetchSingleRecord(
            "SELECT acc_name, acc_email, acc_contact FROM tbl_accounts WHERE acc_id = ?", dentistId);

        System.out.println("\n== Dentist Dashboard ==");
        System.out.println("Name        : " + dentistInfo.get("acc_name"));
        System.out.println("Email       : " + dentistInfo.get("acc_email"));
        System.out.println("Contact     : " + dentistInfo.get("acc_contact"));
        System.out.println("\n1. View My Appointments");
        System.out.println("2. Complete Appointment");
        System.out.println("3. View Completed Appointments");
	System.out.println("4. Update Schedule");
        System.out.println("5. Logout");
        System.out.print("Choice: ");

        String choice = sc.nextLine().trim();
        switch (choice) {
           case "1":
    appointmentService.viewDentistAppointments(conf, sc, dentistId);
    break;
     case "2":
    appointmentService.completeAppointment(conf, sc, dentistId);
    break;
    case "3":
    appointmentService.viewCompletedAppointments(conf, sc, dentistId); // ✅ Corrected
    break;
    case "4":
	DentistService.updateDentistSchedule(sc, conf, dentistId);
	break;
    case "5":
    loop = false;
    break;
    default:
    System.out.println("❌ Invalid option.");

        }
    }
}




      // ===================== Admin Portal =====================
    private void adminPortal() {
        new AdminPortal(sc, conf).run();
    }

    // ===================== Super Admin Portal =====================
    private void superAdminPortal() {
        new SuperAdminPortal(sc, conf).run();
    }
}