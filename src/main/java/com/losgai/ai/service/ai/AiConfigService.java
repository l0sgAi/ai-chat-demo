package com.losgai.ai.service.ai;

import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.enums.ResultCodeEnum;

import java.util.List;

public interface AiConfigService {
    ResultCodeEnum add(AiConfig aiConfig);

    List<AiConfig> queryByKeyWord(String keyWord);

    ResultCodeEnum deleteById(Long id);

    ResultCodeEnum update(AiConfig aiConfig);

    List<AiConfig> getModels();
}
