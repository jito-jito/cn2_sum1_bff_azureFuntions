package com.empresa.functions.usuarios;

import com.empresa.functions.common.HttpResponseUtil;
import com.empresa.functions.common.JsonUtil;
import com.empresa.functions.common.exception.ConflictException;
import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.common.exception.ValidationException;
import com.empresa.functions.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.functions.usuarios.dto.AsignarRolRequest;
import com.empresa.functions.usuarios.dto.CrearUsuarioRequest;
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

public class UsuarioFunctions {

    private final UsuarioService service = new UsuarioService();

    @FunctionName("CrearUsuario")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "usuarios",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        return handle(request, context, () -> {
            CrearUsuarioRequest body = JsonUtil.mapper().readValue(request.getBody().orElse("{}"),
                    CrearUsuarioRequest.class);
            return HttpResponseUtil.json(request, HttpStatus.CREATED, service.crear(body));
        });
    }

    @FunctionName("ListarUsuarios")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "usuarios",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        return handle(request, context, () -> HttpResponseUtil.json(request, HttpStatus.OK, service.listar()));
    }

    @FunctionName("ObtenerUsuario")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "usuarios/{id}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> HttpResponseUtil.json(request, HttpStatus.OK, service.obtener(id)));
    }

    @FunctionName("ActualizarUsuario")
    public HttpResponseMessage actualizar(
            @HttpTrigger(name = "req", methods = { HttpMethod.PUT }, route = "usuarios/{id}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> {
            ActualizarUsuarioRequest body = JsonUtil.mapper().readValue(request.getBody().orElse("{}"),
                    ActualizarUsuarioRequest.class);
            return HttpResponseUtil.json(request, HttpStatus.OK, service.actualizar(id, body));
        });
    }

    @FunctionName("EliminarUsuario")
    public HttpResponseMessage eliminar(
            @HttpTrigger(name = "req", methods = { HttpMethod.DELETE }, route = "usuarios/{id}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> {
            service.eliminar(id);
            return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
        });
    }

    @FunctionName("AsignarRolAUsuario")
    public HttpResponseMessage asignarRol(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "usuarios/{id}/roles",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            ExecutionContext context) {
        return handle(request, context, () -> {
            AsignarRolRequest body = JsonUtil.mapper().readValue(request.getBody().orElse("{}"),
                    AsignarRolRequest.class);
            service.asignarRol(id, body.rolId());
            return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
        });
    }

    @FunctionName("QuitarRolDeUsuario")
    public HttpResponseMessage quitarRol(
            @HttpTrigger(name = "req", methods = { HttpMethod.DELETE }, route = "usuarios/{id}/roles/{rolId}",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") long id,
            @BindingName("rolId") long rolId,
            ExecutionContext context) {
        return handle(request, context, () -> {
            service.quitarRol(id, rolId);
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
            context.getLogger().log(Level.SEVERE, "Error en UsuarioFunctions", e);
            return HttpResponseUtil.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        }
    }
}
