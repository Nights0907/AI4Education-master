package org.musi.AI4Education.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIAnswerDTO {
    private String id;
    private String object;
    private int created;
    private String model;
    private List<ChoicesDTO> choices = new ArrayList<>();
    private String finish_reason;
    public String error;
    private AIAnswerUsageDTO usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChoicesDTO {
        public int index;
        public Delta delta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        public String role;
        public String content = "";
        public String error = "";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AIAnswerUsageDTO {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
    }
}
