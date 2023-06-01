package fr.openent.crre.controllers;

import com.opencsv.CSVReader;
import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.StructureGroupModel;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StructureGroupService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.DefaultStructureGroupService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

/**
 * Created by samuel jollois
 */
public class StructureGroupController extends ControllerHelper {

    private final StructureGroupService structureGroupService;
    private final StructureService structureService;
    private final Storage storage;

    public StructureGroupController(Storage storage, ServiceFactory serviceFactory) {
        super();
        this.structureGroupService = serviceFactory.getStructureGroupService();
        this.structureService = serviceFactory.getStructureService();
        this.storage = storage;
    }

    @Post("/structure/group/import")
    @ApiDoc("Import structure for a specific group")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void groupStructure(final HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!Field.OK.equals(entries.getString(Field.STATUS))) {
                renderError(request);
                return;
            }
            String fileId = entries.getString(Field._ID);
            String filename = entries.getJsonObject(Field.METADATA).getString(Field.FILENAME);
            parseCsv(request, fileId, filename);
        });
    }

    /**
     * Parse CSV file
     *
     * @param request  Http request
     * @param filename name of the file to import
     */
    private void parseCsv(final HttpServerRequest request, final String fileId, String filename) {
        storage.readFile(fileId, event -> {
            try {
                CSVReader csv = new CSVReader(new InputStreamReader(
                        new ByteArrayInputStream(event.getBytes())),
                        ';', '"', 1);
                String[] values;
                JsonArray uais = new JsonArray();

                while ((values = csv.readNext()) != null) {
                    uais.add(values[0]);
                }
                if (uais.size() > 0) {
                    matchUAIID(request, filename, uais);
                } else {
                    returnErrorMessage(request, new Throwable("crre.error.message.missing.uai"), filename);
                }
            } catch (IOException e) {
                log.error(String.format("[Crre@%s::parseCsv]: csv exception %s", this.getClass().getSimpleName(), e.getMessage()));
                returnErrorMessage(request, e.getCause(), filename);
            }
        });
    }

    /**
     * Match structure UAI with its Neo4j id.
     *
     * @param request Http request
     * @param path    Directory path
     * @param uais    UAIs list
     */
    private void matchUAIID(final HttpServerRequest request, final String path, JsonArray uais) {
        structureService.getStructureByUAI(uais, null, uaisEvent -> {
            if (uaisEvent.isRight()) {

                JsonArray data = uaisEvent.right().getValue();
                List<String> ids = new ArrayList<>();
                JsonObject o;
                Pattern regex = Pattern.compile("([a-zA-Z0-9\\s_\\\\.\\-\\(\\):])+(.csv)$");
                Matcher matcher = regex.matcher(path);
                String name = matcher.find() ? matcher.group(0).replace(".csv", "") : UUID.randomUUID().toString();
                for (int i = 0; i < data.size(); i++) {
                    o = data.getJsonObject(i);
                    String id = o.getString(Field.ID);
                    ids.add(id);
                }

                StructureGroupModel structureGroupModel = new StructureGroupModel()
                        .setDescription("")
                        .setName(name)
                        .setStructures(ids);

                structureGroupService.create(structureGroupModel)
                        .onSuccess(res -> {
                            Renders.renderJson(request, new JsonObject());
                            UserUtils.getUserInfos(eb, request,
                                    user -> Logging.add(Contexts.STRUCTUREGROUP.toString(),
                                            Actions.IMPORT.toString(), matcher.group(0), structureGroupModel.toJson(), user));
                        })
                        .onFailure(error -> returnErrorMessage(request, error, path));
            } else {
                returnErrorMessage(request, new Throwable(uaisEvent.left().getValue()), path);
            }
        });
    }

    /**
     * End http request and returns message error. It delete the directory.
     *
     * @param request Http request
     * @param cause   Throwable message
     * @param path    Directory path to delete
     */
    private void returnErrorMessage(HttpServerRequest request, Throwable cause, String path) {
        renderErrorMessage(request, cause);
        deleteImportPath(vertx, path);
    }

    /**
     * Render a message error based on cause message
     *
     * @param request Http request
     * @param cause   Cause error
     */
    private static void renderErrorMessage(HttpServerRequest request, Throwable cause) {
        renderError(request, new JsonObject().put(Field.MESSAGE, cause.getMessage()));
    }

    @Get("/structure/groups")
    @ApiDoc("List all groups of structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void list(final HttpServerRequest request) {
        structureGroupService.listStructureGroups()
                .onSuccess(structureGroupModels -> Renders.renderJson(request, IModelHelper.toJsonArray(structureGroupModels)))
                .onFailure(error -> Renders.renderError(request));
    }


    @Post("/structure/group")
    @ApiDoc("Create a group of Structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "structureGroup",
                structureGroupJson -> {
                    Future<JsonObject> future = structureGroupService.create(new StructureGroupModel(structureGroupJson));
                    Logging.defaultResponseFuture(eb, request, Contexts.STRUCTUREGROUP.toString(), Actions.CREATE.toString(), null, structureGroupJson, future);
                });
    }

    @Put("/structure/group/:id")
    @ApiDoc("Update a group of strctures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "structureGroup", structureGroupJson -> {
            try {
                Integer id = Integer.parseInt(request.params().get(Field.ID));
                Future<JsonObject> future = structureGroupService.update(id, new StructureGroupModel(structureGroupJson));
                Logging.defaultResponseFuture(eb, request, Contexts.STRUCTUREGROUP.toString(), Actions.UPDATE.toString(), null, structureGroupJson, future);
            } catch (NumberFormatException e) {
                log.error(String.format("[CRRE@%s::update] An error occured when casting structureGroup id %s", this.getClass().getSimpleName(), e.getMessage()));
                badRequest(request);
            }
        });
    }

    @Delete("/structure/group")
    @ApiDoc("Delete a group of Structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void delete(final HttpServerRequest request) {
        List<String> params = request.params().getAll(Field.ID);
        if (!params.isEmpty()) {
            try {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                Future<JsonObject> future = structureGroupService.delete(ids);
                Logging.defaultResponsesFuture(eb, request, Contexts.STRUCTUREGROUP.toString(), Actions.DELETE.toString(), params, null, future);
            } catch (NumberFormatException e) {
                log.error(String.format("[CRRE@%s::delete] An error occurred when casting group(s) of structures id(s) %s", this.getClass().getSimpleName(), e.getMessage()));
                badRequest(request);
            }
        } else {
            badRequest(request);
        }
    }
}
