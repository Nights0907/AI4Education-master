package org.musi.AI4Education.service;

import org.musi.AI4Education.common.CommonResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface QuestionCreationService {
    CommonResponse<Map<String, Object>> createQuestion(String sid, MultipartFile question) throws Exception;
}
