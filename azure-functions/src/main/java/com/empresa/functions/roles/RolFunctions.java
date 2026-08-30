package com.empresa.functions.roles;

import com.empresa.functions.common.HttpResponseUtil;
import com.empresa.functions.common.JsonUtil;
import com.empresa.functions.common.exception.ConflictException;
import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.common.exception.ValidationException;
import com.empresa.functions.roles.dto.ActualizarRolRequest;
import com.empresa.functions.roles.dto.CrearRolRequest;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Optional;
import java.util.logging.Level;

public class RolFunctions {

    private final RolService service = new RolService();

    @FunctionName("CrearRol")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "roles",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        return handle(request, context, () -> {
            CrearRolRequest body = JsonUtil.mapper().readValue(request.getBody().orElse("{}"), CrearRolRequest.class);
            return HttpResponseUtil.json(request, HttpStatus.CREATED, service.crear(body));
        });
    }

    @FunctionName("ListarRoles")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "roles",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        return handle(request, context, () -> HttpResponseUtil.json(request, HttpStatus.OK, service.listar()));
    }

    @FunctionName("ObtenerRol")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "roles/{id}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> HttpResponseUtil.json(request, HttpStatus.OK, service.obtener(id)));
    }

    @FunctionName("ActualizarRol")
    public HttpResponseMessage actualizar(
            @HttpTrigger(name = "req", methods = { HttpMethod.PUT }, route = "roles/{id}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> {
            ActualizarRolRequest body = JsonUtil.mapper().readValue(request.getBody().orElse("{}"),
                    ActualizarRolRequest.class);
            return HttpResponseUtil.json(request, HttpStatus.OK, service.actualizar(id, body));
        });
    }

    @FunctionName("EliminarRol")
    public HttpResponseMessage eliminar(
            @HttpTrigger(name = "req", methods = { HttpMethod.DELETE }, route = "roles/{id}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> {
            service.eliminar(id);
            return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
        });
    }

    private interface HandlerAction {
        HttpResponseMessage run() throws Exception;
    }

    private HttpResponseMessage handle(HttpRequestMessage<?> request, ExecutionContext context, HandlerAction action) {
        try {
            return action.run();
        } catch (ValidationException e) {
            return HttpResponseUtil.error(request, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NotFoundException e) {
            return HttpResponseUtil.error(request, HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ConflictException e) {
            return HttpResponseUtil.error(request, HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            context.getLogger().log(Level.SEVERE, "Error en RolFunctions", e);
            return HttpResponseUtil.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        }
    }
}
