package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.*;
import fr.openent.crre.service.BasketService;
import fr.openent.crre.service.ServiceFactory;
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
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;

public class BasketController extends ControllerHelper {
    private final BasketService basketService;

    public BasketController(ServiceFactory serviceFactory) {
        super();
        this.basketService = serviceFactory.getBasketService();
    }

    @Get("/basket/:idCampaign/:idStructure")
    @ApiDoc("List baskets of the user in a campaign and a structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
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
                    if (baskets.isRight()) {
                        JsonArray basketsResult = new JsonArray();
                        List<String> listIdsEquipment = new ArrayList<>();
                        for (Object bask : baskets.right().getValue()) {
                            JsonObject basket = (JsonObject) bask;
                            basketsResult.add(basket);
                            listIdsEquipment.add(basket.getString("id_equipment"));
                        }
                        searchByIds(listIdsEquipment, equipments -> {
                            if (equipments.isRight()) {
                                for (Object bask : basketsResult) {
                                    JsonObject basket = (JsonObject) bask;
                                    String idEquipment = basket.getString("id_equipment");
                                    JsonArray equipmentsArray = equipments.right().getValue();
                                    if (equipmentsArray.size() > 0) {
                                        for (int i = 0; i < equipmentsArray.size(); i++) {
                                            JsonObject equipment = equipmentsArray.getJsonObject(i);
                                            if (idEquipment.equals(equipment.getString(Field.ID))) {
                                                basket.put("equipment", equipment);
                                                break;
                                            } else if(equipmentsArray.size() - 1 == i) {
                                                JsonObject equipmentDefault = new JsonObject();
                                                equipmentDefault.put("urlcouverture", "/crre/public/img/pages-default.png");
                                                equipmentDefault.put("disponibilite", new JsonArray().add(new JsonObject().put("valeur", "Non disponible à long terme")));
                                                equipmentDefault.put("titre", "Manuel introuvable dans le catalogue");
                                                equipmentDefault.put("ean", idEquipment);
                                                equipmentDefault.put("inCatalog", false);
                                                equipmentDefault.put("price", 0.0);
                                                basket.put("equipment", equipmentDefault);
                                            }
                                        }
                                    } else {
                                        JsonObject equipmentDefault = new JsonObject();
                                        equipmentDefault.put("urlcouverture", "/crre/public/img/pages-default.png");
                                        equipmentDefault.put("disponibilite", new JsonArray().add(new JsonObject().put("valeur", "Non disponible à long terme")));
                                        equipmentDefault.put("titre", "Manuel introuvable dans le catalogue");
                                        equipmentDefault.put("ean", idEquipment);
                                        equipmentDefault.put("inCatalog", false);
                                        equipmentDefault.put("price", 0.0);
                                        basket.put("equipment", equipmentDefault);
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
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void getMyBasketOrders(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                Integer id_campaign = request.params().contains("idCampaign") ? parseInt(request.params().get("idCampaign")) : null;
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                boolean old = Boolean.parseBoolean(request.getParam("old"));
                basketService.getMyBasketOrders(user, page, id_campaign, startDate, endDate, old, arrayResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && !request.params().get("q").trim().isEmpty()) {
                try {
                    Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                    int id_campaign = parseInt(request.getParam("idCampaign"));
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    Boolean old = Boolean.valueOf(request.getParam("old"));
                    plainTextSearchName(query, equipments -> basketService.search(query, user,
                            equipments.right().getValue(), id_campaign, startDate, endDate, page, old,
                            arrayResponseHandler(request)));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                badRequest(request);
            }
        });
    }

    @Post("/basket/campaign/:idCampaign")
    @ApiDoc("Create a basket item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, pathPrefix + "basket",
                    basket -> basketService.create(basket, user, defaultResponseHandler(request)));

        });
    }

    @Post("/baskets/campaign")
    @ApiDoc("Create a baskets item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void createBaskets(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> RequestUtils.bodyToJsonArray(request, baskets -> {
            List<Future> futures = new ArrayList<>();
            for (Object basket : baskets) {
                Future<JsonObject> basketFuture = Future.future();
                futures.add(basketFuture);
                basketService.create((JsonObject) basket, user, handlerJsonObject(basketFuture));
            }
            CompositeFuture.all(futures).setHandler(event -> {
                if (event.succeeded()) {
                    JsonArray result = new JsonArray();
                    for (Object f : futures) {
                        result.add(((Future<JsonObject>) f).result());
                    }
                    renderJson(request, result);
                } else {
                    log.error("[CRRE@BasketController@createBaskets] error in future baskets : " + event.cause());
                    badRequest(request);
                }
            });
        }));
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
            basketService.delete(idBasket, defaultResponseHandler(request));

        } catch (ClassCastException e) {
            log.error("An error occurred when casting basket id", e);
            badRequest(request);
        }
    }


    @Put("/basket/:idBasket/amount")
    @ApiDoc("Update a basket's amount")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void updateAmount(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> RequestUtils.bodyToJson(request, pathPrefix + "basket", basket -> {
            try {
                Integer id = parseInt(request.params().get("idBasket"));
                Integer amount = basket.getInteger("amount");
                basketService.updateAmount(user, id, amount, defaultResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred when casting basket id", e);
            }
        }));
    }

    @Put("/basket/:idBasket/comment")
    @ApiDoc("Update a basket's comment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateComment(final HttpServerRequest request) {
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
    public void updateReassort(final HttpServerRequest request) {
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
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void takeOrder(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "basketToOrder", object -> {
            try {
                final Integer idCampaign = parseInt(request.params().get("idCampaign"));
                final String idStructure = object.getString("id_structure");
                final String nameStructure = object.getString("structure_name");
                final String nameBasket = object.getString("basket_name");
                JsonArray baskets = object.containsKey("baskets") ? object.getJsonArray("baskets") : new JsonArray();
                UserUtils.getUserInfos(eb, request, user ->
                        basketService.listebasketItemForOrder(idCampaign, idStructure, user.getUserId(), baskets,
                        listBasket -> {
                            if (listBasket.isRight() && listBasket.right().getValue().size() > 0) {
                                        basketService.takeOrder(request, listBasket.right().getValue(),
                                                idCampaign, user, idStructure, nameStructure, baskets, nameBasket,
                                                Logging.defaultCreateResponsesHandler(eb,
                                                        request,
                                                        Contexts.ORDER.toString(),
                                                        Actions.CREATE.toString(),
                                                        "id_order",
                                                        listBasket.right().getValue()));
                            } else {
                                log.error("An error occurred when listing Baskets");
                                badRequest(request);
                            }
                        }));

            } catch (ClassCastException e) {
                log.error("An error occurred when casting Basket information", e);
                renderError(request);
            }
        });
    }
}
