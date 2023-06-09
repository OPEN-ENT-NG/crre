package fr.openent.crre.controllers;

import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.LogService;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.openent.crre.core.constants.Field.UTF8_BOM;

public class LogController extends ControllerHelper {
    private final LogService logService;

    public LogController(ServiceFactory serviceFactory) {
        this.logService = serviceFactory.getLogService();
    }

    @Get("/logs")
    @ApiDoc("List all Logs")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void list (final HttpServerRequest request) {
        try {
            Integer page = request.params().contains("page")
                ? Integer.parseInt(request.params().get("page"))
                : null;
            logService.list(page, res -> {
                if (res.isRight()) {
                    final JsonArray logs = res.right().getValue();
                    logService.getLogsNumber(event -> {
                        if (event.isRight()) {
                            Integer numberLogs = event.right().getValue().getInteger("number_logs");
                            JsonObject response = new JsonObject();
                            response.put("number_logs", numberLogs)
                                    .put("logs", logs);
                            renderJson(request, response, 200);
                        } else {
                            log.error("An error occurred when collecting numbers of log");
                            renderError(request);
                        }
                    });
                } else {
                    log.error("An error occurred when collecting logs");
                    renderError(request);
                }
            });
        } catch (ClassCastException e) {
            log.error("An error occurred when casting page number", e);
        }
    }

    @Get("/logs/export")
    @ApiDoc("Export logs as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void export (final HttpServerRequest request) {
        logService.list(null, event -> {
            if (event.isRight()) {
                request.response()
                        .putHeader("Content-Type", "text/csv; charset=utf-8")
                        .putHeader("Content-Disposition", "attachment; filename=logs.csv")
                        .end(generateExport(request, event.right().getValue()));
            } else {
                badRequest(request);
            }
        });
    }

    private static String generateExport(HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
          report.append(generateExportLine(request, logs.getJsonObject(i)));
        }
        return report.toString();
    }

    private static String getExportHeader (HttpServerRequest request) {
        return I18n.getInstance().translate("crre.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("user", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("action", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("context", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("resource", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("value", getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    private static String generateExportLine (HttpServerRequest request, JsonObject log) {
        return log.getString("date") + ";" +
                log.getString("username") + ";" +
                log.getString("action") + ";" +
                I18n.getInstance().translate(log.getString("context"), getHost(request),
                        I18n.acceptLanguage(request)) + ";" +
                log.getString("item") + ";" +
                (log.getString("value") != null ? log.getString("value").replace("\\\"", "\"") : "")
                + "\n";
    }
}
