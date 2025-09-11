package org.musi.AI4Education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.musi.AI4Education.domain.AbilityPoint;
import org.musi.AI4Education.domain.BasicQuestion;

import java.util.List;

public interface AbilityPointService extends IService<AbilityPoint> {
    public List<String> parseStringToList(String input);
}
