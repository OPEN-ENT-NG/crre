package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.AccessRight;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.UserService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.openent.crre.service.impl.DefaultUserService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class UserController extends ControllerHelper {

    private final UserService userService;
    private final StructureService structureService;


    public UserController() {
        super();
        this.userService = new DefaultUserService();
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
    }


    @Get("/user/structures")
    @ApiDoc("Retrieve all user structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void getStructures(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user ->
                userService.getStructures(user.getUserId(), structuresResult -> {
                    if(structuresResult.isRight()) {
                        JsonArray structures = structuresResult.right().getValue();
                        renderJson(request, structures);
/* Uncomment if we need to have info (papier, num, mixte) of a structure
List<String> idStructures = new ArrayList<>();
                        for(int i = 0; i < structures.size(); i++) {
                            idStructures.add(structures.getJsonObject(i).getString("id"));
                        }
                        structureService.getAllStructureByIds(idStructures, structuresInfos -> {
                            if(structuresInfos.isRight()) {
                                JsonArray structuresCatalog = structuresInfos.right().getValue();
                                for(int j = 0; j < structures.size(); j++) {
                                    JsonObject structure = structures.getJsonObject(j);
                                    for(int i = 0; i < structuresCatalog.size(); i++) {
                                        JsonObject structureCatalog = structuresCatalog.getJsonObject(i);
                                        if(structure.getString("id").equals(structureCatalog.getString("id_structure"))) {
                                            String catalog = structureCatalog.getString("catalog");
                                            if(structureCatalog.getBoolean("mixte")) {
                                                structure.put("catalog", "Mixte");
                                            } else if (catalog != null){
                                                structure.put("catalog", structureCatalog.getString("catalog"));
                                            } else {
                                                structure.put("catalog", "Mixte");
                                            }
                                        }
                                    }
                                }
                                renderJson(request, structures);
                            } else {
                                JsonObject error = (new JsonObject()).put("[Crre@getStructures] Unable to retrieve structures details :", structuresInfos.left().getValue());
                                Renders.renderJson(request, error, 400);
                            }
                        });*/
                    } else {
                        JsonObject error = (new JsonObject()).put("[Crre@getStructures] Unable to retrieve structures infos :", structuresResult.left().getValue());
                        Renders.renderJson(request, error, 400);
                    }
        }));
    }
}
