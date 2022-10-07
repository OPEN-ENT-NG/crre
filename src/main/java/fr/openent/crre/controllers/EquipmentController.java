package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.security.AccessRight;
import fr.openent.crre.service.EquipmentService;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.impl.DefaultEquipmentService;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
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
import java.util.*;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;
    private final OrderRegionService orderRegionService;

    public EquipmentController() {
        super();
        this.equipmentService = new DefaultEquipmentService(Crre.crreSchema, "equipment");
        this.orderRegionService = new DefaultOrderRegionService("equipment");
    }

    @Get("/equipments")
    @ApiDoc("Get specific equipments ids")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    @Override
    public void list(HttpServerRequest request) {
        List<String> ids = request.params().getAll(Field.ID);
        List<String> idsInt = new ArrayList<>(ids);
        searchByIds(idsInt, arrayResponseHandler(request));
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
            equipmentService.equipment(idEquipment, handlerJsonArray(getEquipmentPromise));
            List<Future> promises = new ArrayList<>();
            promises.add(getEquipmentPromise.future());
            if(idStructure != null){
                orderRegionService.equipmentAlreadyPayed(idEquipment,idStructure, handlerJsonObject(alreadyPayedPromise));
                promises.add(alreadyPayedPromise.future());
            }
            CompositeFuture.all(promises).onComplete(event -> {
                if(event.succeeded()) {
                    JsonObject equipment = getEquipmentPromise.future().result().getJsonObject(0);
                    if(event.result().size()>1){
                        equipment.put("structure_already_payed",alreadyPayedPromise.future().result().getBoolean("exists"));
                    }
                    renderJson(request,equipment);
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
            getAllWithFilter(request, params);
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    private void getAllWithFilter(HttpServerRequest request, HashMap<String, ArrayList<String>> params) {
        equipmentService.filterWord(params, event -> {
            JsonArray ressources = event.right().getValue();
            JsonArray filtres = new JsonArray();
            JsonArray response = new JsonArray();
            Set<String> disciplines_set = new HashSet<>();
            Set<String> niveaux_set = new HashSet<>();
            Set<String> classes_set = new HashSet<>();
            Set<String> editeur_set = new HashSet<>();
            Set<String> public_set = new HashSet<>();
            Set<String> os_set = new HashSet<>();
            Set<String> distributeurs_set = new HashSet<>();
            for(int i = 0; i < ressources.size(); i++) {
                JsonObject ressource = ressources.getJsonObject(i);
                for(int j = 0; j < ressource.getJsonArray("disciplines").size(); j ++) {
                    disciplines_set.add(ressource.getJsonArray("disciplines").getJsonObject(j).getString("libelle"));
                }
                for(int j = 0; j < ressource.getJsonArray("niveaux").size(); j ++) {
                    niveaux_set.add(ressource.getJsonArray("niveaux").getJsonObject(j).getString("libelle"));
                }
                if(ressource.containsKey("classes")) {
                    for(int j = 0; j < ressource.getJsonArray("classes").size(); j ++) {
                        classes_set.add(ressource.getJsonArray("classes").getJsonObject(j).getString("libelle"));
                    }
                }
                if(ressource.containsKey("technos")) {
                    for(int j = 0; j < ressource.getJsonArray("technos").size(); j ++) {
                        os_set.add(ressource.getJsonArray("technos").getJsonObject(j).getString("technologie"));
                    }
                }
                if(ressource.getString("editeur") != null && !ressource.getString("editeur").equals("")) {
                    editeur_set.add(ressource.getString("editeur"));
                }

                if(ressource.getString("distributeur") != null && !ressource.getString("distributeur").equals("")) {
                    distributeurs_set.add(ressource.getString("distributeur"));
                }
                if(ressource.containsKey("publiccible")) {
                    public_set.add(ressource.getString("publiccible"));
                }
            }
            filtres.add(new JsonObject().put("subjects", new JsonArray(Arrays.asList(disciplines_set.toArray())))
                    .put("grades", new JsonArray(Arrays.asList(niveaux_set.toArray())))
                    .put("levels", new JsonArray(Arrays.asList(classes_set.toArray())))
                    .put("os", new JsonArray(Arrays.asList(os_set.toArray())))
                    .put("public", new JsonArray(Arrays.asList(public_set.toArray())))
                    .put("distributeurs", new JsonArray(Arrays.asList(distributeurs_set.toArray())))
                    .put("editors", new JsonArray(Arrays.asList(editeur_set.toArray()))));
            response.add(new JsonObject().put("ressources", ressources))
                    .add(new JsonObject().put("filters", filtres));
            renderJson(request, response);
        });
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
            if(!params.isEmpty()) {
                equipmentService.searchFilter(params, query_word, arrayResponseHandler(request));
            } else {
                equipmentService.searchWord(query_word, arrayResponseHandler(request));
            }
        } catch (ClassCastException | UnsupportedEncodingException e) {
            log.error("An error occurred searching article", e);
        }
    }

    @Get("/equipments/catalog/filter")
    @ApiDoc("Search an equipment by filter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void FilterEquipment(final HttpServerRequest request) {
        try {
            boolean emptyFilter = Boolean.parseBoolean(request.getParam("emptyFilter"));
            HashMap<String, ArrayList<String>> params = new HashMap<>();
            getFilterFromRequest(request, params);
            if (emptyFilter) {
                getAllWithFilter(request, params);
            } else {
                equipmentService.filterWord(params, arrayResponseHandler(request));
            }
        } catch (ClassCastException e) {
            log.error("An error occurred searching article", e);
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
