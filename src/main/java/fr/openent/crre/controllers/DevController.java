package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DevController extends ControllerHelper {
    static private String txtStatus;
    static private Map<String, JsonArray> infoMap = new HashMap<>();

    public DevController() {
        txtStatus = generateExport(new JsonObject());
    }

    // Protect by dev mode
    @Get("/dev/library/status")
    // Allows you to retrieve the status of a bookseller
    public void getStatus(final HttpServerRequest request) {
        request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=libraryStatus.txt")
                .end(txtStatus);
    }

    // Protect by dev mode
    @Post("/dev/library/status")
    // Allows you to define the status of a bookseller
    public void setStatus(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            txtStatus = generateExport(body);
            Renders.ok(request);
        });
    }

    private static String generateExport(JsonObject body) {
        List<JsonObject> lines = body.getJsonArray(Field.DATA, new JsonArray()).stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("0|1|2|3|4|5|6|7|8|9|10|11|12|23|14|15|16|17|18|19|20|ETAT|ID_CGI\r");
        lines.forEach(line ->
                sb.append("0|1|2|3|4|5|6|7|8|9|10|11|12|23|14|15|16|17|18|19|20|")
                        .append(line.getString(Field.STATUS, ""))
                        .append("|")
                        .append(line.getString(Field.ID, ""))
                        .append("\r"));
        return sb.toString();
    }

    // Protect by dev mode
    @Get("/dev/library/info/:id")
    // Allows you to retrieve the status of a bookseller
    public void getInfo(final HttpServerRequest request) {
        Renders.renderJson(request, infoMap.getOrDefault(request.params().get(Field.ID), new JsonArray()));
    }

    // Protect by dev mode
    @Post("/dev/library/info/:id")
    // Permet de récupérer les information d'un libraire a moissoner
    public void setInfo(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            infoMap.put(request.params().get(Field.ID), body.getJsonArray(Field.DATA, new JsonArray()));
            Renders.ok(request);
        });
    }
}
