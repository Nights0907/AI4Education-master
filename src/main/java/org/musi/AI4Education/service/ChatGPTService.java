package org.musi.AI4Education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.json.JSONException;
import org.musi.AI4Education.domain.ChatHistory;
import org.musi.AI4Education.domain.ExplanationChatHistory;
import org.musi.AI4Education.domain.FeimanChatHistory;
import org.musi.AI4Education.domain.InspirationChatHistory;

import java.util.HashMap;
import java.util.List;

public interface ChatGPTService extends IService<ChatHistory> {

    public InspirationChatHistory getInspirationChatHistoryByQid(String qid);

    public ExplanationChatHistory getExplanationChatHistoryByQid(String qid);

    public FeimanChatHistory getFeimanChatHistoryByQid(String qid);

    public String getTextByPcm(String filePath);

    public Boolean getWavByText(String text);

}
