package org.musi.AI4Education.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
@Data
public class ExplanationChatHistory {
    private String sid;
    private String qid;
    private List<HashMap<String,String>> wenxinChatHistory;
}
