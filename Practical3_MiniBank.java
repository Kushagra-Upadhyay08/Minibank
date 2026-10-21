import java.util.Objects;

public class Practical3_MiniBank {

    static int accountCounter = 0;

    static class Address {
        private String line;
        private String city;
        private String pincode;

        Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        String getLine() { return line; }
        String getCity() { return city; }
        String getPincode() { return pincode; }
    }

    static long customerCounter = 100;

    static class Customer implements Cloneable {
        private final String customerId;
        private String name;
        private String email;
        private String mobile;
        private Address address;

        Customer(String name, String email, String mobile, Address address) {
            customerCounter++;
            this.customerId = "CUST" + customerCounter;
            this.name = name;
            this.email = email;
            this.mobile = mobile;
            this.address = address;
        }

        String getCustomerId() { return customerId; }
        String getName() { return name; }
        String getEmail() { return email; }
        String getMobile() { return mobile; }
        Address getAddress() { return address; }

        public Customer clone() {
            try {
                return (Customer) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

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
            if (amount <= 0) return false;
            balance += amount;
            return true;
        }

        boolean withdraw(long amount) {
            if (amount <= 0) return false;
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

        public String toString() {
            return accountNumber + " | " + ownerName + " | " + balance;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (!(o instanceof Account)) return false;
            Account other = (Account) o;
            return this.accountNumber.equals(other.accountNumber);
        }

        public int hashCode() {
            return Objects.hash(accountNumber);
        }
    }

    public static void main(String[] args) {
        Account a1 = new Account("Riya", 5000);
        a1.deposit(2000);
        a1.withdraw(3000);

        Account a2 = new Account("Aman", 3000);

        System.out.println("=== toString() Demo ===");
        System.out.println(a1);
        System.out.println(a2);

        System.out.println("\n=== equals() Demo ===");
        System.out.println("a1.equals(a2): " + a1.equals(a2));
        System.out.println("a1.equals(a1): " + a1.equals(a1));

        System.out.println("\n=== instanceof Demo ===");
        System.out.println("a1 instanceof Account: " + (a1 instanceof Account));

        System.out.println("\n=== Nested Address Class Demo ===");
        Address addr = new Address("123 Main Street", "Ahmedabad", "380001");
        Customer c1 = new Customer("Riya", "riya@email.com", "9876543210", addr);
        System.out.println("Customer: " + c1.getName());
        System.out.println("Address: " + c1.getAddress().getLine() + ", " +
            c1.getAddress().getCity() + " - " + c1.getAddress().getPincode());

        System.out.println("\n=== clone() Demo ===");
        Customer c2 = c1.clone();
        System.out.println("Original: " + c1.getName() + " (" + c1.getCustomerId() + ")");
        System.out.println("Clone: " + c2.getName() + " (" + c2.getCustomerId() + ")");
    }
}
