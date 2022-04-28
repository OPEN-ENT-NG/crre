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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static fr.openent.crre.controllers.LogController.UTF8_BOM;
import static fr.openent.crre.controllers.OrderController.exportPriceComment;
import static fr.openent.crre.helpers.ElasticSearchHelper.*;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.extractedEquipmentInfo;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

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
    @ApiDoc("Create orders for region")
    @SecuredAction(Crre.VALIDATOR_RIGHT)
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user ->
                    RequestUtils.bodyToJson(request, orders -> {
                        if (!orders.isEmpty()) {
                            JsonArray ordersList = orders.getJsonArray("orders");
                            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                            orderRegionService.getLastProject(lastProject -> {
                                if (lastProject.isRight()) {
                                    String last = lastProject.right().getValue().getString("title");
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
                                    String finalTitle = title;
                                    orderRegionService.createProject(title, idProject -> {
                                        if (idProject.isRight()) {
                                            Integer idProjectRight = idProject.right().getValue().getInteger("id");
                                            Logging.insert(eb,
                                                    request,
                                                    null,
                                                    Actions.CREATE.toString(),
                                                    idProjectRight.toString(),
                                                    new JsonObject().put("id", idProjectRight).put("title", finalTitle));
                                            for (int i = 0; i < ordersList.size(); i++) {
                                                List<Future> futures = new ArrayList<>();
                                                JsonObject newOrder = ordersList.getJsonObject(i);
                                                Future<JsonObject> createOrdersRegionFuture = Future.future();
                                                futures.add(createOrdersRegionFuture);
                                                Double price = Double.parseDouble(newOrder.getDouble("price").toString())
                                                        * newOrder.getInteger("amount");
                                                updatePurseLicence(futures, newOrder, "-", price, newOrder.getString("use_credit", "none"));
                                                orderRegionService.createOrdersRegion(newOrder, user, idProjectRight,
                                                        handlerJsonObject(createOrdersRegionFuture));
                                                int finalI = i;
                                                CompositeFuture.all(futures).setHandler(event -> {
                                                    if (event.succeeded()) {
                                                        Number idReturning = createOrdersRegionFuture.result().getInteger("id");
                                                        Logging.insert(eb,
                                                                request,
                                                                Contexts.ORDERREGION.toString(),
                                                                Actions.CREATE.toString(),
                                                                idReturning.toString(),
                                                                new JsonObject().put("order region", newOrder));
                                                        if (finalI == ordersList.size() - 1) {
                                                            request.response().setStatusCode(201).end();
                                                        }
                                                    } else {
                                                        LOGGER.error("An error when you want get id after create order region ",
                                                                event.cause());
                                                        request.response().setStatusCode(400).end();
                                                    }
                                                });
                                            }
                                        } else {
                                            LOGGER.error("An error when you want get id after create project " + idProject.left());
                                            request.response().setStatusCode(400).end();
                                        }
                                    });
                                }
                            });
                        }
                    }));

        } catch (Exception e) {
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
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
                                orderRegionService.insertOldOrders(ordersRegion, true, event1 -> {
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

    @Get("/ordersRegion/orders")
    @ApiDoc("get all orders of each project")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getOrdersByProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null
                    && Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            Boolean old = Boolean.valueOf(request.getParam("old"));
            List<String> projectIds = request.params().getAll("project_id");
            List<Future> futures = new ArrayList<>();
            for (String id : projectIds) {
                Future<JsonArray> projectIdFuture = Future.future();
                futures.add(projectIdFuture);
                int idProject = Integer.parseInt(id);
                orderRegionService.getAllOrderRegionByProject(idProject, filterRejectedSentOrders, old, handlerJsonArray(projectIdFuture));
            }
            getCompositeFutureAllOrderRegionByProject(request, old, futures);
        });
    }

    private void getCompositeFutureAllOrderRegionByProject(HttpServerRequest request, Boolean
            old, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                List<JsonArray> resultsList = event.result().list();
                if (!old) {
                    List<String> listIdsEquipment = new ArrayList<>();
                    for (JsonArray orders : resultsList) {
                        for (Object order : orders) {
                            listIdsEquipment.add(((JsonObject) order).getString("equipment_key"));
                        }
                    }
                    getSearchByIds(request, resultsList, listIdsEquipment);
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
                JsonArray finalResult = new JsonArray();
                for (JsonArray orders : resultsList) {
                    finalResult.add(orders);
                    for (Object order : orders) {
                        JsonObject orderJson = (JsonObject) order;
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date"), formatter);
                        String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                        orderJson.put("creation_date", creation_date);
                        String idEquipment = orderJson.getString("equipment_key");
                        JsonArray equipmentsArray = equipments.right().getValue();
                        if (equipmentsArray.size() > 0) {
                            for (int i = 0; i < equipmentsArray.size(); i++) {
                                JsonObject equipment = equipmentsArray.getJsonObject(i);
                                if (idEquipment.equals(equipment.getString("id"))) {
                                    JsonObject priceDetails = getPriceTtc(equipment);
                                    double price = priceDetails.getDouble("priceTTC") * orderJson.getInteger("amount");
                                    orderJson.put("price", price);
                                    orderJson.put("name", equipment.getString("titre"));
                                    orderJson.put("image", equipment.getString("urlcouverture"));
                                    orderJson.put("ean", equipment.getString("ean"));
                                    orderJson.put("_index", equipment.getString("type"));
                                    orderJson.put("editeur", equipment.getString("editeur"));
                                    orderJson.put("distributeur", equipment.getString("distributeur"));
                                    break;
                                } else if (equipmentsArray.size() - 1 == i) {
                                    orderJson.put("price", 0.0);
                                    orderJson.put("name", "Manuel introuvable dans le catalogue");
                                    orderJson.put("image", "/crre/public/img/pages-default.png");
                                    orderJson.put("ean", idEquipment);
                                    orderJson.put("_index", "NaN");
                                    orderJson.put("editeur", "NaN");
                                    orderJson.put("distributeur", "NaN");
                                }
                            }
                        } else {
                            orderJson.put("price", 0.0);
                            orderJson.put("name", "Manuel introuvable dans le catalogue");
                            orderJson.put("image", "/crre/public/img/pages-default.png");
                            orderJson.put("ean", idEquipment);
                            orderJson.put("_index", "NaN");
                            orderJson.put("editeur", "NaN");
                            orderJson.put("distributeur", "NaN");
                        }
                    }
                }
                renderJson(request, finalResult);
            } else {
                log.error(equipments.left());
                badRequest(request);
            }
        });
    }

    private void getSearchByIdsOld(HttpServerRequest request, List<JsonArray> resultsList) {
        JsonArray finalResult = new JsonArray();
        for (JsonArray orders : resultsList) {
            finalResult.add(orders);
            for (Object order : orders) {
                JsonObject orderJson = (JsonObject) order;
                double price = Double.parseDouble(orderJson.getString("equipment_price")) * orderJson.getInteger("amount");
                orderJson.put("price", price);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date"), formatter);
                String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
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
                                List<Future> futures = new ArrayList<>();
                                for (int i = 0; i < ordersList.size(); i++) {
                                    JsonObject newOrder = ordersList.getJsonObject(i);
                                    Double price = Double.parseDouble(newOrder.getDouble("price").toString());
                                    if (newOrder.getString("status").equals("REJECTED")) {
                                        if (status.equals("valid")) {
                                            updatePurseLicence(futures, newOrder, "-", price,
                                                    newOrder.getString("use_credit", "none"));
                                        }
                                    } else {
                                        if (status.equals("rejected")) {
                                            updatePurseLicence(futures, newOrder, "+", price,
                                                    newOrder.getString("use_credit", "none"));
                                        }
                                    }
                                }
                                CompositeFuture.all(futures).setHandler(event -> {
                                    if (event.succeeded()) {
                                        orderRegionService.updateOrders(ids, status, justification,
                                                Logging.defaultResponsesHandler(eb,
                                                        request,
                                                        Contexts.ORDERREGION.toString(),
                                                        Actions.UPDATE.toString(),
                                                        params,
                                                        null));
                                    } else {
                                        LOGGER.error("An error when you want get id after create order region ",
                                                event.cause());
                                        request.response().setStatusCode(400).end();
                                    }
                                });
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                            }
                        }));

    }

    private void updatePurseLicence(List<Future> futures, JsonObject newOrder, String operation, Double
            price, String use_credit) {
        if (!use_credit.equals("none")) {
            Future<JsonObject> updateFuture = Future.future();
            futures.add(updateFuture);
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
                            newOrder.getString("id_structure"), operation,
                            handlerJsonObject(updateFuture));
                    break;
                }
            }
        }
    }

    @Get("region/orders/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void export(final HttpServerRequest request) {
        List<String> params = request.params().getAll("id");
        List<String> idsEquipment = request.params().getAll("equipment_key");
        List<String> params3 = request.params().getAll("id_structure");
        Boolean old = Boolean.valueOf(request.getParam("old"));
        generateLogs(request, params, idsEquipment, params3, null, old, false);
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
        UserUtils.getUserInfos(eb, request, user -> {
            List<String> params = request.params().getAll("id");
            List<String> idsEquipment = request.params().getAll("equipment_key");
            List<String> params3 = request.params().getAll("id_structure");
            generateLogs(request, params, idsEquipment, params3, user, true, true);
        });
    }

    private void generateLogs(HttpServerRequest
                                      request, List<String> params, List<String> idsEquipment, List<String> params3,
                              UserInfos user, Boolean old, boolean library) {
        JsonArray idStructures = new JsonArray();
        for (String structureId : params3) {
            idStructures.add(structureId);
        }
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> orderRegionFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();

        CompositeFuture.all(structureFuture, orderRegionFuture, equipmentsFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray orderRegion = orderRegionFuture.result();
                JsonArray equipments = equipmentsFuture.result();
                JsonArray ordersClient = new JsonArray(), ordersRegion = new JsonArray();
                beautifyOrders(structures, orderRegion, equipments, ordersClient, ordersRegion);
                if (library) {
                    sendMailLibraryAndRemoveWaitingAdmin(request, user, structures, orderRegion, ordersClient, ordersRegion);
                } else {
                    //Export CSV
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                            .end(generateExport(request, orderRegion));
                }

            }
        });
        if (library) {
            orderRegionService.getOrdersRegionById(idsOrders, false, handlerJsonArray(orderRegionFuture));
        } else {
            orderRegionService.getOrdersRegionById(idsOrders, old, handlerJsonArray(orderRegionFuture));
        }
        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
        searchByIds(idsEquipment, handlerJsonArray(equipmentsFuture));
    }

    private void sendMailLibraryAndRemoveWaitingAdmin(HttpServerRequest request, UserInfos user, JsonArray structures,
                                                      JsonArray orderRegion, JsonArray ordersClient, JsonArray ordersRegion) {
        int nbEtab = structures.size();
        String csvFile = generateExport(request, orderRegion);
        Future<JsonObject> insertOldOrdersFuture = Future.future();
        Future<JsonObject> deleteOldOrderClientFuture = Future.future();
        Future<JsonObject> deleteOldOrderRegionFuture = Future.future();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy-HHmm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        String title = "DD" + simpleDateFormat.format(new Date());
        JsonArray attachment = new fr.wseduc.webutils.collections.JsonArray();
        attachment.add(new JsonObject()
                .put("name", title + ".csv")
                .put("content", Base64.getEncoder().encodeToString(csvFile.getBytes(StandardCharsets.UTF_8))));
        String mail = this.mail.getString("address");
        emailSender.sendMail(request, mail, "Demande Libraire CRRE",
                csvFile, attachment, message -> {
                    if (!message.isRight()) {
                        log.error("[CRRE@OrderRegionController.sendMailLibraryAndRemoveWaitingAdmin] " +
                                "An error has occurred sendMail : " + message.left());
                        renderError(request);
                    } else {
                        try {
                            orderRegionService.insertOldClientOrders(orderRegion, response -> {
                                if (response.isRight()) {
                                    quoteService.insertQuote(user, nbEtab, csvFile, title, response2 -> {
                                        if (response2.isRight()) {
                                            renderJson(request, response2.right().getValue());
                                        } else {
                                            log.error("[CRRE@OrderRegionController.sendMailLibraryAndRemoveWaitingAdmin] " +
                                                    "An error has occurred insertQuote : " + message.left());
                                            renderError(request);
                                        }
                                    });
                                    try {
                                        orderRegionService.deletedOrders(ordersClient, "order_client_equipment",
                                                handlerJsonObject(deleteOldOrderClientFuture));
                                        orderRegionService.deletedOrders(ordersRegion, "order-region-equipment",
                                                handlerJsonObject(deleteOldOrderRegionFuture));
                                        orderRegionService.insertOldOrders(orderRegion, false,
                                                handlerJsonObject(insertOldOrdersFuture));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        log.error(e.getMessage());
                                    }
                                } else {
                                    log.error("[CRRE@OrderRegionController.sendMailLibraryAndRemoveWaitingAdmin] " +
                                            "An error has occurred insertOldClientOrders : " + message.left());
                                    renderError(request);
                                }
                            });
                        } catch (ParseException e) {
                            e.printStackTrace();
                            log.error(e.getMessage());
                        }
                    }
                });

    }

    private void beautifyOrders(JsonArray structures, JsonArray orderRegion, JsonArray equipments, JsonArray
            ordersClient, JsonArray ordersRegion) {
        JsonObject order;
        JsonObject equipment;
        for (int i = 0; i < orderRegion.size(); i++) {
            order = orderRegion.getJsonObject(i);
            // Skip offers
            if (!order.containsKey("totalPriceTTC")) {
                if (order.containsKey("owner_name")) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date"), formatter);
                    String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                    order.put("creation_date", creation_date);
                }
                ordersRegion.add(order.getLong("id"));
                ordersClient.add(order.getLong("id_order_client_equipment"));

                for (int j = 0; j < equipments.size(); j++) {
                    equipment = equipments.getJsonObject(j);
                    if (equipment.getString("id").equals(order.getString("equipment_key"))) {
                        JsonObject priceDetails = getPriceTtc(equipment);
                        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
                        DecimalFormat df2 = new DecimalFormat("#.##", dfs);
                        double priceTTC = priceDetails.getDouble("priceTTC") * order.getInteger("amount");
                        double priceHT = priceDetails.getDouble("prixht") * order.getInteger("amount");
                        order.put("priceht", priceDetails.getDouble("prixht"));
                        order.put("tva5", (priceDetails.containsKey("partTVA5")) ?
                                priceDetails.getDouble("partTVA5") + priceDetails.getDouble("prixht") : null);
                        order.put("tva20", (priceDetails.containsKey("partTVA20")) ?
                                priceDetails.getDouble("partTVA20") + priceDetails.getDouble("prixht") : null);
                        order.put("unitedPriceTTC", priceDetails.getDouble("priceTTC"));
                        order.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
                        order.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
                        extractedEquipmentInfo(order, equipment);
                        if (equipment.getJsonArray("disciplines").size() > 0) {
                            order.put("grade", equipment.getJsonArray("disciplines").getJsonObject(0).getString("libelle"));
                        } else {
                            order.put("grade", "");
                        }
                        putStructuresNameUAI(structures, order);
                        putEANLDE(equipment, order);
                        getUniqueTypeCatalogue(order, equipment);
                        if (equipment.getString("type").equals("articlenumerique")) {
                            JsonArray offers = computeOffers(equipment, order);
                            if (offers.size() > 0) {
                                JsonArray orderOfferArray = new JsonArray();
                                int freeAmount = 0;
                                for (int k = 0; k < offers.size(); k++) {
                                    JsonObject orderOffer = new JsonObject();
                                    orderOffer.put("name", offers.getJsonObject(k).getString("name"));
                                    orderOffer.put("titre", offers.getJsonObject(k).getString("titre"));
                                    orderOffer.put("amount", offers.getJsonObject(k).getLong("value"));
                                    freeAmount += offers.getJsonObject(k).getLong("value");
                                    orderOffer.put("ean", offers.getJsonObject(k).getString("ean"));
                                    orderOffer.put("unitedPriceTTC", 0);
                                    orderOffer.put("totalPriceHT", 0);
                                    orderOffer.put("totalPriceTTC", 0);
                                    orderOffer.put("typeCatalogue", order.getString("typeCatalogue"));
                                    orderOffer.put("creation_date", order.getString("creation_date"));
                                    orderOffer.put("id_structure", order.getString("id_structure"));
                                    orderOffer.put("campaign_name", order.getString("campaign_name"));
                                    orderOffer.put("id", "F" + order.getLong("id") + "_" + k);
                                    orderOffer.put("title", order.getString("title"));
                                    orderOffer.put("comment", offers.getJsonObject(k).getString("comment"));
                                    putStructuresNameUAI(structures, orderOffer);
                                    orderOfferArray.add(orderOffer);
                                    orderRegion.add(orderOffer);
                                }
                                order.put("total_free", freeAmount);
                                order.put("offers", orderOfferArray);
                            }
                        }
                    }
                }
            }
        }
    }

    private void getUniqueTypeCatalogue(JsonObject order, JsonObject equipment) {
        if (order.getString("use_credit").equals("consumable_licences")) {
            if (ArrayUtils.contains(equipment.getString("typeCatalogue").split(Pattern.quote("|")), "Numerique") ||
                    ArrayUtils.contains(equipment.getString("typeCatalogue").split(Pattern.quote("|")), "Consommable")) {
                order.put("typeCatalogue", "Consommable");
            } else {
                order.put("typeCatalogue", "ao_idf_conso");
            }
        } else {
            if (ArrayUtils.contains(equipment.getString("typeCatalogue").split(Pattern.quote("|")), "Numerique") ||
                    ArrayUtils.contains(equipment.getString("typeCatalogue").split(Pattern.quote("|")), "Consommable")) {
                order.put("typeCatalogue", "Numerique");
            } else {
                order.put("typeCatalogue", "ao_idf_pap");
            }
        }
    }

    private void putStructuresNameUAI(JsonArray structures, JsonObject order) {
        for (int s = 0; s < structures.size(); s++) {
            JsonObject structure = structures.getJsonObject(s);
            if (structure.getString("id").equals(order.getString("id_structure"))) {
                order.put("uai_structure", structure.getString("uai"));
                order.put("name_structure", structure.getString("name"));
                order.put("address_structure", structure.getString("address"));
            }
        }
    }

    private void putEANLDE(JsonObject equipment, JsonObject order) {
        if (equipment.getString("type").equals("articlenumerique")) {
            order.put("eanLDE", equipment.getJsonArray("offres").getJsonObject(0).getString("eanlibraire"));
        } else {
            order.put("eanLDE", equipment.getString("ean"));
        }
    }

    private static JsonArray computeOffers(JsonObject equipment, JsonObject order) {
        JsonArray leps = equipment.getJsonArray("offres").getJsonObject(0).getJsonArray("leps");
        Long amount = order.getLong("amount");
        int gratuit = 0;
        int gratuite = 0;
        JsonArray offers = new JsonArray();
        for (int i = 0; i < leps.size(); i++) {
            JsonObject offer = leps.getJsonObject(i);
            JsonArray conditions = offer.getJsonArray("conditions");
            JsonObject offerObject = new JsonObject().put("titre", "Manuel " +
                    offer.getJsonArray("licence").getJsonObject(0).getString("valeur"));
            if (conditions.size() > 1) {
                for (int j = 0; j < conditions.size(); j++) {
                    int condition = conditions.getJsonObject(j).getInteger("conditionGratuite");
                    if (amount >= condition && gratuit < condition) {
                        gratuit = condition;
                        gratuite = conditions.getJsonObject(j).getInteger("gratuite");
                    }
                }
            } else {
                gratuit = offer.getJsonArray("conditions").getJsonObject(0).getInteger("conditionGratuite");
                gratuite = (int) (offer.getJsonArray("conditions").getJsonObject(0).getInteger("gratuite") * Math.floor(amount / gratuit));
            }
            offerObject.put("value", gratuite);
            offerObject.put("ean", offer.getString("ean"));
            offerObject.put("name", offer.getString("titre"));
            offerObject.put("comment", equipment.getString("ean"));
            if (gratuite > 0) {
                offers.add(offerObject);
            }
        }
        return offers;
    }

    private  String generateExport(HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
            report.append(generateExportLine(logs.getJsonObject(i)));
        }
        return report.toString();
    }

    public  String getExportHeader(HttpServerRequest request) {
        return "ID unique" + ";" +
                I18n.getInstance().translate("crre.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Nom étab", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("UAI de l'étab", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Adresse de livraison", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Nom commande", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("CAMPAIGN", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("EAN de la ressource", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Titre de la ressource", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Editeur", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Distributeur", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Numérique", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("id de l'offre choisie", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Type", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.reassort", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("Prix HT de la ressource", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.5", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.20", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.unit.price.ttc", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.amountHT", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.amountTTC", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("csv.comment", getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    public  String generateExportLine(JsonObject log) {
        return (log.containsKey("id_project") ? log.getLong("id").toString() : log.getString("id")) + ";" +
                (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
                (log.getString("name_structure") != null ? log.getString("name_structure") : "") + ";" +
                (log.getString("uai_structure") != null ? log.getString("uai_structure") : "") + ";" +
                (log.getString("address_structure") != null ? log.getString("address_structure") : "") + ";" +
                (log.getString("title") != null ? log.getString("title") : "") + ";" +
                (log.getString("campaign_name") != null ? log.getString("campaign_name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                (log.getString("name") != null ? log.getString("name") : "") + ";" +
                (log.getString("editor") != null ? log.getString("editor") : "") + ";" +
                (log.getString("diffusor") != null ? log.getString("diffusor") : "") + ";" +
                (log.getString("type") != null ? log.getString("type") : "") + ";" +
                (log.getString("eanLDE") != null ? log.getString("eanLDE") : "") + ";" +
                (log.getString("typeCatalogue") != null ? log.getString("typeCatalogue") : "") + ";" +
                (log.getBoolean("reassort") != null ? (log.getBoolean("reassort") ? "Oui" : "Non") : "") + ";" +
                exportPriceComment(log)
                + "\n";
    }
}
