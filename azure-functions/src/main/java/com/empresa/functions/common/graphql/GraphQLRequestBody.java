package com.empresa.functions.common.graphql;

import java.util.Map;

public record GraphQLRequestBody(String query, Map<String, Object> variables, String operationName) {
}
