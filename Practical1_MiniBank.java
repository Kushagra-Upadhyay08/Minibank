import java.util.Scanner;

enum MenuOption {
    OPEN_ACCOUNT, DEPOSIT, WITHDRAW, TRANSFER, EXIT
}

record BankInfo(String name, String branch) {}

public class Practical1_MiniBank {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BankInfo bank = new BankInfo("MiniBank", "Main Branch");

        System.out.println("========================================");
        System.out.println("  Welcome to " + bank.name());
        System.out.println("  Branch: " + bank.branch());
        System.out.println("========================================");

        boolean running = true;

        while (running) {
            System.out.println("\n--- Menu ---");
            MenuOption[] options = MenuOption.values();
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            System.out.print("Enter your choice: ");

            int choice = -1;
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
            } else {
                scanner.next();
            }

            if (choice < 1 || choice > options.length) {
                System.out.println("Invalid choice. Please try again.");
                continue;
            }

            MenuOption selected = options[choice - 1];

            String message = switch (selected) {
                case OPEN_ACCOUNT -> "Open Account — to be implemented in a later lab";
                case DEPOSIT -> "Deposit — to be implemented in a later lab";
                case WITHDRAW -> "Withdraw — to be implemented in a later lab";
                case TRANSFER -> "Transfer — to be implemented in a later lab";
                case EXIT -> "Goodbye! Thank you for using MiniBank.";
            };

            System.out.println(message);

            if (selected == MenuOption.EXIT) {
                running = false;
            }
        }

        scanner.close();
    }
}
