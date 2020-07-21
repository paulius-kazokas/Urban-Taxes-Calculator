package meniu;

import config.DatabaseConfig;
import entities.Property;
import entities.User;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import repositories.IndicatorRepository;
import repositories.PropertyRepository;
import repositories.UserRepository;
import security.SecurityUtils;
import utility.InputVadility;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static config.SystemConstants.*;

public class MeniuActions {

    public static void mainMenuActions() {

        SecurityUtils securityUtils = new SecurityUtils();
        UserRepository userRepository = new UserRepository(new DatabaseConfig());
        Scanner scanner = new Scanner(System.in);

        System.out.print("\nChoice: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "0":
                System.exit(0);
            case "1":
                login(scanner, userRepository, securityUtils);
                break;
            case "2":
                register(scanner, userRepository, securityUtils);
                break;
        }
    }

    public static void login(Scanner scanner, UserRepository userRepository, SecurityUtils securityUtils) {

        System.out.print("\nEnter username: ");

        String username = scanner.nextLine();
        if (userRepository.checkIfUserAlreadyExists(username)) {
            int currentTry = 1;
            int maximumRetires = 5;

            while (currentTry < maximumRetires) {
                System.out.print("\nEnter password for " + username + " : ");
                String password = scanner.nextLine();

                String userDbPassword = userRepository.getInformationByUsername(username, "password");
                String decryptedUserDbPassword = securityUtils.decrypt(userDbPassword);

                if (decryptedUserDbPassword.equals(password)) {
                    System.out.print("\n(logged in as '" + username + "')");
                    Meniu.loggedInMenu(new User(
                            username,
                            password,
                            userRepository.getInformationByUsername(username, UTC_USERS_TABLE_NAME),
                            userRepository.getInformationByUsername(username, UTC_USERS_TABLE_LASTNAME),
                            userRepository.getInformationByUsername(username, UTC_USERS_TABLE_EMAIL),
                            userRepository.getInformationByUsername(username, UTC_USERS_TABLE_PERSONAL_CODE))
                    );
                } else {
                    currentTry++;
                    System.out.println("Failed to login , try again (" + (maximumRetires - currentTry) + ")");
                }
            }

            System.out.println("\nFailed to login, returning to main menu ...");
            Meniu.mainMenu();
        } else {
            System.out.print("\nUsername doesn't exist, would you like to register an account? (y)");
            if (scanner.nextLine().equals("y") || scanner.nextLine().equals("Y")) {
                registerFromSignup(scanner, userRepository, username, securityUtils);
            } else {
                Meniu.mainMenu();
            }
        }
    }

    public static void loginFromRegister(Scanner scanner, UserRepository userRepository, String username, SecurityUtils securityUtils) {

        int currentTry = 1;
        int maximumRetires = 5;

        while (currentTry < maximumRetires) {
            System.out.print("\nEnter password for " + username + " : ");
            String password = scanner.nextLine();

            String userDbPassword = userRepository.getInformationByUsername(username, "password");
            String decryptedUserDbPassword = securityUtils.decrypt(userDbPassword);

            if (decryptedUserDbPassword.equals(password)) {
                System.out.print("\n(logged in as '" + username + "')");
                Meniu.loggedInMenu(new User(
                        username,
                        password,
                        userRepository.getInformationByUsername(username, UTC_USERS_TABLE_NAME),
                        userRepository.getInformationByUsername(username, UTC_USERS_TABLE_LASTNAME),
                        userRepository.getInformationByUsername(username, UTC_USERS_TABLE_EMAIL),
                        userRepository.getInformationByUsername(username, UTC_USERS_TABLE_PERSONAL_CODE))
                );
            } else {
                currentTry++;
                System.out.println("Failed to login , try again (" + (maximumRetires - currentTry) + ")");
            }
        }

        System.out.println("\nFailed to login, returning to main menu ...");
        Meniu.mainMenu();
    }

