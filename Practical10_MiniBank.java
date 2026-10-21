import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Practical10_MiniBank {

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

        synchronized void deposit(long amount) {
            if (amount > 0) {
                balance += amount;
            }
        }

        synchronized void withdraw(long amount) {
            if (amount > 0 && balance >= amount) {
                balance -= amount;
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

    static class TransactionProcessor {
        private ExecutorService pool;

        TransactionProcessor(int threads) {
            this.pool = Executors.newFixedThreadPool(threads);
        }

        void submit(Runnable task) {
            pool.execute(task);
        }

        void stop() throws InterruptedException {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    static class SharedBuffer {
        private String[] buffer;
        private int count;

        SharedBuffer(int capacity) {
            this.buffer = new String[capacity];
            this.count = 0;
        }

        synchronized void produce(String item) throws InterruptedException {
            while (count == buffer.length) {
                wait();
            }
            buffer[count] = item;
            count++;
            System.out.println("Produced: " + item + " | Buffer size: " + count);
            notify();
        }

        synchronized String consume() throws InterruptedException {
            while (count == 0) {
                wait();
            }
            count--;
            String item = buffer[count];
            System.out.println("Consumed: " + item + " | Buffer size: " + count);
            notify();
            return item;
        }
    }

    static void transferWithDeadlockFix(Account a, Account b, long amount) {
        Account first;
        Account second;
        if (a.getAccountNumber().compareTo(b.getAccountNumber()) < 0) {
            first = a;
            second = b;
        } else {
            first = b;
            second = a;
        }

        synchronized (first) {
            synchronized (second) {
                if (a.getBalance() >= amount) {
                    a.withdraw(amount);
                    b.deposit(amount);
                    System.out.println(Thread.currentThread().getName() +
                        ": Transferred " + amount + " from " + a.getAccountNumber() +
                        " to " + b.getAccountNumber());
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Thread Pool - TransactionProcessor ===");
        accountCounter = 0;
        Account poolAccount = new Account("Riya", 0);
        TransactionProcessor processor = new TransactionProcessor(4);

        for (int i = 0; i < 20; i++) {
            processor.submit(() -> {
                poolAccount.deposit(100);
                System.out.println(Thread.currentThread().getName() + " deposited 100");
            });
        }

        processor.stop();
        System.out.println("Final balance after pool: " + poolAccount.getBalance());
        System.out.println("Expected: 2000");

        System.out.println("\n=== Producer-Consumer with wait/notify ===");
        SharedBuffer buffer = new SharedBuffer(3);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.produce("Transaction-" + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.consume();
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        System.out.println("\n=== Deadlock Fix - Consistent Lock Ordering ===");
        accountCounter = 0;
        Account accA = new Account("Alice", 5000);
        Account accB = new Account("Bob", 5000);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transferWithDeadlockFix(accA, accB, 10);
            }
        }, "Thread-AtoB");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transferWithDeadlockFix(accB, accA, 10);
            }
        }, "Thread-BtoA");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Account A: " + accA);
        System.out.println("Account B: " + accB);
        System.out.println("Total (should be 10000): " + (accA.getBalance() + accB.getBalance()));
    }
}
