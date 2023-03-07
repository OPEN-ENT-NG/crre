package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.CreditTypeEnum;
import fr.openent.crre.core.enums.OrderClientEquipmentType;
import fr.openent.crre.helpers.*;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.*;
import fr.openent.crre.model.config.ConfigMailModel;
import fr.openent.crre.security.*;
import fr.openent.crre.service.*;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.openent.crre.service.impl.EmailSendService;
import fr.openent.crre.service.impl.ExportWorker;
import fr.openent.crre.utils.OrderUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final ConfigMailModel mail;
    private final WebClient webClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderService.class);
    private static final String LDE_ORDER_URI = "http://www.lde.fr/4dlink1/4dcgi/idf/ldc";
    private final NotificationService notificationService;

    public OrderRegionController(ServiceFactory serviceFactory) {
        this.emailSender = serviceFactory.getEmailSender();
        this.mail = serviceFactory.getConfig().getMail();
        this.orderRegionService = serviceFactory.getOrderRegionService();
        this.purseService = serviceFactory.getPurseService();
        this.quoteService = serviceFactory.getQuoteService();
        this.structureService = serviceFactory.getStructureService();
        this.webClient = serviceFactory.getWebClient();
        this.vertx = serviceFactory.getVertx();
        this.notificationService = serviceFactory.getNotificationService();
    }

    @Post("/region/orders")
    @ApiDoc("Create orders for region when we don't know the id of the project")
    @SecuredAction(Crre.VALIDATOR_RIGHT)
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user ->
                RequestUtils.bodyToJson(request, orders -> {
                    if (orders.isEmpty()) {
                        noContent(request);
                        return;
                    }
                    List<OrderRegionEquipmentModel> ordersRegionList = IModelHelper.toList(orders.getJsonArray(Field.ORDERS), OrderRegionEquipmentModel.class);
                    List<String> idsEquipment = ordersRegionList.stream()
                            .map(OrderRegionEquipmentModel::getEquipmentKey)
                            .collect(Collectors.toList());
                    List<Integer> IdOrderClientEquipmentList = ordersRegionList.stream()
                            .map(OrderRegionEquipmentModel::getIdOrderClientEquipment)
                            .collect(Collectors.toList());

                    searchByIds(idsEquipment, null)
                            .compose(equipments -> {
                                setPriceToOrder(orders.getJsonArray(Field.ORDERS), equipments);
                                return createProject(user);
                            })
                            .compose(projectModel -> createOrdersRegion(user, orders.getJsonArray(Field.ORDERS), projectModel.getId()))
                            .onSuccess(resJsonObject -> {
                                Renders.renderJson(request, resJsonObject, HttpResponseStatus.CREATED.code());
                                this.notificationService.sendNotificationPrescriber(IdOrderClientEquipmentList);
                            })
                            .onFailure(error -> Renders.renderError(request));

                })
        );
    }

    private Future<ProjectModel> createProject(UserInfos userInfos) {
        Promise<ProjectModel> promise = Promise.promise();

        orderRegionService.getLastProject()
                .compose(lastProject -> {
                    String last = lastProject.getString(Field.TITLE);
                    String date = LocalDate.now().format(DateTimeFormatter.ofPattern(DateHelper.DAY_FORMAT_DASH));
                    String title = "Commande_" + date;
                    if (last != null && title.equals(last.substring(0, last.length() - 2))) {
                        title = title + "_" + (Integer.parseInt(last.substring(last.length() - 1)) + 1);
                    } else {
                        title += "_1";
                    }
                    return orderRegionService.createProject(title);
                })
                .onSuccess(projectModel -> {
                    promise.complete(projectModel);
                    Logging.insert(userInfos, null, Actions.CREATE.toString(), String.valueOf(projectModel.getId()),
                            projectModel.toJson());
                })
                .onFailure(error -> {
                    LOGGER.error(String.format("[CRRE@%s::createProject] An error when create project %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    private Future<JsonObject> createOrdersRegion(UserInfos user, JsonArray ordersList, Integer idProject) {
        Promise<JsonObject> promise = Promise.promise();

        final List<JsonObject> orderList = ordersList.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .collect(Collectors.toList());

        final List<TransactionElement> transactionUpdatePurseList = orderList.stream()
                .map(order -> {
                    String structureId = order.getString(Field.ID_STRUCTURE);
                    Double price = order.getDouble(Field.PRICE) * order.getInteger(Field.AMOUNT);
                    CreditTypeEnum creditTypeEnum = CreditTypeEnum.getValue(order.getString(Field.USE_CREDIT), CreditTypeEnum.NONE);
                    return getUpdatePurseTransaction(structureId, "-", price, creditTypeEnum);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final List<TransactionElement> transactionCreate = orderList.stream()
                .map(order -> orderRegionService.getTransactionCreateOrdersRegion(order, idProject))
                .collect(Collectors.toList());

        final List<TransactionElement> transactionElementList = new ArrayList<>(transactionUpdatePurseList);
        transactionElementList.addAll(transactionCreate);

        String errorMessage = String.format("[CRRE@%s::createOrdersRegion] Fail to run transaction", this.getClass().getSimpleName());
        TransactionHelper.executeTransaction(transactionElementList, errorMessage)
                .onSuccess(transactionResult -> {
                    transactionCreate.stream()
                            .map(TransactionElement::getResult)
                            .map(transactionResultCreate -> transactionResultCreate.getJsonObject(0))
                            .filter(Objects::nonNull)
                            .map(OrderRegionEquipmentModel::new)
                            .forEach(orderRegionEquipmentModel -> Logging.insert(user, Contexts.ORDERREGION.toString(), Actions.CREATE.toString(),
                                    String.valueOf(orderRegionEquipmentModel.getId()), new JsonObject().put("order region", orderRegionEquipmentModel.toJson())));
                    promise.complete(new JsonObject().put(Field.IDPROJET, idProject));
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    @Post("/region/orders/:id")
    @ApiDoc("Create orders for region when we know the id of the project")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrderWithIdProject(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user ->
                    RequestUtils.bodyToJson(request, orders -> {
                        Integer idProject = request.getParam(Field.ID) != null ?
                                Integer.parseInt(request.getParam(Field.ID)) : null;
                        if (orders.isEmpty() || idProject == null) {
                            noContent(request);
                            return;
                        }
                        JsonArray ordersList = orders.getJsonArray(Field.ORDERS);
                        List<String> idsEquipment = ordersList.stream()
                                .filter(JsonObject.class::isInstance)
                                .map(JsonObject.class::cast)
                                .map(order -> order.getString(Field.EQUIPMENT_KEY))
                                .collect(Collectors.toList());
                        searchByIds(idsEquipment, null)
                                .compose(equipments -> {
                                    setPriceToOrder(ordersList, equipments);
                                    return createOrdersRegion(user, ordersList, idProject);
                                })
                                .onSuccess(resJsonObject -> Renders.renderJson(request, resJsonObject, HttpResponseStatus.CREATED.code()))
                                .onFailure(error -> Renders.renderError(request));
                    })
            );
        } catch (Exception e) {
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
        }
    }

    private static void setPriceToOrder(JsonArray ordersList, JsonArray equipmentsArray) {
        if (equipmentsArray.isEmpty()) return;
        //Pour chaque equipment on calcule le prix TTC
        equipmentsArray.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .forEach(equipment -> {
                    double priceTTC = getPriceTtc(equipment).getDouble(Field.PRICETTC);
                    equipment.put(Field.PRICE, priceTTC);
                });


        ordersList.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .forEach(order -> {
                    String idEquipment = order.getString(Field.EQUIPMENT_KEY);
                    equipmentsArray.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .filter(equipment -> idEquipment.equals(equipment.getString(Field.ID)))
                            .map(equipment -> equipment.getDouble(Field.PRICE))
                            .findFirst()
                            .ifPresent(price -> order.put(Field.PRICE, price));
                });
    }

    @Get("/orderRegion/projects")
    @ApiDoc("Get all projects")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureRight.class)
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
                        orderRegionService.getAllIdsStatus()
                                .onSuccess(idsStatusResult -> {
                                    List<Integer> idsStatus = new ArrayList<>();
                                    for (Object id : idsStatusResult) {
                                        idsStatus.add(((JsonObject) id).getInteger(Field.ID));
                                    }
                                    // Store all orders by key (uai + date) and value (id project) No duplicate
                                    HashMap<String, Integer> projetMap = new HashMap<>();
                                    historicCommand(request, sc, lastProject.right().getValue().getInteger(Field.ID),
                                            getEquipmentEvent.right().getValue(), projetMap, idsStatus, part);
                                })
                                .onFailure(error -> {
                                    badRequest(request);
                                    log.error(String.format("[CRRE%s::addOrders] An error occurred when getting all ids by status %s", this.getClass().getSimpleName(), error.getMessage()));
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

    /**
     * Use a lot of JVM memory with {@link Scanner}.
     *
     * @deprecated Replaced by {@link #getOrderLDE(Handler)}
     */
    @Deprecated
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

    /**
     * Get order LDE from LDE API. Due to a large amount of data, the response must be processed little by little.
     * This is why we provide a handler which will be executed for each order.
     *
     * @param orderLDEModelHandler handler executed for each order of the HTTP response
     * @return future completed when HTTP response has finish to be read
     */
    public Future<Void> getOrderLDE(Handler<OrderLDEModel> orderLDEModelHandler) {
        Promise<Void> promise = Promise.promise();

        HttpRequest<Buffer> request = this.webClient.getAbs(LDE_ORDER_URI);
        FileSystem fs = this.vertx.fileSystem();

        //Create tmpFile
        FileHelper.createTempFile(fs)
                //Get tmpFile
                .compose(path -> FileHelper.getFile(fs, path))
                //Write in tmpFile in Stream
                .compose(tmpFile -> HttpRequestHelper.getHttpRequestResponseAsStream(request, tmpFile, false))
                //Read tmpFile in Stream
                .onSuccess(tmpFile -> {
                    //Use atomic to skip header csv line
                    AtomicBoolean headerIsSkip = new AtomicBoolean(false);
                    RecordParser recordParser = RecordParser.newDelimited("\r", bufferedLine -> {
                        if (!headerIsSkip.get()) {
                            headerIsSkip.set(true);
                            return;
                        }
                        orderLDEModelHandler.handle(new OrderLDEModel(bufferedLine.toString()));
                    }).exceptionHandler(error -> {
                        String message = String.format("[CRRE@%s::getOrderLDE] Failed to execute handler: %s",
                                this.getClass().getSimpleName(), error.getMessage());
                        log.error(message);
                    });

                    tmpFile.handler(recordParser)
                            .exceptionHandler(error -> {
                                String message = String.format("[CRRE@%s::getOrderLDE] Failed to stream order LDE response: %s",
                                        this.getClass().getSimpleName(), error.getMessage());
                                log.error(message);
                                promise.fail(error.getMessage());
                            })
                            .endHandler(v -> {
                                tmpFile.close();
                                promise.complete();
                            });
                })
                .onFailure(error -> {
                    String message = String.format("[CRRE@%s::getOrderLDE] Failed to get LDE order: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(message);
                    promise.fail(error.getMessage());
                });
        return promise.future();
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

                order.put(Field.STATUS, "SENT");
                order.put(Field.NAME, values[0]);
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
                                ordersRegion.getJsonObject(i).put("id_structure", structure.getString(Field.ID));
                                checkEtab = false;
                            }
                            k++;
                        }
                        if (finalProject_size > i) {
                            orderRegionService.createProject("Commandes LDE", handlerJsonObject(createProjectFuture));
                        }
                    }
                    CompositeFuture.all(futures).onComplete(event2 -> {
                        if (event2.succeeded()) {
                            TransactionHelper.executeTransaction(orderRegionService.insertOldOrders(ordersRegion, true))
                                    .onSuccess(res -> historicCommand(request, sc, finalProject_id, equipments, projetMap, idsStatus, part + 1))
                                    .onFailure(error -> {
                                        badRequest(request);
                                        log.error(String.format("[CRRE@%s::historicCommand] Insert old orders failed %s", this.getClass().getSimpleName(), error.getMessage()));
                                    });
                        } else {
                            badRequest(request);
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

                filters(params, null, handlerJsonArray(equipmentFilterFuture));

                if (StringUtils.isEmpty(query)) {
                    equipmentFilterAndQFuture.complete(new JsonArray());
                } else {
                    searchfilter(params, query, Collections.singletonList(Field.EAN), handlerJsonArray(equipmentFilterAndQFuture));
                }

                CompositeFuture.all(equipmentFilterFuture, equipmentFilterAndQFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonArray equipmentsGrade = equipmentFilterFuture.result(); // Tout les équipements correspondant aux grades
                        JsonArray equipmentsGradeAndQ = equipmentFilterAndQFuture.result();
                        JsonArray allEquipments = new JsonArray();
                        allEquipments.addAll(equipmentsGrade);
                        allEquipments.addAll(equipmentsGradeAndQ);
                        List<String> equipementIdList = allEquipments.stream()
                                .filter(JsonObject.class::isInstance)
                                .map(JsonObject.class::cast)
                                .map(jsonObject -> jsonObject.getString(Field.EAN))
                                .collect(Collectors.toList());
                        orderRegionService.search(user, equipementIdList, query, startDate,
                                endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                    }
                });
            }
        } else {
            if (query.equals("")) {
                orderRegionService.search(user, new ArrayList<>(), query, startDate,
                        endDate, idStructure, filters, page, old, arrayResponseHandler(request));
            } else {
                plainTextSearchName(query, Collections.singletonList(Field.EAN), equipments -> {
                    List<String> equipementIdList = equipments.right().getValue().stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(jsonObject -> jsonObject.getString(Field.EAN))
                            .collect(Collectors.toList());
                    orderRegionService.search(user, equipementIdList, query, startDate,
                            endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                });
            }
        }
    }

    @Get("/ordersRegion/projects/search_filter")
    @ApiDoc("Get all projects search and filter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureRight.class)
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
                structureService.getStructuresByTypeAndFilter(request.getParam("type"),
                        request.params().getAll("id_structure"), event -> {
                            if (event.isRight()) {
                                JsonArray listeIdStructure = event.right().getValue();
                                filters.add(new JsonObject().put("id_structure", listeIdStructure));
                                getOrders(finalQuery, filters, user, page, request);
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
    @ApiDoc("Get all orders of each project")
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
        searchByIds(listIdsEquipment, null, equipments -> {
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
                                if (idEquipment.equals(equipment.getString(Field.ID))) {
                                    JsonObject priceDetails = equipment.getJsonObject("priceDetails");
                                    double price = 0.0;
                                    if (priceDetails.getDouble("priceTTC") != null && orderJson.getInteger("amount") != null) {
                                        price = priceDetails.getDouble("priceTTC",0.0) * orderJson.getInteger("amount", 0);
                                    }
                                    orderJson.put("price", price);
                                    orderJson.put(Field.NAME, equipment.getString("titre"));
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
        orderJson.put(Field.NAME, "Manuel introuvable dans le catalogue");
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
    @ApiDoc("Update region orders with status")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UpdateStatusRight.class)
    public void updateStatusOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                body -> UserUtils.getUserInfos(eb, request,
                        userInfos -> {
                            try {
                                OrderClientEquipmentType status = OrderClientEquipmentType.getValue(request.getParam(Field.STATUS));
                                if (status == null) {
                                    badRequest(request, "Unknown status");
                                    return;
                                }
                                List<Integer> orderRegionEquipmentIdList = body.getJsonArray(Field.IDS, new JsonArray()).stream()
                                        .filter(Integer.class::isInstance)
                                        .map(Integer.class::cast)
                                        .collect(Collectors.toList());
                                String justification = body.getString(Field.JUSTIFICATION);
                                JsonArray ordersList = body.getJsonArray(Field.ORDERS);
                                List<OrderRegionEquipmentModel> orderRegionModelList = IModelHelper.toList(ordersList, OrderRegionEquipmentModel.class);

                                List<TransactionElement> updatePurseLicenceTransactionList = orderRegionModelList.stream()
                                        .map(orderRegion -> this.getUpdateTransactionElement(status, orderRegion, orderRegion.getPrice()))
                                        .collect(Collectors.toList());

                                String errorMessage = String.format("[CRRE@%s::updateStatusOrders] Fail to update purse licence", this.getClass().getSimpleName());
                                TransactionHelper.executeTransaction(updatePurseLicenceTransactionList, errorMessage)
                                        .compose(res -> this.sequentialUpdateOrderStatus(userInfos, status, orderRegionEquipmentIdList, justification))
                                        .onSuccess(res -> {
                                            Renders.renderJson(request, new JsonArray(orderRegionEquipmentIdList));
                                            this.notificationService.sendNotificationValidator(orderRegionEquipmentIdList);
                                            this.notificationService.sendNotificationPrescriberRegion(orderRegionEquipmentIdList);
                                        })
                                        .onFailure(error -> unauthorized(request));
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                                renderError(request);
                            }
                        }));
    }

    private Future<Void> sequentialUpdateOrderStatus(UserInfos userInfos, OrderClientEquipmentType status, List<Integer> ids, String justification) {
        Promise<Void> promise = Promise.promise();
        List<List<Integer>> partitionOfIdList = ListUtils.partition(ids, 2500);

        Function<List<Integer>, Future<JsonObject>> functionUpdateOrders = orderIdList ->
                this.orderRegionService.updateOrdersStatus(orderIdList, status.toString(), justification);

        FutureHelper.compositeSequential(functionUpdateOrders, partitionOfIdList, true)
                .onSuccess(res -> {
                    promise.complete();
                    List<String> stringIdList = ids.stream().map(String::valueOf).collect(Collectors.toList());
                    Logging.insert(userInfos, Contexts.ORDERREGION.toString(), Actions.UPDATE.toString(),
                            stringIdList, new JsonObject().put(Field.STATUS, status));
                })
                .onFailure(error -> {
                    LOGGER.error(String.format("[CRRE@%s::sequentialUpdateOrderStatus] Fail to update order region status %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    private TransactionElement getUpdateTransactionElement(OrderClientEquipmentType status, OrderRegionEquipmentModel orderRegion, Double price) {
        if (OrderClientEquipmentType.REJECTED.toString().equalsIgnoreCase(orderRegion.getStatus())) {
            if (status.equals(OrderClientEquipmentType.VALID)) {
                 return getUpdatePurseTransaction(orderRegion.getIdStructure(), "-", price, orderRegion.getUseCredit());
            } else {
                return null;
            }
        } else if (status.equals(OrderClientEquipmentType.REJECTED)) {
            return getUpdatePurseTransaction(orderRegion.getIdStructure(), "+", price, orderRegion.getUseCredit());
        }

        return null;
    }

    private TransactionElement getUpdatePurseTransaction(String structureId, String operation, Double amount, CreditTypeEnum creditType) {
        switch (creditType) {
            case LICENCES: {
                return structureService.getTransactionUpdateAmountLicence(structureId, operation, amount.intValue(), false);
            }
            case CONSUMABLE_LICENCES: {
                return structureService.getTransactionUpdateAmountLicence(structureId, operation, amount.intValue(), true);
            }
            case CREDITS: {
                return purseService.getTransactionUpdatePurseAmount(amount, structureId, operation, false);
            }
            case CONSUMABLE_CREDITS: {
                return purseService.getTransactionUpdatePurseAmount(amount, structureId, operation, true);
            }
            default: {
                return null;
            }
        }
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
                    if (eventExport.body().getString(Field.STATUS).equals(Field.OK)) {
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
                if (idsOrders.size() > 0 && idsStructures.size() > 0 && idsEquipments.size() > 0) {
                    generateLogs(request, idsOrders, idsEquipments, idsStructures, user);
                } else {
                    noContent(request);
                }
            });
        });
    }

    private void generateLogs(HttpServerRequest request, List<Integer> idsOrders, List<String> idsEquipments,
                              List<String> idsStructures, UserInfos user) {
        JsonArray idStructures = new JsonArray();
        for (String structureId : idsStructures) {
            idStructures.add(structureId);
        }

        List<Future<JsonArray>> futures = new ArrayList<>();
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();
        futures.add(structureFuture);
        futures.add(equipmentsFuture);

        getOrdersRecursively(0, idsOrders, futures);

        FutureHelper.all(futures).onComplete(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray equipments = equipmentsFuture.result();
                List<Long> ordersClientId = new ArrayList<>();
                JsonArray orderRegions = new JsonArray();
                for (int i = 2; i < futures.size(); i++) {
                    orderRegions.addAll(futures.get(i).result());
                }
                List<Integer> orderRegionIdList = orderRegions.stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(orderRegion -> orderRegion.getInteger(Field.ID))
                        .collect(Collectors.toList());
                orderRegionService.beautifyOrders(structures, orderRegions, equipments, ordersClientId);
                JsonArray orderRegionClean = new JsonArray();
                for (int i = 0; i < orderRegions.size() ; i++){
                    JsonObject order = orderRegions.getJsonObject(i);
                    if (order.getString(Field.STATUS,"").equals(Field.REJECTED) && order.getDouble(Field.PRICE) != null &&
                            !order.getDouble(Field.PRICE, 0.0).equals(0.0)) {
                        orderRegionClean.add(order);
                    }
                }
                Future<List<TransactionElement>> transactionFuture = Future.succeededFuture();
                if (orderRegionClean.size() > 0) {
                    List<TransactionElement> updatePurseLicenceTransactionList = orderRegionClean.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(OrderRegionEquipmentModel::new)
                            .map(orderRegion -> this.getUpdateTransactionElement(OrderClientEquipmentType.VALID, orderRegion, orderRegion.getPrice() * orderRegion.getAmount()))
                            .collect(Collectors.toList());

                    String errorMessage = String.format("[CRRE@%s::generateLogs] Fail to generate logs", this.getClass().getSimpleName());
                    transactionFuture = TransactionHelper.executeTransaction(updatePurseLicenceTransactionList, errorMessage);
                }
                transactionFuture
                        .compose(res -> sendMailLibraryAndRemoveWaitingAdmin(request, user, orderRegions, ordersClientId))
                        .onSuccess(res -> {
                            Renders.ok(request);
                            this.notificationService.sendNotificationValidator(orderRegionIdList);
                            this.notificationService.sendNotificationPrescriberRegion(orderRegionIdList);
                        })
                        .onFailure(error -> unauthorized(request));
            }
        });

        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
        searchByIds(idsEquipments, null, handlerJsonArray(equipmentsFuture));
    }

    private void getOrdersRecursively(int e, List<Integer> listOrders, List<Future<JsonArray>> futures) {
        Future<JsonArray> orderRegionFuture = Future.future();
        futures.add(orderRegionFuture);
        List<Integer> subList = listOrders.subList(e * 5000, min((e +1) * 5000, listOrders.size()) );
        orderRegionService.getOrdersRegionById(subList, false, handlerJsonArray(orderRegionFuture));
        if ((e + 1) * 5000 < listOrders.size()) {
            getOrdersRecursively(e + 1, listOrders, futures);
        }
    }

    private Future<Void> sendMailLibraryAndRemoveWaitingAdmin(HttpServerRequest request, UserInfos user,
                                                      JsonArray orderRegion, List<Long> ordersClientId) {
        Promise<Void> promise = Promise.promise();

        List<JsonObject> allOrderRegionList = orderRegion.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .collect(Collectors.toList());

        List<MailAttachment> attachmentList = ListUtils.partition(allOrderRegionList, 10000).stream()
                .map(splitOrderRegionList -> orderRegionService.generateExport(new JsonArray(splitOrderRegionList)))
                .map(data -> new MailAttachment().setName("DD" + DateHelper.now(DateHelper.MAIL_FORMAT, DateHelper.PARIS_TIMEZONE))
                        .setContent(data.getString(Field.CSVFILE))
                        .setNbEtab(data.getInteger(Field.NB_ETAB)))
                .collect(Collectors.toList());

        Function<MailAttachment, Future<JsonObject>> functionSendMail = attachment -> this.sendMail(request, attachment);
        Function<MailAttachment, Future<MailAttachment>> functionInsertQuote = attachment -> this.insertQuote(user, attachment);

        SqlHelper.getNextVal("quote_id_seq")
                .compose(nextVal -> {
                    for (int i = 0; i < attachmentList.size(); i++) {
                        MailAttachment mailAttachment = attachmentList.get(i);
                        int id = nextVal + i;
                        mailAttachment.setName(mailAttachment.getName() + "-" + id + ".csv");
                    }
                    return FutureHelper.compositeSequential(functionSendMail, attachmentList, true);
                })
                .compose(res -> FutureHelper.compositeSequential(functionInsertQuote, attachmentList, false))
                .compose(res -> insertAndDeleteOrders(orderRegion, ordersClientId))
                .onSuccess(res -> promise.complete())
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::sendMailLibraryAndRemoveWaitingAdmin] An error has occurred when send mail to library and remove waiting admin : %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    private Future<JsonObject> sendMail(HttpServerRequest request, MailAttachment attachment) {
        Promise<JsonObject> promise = Promise.promise();

        String mail = this.mail.getAddress();
        String title = "Demande Libraire CRRE";
        String body = "Demande Libraire CRRE ; csv : " + attachment.getName();
        emailSender.sendMail(request, mail, title, body, attachment, FutureHelper.handlerEitherPromise(promise));

        return promise.future();
    }

    private Future<MailAttachment> insertQuote(UserInfos user, MailAttachment attachment) {
        Promise<MailAttachment> promise = Promise.promise();

        quoteService.insertQuote(user, attachment, returningTitle -> {
            if (returningTitle.isRight()) {
                promise.complete(attachment);
            } else {
                String message = String.format("[CRRE@%s::insertQuote] An error has occurred insertQuote %s : %s",
                        this.getClass().getSimpleName(), attachment.toJson().toString(), returningTitle.left().getValue());
                log.error(message);
                promise.fail(message);
            }
        });

        return promise.future();
    }

    private Future<Void> insertAndDeleteOrders(JsonArray orderRegion, List<Long> ordersClientId) {
        Promise<Void> promise = Promise.promise();
        List<TransactionElement> prepareRequestList = new ArrayList<>();
        prepareRequestList.addAll(orderRegionService.insertOldOrders(orderRegion, false));
        prepareRequestList.addAll(orderRegionService.insertOldClientOrders(orderRegion));
        prepareRequestList.addAll(orderRegionService.deletedOrders(ordersClientId, Field.ORDER_CLIENT_EQUIPMENT));

        TransactionHelper.executeTransaction(prepareRequestList)
                .onSuccess(event -> {
                    log.info("[CRRE@OrderRegionController.insertAndDeleteOrders] " +
                            "Orders Deleted and insert in old table was successfull");
                    promise.complete();
                })
                .onFailure(error -> {
                    String message = String.format("An error has occurred in CompositeFuture : %s", error.getMessage());
                    promise.fail(message);
                    log.error(String.format("[CRRE@O%s::insertAndDeleteOrders] %s", this.getClass().getSimpleName(), message));
                });

        return promise.future();
    }
}