    public static void register(Scanner scanner, UserRepository userRepository, SecurityUtils securityUtils) {

        System.out.print("\nEnter username: ");
        String username = scanner.nextLine();

        if (userRepository.checkIfUserAlreadyExists(username)) {
            System.out.println(username + " already exists, try to login ...");
            loginFromRegister(scanner, userRepository, username, securityUtils);
        } else {

            System.out.print("\nEnter password: ");
            String password = scanner.nextLine();
            String encryptedPassword = securityUtils.encrypt(password);

            System.out.print("\nEnter name: ");
            String name = scanner.nextLine();

            System.out.print("\nEnter lastname: ");
            String lastname = scanner.nextLine();

            System.out.print("\nEnter email: ");
            String email = scanner.nextLine();

            System.out.print("\nEnter personal code: ");
            String personalCode = scanner.nextLine();

            InputVadility iv = new InputVadility();
            if (iv.checkArrayForFalseItemValue(ArrayUtils.toArray(username, password, name, lastname, email, personalCode))) {
                throw new IllegalArgumentException("Invalid user input detected");
            }

            userRepository.registerNewUser(username, encryptedPassword, name, lastname, email, personalCode);
            System.out.print("\n(logged in as '" + username + "')");
            Meniu.loggedInMenu(new User(username, encryptedPassword, name, lastname, email, personalCode));
        }
    }

    public static void registerFromSignup(Scanner scanner, UserRepository userRepository, String username, SecurityUtils securityUtils) {

        System.out.print("\nEnter password: ");
        String password = scanner.nextLine();
        String encryptedPassword = securityUtils.encrypt(password);

        System.out.print("\nEnter name: ");
        String name = scanner.nextLine();

        System.out.print("\nEnter lastname: ");
        String lastname = scanner.nextLine();

        System.out.print("\nEnter email: ");
        String email = scanner.nextLine();

        System.out.print("\nEnter personal code: ");
        String personalCode = scanner.nextLine();

        if (userRepository.checkIfUserAlreadyExists(username)) {
            throw new IllegalArgumentException(username + " already exists");
        }

        InputVadility iv = new InputVadility();
        if (iv.checkArrayForFalseItemValue(ArrayUtils.toArray(username, password, name, lastname, email, personalCode))) {
            throw new IllegalArgumentException("Invalid user input detected");
        }

        userRepository.registerNewUser(username, encryptedPassword, name, lastname, email, personalCode);
        System.out.print("\n(logged in as '" + username + "')");
        Meniu.loggedInMenu(new User(username, encryptedPassword, name, lastname, email, personalCode));
    }

    public static void loggedInMenuActions(User user) {

        Scanner scanner = new Scanner(System.in);

        Property property = new Property(user);
        PropertyRepository pr = new PropertyRepository(new DatabaseConfig());
        IndicatorRepository ir = new IndicatorRepository(new DatabaseConfig());

        System.out.print("\nChoice: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "0":
                Meniu.mainMenu();
            case "1":
                System.out.println("\n" + userProperties(pr, property));
                break;
            case "2":
                loggedInUserIndicators(scanner, pr, property, ir);
                break;
            case "3":
                // bill stuff
                break;
            case "4":
                // account stuff
                break;
        }

        Meniu.loggedInMenu(user);
    }

    public static MultiValuedMap<String, String> userProperties(PropertyRepository pr, Property property) {
        MultiValuedMap<String, String> properties = pr.getUserProperties(property.getOwnderPersonalCode());
        return pr.userHasProperties(properties) ? properties : null;
    }

    public static void loggedInUserIndicators(Scanner scanner, PropertyRepository pr, Property property, IndicatorRepository ir) {

        MultiValuedMap<String, String> properties = userProperties(pr, property);

        if (properties != null) {
            Map<Integer, String> userPropertiesTotal = pr.getUserPropertiesCount(properties);
            System.out.println("Available types:" + userPropertiesTotal);

            System.out.println("Enter property type number: ");
            String propertyTypeInput = scanner.nextLine();
            String typeChoice = userPropertiesTotal.get(Integer.valueOf(propertyTypeInput));

            Map<Integer, String> propertyAddresses = new HashMap<>();
            int addressId = 1;

            if (properties.containsKey(typeChoice)) {
                for (String address : properties.get(typeChoice)) {
                    propertyAddresses.put(addressId, address);
                    addressId++;
                }
            }

            System.out.println("\nChoose address to visualize indicators: ");
            String addressChoiceInput = scanner.nextLine();
            String addressChoice = propertyAddresses.get(Integer.valueOf(addressChoiceInput));

            if (propertyAddresses.containsValue(addressChoice)) {
                Map<Integer, String> propertyIndicators = ir.getPropertyIndicatorsByPropertyAddress(pr, addressChoice);
                System.out.println("Indicator id - " + propertyIndicators.keySet() +
                        "\nMonth start,Month end - " + propertyIndicators.values());
            }
        } else {
            System.out.println("No properties were found, returning to menu ...");
        }


    }

}
