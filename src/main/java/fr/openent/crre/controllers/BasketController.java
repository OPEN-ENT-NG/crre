package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.*;
import fr.openent.crre.service.BasketService;
import fr.openent.crre.service.impl.DefaultBasketService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.*;
import static fr.openent.crre.helpers.ElasticSearchHelper.filter_waiting;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;

public class BasketController extends ControllerHelper {
    private final BasketService basketService;

    public BasketController() {
        super();
        this.basketService = new DefaultBasketService(Crre.crreSchema, "basket");
    }
    @Get("/basket/:idCampaign/:idStructure")
    @ApiDoc("List baskets of a campaign and a structure")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer idCampaign = request.params().contains("idCampaign")
                        ? parseInt(request.params().get("idCampaign"))
                        : null;
                String idStructure = request.params().contains("idStructure")
                        ? request.params().get("idStructure")
                        : null;
                basketService.listBasket(idCampaign, idStructure, user, baskets -> {
                    if(baskets.isRight()) {
                        JsonArray basketsResult = new JsonArray();
                        List<String> listIdsEquipment = new ArrayList<>();
                        for(Object bask : baskets.right().getValue()){
                            JsonObject basket = (JsonObject) bask;
                            basketsResult.add(basket);
                            listIdsEquipment.add(basket.getString("id_equipment"));
                        }
                        searchByIds(listIdsEquipment, equipments -> {
                            if (equipments.isRight()) {
                                for (Object bask : basketsResult) {
                                    JsonObject basket = (JsonObject) bask;
                                    String idEquipment = basket.getString("id_equipment");
                                    for (Object equipment : equipments.right().getValue()) {
                                        JsonObject equipmentJson = (JsonObject) equipment;
                                        if (idEquipment.equals(equipmentJson.getString("id"))) {
                                            basket.put("equipment",equipment);
                                        }
                                    }
                                }
                                renderJson(request, basketsResult);
                            } else {
                                log.error(equipments.left());
                                badRequest(request);
                            }
                        });
                    } else {
                        log.error("An error occurred getting basket", baskets.left());
                        badRequest(request);
                    }
                });
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/allMyOrders")
    @ApiDoc("Get all my baskets orders")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getMyBasketOrders(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                int id_campaign = parseInt(request.getParam("id"));
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                Boolean old = Boolean.valueOf(request.getParam("old"));
                basketService.getMyBasketOrders(user, page, id_campaign, startDate, endDate, old, arrayResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && !request.params().get("q").trim().isEmpty()) {
                try {
                    Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                    int id_campaign = parseInt(request.getParam("id"));
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    Boolean old = Boolean.valueOf(request.getParam("old"));
                    if(!old) {
                        plainTextSearchName(query, equipments -> {
                            if(equipments.right().getValue().size() > 0) {
                                basketService.search(query, null, user, equipments.right().getValue(), id_campaign, startDate, endDate, page, old, arrayResponseHandler(request));
                            } else {
                                basketService.searchWithoutEquip(query, null, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                            }
                        });
                    } else {
                        basketService.search(query, null, user, null, id_campaign, startDate, endDate, page, old, arrayResponseHandler(request));
                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                badRequest(request);
            }
        });
    }

/*    @Get("/basketOrder/filter")
    @ApiDoc("Filter order")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void filter(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
*//*                List<String> params = new ArrayList<>();*//*
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                Boolean old = Boolean.valueOf(request.getParam("old"));
                String q = ""; // Query pour chercher sur le nom du panier, le nom de la ressource ou le nom de l'enseignant
*//*                if (request.params().contains("niveaux.libelle")) {
                    params = request.params().getAll("niveaux.libelle");
                }*//*

                // Récupération de tout les filtres hors grade
                JsonArray filters = new JsonArray();
                int length = request.params().entries().size();
                for (int i = 0; i < length; i++) {
                    if (!request.params().entries().get(i).getKey().equals("id") && !request.params().entries().get(i).getKey().equals("q")
                            && !request.params().entries().get(i).getKey().equals("niveaux.libelle"))
                        filters.add(new JsonObject().put(request.params().entries().get(i).getKey(), request.params().entries().get(i).getValue()));
                }
                // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
                if (request.params().contains("q")) {
                    q = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                }
                int id_campaign = parseInt(request.getParam("id"));
                String finalQ = q;
                // Si nous avons des filtres de grade
                    if(!old) {
                        CompositeFuture.all(equipmentGradeFuture, equipmentGradeAndQFuture).setHandler(event -> {
                            if (event.succeeded()) {
                                JsonArray equipmentsGrade = equipmentGradeFuture.result(); // Tout les équipements correspondant aux grades
                                JsonArray equipmentsGradeAndQ = equipmentGradeAndQFuture.result(); // Tout les équipement correspondant aux grades et à la query
                                JsonArray allEquipments = new JsonArray();
                                allEquipments.add(equipmentsGrade);
                                allEquipments.add(equipmentsGradeAndQ);
                                // Si le tableau trouve des equipements, on recherche avec ou sans query sinon ou cherche sans equipement
                                if (equipmentsGrade.size() > 0) {
                                    if (request.params().contains("q")) {
                                        basketService.searchWithAll(finalQ, filters, user, allEquipments, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                    } else {
                                        basketService.filter(filters, user, equipmentsGrade, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                    }
                                } else {
                                    basketService.searchWithoutEquip(finalQ, filters, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                }
                            }
                        });
                        filter_waiting(null, handlerJsonArray(equipmentGradeFuture));
                        filter_waiting(StringUtils.isEmpty(q) ? null : q, handlerJsonArray(equipmentGradeAndQFuture));
                    } else {

                    }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }*/

    @Post("/basket/campaign/:idCampaign")
    @ApiDoc("Create a basket item")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, pathPrefix + "basket",
                    basket -> basketService.create(basket, user, defaultResponseHandler(request) ));

        });
    }

    @Delete("/basket/:idBasket/campaign/:idCampaign")
    @ApiDoc("Delete a basket item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void delete(HttpServerRequest request) {
        try {
            Integer idBasket = request.params().contains("idBasket")
                    ? parseInt(request.params().get("idBasket"))
                    : null;
            basketService.delete( idBasket, defaultResponseHandler(request));

        } catch (ClassCastException e) {
            log.error("An error occurred when casting basket id", e);
            badRequest(request);
        }
    }


    @Put("/basket/:idBasket/amount")
    @ApiDoc("Update a basket's amount")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void updateAmount(final HttpServerRequest  request){
        RequestUtils.bodyToJson(request, pathPrefix + "basket", basket -> {
            try {
                Integer id = parseInt(request.params().get("idBasket"));
                Integer amount = basket.getInteger("amount") ;
                basketService.updateAmount(id, amount, defaultResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred when casting basket id", e);
            }
        });
    }

    @Put("/basket/:idBasket/comment")
    @ApiDoc("Update a basket's comment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateComment(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, basket -> {
            if (!basket.containsKey("comment")) {
                badRequest(request);
                return;
            }
            try {
                Integer id = parseInt(request.params().get("idBasket"));
                String comment = basket.getString("comment");
                basketService.updateComment(id, comment, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });
    }

    @Put("/basket/:idBasket/reassort")
    @ApiDoc("Update a basket's reassort")
    @SecuredAction(Crre.REASSORT_RIGHT)
    @ResourceFilter(AccessOrderReassortRight.class)
        public void updateReassort(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, basket -> {
            if (!basket.containsKey("reassort")) {
                badRequest(request);
                return;
            }
            try {
                Integer id = parseInt(request.params().get("idBasket"));
                Boolean reassort = basket.getBoolean("reassort");
                basketService.updateReassort(id, reassort, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });
    }

    @Post("/baskets/to/orders/:idCampaign")
    @ApiDoc("Create an order list from basket")
    @SecuredAction(Crre.PRESCRIPTOR_RIGHT)
    @ResourceFilter(PrescriptorRight.class)
    public void takeOrder(final HttpServerRequest  request){
        RequestUtils.bodyToJson( request, pathPrefix + "basketToOrder", object -> {
            try {
                final Integer idCampaign = parseInt(request.params().get("idCampaign"));
                final String idStructure = object.getString("id_structure");
                final String nameStructure = object.getString("structure_name");
                final String nameBasket = object.getString("basket_name");
                JsonArray baskets = object.containsKey("baskets") ? object.getJsonArray("baskets") : new JsonArray();
                basketService.listebasketItemForOrder(idCampaign, idStructure, baskets,
                        listBasket -> {
                            if(listBasket.isRight() && listBasket.right().getValue().size() > 0){
                                UserUtils.getUserInfos(eb, request, user ->
                                        basketService.takeOrder(request , listBasket.right().getValue(),
                                                idCampaign, user, idStructure, nameStructure, baskets, nameBasket,
                                                Logging.defaultCreateResponsesHandler(eb,
                                                        request,
                                                        Contexts.ORDER.toString(),
                                                        Actions.CREATE.toString(),
                                                        "id_order",
                                                        listBasket.right().getValue())));
                            }else{
                                log.error("An error occurred when listing Baskets");
                                badRequest(request);
                            }
                        });

            } catch (ClassCastException e) {
                log.error("An error occurred when casting Basket information", e);
                renderError(request);
            }
        });
    }
}
