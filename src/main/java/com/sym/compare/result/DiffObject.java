package com.sym.compare.result;

import lombok.Data;
import lombok.ToString;

/**
 * 两个对象, 同一个属性比对的差异值
 *
 * @author shenyanming
 * Create on 2021/07/21 10:38
 */
@Data
@ToString
public class DiffObject<E> {

    /**
     * 属性名
     */
    private Object key;

    /**
     * 其中一个对象
     */
    private E left;

    /**
     * 另一个对象
     */
    private E right;

    public DiffObject(Object key, E left, E right){
        this.key = key;
        this.left = left;
        this.right = right;
    }
}
