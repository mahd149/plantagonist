package org.plantagonist.tools;

import org.plantagonist.core.services.AuthService;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.storage.PathsConfig;

public class AuthSmokeTest {
    public static void main(String[] args) {
        try {
            System.out.println("User file = " + PathsConfig.userJson().toAbsolutePath());

            AuthService auth = new AuthService();

            // Use a unique test email/username so you don't collide with real users
            String email = "alice_test@example.com";
            String user  = "alice_test_user";
            char[] pass  = "secret12".toCharArray();

            // Register
            UserProfile u = auth.register(email, user, pass);
            System.out.println("Registered: " + u.getEmail());

            // Login (case-insensitive)
            UserProfile v = auth.login("ALICE_TEST@EXAMPLE.COM", "secret12".toCharArray());
            System.out.println("Logged in: " + v.getEmail());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
