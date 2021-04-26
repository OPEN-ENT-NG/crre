package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.service.BasketService;
import fr.openent.crre.service.OldBasketService;
import fr.openent.crre.service.impl.DefaultBasketService;
import fr.openent.crre.service.impl.DefaultOldBasketService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
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

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static java.lang.Integer.parseInt;

public class OldBasketController extends ControllerHelper {
    private final OldBasketService oldBasketService;
    private final BasketService basketService;

    public OldBasketController() {
        super();
        this.oldBasketService = new DefaultOldBasketService(Crre.crreSchema, "basket");
        this.basketService = new DefaultBasketService(Crre.crreSchema, "basket");
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
                basketService.getMyBasketOrders(user, page, id_campaign, startDate, endDate, true, arrayResponseHandler(request));
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
                    q = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                }
                int id_campaign = parseInt(request.getParam("id"));
                String finalQ = q;
                // Si nous avons des filtres de grade
                if (request.params().contains("q")) {
                    oldBasketService.search(finalQ, filters, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                } else {
                    oldBasketService.filter(filters, user, id_campaign, startDate, endDate, page, arrayResponseHandler(request));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }
}
