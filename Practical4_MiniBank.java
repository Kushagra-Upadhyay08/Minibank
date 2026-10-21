import java.util.regex.Pattern;

public class Practical4_MiniBank {

    static int accountCounter = 0;

    static class Account {
        private final String accountNumber;
        private String ownerName;
        private long balance;

        Account(String ownerName, long openingBalance) {
            accountCounter++;
            this.accountNumber = String.format("AC%04d", accountCounter);
            this.ownerName = ownerName;
            this.balance = openingBalance;
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }
    }

    enum TransactionType {
        DEPOSIT, WITHDRAW, TRANSFER
    }

    record Command(TransactionType type, String accountNumber, long amount) {}

    static class Validator {
        private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
        private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

        static boolean isValidMobile(String mobile) {
            return MOBILE_PATTERN.matcher(mobile).matches();
        }

        static boolean isValidEmail(String email) {
            return EMAIL_PATTERN.matcher(email).matches();
        }

        static boolean isValidPan(String pan) {
            return PAN_PATTERN.matcher(pan).matches();
        }

        static boolean isValidIfsc(String ifsc) {
            return IFSC_PATTERN.matcher(ifsc).matches();
        }
    }

    static class CommandParser {
        static Command parse(String line) {
            String[] parts = line.split(" ");
            if (parts.length != 3) {
                System.out.println("Error: Command must have 3 parts.");
                return null;
            }
            TransactionType type = TransactionType.valueOf(parts[0]);
            String accountNumber = parts[1];
            long amount = Long.parseLong(parts[2]);
            return new Command(type, accountNumber, amount);
        }
    }

    static class StatementFormatter {
        static String buildStatement(Account account) {
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
    }

    public static void main(String[] args) {
        System.out.println("=== Validator Tests ===");

        System.out.println("Mobile '9876543210': " + Validator.isValidMobile("9876543210"));
        System.out.println("Mobile '12345': " + Validator.isValidMobile("12345"));

        System.out.println("Email 'abc@xyz.com': " + Validator.isValidEmail("abc@xyz.com"));
        System.out.println("Email 'invalid': " + Validator.isValidEmail("invalid"));

        System.out.println("PAN 'ABCDE1234F': " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("PAN '12345': " + Validator.isValidPan("12345"));

        System.out.println("IFSC 'SBIN0001234': " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("IFSC 'INVALID': " + Validator.isValidIfsc("INVALID"));

        System.out.println("\n=== Command Parser Test ===");
        Command cmd = CommandParser.parse("DEPOSIT AC0001 500");
        if (cmd != null) {
            System.out.println("Type: " + cmd.type());
            System.out.println("Account: " + cmd.accountNumber());
            System.out.println("Amount: " + cmd.amount());
        }

        System.out.println("\n=== Statement Formatter Test ===");
        Account acc = new Account("Riya", 5000);
        System.out.println(StatementFormatter.buildStatement(acc));
    }
}
