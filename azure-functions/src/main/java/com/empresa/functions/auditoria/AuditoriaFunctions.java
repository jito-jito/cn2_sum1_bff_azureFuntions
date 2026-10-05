package com.empresa.functions.auditoria;

import com.empresa.functions.common.HttpResponseUtil;
import com.empresa.functions.common.exception.ValidationException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class AuditoriaFunctions {

    private final AuditoriaService service = new AuditoriaService();

    @FunctionName("ListarAuditoria")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "auditoria",
                    authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        Map<String, String> query = request.getQueryParameters();
        try {
            return HttpResponseUtil.json(request, HttpStatus.OK,
                    service.listar(query.get("entidad"), query.get("entidadId"), query.get("limit")));
        } catch (ValidationException e) {
            return HttpResponseUtil.error(request, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            context.getLogger().log(Level.SEVERE, "Error en AuditoriaFunctions", e);
            return HttpResponseUtil.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
        }
    }
}
