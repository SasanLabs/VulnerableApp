package org.sasanlabs.configuration;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.sasanlabs.internal.utility.PasswordHashingUtils;
import org.springframework.stereotype.Component;

@Component
public class EmbeddedLDAPConfig {

    private static InMemoryDirectoryServer directoryServer;

    public static InMemoryDirectoryServer getDirectoryServer() {
        return directoryServer;
    }

    @PostConstruct
    public void startLDAPServer() throws Exception {

        InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig("dc=sasanlabs,dc=org");

        directoryServer = new InMemoryDirectoryServer(config);

        directoryServer.startListening();

        seedUsers();
    }

    private String createSaltedPassword(String password) {

        String salt = UUID.randomUUID().toString().substring(0, 8);

        String hash = PasswordHashingUtils.sha256Hex(salt, password);

        return salt + ":" + hash;
    }

    private void addUser(String uid, String name, String password) throws Exception {

        String storedPassword = createSaltedPassword(password);

        directoryServer.add(
                "dn: uid=" + uid + ",dc=sasanlabs,dc=org",
                "objectClass: inetOrgPerson",
                "uid: " + uid,
                "sn: " + name,
                "cn: " + name,
                "userPassword: " + storedPassword);
    }

    private void seedUsers() throws Exception {

        directoryServer.add(
                "dn: dc=sasanlabs,dc=org",
                "objectClass: top",
                "objectClass: domain",
                "dc: sasanlabs");

        addUser("alice", "Alice", "alicePass123");
        addUser("bob", "Bob", "bobPass123");
        addUser("charlie", "Charlie", "charliePass123");
        addUser("antriksh", "Antriksh", "antrikshPass123");

        for (int i = 5; i <= 10; i++) {

            addUser("user" + i, "User " + i, "user" + i + "Pass123");
        }
    }
}
