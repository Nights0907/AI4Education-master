package org.musi.AI4Education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.musi.AI4Education.domain.BasicQuestion;

import java.util.List;

@Mapper
public interface BasicQuestionMapper extends BaseMapper<BasicQuestion> {
}
