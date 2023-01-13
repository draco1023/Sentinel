package com.alibaba.csp.sentinel.dashboard.rule;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.NacosConfigUtil;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.RuleNacosProvider;
import com.alibaba.csp.sentinel.util.AssertUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Draco
 * @since 2022-09-27
 */
@Aspect
@Component
public class SentinelApiClientAspect {
    private static Logger logger = LoggerFactory.getLogger(SentinelApiClientAspect.class);

    @Autowired
    @Qualifier("ruleNacosPublisher")
    private DynamicRulePublisher<List<? extends RuleEntity>> publisher;

    @Autowired
    private RuleNacosProvider<FlowRuleEntity> flowRuleEntityProvider;
    @Autowired
    private RuleNacosProvider<DegradeRuleEntity> degradeRuleEntityProvider;
    @Autowired
    private RuleNacosProvider<ParamFlowRuleEntity> paramFlowRuleEntityProvider;
    @Autowired
    private RuleNacosProvider<GatewayFlowRuleEntity> gatewayFlowRuleEntityProvider;
    @Autowired
    private RuleNacosProvider<SystemRuleEntity> systemRuleEntityProvider;
    @Autowired
    private RuleNacosProvider<AuthorityRuleEntity> authorityRuleEntityProvider;
    @Autowired
    private RuleNacosProvider<ApiDefinitionEntity> apiDefinitionEntityProvider;

