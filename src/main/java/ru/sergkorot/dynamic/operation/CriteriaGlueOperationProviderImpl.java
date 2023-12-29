package ru.sergkorot.dynamic.operation;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import ru.sergkorot.dynamic.glue.Glue;
import ru.sergkorot.dynamic.glue.GlueOperationProvider;

@Component
public class CriteriaGlueOperationProviderImpl implements GlueOperationProvider<Criteria> {
    @Override
    public Glue<Criteria> and() {
        return criteriaList -> new Criteria().andOperator(criteriaList);
    }

    @Override
    public Glue<Criteria> or() {
        return criteriaList -> new Criteria().orOperator(criteriaList);
    }
}
