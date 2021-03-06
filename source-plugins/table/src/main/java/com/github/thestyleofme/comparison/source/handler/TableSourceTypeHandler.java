package com.github.thestyleofme.comparison.source.handler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.thestyleofme.comparison.common.app.service.source.BaseSourceHandler;
import com.github.thestyleofme.comparison.common.app.service.source.SourceDataMapping;
import com.github.thestyleofme.comparison.common.domain.ColMapping;
import com.github.thestyleofme.comparison.common.domain.ComparisonJob;
import com.github.thestyleofme.comparison.common.domain.JobEnv;
import com.github.thestyleofme.comparison.common.infra.annotation.SourceType;
import com.github.thestyleofme.comparison.common.infra.exceptions.HandlerException;
import com.github.thestyleofme.comparison.source.pojo.TableInfo;
import com.github.thestyleofme.driver.core.app.service.DriverSessionService;
import com.github.thestyleofme.driver.core.app.service.session.DriverSession;
import com.github.thestyleofme.plugin.core.infra.utils.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * description
 * </p>
 *
 * @author isaac 2020/10/22 16:31
 * @since 1.0.0
 */
@Component
@SourceType("TABLE")
public class TableSourceTypeHandler implements BaseSourceHandler {

    private final DriverSessionService driverSessionService;

    public TableSourceTypeHandler(DriverSessionService driverSessionService) {
        this.driverSessionService = driverSessionService;
    }

    @Override
    public SourceDataMapping handle(ComparisonJob comparisonJob,
                                    Map<String, Object> env,
                                    Map<String, Object> sourceMap) {
        Long tenantId = comparisonJob.getTenantId();
        TableInfo tableInfo = BeanUtils.map2Bean(sourceMap, TableInfo.class);
        String sourceDatasourceCode = tableInfo.getSourceDatasourceCode();
        String sourceSchema = tableInfo.getSourceSchema();
        String sourceTable = tableInfo.getSourceTable();
        String targetDatasourceCode = tableInfo.getTargetDatasourceCode();
        String targetSchema = tableInfo.getTargetSchema();
        String targetTable = tableInfo.getTargetTable();
        boolean anyMatch = Stream.of(tenantId, sourceDatasourceCode, sourceSchema, sourceTable,
                targetDatasourceCode, targetSchema, targetTable)
                .anyMatch(Objects::isNull);
        if (anyMatch) {
            throw new HandlerException("when comparisonType=TABLE, " +
                    "[tenantId, sourceDatasourceCode, sourceSchema, sourceTable, " +
                    "targetDatasourceCode, targetSchema, targetTable] all cannot be null");
        }
        // 封装ComparisonMapping
        SourceDataMapping sourceDataMapping = new SourceDataMapping();
        handleSource(env, sourceDataMapping, tenantId, sourceDatasourceCode, sourceSchema, sourceTable);
        handleTarget(env, sourceDataMapping, tenantId, targetDatasourceCode, targetSchema, targetTable);
        return sourceDataMapping;
    }

    private void handleSource(Map<String, Object> env,
                              SourceDataMapping sourceDataMapping,
                              Long tenantId,
                              String sourceDatasourceCode,
                              String sourceSchema,
                              String sourceTable) {
        DriverSession sourceDriverSession = driverSessionService.getDriverSession(tenantId, sourceDatasourceCode);
        List<Map<String, Object>> sourceList = sourceDriverSession.tableQuery(sourceSchema, sourceTable);
        // 排序
        List<LinkedHashMap<String, Object>> result = sortListMap(env, sourceList, ColMapping.SOURCE);
        sourceDataMapping.setSourceDataList(result);
    }

    private void handleTarget(Map<String, Object> env,
                              SourceDataMapping sourceDataMapping,
                              Long tenantId,
                              String targetDatasourceCode,
                              String targetSchema,
                              String targetTable) {
        DriverSession targetDriverSession = driverSessionService.getDriverSession(tenantId, targetDatasourceCode);
        List<Map<String, Object>> targetList = targetDriverSession.tableQuery(targetSchema, targetTable);
        // 排序
        List<LinkedHashMap<String, Object>> result = sortListMap(env, targetList, ColMapping.TARGET);
        sourceDataMapping.setTargetDataList(result);
    }

    private List<LinkedHashMap<String, Object>> sortListMap(Map<String, Object> env,
                                                            List<Map<String, Object>> list,
                                                            String position) {
        JobEnv jobEnv = BeanUtils.map2Bean(env, JobEnv.class);
        List<Map<String, Object>> colMapping = jobEnv.getColMapping();
        List<LinkedHashMap<String, Object>> result = new ArrayList<>(list.size());
        if (CollectionUtils.isEmpty(colMapping)) {
            SortedMap<String, Object> sortedMap;
            LinkedHashMap<String, Object> linkedHashMap;
            for (Map<String, Object> map : list) {
                sortedMap = new TreeMap<>(Comparator.reverseOrder());
                sortedMap.putAll(map);
                linkedHashMap = new LinkedHashMap<>(sortedMap);
                result.add(linkedHashMap);
            }
            return result;
        }
        // 根据ColMapping的index进行排序
        List<ColMapping> colMappingList = colMapping.stream()
                .map(map -> BeanUtils.map2Bean(map, ColMapping.class))
                .collect(Collectors.toList());
        LinkedHashMap<String, Object> linkedHashMap = colMappingList.stream()
                .sorted(Comparator.comparingInt(ColMapping::getIndex))
                .collect(Collectors.toMap(
                        o -> {
                            if (ColMapping.SOURCE.equals(position)) {
                                return o.getSourceCol();
                            }
                            return o.getTargetCol();
                        },
                        ColMapping::getIndex,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
        LinkedHashMap<String, Object> temp;
        for (Map<String, Object> map : list) {
            linkedHashMap.putAll(map);
            temp = new LinkedHashMap<>(linkedHashMap);
            result.add(temp);
        }
        return result;
    }
}
