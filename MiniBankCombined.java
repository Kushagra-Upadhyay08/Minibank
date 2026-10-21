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

public class MiniBankCombined {

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

    static class ValidationException extends BankException {
        ValidationException(String message) { super(message); }
    }

    public enum AccountType {
        SAVINGS, CHECKING
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
    }

    public static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String transactionId;
        private final TransactionType type;
        private final long amount;
        private final long timestamp;

        public Transaction(String transactionId, TransactionType type, long amount) {
            this.transactionId = transactionId;
            this.type = type;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }

        public String getTransactionId() { return transactionId; }
        public TransactionType getType() { return type; }
        public long getAmount() { return amount; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%d] %s: %d", timestamp, type, amount);
        }
    }

    public static class Account implements Serializable, Comparable<Account> {
        private static final long serialVersionUID = 1L;

        @Id
        private final String accountNumber;
        private String ownerName;

        @Positive
        private long balance;

        private AccountType accountType;
        private ArrayList<Transaction> transactions;

        public Account(String accountNumber, String ownerName, long initialBalance, AccountType accountType) {
            this.accountNumber = accountNumber;
            this.ownerName = ownerName;
            this.balance = initialBalance;
            this.accountType = accountType;
            this.transactions = new ArrayList<>();
        }

        public String getAccountNumber() { return accountNumber; }
        public String getOwnerName() { return ownerName; }
        public AccountType getAccountType() { return accountType; }

        public synchronized long getBalance() {
            return balance;
        }

        public synchronized void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException("Deposit amount must be positive: " + amount);
            }
            balance += amount;
            transactions.add(new Transaction("TXN-" + System.currentTimeMillis(), TransactionType.DEPOSIT, amount));
        }

        public synchronized void withdraw(long amount) throws InvalidAmountException, InsufficientFundsException {
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be positive: " + amount);
            }
            if (balance < amount) {
                throw new InsufficientFundsException(amount - balance);
            }
            balance -= amount;
            transactions.add(new Transaction("TXN-" + System.currentTimeMillis(), TransactionType.WITHDRAWAL, amount));
        }

        public synchronized void recordTransferIn(long amount) {
            balance += amount;
            transactions.add(new Transaction("TXN-" + System.currentTimeMillis(), TransactionType.TRANSFER_IN, amount));
        }

        public synchronized void recordTransferOut(long amount) {
            balance -= amount;
            transactions.add(new Transaction("TXN-" + System.currentTimeMillis(), TransactionType.TRANSFER_OUT, amount));
        }

        public synchronized ArrayList<Transaction> getTransactions() {
            return new ArrayList<>(transactions);
        }

        @Override
        public int compareTo(Account other) {
            return Long.compare(this.balance, other.balance);
        }

        @Override
        public String toString() {
            return String.format("Account[%s, Owner=%s, Type=%s, Balance=%d]",
                    accountNumber, ownerName, accountType, balance);
        }
    }

    public static class Repository<T> {
        private final HashMap<String, T> storage = new HashMap<>();

        public void save(T entity) {
            try {
                String id = extractId(entity);
                storage.put(id, entity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save entity: " + e.getMessage());
            }
        }

        public T findById(String id) {
            return storage.get(id);
        }

        public ArrayList<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        public boolean exists(String id) {
            return storage.containsKey(id);
        }

        private String extractId(T entity) throws IllegalAccessException {
            Class<?> clazz = entity.getClass();
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        Object val = field.get(entity);
                        return val == null ? null : val.toString();
                    }
                }
                clazz = clazz.getSuperclass();
            }
            throw new IllegalArgumentException("No @Id field found on class " + entity.getClass().getName());
        }
    }

    public static class Validator {
        private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,13}$");
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        public static void validatePhoneNumber(String phone) throws ValidationException {
            if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
                throw new ValidationException("Invalid phone number format: " + phone);
            }
        }

        public static void validateEmail(String email) throws ValidationException {
            if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                throw new ValidationException("Invalid email format: " + email);
            }
        }

        public static void validateAnnotations(Object obj) throws ValidationException {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field f : fields) {
                    if (f.isAnnotationPresent(Positive.class)) {
                        f.setAccessible(true);
                        try {
                            Object val = f.get(obj);
                            if (val instanceof Number) {
                                if (((Number) val).doubleValue() <= 0) {
                                    Positive p = f.getAnnotation(Positive.class);
                                    throw new ValidationException(f.getName() + " " + p.message());
                                }
                            }
                        } catch (IllegalAccessException e) {
                            throw new ValidationException("Validation access error: " + e.getMessage());
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
    }

    public static class TransactionLogger {
        private final Path logDir;

        public TransactionLogger(Path logDir) {
            this.logDir = logDir;
            try {
                if (!Files.exists(logDir)) {
                    Files.createDirectories(logDir);
                }
            } catch (Exception e) {
                System.out.println("Could not create logs directory: " + e.getMessage());
            }
        }

        public void log(String accountNumber, String entry) {
            try {
                Path accountLog = logDir.resolve("account_" + accountNumber + ".log");
                String line = String.format("[%d] %s%n", System.currentTimeMillis(), entry);
                Files.writeString(accountLog, line,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND);
            } catch (Exception e) {
                System.out.println("Log failed: " + e.getMessage());
            }
        }
    }

    public static class ReportGenerator {
        public static void generateReport(Path logsDir, Path reportFile) {
            try {
                if (!Files.exists(logsDir)) {
                    System.out.println("No logs directory found.");
                    return;
                }

                int fileCount = 0;
                long totalBytes = 0;
                StringBuilder sb = new StringBuilder();
                sb.append("=== MiniBank Audit Report ===\n");

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, "*.log")) {
                    for (Path entry : stream) {
                        BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                        fileCount++;
                        totalBytes += attrs.size();
                        sb.append(String.format("File: %s | Size: %d bytes | Created: %s%n",
                                entry.getFileName(), attrs.size(), attrs.creationTime()));
                    }
                }

                sb.append(String.format("Total log files: %d%n", fileCount));
                sb.append(String.format("Total log size: %d bytes%n", totalBytes));

                Files.writeString(reportFile, sb.toString(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                System.out.println(sb.toString());
                System.out.println("Report saved to " + reportFile);
            } catch (Exception e) {
                System.out.println("Report generation error: " + e.getMessage());
            }
        }
    }

    public static class StatePersister {
        public static void save(ArrayList<Account> accounts, Path target) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(target.toFile()))) {
                oos.writeObject(accounts);
            } catch (Exception e) {
                System.out.println("Failed to persist state: " + e.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        public static ArrayList<Account> load(Path source) {
            if (!Files.exists(source)) {
                return new ArrayList<>();
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(source.toFile()))) {
                return (ArrayList<Account>) ois.readObject();
            } catch (Exception e) {
                System.out.println("Failed to load state: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    public static class BankService {
        private final Repository<Account> repository;
        private final TransactionLogger logger;
        private final ExecutorService executor;
        private int accountCounter = 1000;

        public BankService(Repository<Account> repository, TransactionLogger logger) {
            this.repository = repository;
            this.logger = logger;
            this.executor = Executors.newFixedThreadPool(4);
        }

        public synchronized Account openAccount(String name, long initialDeposit) throws ValidationException {
            if (name == null || name.trim().length() < 2) {
                throw new ValidationException("Name must have at least 2 characters.");
            }
            if (initialDeposit <= 0) {
                throw new ValidationException("Initial deposit must be positive.");
            }
            accountCounter++;
            String accNo = "ACC" + accountCounter;
            Account acc = new Account(accNo, name, initialDeposit, AccountType.SAVINGS);
            Validator.validateAnnotations(acc);
            repository.save(acc);
            logger.log(accNo, "OPEN_ACCOUNT: Initial balance " + initialDeposit);
            return acc;
        }

        public Account getAccount(String accountNumber) throws AccountNotFoundException {
            Account acc = repository.findById(accountNumber);
            if (acc == null) {
                throw new AccountNotFoundException(accountNumber);
            }
            return acc;
        }

        public void deposit(String accountNumber, long amount) throws BankException {
            Account acc = getAccount(accountNumber);
            acc.deposit(amount);
            logger.log(accountNumber, "DEPOSIT: " + amount + " | New Balance: " + acc.getBalance());
        }

        public void withdraw(String accountNumber, long amount) throws BankException {
            Account acc = getAccount(accountNumber);
            acc.withdraw(amount);
            logger.log(accountNumber, "WITHDRAW: " + amount + " | New Balance: " + acc.getBalance());
        }

        public void transfer(String fromAccNo, String toAccNo, long amount) throws BankException {
            if (fromAccNo.equals(toAccNo)) {
                throw new BankException("Source and destination accounts must be different.");
            }
            if (amount <= 0) {
                throw new InvalidAmountException("Transfer amount must be positive.");
            }

            Account from = getAccount(fromAccNo);
            Account to = getAccount(toAccNo);

            Account firstLock = from.getAccountNumber().compareTo(to.getAccountNumber()) < 0 ? from : to;
            Account secondLock = firstLock == from ? to : from;

            synchronized (firstLock) {
                synchronized (secondLock) {
                    if (from.getBalance() < amount) {
                        throw new InsufficientFundsException(amount - from.getBalance());
                    }
                    from.recordTransferOut(amount);
                    to.recordTransferIn(amount);
                }
            }

            logger.log(fromAccNo, "TRANSFER_OUT: " + amount + " to " + toAccNo);
            logger.log(toAccNo, "TRANSFER_IN: " + amount + " from " + fromAccNo);
        }

        public ArrayList<Account> listAccounts() {
            ArrayList<Account> all = repository.findAll();
            all.sort(Comparator.naturalOrder());
            return all;
        }

        public String statement(String accountNumber) throws AccountNotFoundException {
            Account acc = getAccount(accountNumber);
            StringBuilder sb = new StringBuilder();
            sb.append("=== Statement for ").append(acc.getAccountNumber()).append(" (").append(acc.getOwnerName()).append(") ===\n");
            sb.append("Current Balance: ").append(acc.getBalance()).append("\n");
            sb.append("Recent Transactions:\n");
            for (Transaction tx : acc.getTransactions()) {
                sb.append("  ").append(tx).append("\n");
            }
            return sb.toString();
        }

        public void shutdown() {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    public record BankInfo(String name, String branch) {}

    public static void main(String[] args) {
        BankInfo info = new BankInfo("MiniBank National", "Main Campus Branch");
        System.out.println("==========================================");
        System.out.println("   " + info.name() + " (" + info.branch() + ")");
        System.out.println("==========================================");

        Path dataDir = Paths.get("minibank_data");
        Path logsDir = dataDir.resolve("logs");
        Path saveFile = dataDir.resolve("accounts.dat");

        Repository<Account> repository = new Repository<>();
        ArrayList<Account> loaded = StatePersister.load(saveFile);
        for (Account a : loaded) {
            repository.save(a);
        }

        TransactionLogger logger = new TransactionLogger(logsDir);
        BankService bankService = new BankService(repository, logger);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n--- MiniBank Menu ---");
            System.out.println("1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Account Statement");
            System.out.println("6. List Accounts (sorted by balance)");
            System.out.println("7. Generate Audit Report");
            System.out.println("8. Save and Exit");
            System.out.print("Enter choice (1-8): ");

            if (!scanner.hasNextInt()) {
                String bad = scanner.nextLine();
                if (bad.equalsIgnoreCase("exit")) {
                    break;
                }
                System.out.println("Invalid input.");
                continue;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

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

        bankService.shutdown();
        scanner.close();
    }
}
