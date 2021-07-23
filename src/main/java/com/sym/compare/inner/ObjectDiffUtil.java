package com.sym.compare.inner;

import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.sym.compare.result.DiffResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 对象比对工具, 支持简单对象、List、Map.
 * 统一将对象转换为{@link java.util.TreeMap}, 再通过
 * {@link Maps#difference(Map, Map, Equivalence)}获取不同key信息.
 *
 * @author shenyanming
 * Create on 2021/07/21 10:57
 */
public class ObjectDiffUtil {

    /**
     * 比对方法
     *
     * @param o1           对象1
     * @param o2           对象2
     * @param keyExtractor 对象非Map必传, 用来转换TreeMap提取key用
     * @param comparator   是否同一个对象判断
     * @param zeroObject   数据比对时，对象1是从数据库查询出来（DB中存在该对象但是所有属性=0），对象2是从ES查询出来（ES构建时所有属性=0不存，那么对象2=null），这样会导致对象1
     *                     只会存在左边，实际上应该认为这2个对象是相等的。为了避免出现大量比对结果不一致的误报，可以业务可以定义出一个空对象，这样工具会对其进行过滤。
     *                     要比较的对象
     * @param <T>          对象类型
     * @param <R>          key类型
     * @return 比对结果
     */
    @SuppressWarnings({"all"})
    public static <T, O> DiffResult<O> diff(T o1, T o2, Function<O, String> keyExtractor, Comparator<O> comparator, O zeroObject) {
        Objects.requireNonNull(comparator, "object comparator is null");

        // 若为list
        if (o1 instanceof List || o2 instanceof List) {
            Objects.requireNonNull(keyExtractor, "keyExtractor is null");
            // 将其转换为treeMap
            Map<String, O> m1 = boxList((List<O>) o1, keyExtractor);
            Map<String, O> m2 = boxList((List<O>) o2, keyExtractor);
            // 执行比较
            return diffMap(m1, m2, comparator, zeroObject);
        }

        // 若为map
        if (o1 instanceof Map || o2 instanceof Map) {
            // 将其转换成TreeMap
            Map<Object, O> m1 = boxMap((Map<Object, O>) o1);
            Map<Object, O> m2 = boxMap((Map<Object, O>) o2);
            // 执行比较
            return diffMap(m1, m2, comparator, zeroObject);
        }

        // 若为简单对象
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        Map<String, O> m1 = boxObject((O) o1, keyExtractor);
        Map<String, O> m2 = boxObject((O) o2, keyExtractor);
        // 执行比较
        return diffMap(m1, m2, comparator, zeroObject);
    }

    @SuppressWarnings("all")
    private static <O> DiffResult<O> diffMap(Map<? extends Object, O> map1, Map<? extends Object, O> map2,
                                             Comparator<O> objectComparator, O zeroObject) {
        // 返回值
        DiffResult<O> diffResult = new DiffResult<>();

        // 比较两个map, 获取不同值
        MapDifference<Object, O> difference = Maps.difference(map1, map2, new Equivalence<O>() {
            @Override
            protected boolean doEquivalent(O t1, O t2) {
                return objectComparator.compare(t1, t2) == 0;
            }

            @Override
            protected int doHash(O o) {
                return o.hashCode();
            }
        });

        // 比较结果, 只存在左边的, 过滤掉初始值
        Map<Object, O> onlyLeftMap = difference.entriesOnlyOnLeft();
        Map<Object, O> map = Maps.filterEntries(onlyLeftMap, entry ->
                Objects.isNull(zeroObject) || objectComparator.compare(entry.getValue(), zeroObject) != 0);
        diffResult.addOnlyInLeft(map);

        // 比较结果, 只存在右边, 过滤掉初始值
        Map<Object, O> onlyRightMap = difference.entriesOnlyOnRight();
        map = Maps.filterEntries(onlyRightMap, entry ->
                Objects.isNull(zeroObject) || objectComparator.compare(entry.getValue(), zeroObject) != 0);
        diffResult.addOnlyInRight(map);

        // 比较结果, key相同, value不同
        diffResult.addValueNotEqual(difference.entriesDiffering());

        return diffResult;
    }

    /**
     * 将原始Map转换成TreeMao
     *
     * @param map 原始map
     * @return treeMap
     */
    private static <O> Map<Object, O> boxMap(Map<Object, O> map) {
        Map<Object, O> notNullMap = MapUtils.emptyIfNull(map);
        return notNullMap instanceof TreeMap ? notNullMap : new TreeMap<>(notNullMap);
    }

    /**
     * 把List包装成TreeMap
     *
     * @param list         原始list
     * @param keyExtractor key提取器，对list中object提取key放入map
     * @param <O>          List中对象类型
     * @return 包装后的Map
     */
    private static <O> Map<String, O> boxList(List<O> list, Function<O, String> keyExtractor) {
        return CollectionUtils.emptyIfNull(list).stream().collect(toMap(keyExtractor));
    }

    /**
     * 将简单对象转换成TreeMap
     *
     * @param obj          简单对象
     * @param keyExtractor ket提取器, 用来从简单对象中提取出可比较的key
     * @return TreeMap
     */
    private static <O> Map<String, O> boxObject(O obj, Function<O, String> keyExtractor) {
        Map<String, O> map = Maps.newHashMap();
        if (Objects.nonNull(obj)) {
            map.put(keyExtractor.apply(obj), obj);
        }
        return map;
    }

    private static <O> Collector<O, ?, Map<String, O>> toMap(Function<O, String> keyExtractor) {
        return Collectors.toMap(keyExtractor, o -> o, throwingMerger(), TreeMap::new);
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

}
