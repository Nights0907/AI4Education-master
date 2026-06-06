package org.musi.AI4Education.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AliyunAsrConfig {
    @Value("${aliyun.asr.app-key}")
    private String appKey;

    @Value("${aliyun.asr.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.asr.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.asr.url}")
    private String url;

    @Value("${aliyun.asr.format:wav}")
    private String format;

    @Value("${aliyun.asr.sample-rate:16000}")
    private int sampleRate;

    @Value("${aliyun.asr.enable-punctuation-prediction:true}")
    private boolean enablePunctuationPrediction;

    @Value("${aliyun.asr.enable-inverse-text-normalization:true}")
    private boolean enableInverseTextNormalization;
}
