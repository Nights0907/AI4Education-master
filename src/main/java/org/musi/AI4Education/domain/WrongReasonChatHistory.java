package org.musi.AI4Education.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
@Data
public class WrongReasonChatHistory {

    private String sid;
    private String qid;
    private List<HashMap<String,String>> wenxinChatHistory;

}
