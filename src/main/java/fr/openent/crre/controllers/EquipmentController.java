package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemFilterField;
import fr.openent.crre.core.enums.ResourceFieldEnum;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.security.AccessRight;
import fr.openent.crre.service.EquipmentService;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerEitherPromise;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;
    private final OrderRegionService orderRegionService;

    public EquipmentController(ServiceFactory serviceFactory) {
        super();
        this.equipmentService = serviceFactory.getEquipmentService();
        this.orderRegionService = serviceFactory.getOrderRegionService();
    }

    @Get("/equipments")
    @ApiDoc("Get specific equipments ids")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    @Override
    public void list(HttpServerRequest request) {
        List<String> ids = request.params().getAll(Field.ID);
        List<String> idsInt = new ArrayList<>(ids);
        searchByIds(idsInt, null, arrayResponseHandler(request));
    }

    @Get("/equipment/:id")
    @ApiDoc("Get an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void equipment(final HttpServerRequest request) {
        try {
            String idEquipment = request.params().contains(Field.ID)
                    ? request.params().get(Field.ID)
                    : null;
            String idStructure = request.params().contains("idStructure")
                    ? request.params().get("idStructure")
                    : null;
            Promise<JsonArray> getEquipmentPromise = Promise.promise();
            Promise<JsonObject> alreadyPayedPromise = Promise.promise();
            equipmentService.equipment(idEquipment, null, handlerEitherPromise(getEquipmentPromise));
            List<Future> promises = new ArrayList<>();
            promises.add(getEquipmentPromise.future());
            if (idStructure != null) {
                orderRegionService.equipmentAlreadyPayed(idEquipment, idStructure, handlerJsonObject(alreadyPayedPromise));
                promises.add(alreadyPayedPromise.future());
            }
            CompositeFuture.all(promises).onComplete(event -> {
                if (event.succeeded()) {
                    JsonObject equipment = getEquipmentPromise.future().result().getJsonObject(0);
                    if (event.result().size() > 1) {
                        equipment.put("structure_already_payed", alreadyPayedPromise.future().result().getBoolean("exists"));
                    }
                    renderJson(request, equipment);
                } else {
                    log.error("Problem to catch equipment with his id");
                    badRequest(request);
                }
            });
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Get("/equipments/catalog")
    @ApiDoc("List equipments of campaign in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void listEquipmentFromCampaign(final HttpServerRequest request) {
        try {
            HashMap<String, ArrayList<String>> params = new HashMap<>();
            getCatalog(params, false)
                    .onSuccess(result -> Renders.renderJson(request, result))
                    .onFailure(error -> Renders.renderError(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Get("/equipments/catalog/filters")
    @ApiDoc("List of item's filters")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void listItemsFilters(final HttpServerRequest request) {
            getCatalog(new HashMap<>(), true)
                    .onSuccess(result -> Renders.renderJson(request, result))
                    .onFailure(error -> Renders.renderError(request));
    }

    private Future<JsonObject> getCatalog(HashMap<String, ArrayList<String>> params, Boolean onlyFilter) {
        Promise<JsonObject> promise = Promise.promise();
        List<String> fields = Boolean.TRUE.equals(onlyFilter) ? Arrays.stream(ResourceFieldEnum.values()).map(ResourceFieldEnum::getValue).collect(Collectors.toList()) : null;
        equipmentService.filterWord(params, fields, event -> {
            if (event.isRight()) {
                JsonArray resources = event.right().getValue();
                FilterItemModel filters = new FilterItemModel();
                for (int i = 0; i < resources.size(); i++) {
                    JsonObject resource = resources.getJsonObject(i);
                    filters.getDisciplines().addAll(JsonHelper.jsonArrayToList(resource.getJsonArray(Field.DISCIPLINES), JsonObject.class)
                            .stream()
                            .map(discipline -> discipline.getString(Field.LIBELLE))
                            .collect(Collectors.toList()));

                    filters.getGrades().addAll(JsonHelper.jsonArrayToList(resource.getJsonArray(Field.NIVEAUX), JsonObject.class)
                            .stream()
                            .map(grade -> grade.getString(Field.LIBELLE))
                            .collect(Collectors.toList()));

                    if (resource.getJsonArray(Field.CLASSES) != null) {
                        filters.getClasses().addAll(JsonHelper.jsonArrayToList(resource.getJsonArray(Field.CLASSES), JsonObject.class)
                                .stream()
                                .map(c -> c.getString(Field.LIBELLE))
                                .collect(Collectors.toList()));
                    }

                    if (resource.getJsonArray(Field.TECHNOS) != null) {
                        filters.getDevices().addAll(JsonHelper.jsonArrayToList(resource.getJsonArray(Field.TECHNOS), JsonObject.class)
                                .stream()
                                .map(techno -> techno.getString(Field.TECHNOLOGY))
                                .collect(Collectors.toList()));
                    }

                    if (resource.getString(Field.EDITEUR) != null && !resource.getString(Field.EDITEUR).equals("")) {
                        filters.getEditors().add(resource.getString(Field.EDITEUR));
                    }

                    if (resource.getString(Field.DISTRIBUTEUR) != null && !resource.getString(Field.DISTRIBUTEUR).equals("")) {
                        filters.getDistributors().add(resource.getString(Field.DISTRIBUTEUR));
                    }

                    if (resource.getString(ItemFilterField.TARGET) != null && !resource.getString(ItemFilterField.TARGET).equals("")) {
                        filters.getTargets().add(resource.getString(ItemFilterField.TARGET));
                    }

                    if (resource.getString(ItemFilterField.TARGET) != null && !resource.getString(ItemFilterField.TARGET).equals("")) {
                        filters.getTargets().add(resource.getString(ItemFilterField.TARGET));
                    }

                    if (resource.getString(Field.TYPE) != null && !resource.getString(Field.TYPE).equals("")) {
                        filters.getCatalogs().add(resource.getString(Field.TYPE));
                    }
                }

                JsonObject catalog = new JsonObject()
                        .put(Field.FILTERS, filters.toJson())
                        .put(Field.RESOURCES, onlyFilter ? new JsonArray() : resources);
                promise.complete(catalog);
            } else {
                log.error(String.format("[CRRE@%s::getCatalog] Fail to get catalog data %s",
                        this.getClass().getSimpleName(), event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        });
        return promise.future();
    }


    @Get("/equipments/catalog/search")
    @ApiDoc("Search an equipment by keyword")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void SearchEquipment(final HttpServerRequest request) {
        try {
            String query_word = URLDecoder.decode(request.getParam("word"), "UTF-8");
            HashMap<String, ArrayList<String>> params = new HashMap<>();
            getFilterFromRequest(request, params);
            if (!params.isEmpty()) {
                equipmentService.searchFilter(params, query_word, null, event -> {
                    if(event.isRight()) {
                        Renders.renderJson(request, new JsonObject().put(Field.RESOURCES, event.right().getValue()));
                    } else {
                        log.error(String.format("[CRRE@%s::SearchEquipment] Failed to get items filtered and searched %s",
                                this.getClass().getSimpleName(), event.left().getValue()));
                        Renders.renderError(request);
                    }
                });
            } else {
                equipmentService.searchWord(query_word, null, event -> {
                    if(event.isRight()) {
                        Renders.renderJson(request, new JsonObject().put(Field.RESOURCES, event.right().getValue()));
                    } else {
                        log.error(String.format("[CRRE@%s::SearchEquipment] Failed to get items searched %s",
                                this.getClass().getSimpleName(), event.left().getValue()));
                        Renders.renderError(request);
                    }
                });
            }
        } catch (ClassCastException | UnsupportedEncodingException e) {
            log.error(String.format("[CRRE@%s::SearchEquipment] Failed to get items searched %s",
                    this.getClass().getSimpleName(), e.getMessage()));
            Renders.renderError(request);
        }
    }

    @Get("/equipments/catalog/filter")
    @ApiDoc("Search an equipment by filter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void FilterEquipment(final HttpServerRequest request) {
        try {
            boolean emptyFilter = Boolean.parseBoolean(request.getParam(Field.EMPTY_FILTER));
            HashMap<String, ArrayList<String>> params = new HashMap<>();
            getFilterFromRequest(request, params);
            if (emptyFilter) {
                getCatalog(params, false)
                        .onSuccess(result -> Renders.renderJson(request, result))
                        .onFailure(error -> Renders.renderError(request));
            } else {
                equipmentService.filterWord(params, null, event -> {
                    if(event.isRight()) {
                        Renders.renderJson(request, new JsonObject().put(Field.RESOURCES, event.right().getValue()));
                    } else {
                        log.error(String.format("[CRRE@%s::FilterEquipment] Failed to get items filtered %s",
                                this.getClass().getSimpleName(), event.left().getValue()));
                        Renders.renderError(request);
                    }
                });
            }
        } catch (ClassCastException e) {
            log.error(String.format("[CRRE@%s::FilterEquipment] Failed to get items searched and filtered %s",
                    this.getClass().getSimpleName(), e.getMessage()));
            Renders.renderError(request);
        }
    }

    private void getFilterFromRequest(HttpServerRequest request, HashMap<String, ArrayList<String>> params) {
        if (request.params().contains("editeur")) {
            params.put("editeur", new ArrayList<>(request.params().getAll("editeur")));
        }
        if (request.params().contains("niveaux.libelle")) {
            params.put("niveaux.libelle", new ArrayList<>(request.params().getAll("niveaux.libelle")));
        }
        if (request.params().contains("classes.libelle")) {
            params.put("classes.libelle", new ArrayList<>(request.params().getAll("classes.libelle")));
        }
        if (request.params().contains("_index")) {
            params.put("_index", new ArrayList<>(request.params().getAll("_index")));
        }
        if (request.params().contains("publiccible")) {
            params.put("publiccible", new ArrayList<>(request.params().getAll("publiccible")));
        }
        if (request.params().contains("disciplines.libelle")) {
            params.put("disciplines.libelle", new ArrayList<>(request.params().getAll("disciplines.libelle")));
        }
        if (request.params().contains("conso")) {
            params.put("conso", new ArrayList<>(request.params().getAll("conso")));
        }
        if (request.params().contains("pro")) {
            params.put("pro", new ArrayList<>(request.params().getAll("pro")));
        }
    }
}
