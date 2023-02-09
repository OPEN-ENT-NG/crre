package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.EquipmentHelper;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.BasketOrderItem;
import fr.openent.crre.security.*;
import fr.openent.crre.service.BasketOrderItemService;
import fr.openent.crre.service.BasketOrderService;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static java.lang.Integer.parseInt;

public class BasketController extends ControllerHelper {
    private final BasketOrderService basketOrderService;
    private final BasketOrderItemService basketOrderItemService;

    public BasketController(ServiceFactory serviceFactory) {
        super();
        this.basketOrderService = serviceFactory.getBasketOrderService();
        this.basketOrderItemService = serviceFactory.getBasketOrderItemService();
    }

    @Get("/basket/:idCampaign/:idStructure")
    @ApiDoc("List baskets of the user in a campaign and a structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void list(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer idCampaign = request.params().contains(Field.IDCAMPAIGN)
                        ? parseInt(request.params().get(Field.IDCAMPAIGN))
                        : null;
                String idStructure = request.params().contains(Field.IDSTRUCTURE)
                        ? request.params().get(Field.IDSTRUCTURE)
                        : null;
                Future<List<BasketOrderItem>> listBasketOrderItemFuture = basketOrderItemService.listBasketOrderItem(idCampaign, idStructure, user.getUserId());
                listBasketOrderItemFuture
                        .compose(basketOrderItemList -> {
                            List<String> itemIdList = basketOrderItemList.stream()
                                    .map(BasketOrderItem::getIdItem)
                                    .collect(Collectors.toList());
                            return searchByIds(itemIdList);
                        })
                        .onSuccess(equipments -> {
                            listBasketOrderItemFuture.result().forEach(basketOrderItem -> {
                                JsonObject equipment = equipments.stream()
                                        .filter(JsonObject.class::isInstance)
                                        .map(JsonObject.class::cast)
                                        .filter(equipmentResult -> basketOrderItem.getIdItem().equals(equipmentResult.getString(Field.ID)))
                                        .findFirst()
                                        .orElse(EquipmentHelper.getDefaultEquipment(basketOrderItem));
                                basketOrderItem.setEquipment(equipment);
                            });
                            renderJson(request, IModelHelper.toJsonArray(listBasketOrderItemFuture.result()));
                        })
                        .onFailure(error -> {
                            log.error(String.format("[CRRE@%s::list] An error occurred getting basket %s",
                                    this.getClass().getSimpleName(), error.getMessage()));
                            badRequest(request);
                        });
            } catch (NumberFormatException e) {
                log.error("[CRRE@%s::list] An error occurred casting campaign id %s", this.getClass().getSimpleName(), e.getMessage());
                Renders.renderError(request);
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
                Integer page = request.getParam(Field.PAGE) != null ? Integer.parseInt(request.getParam(Field.PAGE)) : 0;
                Integer idCampaign = request.params().contains(Field.IDCAMPAIGN) ? parseInt(request.params().get(Field.IDCAMPAIGN)) : null;
                String startDate = request.getParam(Field.STARTDATE);
                String endDate = request.getParam(Field.ENDDATE);
                boolean old = Boolean.parseBoolean(request.getParam(Field.OLD));
                basketOrderService.getMyBasketOrders(user.getUserId(), page, idCampaign, startDate, endDate, old)
                        .onSuccess(basketOrders -> Renders.renderJson(request, IModelHelper.toJsonArray(basketOrders)))
                        .onFailure(error -> Renders.renderError(request));
            } catch (NumberFormatException e) {
                log.error(String.format("[CRRE@%s::getMyBasketOrders] An error occurred casting campaign id %s",
                        this.getClass().getSimpleName(), e.getMessage()));
                Renders.renderError(request);
            }
        });
    }

    @Get("/basketOrder/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains(Field.Q) && !request.params().get(Field.Q).trim().isEmpty()) {
                try {
                    Integer page = request.getParam(Field.PAGE) != null ? Integer.parseInt(request.getParam(Field.PAGE)) : 0;
                    String query = URLDecoder.decode(request.getParam(Field.Q), Field.UTF_DASH_8).toLowerCase();
                    int idCampaign = parseInt(request.getParam(Field.IDCAMPAIGN));
                    String startDate = request.getParam(Field.STARTDATE);
                    String endDate = request.getParam(Field.ENDDATE);
                    Boolean old = Boolean.valueOf(request.getParam(Field.OLD));
                    plainTextSearchName(query)
                            .compose(equipments -> basketOrderService.search(query, user,
                                    equipments, idCampaign, startDate, endDate, page, old))
                            .onSuccess(basketOrderList -> Renders.renderJson(request, IModelHelper.toJsonArray(basketOrderList)))
                            .onFailure(error -> Renders.renderError(request));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    badRequest(request, "UnsupportedEncodingException");
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
            RequestUtils.bodyToJson(request, pathPrefix + Field.BASKET,
                    basketOrderItemJson -> basketOrderItemService.create(new BasketOrderItem(basketOrderItemJson), user)
                            .onSuccess(result -> Renders.renderJson(request, result))
                            .onFailure(error -> Renders.renderError(request)));

        });
    }

    @Post("/baskets/campaign")
    @ApiDoc("Create a baskets item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void createBaskets(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> RequestUtils.bodyToJsonArray(request, basketsArray -> {
            List<BasketOrderItem> basketOrderItemList = IModelHelper.toList(basketsArray, BasketOrderItem.class);
            List<Future<JsonObject>> futures = basketOrderItemList.stream()
                    .map(basketOrderItem -> basketOrderItemService.create(basketOrderItem, user))
                    .collect(Collectors.toList());
            FutureHelper.all(futures).
                    onSuccess(event -> {
                        JsonArray result = new JsonArray(futures.stream().map(Future::result).collect(Collectors.toList()));
                        renderJson(request, result);
                    })
                    .onFailure(error -> {
                        log.error("[CRRE@%s::createBaskets] Error in future baskets : %s", this.getClass().getSimpleName(), error.getMessage());
                        badRequest(request);
                    });
        }));
    }

    @Delete("/basket/:idBasket/campaign/:idCampaign")
    @ApiDoc("Delete a basket item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void delete(HttpServerRequest request) {
        try {
            Integer idBasket = request.params().contains(Field.IDBASKET)
                    ? parseInt(request.params().get(Field.IDBASKET))
                    : null;
            basketOrderItemService.delete(idBasket)
                    .onSuccess(result -> Renders.renderJson(request, result))
                    .onFailure(error -> Renders.renderError(request));

        } catch (NumberFormatException e) {
            log.error(String.format("[CRRE@%s::delete] An error occurred when casting basket id %s", this.getClass().getSimpleName(), e.getMessage()));
            badRequest(request);
        }
    }


    @Put("/basket/:idBasket/amount")
    @ApiDoc("Update a basket's amount")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void updateAmount(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> RequestUtils.bodyToJson(request, pathPrefix + Field.BASKET, basket -> {
            try {
                Integer id = parseInt(request.params().get(Field.IDBASKET));
                Integer amount = basket.getInteger(Field.AMOUNT);
                basketOrderItemService.updateAmount(user, id, amount)
                        .onSuccess(result -> Renders.renderJson(request, result))
                        .onFailure(error -> Renders.renderError(request));
            } catch (ClassCastException e) {
                log.error(String.format("[CRRE@%s::updateAmount] An error occurred when casting basket id %s", this.getClass().getSimpleName(), e.getMessage()));
                Renders.renderError(request);
            }
        }));
    }

    @Put("/basket/:idBasket/comment")
    @ApiDoc("Update a basket's comment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateComment(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, basket -> {
            if (!basket.containsKey(Field.COMMENT)) {
                badRequest(request);
                return;
            }
            try {
                Integer id = parseInt(request.params().get(Field.IDBASKET));
                String comment = basket.getString(Field.COMMENT);
                basketOrderItemService.updateComment(id, comment)
                        .onSuccess(result -> Renders.renderJson(request, result))
                        .onFailure(error -> Renders.renderError(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Renders.renderError(request);
            }
        });
    }

    @Put("/basket/:idBasket/reassort")
    @ApiDoc("Update a basket's reassort")
    @SecuredAction(Crre.REASSORT_RIGHT)
    @ResourceFilter(AccessOrderReassortRight.class)
    public void updateReassort(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, basket -> {
            if (!basket.containsKey(Field.REASSORT)) {
                badRequest(request);
                return;
            }
            try {
                Integer id = parseInt(request.params().get(Field.IDBASKET));
                Boolean reassort = basket.getBoolean(Field.REASSORT);
                basketOrderItemService.updateReassort(id, reassort)
                        .onSuccess(result -> Renders.renderJson(request, result))
                        .onFailure(error -> Renders.renderError(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Renders.renderError(request);
            }
        });
    }

    @Post("/baskets/to/orders/:idCampaign")
    @ApiDoc("Create an order list from basket")
    @SecuredAction(Crre.PRESCRIPTOR_RIGHT)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void takeOrder(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Field.BASKETTOORDER, object -> {
            try {
                final Integer idCampaign = parseInt(request.params().get(Field.IDCAMPAIGN));
                final String idStructure = object.getString(Field.ID_STRUCTURE);
                final String nameStructure = object.getString(Field.STRUCTURE_NAME);
                final String nameBasket = object.getString(Field.BASKET_NAME);
                List<Integer> basketIdList = (object.containsKey(Field.BASKETS) ? object.getJsonArray(Field.BASKETS) : new JsonArray()).stream()
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .collect(Collectors.toList());
                UserUtils.getUserInfos(eb, request, user -> {
                    Future<List<BasketOrderItem>> listBasketItemForOrderFuture = basketOrderItemService.listBasketItemForOrder(idCampaign, idStructure, user.getUserId(), basketIdList);
                    listBasketItemForOrderFuture.compose(listBasket ->
                                    basketOrderItemService.takeOrder(listBasket, idCampaign, user, idStructure, nameBasket))
                            .onSuccess(result -> {
                                Renders.renderJson(request, result);
                                Logging.insert(user, Contexts.ORDER.toString(), Actions.CREATE.toString(), Field.ID_ORDER,
                                        IModelHelper.toJsonArray(listBasketItemForOrderFuture.result()));
                            })
                            .onFailure(error -> {
                                log.error(String.format("[CRRE%s::takeOrder] An error occurred when listing Baskets %s", this.getClass().getSimpleName(), error.getMessage()));
                                badRequest(request);
                            });
                });

            } catch (ClassCastException e) {
                log.error(String.format("[CRRE%s::takeOrder] An error occurred when casting Basket information %s", this.getClass().getSimpleName(), e.getMessage()));
                renderError(request);
            }
        });
    }
}
