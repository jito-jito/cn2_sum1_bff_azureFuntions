package com.empresa.functions.roles.graphql;

import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.roles.RolRepository;
import com.empresa.functions.roles.dto.RolDto;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * Query-only: roles con la relacion inversa "usuarios" (quien tiene asignado
 * este rol), que no existe como endpoint REST hoy -- agregarla aqui no
 * requirio un endpoint nuevo, solo un campo nuevo en el schema (ver
 * RolRepository#usuariosDelRol).
 */
public final class RolGraphQLSchema {

    private static final String SDL = """
            type Usuario {
                id: ID!
                username: String!
                email: String!
                estado: String!
            }

            type Rol {
                id: ID!
                nombre: String!
                descripcion: String
                estado: String!
                fechaCreacion: String!
                usuarios: [Usuario!]!
            }

            type Query {
                roles: [Rol!]!
                rol(id: ID!): Rol
            }
            """;

    private RolGraphQLSchema() {
    }

    public static GraphQL build() {
        return build(new RolRepository());
    }

    static GraphQL build(RolRepository repository) {
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("roles", env -> repository.listar())
                        .dataFetcher("rol", env -> {
                            String rawId = env.getArgument("id");
                            long id = Long.parseLong(rawId);
                            try {
                                return repository.obtenerPorId(id);
                            } catch (NotFoundException e) {
                                return null;
                            }
                        }))
                .type("Rol", builder -> builder
                        .dataFetcher("fechaCreacion", env -> ((RolDto) env.getSource()).fechaCreacion().toString())
                        .dataFetcher("usuarios", env -> repository.usuariosDelRol(((RolDto) env.getSource()).id())))
                .build();

        TypeDefinitionRegistry registry = new SchemaParser().parse(SDL);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(schema).build();
    }
}
