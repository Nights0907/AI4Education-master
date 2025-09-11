package org.musi.AI4Education.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.History;
import org.musi.AI4Education.domain.Student;
import org.musi.AI4Education.mapper.HistoryMapper;
import org.musi.AI4Education.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class HistoryServiceImpl extends ServiceImpl<HistoryMapper, History> implements HistoryService {

    @Autowired
    private HistoryMapper historyMapper;

    @Override
    public History createHistory(History history) {
        historyMapper.insert(history);
        return history;
    }

    @Override
    public List<History> getHistory() {
        String sid = StpUtil.getLoginIdAsString();
        QueryWrapper<History> wrapper = new QueryWrapper<>();
        wrapper.eq("sid", sid);
        List<History> historyList = historyMapper.selectList(wrapper);
        return historyList;
    }

    @Override
    public List<History> getHistoryByDate(String date) {
        String sid = StpUtil.getLoginIdAsString();
        QueryWrapper<History> wrapper = new QueryWrapper<>();
        wrapper.eq("sid", sid);
        wrapper.eq("time", date);
        List<History> historyList = historyMapper.selectList(wrapper);
        return historyList;
    }
}
