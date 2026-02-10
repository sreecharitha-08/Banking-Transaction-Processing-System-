import java.sql.*;
import java.util.Scanner;
import java.security.MessageDigest;

public class BankManagementSystem {

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        int choice;

        do {
            System.out.println("\n===== BANK MANAGEMENT SYSTEM =====");
            System.out.println("1. Create Account");
            System.out.println("2. Check Balance");
            System.out.println("3. Deposit Amount");
            System.out.println("4. Withdraw Amount");
            System.out.println("5. Display Account Details");
            System.out.println("6. Mini Statement");
            System.out.println("7. Close Account");
            System.out.println("8. Exit");
            System.out.print("Enter your choice: ");

            choice = sc.nextInt();

            switch (choice) {
                case 1: createAccount(); break;
                case 2: checkBalance(); break;
                case 3: deposit(); break;
                case 4: withdraw(); break;
                case 5: displayDetails(); break;
                case 6: miniStatement(); break;
                case 7: closeAccount(); break;
                case 8: System.out.println("Thank you!"); break;
                default: System.out.println("Invalid choice!");
            }

        } while (choice != 8);
    }

    // ================= PASSWORD HASHING =================
    static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    // ================= CREATE ACCOUNT =================
    static void createAccount() {
        try (Connection con = DBConnection.getConnection()) {

            sc.nextLine();

            System.out.print("Enter Name: ");
            String name = sc.nextLine();

            String mobile;
            while (true) {
                System.out.print("Enter 10-digit Mobile Number: ");
                mobile = sc.next();
                if (mobile.matches("\\d{10}")) break;
                else System.out.println("Invalid Mobile Number!");
            }

            sc.nextLine();

            System.out.print("Enter Email: ");
            String email = sc.nextLine();

            System.out.print("Set Account Password: ");
            String password = sc.nextLine();

            String hashedPassword = hashPassword(password);

            System.out.print("Enter Address: ");
            String address = sc.nextLine();

            System.out.print("Enter Date: ");
            String date = sc.nextLine();

            System.out.print("Enter Initial Balance: ");
            double balance = sc.nextDouble();

            String sql = "INSERT INTO accounts(name, mobile, email, address, date, balance, password) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, name);
            ps.setString(2, mobile);
            ps.setString(3, email);
            ps.setString(4, address);
            ps.setString(5, date);
            ps.setDouble(6, balance);
            ps.setString(7, hashedPassword);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                System.out.println("Account Created Successfully!");
                System.out.println("Your Account Number is: " + rs.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= AUTHENTICATION =================
    static boolean isAuthenticated(Connection con, int accNo, String password) throws Exception {

        String hashedInput = hashPassword(password);

        PreparedStatement ps = con.prepareStatement(
                "SELECT password FROM accounts WHERE account_number=?");
        ps.setInt(1, accNo);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            if (rs.getString("password").equals(hashedInput)) {
                return true;
            } else {
                System.out.println("Incorrect Password!");
                return false;
            }
        }

        System.out.println("Account Not Found!");
        return false;
    }

    // ================= CHECK BALANCE =================
    static void checkBalance() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();

            sc.nextLine();
            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            if (!isAuthenticated(con, accNo, password)) return;

            PreparedStatement ps = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number=?");
            ps.setInt(1, accNo);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Current Balance: " + rs.getDouble("balance"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DEPOSIT =================
    static void deposit() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();

            sc.nextLine();
            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            if (!isAuthenticated(con, accNo, password)) return;

            System.out.print("Enter Amount: ");
            double amount = sc.nextDouble();

            PreparedStatement update = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_number=?");
            update.setDouble(1, amount);
            update.setInt(2, accNo);

            if (update.executeUpdate() > 0) {

                PreparedStatement txn = con.prepareStatement(
                        "INSERT INTO transactions(account_number, type, amount) VALUES (?, ?, ?)");
                txn.setInt(1, accNo);
                txn.setString(2, "DEPOSIT");
                txn.setDouble(3, amount);
                txn.executeUpdate();

                System.out.println("Deposit Successful!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= WITHDRAW =================
    static void withdraw() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();

            sc.nextLine();
            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            if (!isAuthenticated(con, accNo, password)) return;

            System.out.print("Enter Amount: ");
            double amount = sc.nextDouble();

            PreparedStatement check = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number=?");
            check.setInt(1, accNo);

            ResultSet rs = check.executeQuery();

            if (rs.next() && rs.getDouble("balance") >= amount) {

                PreparedStatement update = con.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE account_number=?");
                update.setDouble(1, amount);
                update.setInt(2, accNo);
                update.executeUpdate();

                PreparedStatement txn = con.prepareStatement(
                        "INSERT INTO transactions(account_number, type, amount) VALUES (?, ?, ?)");
                txn.setInt(1, accNo);
                txn.setString(2, "WITHDRAW");
                txn.setDouble(3, amount);
                txn.executeUpdate();

                System.out.println("Withdrawal Successful!");

            } else {
                System.out.println("Insufficient Balance!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MINI STATEMENT =================
    static void miniStatement() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT type, amount, transaction_date FROM transactions WHERE account_number=?");
            ps.setInt(1, accNo);

            ResultSet rs = ps.executeQuery();

            System.out.println("\n===== MINI STATEMENT =====");

            while (rs.next()) {
                System.out.println(
                        rs.getString("type") + " | " +
                        rs.getDouble("amount") + " | " +
                        rs.getTimestamp("transaction_date"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CLOSE ACCOUNT =================
    static void closeAccount() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();

            sc.nextLine();
            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            if (!isAuthenticated(con, accNo, password)) return;

            PreparedStatement check = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number=?");
            check.setInt(1, accNo);

            ResultSet rs = check.executeQuery();

            if (rs.next() && rs.getDouble("balance") == 0) {

                PreparedStatement delTxn = con.prepareStatement(
                        "DELETE FROM transactions WHERE account_number=?");
                delTxn.setInt(1, accNo);
                delTxn.executeUpdate();

                PreparedStatement delAcc = con.prepareStatement(
                        "DELETE FROM accounts WHERE account_number=?");
                delAcc.setInt(1, accNo);
                delAcc.executeUpdate();

                System.out.println("Account Closed Successfully!");

            } else {
                System.out.println("Balance must be zero to close account.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DISPLAY DETAILS =================
    static void displayDetails() {
        try (Connection con = DBConnection.getConnection()) {

            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT name, email, balance FROM accounts WHERE account_number=?");
            ps.setInt(1, accNo);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("\n===== Account Details =====");
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Balance: " + rs.getDouble("balance"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
