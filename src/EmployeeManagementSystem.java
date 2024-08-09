import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Scanner;

public class EmployeeManagementSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/employee_database";
    private static final String USER = "root";
    private static final String PASS = "*********";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("1. Add Employee");
                System.out.println("2. Add Manager");
                System.out.println("3. Update Employee");
                System.out.println("4. Delete Employee");
                System.out.println("5. Download Employee Data");
                System.out.println("6. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        addEmployee(conn, scanner);
                        break;
                    case 2:
                        addManager(conn,scanner);
                        break;
                    case 3:
                        updateEmployee(conn, scanner);
                        break;
                    case 4:
                        deleteEmployee(conn, scanner);
                        break;
                    case 5:
                        downloadData(conn, scanner);
                        break;
                    case 6:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid option!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addEmployee(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter employee name: ");
        String name = scanner.nextLine();
        System.out.print("Enter employee position: ");
        String position = scanner.nextLine();
        System.out.print("Enter employee salary: ");
        double salary = scanner.nextDouble();
        scanner.nextLine(); // consume newline

        Employee employee = new Employee(0, name, position, salary);
        String sql = "INSERT INTO Employees (name, position, salary) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getPosition());
            pstmt.setDouble(3, employee.getSalary());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                employee.setId(rs.getInt(1));
            }
            System.out.println("Employee "+ employee+" added successfully" );
        }
    }
    private static void addManager(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter manager username: ");
        String username = scanner.nextLine();
        System.out.print("Enter manager password: ");
        String password = scanner.nextLine();

        String hashedPassword = hashPassword(password);

        String sql = "INSERT INTO Manager (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            System.out.println("Manager added successfully!");
        }
    }
    private static void updateEmployee(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter employee ID to update: ");
        int id = scanner.nextInt();
        scanner.nextLine(); // consume newline
        System.out.print("Enter new name: ");
        String name = scanner.nextLine();
        System.out.print("Enter new position: ");
        String position = scanner.nextLine();
        System.out.print("Enter new salary: ");
        double salary = scanner.nextDouble();
        scanner.nextLine(); // consume newline

        Employee employee = new Employee(id, name, position, salary);
        String sql = "UPDATE Employees SET name = ?, position = ?, salary = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getPosition());
            pstmt.setDouble(3, employee.getSalary());
            pstmt.setInt(4, employee.getId());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Employee updated successfully: " + employee);
            } else {
                System.out.println("Employee not found!");
            }
        }
    }

    private static void deleteEmployee(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter employee ID to delete: ");
        int id = scanner.nextInt();
        scanner.nextLine(); // consume newline

        String sql = "DELETE FROM Employees WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Employee deleted successfully!");
            } else {
                System.out.println("Employee not found!");
            }
        }
    }

    private static void downloadData(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter manager username: ");
        String username = scanner.nextLine();
        System.out.print("Enter manager password: ");
        String password = scanner.nextLine();

        if (authenticateManager(conn, username, password)) {
            // Download the employee data as a CSV
            String sql = "SELECT * FROM Employees";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                System.out.println("ID, Name, Position, Salary");
                while (rs.next()) {
                    Employee employee = new Employee(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("position"),
                            rs.getDouble("salary")
                    );
                    System.out.println(employee);
                }
            }

            // Log the download
            Manager manager = getManagerByUsername(conn, username);
            DownloadLog log = new DownloadLog(0, manager, LocalDateTime.now());
            logDownload(conn, log);
            System.out.println("Data downloaded and logged successfully!");
        } else {
            System.out.println("Authentication failed!");
        }
    }

    private static boolean authenticateManager(Connection conn, String username, String password) throws SQLException {
        Manager manager = getManagerByUsername(conn, username);
        if (manager != null) {
            String storedHash = manager.getPasswordHash();
            return storedHash.equals(hashPassword(password));
        }
        return false;
    }

    private static Manager getManagerByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT id, username, password_hash FROM Manager WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Manager(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash")
                );
            }
        }
        return null;
    }

    private static void logDownload(Connection conn, DownloadLog log) throws SQLException {
        String sql = "INSERT INTO DownloadLogs (manager_id, download_time) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, log.getManager().getId());
            pstmt.setTimestamp(2, Timestamp.valueOf(log.getDownloadTime()));
            pstmt.executeUpdate();
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}

