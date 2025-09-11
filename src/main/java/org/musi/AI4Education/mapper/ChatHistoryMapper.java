package org.musi.AI4Education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.musi.AI4Education.domain.ChatHistory;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
