package org.musi.AI4Education.service;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechRecognitionService {
    String recognize(MultipartFile file);
}