    private ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("sentinel-nacos"));

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.setFlowRuleOfMachineAsync(..))")
    public void setFlowRuleOfMachineAsync(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.FLOW_DATA_ID_POSTFIX);
    }

    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchFlowRuleOfMachine(..))")
    public Object fetchFlowRuleOfMachine(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String app = (String) joinPoint.getArgs()[0];
            String ip = (String) joinPoint.getArgs()[1];
            Integer port = (Integer) joinPoint.getArgs()[2];
            return flowRuleEntityProvider.getRules(app)
                    .stream().peek(r -> {
                        r.setApp(app);
                        r.setIp(ip);
                        r.setPort(port);
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error when fetching flow rules", e);
            return joinPoint.proceed();
        }
    }

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.setDegradeRuleOfMachine(..))")
    public void setDegradeRuleOfMachine(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.DEGRADE_DATA_ID_POSTFIX);
    }

    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchDegradeRuleOfMachine(..))")
    public Object fetchDegradeRuleOfMachine(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String app = (String) joinPoint.getArgs()[0];
            String ip = (String) joinPoint.getArgs()[1];
            Integer port = (Integer) joinPoint.getArgs()[2];
            return degradeRuleEntityProvider.getRules(app)
                    .stream().peek(r -> {
                        r.setApp(app);
                        r.setIp(ip);
                        r.setPort(port);
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error when fetching degrade rules", e);
            return joinPoint.proceed();
        }
    }

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.setParamFlowRuleOfMachine(..))")
    public void setParamFlowRuleOfMachine(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.PARAM_FLOW_DATA_ID_POSTFIX);
    }

    @SuppressWarnings("unchecked")
    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchParamFlowRulesOfMachine(..))")
    public Object fetchParamFlowRulesOfMachine(ProceedingJoinPoint joinPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String app = (String) joinPoint.getArgs()[0];
                String ip = (String) joinPoint.getArgs()[1];
                Integer port = (Integer) joinPoint.getArgs()[2];
                return paramFlowRuleEntityProvider.getRules(app)
                        .stream().peek(r -> {
                            r.setApp(app);
                            r.setIp(ip);
                            r.setPort(port);
                        }).collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Error when fetching parameter flow rules", e);
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(ex -> {
            try {
                return ((CompletableFuture<List<ParamFlowRuleEntity>>) joinPoint.proceed()).get();
            } catch (Throwable throwable) {
                logger.error("Error when fetching parameter flow rules via api client", throwable);
                throw new RuntimeException(throwable);
            }
        });
    }

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.modifyGatewayFlowRules(..))")
    public void modifyGatewayFlowRules(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.GATEWAY_FLOW_DATA_ID_POSTFIX);
    }

    @SuppressWarnings("unchecked")
    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchGatewayFlowRules(..))")
    public Object fetchGatewayFlowRules(ProceedingJoinPoint joinPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String app = (String) joinPoint.getArgs()[0];
                String ip = (String) joinPoint.getArgs()[1];
                Integer port = (Integer) joinPoint.getArgs()[2];
                return gatewayFlowRuleEntityProvider.getRules(app)
                        .stream().peek(r -> {
                            r.setApp(app);
                            r.setIp(ip);
                            r.setPort(port);
                        }).collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Error when fetching gateway flow rules", e);
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(ex -> {
            try {
                return ((CompletableFuture<List<GatewayFlowRuleEntity>>) joinPoint.proceed()).get();
            } catch (Throwable throwable) {
                logger.error("Error when fetching gateway flow rules via api client", throwable);
                throw new RuntimeException(throwable);
            }
        });
    }

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.setSystemRuleOfMachine(..))")
    public void setSystemRuleOfMachine(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.SYSTEM_DATA_ID_POSTFIX);
    }

    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchSystemRuleOfMachine(..))")
    public Object fetchSystemRuleOfMachine(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String app = (String) joinPoint.getArgs()[0];
            String ip = (String) joinPoint.getArgs()[1];
            Integer port = (Integer) joinPoint.getArgs()[2];
            return systemRuleEntityProvider.getRules(app)
                    .stream().peek(r -> {
                        r.setApp(app);
                        r.setIp(ip);
                        r.setPort(port);
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error when fetching system rules", e);
            return joinPoint.proceed();
        }
    }

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.setAuthorityRuleOfMachine(..))")
    public void setAuthorityRuleOfMachine(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.AUTHORITY_DATA_ID_POSTFIX);
    }

    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchAuthorityRulesOfMachine(..))")
    public Object fetchAuthorityRulesOfMachine(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String app = (String) joinPoint.getArgs()[0];
            String ip = (String) joinPoint.getArgs()[1];
            Integer port = (Integer) joinPoint.getArgs()[2];
            return authorityRuleEntityProvider.getRules(app)
                    .stream().peek(r -> {
                        r.setApp(app);
                        r.setIp(ip);
                        r.setPort(port);
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error when fetching authority rules", e);
            return joinPoint.proceed();
        }
    }

    @After("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.modifyApis(..))")
    public void modifyApis(JoinPoint joinPoint) throws Exception {
        publish(joinPoint, NacosConfigUtil.API_DATA_ID_POSTFIX);
    }

    @SuppressWarnings("unchecked")
    @Around("execution(public * com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient.fetchApis(..))")
    public Object fetchApis(ProceedingJoinPoint joinPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String app = (String) joinPoint.getArgs()[0];
                String ip = (String) joinPoint.getArgs()[1];
                Integer port = (Integer) joinPoint.getArgs()[2];
                return apiDefinitionEntityProvider.getRules(app)
                        .stream().peek(r -> {
                            r.setApp(app);
                            r.setIp(ip);
                            r.setPort(port);
                        }).collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Error when fetching gateway apis", e);
                throw new RuntimeException(e);
            }
        }, executor).exceptionally(ex -> {
            try {
                return ((CompletableFuture<List<ApiDefinitionEntity>>) joinPoint.proceed()).get();
            } catch (Throwable throwable) {
                logger.error("Error when fetching gateway apis via api client", throwable);
                throw new RuntimeException(throwable);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void publish(JoinPoint joinPoint, String suffix) throws Exception {
        Object[] args = joinPoint.getArgs();
        String app = (String) args[0];
        AssertUtil.notEmpty(app, "app name cannot be empty");
        List<? extends RuleEntity> rules = (List<? extends RuleEntity>) args[3];
        publisher.publish(app + suffix, rules);
    }

}
