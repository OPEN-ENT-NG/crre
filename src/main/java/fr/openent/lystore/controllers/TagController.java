package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.TagService;
import fr.openent.lystore.service.impl.DefaultTagService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class TagController extends ControllerHelper {

    private final TagService tagService;

    public TagController() {
        super();
        this.tagService = new DefaultTagService(Lystore.lystoreSchema, "tag");
    }

    @Get("/tags")
    @ApiDoc("List all tags in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getAll(HttpServerRequest request) {
        tagService.getAll(arrayResponseHandler(request));
    }

    @Post("/tag")
    @ApiDoc("Create a tag")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "tag", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject tag) {
                tagService.create(tag, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.TAG.toString(),
                        Actions.CREATE.toString(),
                        null,
                        tag));
            }
        });
    }

    @Put("/tag/:id")
    @ApiDoc("Update a tag")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "tag", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject tag) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    tagService.update(id, tag, Logging.defaultResponseHandler(eb,
                            request,
                            Contexts.TAG.toString(),
                            Actions.UPDATE.toString(),
                            request.params().get("id"),
                            tag));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting tag id", e);
                    badRequest(request);
                }
            }
        });
    }

    @Delete("/tag")
    @ApiDoc("Delete a tag")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                tagService.delete(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.TAG.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting tag id", e);
            badRequest(request);
        }
    }

}
