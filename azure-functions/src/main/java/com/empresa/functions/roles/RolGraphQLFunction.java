package com.empresa.functions.roles;

import com.empresa.functions.common.graphql.GraphQLHttpHandler;
import com.empresa.functions.roles.graphql.RolGraphQLSchema;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.GraphQL;
import java.util.Optional;

/**
 * Capa GraphQL del dominio roles, complementaria a RolFunctions (REST). Ver
 * UsuarioGraphQLFunction para el mismo patron en el otro dominio.
 */
public class RolGraphQLFunction {

    private static final GraphQL GRAPHQL = RolGraphQLSchema.build();

    @FunctionName("RolesGraphQL")
    public HttpResponseMessage query(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "graphql/roles",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        return GraphQLHttpHandler.handle(request, context, GRAPHQL);
    }
}
