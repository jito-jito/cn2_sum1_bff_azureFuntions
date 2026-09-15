package com.empresa.functions.common.graphql;

import com.empresa.functions.common.HttpResponseUtil;
import com.empresa.functions.common.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Ejecuta una request GraphQL ({"query", "variables", "operationName"}) contra
 * un GraphQL ya construido y devuelve la respuesta en el formato estandar del
 * spec ({"data", "errors"}). Compartido por las Functions GraphQL de cada
 * dominio para no repetir el parseo de request/response en cada una (ver
 * UsuarioGraphQLFunction y RolGraphQLFunction).
 */
public final class GraphQLHttpHandler {

    private GraphQLHttpHandler() {
    }

    public static HttpResponseMessage handle(HttpRequestMessage<Optional<String>> request, ExecutionContext context,
            GraphQL graphQL) {
        GraphQLRequestBody body;
        try {
            body = JsonUtil.mapper().readValue(request.getBody().orElse("{}"), GraphQLRequestBody.class);
        } catch (JsonProcessingException e) {
            return HttpResponseUtil.error(request, HttpStatus.BAD_REQUEST, "JSON de request invalido");
        }
        if (body.query() == null || body.query().isBlank()) {
            return HttpResponseUtil.error(request, HttpStatus.BAD_REQUEST, "El campo 'query' es requerido");
        }
        try {
            ExecutionInput.Builder input = ExecutionInput.newExecutionInput().query(body.query());
            if (body.variables() != null) {
                input.variables(body.variables());
            }
            if (body.operationName() != null) {
                input.operationName(body.operationName());
            }
            ExecutionResult result = graphQL.execute(input.build());
            // Convencion GraphQL: siempre 200, los errores de resolucion viajan
            // dentro del body en "errors" (a diferencia de REST, donde el status
            // HTTP ya indica el resultado).
            return HttpResponseUtil.json(request, HttpStatus.OK, result.toSpecification());
        } catch (Exception e) {
            context.getLogger().log(Level.SEVERE, "Error ejecutando query GraphQL", e);
            return HttpResponseUtil.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        }
    }
}
