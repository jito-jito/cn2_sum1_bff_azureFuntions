package com.empresa.functions.usuarios.graphql;

import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.roles.dto.RolDto;
import com.empresa.functions.usuarios.UsuarioRepository;
import com.empresa.functions.usuarios.dto.UsuarioDto;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * Query-only: usuarios con sus roles ya anidados (UsuarioRepository ya trae
 * esa relacion resuelta con una sola query batched, ver
 * UsuarioRepository#rolesDeUsuarios), asi que "roles" en el tipo Usuario es
 * solo lectura de propiedad, no dispara una consulta nueva por usuario.
 */
public final class UsuarioGraphQLSchema {

    private static final String SDL = """
            type Rol {
                id: ID!
                nombre: String!
                descripcion: String
                estado: String!
                fechaCreacion: String!
            }

            type Usuario {
                id: ID!
                username: String!
                email: String!
                nombreCompleto: String
                estado: String!
                roles: [Rol!]!
                fechaCreacion: String!
                fechaModificacion: String
            }

            type Query {
                usuarios: [Usuario!]!
                usuario(id: ID!): Usuario
            }
            """;

    private UsuarioGraphQLSchema() {
    }

    public static GraphQL build() {
        return build(new UsuarioRepository());
    }

    static GraphQL build(UsuarioRepository repository) {
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("usuarios", env -> repository.listar())
                        .dataFetcher("usuario", env -> {
                            String rawId = env.getArgument("id");
                            long id = Long.parseLong(rawId);
                            try {
                                return repository.obtenerPorId(id);
                            } catch (NotFoundException e) {
                                return null;
                            }
                        }))
                .type("Usuario", builder -> builder
                        .dataFetcher("fechaCreacion", env -> ((UsuarioDto) env.getSource()).fechaCreacion().toString())
                        .dataFetcher("fechaModificacion", env -> {
                            var fecha = ((UsuarioDto) env.getSource()).fechaModificacion();
                            return fecha == null ? null : fecha.toString();
                        }))
                .type("Rol", builder -> builder
                        .dataFetcher("fechaCreacion", env -> ((RolDto) env.getSource()).fechaCreacion().toString()))
                .build();

        TypeDefinitionRegistry registry = new SchemaParser().parse(SDL);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(schema).build();
    }
}
