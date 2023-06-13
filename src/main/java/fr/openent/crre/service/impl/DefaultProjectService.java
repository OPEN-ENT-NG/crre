package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.ProjectModel;
import fr.openent.crre.service.ProjectService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefaultProjectService implements ProjectService {
    @Override
    public Future<Optional<ProjectModel>> createProject(ProjectModel project) {
        Promise<Optional<ProjectModel>> promise = Promise.promise();

        String queryProjectEquipment = "" +
                "INSERT INTO " + Crre.crreSchema + ".project " +
                "( title, comment ) VALUES " +
                "( ?,? )  RETURNING *;";
        JsonArray params = new JsonArray();

        params.add(project.getTitle());
        params.add(project.getComment());

        String errorMessage = String.format("[CRRE@%s::createProject] Fail to create project",
                this.getClass().getSimpleName());
        Sql.getInstance().prepared(queryProjectEquipment, params, new DeliveryOptions().setSendTimeout(600000L),
                SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, ProjectModel.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<ProjectModel>> getProjectList(List<Integer> orderRegionEquipmentIdList) {
        Promise<List<ProjectModel>> promise = Promise.promise();

        if (orderRegionEquipmentIdList.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        JsonArray params = new JsonArray(new ArrayList<>(orderRegionEquipmentIdList));
        params.addAll(new JsonArray(orderRegionEquipmentIdList));
        String query = "SELECT p.* " +
                "FROM crre.project p " +
                "LEFT JOIN crre.\"order-region-equipment-old\" o_r_e_o ON p.id = o_r_e_o.id_project " +
                "LEFT JOIN crre.\"order-region-equipment\" o_r_e ON o_r_e.id_project = p.id " +
                "WHERE o_r_e_o.id IN " + Sql.listPrepared(orderRegionEquipmentIdList) + " OR o_r_e.id IN " + Sql.listPrepared(orderRegionEquipmentIdList) + ";";

        String errorMessage = String.format("[CRRE@%s::getProjectList] Fail to get project list", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, ProjectModel.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<Optional<ProjectModel>> getLastProject() {
        Promise<Optional<ProjectModel>> promise = Promise.promise();

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT p.*" +
                " FROM  " + Crre.crreSchema + ".project p " +
                " ORDER BY p.id DESC LIMIT 1";
        String errorMessage = String.format("[CRRE@%s::getLastProject] Fail to get last project", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, ProjectModel.class, errorMessage)));

        return promise.future();
    }
}
