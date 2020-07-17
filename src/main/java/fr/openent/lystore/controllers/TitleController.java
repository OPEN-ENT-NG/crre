package fr.openent.lystore.controllers;

import com.opencsv.CSVReader;
import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ImportCSVHelper;
import fr.openent.lystore.service.CampaignService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.TitleService;
import fr.openent.lystore.service.impl.DefaultCampaignService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.openent.lystore.service.impl.DefaultTitleService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class TitleController extends ControllerHelper {

    private final TitleService titleService;
    private final StructureService structureService;
    private final CampaignService campaignService;
    private final ImportCSVHelper importCSVHelper;

    public TitleController(Vertx vertx, EventBus eb) {
        super();
        titleService = new DefaultTitleService(Lystore.lystoreSchema, "title");
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.campaignService = new DefaultCampaignService(Lystore.lystoreSchema, "campaign");
        importCSVHelper = new ImportCSVHelper(vertx, eb);
    }

    @Get("/titles/campaigns/:idCampaign")
    @ApiDoc("Get list of the titles")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getTitles(HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.getParam("idCampaign"));
            titleService.getTitles(idCampaign, event -> {
                if (event.isRight()) {
                    JsonArray values = event.right().getValue();
                    JsonArray structureIds = new JsonArray();
                    for (int i = 0; i < values.size(); i++) {
                        JsonObject value = values.getJsonObject(i);
                        structureIds.add(value.getString("id_structure"));
                        value.put("titles", new JsonArray(value.getString("titles")));
                    }

                    structureService.getStructureById(structureIds, structureEvt -> {
                        if (structureEvt.isRight()) {
                            JsonArray structures = structureEvt.right().getValue();
                            JsonObject structure, map = new JsonObject();
                            for (int i = 0; i < structures.size(); i++) {
                                structure = structures.getJsonObject(i);
                                map.put(structure.getString("id"), structure.getString("name"));
                            }

                            for (int i = 0; i < values.size(); i++) {
                                structure = values.getJsonObject(i);
                                structure.put("name", map.getString(structure.getString("id_structure")));
                            }

                            renderJson(request, values);
                        } else {
                            log.error("Failed to find structures name");
                            renderError(request, new JsonObject().put("error", "Failed to find structures name"));
                        }
                    });
                } else {
                    renderError(request, new JsonObject().put("error", event.left().getValue()));
                }
            });
        } catch (NumberFormatException e) {
            badRequest(request);
        }
    }

    @Get("/titles/campaigns/admin/:idCampaign")
    @ApiDoc("Get list of the titles")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getAdminTitles(HttpServerRequest request) {
        try {
            Integer campaignId = Integer.parseInt(request.getParam("idCampaign"));

            titleService.getTitlesAdmin(campaignId, arrayResponseHandler(request));
        } catch (NumberFormatException e) {
            log.error("An error occurred when casting Campaign identifier");
            badRequest(request);
        }
    }

    @Get("/titles/campaigns/:idCampaign/structures/:idStructure")
    @ApiDoc("Get titles list based on campaign identifier and structure identifier")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getCampaignTitles(HttpServerRequest request) {
        try {
            String structureId = request.getParam("idStructure");
            Integer campaignId = Integer.parseInt(request.getParam("idCampaign"));

            titleService.getTitles(campaignId, structureId, arrayResponseHandler(request));
        } catch (NumberFormatException e) {
            log.error("An error occurred when casting Campaign identifier");
            badRequest(request);
        }
    }

    @Delete("/titles/:idTitle/campaigns/:idCampaign/structures/:idStructure")
    @ApiDoc("Delete a relationship between campaign, title and structure")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void deleteRelation(HttpServerRequest request) {
        try {
            String idStructure = request.getParam("idStructure");
            Integer idCampaign = Integer.parseInt(request.getParam("idCampaign"));
            Integer idTitle = Integer.parseInt(request.getParam("idTitle"));
            
            titleService.deleteRelation(idCampaign, idTitle, idStructure, defaultResponseHandler(request));
        } catch (NumberFormatException e) {
            badRequest(request);
        }
    }

    @Post("/titles/campaigns/:idCampaign/import")
    @ApiDoc("Import titles into a specific campaign")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void importTitles(HttpServerRequest request) {
        Integer idCampaign;
        try {
            idCampaign = Integer.parseInt(request.getParam("idCampaign"));
        } catch (NumberFormatException e) {
            badRequest(request);
            return;
        }
        final String importId = UUID.randomUUID().toString();
        final String path = config.getString("import-folder", "/tmp") + File.separator + importId;
        request.pause();
        importCSVHelper.getParsedCSV(request, path, csvEvent -> {
            request.resume();
            if (csvEvent.isRight()) {
                Buffer content = csvEvent.right().getValue();
                titleService.getTitles(titleEvent -> {
                    if (titleEvent.isRight()) {
                        JsonArray values = titleEvent.right().getValue();
                        final JsonObject titlesMap = new JsonObject();
                        for (int i = 0; i < values.size(); i++) {
                            titlesMap.put(values.getJsonObject(i).getString("name"), values.getJsonObject(i).getInteger("id"));
                        }
                        campaignService.getCampaignStructures(idCampaign, campaignEvent -> {
                            if (campaignEvent.isRight()) {
                                JsonArray campaignValues = campaignEvent.right().getValue(), campaignStructures = new JsonArray();
                                for (int i = 0; i < campaignValues.size(); i++) {
                                    campaignStructures.add(campaignValues.getJsonObject(i).getString("id_structure"));
                                }
                                try {
                                    CSVReader csv = new CSVReader(new InputStreamReader(
                                            new ByteArrayInputStream(content.getBytes())),
                                            ';', '"', 1);
                                    String[] line;
                                    JsonArray csvUais = new JsonArray();
                                    while ((line = csv.readNext()) != null) {
                                        if (!csvUais.contains(line[0])) {
                                            csvUais.add(line[0]);
                                        }
                                    }
                                    structureService.getStructureByUAI(csvUais, structureEvent -> {
                                        if (structureEvent.isRight()) {
                                            JsonArray structures = structureEvent.right().getValue();
                                            JsonObject structure, structureMap = new JsonObject();
                                            for (int i = 0; i < structures.size(); i++) {
                                                structure = structures.getJsonObject(i);
                                                structureMap.put(structure.getString("uai"), structure.getString("id"));
                                            }
                                            titleService.getRelationForCampaign(idCampaign, event -> {
                                                if (event.isRight()) {
                                                    parseTitlesImport(request, idCampaign, campaignStructures, structureMap, content, titlesMap, event.right().getValue());
                                                } else {
                                                    log.error("Failed to get title relation", event.left().getValue());
                                                    renderError(request, new JsonObject().put("error", event.left().getValue()));
                                                }
                                            });
                                        } else {
                                            log.error("Failed to load all structures", structureEvent.left().getValue());
                                            renderError(request, new JsonObject().put("error", structureEvent.left().getValue()));
                                        }
                                    });
                                } catch (IOException e) {
                                    log.error("Failed to parse CSV", e);
                                }
                            } else {
                                log.error("An error occurred when listing all campaign structures", campaignEvent.left().getValue());
                                renderError(request, new JsonObject().put("error", campaignEvent.left().getValue()));
                            }
                        });
                    } else {
                        log.error("An error occurred when listing all titles", titleEvent.left().getValue());
                        renderError(request, new JsonObject().put("error", titleEvent.left().getValue()));
                    }
                });
            } else {
                renderError(request);
            }
        });
    }

    private void parseTitlesImport(HttpServerRequest request, Integer idCampaign, JsonArray campaignStructures, JsonObject csvUaisMap,
                                   Buffer content, JsonObject titlesMap, JsonArray existingValues) {
        try {
            String[] line;
            JsonObject importMap = new JsonObject(),
                    newTitlesMaps = new JsonObject(),
                    map;
            CSVReader csv = new CSVReader(new InputStreamReader(
                    new ByteArrayInputStream(content.getBytes())),
                    ';', '"', 1);
            while ((line = csv.readNext()) != null) {
                String uai = line[0].trim();
                String title = (titlesMap.getMap().containsKey(line[1].trim())) ? titlesMap.getInteger(line[1].trim()).toString() : line[1].trim();
                map = titlesMap.getMap().containsKey(line[1].trim()) ? importMap : newTitlesMaps;
                if (!title.equals("")) {
                    if (!map.containsKey(title)) {
                        // If import map does not contains title identifier, add it and initialize the value with an empty JsonArray
                        map.put(title, new JsonArray());
                    }
                    if (uai.isEmpty()) {
                        // If no UAI is filled, insert title for all campaign structures
                        JsonArray structures = new JsonArray();
                        for (int i = 0; i < campaignStructures.size(); i++) {
                            if (!relationExists(existingValues, campaignStructures.getString(i), line[1].trim())) {
                                structures.add(campaignStructures.getString(i));
                            }
                        }
                        map.put(title, structures);
                    } else if (csvUaisMap.containsKey(uai) && !relationExists(existingValues, csvUaisMap.getString(uai), line[1].trim())) {
                        // If relation does not exists, insert structure title identifier array
                        if (csvUaisMap.containsKey(line[0].trim())) {
                            map.getJsonArray(title).add(csvUaisMap.getString(line[0].trim()));
                        }
                    }
                    // Otherwise, nothing happen...
                }
            }
            titleService.importTitlesForCampaign(idCampaign, importMap, newTitlesMaps, defaultResponseHandler(request));
        } catch (IOException e) {
            log.error("Failed to parse CSV", e);
            renderError(request, new JsonObject().put("error", e));
        }
    }

    private boolean relationExists(JsonArray existingValues, String id, String title) {
        JsonObject o;
        for (int i = 0; i < existingValues.size(); i++) {
            o = existingValues.getJsonObject(i);
            if (id.equals(o.getString("id_structure")) && title.equals(o.getString("name"))) {
                return true;
            }
        }
        return false;
    }

}
