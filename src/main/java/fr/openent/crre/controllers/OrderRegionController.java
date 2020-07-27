package fr.openent.crre.controllers;

import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.ManagerRight;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public OrderRegionController() {
        this.orderRegionService = new DefaultOrderRegionService("equipment");

    }


    @Post("/region/order")
    @ApiDoc("Create an order with id order client when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createWithOrderClientAdminOrder(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, event -> RequestUtils.bodyToJson(request,
                order -> RequestUtils.bodyToJson(request,
                        orderRegion ->  orderRegionService.setOrderRegion(order, event,
                                Logging.defaultResponseHandler(eb, request, Contexts.ORDERREGION.toString(),
                                        Actions.CREATE.toString(),null, orderRegion)))));
    }

    @Put("/region/order/:id")
    @ApiDoc("Update an order when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateAdminOrder(final HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        UserUtils.getUserInfos(eb, request, event -> RequestUtils.bodyToJson(request,
                order -> RequestUtils.bodyToJson(request,
                        orderRegion ->  orderRegionService.updateOrderRegion(order, idOrder, event,
                                Logging.defaultResponseHandler(eb, request, Contexts.ORDERREGION.toString(),
                                        Actions.UPDATE.toString(), Integer.toString(idOrder), new JsonObject().put("orderRegion", orderRegion))))));
    }

    @Post("/region/orders/")
    @ApiDoc("Create orders from a region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        try{
            UserUtils.getUserInfos(eb, request, user -> {
                RequestUtils.bodyToJson(request, orders -> {
                    if (!orders.isEmpty()) {
                        JsonArray ordersList = orders.getJsonArray("orders");
                        Integer id_title = ordersList.getJsonObject(0).getInteger("title_id");
                        orderRegionService.createProject(id_title, idProject -> {
                            if(idProject.isRight()){
                                Integer idProjectRight = idProject.right().getValue().getInteger("id");
                                Logging.insert(eb,
                                        request,
                                        null,
                                        Actions.CREATE.toString(),
                                        idProjectRight.toString(),
                                        new JsonObject().put("id", idProjectRight).put("id_title", id_title));
                                for(int i = 0 ; i<ordersList.size() ; i++){
                                    JsonObject newOrder = ordersList.getJsonObject(i);
                                    orderRegionService.createOrdersRegion(newOrder, user, idProjectRight, orderCreated -> {
                                        if(orderCreated.isRight()){
                                            Number idReturning = orderCreated.right().getValue().getInteger("id");
                                            Logging.insert(eb,
                                                    request,
                                                    Contexts.ORDERREGION.toString(),
                                                    Actions.CREATE.toString(),
                                                    idReturning.toString(),
                                                    new JsonObject().put("order region", newOrder));
                                        } else {
                                            LOGGER.error("An error when you want get id after create order region " + orderCreated.left());
                                            request.response().setStatusCode(400).end();
                                        }
                                    });
                                }
                                request.response().setStatusCode(201).end();
                            } else {
                                LOGGER.error("An error when you want get id after create project " + idProject.left());
                                request.response().setStatusCode(400).end();
                            }
                        });
                    }
                });
            });
        } catch( Exception e){
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
        }
    };

    @Delete("/region/:id/order")
    @ApiDoc("delete order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteOrderRegion(final HttpServerRequest request) {
        int idRegion = Integer.parseInt(request.getParam("id"));
        orderRegionService.deleteOneOrderRegion(idRegion, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDERREGION.toString(),
                Actions.DELETE.toString(),
                Integer.toString(idRegion),
                new JsonObject().put("idRegion", idRegion)));
    }

    @Get("/orderRegion/:id/order")
    @ApiDoc("get order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOneOrder(HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        orderRegionService.getOneOrderRegion(idOrder, defaultResponseHandler(request));
    }

    @Put("/order/region/:idOperation/operation")
    @ApiDoc("update operation in orders region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperation(final HttpServerRequest request) {
        badRequest(request);
    }
}
