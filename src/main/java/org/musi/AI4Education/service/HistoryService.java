package org.musi.AI4Education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.musi.AI4Education.domain.History;
import java.util.List;

public interface HistoryService extends IService<History> {
    //创建历史记录
    public History createHistory(History history);

    //查询所有历史记录
    public List<History> getHistory();

    //查询特定时间的历史记录
    public List<History> getHistoryByDate(String date);
}
