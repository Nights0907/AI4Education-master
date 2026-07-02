package org.musi.AI4Education.service;

import org.musi.AI4Education.domain.chat.ChatHistory;
import org.musi.AI4Education.domain.chat.ExplanationChatHistory;
import org.musi.AI4Education.domain.chat.FeimanChatHistory;
import org.musi.AI4Education.domain.chat.InspirationChatHistory;
import com.baomidou.mybatisplus.extension.service.IService;
import org.json.JSONException;

import java.util.HashMap;
import java.util.List;

public interface ChatGPTService extends IService<ChatHistory> {

    public InspirationChatHistory getInspirationChatHistoryByQid(String qid);

    public ExplanationChatHistory getExplanationChatHistoryByQid(String qid);

    public FeimanChatHistory getFeimanChatHistoryByQid(String qid);

    public String getTextByPcm(String filePath);

    public Boolean getWavByText(String text);

}
