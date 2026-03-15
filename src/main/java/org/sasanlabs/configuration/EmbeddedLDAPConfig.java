package org.sasanlabs.configuration;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import javax.annotation.PostConstruct;
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
                new InMemoryDirectoryServerConfig("dc=example,dc=com");

        directoryServer = new InMemoryDirectoryServer(config);

        directoryServer.startListening();

        seedUsers();
    }

    private void seedUsers() throws Exception {

        directoryServer.add(
                "dn: dc=example,dc=com", "objectClass: top", "objectClass: domain", "dc: example");

        directoryServer.add(
                "dn: uid=alice,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "uid: alice",
                "sn: Alice",
                "cn: Alice",
                "userPassword: password123");

        directoryServer.add(
                "dn: uid=bob,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "uid: bob",
                "sn: Bob",
                "cn: Bob",
                "userPassword: password123");

        directoryServer.add(
                "dn: uid=charlie,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "uid: charlie",
                "sn: Charlie",
                "cn: Charlie",
                "userPassword: password123");

        for (int i = 4; i <= 10; i++) {

            directoryServer.add(
                    "dn: uid=user" + i + ",dc=example,dc=com",
                    "objectClass: inetOrgPerson",
                    "uid: user" + i,
                    "sn: User" + i,
                    "cn: User" + i,
                    "userPassword: password123");
        }
    }
}
