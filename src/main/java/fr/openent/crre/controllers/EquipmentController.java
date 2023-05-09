package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemFilterField;
import fr.openent.crre.core.enums.ResourceFieldEnum;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.model.item.Item;
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
            getCatalog(new FilterItemModel(), false)
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
            getCatalog(new FilterItemModel(), true)
                    .onSuccess(result -> Renders.renderJson(request, result))
                    .onFailure(error -> Renders.renderError(request));
    }

    private FilterItemModel buildFilterFromEquipment(List<Item> itemList) {
        FilterItemModel filters = new FilterItemModel();
        filters.setDisciplines(itemList.stream().flatMap(item -> item.getDisciplines().stream()).collect(Collectors.toList()));
        filters.setLevels(itemList.stream().flatMap(item -> item.getLevels().stream()).collect(Collectors.toList()));
        filters.setClasses(itemList.stream().flatMap(item -> item.getClasses().stream()).collect(Collectors.toList()));
        filters.setDevices(itemList.stream().flatMap(item -> JsonHelper.jsonArrayToList(item.getTechnos(), JsonObject.class).stream()
                .filter(techno -> techno.containsKey(Field.TECHNOLOGY))
                .map(techno -> techno.getString(Field.TECHNOLOGY))).collect(Collectors.toList()));
        filters.setEditors(itemList.stream().map(Item::getEditor).collect(Collectors.toList()));
        filters.setDistributors(itemList.stream().map(Item::getDistributor).collect(Collectors.toList()));
        filters.setTargets(itemList.stream().map(Item::getTarget).collect(Collectors.toList()));
        filters.setCatalogs(itemList.stream().map(Item::getCatalog).collect(Collectors.toList()));
        filters.setBooksellers(itemList.stream().map(Item::getBookSeller).collect(Collectors.toList()));

        return filters;
    }

    private Future<JsonObject> getCatalog(FilterItemModel filterItemModel, Boolean onlyFilter) {
        Promise<JsonObject> promise = Promise.promise();
        List<String> fields = Boolean.TRUE.equals(onlyFilter) ? Arrays.stream(ResourceFieldEnum.values()).map(ResourceFieldEnum::getValue).collect(Collectors.toList()) : null;
        equipmentService.filterWord(filterItemModel, fields, event -> {
            if (event.isRight()) {
                JsonArray resources = event.right().getValue();
                List<Item> itemList = IModelHelper.toList(resources, Item.class);

                JsonObject catalog = new JsonObject()
                        .put(Field.FILTERS, this.buildFilterFromEquipment(itemList).toJson())
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
            FilterItemModel filterItemModel = getFilterFromRequest(request);
            if (!filterItemModel.isEmpty()) {
                equipmentService.searchFilter(filterItemModel, null)
                        .onSuccess(result -> Renders.renderJson(request, new JsonObject().put(Field.RESOURCES, result)))
                        .onFailure(error -> {
                            Renders.renderError(request);
                            log.error(String.format("[CRRE@%s::SearchEquipment] Failed to get items filtered and searched %s:%s",
                                    this.getClass().getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
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
            FilterItemModel filterItemModel = getFilterFromRequest(request);
            if (emptyFilter) {
                getCatalog(filterItemModel, false)
                        .onSuccess(result -> Renders.renderJson(request, result))
                        .onFailure(error -> Renders.renderError(request));
            } else {
                equipmentService.filterWord(filterItemModel, null, event -> {
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

    private FilterItemModel getFilterFromRequest(HttpServerRequest request) {
        FilterItemModel filterItemModel = new FilterItemModel();
        if (request.params().contains(Field.EDITEUR)) {
            filterItemModel.setEditors(request.params().getAll(Field.EDITEUR));
        }
        if (request.params().contains(ItemFilterField.GRADES_FIELD)) {
            filterItemModel.setLevels(new ArrayList<>(request.params().getAll(ItemFilterField.GRADES_FIELD)));
        }
        if (request.params().contains(ItemFilterField.CLASSES_FIELD)) {
            filterItemModel.setClasses(new ArrayList<>(request.params().getAll(ItemFilterField.CLASSES_FIELD)));
        }
        if (request.params().contains(Field._INDEX)) {
            filterItemModel.setCatalogs(new ArrayList<>(request.params().getAll(Field._INDEX)));
        }
        if (request.params().contains(ItemFilterField.TARGET)) {
            filterItemModel.setTargets(new ArrayList<>(request.params().getAll(ItemFilterField.TARGET)));
        }
        if (request.params().contains(ItemFilterField.DISCIPLINES_FIELD)) {
            filterItemModel.setDisciplines(new ArrayList<>(request.params().getAll(ItemFilterField.DISCIPLINES_FIELD)));
        }
        if (request.params().contains(Field.CONSO)) {
            filterItemModel.setItemTypes(new ArrayList<>(request.params().getAll(Field.CONSO)));
        }
        if (request.params().contains(Field.PRO)) {
            filterItemModel.setStructureSectors(new ArrayList<>(request.params().getAll(Field.PRO)));
        }
        if (request.params().contains(Field.BOOKSELLERS)) {
            filterItemModel.setBooksellers(new ArrayList<>(request.params().getAll(Field.BOOKSELLERS)));
        }
        return filterItemModel;
    }
}
