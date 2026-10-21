public class Practical6_MiniBank {

    static int accountCounter = 0;

    interface Transactable {
        void deposit(long amount);
        boolean withdraw(long amount);
    }

    interface InterestBearing {
        double interestRate();
        default double yearlyInterest(long balance) {
            return balance * interestRate() / 100.0;
        }
    }

    @FunctionalInterface
    interface WithdrawRule {
        boolean allow(Account account, long amount);
    }

    interface Premium {}

    static abstract class Account implements Transactable, InterestBearing {
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

        abstract boolean canWithdraw(long amount);

        public void deposit(long amount) {
            if (amount > 0) {
                balance += amount;
            }
        }

        public boolean withdraw(long amount) {
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

        public double interestRate() { return 4.0; }

        boolean canWithdraw(long amount) {
            return (getBalance() - amount) >= minBalance;
        }
    }

    static class CurrentAccount extends Account implements Premium {
        private long overdraftLimit;

        CurrentAccount(String ownerName, long openingBalance, long overdraftLimit) {
            super(ownerName, openingBalance);
            this.overdraftLimit = overdraftLimit;
        }

        public double interestRate() { return 0.0; }

        boolean canWithdraw(long amount) {
            return (getBalance() - amount) >= -overdraftLimit;
        }
    }

    static class FixedDepositAccount extends Account {
        FixedDepositAccount(String ownerName, long openingBalance) {
            super(ownerName, openingBalance);
        }

        public double interestRate() { return 7.0; }

        boolean canWithdraw(long amount) {
            return false;
        }
    }

    public static void main(String[] args) {
        SavingsAccount savings = new SavingsAccount("Riya", 10000, 1000);
        CurrentAccount current = new CurrentAccount("Aman", 5000, 2000);
        FixedDepositAccount fd = new FixedDepositAccount("Priya", 50000);

        System.out.println("=== Transactable Interface Demo ===");
        Transactable t = savings;
        t.deposit(2000);
        System.out.println("Savings after deposit 2000: " + savings.getBalance());

        System.out.println("\n=== InterestBearing Default Method ===");
        System.out.println("Savings yearly interest: " + savings.yearlyInterest(savings.getBalance()));
        System.out.println("FD yearly interest: " + fd.yearlyInterest(fd.getBalance()));

        System.out.println("\n=== WithdrawRule - Anonymous Class ===");
        WithdrawRule rule1 = new WithdrawRule() {
            public boolean allow(Account account, long amount) {
                return account.getBalance() >= amount;
            }
        };
        System.out.println("Rule1 allows withdraw 5000 from savings: " + rule1.allow(savings, 5000));

        System.out.println("\n=== WithdrawRule - Lambda ===");
        WithdrawRule rule2 = (account, amount) -> account.getBalance() >= amount * 2;
        System.out.println("Rule2 allows withdraw 5000 from savings (needs 2x): " + rule2.allow(savings, 5000));

        System.out.println("\n=== Marker Interface (Premium) ===");
        Account[] accounts = { savings, current, fd };
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] instanceof Premium) {
                System.out.println(accounts[i].getOwnerName() + " is a Premium account holder");
            }
        }

        System.out.println("\n=== All Accounts ===");
        for (int i = 0; i < accounts.length; i++) {
            System.out.println(accounts[i]);
        }
    }
}
