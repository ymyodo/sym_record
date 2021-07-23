package com.sym.common;

import com.sym.compare.CompareUtil;
import com.sym.compare.config.CompareConfig;
import com.sym.compare.config.CompareConfigBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author shenyanming
 * Create on 2021/07/20 17:43
 */
@Slf4j
public class GrayTest {

    @Test
    public void test01() throws Exception {
        Supplier<List<Integer>> s1 = () -> Arrays.asList(100, 200, 300);
        Supplier<List<Integer>> s2 = () -> Arrays.asList(50, 200, 500);

        CompareConfig<Integer> config = CompareConfigBuilder.integerDefaultConfig();

        List<Integer> list = CompareUtil.gray(s1, s2, config);
        System.out.println(list);
        Thread.sleep(1500);
    }
}
