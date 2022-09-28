/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class RuleNacosProvider<T extends RuleEntity> implements DynamicRuleProvider<List<T>> {
    private static final Map<Class<? extends RuleEntity>, String> TYPE_MAP = new HashMap<>(8);

    static {
        TYPE_MAP.put(FlowRuleEntity.class, NacosConfigUtil.FLOW_DATA_ID_POSTFIX);
        TYPE_MAP.put(DegradeRuleEntity.class, NacosConfigUtil.DEGRADE_DATA_ID_POSTFIX);
        TYPE_MAP.put(ParamFlowRuleEntity.class, NacosConfigUtil.PARAM_FLOW_DATA_ID_POSTFIX);
        TYPE_MAP.put(GatewayFlowRuleEntity.class, NacosConfigUtil.GATEWAY_FLOW_DATA_ID_POSTFIX);
        TYPE_MAP.put(SystemRuleEntity.class, NacosConfigUtil.SYSTEM_DATA_ID_POSTFIX);
        TYPE_MAP.put(AuthorityRuleEntity.class, NacosConfigUtil.AUTHORITY_DATA_ID_POSTFIX);
        TYPE_MAP.put(ApiDefinitionEntity.class, NacosConfigUtil.API_DATA_ID_POSTFIX);
    }

    private final ConfigService configService;
    private final Converter<String, List<T>> converter;
    private final String suffix;

    public RuleNacosProvider(ConfigService configService, Class<T> clazz) {
        this.configService = configService;
        this.converter = s -> JSON.parseArray(s, clazz);
        this.suffix = TYPE_MAP.get(clazz);
    }

    @Override
    public List<T> getRules(String appName) throws Exception {
        String rules = configService.getConfig(appName + suffix, NacosConfigUtil.GROUP_ID, 3000);
        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        return converter.convert(rules);
    }
}
