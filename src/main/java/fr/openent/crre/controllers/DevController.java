package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.service.ServiceFactory;
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
    static private final Map<String, String> txtStatusMap = new HashMap<>();
    static private final Map<String, JsonArray> infoMap = new HashMap<>();
    private static final String LDE_CAT_PAP = "https://www.lde.fr/4dlink1/4DCGI/IDF/json_cat_pap.json";
    private static final String LDE_CAT_NUM = "https://www.lde.fr/4dlink1/4DCGI/IDF/json_cat_num.json";
    private final ServiceFactory serviceFactory;

    public DevController(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    // Protect by dev mode
    @Get("/dev/bookseller/status/:bookseller")
    // Allows you to retrieve the status of a bookseller
    public void getStatus(final HttpServerRequest request) {
        request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=libraryStatus.txt")
                .end(txtStatusMap.getOrDefault(request.params().get(Field.BOOKSELLER), generateExport(new JsonObject())));
    }

    // Protect by dev mode
    @Post("/dev/bookseller/status/:bookseller")
    // Allows you to define the status of a bookseller
    public void setStatus(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            txtStatusMap.put(request.params().get(Field.BOOKSELLER), generateExport(body));
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
    @Get("/dev/bookseller/info/:bookseller")
    // Allows you to retrieve the status of a bookseller
    public void getInfo(final HttpServerRequest request) {
        // For dev environments only, when there is no value, the LDE values are taken by default
        if (Field.PAPIER.equals(request.params().get(Field.BOOKSELLER)) && (!infoMap.containsKey(request.params().get(Field.BOOKSELLER)) || infoMap.get(request.params().get(Field.BOOKSELLER)).isEmpty())) {
            this.serviceFactory.getWebClient().getAbs(LDE_CAT_PAP)
                            .send(httpResponseAsyncResult -> {
                                if (httpResponseAsyncResult.succeeded()) {
                                    request.response().setStatusCode(200)
                                            .headers().addAll(httpResponseAsyncResult.result().headers());
                                    request.response().write(httpResponseAsyncResult.result().bodyAsString());
                                } else {
                                    Renders.renderError(request);
                                }
                            });
            return;
        }

        // For dev environments only, when there is no value, the LDE values are taken by default
        if (Field.NUMERIC.equals(request.params().get(Field.BOOKSELLER)) && (!infoMap.containsKey(request.params().get(Field.BOOKSELLER)) || infoMap.get(request.params().get(Field.BOOKSELLER)).isEmpty())) {
            this.serviceFactory.getWebClient().getAbs(LDE_CAT_NUM)
                    .send(httpResponseAsyncResult -> {
                        if (httpResponseAsyncResult.succeeded()) {
                            request.response().setStatusCode(200)
                                    .headers().addAll(httpResponseAsyncResult.result().headers());
                            request.response().write(httpResponseAsyncResult.result().bodyAsString());
                        } else {
                            Renders.renderError(request);
                        }
                    });
            return;
        }

        Renders.renderJson(request, infoMap.getOrDefault(request.params().get(Field.BOOKSELLER), new JsonArray()));
    }

    // Protect by dev mode
    @Post("/dev/bookseller/info/:bookseller")
    // Permet de récupérer les information d'un libraire a moissoner
    public void setInfo(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            infoMap.put(request.params().get(Field.BOOKSELLER), body.getJsonArray(Field.DATA, new JsonArray()));
            Renders.ok(request);
        });
    }
}
