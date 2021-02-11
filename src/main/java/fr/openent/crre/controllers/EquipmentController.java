package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.service.EquipmentService;
import fr.openent.crre.service.impl.DefaultEquipmentService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;
    private String query_word;
    private final HashMap<String, ArrayList<String>> query_filter;

    public EquipmentController() {
        super();
        this.equipmentService = new DefaultEquipmentService(Crre.crreSchema, "equipment");
        this.query_filter = new HashMap<>();
        this.query_word = "";
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
            equipmentService.searchAll(arrayResponseHandler(request));

        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
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
            String word = URLDecoder.decode(request.getParam("word"), "UTF-8");
            String filter = request.getParam("filter");
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
            } else {
                this.query_filter.put(filter, new ArrayList<String>(Arrays.asList(word)));
            }

            // empty filter
            if (this.query_filter.isEmpty()) {
                if(!(this.query_word == "")) {
                    equipmentService.searchFilter(this.query_filter, this.query_word, arrayResponseHandler(request));
                } else {
                    equipmentService.searchAll(arrayResponseHandler(request));
                }
            } else {
                if(!(this.query_word == "")) {
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
