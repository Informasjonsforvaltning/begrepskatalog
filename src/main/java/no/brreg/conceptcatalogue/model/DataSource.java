package no.brreg.conceptcatalogue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSource {
    @JsonProperty("dataSourceType")
    private DataSourceTypeEnum dataSourceType;
    @JsonProperty("url")
    private String url;
    @JsonProperty("acceptHeaderValue")
    private String acceptHeaderValue;
    @JsonProperty("publisherId")
    private String publisherId;
    @JsonProperty("description")
    private String description;

    @AllArgsConstructor
    public enum DataSourceTypeEnum {
        SKOS_AP_NO("SKOS-AP-NO"),
        DCAT_AP_NO("DCAT-AP-NO");

        private String value;

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
