package fr.openent.crre.controllers;

import com.opencsv.CSVReader;
import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.exception.CRREException;
import fr.openent.crre.exception.ImportPurseException;
import fr.openent.crre.helpers.HttpRequestHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.Credits;
import fr.openent.crre.model.Purse;
import fr.openent.crre.model.PurseImport;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StructureService;
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
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.crre.core.constants.Field.UTF8_BOM;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class PurseController extends ControllerHelper {


    private final StructureService structureService;
    private final PurseService purseService;
    private final Storage storage;

    public PurseController(Storage storage, ServiceFactory serviceFactory) {
        super();
        this.structureService = serviceFactory.getStructureService();
        this.purseService = serviceFactory.getPurseService();
        this.storage = storage;
    }

    @Post("/purses/import")
    @ApiDoc("Import purse")
    @SecuredAction(Crre.ADMINISTRATOR_RIGHT)
    @ResourceFilter(AdministratorRight.class)
    public void purse(final HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!Field.OK.equals(entries.getString(Field.STATUS))) {
                renderError(request);
                return;
            }
            String fileId = entries.getString("_id");
            String filename = entries.getJsonObject("metadata").getString("filename");
            parseCsv(fileId, request)
                    .onSuccess(success -> {
                        Renders.renderJson(request, new JsonObject());
                        JsonObject contentObject = new JsonObject().put("content", success.getBase64());
                        Logging.insert(eb, request, Contexts.PURSE.toString(),
                                Actions.IMPORT.toString(), "", contentObject);
                        deleteImportPath(vertx, filename);
                    })
                    .onFailure(fail -> HttpRequestHelper.sendError(request, fail));
        });
    }

    /**
     * Parse CSV file
     */
    private Future<PurseImport> parseCsv(final String fileId, HttpServerRequest request) {
        Promise<PurseImport> promise = Promise.promise();
        storage.readFile(fileId, event -> {
            try {
                CSVReader csv = new CSVReader(new InputStreamReader(
                        new ByteArrayInputStream(event.getBytes())),
                        ';', '"', 1);
                String[] values;
                List<String> uaiList = new ArrayList<>();
                List<String> uaiErrorList = new ArrayList<>();
                List<Purse> purses = new ArrayList<>();

                // Inutiliser à supprimer un jour si on est sur que les licences ne seront plus de mise et le nombre d'élèves ne prends pas tout les cas (pro, bma, cap etc..)
                JsonObject licences = new JsonObject();
                JsonObject consumable_licences = new JsonObject();
                JsonObject seconds = new JsonObject();
                JsonObject premieres = new JsonObject();
                JsonObject terminales = new JsonObject();
                while ((values = csv.readNext()) != null) {
                    Purse purse = new Purse();
                    String uai = values[0];
                    purse.setUai(uai);
                    try {
                        purse.setCredits(new Credits(values[1], values[2], values[3]));
                        purse.setCreditsConsumable(new Credits(values[4], values[5], values[6]));
                        addValueToJson(uai, licences, values[7]);
                        addValueToJson(uai, consumable_licences, values[8]);
                        addValueToJson(uai, seconds, values[9]);
                        addValueToJson(uai, premieres, values[10]);
                        addValueToJson(uai, terminales, values[11]);
                    } catch (NumberFormatException e) {
                        uaiErrorList.add(uai);
                        continue;
                    }
                    uaiList.add(uai);
                    purses.add(purse);
                }
                if (uaiList.size() > 0 || uaiErrorList.size() > 0) {
                    matchUAIID(uaiList, uaiErrorList, purses, licences, consumable_licences, seconds, premieres, terminales)
                            .onSuccess(success -> promise.complete(new PurseImport(uaiErrorList, event.toString())))
                            .onFailure(fail -> {
                                if (fail instanceof ImportPurseException) {
                                    ((ImportPurseException) fail).setUAIErrorList(uaiErrorList);
                                }
                                promise.fail(fail);
                            });
                } else {
                    promise.fail(new ImportPurseException("crre.error.message.missing.uai"));
                }
            } catch (IOException e) {
                log.error("[Crre@CSVImport]: csv exception", e);
                promise.fail(new ImportPurseException("crre.error.message.parse.csv"));
            }
        });
        return promise.future();
    }

    private Credits computeAmount(String newAmount, String addAmount, String refundAmount) {
        Credits newCredit = new Credits();
        if (!newAmount.isEmpty() && !addAmount.isEmpty()) {
            newCredit.setNewValue(Double.parseDouble(newAmount) + Double.parseDouble(addAmount));
        } else {
            if (!newAmount.isEmpty()) {
                newCredit.setNewValue(Double.parseDouble(newAmount));
            } else if (!addAmount.isEmpty() && !refundAmount.isEmpty()) {
                newCredit.setAddValue(Double.parseDouble(addAmount));
                newCredit.setRefundValue(Double.parseDouble(refundAmount));
            } else if (!addAmount.isEmpty()) {
                newCredit.setAddValue(Double.parseDouble(addAmount));
            } else if (!refundAmount.isEmpty()) {
                newCredit.setRefundValue(Double.parseDouble(refundAmount));
            }
        }
        return newCredit;
    }


    private void addValueToJson(String uai, JsonObject object, String value) {
        if (value.isEmpty()) {
            object.put(uai, 0);
        } else {
            object.put(uai, Integer.parseInt(value));
        }
    }

    /**
     * Match structure UAI with its Neo4j id.
     *
     * @param uaiList      UAIs list
     * @param uaiErrorList
     * @param purses       Object containing UAI as key and purse amount as value
     */
    private Future<PurseImport> matchUAIID(List<String> uaiList,
                                           List<String> uaiErrorList, List<Purse> purses,
                                           final JsonObject licence, final JsonObject consumable_licence,
                                           final JsonObject seconds, final JsonObject premieres, final JsonObject terminales) {
        Promise<PurseImport> promise = Promise.promise();
        structureService.getConsumableFormation(formations -> {
            if (formations.isRight()) {
                JsonArray res_consumable_formations = formations.right().getValue();
                List<String> consumable_formations = res_consumable_formations
                        .stream()
                        .map((json) -> ((JsonObject) json).getString("label"))
                        .collect(Collectors.toList());
                structureService.getStructureByUAI(new JsonArray(uaiList), consumable_formations, event -> {
                    if (event.isRight()) {
                        final JsonArray structures = event.right().getValue();
                        JsonObject statementsValues = new JsonObject();
                        for (int i = 0; i < structures.size(); i++) {
                            final JsonObject structure = structures.getJsonObject(i);
                            boolean professionnal_structure = structure.containsKey("type") &&
                                    structure.getString("type") != null && structure.getString("type").equals("LYCEE PROFESSIONNEL");
                            try {
                                int licences = licence.getInteger(structure.getString("uai"));
                                int consumable_licences = consumable_licence.getInteger(structure.getString("uai"));
                                int seconde = seconds.getInteger(structure.getString("uai"));
                                int premiere = premieres.getInteger(structure.getString("uai"));
                                int terminale = terminales.getInteger(structure.getString("uai"));

                                int minimumLicences;
                                if (professionnal_structure) {
                                    minimumLicences = seconde * 3 + premiere * 3 + terminale * 3;
                                } else {
                                    minimumLicences = seconde * 9 + premiere * 8 + terminale * 7;
                                }
                                if (licences < minimumLicences) {
                                    licence.put(structure.getString("uai"), minimumLicences);
                                }

                                int minimumConsumableLicences;
                                minimumConsumableLicences = structure.getInteger("nbr_students_consumables") * 2;
                                if (consumable_licences < minimumConsumableLicences) {
                                    consumable_licence.put(structure.getString("uai"), minimumConsumableLicences);
                                }

                            } catch (NumberFormatException e) {
                            }
                            Purse purse = purses
                                    .stream()
                                    .filter(purseFilter -> purseFilter.getUai().equals(structure.getString(Field.UAI)))
                                    .findFirst()
                                    .orElse(null);

                            if (purse != null) {
                                purse.setIdStructure(structure.getString(Field.ID));
                            }
                            statementsValues.put(structure.getString(Field.ID), new JsonObject()
                                    .put("purses", purse != null ? purse.toJson() : null)
                                    .put("licence", licence.getInteger(structure.getString("uai")))
                                    .put("consumable_licence", consumable_licence.getInteger(structure.getString("uai")))
                                    .put("second", seconds.getInteger(structure.getString("uai")))
                                    .put("premiere", premieres.getInteger(structure.getString("uai")))
                                    .put("terminale", terminales.getInteger(structure.getString("uai")))
                                    .put("pro", professionnal_structure)
                            );
                        }
                        purseService.launchImport(statementsValues, uaiErrorList)
                                .onSuccess(promise::complete)
                                .onFailure(promise::fail);
                    } else {
                        promise.fail(event.left().getValue());
                    }
                });
            } else {
                promise.fail(formations.left().getValue());
            }
        });
        return promise.future();
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
            List<String> params = request.params().getAll(Field.ID);
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
                            if (event.isRight()) {
                                Logging.defaultResponseHandler(eb,
                                        request,
                                        Contexts.PURSE.toString(),
                                        Actions.UPDATE.toString(),
                                        request.params().get("idStructure"),
                                        body).handle(new Either.Right<>(event.right().getValue()));
                            } else {
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
                    getPursesInformations(request, ids, purses);
                } else {
                    badRequest(request);
                }
            });
        } catch (NumberFormatException e) {
            log.error("[Crre@purses] : An error occurred when casting purses", e);
            badRequest(request);
        }
    }

    @Get("/purse/search")
    @ApiDoc("Search in purses")
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
                    ids.add(structure.getString(Field.ID));
                }
                if (ids.isEmpty()) {
                    Renders.renderJson(request, new JsonObject());
                } else {
                    purseService.getPursesStudentsAndLicences(page, ids, event -> {
                        if (event.isRight()) {
                            JsonArray purses = event.right().getValue();
                            getPursesInformations(request, ids, purses);
                        } else {
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

    private void getPursesInformations(HttpServerRequest request, JsonArray ids, JsonArray purses) {
        structureService.getConsumableFormation(formations -> {
            if (formations.isRight()) {
                JsonArray res_consumable_formations = formations.right().getValue();
                List<String> consumable_formations = res_consumable_formations
                        .stream()
                        .map((json) -> ((JsonObject) json).getString("label"))
                        .collect(Collectors.toList());
                retrieveStructuresData(ids, consumable_formations, purses, request);
            } else {
                renderErrorMessage(request, new Throwable(formations.left().getValue()));
            }
        });
    }

    /**
     * Retrieve structure uais and name based on ids list
     *
     * @param ids     JsonArray containing ids list
     * @param purses  JsonArray containing purses list
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
     *
     * @param ids     JsonArray containing ids list
     * @param purses  Values to exports
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

                if (purse.getString("id_structure").equals(structure.getString(Field.ID))) {
                    structure.put("id_structure", purse.getString("id_structure"));
                    // we convert amount to get a number instead of a string
                    structure.put("amount", purse.getDouble("amount", 0.0));
                    structure.put("initial_amount", purse.getDouble("initial_amount", 0.0));
                    structure.put("consumable_amount", purse.getDouble("consumable_amount", 0.0));
                    structure.put("consumable_initial_amount", purse.getDouble("consumable_initial_amount", 0.0));
                    structure.put("licence_amount", purse.getInteger("licence_amount", 0));
                    structure.put("licence_initial_amount", purse.getInteger("licence_initial_amount", 0));
                    structure.put("consumable_licence_amount", purse.getInteger("consumable_licence_amount", 0));
                    structure.put("consumable_licence_initial_amount", purse.getInteger("consumable_licence_initial_amount", 0));
                    if (purse.getBoolean("pro", false)) {
                        structure.put("seconde", purse.getInteger("seconde", 0) * 3);
                        structure.put("premiere", purse.getInteger("premiere", 0) * 3);
                        structure.put("terminale", purse.getInteger("terminale", 0) * 3);
                    } else {
                        structure.put("seconde", purse.getInteger("seconde", 0) * 9);
                        structure.put("premiere", purse.getInteger("premiere", 0) * 8);
                        structure.put("terminale", purse.getInteger("terminale", 0) * 7);
                    }
                }
            }
        }
        return structures;
    }

    /**
     * Launch export. Build CSV based on values parameter
     *
     * @param purses  values to export
     * @param request Http request
     */
    private static void launchExport(JsonArray purses, HttpServerRequest request) {
        StringBuilder exportString = new StringBuilder(UTF8_BOM).append(UTF8_BOM).append(getCSVHeader(request));
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
     *
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
     *
     * @param purse purse Values
     * @return CSV Line
     */
    private static String getCSVLine(JsonObject purse) {
        return (purse.getString("uai") != null ? purse.getString("uai") : "") + ";" +
                (purse.getString(Field.NAME) != null ? purse.getString(Field.NAME) : "") + ";" +
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
     *
     * @param request Http request
     * @return File name
     */
    private static String getFileExportName(HttpServerRequest request) {
        return I18n.getInstance().translate("purse", getHost(request), I18n.acceptLanguage(request)) +
                ".csv";
    }
}
