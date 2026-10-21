public class Practical8_MiniBank {

    static class BankException extends Exception {
        BankException(String message) {
            super(message);
        }
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
        InvalidAmountException(String message) {
            super(message);
        }
    }

    static class AuditLog implements AutoCloseable {
        private boolean open;

        AuditLog() {
            this.open = true;
            System.out.println("[AuditLog] Opened");
        }

        void log(String message) {
            if (open) {
                System.out.println("[AuditLog] " + message);
            }
        }

        public void close() {
            open = false;
            System.out.println("[AuditLog] Closed");
        }
    }

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

        void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException("Deposit amount must be positive. Got: " + amount);
            }
            balance += amount;
        }

        void withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be positive. Got: " + amount);
            }
            if (balance < amount) {
                throw new InsufficientFundsException(amount - balance);
            }
            balance -= amount;
        }

        void transfer(Account to, long amount) throws BankException {
            try {
                this.withdraw(amount);
                to.deposit(amount);
                System.out.println("Transfer of " + amount + " from " + this.accountNumber +
                    " to " + to.accountNumber + " successful.");
            } catch (BankException e) {
                throw e;
            } finally {
                System.out.println("[Finally] Transfer attempt completed for " + this.accountNumber);
            }
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }
    }

    public static void main(String[] args) {
        Account a1 = new Account("Riya", 1000);
        Account a2 = new Account("Aman", 500);

        System.out.println("=== Initial State ===");
        System.out.println(a1);
        System.out.println(a2);

        System.out.println("\n=== Test: Valid Deposit ===");
        try {
            a1.deposit(2000);
            System.out.println("Deposit successful. " + a1);
        } catch (InvalidAmountException e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test: Invalid Deposit (negative) ===");
        try {
            a1.deposit(-500);
        } catch (InvalidAmountException e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test: Valid Withdrawal ===");
        try {
            a1.withdraw(1000);
            System.out.println("Withdrawal successful. " + a1);
        } catch (BankException e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test: Insufficient Funds ===");
        try {
            a1.withdraw(5000);
        } catch (InsufficientFundsException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Shortfall: " + e.getShortfall());
        } catch (InvalidAmountException e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Test: Transfer ===");
        try {
            a1.transfer(a2, 500);
            System.out.println(a1);
            System.out.println(a2);
        } catch (BankException e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }

        System.out.println("\n=== Test: try-with-resources (AutoCloseable) ===");
        try (AuditLog log = new AuditLog()) {
            log.log("Performing operations...");
            a1.deposit(100);
            log.log("Deposit done. Balance: " + a1.getBalance());
        } catch (InvalidAmountException e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\n=== Final State ===");
        System.out.println(a1);
        System.out.println(a2);
    }
}
