package org.plantagonist.core.services;

import org.mindrot.jbcrypt.BCrypt;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.repositories.UserStore;
import org.plantagonist.core.repositories.UserRepositoryJson;

import java.util.Arrays;

public class AuthService {
    private final UserStore users;

    /** Default to JSON store at ~/.plantagonist/users.json */
    public AuthService() {
        this(new UserRepositoryJson());
    }

    public AuthService(UserStore users) {
        this.users = users;
    }

    public UserProfile register(String email, String username, char[] password) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email required");
        if (password == null || password.length < 6) throw new IllegalArgumentException("Password too short");

        String hash = BCrypt.hashpw(new String(password), BCrypt.gensalt(12));
        Arrays.fill(password, '\0');

        UserProfile u = UserProfile.create(email, username, hash);
        users.insert(u);
        return u;
    }

    public UserProfile login(String emailOrUsername, char[] password) {
        if (emailOrUsername == null || emailOrUsername.isBlank())
            throw new IllegalArgumentException("Email/username required");

        UserProfile u = emailOrUsername.contains("@")
                ? users.findByEmail(emailOrUsername.trim().toLowerCase())
                : users.findByUsername(emailOrUsername.trim());

        if (u == null) throw new IllegalStateException("Account not found");
        boolean ok = BCrypt.checkpw(new String(password), u.getPasswordHash());
        Arrays.fill(password, '\0');
        if (!ok) throw new IllegalStateException("Invalid credentials");
        return u;
    }
}
