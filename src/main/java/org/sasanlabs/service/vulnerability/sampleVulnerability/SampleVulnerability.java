package org.sasanlabs.service.vulnerability.sampleVulnerability;

import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.Variant;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.service.vulnerability.bean.GenericVulnerabilityResponseBean;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This is a sample vulnerability for helping developers in adding a new Vulnerability for
 * VulnerableApp
 *
 * @author KSASAN preetkaran20@gmail.com
 */
/**
 * {@code VulnerableAppRestController} annotation is similar to {@link
 * org.springframework.stereotype.Controller} Annotation
 */
@VulnerableAppRestController(
        /**
         * "descriptionLabel" parameter of annotation is i18n label stored in {@link
         * /VulnerableApp/src/main/resources/i18n/}. This descriptionLabel
         * will be shown in the UI as the description of the Vulnerability. It helps students to
         * learn about the vulnerability and can also include some of the useful references etc.
         */
        descriptionLabel = "SAMPLE_VULNERABILITY",
        /**
         * "value" parameter of annotation is used to create the request mapping. e.g. for the below
         * parameter value, /VulnerableApp/SampleVulnerability will be created as URI Path.
         */
        value = "SampleVulnerability")
public class SampleVulnerability {

    /**
     * {@code AttackVector} annotation is used to create the Hints section in the User Interface.
     * This annotation can be mentioned multiple times in case the same vulnerability level
     */
    @AttackVector(
            /**
             * "vulnerabilityExposed" parameter is used to depict the Vulnerability exposed by the
             * level. For example say a level is exposing SQL_INJECTION.
             */
            vulnerabilityExposed = VulnerabilityType.SAMPLE_VULNERABILITY,
            /**
             * "description" parameter of annotation is i18n label stored in {@link
             * /VulnerableApp/src/main/resources/i18n/}. This description
             * will be shown in the UI as hint to give some indication on how the level is handling
             * input to help user to crack the level.
             */
            description = "SAMPLE_VULNERABILITY_USER_INPUT_HANDLING_INJECTION",

            /**
             * "payload" parameter of annotation is i18n label stored in {@link
             * /VulnerableApp/src/main/resources/attackvectors/*.properties}. This payload will be
             * shown in UI to help users find/exploit the vulnerability
             */
            payload = "NOT_APPLICABLE")
    /**
     * This annotation is similar to {@link RequestMapping} SpringBoot annotation. It will map the
     * endpoint to /VulnerableApp/SampleVulnerability/LEVEL_1 where LEVEL_1 is coming from the value
     * parameter.
     */
    @VulnerableAppRequestMapping(
            /**
             * "value" parameter is used to map the level to URI path
             * /VulnerableApp/SampleVulnerability/${value}.
             */
            value = LevelConstants.LEVEL_1,

            /**
             * "htmlTemplate" is used to load the UI for the level for taking input from the user.
             * It points to files in directory
             * src/main/resource/static/templates/${VulnerabilityName} e.g.
             * src/main/resource/static/templates/SampleVulnerability as ${htmlTemplate}.js,
             * ${htmlTemplate}.css, ${htmlTemplate}.html. e.g. in this case it will be:
             * src/main/resource/static/templates/SampleVulnerability/LEVEL_1/SampleVulnerability_Level1.js
             * etc
             *
             * <p>CSS, JS and HTML are all loaded to render the UI.
             */
            htmlTemplate = "LEVEL_1/SampleVulnerability")
    public GenericVulnerabilityResponseBean<String> sampleUnsecuredLevel(@RequestParam("name") String key) {
        /** Add Business logic here */
        return new GenericVulnerabilityResponseBean<>("Not Implemented", true);
    }

    /** For secured level there is no need for {@link AttackVector} annotation. */
    @VulnerableAppRequestMapping(
            value = LevelConstants.LEVEL_2,
           
            // Can reuse the same UI template in case it doesn't change between levels
            htmlTemplate = "LEVEL_1/SampleVulnerability",
            /**
             * "variant" parameter defines whether the level is secure or not and same is depicted
             * in the UI as a closed lock and open lock icon. Default value of the variant is
             * UNSECURE so in case a secure level is added, please add the variant as {@link
             * Variant#SECURE}
             */
            variant = Variant.SECURE)
    public GenericVulnerabilityResponseBean<String> sampleSecuredLevel(@RequestParam("name") String key) {
        /** Add Business logic here */
    	return new GenericVulnerabilityResponseBean<>("Not Implemented", true);
    }
}