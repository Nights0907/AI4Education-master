package org.musi.AI4Education.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.History;
import org.musi.AI4Education.domain.Student;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/student/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @GetMapping("")
    public CommonResponse<List<History>> getHistory() {
        if (StpUtil.isLogin()) {
            List<History> historyList = historyService.getHistory();
            return CommonResponse.creatForSuccess(historyList);
        } else {
            return CommonResponse.creatForError("请先登录！");
        }
    }

    @GetMapping("/date")
    public CommonResponse<List<History>> getHistoryByDate(@RequestParam String date) {
        if (StpUtil.isLogin()) {
            List<History> historyList = historyService.getHistoryByDate(date);
            return CommonResponse.creatForSuccess(historyList);
        } else {
            return CommonResponse.creatForError("请先登录！");
        }
    }

}
