import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Practical14_MiniBank {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Id {}

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Positive {
        String message() default "must be > 0";
    }

    static class BankException extends Exception {
        BankException(String message) { super(message); }
    }

    static class InsufficientFundsException extends BankException {
        private long shortfall;
        InsufficientFundsException(long shortfall) {
            super("Insufficient funds. Short by: " + shortfall);
            this.shortfall = shortfall;
        }
        long getShortfall() { return shortfall; }
    }

    static class AccountNotFoundException extends BankException {
        AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }

    static class InvalidAmountException extends BankException {
        InvalidAmountException(String message) { super(message); }
    }

    static int accountCounter = 0;

    static class Account implements Serializable, Comparable<Account> {
        private static final long serialVersionUID = 1L;

        @Id
        private final String accountNumber;
        private String ownerName;
        @Positive
        private long balance;
        private boolean active;

        Account(String ownerName, long openingBalance) {
            accountCounter++;
            this.accountNumber = String.format("AC%04d", accountCounter);
            this.ownerName = ownerName;
            this.balance = openingBalance;
            this.active = true;
        }

        Account(String ownerName) {
            this(ownerName, 0);
        }

        synchronized void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) throw new InvalidAmountException("Deposit amount must be positive.");
            balance += amount;
        }

        synchronized void withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
            if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be positive.");
            if (balance < amount) throw new InsufficientFundsException(amount - balance);
            balance -= amount;
        }

        void transfer(Account to, long amount) throws BankException {
            Account first;
            Account second;
            if (this.accountNumber.compareTo(to.accountNumber) < 0) {
                first = this; second = to;
            } else {
                first = to; second = this;
            }
            synchronized (first) {
                synchronized (second) {
                    this.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }
        boolean isActive() { return active; }

        @Override
        public int compareTo(Account other) {
            return this.accountNumber.compareTo(other.accountNumber);
        }

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Account)) return false;
            return this.accountNumber.equals(((Account) o).accountNumber);
        }

        @Override
        public int hashCode() {
            return accountNumber.hashCode();
        }
    }

    static class Repository<T> {
        private HashMap<String, T> store;

        Repository() { this.store = new HashMap<>(); }

        T save(T entity) {
            String id = findIdValue(entity);
            if (id != null) store.put(id, entity);
            return entity;
        }

        T findById(String id) { return store.get(id); }

        ArrayList<T> findAll() {
            return new ArrayList<>(store.values());
        }

        boolean delete(String id) {
            return store.remove(id) != null;
        }

        int count() { return store.size(); }

        private String findIdValue(T entity) {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isAnnotationPresent(Id.class)) {
                    fields[i].setAccessible(true);
                    try {
                        Object value = fields[i].get(entity);
                        return value != null ? value.toString() : null;
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    static class Validator {
        private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

        static boolean isValidMobile(String mobile) { return MOBILE_PATTERN.matcher(mobile).matches(); }
        static boolean isValidEmail(String email) { return EMAIL_PATTERN.matcher(email).matches(); }
    }

    static class TransactionLog {
        static void append(Path file, String line) {
            try {
                Files.writeString(file, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                System.out.println("Log error: " + e.getMessage());
            }
        }
    }

    static class StatePersister {
        static void save(ArrayList<Account> accounts, Path file) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
                Account[] arr = new Account[accounts.size()];
                for (int i = 0; i < accounts.size(); i++) arr[i] = accounts.get(i);
                oos.writeObject(arr);
            } catch (Exception e) {
                System.out.println("Save error: " + e.getMessage());
            }
        }

        static ArrayList<Account> load(Path file) {
            ArrayList<Account> list = new ArrayList<>();
            if (!Files.exists(file)) return list;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
                Account[] arr = (Account[]) ois.readObject();
                for (int i = 0; i < arr.length; i++) list.add(arr[i]);
                int maxCounter = 0;
                for (int i = 0; i < arr.length; i++) {
                    String num = arr[i].getAccountNumber().replace("AC", "");
                    int n = Integer.parseInt(num);
                    if (n > maxCounter) maxCounter = n;
                }
                accountCounter = maxCounter;
            } catch (Exception e) {
                System.out.println("Load error: " + e.getMessage());
            }
            return list;
        }
    }

    static class ReportGenerator {
        static void generateReport(Path logsFolder, Path reportFile) {
            long totalDeposits = 0;
            long totalWithdrawals = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsFolder, "*.log")) {
                for (Path logFile : stream) {
                    java.io.BufferedReader reader = Files.newBufferedReader(logFile);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 3) {
                            long amount = Long.parseLong(parts[2]);
                            if (parts[0].equals("DEPOSIT")) totalDeposits += amount;
                            else if (parts[0].equals("WITHDRAW")) totalWithdrawals += amount;
                        }
                    }
                    reader.close();
                }
            } catch (Exception e) {
                System.out.println("Report error: " + e.getMessage());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("         END-OF-DAY REPORT\n");
            sb.append("========================================\n");
            sb.append("Total Deposits   : Rs. ").append(totalDeposits).append("\n");
            sb.append("Total Withdrawals: Rs. ").append(totalWithdrawals).append("\n");
            sb.append("Net Change       : Rs. ").append(totalDeposits - totalWithdrawals).append("\n");
            sb.append("========================================\n");

            try {
                Files.writeString(reportFile, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception e) {
                System.out.println("Write report error: " + e.getMessage());
            }

            System.out.println(sb);
        }
    }

    static class TransactionProcessor {
        private ExecutorService pool;

        TransactionProcessor(int threads) {
            pool = Executors.newFixedThreadPool(threads);
        }

        void submit(Runnable task) { pool.execute(task); }

        void stop() throws InterruptedException {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    static class BankService {
        private Repository<Account> repository;
        private Path logFile;

        BankService(Repository<Account> repository, Path logFile) {
            this.repository = repository;
            this.logFile = logFile;
        }

        Account openAccount(String ownerName, long openingBalance) throws InvalidAmountException {
            if (openingBalance < 0) throw new InvalidAmountException("Opening balance cannot be negative.");
            Account account = new Account(ownerName, openingBalance);
            repository.save(account);
            TransactionLog.append(logFile, "OPEN " + account.getAccountNumber() + " " + openingBalance);
            return account;
        }

        void deposit(String accountNumber, long amount) throws BankException {
            Account account = repository.findById(accountNumber);
            if (account == null) throw new AccountNotFoundException(accountNumber);
            account.deposit(amount);
            repository.save(account);
            TransactionLog.append(logFile, "DEPOSIT " + accountNumber + " " + amount);
        }

        void withdraw(String accountNumber, long amount) throws BankException {
            Account account = repository.findById(accountNumber);
            if (account == null) throw new AccountNotFoundException(accountNumber);
            account.withdraw(amount);
            repository.save(account);
            TransactionLog.append(logFile, "WITHDRAW " + accountNumber + " " + amount);
        }

        void transfer(String fromAcc, String toAcc, long amount) throws BankException {
            Account from = repository.findById(fromAcc);
            Account to = repository.findById(toAcc);
            if (from == null) throw new AccountNotFoundException(fromAcc);
            if (to == null) throw new AccountNotFoundException(toAcc);
            from.transfer(to, amount);
            repository.save(from);
            repository.save(to);
            TransactionLog.append(logFile, "TRANSFER " + fromAcc + " " + toAcc + " " + amount);
        }

        String statement(String accountNumber) throws AccountNotFoundException {
            Account account = repository.findById(accountNumber);
            if (account == null) throw new AccountNotFoundException(accountNumber);
            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("         ACCOUNT STATEMENT\n");
            sb.append("========================================\n");
            sb.append("Account Number : ").append(account.getAccountNumber()).append("\n");
            sb.append("Owner Name     : ").append(account.getOwnerName()).append("\n");
            sb.append("Balance        : Rs. ").append(account.getBalance()).append("\n");
            sb.append("========================================");
            return sb.toString();
        }

        ArrayList<Account> listAccounts() {
            ArrayList<Account> list = repository.findAll();
            list.sort(new Comparator<Account>() {
                public int compare(Account a, Account b) {
                    return Long.compare(b.getBalance(), a.getBalance());
                }
            });
            return list;
        }
    }

    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get("data");
        Path logsDir = Paths.get("data", "logs");
        Files.createDirectories(logsDir);

        Path saveFile = dataDir.resolve("accounts.dat");
        Path logFile = logsDir.resolve("transactions.log");

        Repository<Account> repository = new Repository<>();

        ArrayList<Account> loaded = StatePersister.load(saveFile);
        for (int i = 0; i < loaded.size(); i++) {
            repository.save(loaded.get(i));
        }
        if (loaded.size() > 0) {
            System.out.println("Loaded " + loaded.size() + " accounts from previous session.");
        }

        BankService bankService = new BankService(repository, logFile);
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n========================================");
            System.out.println("            MINIBANK MENU");
            System.out.println("========================================");
            System.out.println("1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Statement");
            System.out.println("6. List All Accounts");
            System.out.println("7. Generate Report");
            System.out.println("8. Exit");
            System.out.print("Enter choice: ");

            int choice = -1;
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
            } else {
                scanner.nextLine();
                System.out.println("Invalid input.");
                continue;
            }

            try {
                switch (choice) {
                    case 1: {
                        System.out.print("Enter owner name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter opening balance: ");
                        long balance = Long.parseLong(scanner.nextLine());
                        Account acc = bankService.openAccount(name, balance);
                        System.out.println("Account created: " + acc);
                        break;
                    }
                    case 2: {
                        System.out.print("Enter account number: ");
                        String accNo = scanner.nextLine();
                        System.out.print("Enter deposit amount: ");
                        long amount = Long.parseLong(scanner.nextLine());
                        bankService.deposit(accNo, amount);
                        System.out.println("Deposit successful.");
                        break;
                    }
                    case 3: {
                        System.out.print("Enter account number: ");
                        String accNo = scanner.nextLine();
                        System.out.print("Enter withdrawal amount: ");
                        long amount = Long.parseLong(scanner.nextLine());
                        bankService.withdraw(accNo, amount);
                        System.out.println("Withdrawal successful.");
                        break;
                    }
                    case 4: {
                        System.out.print("Enter source account number: ");
                        String from = scanner.nextLine();
                        System.out.print("Enter destination account number: ");
                        String to = scanner.nextLine();
                        System.out.print("Enter transfer amount: ");
                        long amount = Long.parseLong(scanner.nextLine());
                        bankService.transfer(from, to, amount);
                        System.out.println("Transfer successful.");
                        break;
                    }
                    case 5: {
                        System.out.print("Enter account number: ");
                        String accNo = scanner.nextLine();
                        System.out.println(bankService.statement(accNo));
                        break;
                    }
                    case 6: {
                        ArrayList<Account> accounts = bankService.listAccounts();
                        if (accounts.size() == 0) {
                            System.out.println("No accounts found.");
                        } else {
                            System.out.println("=== All Accounts (sorted by balance) ===");
                            for (int i = 0; i < accounts.size(); i++) {
                                System.out.println("  " + accounts.get(i));
                            }
                        }
                        break;
                    }
                    case 7: {
                        Path reportFile = dataDir.resolve("report.txt");
                        ReportGenerator.generateReport(logsDir, reportFile);
                        break;
                    }
                    case 8: {
                        ArrayList<Account> allAccounts = repository.findAll();
                        StatePersister.save(allAccounts, saveFile);
                        System.out.println("Accounts saved. Goodbye!");
                        running = false;
                        break;
                    }
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format.");
            }
        }

        scanner.close();
    }
}
