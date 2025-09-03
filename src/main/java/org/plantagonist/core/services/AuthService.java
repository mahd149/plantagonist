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

    public AuthService(UserStore users)
    {
        this.users = users;
    }

    public UserProfile register(String email, String username, char[] password) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email required");
        if (password == null || password.length < 6) throw new IllegalArgumentException("Password too short");

        System.out.println("[register] file = " + org.plantagonist.core.storage.PathsConfig.userJson().toAbsolutePath());

        // Make a temporary String ONLY for hashing & self-verify (remove later)
        String plain = new String(password);

        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(plain, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        System.out.println("[register] hash prefix = " + hash.substring(0, 7));

        // Real check: compare the same plain text you just hashed
        boolean selfOk = org.mindrot.jbcrypt.BCrypt.checkpw(plain, hash);
        System.out.println("[register] self-verify = " + selfOk);  // <-- should print true

        // Now it's safe to wipe the char[] (the String 'plain' will linger; OK for debug only)
        java.util.Arrays.fill(password, '\0');

        UserProfile u = UserProfile.create(email, username, hash);
        users.insert(u); // ensure this writes to ~/.plantagonist/data/user.json
        return u;
    }



    public UserProfile login(String emailOrUsername, char[] password) {
        System.out.println("[AuthService] user.json = " + org.plantagonist.core.storage.PathsConfig.userJson().toAbsolutePath());

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
