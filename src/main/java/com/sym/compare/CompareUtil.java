package com.sym.compare;

import com.sym.compare.config.CompareConfig;
import com.sym.compare.inner.ObjectDiffUtil;
import com.sym.compare.report.IReporter;
import com.sym.compare.result.DiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 对象比对工具
 *
 * @author shenyanming
 * Create on 2021/07/21 10:36
 */
@Slf4j
public class CompareUtil {

    public static <E, O> E gray(Supplier<E> oSupplier, Supplier<E> nSupplier, CompareConfig<O> config) throws Exception {
        Objects.requireNonNull(oSupplier);
        Objects.requireNonNull(nSupplier);
        Objects.requireNonNull(config);

        E oldResult = null;
        E newResult = null;
        Future<E> oldFuture = null;
        Future<E> newFuture = null;

        // 数据查询
        if (config.isSwitchToNewQuery()) {
            newFuture = asyncQuery(config.getNewQueryPool(), nSupplier);
        } else {
            oldFuture = asyncQuery(config.getOldQueryPool(), oSupplier);
        }

        // 2.开启了比对开关，那么还需要查另一个数据
        if (config.isSwitchToCmp() && config.isSwitchToNewQuery()) {
            oldFuture = asyncQuery(config.getOldQueryPool(), oSupplier);
        } else if (config.isSwitchToCmp() && !config.isSwitchToNewQuery()) {
            newFuture = asyncQuery(config.getNewQueryPool(), nSupplier);
        }

        // 3.从future中获取结果，主流程的异常需要抛出去给上游，灰度流程的异常不应该影响主流程
        boolean grayQueryFail = false;
        try {
            oldResult = getAsyncResult(oldFuture, config.getOldQueryWaitMillis());
        } catch (Exception e) {
            if (config.isSwitchToNewQuery()) {
                log.warn("Gray Query Fail!", e);
                grayQueryFail = true;
            } else {
                // 主数据查询超时
                log.error("Main Query Fail!", e);
                throw e;
            }
        }
        try {
            newResult = getAsyncResult(newFuture, config.getNewQueryWaitMillis());
        } catch (Exception e) {
            // 主数据查询超时
            if (config.isSwitchToNewQuery()) {
                log.error("Main Query Fail!", e);
                throw e;
            } else {
                log.warn("Gray Query Fail!", e);
                grayQueryFail = true;
            }
        }

        // 4.比对，灰度流程没有异常才开始比较
        if (config.isSwitchToCmp() && !grayQueryFail) {
            asyncCmp(oldResult, newResult, oSupplier, nSupplier, config);
        }

        // 5.返回结果
        return config.isSwitchToNewQuery() ? newResult : oldResult;

    }

    /**
     * 异步执行任务
     *
     * @param executor 线程池
     * @param task     任务
     * @param <E>      任务返回值类型
     * @return future
     */
    private static <E> Future<E> asyncQuery(ExecutorService executor, Supplier<E> task) {
        return executor.submit(new NamedCallableTask<>("query-task", task::get));
    }

    /**
     * 获取future的返回值，不捕获异常
     *
     * @param future  待获取结果的future对象
     * @param timeout future.get超时时间
     * @param <E>     返回值类型
     * @return future返回值
     * @throws Exception future.get的异常，主要是TimeoutException和业务执行的异常
     */
    private static <E> E getAsyncResult(Future<E> future, long timeout) throws Exception {
        if (future != null) {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        }
        return null;
    }

    /**
     * 使用cmp专用线程池对Object进行比较
     *
     * @param oldResult 旧链路查询的结果
     * @param newResult 新链路查询的结果
     * @param oSupplier 如果多次比较的话，比对不一致将通过此函数再次获取旧链路查询结果数据
     * @param nSupplier 如果多次比较的话，比对不一致将通过此函数再次获取新链路查询结果数据
     * @param config    配置对象
     * @param <E>       查询结果返回值类型
     * @param <O>       具体的VO对象类型
     */
    private static <E, O> void asyncCmp(E oldResult, E newResult, Supplier<E> oSupplier, Supplier<E> nSupplier,
                                        CompareConfig<O> config) {
        if (config.isSwitchToCmp()) {
            // 定义cmp task
            Runnable runnable = () -> {
                try {
                    startCmp(oldResult, newResult, oSupplier, nSupplier, config);
                } catch (NullPointerException e) {
                    log.error("config.keyExtractor or other object may be null", e);
                } catch (Exception e) {
                    log.error("exception occurred is compare task", e);
                }
            };
            // 提交执行
            config.getCmpPool().execute(new NamedRunnableTask("cmp task", runnable));
        }
    }

    /**
     * 实际的Object比对，主要是定义比对流程。最多比较config配置的次数，每次比对需要睡眠config中配置的时间
     *
     * @param oldResult 旧链路查询的结果
     * @param newResult 新链路查询的结果
     * @param oSupplier 如果多次比较的话，比对不一致将通过此函数再次获取旧链路查询结果数据
     * @param nSupplier 如果多次比较的话，比对不一致将通过此函数再次获取新链路查询结果数据
     * @param config    配置对象
     * @param <E>       查询结果返回值类型
     * @param <O>       具体的VO对象类型
     */
    private static <E, O> void startCmp(E oldResult, E newResult, Supplier<E> oSupplier, Supplier<E> nSupplier,
                                        CompareConfig<O> config) {
        Function<O, String> extractor = config.getKeyExtractor();
        Comparator<O> comparator = config.getObjectComparator();
        O zero = config.getZeroObject();

        DiffResult<O> diffResult = new DiffResult<>();
        for (int i = 0; i < config.getMaxCmpTimes(); i++) {
            // 使用Object比对工具进行比对
            diffResult = ObjectDiffUtil.diff(oldResult, newResult, extractor, comparator, zero);
            // 如果有不同，则sleep一段时间，重新查询数据再次比较
            if (diffResult.hasDifference()) {
                // 打印日志，睡眠
                log.debug("{} gray {} time result:{}", config.getBusinessFlag(), (i + 1), diffResult);
                try {
                    Thread.sleep(config.getCmpSleepMillis());
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                // 重新查询
                oldResult = oSupplier.get();
                newResult = nSupplier.get();
            } else {
                // 结果相同，不需要再比较了
                break;
            }
        }

        // 记录日志，并且上报结果
        reportGrayResult(config.getBusinessFlag(), diffResult, config.getReporter());
    }

    /**
     * 将比对结果进行上报
     *
     * @param businessFlag 业务标记，用于cat的指标名
     * @param diffResult   比对结果
     */
    private static <E> void reportGrayResult(String businessFlag, DiffResult<E> diffResult,
                                             IReporter<E> reporter) {
        if (Objects.isNull(reporter)) {
            return;
        }
        reporter.report(businessFlag, diffResult);
    }

    public static class NamedRunnableTask implements Runnable {
        private String name;
        private Runnable runnable;

        public NamedRunnableTask(String name, Runnable runnable) {
            this.name = name;
            this.runnable = runnable;
        }

        public String getName() {
            return name;
        }

        @Override
        public void run() {
            this.runnable.run();
        }
    }

    public static class NamedCallableTask<V> implements Callable<V> {
        private String name;
        private Callable<V> callable;

        public NamedCallableTask(String name, Callable callable) {
            this.name = name;
            this.callable = callable;
        }

        public String getName() {
            return name;
        }


        @Override
        public V call() throws Exception {
            return this.callable.call();
        }
    }
}
