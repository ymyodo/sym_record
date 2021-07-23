package com.sym.compare.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sym.compare.report.IReporter;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * 配置类构建器
 *
 * @author shenyanming
 * Create on 2021/07/21 20:13
 */
@Data
public class CompareConfigBuilder<E> {

    private CompareConfigBuilder() {
    }

    /**
     * 初始化一个builder
     */
    public static <E> CompareConfigBuilder<E> newBuilder() {
        return new CompareConfigBuilder<>();
    }

    public CompareConfig<E> build() {
        Objects.requireNonNull(oldQueryPool, "oldQueryPool must not null");
        Objects.requireNonNull(newQueryPool, "newQueryPool must not null");
        Objects.requireNonNull(cmpPool, "cmpPool must not null");
        Objects.requireNonNull(objectComparator, "objectComparator must not null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(businessFlag), "businessFlag must not empty");
        return new CompareConfig<>(this);
    }

    public static CompareConfig<Integer> integerDefaultConfig() {
        CompareConfigBuilder<Integer> builder = new CompareConfigBuilder<>();
        builder.oldQueryPool = Executors.newFixedThreadPool(10);
        builder.newQueryPool = Executors.newFixedThreadPool(10);
        builder.cmpPool = Executors.newFixedThreadPool(1);
        builder.oldQueryWaitMillis = 3000L;
        builder.newQueryWaitMillis = 3000L;
        builder.cmpSleepMillis = 1000L;
        builder.maxCmpTimes = 1;
        builder.businessFlag = "default";
        builder.objectComparator = Integer::compareTo;
        builder.keyExtractor = Object::toString;
        builder.switchToCmp = true;
        builder.reporter = (flag, result) -> {
            Logger logger = LoggerFactory.getLogger("defaultReporter");
            logger.info("businessFlag={}, result={}", flag, result);
        };
        return builder.build();
    }

    /**
     * 旧逻辑执行线程池
     */
    private ExecutorService oldQueryPool;

    /**
     * 新逻辑执行线程池
     */
    private ExecutorService newQueryPool;

    /**
     * 数据比对执行线程池
     */
    private ExecutorService cmpPool;

    /**
     * 旧逻辑查询超时时间
     */
    private long oldQueryWaitMillis;

    /**
     * 新逻辑查询超时时间
     */
    private long newQueryWaitMillis;

    /**
     * 两次比对中间的睡眠时间
     */
    private long cmpSleepMillis;

    /**
     * 是否切换到新逻辑
     */
    private boolean switchToNewQuery;

    /**
     * 是否开启数据比对
     */
    private boolean switchToCmp;

    /**
     * 最大数据比对次数
     */
    private int maxCmpTimes;

    /**
     * 对象key提取器
     */
    private Function<E, String> keyExtractor;

    /**
     * 对象比较器
     */
    private Comparator<E> objectComparator;

    /**
     * 空对象
     */
    private E zeroObject;

    /**
     * 业务标记, 用于打日志
     */
    private String businessFlag;

    /**
     * 报告器
     */
    private IReporter<E> reporter;

    /* builder methods */

    public CompareConfigBuilder<E> oldQueryPool(ExecutorService oldQueryPool) {
        this.oldQueryPool = oldQueryPool;
        return this;
    }

    public CompareConfigBuilder<E> newQueryPool(ExecutorService newQueryPool) {
        this.newQueryPool = newQueryPool;
        return this;
    }

    public CompareConfigBuilder<E> cmpPool(ExecutorService cmpPool) {
        this.cmpPool = cmpPool;
        return this;
    }

    public CompareConfigBuilder<E> oldQueryWaitMillis(long oldQueryWaitMillis) {
        Preconditions.checkState(oldQueryWaitMillis > 0, "oldQueryWaitMillis should great than 0");
        this.oldQueryWaitMillis = oldQueryWaitMillis;
        return this;
    }

    public CompareConfigBuilder<E> newQueryWaitMillis(long newQueryWaitMillis) {
        Preconditions.checkState(newQueryWaitMillis > 0, "newQueryWaitMillis should great than 0");
        this.newQueryWaitMillis = newQueryWaitMillis;
        return this;
    }

    public CompareConfigBuilder<E> cmpSleepMillis(long cmpSleepMillis) {
        Preconditions.checkState(cmpSleepMillis > 0, "cmpSleepMillis should great than 0");
        this.cmpSleepMillis = cmpSleepMillis;
        return this;
    }

    public CompareConfigBuilder<E> switchToNewQuery(boolean switchToNewQuery) {
        this.switchToNewQuery = switchToNewQuery;
        return this;
    }

    public CompareConfigBuilder<E> switchToCmp(boolean switchToCmp) {
        this.switchToCmp = switchToCmp;
        return this;
    }

    public CompareConfigBuilder<E> maxCmpTimes(int maxCmpTimes) {
        Preconditions.checkState(maxCmpTimes > 0, "maxCmpTimes should great than 0");
        this.maxCmpTimes = maxCmpTimes;
        return this;
    }

    public CompareConfigBuilder<E> keyExtractor(Function<E, String> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        this.keyExtractor = keyExtractor;
        return this;
    }

    public CompareConfigBuilder<E> objectComparator(Comparator<E> objectComparator) {
        this.objectComparator = objectComparator;
        return this;
    }

    public CompareConfigBuilder<E> zeroObject(E zeroObject) {
        this.zeroObject = zeroObject;
        return this;
    }

    public CompareConfigBuilder<E> businessFlag(String businessFlag) {
        this.businessFlag = businessFlag;
        return this;
    }

    public CompareConfigBuilder<E> reporter(IReporter<E> reporter) {
        this.reporter = reporter;
        return this;
    }
}
