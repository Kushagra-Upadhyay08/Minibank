public class Practical9_MiniBank {

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

        void deposit(long amount) {
            if (amount > 0) {
                balance += amount;
            }
        }

        synchronized void depositSync(long amount) {
            if (amount > 0) {
                balance += amount;
            }
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }
    }

    static class AccountWorker implements Runnable {
        private Account account;
        private int times;
        private long amount;
        private boolean useSynchronized;

        AccountWorker(Account account, int times, long amount, boolean useSynchronized) {
            this.account = account;
            this.times = times;
            this.amount = amount;
            this.useSynchronized = useSynchronized;
        }

        public void run() {
            for (int i = 0; i < times; i++) {
                if (useSynchronized) {
                    account.depositSync(amount);
                } else {
                    account.deposit(amount);
                }
            }
            System.out.println(Thread.currentThread().getName() + " finished.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 10;
        int depositsPerThread = 1000;

        System.out.println("=== Without Synchronization (Race Condition) ===");
        accountCounter = 0;
        Account unsafeAccount = new Account("TestUser", 0);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new AccountWorker(unsafeAccount, depositsPerThread, 1, false));
            threads[i].setName("Thread-" + (i + 1));
            threads[i].start();
        }

        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }

        System.out.println("Expected balance: " + (threadCount * depositsPerThread));
        System.out.println("Actual balance (unsafe): " + unsafeAccount.getBalance());
        System.out.println("Race condition detected: " +
            (unsafeAccount.getBalance() != threadCount * depositsPerThread));

        System.out.println("\n=== With Synchronization (Thread Safe) ===");
        accountCounter = 0;
        Account safeAccount = new Account("TestUser", 0);
        Thread[] safeThreads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            safeThreads[i] = new Thread(new AccountWorker(safeAccount, depositsPerThread, 1, true));
            safeThreads[i].setName("SafeThread-" + (i + 1));
            safeThreads[i].start();
        }

        for (int i = 0; i < threadCount; i++) {
            safeThreads[i].join();
        }

        System.out.println("Expected balance: " + (threadCount * depositsPerThread));
        System.out.println("Actual balance (safe): " + safeAccount.getBalance());
        System.out.println("Correct: " + (safeAccount.getBalance() == threadCount * depositsPerThread));

        System.out.println("\n=== Thread Lifecycle ===");
        accountCounter = 0;
        Account demoAccount = new Account("Demo", 0);
        Thread t = new Thread(new AccountWorker(demoAccount, 5, 100, true));
        t.setName("LifecycleThread");
        System.out.println("State before start: " + t.getState());
        t.start();
        System.out.println("State after start: " + t.getState());
        t.join();
        System.out.println("State after join: " + t.getState());
        System.out.println("Final balance: " + demoAccount.getBalance());
    }
}
