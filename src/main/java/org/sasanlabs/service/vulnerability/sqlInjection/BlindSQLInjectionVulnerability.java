package org.sasanlabs.service.vulnerability.sqlInjection;

import java.util.Map;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.Variant;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.sasanlabs.vulnerability.utils.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This is the most difficult and slowest attack which is done only if Error Based and Union Based
 * SQLInjections are not possible. In this attack response from the API is just {@code true} or
 * {@code false} and to get the data from the API time based/condition based attacks are done like
 * Wait for 10 seconds if 5 users are there in the data based and this way if query starts following
 * those conditions then this attack is possible.
 *
 * @author preetkaran20@gmail.com KSASAN
 */
@VulnerableAppRestController(
        descriptionLabel = "SQL_INJECTION_VULNERABILITY",
        value = "BlindSQLInjectionVulnerability")
public class BlindSQLInjectionVulnerability {

    private JdbcTemplate applicationJdbcTemplate;

    static final String CAR_IS_PRESENT_RESPONSE = "{ \"isCarPresent\": true}";

    public BlindSQLInjectionVulnerability(
            @Qualifier("applicationJdbcTemplate") JdbcTemplate applicationJdbcTemplate) {
        this.applicationJdbcTemplate = applicationJdbcTemplate;
    }

    @AttackVector(
            vulnerabilityExposed = VulnerabilityType.BLIND_SQL_INJECTION,
            description = "BLIND_SQL_INJECTION_URL_PARAM_APPENDED_DIRECTLY_TO_QUERY",
            payload = "BLIND_SQL_INJECTION_PAYLOAD_LEVEL_1")
    @VulnerableAppRequestMapping(
            value = LevelConstants.LEVEL_1,
            htmlTemplate = "LEVEL_1/SQLInjection_Level1")
    public ResponseEntity<String> getCarInformationLevel1(
            @RequestParam Map<String, String> queryParams) {
        String id = queryParams.get(Constants.ID);
        BodyBuilder bodyBuilder = ResponseEntity.status(HttpStatus.OK);
        return applicationJdbcTemplate.query(
                "select * from cars where id=" + id,
                (rs) -> {
                    if (rs.next()) {
                        return bodyBuilder.body(CAR_IS_PRESENT_RESPONSE);
                    }
                    return bodyBuilder.body(
                            ErrorBasedSQLInjectionVulnerability.CAR_IS_NOT_PRESENT_RESPONSE);
                });
    }

    @AttackVector(
            vulnerabilityExposed = VulnerabilityType.BLIND_SQL_INJECTION,
            description =
                    "BLIND_SQL_INJECTION_URL_PARAM_WRAPPED_WITH_SINGLE_QUOTE_APPENDED_TO_QUERY",
            payload = "BLIND_SQL_INJECTION_PAYLOAD_LEVEL_2")
    @VulnerableAppRequestMapping(
            value = LevelConstants.LEVEL_2,
            htmlTemplate = "LEVEL_1/SQLInjection_Level1")
    public ResponseEntity<String> getCarInformationLevel2(
            @RequestParam Map<String, String> queryParams) {
        String id = queryParams.get(Constants.ID);
        BodyBuilder bodyBuilder = ResponseEntity.status(HttpStatus.OK);
        bodyBuilder.body(ErrorBasedSQLInjectionVulnerability.CAR_IS_NOT_PRESENT_RESPONSE);
        return applicationJdbcTemplate.query(
                "select * from cars where id='" + id + "'",
                (rs) -> {
                    if (rs.next()) {
                        return bodyBuilder.body(CAR_IS_PRESENT_RESPONSE);
                    }
                    return bodyBuilder.body(
                            ErrorBasedSQLInjectionVulnerability.CAR_IS_NOT_PRESENT_RESPONSE);
                });
    }

    @VulnerableAppRequestMapping(
            value = LevelConstants.LEVEL_3,
            variant = Variant.SECURE,
            htmlTemplate = "LEVEL_1/SQLInjection_Level1")
    public ResponseEntity<String> getCarInformationLevel3(
            @RequestParam Map<String, String> queryParams) {
        String id = queryParams.get(Constants.ID);
        BodyBuilder bodyBuilder = ResponseEntity.status(HttpStatus.OK);
        bodyBuilder.body(ErrorBasedSQLInjectionVulnerability.CAR_IS_NOT_PRESENT_RESPONSE);
        return applicationJdbcTemplate.query(
                (conn) -> conn.prepareStatement("select * from cars where id=?"),
                (prepareStatement) -> {
                    prepareStatement.setString(1, id);
                },
                (rs) -> {
                    if (rs.next()) {
                        return bodyBuilder.body(CAR_IS_PRESENT_RESPONSE);
                    }
                    return bodyBuilder.body(
                            ErrorBasedSQLInjectionVulnerability.CAR_IS_NOT_PRESENT_RESPONSE);
                });
    }
}
