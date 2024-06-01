package com.kj.camunda_demo1.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author 17601
 */
@Component
@Slf4j
public class SubscribeTask {
    // 引擎端url 前缀
    private static final String CAMUNDA_BASE_URL = "http://192.168.72.128:8090/engine-rest";

    private ExternalTaskClient client = null;

    /**
     * 获取客户端
     * asyncResponseTimeout ： long polling timeout
     */
    private ExternalTaskClient getClient() {
        if (client == null) {
            client = ExternalTaskClient.create()
                    .baseUrl(CAMUNDA_BASE_URL)
                    .asyncResponseTimeout(10000)
                    .build();
        }
        return client;
    }

    @PostConstruct
    public void handleShoppingCart() {
        getClient().subscribe("shopping_cart")
                .processDefinitionKey("Process_shopping")
                .lockDuration(2000)
                .handler((externalTask, externalTaskService) -> {
                    log.info("订阅到加入购物车任务");
                    Map<String, Object> goodVariable = Variables.createVariables()
                            .putValue("msg", "我是java服务发送的消息：加入到购物车")
                            .putValue("size", "xl")
                            .putValue("count", 2);

                    externalTaskService.complete(externalTask, goodVariable);
                }).open();
    }

    @PostConstruct
    public void handlePay() {
        getClient().subscribe("pay")
                .processDefinitionKey("Process_shopping")
                .lockDuration(2000)
                .handler((externalTask, externalTaskService) -> {
                    log.info("付款去发货");
                    Object size = externalTask.getVariable("size");
                    Object count = externalTask.getVariable("count");
                    Object msg = externalTask.getVariable("msg");
                    log.info("收到付款任务，商品大小：{},商品数量：{},商品信息：{}", size, count, msg);

                    VariableMap variableMap = Variables.createVariables().putValue("toWhere", "北京");

                    externalTaskService.complete(externalTask, variableMap);
                }).open();
    }

    @PostConstruct
    public void handleLogisticsDelivery() {
        getClient().subscribe("send")
                .processDefinitionKey("Process_shopping")
                .lockDuration(2000)
                .handler((externalTask, externalTaskService) -> {
                    Object toWhere = externalTask.getVariable("toWhere");
                    log.info("收到发货任务，目的地：{}", toWhere);

                    externalTaskService.complete(externalTask);
                }).open();
    }

}