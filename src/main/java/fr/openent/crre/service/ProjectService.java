package fr.openent.crre.service;

import fr.openent.crre.model.ProjectModel;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    /**
     * Create a new project
     *
     * @param project model which contains the information of the new project
     * @return a future of the project created
     */
    Future<Optional<ProjectModel>> createProject(ProjectModel project);

    /**
     * Get all project from an order region id list
     *
     * @param orderRegionEquipmentIdList list of order region id
     */
    Future<List<ProjectModel>> getProjectList(List<Integer> orderRegionEquipmentIdList);

    /**
     * Get the last project created
     *
     * @return a future of the last project created
     */
    Future<Optional<ProjectModel>> getLastProject();
}
