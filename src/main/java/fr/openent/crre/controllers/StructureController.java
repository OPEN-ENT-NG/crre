package fr.openent.crre.controllers;

import com.opencsv.CSVReader;
import fr.openent.crre.Crre;
import fr.openent.crre.security.*;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.io.*;
import java.text.ParseException;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class StructureController extends ControllerHelper {

    private final DefaultStructureService structureService;

    public StructureController(){
        super();
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

    @Get("/structures")
    @ApiDoc("Returns all structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getStructures(HttpServerRequest request){
        structureService.getStructures(arrayResponseHandler(request));
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
                            log.info("Empty structures file");
                            return;
                        }
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
                            if(event.isRight()) {
                                log.info("success");
                                JsonArray uaisNeo = event.right().getValue();
                                for(int i = 0; i < structures.size(); i++) {
                                    String uai = structures.getJsonObject(i).getString("uai");
                                    for(int j = 0; j < uaisNeo.size(); j++) {
                                        if(uaisNeo.getJsonObject(j).getString("uai").equals(uai)) {
                                            structures.getJsonObject(i).put("id", uaisNeo.getJsonObject(j).getString("id"));
                                            finalStructures.add(structures.getJsonObject(i));
                                            break;
                                        }
                                    }
                                }
                                try {
                                    structureService.insertNewStructures(finalStructures, event2 -> {
                                      if(event2.isRight()) {
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


    @Put("/structure/amount/update")
    @ApiDoc("Update student amount in structure")
    @SecuredAction(Crre.UPDATE_STUDENT_RIGHT)
    @ResourceFilter(updateStudentRight.class)
    public void updateAmount(final HttpServerRequest  request){
            try {
                int seconde = Integer.parseInt(request.params().get("seconde"));
                int premiere = Integer.parseInt(request.params().get("premiere"));
                int terminale = Integer.parseInt(request.params().get("terminale"));
                String id_structure = request.params().get("id_structure");
                boolean pro = Boolean.getBoolean(request.params().get("pro"));
                int previousTotal = Integer.parseInt(request.params().get("previousTotal"));
                int total_licence;

                if(pro) {
                    total_licence = seconde * 3 + premiere * 3 + terminale * 3;
                } else {
                    total_licence = seconde * 9 + premiere * 8 + terminale * 7;
                }

                int difference = total_licence - previousTotal;

                Future<JsonObject> updateAmountFuture = Future.future();
                Future<JsonObject> updateAmountLicenceFuture = Future.future();

                CompositeFuture.all(updateAmountFuture, updateAmountLicenceFuture).setHandler(event -> {
                    if(event.succeeded()) {
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
    public void getAmount(final HttpServerRequest  request) {
            try {
                String id_structure = request.params().get("id_structure");
                structureService.getAmount(id_structure, defaultResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred when casting basket id", e);
            }
    }
}
