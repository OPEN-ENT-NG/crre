package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.CreditTypeEnum;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.*;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.*;
import fr.openent.crre.model.config.ConfigMailModel;
import fr.openent.crre.model.export.ExportOrderRegion;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.UpdateStatusRight;
import fr.openent.crre.security.ValidatorAndStructureHistoricRight;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.service.*;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.openent.crre.service.impl.EmailSendService;
import fr.openent.crre.service.impl.ExportWorker;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
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
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
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
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;
    private final ProjectService projectService;
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
        this.projectService = serviceFactory.getProjectService();
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
                RequestUtils.bodyToJson(request, pathPrefix + Field.CREATEADMINORDER, orders -> {
                    List<OrderRegionEquipmentModel> ordersRegionList = IModelHelper.toList(orders.getJsonArray(Field.ORDERS, new JsonArray()), OrderRegionEquipmentModel.class);
                    if (orders.isEmpty() || ordersRegionList.isEmpty()) {
                        noContent(request);
                        return;
                    }
                    List<String> idsEquipment = ordersRegionList.stream()
                            .map(OrderRegionEquipmentModel::getEquipmentKey)
                            .collect(Collectors.toList());
                    List<Integer> IdOrderClientEquipmentList = ordersRegionList.stream()
                            .map(OrderRegionEquipmentModel::getIdOrderClientEquipment)
                            .collect(Collectors.toList());
                    String commentProject = orders.getString(Field.COMMENT);

                    searchByIds(idsEquipment, null)
                            .compose(equipments -> {
                                setPriceToOrder(orders.getJsonArray(Field.ORDERS), equipments);
                                return createProject(user, commentProject);
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

    private Future<ProjectModel> createProject(UserInfos userInfos, String commentProject) {
        Promise<ProjectModel> promise = Promise.promise();

        projectService.getLastProject()
                .compose(lastProject -> {
                    String last = lastProject.getTitle();
                    String date = LocalDate.now().format(DateTimeFormatter.ofPattern(DateHelper.DAY_FORMAT_DASH));
                    String title = "Commande_" + date;
                    if (last != null && title.equals(last.substring(0, last.length() - 2))) {
                        title = title + "_" + (Integer.parseInt(last.substring(last.length() - 1)) + 1);
                    } else {
                        title += "_1";
                    }
                    return projectService.createProject(new ProjectModel().setTitle(title).setComment(commentProject));
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

    @Post("/ordersRegion/projects")
    @ApiDoc("Get all projects")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureHistoricRight.class)
    public void getAllProjects(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Field.PROJECTSEARCH, filterOrderRegions -> {
            UserUtils.getUserInfos(eb, request, user -> {
                FilterModel filters = new FilterModel(filterOrderRegions);
                FilterItemModel filtersItem = new FilterItemModel(filterOrderRegions);
                Future<JsonArray> structureFuture;
                if (filters.getStructureTypes().size() > 0 || filters.getIdsStructure().size() > 0) {
                    structureFuture = structureService.getStructuresFilter(filters.getStructureTypes(), filters.getIdsStructure());
                } else {
                    structureFuture = Future.succeededFuture(new JsonArray());
                }

                structureFuture.compose(structures -> {
                            filters.setIdsStructure(JsonHelper.jsonArrayToList(structures, JsonObject.class)
                                    .stream()
                                    .map(structure -> structure.getString(Field.IDSTRUCTURE))
                                    .collect(Collectors.toList()));
                            return getProjects(filters, filtersItem);
                        })
                        .onSuccess(res -> renderJson(request, res))
                        .onFailure(fail -> renderError(request));
            });
        });
    }


    @Get("/add/orders/lde")
    @ApiDoc("Insert old orders from LDE")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void addOrders(HttpServerRequest request) throws IOException {
        Scanner sc = getOrderLDE();
        projectService.getLastProject()
                .onSuccess(lastProject -> {
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
                                        historicCommand(request, sc, lastProject.getId(),
                                                getEquipmentEvent.right().getValue(), projetMap, idsStatus, part);
                                    })
                                    .onFailure(error -> {
                                        badRequest(request);
                                        log.error(String.format("[CRRE%s::addOrders] An error occurred when getting all ids by status %s", this.getClass().getSimpleName(), error.getMessage()));
                                    });
                        } else {
                            badRequest(request);
                            log.error(String.format("[CRRE@%s::addOrders] Failed to get items %s",
                                    this.getClass().getSimpleName(), getEquipmentEvent.left().getValue()));
                        }
                    });
                })
                .onFailure(error -> {
                    badRequest(request);
                    log.error(String.format("[CRRE@%s::addOrders] Failed to add orders %s",
                            this.getClass().getSimpleName(), error.getMessage()));
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
                        if (finalProject_size > i) {
                            futures.add(projectService.createProject(new ProjectModel().setTitle(I18n.getInstance().translate("crre.lde.orders", getHost(request), I18n.acceptLanguage(request)))));
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


    private Future<JsonArray> getProjects(FilterModel filters, FilterItemModel filtersItem) {
        Promise<JsonArray> promise = Promise.promise();
        FilterItemModel filtersItemQuery = new FilterItemModel().setSearchingText(filtersItem.getSearchingText());
        FilterItemModel filtersItemFilter = filtersItem.clone().setSearchingText(null);

        Future<JsonArray> filterFuture = filtersItem.hasFilters() ?
                searchfilter(filtersItemFilter, Collections.singletonList(Field.EAN)) : Future.succeededFuture(new JsonArray());

        Future<JsonArray> searchFuture = filtersItem.getSearchingText() != null ?
                searchfilter(filtersItemQuery, Collections.singletonList(Field.EAN)) : Future.succeededFuture(new JsonArray());

        CompositeFuture.all(filterFuture, searchFuture)
                .compose(items -> {
                    JsonArray itemsSearch = searchFuture.result();
                    JsonArray itemsFilter = filterFuture.result();
                    List<String> itemSearchedIdsList = itemsSearch.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(jsonObject -> jsonObject.getString(Field.EAN))
                            .collect(Collectors.toList());
                    List<String> itemFilteredIdsList = itemsFilter.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(jsonObject -> jsonObject.getString(Field.EAN))
                            .collect(Collectors.toList());
                    return orderRegionService.search(filters, filtersItem, itemSearchedIdsList, itemFilteredIdsList);
                })
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }

    private Future<JsonArray> getOrders(FilterModel filters, FilterItemModel filtersItem, List<Integer> idsProjects) {
        Promise<JsonArray> promise = Promise.promise();
        FilterItemModel filtersItemQuery = new FilterItemModel().setSearchingText(filtersItem.getSearchingText());
        FilterItemModel filtersItemFilter = filtersItem.clone().setSearchingText(null);

        Future<JsonArray> filterFuture = filtersItem.hasFilters() ?
                searchfilter(filtersItemFilter, Collections.singletonList(Field.EAN)) : Future.succeededFuture(new JsonArray());

        Future<JsonArray> searchFuture = filtersItem.getSearchingText() != null ?
                searchfilter(filtersItemQuery, Collections.singletonList(Field.EAN)) : Future.succeededFuture(new JsonArray());

        CompositeFuture.all(filterFuture, searchFuture)
                .compose(items -> {
                    JsonArray itemsSearch = searchFuture.result();
                    JsonArray itemsFilter = filterFuture.result();
                    List<String> itemSearchedIdsList = itemsSearch.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(jsonObject -> jsonObject.getString(Field.EAN))
                            .collect(Collectors.toList());
                    List<String> itemFilteredIdsList = itemsFilter.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(jsonObject -> jsonObject.getString(Field.EAN))
                            .collect(Collectors.toList());
                    return orderRegionService.getAllOrderRegionByProject(idsProjects, filters, filtersItem, itemSearchedIdsList, itemFilteredIdsList);
                })
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }

    @Post("/ordersRegion/orders")
    @ApiDoc("Get all orders of each project")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getOrdersByProjects(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Field.ORDERSBYPROJECT, orderRegions -> {
            UserUtils.getUserInfos(eb, request, user -> {
                List<Integer> idsProjects = orderRegions.getJsonArray(Field.IDS_PROJECT)
                        .stream()
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .collect(Collectors.toList());
                FilterModel filters = new FilterModel(orderRegions);
                FilterItemModel filtersItem = new FilterItemModel(orderRegions);
                Future<JsonArray> ordersFuture = getOrders(filters, filtersItem, idsProjects);
                ordersFuture.compose(orderResults -> {
                            List<String> listIdsEquipment = orderResults.stream()
                                    .filter(JsonObject.class::isInstance)
                                    .map(JsonObject.class::cast)
                                    .filter(order -> !order.getBoolean(Field.OLD))
                                    .map(order -> order.getString(Field.EQUIPMENT_KEY, null))
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.toList());
                            return searchByIds(listIdsEquipment, null);
                        })
                        .onSuccess(itemResults -> {
                            for (int i = 0; i < itemResults.size(); i++) {
                                JsonObject equipment = itemResults.getJsonObject(i);
                                JsonObject priceDetails = getPriceTtc(equipment);
                                equipment.put("priceDetails", priceDetails);
                            }
                            ordersFuture.result().stream()
                                    .filter(JsonObject.class::isInstance)
                                    .map(JsonObject.class::cast)
                                    .forEach(order -> {
                                                // Verifie si l'order est dans la table old ou non
                                                if (order.getBoolean(Field.OLD)) {
                                                    getSearchByIdsOld(order);
                                                } else {
                                                    getSearchByIds(order, itemResults);
                                                }
                                            }
                                    );

                            Map<Integer, List<JsonObject>> ordersGrouped = ordersFuture.result()
                                    .stream()
                                    .map(JsonObject.class::cast)
                                    .collect(Collectors.groupingBy(order -> order.getInteger(Field.ID_PROJECT)));

                            JsonArray projectsArray = new JsonArray();
                            for (Map.Entry<Integer, List<JsonObject>> entry : ordersGrouped.entrySet()) {
                                JsonArray ordersArray = new JsonArray();
                                for (JsonObject order : entry.getValue()) {
                                    ordersArray.add(order);
                                }
                                projectsArray.add(ordersArray);
                            }

                            renderJson(request, projectsArray);
                        })
                        .onFailure(fail -> {
                            log.error(fail.getMessage());
                            badRequest(request);
                        });
            });
        });
    }

    private void getSearchByIds(JsonObject order, JsonArray items) {
        String creation_date = "";
        if (order.getString("creation_date") != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date", ""), formatter);
            creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
        }
        order.put("creation_date", creation_date);
        String idEquipment = order.getString("equipment_key", "");
        if (items.size() > 0 && idEquipment != null) {
            for (int i = 0; i < items.size(); i++) {
                JsonObject equipment = items.getJsonObject(i);
                if (idEquipment.equals(equipment.getString(Field.ID))) {
                    JsonObject priceDetails = equipment.getJsonObject("priceDetails");
                    double price = 0.0;
                    if (priceDetails.getDouble("priceTTC") != null && order.getInteger("amount") != null) {
                        price = priceDetails.getDouble("priceTTC", 0.0) * order.getInteger("amount", 0);
                    }
                    order.put("price", price);
                    order.put(Field.NAME, equipment.getString("titre"));
                    order.put("image", equipment.getString("urlcouverture", "/crre/public/img/pages-default.png"));
                    order.put("ean", equipment.getString("ean", idEquipment));
                    order.put("_index", equipment.getString("type", "NaN"));
                    order.put("editeur", equipment.getString("editeur", "NaN"));
                    order.put("distributeur", equipment.getString("distributeur", "NaN"));
                    break;
                } else if (items.size() - 1 == i) {
                    equipmentNotFound(order, idEquipment);
                }
            }
        } else {
            equipmentNotFound(order, idEquipment);
        }
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

    private void getSearchByIdsOld(JsonObject order) {
        double price = 0.0;
        if (order.getString(Field.PRICE) != null && order.getInteger(Field.AMOUNT) != null) {
            price = Double.parseDouble(order.getString(Field.PRICE)) * order.getInteger(Field.AMOUNT, 0);
        }
        order.put(Field.PRICE, price);
        String creation_date = "";
        if (order.getString(Field.CREATION_DATE) != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString(Field.CREATION_DATE, ""), formatter);
            creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
        }
        order.put(Field.CREATION_DATE, creation_date);
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
                                OrderStatus status = OrderStatus.getValue(request.getParam(Field.STATUS));
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

    private Future<Void> sequentialUpdateOrderStatus(UserInfos userInfos, OrderStatus status, List<Integer> ids, String justification) {
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

    private TransactionElement getUpdateTransactionElement(OrderStatus status, OrderRegionEquipmentModel orderRegion, Double price) {
        if (OrderStatus.REJECTED.toString().equalsIgnoreCase(orderRegion.getStatus())) {
            if (status.equals(OrderStatus.VALID)) {
                return getUpdatePurseTransaction(orderRegion.getIdStructure(), "-", price, orderRegion.getUseCredit());
            } else {
                return null;
            }
        } else if (status.equals(OrderStatus.REJECTED)) {
            return getUpdatePurseTransaction(orderRegion.getIdStructure(), "+", price, orderRegion.getUseCredit());
        }

        return null;
    }

    private TransactionElement getUpdateTransactionElement(OrderStatus status, OrderRegionBeautifyModel orderRegion) {
        if (OrderStatus.REJECTED.toString().equalsIgnoreCase(orderRegion.getOrderRegion().getStatus())) {
            if (status.equals(OrderStatus.VALID)) {
                return getUpdatePurseTransaction(orderRegion.getOrderRegion().getIdStructure(), "-", orderRegion.getTotalPriceTTC(), CreditTypeEnum.getValue(orderRegion.getCampaign().getUseCredit(), CreditTypeEnum.NONE));
            } else {
                return null;
            }
        } else if (status.equals(OrderStatus.REJECTED)) {
            return getUpdatePurseTransaction(orderRegion.getOrderRegion().getIdStructure(), "+", orderRegion.getTotalPriceTTC(), CreditTypeEnum.getValue(orderRegion.getCampaign().getUseCredit(), CreditTypeEnum.NONE));
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
        RequestUtils.bodyToJson(request, params ->
                UserUtils.getUserInfos(eb, request, user -> {
                    params.put(Field.IDUSER, user.getUserId());
                    ExportOrderRegion exportOrderRegion = new ExportOrderRegion(params);
                    if (exportOrderRegion.getIdsOrders().size() > 1000) {
                        launchWorker(exportOrderRegion, null);
                        ok(request);
                    } else {
                        launchWorker(exportOrderRegion, request);
                    }
                })
        );
    }

    private void launchWorker(ExportOrderRegion exportOrderRegion, HttpServerRequest request) {
        eb.request(ExportWorker.class.getSimpleName(), exportOrderRegion.toEventBusJson(), new DeliveryOptions().setSendTimeout(1000 * 1000L),
                handlerToAsyncHandler(eventExport -> {
                            if (eventExport.body().getString(Field.STATUS).equals(Field.OK)) {
                                if (request != null) {
                                    if (eventExport.body().getJsonObject(Field.DATA, new JsonObject()).getString(Field.CSVFILE, null) == null) {
                                        Renders.badRequest(request);
                                        return;
                                    }
                                    //Export CSV
                                    request.response()
                                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                            .end(eventExport.body().getJsonObject(Field.DATA).getString(Field.CSVFILE));
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

        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();

        List<OrderRegionBeautifyModel> orderRegionBeautifyList = new ArrayList<>();
        List<Integer> orderRegionIdList = new ArrayList<>();

        CompositeFuture.all(structureFuture, equipmentsFuture)
                .compose(res -> this.getOrder(idsOrders))
                .compose(orderRegionComplexList -> {
                    orderRegionComplexList = orderRegionComplexList.stream()
                            .filter(orderRegionComplex ->
                                    !OrderStatus.SENT.toString().equals(orderRegionComplex.getOrderRegion().getStatus()) &&
                                            !OrderStatus.DONE.toString().equals(orderRegionComplex.getOrderRegion().getStatus()))
                            .collect(Collectors.toList());
                    JsonArray structures = structureFuture.result();
                    JsonArray equipments = equipmentsFuture.result();

                    orderRegionIdList.addAll(orderRegionComplexList.stream().map(orderRegionComplex -> orderRegionComplex.getOrderRegion().getId()).collect(Collectors.toList()));
                    orderRegionBeautifyList.addAll(orderRegionService.orderResultToBeautifyModel(structures, orderRegionComplexList, equipments));
                    List<OrderRegionBeautifyModel> orderRegionClean = orderRegionBeautifyList.stream()
                            .filter(orderRegionBeautifyModel ->
                                    Field.REJECTED.equals(orderRegionBeautifyModel.getOrderRegion().getStatus()) &&
                                            !Double.valueOf(0.0).equals(orderRegionBeautifyModel.getPrice()))
                            .collect(Collectors.toList());

                    Future<List<TransactionElement>> transactionFuture = Future.succeededFuture();
                    if (orderRegionClean.size() > 0) {
                        List<TransactionElement> updatePurseLicenceTransactionList = orderRegionClean.stream()
                                .map(orderRegionBeautifyModel -> this.getUpdateTransactionElement(OrderStatus.VALID, orderRegionBeautifyModel))
                                .collect(Collectors.toList());

                        String errorMessage = String.format("[CRRE@%s::generateLogs] Fail to generate logs", this.getClass().getSimpleName());
                        transactionFuture = TransactionHelper.executeTransaction(updatePurseLicenceTransactionList, errorMessage);
                    }
                    return transactionFuture;
                })
                .compose(res -> sendMailLibraryAndRemoveWaitingAdmin(request, user, orderRegionBeautifyList))
                .onSuccess(res -> {
                    Renders.ok(request);
                    this.notificationService.sendNotificationValidator(orderRegionIdList);
                    this.notificationService.sendNotificationPrescriberRegion(orderRegionIdList);
                })
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::generateLogs] Fail to generate logs %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    unauthorized(request);
                });

        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
        searchByIds(idsEquipments, null, handlerJsonArray(equipmentsFuture));
    }

    private Future<List<OrderRegionComplex>> getOrder(List<Integer> listOrders) {
        Promise<List<OrderRegionComplex>> promise = Promise.promise();
        List<List<Integer>> partition = ListUtils.partition(listOrders, 5000);

        Function<List<Integer>, Future<List<OrderRegionComplex>>> function = idOrderList -> this.orderRegionService.getOrdersRegionById(idOrderList, false);

        FutureHelper.compositeSequential(function, partition, true)
                .onSuccess(res ->
                        promise.complete(res.stream().flatMap(listFuture -> listFuture.result().stream()).collect(Collectors.toList())))
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::getOrder] Fail to get all order %s", this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    private Future<Void> sendMailLibraryAndRemoveWaitingAdmin(HttpServerRequest request, UserInfos user, List<OrderRegionBeautifyModel> orderRegion) {
        Promise<Void> promise = Promise.promise();

        List<MailAttachment> attachmentList = ListUtils.partition(orderRegion, 10000).stream()
                .map(orderRegionService::generateExport)
                .map(data -> new MailAttachment().setName("DD" + DateHelper.now(DateHelper.MAIL_FORMAT, DateHelper.PARIS_TIMEZONE))
                        .setContent(data.getString(Field.CSVFILE))
                        .setNbEtab(data.getInteger(Field.NB_ETAB)))
                .collect(Collectors.toList());

        Function<MailAttachment, Future<JsonObject>> functionSendMail = attachment -> this.sendMail(request, attachment);
        Function<MailAttachment, Future<MailAttachment>> functionInsertQuote = attachment -> this.insertQuote(user, attachment);

        List<Long> ordersClientId = orderRegion.stream()
                .map(orderRegionBeautify -> orderRegionBeautify.getOrderRegion().getIdOrderClientEquipment().longValue())
                .collect(Collectors.toList());

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

    private Future<Void> insertAndDeleteOrders(List<OrderRegionBeautifyModel> orderRegion, List<Long> ordersClientId) {
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
