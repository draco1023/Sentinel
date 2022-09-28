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
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
@Configuration
public class NacosConfig {
    @Value("${nacos.config.server-addr}")
    private String serverAddr;

    @Bean
    public Converter<List<? extends RuleEntity>, String> ruleEntityEncoder() {
        return JSON::toJSONString;
    }

    @Bean
    public ConfigService nacosConfigService() throws Exception {
        return ConfigFactory.createConfigService(serverAddr);
    }

    @Bean
    public RuleNacosProvider<FlowRuleEntity> flowRuleEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, FlowRuleEntity.class);
    }

    @Bean
    public RuleNacosProvider<DegradeRuleEntity> degradeRuleEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, DegradeRuleEntity.class);
    }

    @Bean
    public RuleNacosProvider<ParamFlowRuleEntity> paramFlowRuleEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, ParamFlowRuleEntity.class);
    }

    @Bean
    public RuleNacosProvider<GatewayFlowRuleEntity> gatewayFlowRuleEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, GatewayFlowRuleEntity.class);
    }

    @Bean
    public RuleNacosProvider<SystemRuleEntity> systemRuleEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, SystemRuleEntity.class);
    }

    @Bean
    public RuleNacosProvider<AuthorityRuleEntity> authorityRuleEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, AuthorityRuleEntity.class);
    }

    @Bean
    public RuleNacosProvider<ApiDefinitionEntity> apiDefinitionEntityProvider(ConfigService configService) {
        return new RuleNacosProvider<>(configService, ApiDefinitionEntity.class);
    }
}
