package org.musi.AI4Education.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.json.JSONException;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.service.ChatGPTService;
import org.musi.AI4Education.service.SpeechRecognitionService;
import org.musi.AI4Education.service.StudentProfileService;
import org.musi.AI4Education.service.impl.GptServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class ChatGPTController {

    @Resource
    GptServiceImpl gptService;
    @Autowired
    private ChatGPTService chatGPTservice;
    @Autowired
    private StudentProfileService studentProfileService;
    @Autowired
    private SpeechRecognitionService speechRecognitionService;


    @GetMapping(value = "/chat/inspiration", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AIAnswerDTO>> getChatGPTForInspirationStream(@RequestParam String sid,@RequestParam String qid,@RequestParam String content) {
        String effectiveSid = resolveSid(sid);
        System.out.println("Question："+content);
        return gptService.doChatGPTStreamForInspiration(effectiveSid,qid,content)
                .map(aiAnswerDTO -> ServerSentEvent.<AIAnswerDTO>builder()//进行结果的封装，再返回给前端
                        .data(aiAnswerDTO)
                        .build()
                )
                .onErrorResume(e -> Flux.empty());
    }

    // ignore_security_alert
    @PostMapping(value = "/chat/inspiration/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getChatGPTForInspirationStreamByAudio(@RequestParam("file") MultipartFile file, @RequestParam String qid, @RequestParam String sid) {
        String effectiveSid = resolveSid(sid);
        return Mono.fromCallable(() -> speechRecognitionService.recognize(file))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(content -> {
                    System.out.println("语音识别内容：" + content);
                    System.out.println("Question：" + content);
                    return Flux.concat(
                            Flux.just(ServerSentEvent.<String>builder().event("recognized").data(content).build()),
                            gptService.doChatGPTStreamForInspiration(effectiveSid, qid, content)
                                    .map(aiAnswerDTO -> ServerSentEvent.<String>builder()
                                            .data(com.alibaba.fastjson.JSON.toJSONString(aiAnswerDTO))
                                            .build())
                    );
                })
                .onErrorResume(e -> Flux.empty());
    }

    @GetMapping(value = "/chat/explanation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AIAnswerDTO>> getChatGPTForExplanationStream(@RequestParam String sid,@RequestParam String content,@RequestParam String qid) throws JSONException {

        String effectiveSid = resolveSid(sid);
        Map<String, Object> result = studentProfileService.getStudentProfileInformation(effectiveSid);
        Object knowledgeList = result.get("knowledge_weight");
        Object abilityList = result.get("ability_weight");
        String studentCharactor = knowledgeList.toString()+abilityList.toString();

        return gptService.doChatGPTStreamForExplanation(effectiveSid,qid,content,studentCharactor)
                .map(aiAnswerDTO -> ServerSentEvent.<AIAnswerDTO>builder()//进行结果的封装，再返回给前端
                        .data(aiAnswerDTO)
                        .build()
                )
                .onErrorResume(e -> Flux.empty());

    }

    // ignore_security_alert
    @PostMapping(value = "/chat/explanation/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getChatGPTForExplanationStreamByAudio(@RequestParam("file") MultipartFile file, @RequestParam String qid, @RequestParam String sid) throws JSONException {

        String effectiveSid = resolveSid(sid);
        Map<String, Object> result = studentProfileService.getStudentProfileInformation(effectiveSid);
        Object knowledgeList = result.get("knowledge_weight");
        Object abilityList = result.get("ability_weight");
        String studentCharactor = knowledgeList.toString()+abilityList.toString();

        return Mono.fromCallable(() -> speechRecognitionService.recognize(file))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(content -> {
                    System.out.println("语音识别内容：" + content);
                    System.out.println("Question：" + content);
                    return Flux.concat(
                            Flux.just(ServerSentEvent.<String>builder().event("recognized").data(content).build()),
                            gptService.doChatGPTStreamForExplanation(effectiveSid, qid, content, studentCharactor)
                                    .map(aiAnswerDTO -> ServerSentEvent.<String>builder()
                                            .data(com.alibaba.fastjson.JSON.toJSONString(aiAnswerDTO))
                                            .build())
                    );
                })
                .onErrorResume(e -> Flux.empty());

    }


    @GetMapping("/chat/feiman")
    public Flux<ServerSentEvent<AIAnswerDTO>> getChatGPTForFeimanStream(@RequestParam String content,@RequestParam String qid,@RequestParam String sid) throws JSONException {

        String effectiveSid = resolveSid(sid);
        System.out.println("Question："+content);
        return gptService.doChatGPTStreamForFeiman(effectiveSid,qid,content)
                .map(aiAnswerDTO -> ServerSentEvent.<AIAnswerDTO>builder()//进行结果的封装，再返回给前端
                        .data(aiAnswerDTO)
                        .build()
                )
                .onErrorResume(e -> Flux.empty());

    }

    // ignore_security_alert
    @PostMapping(value = "/chat/feiman/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getChatGPTForFeimanStreamByAudio(@RequestParam("file") MultipartFile file, @RequestParam String qid, @RequestParam String sid) throws JSONException {
        String effectiveSid = resolveSid(sid);
        return Mono.fromCallable(() -> speechRecognitionService.recognize(file))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(content -> {
                    System.out.println("语音识别内容：" + content);
                    System.out.println("Question：" + content);
                    return Flux.concat(
                            Flux.just(ServerSentEvent.<String>builder().event("recognized").data(content).build()),
                            gptService.doChatGPTStreamForFeiman(effectiveSid, qid, content)
                                    .map(aiAnswerDTO -> ServerSentEvent.<String>builder()
                                            .data(com.alibaba.fastjson.JSON.toJSONString(aiAnswerDTO))
                                            .build())
                    );
                })
                .onErrorResume(e -> Flux.empty());
    }


    @GetMapping("/chat/inspiration/history")
    public CommonResponse<InspirationChatHistory> getInspirationChatHistroyByQid(@RequestParam String qid) throws IOException {
        if(StpUtil.isLogin()){
            InspirationChatHistory chatHistory = chatGPTservice.getInspirationChatHistoryByQid(qid);
            System.out.println(chatHistory);
            return CommonResponse.creatForSuccess(chatHistory);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/chat/explanation/history")
    public CommonResponse<ExplanationChatHistory> getExplanationChatHistroyByQid(@RequestParam String qid) throws IOException {
        if(StpUtil.isLogin()){
            ExplanationChatHistory chatHistory = chatGPTservice.getExplanationChatHistoryByQid(qid);
            System.out.println(chatHistory);
            return CommonResponse.creatForSuccess(chatHistory);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/chat/feiman/history")
    public CommonResponse<FeimanChatHistory> getFeimanChatHistroyByQid(@RequestParam String qid) throws IOException {
        if(StpUtil.isLogin()){
            FeimanChatHistory chatHistory = chatGPTservice.getFeimanChatHistoryByQid(qid);
            System.out.println(chatHistory);
            return CommonResponse.creatForSuccess(chatHistory);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    private String resolveSid(String sid) {
        if (sid == null || sid.trim().isEmpty() || "undefined".equalsIgnoreCase(sid.trim())) {
            if (!StpUtil.isLogin()) {
                throw new IllegalArgumentException("请先登录，或在请求中传入有效 sid");
            }
            return StpUtil.getLoginIdAsString();
        }
        return sid.trim();
    }

}
