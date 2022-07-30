package org.sasanlabs.service.vulnerability.xss.reflected;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class XSSInImgTagAttributeTest {

    @ParameterizedTest
    @CsvSource({
        "/VulnerableApp/images/ZAP.png,/VulnerableApp/images/ZAP.png",
        "/VulnerableApp/images/path/to/ZAP.png,/VulnerableApp/images/path/to/ZAP.png",
        "/VulnerableApp/images/<greek>διαδρομή/προς</greek>/ZAP.png,/VulnerableApp/images/"
                + "&#x3c;greek&#x3e;&#x3b4;&#x3b9;&#x3b1;&#x3b4;&#x3c1;&#x3bf;&#x3bc;ή/"
                + "&#x3c0;&#x3c1;&#x3bf;&#x3c2;&#x3c;/greek&#x3e;/ZAP.png"
    })
    public void getVulnerablePayloadLevelSecure_validPaths(String input, String expected) {
        XSSInImgTagAttribute subject = new XSSInImgTagAttribute();

        ResponseEntity<String> actual = subject.getVulnerablePayloadLevelSecure(input);

        String expectedString = "<img src=\"" + expected + "\" width=\"400\" height=\"300\"/>";

        assertEquals(expectedString, actual.getBody());
    }

    @ParameterizedTest
    @CsvSource({"''onerror='alert(1);'", " onerror=alert`1`", "http://evil.org/maliciousImage.png"})
    public void getVulnerablePayloadLevelSecure_exploitsFromLowerLevels(String input) {
        XSSInImgTagAttribute subject = new XSSInImgTagAttribute();

        ResponseEntity<String> actual = subject.getVulnerablePayloadLevelSecure(input);

        assertSame(actual.getStatusCode(), HttpStatus.BAD_REQUEST);
    }
}
