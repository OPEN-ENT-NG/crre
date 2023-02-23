package fr.openent.crre.service;

import fr.openent.crre.model.ProjectModel;
import io.vertx.core.Future;

import java.util.List;

public interface ProjectService {
    /**
     * Get all project from an order region id list
     *
     * @param orderRegionEquipmentIdList list of order region id
     */
    Future<List<ProjectModel>> getProjectList(List<Integer> orderRegionEquipmentIdList);
}
