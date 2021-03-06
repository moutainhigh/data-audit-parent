package com.github.thestyleofme.comparison.common.infra.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.github.thestyleofme.comparison.common.infra.exceptions.HandlerException;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * description
 * </p>
 *
 * @author isaac 2020/10/23 15:30
 * @since 1.0.0
 */
@Slf4j
public class CommonUtil {

    private CommonUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(ClassLoader classLoader,
                                    Class<T> clazz,
                                    InvocationHandler invocationHandler) {
        return (T) Proxy.newProxyInstance(
                classLoader,
                new Class[]{clazz},
                invocationHandler);
    }

    /**
     * <p>
     * 根据大小获得每个批次的数量
     * </p>
     *
     * @author isaac 2020/10/27 16:06
     * @since 1.0.0
     */
    public static int calculateBatchSize(int listSize) {
        int defaultBatchSize = 1024;
        int pow = 10;
        int batchSize;
        if (listSize < defaultBatchSize * pow) {
            // 1024*10 -> 1万 1
            batchSize = defaultBatchSize;
        } else if (listSize < defaultBatchSize * pow * pow) {
            // 1024*100 -> 10万 4
            batchSize = listSize / 6;
        } else if (listSize < defaultBatchSize * pow * pow * pow) {
            // 1024*1000 -> 100万 8
            batchSize = listSize / 10;
        } else if (listSize < defaultBatchSize * pow * pow * pow * pow) {
            // 1024*10000 -> 1000万 16
            batchSize = listSize / 16;
        } else {
            batchSize = listSize / 32;
        }
        return batchSize;
    }

    /**
     * 分批
     *
     * @param list      等分批的集合
     * @param batchSize 每个批次的数量
     * @return List
     * @author isacc 2020/10/27 16:06
     */
    public static <T> List<List<T>> splitList(List<T> list, int batchSize) {
        int listSize = list.size();
        List<List<T>> listArray = new ArrayList<>();
        int toIndex = batchSize;
        for (int i = 0; i < listSize; i += batchSize) {
            if (i + batchSize > listSize) {
                // toIndex最后没有batchSize条数据则list中有几条
                toIndex = listSize - i;
            }
            listArray.add(list.subList(i, i + toIndex));
        }
        return listArray;
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
                log.debug("the file[{}] successfully deleted", path);
            } catch (IOException e) {
                throw new HandlerException("file[{}] delete error", path);
            }
        }
    }

}
