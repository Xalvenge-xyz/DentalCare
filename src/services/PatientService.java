package services;

import config.config;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PatientService {

    // ===================== Add Patient =====================
    public static void addPatient(Scanner sc, config conf) {
	try {
	    System.out.print("Enter Patient Name: ");
	    String name = sc.nextLine().trim();
	    System.out.print("Enter Age: ");
	    int age = Integer.parseInt(sc.nextLine().trim());
	    String sex; 
	    do {
		System.out.print("Enter Sex (M/F): ");
		sex = sc.nextLine().trim(); 
		if (!sex.equalsIgnoreCase("M") && !sex.equalsIgnoreCase("F")) {
		    System.out.println("❌ Invalid input. Please enter 'M' or 'F' only.");
		}
	    } while (!sex.equalsIgnoreCase("M") && !sex.equalsIgnoreCase("F"));
	    System.out.print("Enter Contact Number: ");
	    String contact = sc.nextLine().trim();
	    System.out.print("Enter Address: ");
	    String address = sc.nextLine().trim();
	    conf.updateRecord(
		"INSERT INTO tbl_patients (pat_name, pat_age, pat_sex, pat_contact, pat_address) VALUES (?, ?, ?, ?, ?)",
		name, age, sex, contact, address
	    );
	    System.out.println("✅ Patient added successfully.");
	} catch (NumberFormatException e) {
	    System.out.println("❌ Error: Age must be a valid number.");
	} 
	catch (Exception e) {
	    System.out.println("❌ Error adding patient: " + e.getMessage());
	}
    }

    // ===================== View Patients =====================
    public static void viewPatients(Scanner sc, config conf) {
        List<Map<String, Object>> patients = conf.fetchRecords(
            "SELECT * FROM tbl_patients ORDER BY pat_name"
        );
        if (patients.isEmpty()) {
            System.out.println("No patients found.");
            return;
        }
        System.out.println("\n== Patient List ==");
        for (Map<String, Object> p : patients) {
            System.out.println(p.get("pat_id") + ". " + p.get("pat_name") +
                               " | Age: " + p.get("pat_age") +
                               " | Sex: " + p.get("pat_sex") +
                               " | Contact: " + p.get("pat_contact") +
                               " | Address: " + p.get("pat_address"));
        }
    }

    // ===================== Update Patient =====================
//    public static void updatePatient(Scanner sc, config conf) {
//        viewPatients(sc, conf);
//        try {
//            System.out.print("\nEnter Patient ID to update: ");
//            int id = Integer.parseInt(sc.nextLine().trim());
//
//            List<Map<String, Object>> patient = conf.fetchRecords(
//                "SELECT * FROM tbl_patients WHERE pat_id = ?", id
//            );
//            if (patient.isEmpty()) {
//                System.out.println("❌ Patient not found.");
//                return;
//            }
//
//            Map<String, Object> p = patient.get(0);
//            System.out.print("Enter New Name (" + p.get("pat_name") + "): ");
//            String name = sc.nextLine().trim();
//            if (name.isEmpty()) name = p.get("pat_name").toString();
//
//            System.out.print("Enter New Age (" + p.get("pat_age") + "): ");
//            String ageStr = sc.nextLine().trim();
//            int age = ageStr.isEmpty() ? Integer.parseInt(p.get("pat_age").toString()) : Integer.parseInt(ageStr);
//
//            System.out.print("Enter New Sex (" + p.get("pat_sex") + "): ");
//            String sex = sc.nextLine().trim();
//            if (sex.isEmpty()) sex = p.get("pat_sex").toString();
//
//            System.out.print("Enter New Contact (" + p.get("pat_contact") + "): ");
//            String contact = sc.nextLine().trim();
//            if (contact.isEmpty()) contact = p.get("pat_contact").toString();
//
//            System.out.print("Enter New Address (" + p.get("pat_address") + "): ");
//            String address = sc.nextLine().trim();
//            if (address.isEmpty()) address = p.get("pat_address").toString();
//
//            conf.updateRecord(
//                "UPDATE tbl_patients SET pat_name = ?, pat_age = ?, pat_sex = ?, pat_contact = ?, pat_address = ? WHERE pat_id = ?",
//                name, age, sex, contact, address, id
//            );
//            System.out.println("✅ Patient updated successfully.");
//        } catch (Exception e) {
//            System.out.println("❌ Error updating patient: " + e.getMessage());
//        }
//    }

    // ===================== Delete Patient =====================
    public static void deletePatient(Scanner sc, config conf) {
        viewPatients(sc, conf);
        try {
            System.out.print("\nEnter Patient ID to delete: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            List<Map<String, Object>> patient = conf.fetchRecords(
                "SELECT * FROM tbl_patients WHERE pat_id = ?", id
            );
            if (patient.isEmpty()) {
                System.out.println("❌ Patient not found.");
                return;
            }

            System.out.print("Are you sure you want to delete " + patient.get(0).get("pat_name") + "? (Y/N): ");
            String confirm = sc.nextLine().trim();
            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("❌ Deletion cancelled.");
                return;
            }

            conf.updateRecord(
                "DELETE FROM tbl_patients WHERE pat_id = ?", id
            );
            System.out.println("✅ Patient deleted successfully.");
        } catch (Exception e) {
            System.out.println("❌ Error deleting patient: " + e.getMessage());
        }
    }
}
