package org.sasanlabs.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * This is the Configuration Class for Injecting Configurations into the Context.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Configuration
public class VulnerableAppConfiguration {

    private static final String I18N_MESSAGE_FILE_LOCATION = "classpath:i18n/messages";
    private static final String ATTACK_VECTOR_PAYLOAD_PROPERTY_FILES_LOCATION_PATTERN =
            "classpath:/attackvectors/*.properties";

    /**
     * Will Inject MessageBundle into messageSource bean.
     *
     * @return resourceBundle
     */
    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(I18N_MESSAGE_FILE_LOCATION);
        messageSource.setCacheSeconds(100);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.US);
        return resolver;
    }

    /**
     * This method reads all the property which are useful for vulnerableApp and then injects them
     * into the context so that entire application can use it.
     *
     * @param resourceLoader loads the resources
     * @return {@link VulnerableAppProperties} which is injected in spring context.
     * @throws IOException throws input-output exception
     */
    @Bean
    public VulnerableAppProperties propertyLoader(ResourceLoader resourceLoader)
            throws IOException {
        Resource[] attackVectorsResources =
                new PathMatchingResourcePatternResolver()
                        .getResources(ATTACK_VECTOR_PAYLOAD_PROPERTY_FILES_LOCATION_PATTERN);
        Properties attackVectorProperties = new Properties();
        for (Resource attackVectorResource : attackVectorsResources) {
            PropertiesLoaderUtils.fillProperties(attackVectorProperties, attackVectorResource);
        }
        return new VulnerableAppProperties(attackVectorProperties);
    }

    /**
     * DB Configuration Below configuration is done to restrict the Application user rights and not
     * to give admin access rights to it. This is quite important because in case of any
     * Vulnerability in application it reduces the impact of the destruction because of the
     * vulnerability.
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.admin")
    public DataSourceProperties adminDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.admin.configuration")
    public DataSource adminDataSource(
            @Qualifier("adminDataSourceProperties")
                    DataSourceProperties adminDataSourceProperties) {
        return adminDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Initializes the admin DataSource by running schema and data SQL scripts. This creates tables,
     * the 'application' H2 DB user, and grants permissions. Must run before the
     * applicationDataSource bean tries to connect.
     */
    @Bean
    @DependsOnDatabaseInitialization
    public DataSourceInitializer adminDataSourceInitializer(
            @Qualifier("adminDataSource") DataSource adminDataSource,
            @Value("${spring.datasource.application.password}") String appPassword) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        JdbcTemplate adminJdbcTemplate = new JdbcTemplate(adminDataSource);
        adminJdbcTemplate.execute(
                String.format("CREATE USER application PASSWORD '%s'", appPassword));
        populator.addScript(new ClassPathResource("scripts/SQLInjection/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/xss/PersistentXSS/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/XXEVulnerability/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/SQLInjection/db/data.sql"));
        populator.addScript(new ClassPathResource("scripts/IDOR/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/IDOR/db/data.sql"));
        populator.addScript(new ClassPathResource("scripts/Authentication/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/Authentication/db/data.sql"));
        populator.addScript(new ClassPathResource("scripts/PasswordReset/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/CryptographicFailures/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/SessionManagement/db/schema.sql"));
        populator.addScript(new ClassPathResource("scripts/SessionManagement/db/data.sql"));
        populator.addScript(new ClassPathResource("scripts/OTP/db/schema.sql"));
        populator.setSeparator(";");

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(adminDataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    @Lazy
    @ConfigurationProperties("spring.datasource.application")
    public DataSourceProperties applicationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Lazy
    @DependsOn("adminDataSourceInitializer")
    @ConfigurationProperties("spring.datasource.application.configuration")
    public DataSource applicationDataSource(
            @Qualifier("applicationDataSourceProperties")
                    DataSourceProperties applicationDataSourceProperties) {
        return applicationDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Lazy
    public JdbcTemplate applicationJdbcTemplate(
            @Qualifier("applicationDataSource") DataSource applicationDataSource) {
        return new JdbcTemplate(applicationDataSource);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
