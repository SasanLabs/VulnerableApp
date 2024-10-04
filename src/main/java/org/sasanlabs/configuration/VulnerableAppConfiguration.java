package org.sasanlabs.configuration;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.service.vulnerability.fileupload.UnrestrictedFileUpload;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

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
    private static final List<String> MAX_FILE_UPLOAD_SIZE_OVERRIDE_PATHS =
            Arrays.asList(
                    "/" + UnrestrictedFileUpload.CONTROLLER_PATH + "/" + LevelConstants.LEVEL_9);

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
     * @param resourceLoader
     * @return {@link VulnerableAppProperties} which is injected in spring context.
     * @throws IOException
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
        VulnerableAppProperties vulnerableAppProperties =
                new VulnerableAppProperties(attackVectorProperties);
        return vulnerableAppProperties;
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

    @Bean
    @ConfigurationProperties("spring.datasource.application")
    public DataSourceProperties applicationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
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
    public JdbcTemplate applicationJdbcTemplate(
            @Qualifier("applicationDataSource") DataSource applicationDataSource) {
        return new JdbcTemplate(applicationDataSource);
    }

    /**
     * Customized MultipartFilter bean disables default max upload size for multipart files and
     * their overall requests, for select paths. See {@link
     * UnrestrictedFileUpload#getVulnerablePayloadLevel10()} for usage.
     */
    @Bean
    @Order(0)
    public MultipartFilter multipartFilter() {
        class MaxUploadSizeOverrideMultipartFilter extends MultipartFilter {
            @Override
            protected MultipartResolver lookupMultipartResolver(HttpServletRequest request) {
                if (MAX_FILE_UPLOAD_SIZE_OVERRIDE_PATHS.contains(request.getServletPath())) {
                    CommonsMultipartResolver multipart = new CommonsMultipartResolver();
                    multipart.setMaxUploadSize(-1);
                    multipart.setMaxUploadSizePerFile(-1);
                    return multipart;
                } else {
                    // returns default implementation
                    return lookupMultipartResolver();
                }
            }
        };
        return new MaxUploadSizeOverrideMultipartFilter();
    }
}
