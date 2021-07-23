package com.sym.compare.report;

import com.sym.compare.result.DiffResult;

/**
 * @author shenyanming
 * Create on 2021/07/23 13:59
 */
public interface IReporter<E> {

    /**
     * 将比对结果做报告
     *
     * @param result 比对结果
     */
    void report(String businessFlag, DiffResult<E> result);
}
