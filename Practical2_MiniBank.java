public class Practical2_MiniBank {

    static long customerCounter = 100;

    static String generateCustomerId() {
        customerCounter++;
        return "CUST" + customerCounter;
    }

    static class Customer {
        private final String customerId;
        private String name;
        private String email;
        private String mobile;

        Customer(String name, String email, String mobile) {
            this.customerId = generateCustomerId();
            this.name = name;
            this.email = email;
            this.mobile = mobile;
        }

        String getCustomerId() { return customerId; }
        String getName() { return name; }
        String getEmail() { return email; }
        String getMobile() { return mobile; }
    }

    static int accountCounter = 0;

    static class Account {
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

        boolean deposit(long amount) {
            if (amount <= 0) {
                return false;
            }
            balance += amount;
            return true;
        }

        boolean withdraw(long amount) {
            if (amount <= 0) {
                return false;
            }
            if (balance >= amount) {
                balance -= amount;
                return true;
            }
            return false;
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }
        boolean isActive() { return active; }
    }

    public static void main(String[] args) {
        Account[] accounts = new Account[3];
        accounts[0] = new Account("Riya", 5000);
        accounts[1] = new Account("Aman", 3000);
        accounts[2] = new Account("Priya");

        System.out.println("=== Initial Balances ===");
        for (int i = 0; i < accounts.length; i++) {
            System.out.println(accounts[i].getAccountNumber() + " | " +
                accounts[i].getOwnerName() + " | Balance: " + accounts[i].getBalance());
        }

        System.out.println("\n=== Performing Operations ===");

        accounts[0].deposit(2000);
        System.out.println(accounts[0].getOwnerName() + " deposited 2000. Balance: " + accounts[0].getBalance());

        boolean success = accounts[0].withdraw(3000);
        System.out.println(accounts[0].getOwnerName() + " withdraw 3000: " + (success ? "Success" : "Failed") +
            ". Balance: " + accounts[0].getBalance());

        success = accounts[0].withdraw(10000);
        System.out.println(accounts[0].getOwnerName() + " withdraw 10000: " + (success ? "Success" : "Failed") +
            ". Balance: " + accounts[0].getBalance());

        accounts[1].deposit(500);
        System.out.println(accounts[1].getOwnerName() + " deposited 500. Balance: " + accounts[1].getBalance());

        accounts[2].deposit(1000);
        System.out.println(accounts[2].getOwnerName() + " deposited 1000. Balance: " + accounts[2].getBalance());

        System.out.println("\n=== Final Balances ===");
        for (int i = 0; i < accounts.length; i++) {
            System.out.println(accounts[i].getAccountNumber() + " | " +
                accounts[i].getOwnerName() + " | Balance: " + accounts[i].getBalance());
        }
    }
}
