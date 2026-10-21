import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ArrayList;

public class Practical13_MiniBank {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Id {}

    static int accountCounter = 0;

    static class Account {
        @Id
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

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }
    }

    static class Customer {
        @Id
        private final String customerId;
        private String name;

        Customer(String customerId, String name) {
            this.customerId = customerId;
            this.name = name;
        }

        @Override
        public String toString() {
            return customerId + " | " + name;
        }
    }

    static class Repository<T> {
        private HashMap<String, T> store;

        Repository() {
            this.store = new HashMap<>();
        }

        T save(T entity) {
            String id = findIdValue(entity);
            if (id != null) {
                store.put(id, entity);
            }
            return entity;
        }

        @SafeVarargs
        final void saveAll(T... items) {
            for (int i = 0; i < items.length; i++) {
                save(items[i]);
            }
        }

        T findById(String id) {
            return store.get(id);
        }

        ArrayList<T> findAll() {
            ArrayList<T> list = new ArrayList<>();
            for (String key : store.keySet()) {
                list.add(store.get(key));
            }
            return list;
        }

        boolean delete(String id) {
            if (store.containsKey(id)) {
                store.remove(id);
                return true;
            }
            return false;
        }

        int count() {
            return store.size();
        }

        private String findIdValue(T entity) {
            Field[] fields = entity.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isAnnotationPresent(Id.class)) {
                    fields[i].setAccessible(true);
                    try {
                        Object value = fields[i].get(entity);
                        if (value != null) {
                            return value.toString();
                        }
                    } catch (IllegalAccessException e) {
                        System.out.println("Error reading @Id field: " + e.getMessage());
                    }
                }
            }
            return null;
        }
    }

    static <N extends Number> double average(N[] values) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i].doubleValue();
        }
        return sum / values.length;
    }

    public static void main(String[] args) {
        accountCounter = 0;

        System.out.println("=== Repository<Account> ===");
        Repository<Account> accountRepo = new Repository<>();

        Account a1 = new Account("Riya", 5000);
        Account a2 = new Account("Aman", 3000);
        Account a3 = new Account("Priya", 8000);

        accountRepo.saveAll(a1, a2, a3);

        System.out.println("Total accounts: " + accountRepo.count());

        System.out.println("\nFind by ID 'AC0002': " + accountRepo.findById("AC0002"));

        System.out.println("\nAll accounts:");
        ArrayList<Account> allAccounts = accountRepo.findAll();
        for (int i = 0; i < allAccounts.size(); i++) {
            System.out.println("  " + allAccounts.get(i));
        }

        System.out.println("\nDelete AC0002: " + accountRepo.delete("AC0002"));
        System.out.println("Total after delete: " + accountRepo.count());

        System.out.println("\n=== Repository<Customer> (same class, different type) ===");
        Repository<Customer> customerRepo = new Repository<>();
        customerRepo.save(new Customer("CUST101", "Riya"));
        customerRepo.save(new Customer("CUST102", "Aman"));

        System.out.println("Total customers: " + customerRepo.count());
        ArrayList<Customer> allCustomers = customerRepo.findAll();
        for (int i = 0; i < allCustomers.size(); i++) {
            System.out.println("  " + allCustomers.get(i));
        }

        System.out.println("\n=== Bounded Type Parameter: average() ===");
        Integer[] intValues = {10, 20, 30, 40, 50};
        System.out.println("Average of integers: " + average(intValues));

        Double[] doubleValues = {1.5, 2.5, 3.5};
        System.out.println("Average of doubles: " + average(doubleValues));
    }
}
