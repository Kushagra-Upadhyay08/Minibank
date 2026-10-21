import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class Practical7_MiniBank {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Id {}

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Positive {
        String message() default "must be > 0";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MaxLength {
        int value();
    }

    static int accountCounter = 0;

    static class Account {
        @Id
        private final String accountNumber;
        @MaxLength(50)
        private String ownerName;
        @Positive
        private long balance;
        private boolean active;

        Account(String ownerName, long openingBalance) {
            accountCounter++;
            this.accountNumber = String.format("AC%04d", accountCounter);
            this.ownerName = ownerName;
            this.balance = openingBalance;
            this.active = true;
        }

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | Balance: " + balance;
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        long getBalance() { return balance; }
    }

    static class AnnotationValidator {
        static String[] validate(Object obj) {
            ArrayList<String> errors = new ArrayList<>();
            Field[] fields = obj.getClass().getDeclaredFields();

            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                field.setAccessible(true);

                try {
                    if (field.isAnnotationPresent(Positive.class)) {
                        Positive annotation = field.getAnnotation(Positive.class);
                        Object value = field.get(obj);
                        if (value instanceof Number) {
                            if (((Number) value).doubleValue() <= 0) {
                                errors.add(field.getName() + ": " + annotation.message());
                            }
                        }
                    }

                    if (field.isAnnotationPresent(MaxLength.class)) {
                        MaxLength annotation = field.getAnnotation(MaxLength.class);
                        Object value = field.get(obj);
                        if (value instanceof String) {
                            if (((String) value).length() > annotation.value()) {
                                errors.add(field.getName() + ": length must be <= " + annotation.value());
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    errors.add("Could not access field: " + field.getName());
                }
            }

            String[] result = new String[errors.size()];
            for (int i = 0; i < errors.size(); i++) {
                result[i] = errors.get(i);
            }
            return result;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Valid Account ===");
        Account validAccount = new Account("Riya", 5000);
        System.out.println(validAccount);
        String[] errors = AnnotationValidator.validate(validAccount);
        System.out.println("Errors: " + errors.length);

        System.out.println("\n=== Invalid Account (negative balance) ===");
        Account invalidAccount = new Account("Aman", -100);
        System.out.println(invalidAccount);
        errors = AnnotationValidator.validate(invalidAccount);
        System.out.println("Errors: " + errors.length);
        for (int i = 0; i < errors.length; i++) {
            System.out.println("  - " + errors[i]);
        }

        System.out.println("\n=== @Id Field Detection ===");
        Field[] fields = Account.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isAnnotationPresent(Id.class)) {
                System.out.println("@Id is on field: " + fields[i].getName());
            }
        }

        System.out.println("\n=== @Override Verification ===");
        System.out.println("toString() is overridden: " + validAccount.toString());
    }
}
