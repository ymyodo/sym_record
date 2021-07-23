package com.sym.guava;

import com.google.common.eventbus.EventBus;
import com.sym.guava.eventbus.event.CommodityEvent;
import com.sym.guava.eventbus.event.OrderEvent;
import com.sym.guava.eventbus.event.OrderEvent0;
import com.sym.guava.eventbus.handler.DefaultSubscriberExceptionHandler;
import com.sym.guava.eventbus.subscriber.CommoditySubscriber;
import com.sym.guava.eventbus.subscriber.GenericSubscriber;
import com.sym.guava.eventbus.subscriber.OrderSubscriber;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ym.shen
 * Created on 2020/4/21 14:38
 */
public class EventBusTest {

    private OrderSubscriber orderSubscriber = new OrderSubscriber();
    private EventBus eventBus;

    @Before
    public void before() {
        // 创建事件总线
        eventBus = new EventBus(new DefaultSubscriberExceptionHandler());
        // 设置订阅者
        eventBus.register(orderSubscriber);
    }

    /**
     * 正常发布事件
     */
    @Test
    public void test0001() {
        // 发布事件
        eventBus.post(new OrderEvent(123, "{id:123, name:'牛奶'}"));
    }

    /**
     * 发布订单子类事件{@link OrderEvent0}
     */
    @Test
    public void test0002() {
        eventBus.post(new OrderEvent0());
    }

    /**
     * 处理事件出现异常
     */
    @Test
    public void test0003() {
        eventBus.register(new CommoditySubscriber());
        // 发布时间
        eventBus.post(new CommodityEvent());
    }

    /**
     * 测试通用的处理器
     */
    @Test
    public void test0004() {
        eventBus.unregister(orderSubscriber);
        eventBus.register(new GenericSubscriber());

        // 发布订单事件
        eventBus.post(new OrderEvent());
        // 发布商品事件
        eventBus.post(new CommodityEvent());
    }
}
