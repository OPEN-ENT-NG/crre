package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.PrescriptorRight;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.security.updateStudentRight;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class StructureController extends ControllerHelper {

    private final DefaultStructureService structureService;

    public StructureController(EventBus eventBus) {
        super();
        this.structureService = new DefaultStructureService(Crre.crreSchema, eventBus);
    }

    @Get("/structures")
    @ApiDoc("Returns all structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getStructures(HttpServerRequest request) {
        structureService.getStructures(arrayResponseHandler(request));
    }

    @Get("/groups/rights")
    @ApiDoc("Create all manual groups")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void createGroupsRights(HttpServerRequest request) {
        JsonArray groups = new JsonArray()
                .add(
                        new JsonObject().put("name", "CRRE-ADMINISTRATEUR")
                                .put("role", "CRRE - Administrateurs"))
                .add(
                        new JsonObject().put("name", "CRRE-VALIDATEUR")
                                .put("role", "CRRE - Validateur"))
                .add(
                        new JsonObject().put("name", "CRRE-PRESCRIPTEUR")
                                .put("role", "CRRE - Prescripteur"));
        structureService.getStructuresWithoutRight(event -> {
            if (event.isRight()) {
                int part = 0;
                JsonArray structures = event.right().getValue();
                renderJson(request, new JsonObject().put("message", "Ok"), 200);
                insertManualGroups(request, groups, structures, part);
            }
        });
    }

    private void insertManualGroups(HttpServerRequest request, JsonArray groups, JsonArray structures, int part) {
        List<Future> futures = new ArrayList<>();
        List<Future> futuresLink = new ArrayList<>();

        int start = part * 5;
        int end = start + 5;
        boolean isEnd = false;
        if (end >= structures.size()) {
            end = structures.size();
            isEnd = true;
        }
        for (int i = start; i < end; i++) {
            for (int j = 0; j < 3; j++) {
                Future<JsonObject> insertGroupFuture = Future.future();
                Future<JsonObject> getRoleFuture = Future.future();
                Future<JsonObject> linkRoleGroupFuture = Future.future();
                futures.add(insertGroupFuture);
                futures.add(getRoleFuture);
                futuresLink.add(linkRoleGroupFuture);
                String id_structure = structures.getJsonObject(i).getString("id");
                JsonObject group = new JsonObject()
                        .put("name", groups.getJsonObject(j).getString("name"))
                        .put("autolinkTargetAllStructs", false)
                        .put("autolinkTargetStructs", new JsonArray())
                        .put("autolinkUsersFromGroups", new JsonArray())
                        .put("groupDisplayName", groups.getJsonObject(j).getString("name"));
                String role = groups.getJsonObject(j).getString("role");
                structureService.createOrUpdateManual(group, id_structure, null, handlerJsonObject(insertGroupFuture));
                structureService.getRole(role, handlerJsonObject(getRoleFuture));
                CompositeFuture.all(futures).setHandler(event -> {
                    if (event.succeeded()) {
                        String groupId = insertGroupFuture.result().getString("id");
                        String roleId = getRoleFuture.result().getString("id");
                        structureService.linkRoleGroup(groupId, roleId, handlerJsonObject(linkRoleGroupFuture));
                    } else {
                        log.error("Failed to insert group and/or get roles");
                    }
                });
            }
        }
        boolean finalIsEnd = isEnd;
        CompositeFuture.all(futuresLink).setHandler(event -> {
            if (event.succeeded()) {
                log.info("ok part " + part);
                if (finalIsEnd) {
                    log.info("Linked all groups to roles");
                } else {
                    insertManualGroups(request, groups, structures, part + 1);
                }
            } else {
                log.error("ko part " + part);
            }
        });
    }


    @Post("/structures/new")
    @ApiDoc("Insert new structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getStructuresNew(HttpServerRequest request) {
        {
            request.setExpectMultipart(true);
            final Buffer buff = Buffer.buffer();
            log.info("--START getAllStructures --");
            request.uploadHandler(upload -> {
                upload.handler(buff::appendBuffer);
                upload.endHandler(end -> {
                    try {
                        Scanner sc = getScanner(buff, upload);
                        if (sc == null) return;
                        JsonArray uais = new JsonArray();
                        JsonArray structures = new JsonArray();
                        JsonArray finalStructures = new JsonArray();
                        while (sc.hasNextLine()) {
                            String userLine = sc.nextLine();
                            String[] values = userLine.split(";");
                            uais.add(values[1]);
                            JsonObject structure = new JsonObject();
                            structure.put("uai", values[1]);
                            structure.put("name", values[2]);
                            structure.put("city", values[3]);
                            structure.put("region", values[5]);
                            structure.put("public", values[6]);
                            structure.put("mixte", values[7]);
                            structure.put("catalog", values[8]);
                            structures.add(structure);
                        }
                        structureService.getStructureByUAI(uais, null, event -> {
                            if (event.isRight()) {
                                matchUAIStructure(structures, finalStructures, event);
                                try {
                                    structureService.insertNewStructures(finalStructures, event2 -> {
                                        if (event2.isRight()) {
                                            renderJson(request, event2.right().getValue());
                                        }
                                    });
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException e) {
                        log.error("Error reading zip", e);
                    } finally {
                        log.info("--END getAllStructures --");
                    }
                });
            });
        }
    }

    @Post("/reliquat/add")
    @ApiDoc("Insert reliquats")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void addReliquat(HttpServerRequest request) {
        {
            request.setExpectMultipart(true);
            final Buffer buff = Buffer.buffer();
            log.info("--START addReliquats --");
            request.uploadHandler(upload -> {
                upload.handler(buff::appendBuffer);
                upload.endHandler(end -> {
                    try {
                        Scanner sc = getScanner(buff, upload);
                        if (sc == null) return;
                        JsonArray uais = new JsonArray();
                        JsonArray structures = new JsonArray();
                        JsonArray finalStructures = new JsonArray();
                        while (sc.hasNextLine()) {
                            String userLine = sc.nextLine();
                            String[] values = userLine.split(";");
                            if (values[3].equals("Papier")) {
                                JsonObject structure = new JsonObject();
                                uais.add(values[1]);
                                structure.put("uai", values[1]);
                                structure.put("reliquat", values[9]);
                                structures.add(structure);
                            }
                        }
                        structureService.getAllStructuresDetailByUAI(uais, event -> {
                            if (event.isRight()) {
                                matchUAIStructure(structures, finalStructures, event);
                                try {
                                    structureService.updateReliquats(finalStructures, event2 -> {
                                        if (event2.isRight()) {
                                            renderJson(request, event2.right().getValue());
                                        }
                                    });
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException e) {
                        log.error("Error reading zip", e);
                    } finally {
                        log.info("--END addReliquats --");
                    }
                });
            });
        }
    }

    private Scanner getScanner(Buffer buff, HttpServerFileUpload upload) throws IOException {
        log.info("Unzip  : " + upload.filename());
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(buff.getBytes()));
        ZipEntry userFileZipEntry = zipStream.getNextEntry();
        assert userFileZipEntry != null;
        log.info("Reading : " + userFileZipEntry.getName());
        Scanner sc = new Scanner(zipStream, "ISO-8859-1");
        sc.useDelimiter(";");
        // skip header
        if (sc.hasNextLine()) {
            sc.nextLine();
        } else {
            log.info("Empty file");
            return null;
        }
        return sc;
    }

    private void matchUAIStructure(JsonArray structures, JsonArray finalStructures, Either<String, JsonArray> event) {
        log.info("success");
        JsonArray uaisNeo = event.right().getValue();
        for (int i = 0; i < structures.size(); i++) {
            String uai = structures.getJsonObject(i).getString("uai");
            for (int j = 0; j < uaisNeo.size(); j++) {
                if (uaisNeo.getJsonObject(j).getString("uai").equals(uai)) {
                    structures.getJsonObject(i).put("id", uaisNeo.getJsonObject(j).getString("id"));
                    finalStructures.add(structures.getJsonObject(i));
                    break;
                }
            }
        }
    }


    @Put("/structure/amount/update")
    @ApiDoc("Update student amount in structure")
    @SecuredAction(Crre.UPDATE_STUDENT_RIGHT)
    @ResourceFilter(updateStudentRight.class)
    public void updateAmount(final HttpServerRequest request) {
        try {
            int seconde = Integer.parseInt(request.params().get("seconde"));
            int premiere = Integer.parseInt(request.params().get("premiere"));
            int terminale = Integer.parseInt(request.params().get("terminale"));
            String id_structure = request.params().get("id_structure");
            boolean pro = Boolean.getBoolean(request.params().get("pro"));
            int previousTotal = Integer.parseInt(request.params().get("previousTotal"));
            int total_licence;

            if (pro) {
                total_licence = seconde * 3 + premiere * 3 + terminale * 3;
            } else {
                total_licence = seconde * 9 + premiere * 8 + terminale * 7;
            }

            int difference = total_licence - previousTotal;

            Future<JsonObject> updateAmountFuture = Future.future();
            Future<JsonObject> updateAmountLicenceFuture = Future.future();

            CompositeFuture.all(updateAmountFuture, updateAmountLicenceFuture).setHandler(event -> {
                if (event.succeeded()) {
                    log.info("Update amount licence success");
                    request.response().setStatusCode(201).end();
                } else {
                    log.error("Update licences amount failed");
                }
            });
            structureService.updateAmount(id_structure, seconde, premiere, terminale, handlerJsonObject(updateAmountFuture));
            structureService.reinitAmountLicence(id_structure, difference, handlerJsonObject(updateAmountLicenceFuture));
        } catch (ClassCastException e) {
            log.error("An error occurred when updating licences amount", e);
        }
    }

    @Get("/structure/amount")
    @ApiDoc("Get all students amount by structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void getAmount(final HttpServerRequest request) {
        try {
            String id_structure = request.params().get("id_structure");
            structureService.getAmount(id_structure, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred when casting basket id", e);
        }
    }
}
