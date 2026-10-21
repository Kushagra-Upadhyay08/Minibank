public class Practical5_MiniBank {

    static int accountCounter = 0;

    static abstract class Account {
        private final String accountNumber;
        private String ownerName;
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

        abstract double interestRate();
        abstract boolean canWithdraw(long amount);

        boolean deposit(long amount) {
            if (amount <= 0) return false;
            balance += amount;
            return true;
        }

        boolean withdraw(long amount) {
            if (amount <= 0) return false;
            if (canWithdraw(amount)) {
                balance -= amount;
                return true;
            }
            return false;
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }
        boolean isActive() { return active; }

        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }
    }

    static class SavingsAccount extends Account {
        private long minBalance;

        SavingsAccount(String ownerName, long openingBalance, long minBalance) {
            super(ownerName, openingBalance);
            this.minBalance = minBalance;
        }

        double interestRate() {
            return 4.0;
        }

        boolean canWithdraw(long amount) {
            return (getBalance() - amount) >= minBalance;
        }
    }

    static class CurrentAccount extends Account {
        private long overdraftLimit;

        CurrentAccount(String ownerName, long openingBalance, long overdraftLimit) {
            super(ownerName, openingBalance);
            this.overdraftLimit = overdraftLimit;
        }

        double interestRate() {
            return 0.0;
        }

        boolean canWithdraw(long amount) {
            return (getBalance() - amount) >= -overdraftLimit;
        }
    }

    static class FixedDepositAccount extends Account {
        FixedDepositAccount(String ownerName, long openingBalance) {
            super(ownerName, openingBalance);
        }

        double interestRate() {
            return 7.0;
        }

        boolean canWithdraw(long amount) {
            return false;
        }
    }

    public static void main(String[] args) {
        Account[] accounts = new Account[3];
        accounts[0] = new SavingsAccount("Riya", 10000, 1000);
        accounts[1] = new CurrentAccount("Aman", 5000, 2000);
        accounts[2] = new FixedDepositAccount("Priya", 50000);

        System.out.println("=== Polymorphic Interest Rates ===");
        for (int i = 0; i < accounts.length; i++) {
            System.out.println(accounts[i].getOwnerName() + " (" +
                accounts[i].getClass().getSimpleName() + "): " +
                accounts[i].interestRate() + "%");
        }

        System.out.println("\n=== Withdrawal Tests ===");

        boolean result = accounts[0].withdraw(8000);
        System.out.println("Savings withdraw 8000: " + (result ? "Success" : "Failed") +
            " | Balance: " + accounts[0].getBalance());

        result = accounts[0].withdraw(2000);
        System.out.println("Savings withdraw 2000 (would go below minBalance): " +
            (result ? "Success" : "Failed") + " | Balance: " + accounts[0].getBalance());

        result = accounts[1].withdraw(6000);
        System.out.println("Current withdraw 6000 (uses overdraft): " +
            (result ? "Success" : "Failed") + " | Balance: " + accounts[1].getBalance());

        result = accounts[2].withdraw(1000);
        System.out.println("FD withdraw 1000 (locked): " +
            (result ? "Success" : "Failed") + " | Balance: " + accounts[2].getBalance());

        System.out.println("\n=== Pattern instanceof Check ===");
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] instanceof FixedDepositAccount fd) {
                System.out.println(fd.getOwnerName() + " has a Fixed Deposit with " +
                    fd.interestRate() + "% interest");
            }
        }

        System.out.println("\n=== All Accounts ===");
        for (int i = 0; i < accounts.length; i++) {
            System.out.println(accounts[i]);
        }
    }
}
