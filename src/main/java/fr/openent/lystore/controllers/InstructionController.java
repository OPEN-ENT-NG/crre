package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportTypes;
import fr.openent.lystore.helpers.ExportHelper;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.openent.lystore.service.impl.DefaultInstructionService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class InstructionController extends ControllerHelper {
    private InstructionService instructionService ;
    private Storage storage;
    private ExportService exportService;

    public InstructionController(Storage storage) {
        super();
        this.storage = storage;
        this.instructionService = new DefaultInstructionService(Lystore.lystoreSchema, "instruction");
        this.exportService = new DefaultExportServiceService(storage);
    }

    @Get("/exercises")
    @ApiDoc("Returns all exercises in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExercises (HttpServerRequest request) { instructionService.getExercises(arrayResponseHandler(request));
    }

    @Get("/instructions/")
    @ApiDoc("List all instructions in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getInstructions(HttpServerRequest request) {
        instructionService.getInstructions(request.params().getAll("q"), either -> {
            if (either.isLeft()) {
                if ("404".equals(either.left().getValue())) {
                    notFound(request);
                } else {
                    renderError(request);
                }
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }

    @Post("/instruction")
    @ApiDoc("Create an instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "instruction", instruction -> instructionService.create(instruction, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.CREATE.toString(),
                instruction.toString(),
                instruction)));
    }

    @Put("/instruction/:idInstruction")
    @ApiDoc("Uptdate an instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateInstruction(final HttpServerRequest request) {
        final Integer idInstruction = Integer.parseInt(request.params().get("idInstruction"));
        RequestUtils.bodyToJson(request, pathPrefix + "instruction", instruction -> instructionService.updateInstruction(idInstruction, instruction, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.UPDATE.toString(),
                idInstruction.toString(),
                instruction)));
    }

    @Delete("/instructions")
    @ApiDoc("Delete instructions")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteInstruction(final HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, instructionIds -> instructionService.deleteInstruction(instructionIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.DELETE.toString(),
                instructionIds.toString(),
                new JsonObject().put("ids", instructionIds))));
    }

    @Get("/instructions/:id/export")
    @ApiDoc("Export given instruction")
    public void exportInstruction(HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Lystore.INSTRUCTIONS,  Lystore.XLSX, ExportTypes.RME, "_Récapitulatif_mesures_engagées");
    }

    @Get("/instructions/export/equipment/rapport/:id/:type")
    @ApiDoc("Export given instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void exportRapportEquipment(HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Lystore.INSTRUCTIONS,  Lystore.XLSX, ExportTypes.EQUIPMENT_INSTRUCTION, "_EQUIPEMENT_RAPPORT_");//type
    }

    @Get("/instruction/:id/operations")
    @ApiDoc("List all operation of instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOperationOfInstruction(HttpServerRequest request) {
        instructionService.getOperationOfInstruction(Integer.parseInt(request.getParam("id")), arrayResponseHandler(request));
    }

    @Get("/instructions/export/notification/equpment/:id")
    @ApiDoc("export given instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void exportNotificationEquipment(HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Lystore.INSTRUCTIONS, Lystore.XLSX,ExportTypes.NOTIFICATION_CP, "_Notification_Equipement_CP");
    }

    @Get("/instructions/export/publipostage/equipment/:id")
    @ApiDoc("export publipostage excel with id instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void exportPublipostageExcel(HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Lystore.INSTRUCTIONS, Lystore.XLSX,ExportTypes.PUBLIPOSTAGE, "_Liste_Etablissements_Publipostage_Notification");
    }

    ;


    @Get("/instructions/export/subvention/equipment/:id")
    @ApiDoc("export subvention excel with id instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void exportSubventionEquipment(HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Lystore.INSTRUCTIONS,Lystore.XLSX, ExportTypes.SUBVENTION, "_subvention_equipement");
    }

    @Get("/instructions/export/iris/:id")
    @ApiDoc("export irirs excel with id instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void exportIris(HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Lystore.INSTRUCTIONS, Lystore.XLSX, ExportTypes.IRIS, "_Iris");
    }
}
