package fr.openent.crre.controllers;

import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.ValidatorRight;
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
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public OrderRegionController() {
        this.orderRegionService = new DefaultOrderRegionService("equipment");

    }


    @Post("/region/order")
    @ApiDoc("Create an order with id order client when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
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
    @ResourceFilter(ValidatorRight.class)
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
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        try{
            UserUtils.getUserInfos(eb, request, user -> {
                RequestUtils.bodyToJson(request, orders -> {
                    if (!orders.isEmpty()) {
                        JsonArray ordersList = orders.getJsonArray("orders");
                        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                        String title = "Commande_" + date + "_" + orders.size();
                        orderRegionService.createProject(title, idProject -> {
                            if(idProject.isRight()){
                                Integer idProjectRight = idProject.right().getValue().getInteger("id");
                                Logging.insert(eb,
                                        request,
                                        null,
                                        Actions.CREATE.toString(),
                                        idProjectRight.toString(),
                                        new JsonObject().put("id", idProjectRight).put("title", title));
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
    @ResourceFilter(ValidatorRight.class)
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
    @ResourceFilter(ValidatorRight.class)
    public void getOneOrder(HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        orderRegionService.getOneOrderRegion(idOrder, defaultResponseHandler(request));
    }

    @Get("/orderRegion/orders")
    @ApiDoc("get all orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllOrder(HttpServerRequest request) {
        orderRegionService.getAllOrderRegion(arrayResponseHandler(request));
    }

    @Get("/orderRegion/orders/:id")
    @ApiDoc("get all orders by project ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllOrderByProject(HttpServerRequest request) {
        int idProject = Integer.parseInt(request.getParam("id"));
        orderRegionService.getAllOrderRegionByProject(idProject, arrayResponseHandler(request));
    }

    @Get("/orderRegion/projects")
    @ApiDoc("get all projects ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            orderRegionService.getAllProjects(user, arrayResponseHandler(request));
        });
    }

    @Get("/ordersRegion/projects/filter")
    @ApiDoc("get all projects ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getProjectsDate(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            orderRegionService.filter(user, startDate, endDate, arrayResponseHandler(request));
        });
    }

    @Get("/ordersRegion/projects/search_filter")
    @ApiDoc("get all projects search and filter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getProjectsDateSearch(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && request.params().get("q").trim() != "") {
                try {
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8");
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    orderRegionService.searchName(query, equipments -> {
                        if(equipments.right().getValue().size() > 0) {
                            orderRegionService.filterSearch(user, equipments.right().getValue(), query, startDate, endDate, arrayResponseHandler(request));
                        } else {
                            orderRegionService.filterSearchWithoutEquip(user, query, startDate, endDate, arrayResponseHandler(request));
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Get("/ordersRegion/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && request.params().get("q").trim() != "") {
                try {
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8");
                    orderRegionService.searchName(query, equipments -> {
                        if(equipments.right().getValue().size() > 0) {
                            orderRegionService.search(query, user, equipments.right().getValue(), arrayResponseHandler(request));
                        } else {
                            orderRegionService.searchWithoutEquip(query, user, arrayResponseHandler(request));
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } else {
                badRequest(request);
            }
        });
    }

    @Put("/order/region/:idOperation/operation")
    @ApiDoc("update operation in orders region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void updateOperation(final HttpServerRequest request) {
        badRequest(request);
    }
}
