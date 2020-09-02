package fr.openent.crre.controllers;

import com.opencsv.CSVReader;
import fr.openent.crre.Crre;
import fr.openent.crre.helpers.ImportCSVHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.EquipmentService;
import fr.openent.crre.service.impl.DefaultEquipmentService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static jdk.nashorn.internal.objects.NativeArray.push;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;
    private final ImportCSVHelper importCSVHelper;
    private ArrayList<String> filter_array;
    private HashMap<String, ArrayList<String>> query_filter;

    public EquipmentController(Vertx vertx) {
        super();
        this.equipmentService = new DefaultEquipmentService(Crre.crreSchema, "equipment");
        this.importCSVHelper = new ImportCSVHelper(vertx, this.eb);
        this.filter_array = new ArrayList<String> ();
        this.query_filter = new HashMap<String, ArrayList<String>>();
    }

    @Get("/equipments")
    @ApiDoc("List all equipments in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @Override
    public void list(HttpServerRequest request) {
        Integer page = request.params().contains("page") ? Integer.parseInt(request.getParam("page")) : 0;
        String order = request.params().contains("order") ? request.getParam("order") : "name";
        Boolean reverse = request.params().contains("reverse") && Boolean.parseBoolean(request.getParam("reverse"));
        List<String> queries = request.params().getAll("q");
        equipmentService.listEquipments(page, order, reverse, queries, arrayResponseHandler(request));
    }

    @Get("/equipments/pages/count")
    @ApiDoc("Get page number in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getPageNumber(HttpServerRequest request) {
        MultiMap params = request.params();
        List<String> queries = params.getAll("q");
        if (params.contains("idCampaign") && params.contains("idStructure")) {
            try {
                equipmentService.getNumberPagesCatalog(queries, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                badRequest(request);
                log.error("An error occured while casting campaign identifier", e);
            }
        } else {
            equipmentService.getNumberPages(queries, defaultResponseHandler(request));
        }
    }

    @Get("/equipment/:id")
    @ApiDoc("Get an equipment")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void equipment(final HttpServerRequest request) {
        try {
            Integer idEquipment = request.params().contains("id")
                    ? Integer.parseInt(request.params().get("id"))
                    : null;
            equipmentService.equipment(idEquipment, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }
    @Get("/equipments/catalog")
    @ApiDoc("List equipments of campaign in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listEquipmentFromCampaign(final HttpServerRequest request) {
        try {
            equipmentService.searchAll(arrayResponseHandler(request));
            equipmentService.syncES();

        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Get("/equipments/catalog/syncES")
    @ApiDoc("Sync equipments from database to ES")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void syncES() {
        try {
            equipmentService.syncES();

        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Get("/equipments/subjects")
    @ApiDoc("List subjects")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listSubjects(final HttpServerRequest request) {
        try {
            equipmentService.listSubjects(arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Get("/equipments/grades")
    @ApiDoc("List grades")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listGrades(final HttpServerRequest request) {
        try {
            equipmentService.listGrades(arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Get("/equipments/catalog/search/:word")
    @ApiDoc("Search an equipment by keyword")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void SearchEquipment(final HttpServerRequest request) {
        try {
            String word = request.getParam("word");
            equipmentService.searchWord(word, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred searching article", e);
        }
    }

    @Get("/equipments/catalog/filter/:filter/:word")
    @ApiDoc("Search an equipment by keyword")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void FilterEquipment(final HttpServerRequest request) throws UnsupportedEncodingException {
        try {
            String word = URLDecoder.decode(request.getParam("word"), "UTF-8");
            String filter = request.getParam("filter");

/*            if(this.query_filter.containsKey(filter)) {
                this.query_filter.remove(filter);
            }*/


                if(this.query_filter.containsKey(filter)) {
                    if(this.query_filter.get(filter).contains(word)) {
                        this.query_filter.get(filter).remove(word);
                    } else {
                        this.query_filter.get(filter).add(word);
                    }
                } else {
                    this.query_filter.put(filter, new ArrayList<String>(Arrays.asList(word)));
                }

            equipmentService.filterWord(this.query_filter, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred searching article", e);
        }
    }

    @Get("/equipments/admin/:idCampaign")
    @ApiDoc("List equipments of campaign in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listAllEquipmentFromCampaign(final HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
            String idStructure = request.params().contains("idStructure")
                    ? request.params().get("idStructure")
                    : null;
            equipmentService.listAllEquipments(idCampaign, idStructure, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Post("/equipment")
    @ApiDoc("Create an equipment")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "equipment",
                equipment -> equipmentService.create(equipment, Logging.defaultResponseHandler(eb,
                request,
                Contexts.EQUIPMENT.toString(),
                Actions.CREATE.toString(),
                null,
                equipment)));
    }

    @Put("/equipment/:id")
    @ApiDoc("Update an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "equipment",
                equipment -> {
            try {
                final Integer id = Integer.parseInt(request.params().get("id"));
                equipmentService.updateEquipment(id, equipment,
                        eventUpdateEquipment -> {
                            if(eventUpdateEquipment.isRight()){
                                final Integer optionsCreate =  equipment
                                        .getJsonArray("optionsCreate").size();
                                equipmentService.prepareUpdateOptions( optionsCreate , id,
                                        resultObject -> {
                                            if(resultObject.isRight()) {
                                                equipmentService.updateOptions( id, equipment,
                                                        resultObject.right().getValue(),
                                                        Logging.defaultResponseHandler(eb,
                                                                request,
                                                                Contexts.EQUIPMENT.toString(),
                                                                Actions.UPDATE.toString(),
                                                                request.params().get("id"),
                                                                equipment) );
                                            }else {
                                              log.error("An error occurred when preparing options update");
                                            }
                                        });
                            }
                        });
            } catch (ClassCastException e) {
                log.error("An error occurred when casting equipment id", e);
            }
        });
    }

    @Put("/equipments/:status")
    @ApiDoc("Update equipments to provided status")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void setStatus(final HttpServerRequest request) {
        try {
            String status = request.params().get("status");
            List<String> ids = request.params().getAll("id");
            List<Integer> equipmentIds = new ArrayList<>();
            for (String id : ids) {
                equipmentIds.add(Integer.parseInt(id));
            }
            if (!ids.isEmpty()) {
                equipmentService.setStatus(equipmentIds, status, defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        } catch (NumberFormatException e) {
            log.error("An error occurred when parsing equipments ids", e);
        }
    }

    @Post("/equipments/contract/:id/import")
    @ApiDoc("Import equipments")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void importEquipment(final HttpServerRequest request) {
        final String importId = UUID.randomUUID().toString();
        final String path = config.getString("import-folder", "/tmp") + File.separator + importId;
        importCSVHelper.getParsedCSV(request, path, event -> {
            if (event.isRight()) {
                Buffer content = event.right().getValue();
                parseEquipmentCsv(request, path, content);
            } else {
                renderError(request);
            }
        });
    }

    private Boolean isATrueValue(String value) {
        return "oui".equals(value.trim()) || "o".equals(value.trim()) || "yes".equals(value.trim()) || "y".equals(value.trim());
    }

    /**
     * Parse CSV file
     *
     * @param request Http request
     * @param path    Directory path
     */
    private void parseEquipmentCsv(final HttpServerRequest request, final String path, Buffer content) {
        try {
            CSVReader csv = new CSVReader(new InputStreamReader(
                    new ByteArrayInputStream(content.getBytes())),
                    ';', '"', 1);
            String[] values;
            JsonArray equipments = new JsonArray();
            JsonArray references = new JsonArray();
            while ((values = csv.readNext()) != null) {
                JsonObject object = new JsonObject();
                if (!"".equals(values[0].trim())) {
                    references.add(values[0].trim());
                }
                object.put("reference", values[0].trim());
                object.put("name", values[1].trim());
                object.put("price", values[2].trim());
                object.put("id_tax", values[3].trim());
                object.put("warranty", Integer.parseInt(values[4].trim()));
                object.put("catalog_enabled", true);
                object.put("id_contract", Integer.parseInt(request.getParam("id").trim()));
                object.put("type", values[5].trim());
                object.put("status", isATrueValue(values[6]) ? "AVAILABLE" : "OUT_OF_STOCK");
                object.put("price_editable", isATrueValue(values[7]));
                object.put("name_tag", values[8]);

                equipments.add(object);
            }
            if (equipments.size() > 0) {
                equipmentService.importEquipments(equipments, references, event -> {
                    if (event.isRight()) {
                        created(request);
                    } else {
                        returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                    }
                });
            } else {
                returnErrorMessage(request, new Throwable("missing.equipment"), path);
            }
        } catch (IOException e) {
            log.error("[Crre@CSVImport]: csv exception", e);
            returnErrorMessage(request, e.getCause(), path);
            deleteImportPath(vertx, path);
        }
    }

    /**
     * End http request and returns message error. It delete the directory.
     *
     * @param request Http request
     * @param cause   Throwable message
     * @param path    Directory path to delete
     */
    private void returnErrorMessage(HttpServerRequest request, Throwable cause, String path) {
        renderErrorMessage(request, cause);
        deleteImportPath(vertx, path);
    }

    /**
     * Render a message error based on cause message
     *
     * @param request Http request
     * @param cause   Cause error
     */
    private static void renderErrorMessage(HttpServerRequest request, Throwable cause) {
        renderError(request, new JsonObject().put("message", cause.getMessage()));
    }

    @Delete("/equipment")
    @ApiDoc("Delete an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                equipmentService.delete(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.EQUIPMENT.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting equipment(s) id(s)", e);
            badRequest(request);
        }
    }



    @Get("/equipments/search")
    @ApiDoc("Search equipment through reference and name")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void search(HttpServerRequest request) {
        if (request.params().contains("q") && request.params().get("q").trim() != "" && request.params().contains("field")) {
            String query = request.getParam("q");
            List<String> params = request.params().getAll("field");
                equipmentService.search(query,params, arrayResponseHandler(request));
        } else {
            badRequest(request);
        }
    }
}
