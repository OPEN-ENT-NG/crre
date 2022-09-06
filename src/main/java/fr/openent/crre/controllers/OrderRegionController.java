package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.QuoteService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.*;
import fr.openent.crre.utils.OrderUtils;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static fr.openent.crre.helpers.ElasticSearchHelper.*;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static java.lang.Math.min;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;
    private final PurseService purseService;
    private final StructureService structureService;
    private final QuoteService quoteService;
    private final EmailSendService emailSender;
    private final JsonObject mail;
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderService.class);


    public OrderRegionController(Vertx vertx, JsonObject config, JsonObject mail) {
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.emailSender = new EmailSendService(emailSender);
        this.mail = mail;
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.purseService = new DefaultPurseService();
        this.quoteService = new DefaultQuoteService("equipment");
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
    }

    @Post("/region/orders")
    @ApiDoc("Create orders for region when we don't know the id of the project")
    @SecuredAction(Crre.VALIDATOR_RIGHT)
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user ->
                RequestUtils.bodyToJson(request, orders -> {
                    if (!orders.isEmpty()) {
                        JsonArray ordersList = orders.getJsonArray("orders");
                        HashSet<String> idsEquipment = new HashSet<>();
                        for (int i = 0; i < ordersList.size(); i++) {
                            idsEquipment.add(ordersList.getJsonObject(i).getString("equipment_key"));
                        }
                        Promise<Void> equipmentsFuture = Promise.promise();
                        Promise<Integer> projectIdFuture = Promise.promise();
                        List<Future> promises = new ArrayList<>();
                        promises.add(equipmentsFuture.future());
                        promises.add(projectIdFuture.future());
                        CompositeFuture.all(promises).onComplete(event -> {
                                    if (event.succeeded()) {
                                        Integer idProject = projectIdFuture.future().result();
                                        createOrdersRegion(request, user, ordersList, idProject);
                                    } else {
                                        LOGGER.error("[CRRE] OrderRegionController@createAdminOrder An error in compositeFuture :  " +
                                                event.cause().getMessage());
                                        request.response().setStatusCode(400).end();
                                    }
                                });
                        getLastProject(request, projectIdFuture);
                        searchByIds(new ArrayList<>(idsEquipment), equipments -> {
                            if (equipments.isRight()) {
                                setPriceToOrder(ordersList, equipments);
                                equipmentsFuture.complete();
                            } else {
                                log.error("[CRRE] OrderRegionController@createAdminOrder searchByIds failed : " +
                                        equipments.left().getValue());
                                equipmentsFuture.fail("[CRRE] OrderRegionController@createAdminOrder searchByIds failed : " +
                                        equipments.left().getValue());
                            }
                        });
                    } else {
                        noContent(request);
                    }
                })
            );
        } catch (Exception e) {
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
        }
    }

    private void getLastProject(HttpServerRequest request, Promise<Integer> projectIdFuture) {
        orderRegionService.getLastProject(lastProject -> {
            if (lastProject.isRight()) {
                String last = lastProject.right().getValue().getString("title");
                String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String title = "Commande_" + date;
                if (last != null) {
                    if (title.equals(last.substring(0, last.length() - 2))) {
                        title = title + "_" + (Integer.parseInt(last.substring(last.length() - 1)) + 1);
                    } else {
                        title += "_1";
                    }
                } else {
                    title += "_1";
                }
                createProject(request, title, projectIdFuture);
            } else {
                LOGGER.error("[CRRE] OrderRegionController@getLastProject An error when you want get last project " +
                        lastProject.left());
                projectIdFuture.fail("[CRRE] OrderRegionController@getLastProject An error when you want get last project " +
                        lastProject.left());
            }
        });
    }

    private void createProject(HttpServerRequest request, String title, Promise<Integer> projectIdFuture) {
        orderRegionService.createProject(title, idProject -> {
            if (idProject.isRight()) {
                Integer idProjectRight = idProject.right().getValue().getInteger("id");
                Logging.insert(eb,
                        request,
                        null,
                        Actions.CREATE.toString(),
                        idProjectRight.toString(),
                        new JsonObject().put("id", idProjectRight).put("title", title));
                projectIdFuture.complete(idProjectRight);
            } else {
                LOGGER.error("[CRRE] OrderRegionController@createProject An error when you want create project " +
                        idProject.left());
                projectIdFuture.fail("[CRRE] OrderRegionController@createProject An error when you want create project " +
                        idProject.left());
            }
        });
    }

    private void createOrdersRegion(HttpServerRequest request, UserInfos user, JsonArray ordersList, Integer idProjectRight) {
        AtomicBoolean stillHaveCredit = new AtomicBoolean(true);
        for (int i = 0; i < ordersList.size(); i++) {
            if(stillHaveCredit.get()) {
                JsonObject newOrder = ordersList.getJsonObject(i);
                Double price = newOrder.getDouble("price") * newOrder.getInteger("amount");
                int finalI = i;
                updatePurseLicence(newOrder, "-", price, newOrder.getString("use_credit", "none"))
                        .onSuccess(res -> orderRegionService.createOrdersRegion(newOrder, user, idProjectRight, event -> {
                                    if (event.isRight()) {
                                        Number idReturning = event.right().getValue().getInteger("id");
                                        Logging.insert(eb,
                                                request,
                                                Contexts.ORDERREGION.toString(),
                                                Actions.CREATE.toString(),
                                                idReturning.toString(),
                                                new JsonObject().put("order region", newOrder));
                                        if (finalI == ordersList.size() - 1) {
                                            renderJson(request,new JsonObject().put("idProject",idProjectRight),201);
                                        }
                                    } else {
                                        LOGGER.error("An error when you want get id after create order region ",
                                                event.left().getValue());
                                        request.response().setStatusCode(400).end();
                                        stillHaveCredit.set(false);
                                    }
                                }))
                        .onFailure(err -> {
                            LOGGER.error("[CRRE] OrderRegionController: " + err.getMessage() + ", " + err.getCause(),
                                    err.getCause());
                            request.response().setStatusCode(400).end();
                            stillHaveCredit.set(false);
                        });
            }
        }
    }

    @Post("/region/orders/:id")
    @ApiDoc("Create orders for region when we know the id of the project")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrderWithIdProject(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user ->
                    RequestUtils.bodyToJson(request, orders -> {
                        Integer idProject = request.getParam("id") != null ?
                                Integer.parseInt(request.getParam("id")) : null;
                        if (!orders.isEmpty() || idProject == null) {
                            JsonArray ordersList = orders.getJsonArray("orders");
                            HashSet<String> idsEquipment = new HashSet<>();
                            for (int i = 0; i < ordersList.size(); i++) {
                                idsEquipment.add(ordersList.getJsonObject(i).getString("equipment_key"));
                            }
                            searchByIds(new ArrayList<>(idsEquipment), equipments -> {
                                if (equipments.isRight()) {
                                    setPriceToOrder(ordersList, equipments);
                                    createOrdersRegion(request, user, ordersList, idProject);
                                } else {
                                    log.error("[CRRE] OrderRegionController@createAdminOrderWithIdProject searchByIds failed : " +
                                            equipments.left().getValue());
                                    badRequest(request);
                                }
                            });
                        } else {
                            noContent(request);
                        }
                    })
            );
        } catch (Exception e) {
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
        }
    }

    private static void setPriceToOrder(JsonArray ordersList, Either<String, JsonArray> equipments) {
        JsonArray equipmentsArray = equipments.right().getValue();
        for (int j = 0; j < equipmentsArray.size(); j++) {
            JsonObject equipment = equipmentsArray.getJsonObject(j);
            double priceTTC = getPriceTtc(equipment).getDouble("priceTTC");
            equipment.put("price",priceTTC);
        }
        for (int i = 0; i < ordersList.size(); i++) {
            JsonObject order = ordersList.getJsonObject(i);
            String idEquipment = order.getString("equipment_key");
            if (equipmentsArray.size() > 0) {
                for (int j = 0; j < equipmentsArray.size(); j++) {
                    JsonObject equipment = equipmentsArray.getJsonObject(j);
                    if (idEquipment.equals(equipment.getString("id"))) {
                        order.put("price",equipment.getDouble("price"));
                    }
                }
            }
        }
    }

    @Get("/orderRegion/projects")
    @ApiDoc("get all projects ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            Integer page = OrderUtils.formatPage(request);
            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            boolean old = Boolean.parseBoolean(request.getParam("old"));
            String idStructure = request.getParam("idStructure");
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null &&
                    Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            orderRegionService.getAllProjects(user, startDate, endDate, page, filterRejectedSentOrders, idStructure, old, arrayResponseHandler(request));
        });
    }

    @Get("/add/orders/lde")
    @ApiDoc("Insert old orders from LDE")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void addOrders(HttpServerRequest request) throws IOException {
        Scanner sc = getOrderLDE();
        orderRegionService.getLastProject(lastProject -> {
            if (lastProject.isRight()) {
                int part = 0;
                search_All(getEquipmentEvent -> {
                    if (getEquipmentEvent.isRight()) {
                        ok(request);
                        orderRegionService.getAllIdsStatus(event -> {
                            if (event.isRight()) {
                                JsonArray result = event.right().getValue();
                                List<Integer> idsStatus = new ArrayList<>();
                                for (Object id : result) {
                                    idsStatus.add(((JsonObject) id).getInteger("id"));
                                }
                                // Store all orders by key (uai + date) and value (id project) No duplicate
                                HashMap<String, Integer> projetMap = new HashMap<>();
                                historicCommand(request, sc, lastProject.right().getValue().getInteger("id"),
                                        getEquipmentEvent.right().getValue(), projetMap, idsStatus, part);
                            } else {
                                badRequest(request);
                                log.error("getAllIdsStatus failed", event.left().getValue());
                            }
                        });
                    } else {
                        badRequest(request);
                        log.error("search_All failed", getEquipmentEvent.left().getValue());
                    }
                });
            } else {
                badRequest(request);
                log.error("getLastProject failed", lastProject.left().getValue());
            }
        });
    }

    public Scanner getOrderLDE() throws IOException {
        disableSslVerification();
        URL hh = new URL("http://www.lde.fr/4dlink1/4dcgi/idf/ldc");
        URLConnection connection;
        if (System.getProperty("httpclient.proxyHost") != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(System.getProperty("httpclient.proxyHost"),
                            Integer.parseInt(System.getProperty("httpclient.proxyPort"))));
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            System.getProperty("httpclient.proxyUsername"),
                            System.getProperty("httpclient.proxyPassword").toCharArray());
                }
            });
            connection = hh.openConnection(proxy);
        } else {
            connection = hh.openConnection();
        }
        String redirect = connection.getHeaderField("Location");
        if (redirect != null) {
            if (System.getProperty("httpclient.proxyHost") != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress(System.getProperty("httpclient.proxyHost"),
                                Integer.parseInt(System.getProperty("httpclient.proxyPort"))));
                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                System.getProperty("httpclient.proxyUsername"),
                                System.getProperty("httpclient.proxyPassword").toCharArray());
                    }
                });
                connection = new URL(redirect).openConnection(proxy);
            } else {
                connection = new URL(redirect).openConnection();
            }
        }
        Scanner sc = new Scanner(new InputStreamReader(connection.getInputStream()));
        // skip header
        if (sc.hasNextLine()) {
            sc.nextLine();
        } else {
            log.info("Empty file");
        }
        return sc;
    }

    private void historicCommand(HttpServerRequest request, Scanner sc, Integer lastProjectId, JsonArray equipments,
                                 HashMap<String, Integer> projetMap, List<Integer> idsStatus, int part) {
        JsonArray ordersRegion = new JsonArray();
        JsonArray uais = new JsonArray();
        List<Future> futures = new ArrayList<>();


        int project_id = lastProjectId; // Id of the last project created
        int project_size = 0; // Size of project to create dynamically project in db
        int total = part * 1000;
        if (!sc.hasNextLine()) {
            orderRegionService.setIdOrderRegion(event -> {
                if (event.isRight()) {
                    log.info("add LDE orders finished with success");
                } else {
                    log.info("[LDE Historic] Error : Unable to set correct id to order region equipment table : " + event.left().getValue());
                }
            });
        } else {
            log.info("Processing LDE orders part " + part);
            while (sc.hasNextLine() && total < 1000 * part + 1000) {
                total++;
                String userLine = sc.nextLine();
                String[] values = userLine.split(Pattern.quote("|"));
                JsonObject order = new JsonObject();
                /* Current project id : get the last project id and increment it
                or get the project id stored in projectMap if the key already exists to group all orders by uai + date */
                int currentProject = project_id;
                if (!projetMap.containsKey(values[11] + values[17])) {
                    projetMap.put(values[11] + values[17], project_id);
                    project_id++;
                    project_size++;
                } else {
                    currentProject = projetMap.get(values[11] + values[17]);
                }
                try {
                    if (values[17].equals("00/00/00")) {
                        order.put("owner_name", "Commande " + (new Date().getYear() + 1900));
                        order.put("owner_id", "Commande " + (new Date().getYear() + 1900));
                        order.put("creation_date", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

                    } else {
                        int year = new SimpleDateFormat("dd/MM/yyyy").parse(values[17]).getYear() + 1900;
                        order.put("owner_name", "Commande " + year);
                        order.put("owner_id", "Commande " + year);
                        order.put("creation_date", values[17]);
                    }

                } catch (ParseException e) {
                    order.put("owner_name", "Commande " + (new Date().getYear() + 1900));
                    order.put("owner_id", "Commande " + (new Date().getYear() + 1900));
                    order.put("creation_date", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                    e.printStackTrace();
                }

                order.put("status", "SENT");
                order.put("name", values[0]);
                order.put("editor", values[1]);
                order.put("uai", values[11]);
                order.put("diffusor", values[2]);
                order.put("equipment_key", values[4]);
                order.put("amount", Math.abs(Integer.parseInt(values[5])));
                order.put("unitedPriceTTC", Double.parseDouble(values[9].replace(",", ".")));
                order.put("reassort", !values[14].isEmpty());
                order.put("id_project", currentProject);
                if (!values[21].isEmpty() && idsStatus.contains(Integer.parseInt(values[21]))) {
                    order.put("id_status", Integer.parseInt(values[21]));
                } else {
                    order.put("id_status", 1000);
                }
                ordersRegion.add(order);
                uais.add(values[11]);
            }

            int finalProject_id = project_id;
            int finalProject_size = project_size;
            structureService.getStructureByUAI(uais, null, structureEvent -> {
                if (structureEvent.isRight()) {
                    boolean checkEquip;
                    boolean checkEtab;
                    int k;
                    int j;
                    JsonArray structures = structureEvent.right().getValue();
                    for (int i = 0; i < ordersRegion.size(); i++) {
                        Future<JsonObject> createProjectFuture = Future.future();
                        if (finalProject_size > i) {
                            futures.add(createProjectFuture);
                        }
                        checkEquip = true;
                        checkEtab = true;
                        j = 0;
                        k = 0;
                        while (checkEquip && j < equipments.size()) {
                            if (equipments.getJsonObject(j).getString("ean")
                                    .equals(ordersRegion.getJsonObject(i).getString("equipment_key"))) {
                                JsonObject equipment = equipments.getJsonObject(j);
                                ordersRegion.getJsonObject(i).put("type", equipment.getString("type"));
                                if (equipment.getJsonArray("disciplines").size() > 0) {
                                    ordersRegion.getJsonObject(i).put("grade",
                                            equipment.getJsonArray("disciplines").getJsonObject(0).getString("libelle"));
                                }
                                ordersRegion.getJsonObject(i).put("image", equipment.getString("urlcouverture"));
                                checkEquip = false;
                            }
                            j++;
                        }
                        while (checkEtab && k < structures.size()) {
                            if (structures.getJsonObject(k).getString("uai").equals(ordersRegion.getJsonObject(i).getString("uai"))) {
                                JsonObject structure = structures.getJsonObject(k);
                                ordersRegion.getJsonObject(i).put("id_structure", structure.getString("id"));
                                checkEtab = false;
                            }
                            k++;
                        }
                        if (finalProject_size > i) {
                            orderRegionService.createProject("Commandes LDE", handlerJsonObject(createProjectFuture));
                        }
                    }
                    CompositeFuture.all(futures).setHandler(event2 -> {
                        if (event2.succeeded()) {
                            try {
                                orderRegionService.recursiveInsertOldOrders(ordersRegion, true, 0, event1 -> {
                                    if (event1.isRight()) {
                                        historicCommand(request, sc, finalProject_id, equipments, projetMap, idsStatus, part + 1);
                                    } else {
                                        badRequest(request);
                                        log.error("Insert old orders failed");
                                    }
                                });
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    badRequest(request);
                    log.error("getStructureByUAI failed");
                }
            });
        }
    }

    private void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs,
                                                       String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs,
                                                       String authType) {
                        }
                    }};

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private void getOrders(String query, JsonArray filters, UserInfos user, Integer page, HttpServerRequest request) {
        HashMap<String, ArrayList<String>> params = new HashMap<>();
        if (request.params().contains("editeur")) {
            params.put("editeur", new ArrayList<>(request.params().getAll("editeur")));
        }
        if (request.params().contains("distributeur")) {
            params.put("distributeur", new ArrayList<>(request.params().getAll("distributeur")));
        }
        if (request.params().contains("_index")) {
            params.put("_index", new ArrayList<>(request.params().getAll("_index")));
        }

        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");
        String idStructure = request.getParam("idStructure");
        Boolean old = Boolean.valueOf(request.getParam("old"));

        int length = request.params().entries().size();
        for (int i = 0; i < length; i++) {
            if (!request.params().entries().get(i).getKey().equals("q") &&
                    !request.params().entries().get(i).getKey().equals("startDate") &&
                    !request.params().entries().get(i).getKey().equals("distributeur") &&
                    !request.params().entries().get(i).getKey().equals("editeur") &&
                    !request.params().entries().get(i).getKey().equals("_index") &&
                    !request.params().entries().get(i).getKey().equals("type") &&
                    !request.params().entries().get(i).getKey().equals("endDate") &&
                    !request.params().entries().get(i).getKey().equals("page") &&
                    !request.params().entries().get(i).getKey().equals("id_structure") &&
                    !request.params().entries().get(i).getKey().equals("old") &&
                    !request.params().entries().get(i).getKey().equals("idStructure"))
                filters.add(new JsonObject().put(request.params().entries().get(i).getKey(),
                        request.params().entries().get(i).getValue()));
        }
        if (params.size() > 0) {
            if (!old) {
                Future<JsonArray> equipmentFilterFuture = Future.future();
                Future<JsonArray> equipmentFilterAndQFuture = Future.future();

                filters(params, handlerJsonArray(equipmentFilterFuture));

                if (StringUtils.isEmpty(query)) {
                    equipmentFilterAndQFuture.complete(new JsonArray());
                } else {
                    searchfilter(params, query, handlerJsonArray(equipmentFilterAndQFuture));
                }

                CompositeFuture.all(equipmentFilterFuture, equipmentFilterAndQFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonArray equipmentsGrade = equipmentFilterFuture.result(); // Tout les équipements correspondant aux grades
                        JsonArray equipmentsGradeAndQ = equipmentFilterAndQFuture.result();
                        JsonArray allEquipments = new JsonArray();
                        allEquipments.addAll(equipmentsGrade);
                        allEquipments.addAll(equipmentsGradeAndQ);
                        orderRegionService.search(user, allEquipments, query, startDate,
                                endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                    }
                });
            }
        } else {
            if (query.equals("")) {
                orderRegionService.search(user, new JsonArray(), query, startDate,
                        endDate, idStructure, filters, page, old, arrayResponseHandler(request));
            } else {
                plainTextSearchName(query, equipments -> {
                    orderRegionService.search(user, equipments.right().getValue(), query, startDate,
                            endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                });
            }
        }
    }

    @Get("/ordersRegion/projects/search_filter")
    @ApiDoc("get all projects search and filter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getProjectsDateSearch(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String query = "";
            JsonArray filters = new JsonArray();
            Integer page = OrderUtils.formatPage(request);
            if (request.params().contains("q")) {
                try {
                    query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            if (request.params().contains("type") || request.params().contains("id_structure")) {
                String finalQuery = query;
                Integer finalPage = page;
                structureService.getStructuresByTypeAndFilter(request.getParam("type"),
                        request.params().getAll("id_structure"), event -> {
                            if (event.isRight()) {
                                JsonArray listeIdStructure = event.right().getValue();
                                filters.add(new JsonObject().put("id_structure", listeIdStructure));
                                getOrders(finalQuery, filters, user, finalPage, request);
                            } else {
                                log.error(event.left().getValue());
                                badRequest(request);
                            }
                        });
            } else {
                getOrders(query, filters, user, page, request);
            }
        });
    }

    @Post("/ordersRegion/orders")
    @ApiDoc("get all orders of each project")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getOrdersByProjects(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, orderRegions -> {
            UserUtils.getUserInfos(eb, request, user -> {
                boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null
                        && Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
                Boolean old = Boolean.valueOf(request.getParam("old"));
                List<Integer> idsProjects = orderRegions.getJsonArray("idsProjects").getList();
                List<Future> futures = new ArrayList<>();
                for (int id : idsProjects) {
                    Future<JsonArray> projectIdFuture = Future.future();
                    futures.add(projectIdFuture);
                    orderRegionService.getAllOrderRegionByProject(id, filterRejectedSentOrders, old, handlerJsonArray(projectIdFuture));
                }
                getCompositeFutureAllOrderRegionByProject(request, old, futures);
            });
        });
    }

    private void getCompositeFutureAllOrderRegionByProject(HttpServerRequest request, Boolean old, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                List<JsonArray> resultsList = event.result().list();
                if (!old) {
                    HashSet<String> listIdsEquipment = new HashSet<>();
                    for (JsonArray orders : resultsList) {
                        for (Object order : orders) {
                            if(((JsonObject) order).getString("equipment_key") != null) {
                                listIdsEquipment.add(((JsonObject) order).getString("equipment_key", ""));
                            }
                        }
                    }
                    getSearchByIds(request, resultsList, new ArrayList<>(listIdsEquipment));
                } else {
                    getSearchByIdsOld(request, resultsList);
                }

            } else {
                log.error(event.cause());
                badRequest(request);
            }
        });
    }

    private void getSearchByIds(HttpServerRequest
                                        request, List<JsonArray> resultsList, List<String> listIdsEquipment) {
        searchByIds(listIdsEquipment, equipments -> {
            if (equipments.isRight()) {
                JsonArray equipmentsArray = equipments.right().getValue();
                for (int i = 0; i < equipmentsArray.size(); i++) {
                    JsonObject equipment = equipmentsArray.getJsonObject(i);
                    JsonObject priceDetails = getPriceTtc(equipment);
                    equipment.put("priceDetails", priceDetails);
                }
                for (JsonArray orders : resultsList) {
                    for (Object order : orders) {
                        JsonObject orderJson = (JsonObject) order;
                        String creation_date = "";
                        if (orderJson.getString("creation_date") != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date",""), formatter);
                            creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                        }
                        orderJson.put("creation_date", creation_date);
                        String idEquipment = orderJson.getString("equipment_key","");
                        if (equipmentsArray.size() > 0 && idEquipment != null) {
                            for (int i = 0; i < equipmentsArray.size(); i++) {
                                JsonObject equipment = equipmentsArray.getJsonObject(i);
                                if (idEquipment.equals(equipment.getString("id"))) {
                                    JsonObject priceDetails = equipment.getJsonObject("priceDetails");
                                    double price = 0.0;
                                    if (priceDetails.getDouble("priceTTC") != null && orderJson.getInteger("amount") != null) {
                                        price = priceDetails.getDouble("priceTTC",0.0) * orderJson.getInteger("amount", 0);
                                    }
                                    orderJson.put("price", price);
                                    orderJson.put("name", equipment.getString("titre"));
                                    orderJson.put("image", equipment.getString("urlcouverture", "/crre/public/img/pages-default.png"));
                                    orderJson.put("ean", equipment.getString("ean", idEquipment));
                                    orderJson.put("_index", equipment.getString("type","NaN"));
                                    orderJson.put("editeur", equipment.getString("editeur","NaN"));
                                    orderJson.put("distributeur", equipment.getString("distributeur","NaN"));
                                    break;
                                } else if (equipmentsArray.size() - 1 == i) {
                                    equipmentNotFound(orderJson, idEquipment);
                                }
                            }
                        } else {
                            equipmentNotFound(orderJson, idEquipment);
                        }
                    }
                }
                renderJson(request, new JsonArray(resultsList));
            } else {
                log.error(equipments.left());
                badRequest(request);
            }
        });
    }

    private void equipmentNotFound(JsonObject orderJson, String idEquipment) {
        orderJson.put("price", 0.0);
        orderJson.put("name", "Manuel introuvable dans le catalogue");
        orderJson.put("image", "/crre/public/img/pages-default.png");
        orderJson.put("ean", idEquipment);
        orderJson.put("_index", "NaN");
        orderJson.put("editeur", "NaN");
        orderJson.put("distributeur", "NaN");
    }

    private void getSearchByIdsOld(HttpServerRequest request, List<JsonArray> resultsList) {
        JsonArray finalResult = new JsonArray();
        for (JsonArray orders : resultsList) {
            finalResult.add(orders);
            for (Object order : orders) {
                JsonObject orderJson = (JsonObject) order;
                double price = 0.0;
                if (orderJson.getString("equipment_price") != null && orderJson.getInteger("amount") != null) {
                    price = Double.parseDouble(orderJson.getString("equipment_price")) * orderJson.getInteger("amount", 0);
                }
                orderJson.put("price", price);
                String creation_date = "";
                if (orderJson.getString("creation_date") != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date", ""), formatter);
                    creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                }
                orderJson.put("creation_date", creation_date);
            }
        }
        renderJson(request, finalResult);
    }

    @Put("/region/orders/:status")
    @ApiDoc("update region orders with status")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void validateOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                orders -> UserUtils.getUserInfos(eb, request,
                        userInfos -> {
                            try {
                                String status = request.getParam("status");
                                List<String> params = new ArrayList<>();
                                for (Object id : orders.getJsonArray("ids")) {
                                    params.add(id.toString());
                                }
                                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                                String justification = orders.getString("justification");
                                JsonArray ordersList = orders.getJsonArray("orders");
                                updatePurseLicence(status, ordersList, 0, purse -> {
                                    if(purse.isRight()){
                                        updateStatusRecursive(request, status, ids, justification, 0);
                                    } else {
                                        unauthorized(request);
                                    }
                                });
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                            }
                        }));
    }

    private void updateStatusRecursive(HttpServerRequest request, String status, List<Integer> ids, String justification, int e) {
        List<Integer> idsSplit = ids.subList(e * 25000, min( (e + 1) * 25000, ids.size() ) );
        orderRegionService.updateOrders(idsSplit, status, justification, event -> {
            if (event.isRight()) {
                if ( (e + 1) * 25000 < ids.size() ) {
                    updateStatusRecursive(request, status, ids, justification, e + 1);
                } else {
                    request.response().setStatusCode(200).end();
                }
            } else {
                LOGGER.error("An error when you want get id after create order region ",
                        event.left().getValue());
                request.response().setStatusCode(400).end();
            }
        });
    }

    private void updatePurseLicence(String status, JsonArray ordersList, int i, Handler<Either<String, JsonObject>> handler) {
        JsonObject order = ordersList.getJsonObject(i);
        Double price = order.getDouble("price", (double) 0);

        if (order.getString("status").equals("REJECTED")) {
            if (status.equals("valid")) {
                updatePurseLicence(order, "-", price, order.getString("use_credit", "none"))
                        .onSuccess(res -> {
                            if (i + 1 < ordersList.size()){
                                updatePurseLicence(status, ordersList, i + 1, handler);
                            } else {
                                handler.handle(new Either.Right<>(new JsonObject()));
                            }
                        })
                        .onFailure(err -> {
                            LOGGER.error("[CRRE] OrderRegionController@updatePurseLicence : " + err.getMessage() + ", " + err.getCause(), err.getCause());
                            updatePurseLicenceRoolback(status, ordersList, i - 1, handler);
                        });
            }
        } else if (status.equals("rejected")) {
            updatePurseLicence(order, "+", price, order.getString("use_credit", "none"))
                    .onSuccess(res -> {
                        if (i + 1 < ordersList.size()){
                            updatePurseLicence(status, ordersList, i + 1, handler);
                        } else {
                            handler.handle(new Either.Right<>(new JsonObject()));
                        }
                    })
                    .onFailure(err -> {
                        LOGGER.error("[CRRE] OrderRegionController@updatePurseLicence : " + err.getMessage() + ", " + err.getCause(), err.getCause());
                        updatePurseLicenceRoolback(status, ordersList, i - 1, handler);
                    });
        } else if (i + 1 < ordersList.size()){
            updatePurseLicence(status, ordersList, i + 1, handler);
        } else {
            handler.handle(new Either.Right<>(new JsonObject()));
        }
    }

    private void updatePurseLicenceRoolback(String status, JsonArray ordersList, int i, Handler<Either<String, JsonObject>> handler) {
        JsonObject order = ordersList.getJsonObject(i);
        Double price = Double.parseDouble(order.getDouble("price").toString());

        if (order.getString("status").equals("REJECTED")) {
            if (status.equals("valid")) {
                updatePurseLicence(order, "+", price, order.getString("use_credit", "none"))
                        .onSuccess(res -> {
                            if (i - 1 >= 0){
                                updatePurseLicenceRoolback(status, ordersList, i - 1, handler);
                            } else {
                                LOGGER.info("[CRRE] OrderRegionController@updatePurseLicenceRoolback : roolback purse is success");
                                handler.handle(new Either.Left<>("[CRRE] OrderRegionController@updatePurseLicenceRoolback"));
                            }
                        })
                        .onFailure(err -> {
                            LOGGER.error("[CRRE] OrderRegionController@updatePurseLicenceRoolback : " + err.getMessage() + ", " + err.getCause(), err.getCause());
                            handler.handle(new Either.Left<>("[CRRE] OrderRegionController@updatePurseLicenceRoolback : " + err.getMessage() + ", " + err.getCause()));
                        });
            }
        } else if (status.equals("rejected")) {
            updatePurseLicence(order, "-", price, order.getString("use_credit", "none"))
                    .onSuccess(res -> {
                        if (i - 1 >= 0){
                            updatePurseLicenceRoolback(status, ordersList, i - 1, handler);
                        } else {
                            LOGGER.info("[CRRE] OrderRegionController@updatePurseLicenceRoolback : roolback purse is success");
                            handler.handle(new Either.Left<>("[CRRE] OrderRegionController@updatePurseLicenceRoolback"));
                        }
                    })
                    .onFailure(err -> {
                        LOGGER.error("[CRRE] OrderRegionController@updatePurseLicenceRoolback : " + err.getMessage() + ", " + err.getCause(), err.getCause());
                        handler.handle(new Either.Left<>("[CRRE] OrderRegionController@updatePurseLicenceRoolback : " + err.getMessage() + ", " + err.getCause()));
                    });
        } else if (i - 1 >= 0){
            updatePurseLicenceRoolback(status, ordersList, i - 1, handler);
        } else {
            LOGGER.info("[CRRE] OrderRegionController@updatePurseLicenceRoolback : roolback purse is success");
            handler.handle(new Either.Left<>("[CRRE] OrderRegionController@updatePurseLicenceRoolback"));
        }
    }


    private Future<JsonObject> updatePurseLicence(JsonObject newOrder, String operation, Double price, String use_credit) {
        Future<JsonObject> updateFuture = Future.future();
        if (!use_credit.equals("none")) {
            switch (use_credit) {
                case "licences": {
                    structureService.updateAmountLicence(newOrder.getString("id_structure"), operation,
                            newOrder.getInteger("amount"),
                            handlerJsonObject(updateFuture));
                    break;
                }
                case "consumable_licences": {
                    structureService.updateAmountConsumableLicence(newOrder.getString("id_structure"), operation,
                            newOrder.getInteger("amount"),
                            handlerJsonObject(updateFuture));
                    break;
                }
                case "credits": {
                    purseService.updatePurseAmount(price,
                            newOrder.getString("id_structure"), operation, false,
                            handlerJsonObject(updateFuture));
                    break;
                }
                case "consumable_credits": {
                    purseService.updatePurseAmount(price,
                            newOrder.getString("id_structure"), operation, true,
                            handlerJsonObject(updateFuture));
                    break;
                }
            }
        } else {
            updateFuture = Future.succeededFuture();
        }
        return updateFuture;

    }

    @Post("region/orders/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void export(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, orderRegions ->
                UserUtils.getUserInfos(eb, request, user -> {
                    List<String> idsOrders = orderRegions.getJsonArray("idsOrders").getList();
                    List<String> idsEquipments = orderRegions.getJsonArray("idsEquipments").getList();
                    List<String> idsStructures = orderRegions.getJsonArray("idsStructures").getList();
                    Boolean old = orderRegions.getBoolean("old");
                    JsonObject params = new JsonObject()
                            .put("idsOrders", idsOrders)
                            .put("idsEquipments", idsEquipments)
                            .put("idsStructures", idsStructures)
                            .put("idUser", user.getUserId())
                            .put("old", old);
                    JsonObject exportParams = new JsonObject()
                            .put("params", params)
                            .put("action", "saveOrderRegion");
                    if (idsOrders.size() > 1000) {
                        launchWorker(exportParams,null);
                        ok(request);
                    } else {
                        launchWorker(exportParams, request);
                    }
                })
        );
    }

    private void launchWorker(JsonObject params, HttpServerRequest request) {
        eb.send(ExportWorker.class.getSimpleName(), params, new DeliveryOptions().setSendTimeout(1000 * 1000L),
                handlerToAsyncHandler(eventExport -> {
                    if (eventExport.body().getString("status").equals("ok")) {
                        if (request != null) {
                            //Export CSV
                            request.response()
                                    .putHeader("Content-Type", "text/csv; charset=utf-8")
                                    .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                    .end(eventExport.body().getJsonObject("data").getString("csvFile"));
                        }
                    } else {
                        log.error("Ko calling worker " + eventExport.body().toString());
                        if (request != null) {
                            renderError(request);
                        }
                    }
                }
        ));
    }

    @Get("region/orders/old/status")
    @ApiDoc("Update status of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void updateStatusOrders(final HttpServerRequest request) {
        // LDE function that returns status widh order id
        orderRegionService.getStatusByOrderId(event -> {
            if (event.isRight()) {
                JsonArray listIdOrders = event.right().getValue();
                for (int i = 0; i < listIdOrders.size(); i++) {
                    listIdOrders.getJsonObject(i).put("status", randomStatus());
                }
                if (listIdOrders.size() > 0) {
                    // Update status in sql base
                    orderRegionService.updateStatus(listIdOrders, event2 -> {
                        if (event2.isRight()) {
                            renderJson(request, event2.right().getValue());
                        } else {
                            log.error(event2.left().getValue());
                            badRequest(request);
                        }
                    });
                } else {
                    ok(request);
                }
            } else {
                log.error(event.left().getValue());
                badRequest(request);
            }
        });
    }

    int randomStatus() {
        int[] tab = {1, 2, 3, 4, 6, 7, 9, 10, 14, 15, 20, 35, 55, 57, 58, 59};
        Random rn = new Random();
        int range = 15 + 1;
        int randomNum = rn.nextInt(range);
        return tab[randomNum];
    }

    @Post("region/orders/library")
    @ApiDoc("Generate and send mail to library")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void exportLibrary(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, orderRegions -> {
            UserUtils.getUserInfos(eb, request, user -> {
                List<Integer> idsOrders = orderRegions.getJsonArray("idsOrders").getList();
                List<String> idsEquipments = orderRegions.getJsonArray("idsEquipments").getList();
                List<String> idsStructures = orderRegions.getJsonArray("idsStructures").getList();
                generateLogs(request, idsOrders, idsEquipments, idsStructures, user);
            });
        });
    }

    private void generateLogs(HttpServerRequest request, List<Integer> idsOrders, List<String> idsEquipments,
                              List<String> idsStructures, UserInfos user) {
        JsonArray idStructures = new JsonArray();
        for (String structureId : idsStructures) {
            idStructures.add(structureId);
        }

        List<Future> futures = new ArrayList<>();
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();
        futures.add(structureFuture);
        futures.add(equipmentsFuture);

        getOrdersRecursively(0, idsOrders, futures);

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray equipments = equipmentsFuture.result();
                JsonArray ordersRegionId = new JsonArray(), ordersClientId = new JsonArray(), orderRegion = new JsonArray();
                for (int i = 2; i < futures.size(); i++) {
                    orderRegion.addAll((JsonArray) futures.get(i).result());
                }
                orderRegionService.beautifyOrders(structures, orderRegion, equipments, ordersClientId, ordersRegionId);
                JsonArray orderRegionClean = new JsonArray();
                for (int i = 0; i < orderRegion.size() ; i++){
                    JsonObject order = orderRegion.getJsonObject(i);
                    if (order.getString("status","").equals("REJECTED") && order.getDouble("price") != null &&
                            !order.getDouble("price", 0.0).equals(0.0)) {
                        orderRegionClean.add(order);
                    }
                }
                if (orderRegionClean.size() > 0) {
                    updatePurseLicence("valid", orderRegionClean, 0, purse -> {
                        if (purse.isRight()) {
                            sendMailLibraryAndRemoveWaitingAdmin(request, user, orderRegion, ordersClientId, ordersRegionId);
                        } else {
                            unauthorized(request);
                        }
                    });
                } else {
                    sendMailLibraryAndRemoveWaitingAdmin(request, user, orderRegion, ordersClientId, ordersRegionId);
                }
            }
        });

        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
        searchByIds(idsEquipments, handlerJsonArray(equipmentsFuture));
    }

    private void getOrdersRecursively(int e, List<Integer> listOrders, List<Future> futures) {
        Future<JsonArray> orderRegionFuture = Future.future();
        futures.add(orderRegionFuture);
        List<Integer> subList = listOrders.subList(e * 5000, min((e +1) * 5000, listOrders.size()) );
        orderRegionService.getOrdersRegionById(subList, false, handlerJsonArray(orderRegionFuture));
        if ((e + 1) * 5000 < listOrders.size()) {
            getOrdersRecursively(e + 1, listOrders, futures);
        }
    }

    private void sendMailLibraryAndRemoveWaitingAdmin(HttpServerRequest request, UserInfos user,
                                                      JsonArray orderRegion, JsonArray ordersClientId, JsonArray ordersRegionId) {
        JsonArray attachment = new fr.wseduc.webutils.collections.JsonArray();

        int e = 0;
        while (e * 100000 < orderRegion.size()) {
            JsonArray orderRegionSplit = new JsonArray();
            for(int i = e * 100000; i < min((e +1) * 100000, orderRegion.size()); i ++){
                orderRegionSplit.add(orderRegion.getJsonObject(i));
            }
            JsonObject data = orderRegionService.generateExport(orderRegionSplit);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy-HHmmss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
            String title = "DD" + simpleDateFormat.format(new Date());
            attachment.add(new JsonObject()
                    .put("name", title + ".csv")
                    .put("content", Base64.getEncoder().encodeToString(data.getString("csvFile").getBytes(StandardCharsets.UTF_8)))
                    .put("nbEtab", data.getInteger("nbEtab")));
            e++;
        }
        sendMails(request, user, orderRegion, ordersClientId, ordersRegionId, attachment, 0);
    }

    private void sendMails(HttpServerRequest request, UserInfos user, JsonArray orderRegion, JsonArray ordersClientId,
                           JsonArray ordersRegionId, JsonArray attachment, int e) {
        JsonArray singleAttachment = new JsonArray().add(attachment.getJsonObject(e));
        String mail = this.mail.getString("address");
        emailSender.sendMail(request, mail, "Demande Libraire CRRE",
                "Demande Libraire CRRE ; csv : " + attachment.getJsonObject(e).getString("name"), singleAttachment, message -> {
                    if (!message.isRight()) {
                        log.error("[CRRE@OrderRegionController.sendMails] " +
                                "An error has occurred sendMail : " + message.left());
                        renderError(request);
                    } else {
                        if (e + 1 < attachment.size()){
                            sendMails(request, user, orderRegion, ordersClientId, ordersRegionId, attachment, e +1);
                        } else {
                            insertQuote(request, user, attachment, 0, orderRegion, ordersClientId, ordersRegionId);
                        }
                    }
                });
    }

    private void insertQuote(HttpServerRequest request, UserInfos user, JsonArray attachment, int e,
                             JsonArray orderRegion, JsonArray ordersClientId, JsonArray ordersRegionId) {
        JsonObject singleAttachment = attachment.getJsonObject(e);
        Integer nbEtab = singleAttachment.getInteger("nbEtab");
        String csvFileBase64 = singleAttachment.getString("content");
        String title = singleAttachment.getString("name").replace(".csv","");
        quoteService.insertQuote(user, nbEtab, csvFileBase64, title, response2 -> {
            if (response2.isRight()) {
                if (e + 1 < attachment.size()){
                    insertQuote(request, user, attachment, e +1, orderRegion, ordersClientId, ordersRegionId);
                } else {
                    insertAndDeleteOrders(request, orderRegion, ordersClientId, ordersRegionId);
                }
            } else {
                log.error("[CRRE@OrderRegionController.insertQuote] " +
                        "An error has occurred insertQuote : " + response2.left().getValue());
                renderError(request);
            }
        });
    }

    private void insertAndDeleteOrders(HttpServerRequest request, JsonArray orderRegion, JsonArray ordersClientId, JsonArray ordersRegionId) {
        try {
            orderRegionService.recursiveInsertOldClientOrders(orderRegion, 0, response -> {
                if (response.isRight()) {
                    try {
                        Future<JsonObject> insertOldOrdersFuture = Future.future();
                        Future<JsonObject> deleteOrderClientFuture = Future.future();
                        Future<JsonObject> deleteOrderRegionFuture = Future.future();
                        orderRegionService.deletedOrdersRecursive(ordersClientId, "order_client_equipment", 0,
                                handlerJsonObject(deleteOrderClientFuture));
                        orderRegionService.deletedOrdersRecursive(ordersRegionId, "order-region-equipment", 0,
                                handlerJsonObject(deleteOrderRegionFuture));
                        orderRegionService.recursiveInsertOldOrders(orderRegion, false, 0,
                                handlerJsonObject(insertOldOrdersFuture));
                        CompositeFuture.all(insertOldOrdersFuture, deleteOrderClientFuture, deleteOrderRegionFuture).setHandler(event -> {
                            if (event.succeeded()) {
                                ok(request);
                                log.info("[CRRE@OrderRegionController.insertAndDeleteOrders] " +
                                        "Orders Deleted and insert in old table was successfull");
                            } else {
                                log.error("[CRRE@OrderRegionController.insertAndDeleteOrders] " +
                                        "An error has occurred in CompositeFuture : " + event.cause().getMessage());
                            }
                        });
                    } catch (ParseException err) {
                        err.printStackTrace();
                        log.error(err.getMessage());
                    }
                } else {
                    log.error("[CRRE@OrderRegionController.insertAndDeleteOrders] " +
                            "An error has occurred insertOldClientOrders : " + response.left().getValue());
                }
            });
        } catch (ParseException err) {
            err.printStackTrace();
            log.error(err.getMessage());
        }
    }
}
