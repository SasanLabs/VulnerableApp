package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * This class is the response bean for the ChallengeCard annotation.
 * It provides the detailed learning challenges, hints, and payloads
 * associated with a specific vulnerability level.
 *
 * <p>Note: As these beans are returned to the UI, they expect that all 
 * the labels/property keys are resolved to their respective values via 
 * the MessageBundle.
 *
 * @author Aryan mr.aryankaushal@gmail.com
 */
public class ChallengeCardResponseBean {
    
    @JsonProperty("ChallengeText")
    private String challengeText;

    @JsonProperty("hints")
    private List<HintResponseBean> hints;

    @JsonProperty("payload")
    private PayloadResponseBean payload;

    public ChallengeCardResponseBean(String challengeText, List<HintResponseBean> hints, PayloadResponseBean payload) {
        this.challengeText = challengeText;
        this.hints = hints;
        this.payload = payload;
    }

    public String getChallengeText() {
        return challengeText;
    }

    public List<HintResponseBean> getHints() {
        return hints;
    }

    public PayloadResponseBean getPayload() {
        return payload;
    }

    /**
     * Response bean for the Hint within a ChallengeCard.
     */
    public static class HintResponseBean {
        
        @JsonProperty("order")
        private int order;

        @JsonProperty("text")
        private String text;

        public HintResponseBean(int order, String text) {
            this.order = order;
            this.text = text;
        }

        public int getOrder() {
            return order;
        }

        public String getText() {
            return text;
        }
    }

    public static class PayloadResponseBean {
        
        @JsonProperty("description")
        private String description;

        @JsonProperty("value")
        private String value;

        public PayloadResponseBean(String description, String value) {
            this.description = description;
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public String getValue() {
            return value;
        }
    }
}
