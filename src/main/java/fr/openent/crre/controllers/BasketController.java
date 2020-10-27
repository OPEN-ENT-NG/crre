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
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;

public class BasketController extends ControllerHelper {
    private final BasketService basketService;
    private final Storage storage;

    public BasketController(Vertx vertx, Storage storage, JsonObject slackConfiguration,JsonObject mail) {
        super();
        this.storage = storage;
        this.basketService = new DefaultBasketService(Crre.crreSchema, "basket", vertx, slackConfiguration, mail);
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
                basketService.listBasket(idCampaign, idStructure, arrayResponseHandler(request), user);
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/:idBasketOrder")
    @ApiDoc("Get basket order thanks to the id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBasketOrder(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer idBasketOrder = request.params().contains("idBasketOrder")
                        ? parseInt(request.params().get("idBasketOrder"))
                        : null;
                basketService.getBasketOrder(idBasketOrder, arrayResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/:idCampaign")
    @ApiDoc("Get baskets orders of my structures for this campaign")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBasketsOrders(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer idCampaign = request.params().contains("idCampaign")
                        ? parseInt(request.params().get("idCampaign"))
                        : null;
                basketService.getBasketsOrders(idCampaign, arrayResponseHandler(request), user);
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
                basketService.getMyBasketOrders(arrayResponseHandler(request), user);
            } catch (ClassCastException e) {
                log.error("An error occurred casting campaign id", e);
            }
        });
    }

    @Get("/basketOrder/history")
    @ApiDoc("Get all my baskets orders")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructureHistoryBaskets(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                basketService.getMyBasketOrders(arrayResponseHandler(request), user);
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
            if (request.params().contains("q") && request.params().get("q").trim() != "") {
                String query = request.getParam("q");
                int id_campaign = parseInt(request.getParam("id"));
                basketService.search(query, user, id_campaign, arrayResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

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
    @ResourceFilter(PersonnelRight.class)
    public void updateAmount(final HttpServerRequest  request){
        RequestUtils.bodyToJson(request, pathPrefix + "basket", basket -> {
            try {
                Integer id = parseInt(request.params().get("idBasket"));
                Integer amount = basket.getInteger("amount") ;
                basketService.updateAmount(id, amount,  defaultResponseHandler(request));
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

    @Put("/basket/:idBasket/priceProposal")
    @ApiDoc("Update the price proposal of a basket")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessPriceProposalRight.class)
    public void updatePriceProposal(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, basket -> {
            if (!basket.containsKey("price_proposal")) {
                badRequest(request);
                return;
            }
            try {
                Integer id = parseInt(request.params().get("idBasket"));
                Double price_proposal = basket.getDouble("price_proposal");
                basketService.updatePriceProposal(id, price_proposal, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                Integer id = parseInt(request.params().get("idBasket"));
                basketService.updatePriceProposal(id, null, defaultResponseHandler(request));
            }
        });
    }

    @Post("/baskets/to/orders/:idCampaign")
    @ApiDoc("Create an order list from basket")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
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
                                UserUtils.getUserInfos(eb, request, user -> {
                                    basketService.takeOrder(request , listBasket.right().getValue(),
                                            idCampaign, user, idStructure, nameStructure, baskets, nameBasket,
                                            Logging.defaultCreateResponsesHandler(eb,
                                                    request,
                                                    Contexts.ORDER.toString(),
                                                    Actions.CREATE.toString(),
                                                    "id_order",
                                                    listBasket.right().getValue()));
                                });
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

    @Post("/basket/:id/file")
    @ApiDoc("Upload a file for a specific cart")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PostBasketFileRight.class)
    public void uploadFile(HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                renderError(request);
                return;
            }
            try {
                Integer basketId = parseInt(request.getParam("id"));
                String fileId = entries.getString("_id");
                String filename = entries.getJsonObject("metadata").getString("filename");
                basketService.addFileToBasket(basketId, fileId, filename, event -> {
                    if (event.isRight()) {
                        JsonObject response = new JsonObject()
                                .put("id", fileId)
                                .put("filname", filename);
                        request.response().setStatusCode(201).putHeader("Content-Type", "application/json").end(response.toString());
                    } else {
                        deleteFile(fileId);
                        renderError(request);
                    }
                });
            } catch (NumberFormatException e) {
                renderError(request);
            }
        });
    }

    @Delete("/basket/:id/file/:fileId")
    @ApiDoc("Delete file from basket")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PostBasketFileRight.class)
    public void deleteFileFromBasket(HttpServerRequest request) {
        Integer basketId = parseInt(request.getParam("id"));
        String fileId = request.getParam("fileId");

        basketService.deleteFileFromBasket(basketId, fileId, event -> {
            if (event.isRight()) {
                request.response().setStatusCode(204).end();
                deleteFile(fileId);
            } else {
                renderError(request);
            }
        });
    }

    /**
     * Delete file from storage based on identifier
     *
     * @param fileId File identifier to delete
     */
    private void deleteFile(String fileId) {
        storage.removeFile(fileId, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Crre@uploadFile] An error occurred while removing " + fileId + " file.");
            }
        });
    }

    @Get("/basket/:id/file/:fileId")
    @ApiDoc("Download specific file")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getFile(HttpServerRequest request) {
        Integer basketId = parseInt(request.getParam("id"));
        String fileId = request.getParam("fileId");
        basketService.getFile(basketId, fileId, event -> {
            if (event.isRight()) {
                JsonObject file = event.right().getValue();
                storage.sendFile(fileId, file.getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }
}
