package fr.openent.crre.controllers;

import com.opencsv.CSVReader;
import fr.openent.crre.Crre;
import fr.openent.crre.helpers.ImportCSVHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.DefaultPurseService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class PurseController extends ControllerHelper {


    private final ImportCSVHelper importCSVHelper;
    private final StructureService structureService;
    private final PurseService purseService;

    public PurseController(Vertx vertx) {
        super();
        this.importCSVHelper = new ImportCSVHelper(vertx, this.eb);
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
        this.purseService = new DefaultPurseService();
    }

    @Post("/purses/import")
    @ApiDoc("Import purse")
    @SecuredAction(Crre.ADMINISTRATOR_RIGHT)
    @ResourceFilter(AdministratorRight.class)
    public void purse(final HttpServerRequest request) {
        final String importId = UUID.randomUUID().toString();
        final String path = config.getString("import-folder", "/tmp") + File.separator + importId;
        importCSVHelper.getParsedCSV(request, path, event -> {
            if (event.isRight()) {
                Buffer content = event.right().getValue();
                parseCsv(request, path, content);
            } else {
                renderError(request);
            }
        });
    }

    /**
     * Parse CSV file
     *
     * @param request Http request
     * @param path    Directory path
     */
    private void parseCsv(final HttpServerRequest request, final String path, Buffer content) {
        try {
            CSVReader csv = new CSVReader(new InputStreamReader(
                    new ByteArrayInputStream(content.getBytes())),
                    ';', '"', 1);
            String[] values;
            JsonArray uais = new fr.wseduc.webutils.collections.JsonArray();
            JsonObject amounts = new JsonObject();
            JsonObject licences = new JsonObject();
            JsonObject consumable_licences = new JsonObject();
            JsonObject seconds = new JsonObject();
            JsonObject premieres = new JsonObject();
            JsonObject terminales = new JsonObject();
            while ((values = csv.readNext()) != null) {
                amounts.put(values[0], values[1]);
                licences.put(values[0], values[2]);
                consumable_licences.put(values[0], values[3]);
                seconds.put(values[0], values[4]);
                premieres.put(values[0], values[5]);
                terminales.put(values[0], values[6]);
                uais.add(values[0]);
            }
            if (uais.size() > 0) {
                matchUAIID(request, path, uais, amounts, licences, consumable_licences, seconds, premieres, terminales, content.toString());
            } else {
                returnErrorMessage(request, new Throwable("missing.uai"), path);
            }
        } catch (IOException e) {
            log.error("[Crre@CSVImport]: csv exception", e);
            returnErrorMessage(request, e.getCause(), path);
        }
    }

    /**
     * Match structure UAI with its Neo4j id.
     *
     * @param request Http request
     * @param path    Directory path
     * @param uais    UAIs list
     * @param amount  Object containing UAI as key and purse amount as value
     */
    private void matchUAIID(final HttpServerRequest request, final String path, JsonArray uais,
                            final JsonObject amount, final JsonObject licence, final JsonObject consumable_licence,
                            final JsonObject seconds, final JsonObject premieres, final JsonObject terminales, final String contentFile) {
        structureService.getConsumableFormation(formations -> {
            if(formations.isRight()) {
                JsonArray res_consumable_formations = formations.right().getValue();
                List<String> consumable_formations = res_consumable_formations
                        .stream()
                        .map((json) -> ((JsonObject)json).getString("label"))
                        .collect(Collectors.toList());
                structureService.getStructureByUAI(uais, consumable_formations, event -> {
                    if (event.isRight()) {
                        final JsonArray structures = event.right().getValue();
                        JsonObject statementsValues = new JsonObject();
                        JsonObject structure;
                        boolean invalidDatas = false;
                        for (int i = 0; i < structures.size(); i++) {
                            structure = structures.getJsonObject(i);
                            boolean professionnal_structure = structure.getString("type").equals("LYCEE PROFESSIONNEL");
                            try {
                                int licences = Integer.parseInt(licence.getString(structure.getString("uai")));
                                int consumable_licences =  Integer.parseInt(consumable_licence.getString(structure.getString("uai")));
                                int seconde = Integer.parseInt(seconds.getString(structure.getString("uai")));
                                int premiere = Integer.parseInt(premieres.getString(structure.getString("uai")));
                                int terminale = Integer.parseInt(terminales.getString(structure.getString("uai")));

                                int minimumLicences;
                                if(professionnal_structure){
                                    minimumLicences = seconde * 3 + premiere * 3 + terminale * 3;
                                } else {
                                    minimumLicences = seconde * 9 + premiere * 8 + terminale * 7;
                                }
                                if(licences < minimumLicences){
                                    licence.put(structure.getString("uai"), Integer.toString(minimumLicences));
                                }

                                int minimumConsumableLicences;
                                minimumConsumableLicences = structure.getInteger("nbr_students_consumables") * 2;
                                if(consumable_licences < minimumConsumableLicences){
                                    consumable_licence.put(structure.getString("uai"), Integer.toString(minimumConsumableLicences));
                                }

                            } catch (NumberFormatException e){
                                invalidDatas = true;
                            }
                            statementsValues.put(structure.getString("id"), new JsonObject()
                                    .put("amount",amount.getString(structure.getString("uai")))
                                    .put("licence",licence.getString(structure.getString("uai")))
                                    .put("consumable_licence",consumable_licence.getString(structure.getString("uai")))
                                    .put("second",seconds.getString(structure.getString("uai")))
                                    .put("premiere",premieres.getString(structure.getString("uai")))
                                    .put("terminale",terminales.getString(structure.getString("uai")))
                                    .put("pro",professionnal_structure)
                            );
                        }
                        launchImport(request, path, statementsValues, contentFile, invalidDatas);

                    } else {
                        returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                    }
                });
            }else{
                returnErrorMessage(request, new Throwable(formations.left().getValue()), path);
            }
        });
    }

    /**
     * Launch database import
     *
     * @param request          Http request
     * @param path             Directory path
     * @param statementsValues Object containing statement values
     */
    private void launchImport(final HttpServerRequest request, final String path,
                              JsonObject statementsValues, final String contentFile, boolean invalidDatas) {
        try {
            purseService.launchImport(statementsValues, invalidDatas, event -> {
                        if (event.isRight()) {
                            Renders.renderJson(request, event.right().getValue());
                            JsonObject contentObject = new JsonObject().put("content", contentFile);
                            Logging.insert(eb, request, Contexts.PURSE.toString(),
                                    Actions.IMPORT.toString(), "", contentObject);
                            deleteImportPath(vertx, path);
                        } else {
                            returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                        }
                    });
        } catch (NumberFormatException e) {
            log.error("[Crre@launchImport] : An error occurred when parsing campaign id", e);
            returnErrorMessage(request, e.getCause(), path);
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
    private void renderErrorMessage(HttpServerRequest request, Throwable cause) {
        renderError(request, new JsonObject().put("message", cause.getMessage()));
    }

    @Get("/purses/export")
    @ApiDoc("Export purses")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void export(final HttpServerRequest request) {
        try {
            List<String> params = request.params().getAll("id");
            purseService.getPursesStudentsAndLicences(params, event -> {
                if (event.isRight()) {
                    JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray purses = event.right().getValue();
                    for (int i = 0; i < purses.size(); i++) {
                        ids.add(purses.getJsonObject(i).getString("id_structure"));
                    }
                    retrieveStructures(ids, purses, request);
                } else {
                    badRequest(request);
                }
            });
        } catch (NumberFormatException e) {
            log.error("[Crre@CSVExport] : An error occurred when casting purses", e);
            badRequest(request);
        }
    }

    @Put("/purse/:idStructure")
    @ApiDoc("Update a purse based on his id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateHolder(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "purse", body -> {
            try {
                purseService.update(request.params().get("idStructure"), body, event -> {
                    if(event.isRight()){
                        Logging.defaultResponseHandler(eb,
                                request,
                                Contexts.PURSE.toString(),
                                Actions.UPDATE.toString(),
                                request.params().get("idStructure"),
                                body).handle(new Either.Right<>(event.right().getValue()));
                    }else{
                        badRequest(request);
                    }
                }
                );
            } catch (NumberFormatException e) {
                log.error("An error occurred when casting purse id", e);
                badRequest(request);
            }

        });
    }

    @Get("/purses/list")
    @ApiDoc("Get purses")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void list(final HttpServerRequest request) {
        try {
            Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
            purseService.getPursesStudentsAndLicences(page, null, event -> {
                if (event.isRight()) {
                    JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray purses = event.right().getValue();
                    JsonObject purse;
                    for (int i = 0; i < purses.size(); i++) {
                        purse = purses.getJsonObject(i);
                        ids.add(purse.getString("id_structure"));
                    }
                    structureService.getConsumableFormation(formations -> {
                        if(formations.isRight()) {
                            JsonArray res_consumable_formations = formations.right().getValue();
                            List<String> consumable_formations = res_consumable_formations
                                    .stream()
                                    .map((json) -> ((JsonObject)json).getString("label"))
                                    .collect(Collectors.toList());
                            retrieveStructuresData(ids, consumable_formations, purses, request);
                        } else {
                            renderErrorMessage(request, new Throwable(formations.left().getValue()));
                        }
                    });
                } else {
                    badRequest(request);
                }
            });
        } catch (NumberFormatException e) {
            log.error("[Crre@purses] : An error occurred when casting purses", e);
            badRequest(request);
        }
    }

    /**
     * Retrieve structure uais and name based on ids list
     * @param ids JsonArray containing ids list
     * @param purses JsonArray containing purses list
     * @param request Http request
     */
    private void retrieveStructuresData(JsonArray ids, List<String> consumable_formations, final JsonArray purses,
                                        final HttpServerRequest request) {
        structureService.getStructureById(ids, consumable_formations, event -> {
            if (event.isRight()) {
                Renders.renderJson(request, prepareDataPurses(purses, event));

            } else {
                renderError(request, new JsonObject().put("message",
                        event.left().getValue()));
            }
        });
    }

    /**
     * Retrieve structure uais based on ids list
     * @param ids JsonArray containing ids list
     * @param purses Values to exports
     * @param request Http request
     */
    private void retrieveStructures(JsonArray ids, final JsonArray purses,
                              final HttpServerRequest request) {
        structureService.getStructureById(ids, null, event -> {
            if (event.isRight()) {
                launchExport(prepareDataPurses(purses, event), request);
            } else {
                renderError(request, new JsonObject().put("message",
                        event.left().getValue()));
            }
        });
    }

    private JsonArray prepareDataPurses(JsonArray purses, Either<String, JsonArray> event) {
        JsonArray structures = event.right().getValue();
        JsonObject structure;
        JsonObject purse;

        // put structure name / uai on the purse according to structure id
        for (int i = 0; i < structures.size(); i++) {
            structure = structures.getJsonObject(i);
            for (int j = 0; j < purses.size(); j++) {
                purse = purses.getJsonObject(j);

                if (purse.getString("id_structure").equals(structure.getString("id"))) {
                    structure.put("id_structure",purse.getString("id_structure"));
                    // we convert amount to get a number instead of a string
                    structure.put("amount", purse.getDouble("amount",0.0));
                    structure.put("initial_amount", purse.getDouble("initial_amount",0.0));
                    structure.put("licence_amount", purse.getInteger("licence_amount",0));
                    structure.put("licence_initial_amount", purse.getInteger("licence_initial_amount",0));
                    structure.put("consumable_licence_amount", purse.getInteger("consumable_licence_amount",0));
                    structure.put("consumable_licence_initial_amount", purse.getInteger("consumable_licence_initial_amount",0));
                    if (purse.getBoolean("pro",false)) {
                        structure.put("seconde", purse.getInteger("seconde",0) * 3);
                        structure.put("premiere", purse.getInteger("premiere",0) * 3);
                        structure.put("terminale", purse.getInteger("terminale",0) * 3);
                    } else {
                        structure.put("seconde", purse.getInteger("seconde",0) * 9);
                        structure.put("premiere", purse.getInteger("premiere",0) * 8);
                        structure.put("terminale", purse.getInteger("terminale",0) * 7);
                    }
                }
            }
        }
        return structures;
    }

    /**
     * Launch export. Build CSV based on values parameter
     * @param purses values to export
     * @param request Http request
     */
    private static void launchExport(JsonArray purses, HttpServerRequest request) {
        StringBuilder exportString = new StringBuilder(getCSVHeader(request));
        for (Object purse : purses) {
            exportString.append(getCSVLine((JsonObject) purse));
        }
        request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=" + getFileExportName(request))
                .end(exportString.toString());
    }

    /**
     * Get CSV Header using internationalization
     * @param request Http request
     * @return CSV file Header
     */
    private static String getCSVHeader(HttpServerRequest request) {
        return I18n.getInstance().translate("UAI", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.structure", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.purse.money.initial", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.purse.money.rest", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.purse.licence.initial", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.purse.licence.rest", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.purse.licence.conso.initial", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.purse.licence.conso.rest", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.second.class", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.premiere.class", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.terminal.class", getHost(request), I18n.acceptLanguage(request)) + "\n";
    }

    /**
     * Get CSV Line
     * @param purse purse Values
     * @return CSV Line
     */
    private static String getCSVLine(JsonObject purse) {
        return  (purse.getString("uai") != null ? purse.getString("uai") : "") + ";" +
                (purse.getString("name") != null ? purse.getString("name") : "") + ";" +
                (purse.getDouble("initial_amount") != null ? purse.getDouble("initial_amount").toString() : "") + ";" +
                (purse.getDouble("amount") != null ? purse.getDouble("amount").toString() : "") + ";" +
                (purse.getInteger("licence_initial_amount") != null ? purse.getInteger("licence_initial_amount").toString() : "") + ";" +
                (purse.getInteger("licence_amount") != null ? purse.getInteger("licence_amount").toString() : "") + ";" +
                (purse.getInteger("consumable_licence_initial_amount") != null ? purse.getInteger("consumable_licence_initial_amount").toString() : "") + ";" +
                (purse.getInteger("consumable_licence_amount") != null ? purse.getInteger("consumable_licence_amount").toString() : "") + ";" +
                (purse.getInteger("seconde") != null ? purse.getInteger("seconde").toString() : "") + ";" +
                (purse.getInteger("premiere") != null ? purse.getInteger("premiere").toString() : "") + ";" +
                (purse.getInteger("terminale") != null ? purse.getInteger("terminale").toString() : "") + "\n";
    }

    /**
     * Get File Export Name. It use internationalization to build the name.
     * @param request Http request
     * @return File name
     */
    private static String getFileExportName(HttpServerRequest request) {
        return I18n.getInstance().translate("purse", getHost(request), I18n.acceptLanguage(request)) +
                ".csv";
    }

    @Get("/purse/search")
    @ApiDoc("Search in quotes")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void search(HttpServerRequest request) throws UnsupportedEncodingException {
        String query = "";
        Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
        if (request.params().contains("q")) {
            query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
        }
        structureService.searchStructureByNameUai(query, structures -> {
            if (structures.isRight()) {
                JsonArray structuresJson = structures.right().getValue();
                JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
                for (int i = 0; i < structuresJson.size(); i++) {
                    JsonObject structure = structuresJson.getJsonObject(i);
                    ids.add(structure.getString("id"));
                }
                if(ids.isEmpty()){
                    Renders.renderJson(request, new JsonObject());
                }else {
                    purseService.getPursesStudentsAndLicences(page, ids, event -> {
                        if (event.isRight()) {
                            JsonArray purses = event.right().getValue();
                            Renders.renderJson(request, prepareDataPurses(purses, structures));
                        } else {
                            log.error(event.left().getValue());
                            badRequest(request);
                        }
                    });
                }
            } else {
                log.error(structures.left().getValue());
                badRequest(request);
            }
        });
    }
}
