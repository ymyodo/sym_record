package com.sym.compare.result;

import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.gson.Gson;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对象比对结果值
 *
 * @author shenyanming
 * Create on 2021/07/21 10:42
 * @see DiffObject
 */
@Data
public class DiffResult<E> {

    private static Gson gson = new Gson();

    /**
     * 只存在于其中一个对象
     */
    private List<DiffObject<E>> onlyInLeft;

    /**
     * 只存在于另一个对象
     */
    private List<DiffObject<E>> onlyInRight;

    /**
     * 同时存在于两个对象中, 但是值不一样
     */
    private List<DiffObject<E>> valueNotEqual;

    /**
     * 比对结果是否存在不一致
     *
     * @return true-不一致
     */
    public boolean hasDifference() {
        return CollectionUtils.isNotEmpty(onlyInLeft)
                || CollectionUtils.isNotEmpty(onlyInRight)
                || CollectionUtils.isNotEmpty(valueNotEqual);
    }

    /**
     * 添加只存在于左半部分的数据
     */
    public void addOnlyInLeft(Map<?, E> left) {
        onlyInLeft = initIfNull(onlyInLeft);
        left.forEach((k, v) -> onlyInLeft.add(new DiffObject<>(k, v, null)));
    }

    /**
     * 添加只存在于右半部分的数据
     */
    public void addOnlyInRight(Map<?, E> right) {
        onlyInRight = initIfNull(onlyInRight);
        right.forEach((k, v) -> onlyInRight.add(new DiffObject<>(k, v, null)));
    }

    /**
     * 添加左右部分都存在但是值不一样的数据
     */
    public void addValueNotEqual(Map<?, MapDifference.ValueDifference<E>> middle) {
        valueNotEqual = initIfNull(valueNotEqual);
        for (Map.Entry<?, MapDifference.ValueDifference<E>> entry : MapUtils.emptyIfNull(middle).entrySet()) {
            valueNotEqual.add(new DiffObject<>(entry.getKey(), entry.getValue().leftValue(), entry.getValue().rightValue()));
        }
    }

    @Override
    public String toString() {
        if (!hasDifference()) {
            return "DiffResult{}";
        }
        return gson.toJson(this);
    }

    private List<DiffObject<E>> initIfNull(List<DiffObject<E>> list) {
        return Objects.isNull(list) ? Lists.newArrayList() : list;
    }
}
