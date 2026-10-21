import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class Practical11_MiniBank {

    static int accountCounter = 0;

    static class Account implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String accountNumber;
        private String ownerName;
        private long balance;

        Account(String ownerName, long openingBalance) {
            accountCounter++;
            this.accountNumber = String.format("AC%04d", accountCounter);
            this.ownerName = ownerName;
            this.balance = openingBalance;
        }

        void deposit(long amount) {
            if (amount > 0) balance += amount;
        }

        void withdraw(long amount) {
            if (amount > 0 && balance >= amount) balance -= amount;
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }
    }

    static class StatePersister {
        static void save(Account[] accounts, Path file) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
                oos.writeObject(accounts);
                System.out.println("Saved " + accounts.length + " accounts to " + file);
            } catch (Exception e) {
                System.out.println("Error saving: " + e.getMessage());
            }
        }

        static Account[] load(Path file) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
                Account[] accounts = (Account[]) ois.readObject();
                System.out.println("Loaded " + accounts.length + " accounts from " + file);
                return accounts;
            } catch (Exception e) {
                System.out.println("Error loading: " + e.getMessage());
                return new Account[0];
            }
        }
    }

    static class TransactionLog {
        static void append(Path file, String line) {
            try {
                Files.writeString(file, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                System.out.println("Error appending to log: " + e.getMessage());
            }
        }
    }

    static class ReportGenerator {
        static void generateReport(Path logsFolder, Path reportFile) {
            long totalDeposits = 0;
            long totalWithdrawals = 0;
            int fileCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsFolder, "*.log")) {
                for (Path logFile : stream) {
                    fileCount++;
                    BasicFileAttributes attrs = Files.readAttributes(logFile, BasicFileAttributes.class);
                    System.out.println("Processing: " + logFile.getFileName() +
                        " | Size: " + attrs.size() + " bytes" +
                        " | Modified: " + attrs.lastModifiedTime());

                    try (BufferedReader reader = Files.newBufferedReader(logFile)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(" ");
                            if (parts.length >= 3) {
                                long amount = Long.parseLong(parts[2]);
                                if (parts[0].equals("DEPOSIT")) {
                                    totalDeposits += amount;
                                } else if (parts[0].equals("WITHDRAW")) {
                                    totalWithdrawals += amount;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading logs: " + e.getMessage());
            }

            StringBuilder report = new StringBuilder();
            report.append("========================================\n");
            report.append("         END-OF-DAY REPORT\n");
            report.append("========================================\n");
            report.append("Files processed  : ").append(fileCount).append("\n");
            report.append("Total Deposits   : Rs. ").append(totalDeposits).append("\n");
            report.append("Total Withdrawals: Rs. ").append(totalWithdrawals).append("\n");
            report.append("Net Change       : Rs. ").append(totalDeposits - totalWithdrawals).append("\n");
            report.append("========================================\n");

            try {
                Files.writeString(reportFile, report.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("\nReport written to: " + reportFile);
            } catch (Exception e) {
                System.out.println("Error writing report: " + e.getMessage());
            }

            System.out.println(report);
        }
    }

    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get("data");
        Path logsDir = Paths.get("data", "logs");
        Files.createDirectories(logsDir);

        System.out.println("=== Creating and Saving Accounts ===");
        accountCounter = 0;
        Account[] accounts = new Account[3];
        accounts[0] = new Account("Riya", 5000);
        accounts[1] = new Account("Aman", 3000);
        accounts[2] = new Account("Priya", 8000);

        for (int i = 0; i < accounts.length; i++) {
            System.out.println(accounts[i]);
        }

        Path saveFile = dataDir.resolve("accounts.dat");
        StatePersister.save(accounts, saveFile);

        System.out.println("\n=== Loading Accounts ===");
        Account[] loaded = StatePersister.load(saveFile);
        for (int i = 0; i < loaded.length; i++) {
            System.out.println(loaded[i]);
        }

        System.out.println("\n=== Creating Transaction Logs ===");
        Path logFile = logsDir.resolve("transactions.log");
        Files.deleteIfExists(logFile);

        TransactionLog.append(logFile, "DEPOSIT AC0001 500");
        TransactionLog.append(logFile, "DEPOSIT AC0002 1000");
        TransactionLog.append(logFile, "WITHDRAW AC0001 200");
        TransactionLog.append(logFile, "DEPOSIT AC0003 300");
        TransactionLog.append(logFile, "WITHDRAW AC0002 150");

        System.out.println("\n=== Generating Report ===");
        Path reportFile = dataDir.resolve("report.txt");
        ReportGenerator.generateReport(logsDir, reportFile);
    }
}
