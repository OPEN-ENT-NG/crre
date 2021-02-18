package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.service.EquipmentService;
import fr.openent.crre.service.impl.DefaultEquipmentService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;
    private String query_word;
    private final boolean haveFilter;
    private HashMap<String, ArrayList<String>> query_filter;

    public EquipmentController() {
        super();
        this.equipmentService = new DefaultEquipmentService(Crre.crreSchema, "equipment");
        this.query_filter = new HashMap<>();
        this.query_word = "";
        this.haveFilter = false;
    }

    @Get("/equipments")
    @ApiDoc("List all equipments in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @Override
    public void list(HttpServerRequest request) {
        List<String> orderIds = request.params().getAll("order_id");
        List<String> orderIdsInt = new ArrayList<>(orderIds);
        searchByIds(orderIdsInt, arrayResponseHandler(request));
    }

    @Get("/equipment/:id")
    @ApiDoc("Get an equipment")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void equipment(final HttpServerRequest request) {
        try {
            String idEquipment = request.params().contains("id")
                    ? request.params().get("id")
                    : null;
            equipmentService.equipment(idEquipment, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }
    @Get("/equipments/catalog")
    @ApiDoc("List equipments of campaign in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listEquipmentFromCampaign(final HttpServerRequest request) {
        try {
            searchAllWithFilter(request);

        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    private void searchAllWithFilter(HttpServerRequest request) {
        if(haveFilter) {
            equipmentService.searchAll(event -> {
                JsonArray ressources = event.right().getValue();
                JsonArray response = new JsonArray().add(new JsonObject().put("ressources", ressources));
                renderJson(request, response);
            });
        } else {
            equipmentService.searchAll(event -> {
                JsonArray ressources = event.right().getValue();
                JsonArray filtres = new JsonArray();
                JsonArray response = new JsonArray();
                Set<String> disciplines_set = new HashSet<>();
                Set<String> niveaux_set = new HashSet<>();
                Set<String> editeur_set = new HashSet<>();
                Set<String> public_set = new HashSet<>();
                Set<String> os_set = new HashSet<>();
                for(int i = 0; i < ressources.size(); i++) {
                    JsonObject ressource = ressources.getJsonObject(i);
                    for(int j = 0; j < ressource.getJsonArray("disciplines").size(); j ++) {
                        disciplines_set.add(ressource.getJsonArray("disciplines").getJsonObject(j).getString("libelle"));
                    }
                    for(int j = 0; j < ressource.getJsonArray("niveaux").size(); j ++) {
                        niveaux_set.add(ressource.getJsonArray("niveaux").getJsonObject(j).getString("libelle"));
                    }
                    if(ressource.containsKey("technos")) {
                        for(int j = 0; j < ressource.getJsonArray("technos").size(); j ++) {
                            os_set.add(ressource.getJsonArray("technos").getJsonObject(j).getString("technologie"));
                        }
                    }
                    if(ressource.getString("editeur") != null && !ressource.getString("editeur").equals("")) {
                        editeur_set.add(ressource.getString("editeur"));
                    }
                    if(ressource.containsKey("publiccible")) {
                        public_set.add(ressource.getString("publiccible"));
                    }
                }
                filtres.add(new JsonObject().put("disciplines", new JsonArray(Arrays.asList(disciplines_set.toArray())))
                                            .put("niveaux", new JsonArray(Arrays.asList(niveaux_set.toArray())))
                                            .put("os", new JsonArray(Arrays.asList(os_set.toArray())))
                                            .put("public", new JsonArray(Arrays.asList(public_set.toArray())))
                                            .put("editors", new JsonArray(Arrays.asList(editeur_set.toArray()))));
                response.add(new JsonObject().put("ressources", ressources))
                        .add(new JsonObject().put("filters", filtres));
                renderJson(request, response);
            });
            //this.haveFilter = true;
        }
    }

    @Get("/equipments/catalog/search")
    @ApiDoc("Search an equipment by keyword")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void SearchEquipment(final HttpServerRequest request) {
        try {
            this.query_word = URLDecoder.decode(request.getParam("word"), "UTF-8");
            if(!this.query_filter.isEmpty()) {
                equipmentService.searchFilter(this.query_filter, this.query_word, arrayResponseHandler(request));
            } else {
                equipmentService.searchWord(this.query_word, arrayResponseHandler(request));
            }
        } catch (ClassCastException | UnsupportedEncodingException e) {
            log.error("An error occurred searching article", e);
        }
    }

    @Get("/equipments/catalog/filter")
    @ApiDoc("Search an equipment by keyword")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void FilterEquipment(final HttpServerRequest request) throws UnsupportedEncodingException {
        try {
            String word = request.getParam("word");
            if(word != null)
                word = URLDecoder.decode(word, "UTF-8");
            String filter = request.getParam("filter");
            String emptyFilter = request.getParam("emptyFilter");
            if(emptyFilter != null && emptyFilter.equals("true")){
                this.query_filter = new HashMap<>();
                this.query_word = "";
            }
            if (this.query_filter.containsKey(filter)) {
                // filter already checked
                if (this.query_filter.get(filter).contains(word)) {
                    this.query_filter.get(filter).remove(word);
                    if (this.query_filter.get(filter).isEmpty()) {
                        this.query_filter.remove(filter);
                    }
                } else {
                    // new filter
                    this.query_filter.get(filter).add(word);
                }
            } else if(word != null) {
                this.query_filter.put(filter, new ArrayList<>(Arrays.asList(word)));
            }

            // empty filter
            if (this.query_filter.isEmpty()) {
                if(!(this.query_word.equals(""))) {
                    equipmentService.searchFilter(this.query_filter, this.query_word, arrayResponseHandler(request));
                } else {
                    searchAllWithFilter(request);
                }
            } else {
                if(!(this.query_word.equals(""))) {
                    equipmentService.searchFilter(this.query_filter, this.query_word, arrayResponseHandler(request));
                } else {
                    equipmentService.filterWord(this.query_filter, arrayResponseHandler(request));
                }
            }
        } catch (ClassCastException e) {
            log.error("An error occurred searching article", e);
        }
    }
}
