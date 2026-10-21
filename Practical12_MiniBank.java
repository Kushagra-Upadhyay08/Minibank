import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class Practical12_MiniBank {

    static int accountCounter = 0;

    static class Account implements Comparable<Account> {
        private final String accountNumber;
        private String ownerName;
        private long balance;

        Account(String ownerName, long openingBalance) {
            accountCounter++;
            this.accountNumber = String.format("AC%04d", accountCounter);
            this.ownerName = ownerName;
            this.balance = openingBalance;
        }

        synchronized void deposit(long amount) {
            if (amount > 0) balance += amount;
        }

        synchronized void withdraw(long amount) {
            if (amount > 0 && balance >= amount) balance -= amount;
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }

        @Override
        public int compareTo(Account other) {
            return this.accountNumber.compareTo(other.accountNumber);
        }

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }
    }

    static class AccountStore {
        private HashMap<String, Account> accounts;
        private ArrayList<String> transactionHistory;

        AccountStore() {
            this.accounts = new HashMap<>();
            this.transactionHistory = new ArrayList<>();
        }

        void add(Account account) {
            accounts.put(account.getAccountNumber(), account);
            transactionHistory.add("ADDED " + account.getAccountNumber());
        }

        Account get(String accountNumber) {
            return accounts.get(accountNumber);
        }

        ArrayList<Account> getAll() {
            ArrayList<Account> list = new ArrayList<>();
            for (String key : accounts.keySet()) {
                list.add(accounts.get(key));
            }
            return list;
        }

        ArrayList<Account> sortedByBalance() {
            ArrayList<Account> list = getAll();
            list.sort(new Comparator<Account>() {
                public int compare(Account a, Account b) {
                    return Long.compare(b.getBalance(), a.getBalance());
                }
            });
            return list;
        }

        ArrayList<String> getTransactionHistory() {
            return transactionHistory;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        accountCounter = 0;

        System.out.println("=== AccountStore with HashMap ===");
        AccountStore store = new AccountStore();

        Account a1 = new Account("Riya", 5000);
        Account a2 = new Account("Aman", 2000);
        Account a3 = new Account("Priya", 9000);

        store.add(a1);
        store.add(a2);
        store.add(a3);

        System.out.println("All accounts:");
        ArrayList<Account> all = store.getAll();
        for (int i = 0; i < all.size(); i++) {
            System.out.println("  " + all.get(i));
        }

        System.out.println("\nLookup AC0002: " + store.get("AC0002"));

        System.out.println("\n=== Sorted by Balance (Descending) ===");
        ArrayList<Account> sorted = store.sortedByBalance();
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println("  " + sorted.get(i));
        }

        System.out.println("\n=== Comparable (Natural Order) ===");
        ArrayList<Account> natural = store.getAll();
        natural.sort(null);
        for (int i = 0; i < natural.size(); i++) {
            System.out.println("  " + natural.get(i));
        }

        System.out.println("\n=== Transaction History ===");
        ArrayList<String> history = store.getTransactionHistory();
        for (int i = 0; i < history.size(); i++) {
            System.out.println("  " + history.get(i));
        }

        System.out.println("\n=== ConcurrentHashMap (Thread Safe) ===");
        ConcurrentHashMap<String, Account> concurrentStore = new ConcurrentHashMap<>();
        concurrentStore.put(a1.getAccountNumber(), a1);
        concurrentStore.put(a2.getAccountNumber(), a2);
        concurrentStore.put(a3.getAccountNumber(), a3);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    Account acc = concurrentStore.get("AC0001");
                    if (acc != null) {
                        acc.deposit(1);
                    }
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }

        System.out.println("AC0001 balance after concurrent deposits: " + a1.getBalance());
        System.out.println("Expected: " + (5000 + 10 * 100));
    }
}
