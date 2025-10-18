package services;

import config.config;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ApprovalService {

    public static void approveAccounts(Scanner sc, config conf, String roleToApprove) {
        // Exclude SuperAdmin accounts from pending approvals
        String qry = "SELECT acc_id, acc_name, acc_email FROM tbl_accounts " +
                     "WHERE acc_role = ? AND acc_status = 'Pending Approval' AND acc_role != 'SuperAdmin'";
        List<Map<String, Object>> pending = conf.fetchRecords(qry, roleToApprove);

        if (pending == null || pending.isEmpty()) {
            System.out.println("\n✅ No " + roleToApprove + " accounts pending approval.");
            return;
        }

        System.out.println("\n== Pending " + roleToApprove + " Accounts ==");
        for (Map<String, Object> acc : pending) {
            System.out.println("ID: " + acc.get("acc_id") +
                               " | Name: " + acc.get("acc_name") +
                               " | Email: " + acc.get("acc_email"));
        }

        System.out.println("\nEnter the ID of the account to process. Type 0 to finish.");

        while (true) {
            System.out.print("Choice (ID): ");
            String input = sc.nextLine().trim();

            int accId;
            try {
                accId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a valid numeric ID.");
                continue;
            }

            if (accId == 0) break; // Finish processing

            // Find account by ID
            Map<String, Object> account = null;
            for (Map<String, Object> acc : pending) {
                if ((Integer) acc.get("acc_id") == accId) {
                    account = acc;
                    break;
                }
            }

            if (account == null) {
                System.out.println(" No pending account found with ID: " + accId);
                continue;
            }

            // Ask action
            System.out.print("Approve, Reject, or Skip '" + account.get("acc_name") + "'? (A/R/S): ");
            String action = sc.nextLine().trim().toUpperCase();

            switch (action) {
                case "A":
                    conf.updateRecord("UPDATE tbl_accounts SET acc_status = 'Approved' WHERE acc_id = ?", accId);
                    System.out.println("" + account.get("acc_name") + " approved.");
                    break;
                case "R":
                    conf.updateRecord("UPDATE tbl_accounts SET acc_status = 'Rejected' WHERE acc_id = ?", accId);
                    System.out.println("" + account.get("acc_name") + " rejected.");
                    break;
                case "S":
                    System.out.println(" Skipped " + account.get("acc_name"));
                    break;
                default:
                    System.out.println("Invalid action. Use A, R, or S.");
                    break;
            }
        }

        System.out.println("\nAll selected " + roleToApprove + " accounts have been processed.");
    }
}