package ru.sergkorot.dynamic.operation;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.sergkorot.dynamic.model.BaseSearchParam;
import ru.sergkorot.dynamic.model.ComplexSearchParam;
import ru.sergkorot.dynamic.model.PageAttribute;
import ru.sergkorot.dynamic.model.enums.GlueOperation;
import ru.sergkorot.dynamic.util.SortUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sykorotaye
 * Service for building requests into the mongodb with pagination
 * @see OperationService
 */
@Service
@SuppressWarnings("unused")
public class CriteriaOperationService implements OperationService<Criteria> {

    private final OperationProvider<Criteria> operationProvider;
    private final Map<String, ManualOperationProvider<Criteria>> manualOperationProviderMap;

    /**
     * Constructor which contains operation provider bean and list with manualOperationProviders (user's implementations)
     *
     * @param operationProvider        - interface for providing a different operations
     * @param manualOperationProviders - Interface for user's implementations with custom operations for requests into database
     * @see OperationProvider
     * @see ManualOperationProvider
     */
    public CriteriaOperationService(OperationProvider<Criteria> operationProvider,
                                    List<ManualOperationProvider<Criteria>> manualOperationProviders) {
        this.operationProvider = operationProvider;
        this.manualOperationProviderMap = CollectionUtils.isEmpty(manualOperationProviders)
                ? null
                : manualOperationProviders.stream().collect(Collectors.toMap(ManualOperationProvider::fieldName, Function.identity()));
    }

    @Override
    public Criteria buildBaseByParams(List<BaseSearchParam> baseSearchParams, GlueOperation glue) {
        if (CollectionUtils.isEmpty(baseSearchParams)) {
            return new Criteria();
        }

        List<Criteria> criteriaList = baseSearchParams
                .stream()
                .map(this::constructCriteria)
                .collect(Collectors.toList());

        return glue.glueCriteriaOperation(criteriaList);
    }

    @Override
    public Criteria buildComplexByParams(List<ComplexSearchParam> complexSearchParams, GlueOperation externalGlue) {
        List<Criteria> criteriaList = complexSearchParams.stream()
                .map(complexSearchParam ->
                        buildBaseByParams(
                                complexSearchParam.getBaseSearchParams(),
                                complexSearchParam.getInternalGlue()
                        ))
                .collect(Collectors.toList());
        return externalGlue.glueCriteriaOperation(criteriaList);
    }

    /**
     * Create PageRequest extension for paging and sorting settings
     *
     * @param query            - query for mongodb request
     * @param pageAttribute    - attribute class for pagination and sorting
     * @param searchSortFields - fields by which sorting is possible in the database
     * @return Query
     * @see PageAttribute
     */
    public Query buildPageSettings(Query query, PageAttribute pageAttribute, List<String> searchSortFields) {
        return query
                .limit(pageAttribute.getLimit())
                .skip(pageAttribute.getOffset())
                .with(SortUtils.makeSort(searchSortFields, pageAttribute.getSortBy()));
    }

    private Criteria constructCriteria(BaseSearchParam param) {
        if (!CollectionUtils.isEmpty(manualOperationProviderMap) && manualOperationProviderMap.containsKey(param.getName())) {
            return manualOperationProviderMap.get(param.getName()).buildOperation(param);
        }
        return buildOperation(param, operationProvider);
    }
}
