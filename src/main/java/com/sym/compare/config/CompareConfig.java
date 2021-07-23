package com.sym.compare.config;

import com.sym.compare.report.IReporter;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * @author shenyanming
 * Create on 2021/07/21 20:15
 */
public class CompareConfig<E> {

    private final CompareConfigBuilder<E> builder;

    public CompareConfig(CompareConfigBuilder<E> builder) {
        this.builder = builder;
    }

    public ExecutorService getOldQueryPool() {
        return builder.getOldQueryPool();
    }

    public ExecutorService getNewQueryPool() {
        return builder.getNewQueryPool();
    }

    public ExecutorService getCmpPool() {
        return builder.getCmpPool();
    }

    public long getOldQueryWaitMillis() {
        return builder.getOldQueryWaitMillis();
    }

    public long getNewQueryWaitMillis() {
        return builder.getNewQueryWaitMillis();
    }

    public boolean isSwitchToNewQuery() {
        return builder.isSwitchToNewQuery();
    }

    public boolean isSwitchToCmp() {
        return builder.isSwitchToCmp();
    }

    public int getMaxCmpTimes() {
        return builder.getMaxCmpTimes();
    }

    public Function<E, String> getKeyExtractor() {
        return builder.getKeyExtractor();
    }

    public Comparator<E> getObjectComparator() {
        return builder.getObjectComparator();
    }

    public E getZeroObject() {
        return builder.getZeroObject();
    }

    public long getCmpSleepMillis() {
        return builder.getCmpSleepMillis();
    }

    public String getBusinessFlag() {
        return builder.getBusinessFlag();
    }

    public IReporter<E> getReporter(){
        return builder.getReporter();
    }
}
