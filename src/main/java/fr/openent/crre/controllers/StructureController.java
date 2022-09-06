package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.*;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
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

    private static final String SECONDE = "seconde";
    private static final String PREMIERE = "premiere";
    private static final String TERMINALE = "terminale";

    private static final String SECONDETECHNO = "secondetechno";

    private static final String PREMIERETECHNO = "premieretechno";

    private static final String TERMINALETECHNO = "terminaletechno";
    private static final String SECONDEPRO = "secondepro";
    private static final String PREMIEREPRO = "premierepro";
    private static final String TERMINALEPRO = "terminalepro";
    private static final String BMA1 = "bma1";
    private static final String BMA2 = "bma2";
    private static final String CAP1 = "cap1";
    private static final String CAP2 = "cap2";
    private static final String CAP3 = "cap3";
    private static final String ID_STRUCTURE = "id_structure";
    private static final String UAI = "uai";
    private static final String ID = "id";

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
                        new JsonObject().put("name", "CRRE-VALIDEUR")
                                .put("role", "val"))
                .add(
                        new JsonObject().put("name", "CRRE-PRESCRIPTEUR")
                                .put("role", "presc"));
        structureService.getStructuresWithoutRight(event -> {
            if (event.isRight()) {
                int part = 0;
                JsonArray structures = event.right().getValue();
                renderJson(request, new JsonObject().put("message", "Ok"), 200);
                insertManualGroups(groups, structures, part);
            } else {
                log.error("Failed to getStructuresWithoutRight : ", event.left().getValue());
                renderError(request);
            }
        });
    }

    private void insertManualGroups(JsonArray groups, JsonArray structures, int part) {
        // List futures getting role and inserting group
        List<Future> futures = new ArrayList<>();

        // List futures linking role and group
        List<Future> futuresLink = new ArrayList<>();

        int start = part * 5;
        int end = start + 5;
        boolean isEnd = false;
        if (end >= structures.size()) {
            end = structures.size();
            isEnd = true;
        }
        for (int i = start; i < end; i++) {
            for (int j = 0; j < 2; j++) {
                Future<JsonObject> insertGroupFuture = Future.future();
                Future<JsonObject> getRoleFuture = Future.future();
                Future<JsonObject> linkRoleGroupFuture = Future.future();
                futures.add(insertGroupFuture);
                futures.add(getRoleFuture);
                futuresLink.add(linkRoleGroupFuture);
                String id_structure = structures.getJsonObject(i).getString(ID);
                log.info("Insert manual groups and rights for CRRE for structure : " + structures.getJsonObject(i).getString(UAI));
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
                        String groupId = insertGroupFuture.result().getString(ID);
                        String roleId = getRoleFuture.result().getString(ID);
                        if (roleId != null && groupId != null) {
                            structureService.linkRoleGroup(groupId, roleId, handlerJsonObject(linkRoleGroupFuture));
                        } else {
                            linkRoleGroupFuture.fail("Failed to insert group and/or get roles -  groupId or roleId are null ; for id_structure :" + id_structure);
                            log.error("Failed to insert group and/or get roles -  groupId or roleId are null ; for id_structure : " + id_structure);
                        }
                    } else {
                        log.error("Failed to insert group and/or get roles - 1 ; for id_structure : " + id_structure, event.cause());
                    }
                });
            }
        }
        boolean finalIsEnd = isEnd;
        CompositeFuture.join(futuresLink).setHandler(event -> {
            if (event.succeeded()) {
                log.info("ok part " + part);
            } else {
                log.error("ko part " + part);
            }
            // Check if final part else continue script
            if (finalIsEnd) {
                log.info("Linked all groups to roles");
            } else {
                insertManualGroups(groups, structures, part + 1);
            }
        });
    }

    @Get("/structures/students")
    @ApiDoc("Insert new students")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void insertStudents(HttpServerRequest request) {
        structureService.getAllStructure(structures -> {
            if(structures.isRight()) {
                log.info("[getStudents] getAllStructures OK");
                JsonArray structure_id = structures.right().getValue();
                JsonArray ids = new JsonArray();
                for (int i = 0; i < structure_id.size(); i++) {
                    ids.add(structure_id.getJsonObject(i).getString(ID_STRUCTURE));
                }
                structureService.insertStudentsInfos(ids, defaultResponseHandler(request));
            } else {
                log.error("Failed to get all structures",structures.left());
                renderJson(request, new JsonObject().put("message", "error"));
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
                            structure.put(UAI, values[1]);
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
                                structure.put(UAI, values[1]);
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
            String uai = structures.getJsonObject(i).getString(UAI);
            for (int j = 0; j < uaisNeo.size(); j++) {
                if (uaisNeo.getJsonObject(j).getString(UAI).equals(uai)) {
                    structures.getJsonObject(i).put(ID, uaisNeo.getJsonObject(j).getString(ID));
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
        RequestUtils.bodyToJson(request, students -> {
            try {
                String id_structure = request.params().get(ID_STRUCTURE);
                int previousTotal = Integer.parseInt(request.params().get("previousTotal"));
                int total_licence = students.getInteger(SECONDE) * 9 + students.getInteger(PREMIERE) * 8 + students.getInteger(TERMINALE) * 7 +
                        ((students.getInteger(SECONDEPRO) + students.getInteger(PREMIEREPRO)  + students.getInteger(TERMINALEPRO) +
                        students.getInteger(CAP1) + students.getInteger(CAP2)  + students.getInteger(CAP3) +
                        students.getInteger(BMA1) + students.getInteger(BMA2))  * 3);

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
                structureService.updateAmount(id_structure, students, handlerJsonObject(updateAmountFuture));
                structureService.reinitAmountLicence(id_structure, difference, handlerJsonObject(updateAmountLicenceFuture));
            } catch (ClassCastException e) {
                log.error("An error occurred when updating licences amount", e);
            }
        });
    }

    @Get("/structure/amount")
    @ApiDoc("Get all students amount by structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorAndStructureRight.class)
    public void getAmount(final HttpServerRequest request) {
        try {
            String id_structure = request.params().get("idStructure");
            structureService.getAmount(id_structure, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred when casting basket id", e);
        }
    }
}
