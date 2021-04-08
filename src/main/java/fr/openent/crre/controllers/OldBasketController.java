package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OldBasketService;
import fr.openent.crre.service.impl.DefaultOldBasketService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;

public class OldBasketController extends ControllerHelper {
    private final OldBasketService oldBasketService;

    public OldBasketController() {
        super();
        this.oldBasketService = new DefaultOldBasketService(Crre.crreSchema, "basket");
    }
    @Get("/basket/old/:idCampaign/:idStructure")
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
                oldBasketService.listBasket(idCampaign, idStructure, user, baskets -> {
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

    @Get("/basketOrder/old/:idBasketOrder")
    @ApiDoc("Get basket order thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBasketOrder(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer idBasketOrder = request.params().contains("idBasketOrder")
                        ? parseInt(request.params().get("idBasketOrder"))
                        : null;
                oldBasketService.getBasketOrder(idBasketOrder, arrayResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/old/:idCampaign")
    @ApiDoc("Get baskets orders of my structures for this campaign")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBasketsOrders(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer idCampaign = request.params().contains("idCampaign")
                        ? parseInt(request.params().get("idCampaign"))
                        : null;
                oldBasketService.getBasketsOrders(idCampaign, arrayResponseHandler(request), user);
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/old/allMyOrders")
    @ApiDoc("Get all my baskets orders")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getMyBasketOrders(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                int id_campaign = parseInt(request.getParam("id"));
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                oldBasketService.getMyBasketOrders(user, page, id_campaign, startDate, endDate, arrayResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/old/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && request.params().get("q").trim() != "") {
                try {
                    Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8");
                    int id_campaign = parseInt(request.getParam("id"));
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    oldBasketService.search(query, null, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/basketOrder/old/filter")
    @ApiDoc("Filter order")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void filter(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                List<String> params = new ArrayList<>();
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                String q = ""; // Query pour chercher sur le nom du panier, le nom de la ressource ou le nom de l'enseignant
                if (request.params().contains("niveaux.libelle")) {
                    params = request.params().getAll("niveaux.libelle");
                }

                // Récupération de tout les filtres hors grade
                JsonArray filters = new JsonArray();
                int length = request.params().entries().size();
                for (int i = 0; i < length; i++) {
                    if (!request.params().entries().get(i).getKey().equals("id") && !request.params().entries().get(i).getKey().equals("q") && !request.params().entries().get(i).getKey().equals("niveaux.libelle"))
                        filters.add(new JsonObject().put(request.params().entries().get(i).getKey(), request.params().entries().get(i).getValue()));
                }
                // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
                if (request.params().contains("q")) {
                    q = URLDecoder.decode(request.getParam("q"), "UTF-8");
                }
                int id_campaign = parseInt(request.getParam("id"));
                String finalQ = q;
                // Si nous avons des filtres de grade
                if (params.size() > 0) {
                                if (request.params().contains("q")) {
                                    oldBasketService.searchWithAll(finalQ, filters, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                } else {
                                    oldBasketService.filter(filters, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                }
                } else {
                    // Recherche avec les filtres autres que grade
                            oldBasketService.search(finalQ, filters, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                        }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }
}
