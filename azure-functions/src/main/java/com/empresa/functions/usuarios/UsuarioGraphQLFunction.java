package com.empresa.functions.usuarios;

import com.empresa.functions.common.graphql.GraphQLHttpHandler;
import com.empresa.functions.usuarios.graphql.UsuarioGraphQLSchema;
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
 * Capa GraphQL del dominio usuarios, complementaria a UsuarioFunctions (REST).
 * El schema se construye una sola vez por invocacion "caliente" (mismo motivo
 * que DataSourceProvider/ValidationUtil: parsear el SDL y armar el
 * RuntimeWiring tiene costo que no vale la pena repetir por request).
 */
public class UsuarioGraphQLFunction {

    private static final GraphQL GRAPHQL = UsuarioGraphQLSchema.build();

    @FunctionName("UsuariosGraphQL")
    public HttpResponseMessage query(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "graphql/usuarios",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        return GraphQLHttpHandler.handle(request, context, GRAPHQL);
    }
}
