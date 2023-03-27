package fr.openent.crre.service;

import io.vertx.core.Future;

import java.util.List;
import java.util.Map;

public interface WorkflowService {
    /**
     * Retrieve a user's list of workflows based on structures
     *
     * @param userId user id
     * @param structureIdList list of structure id
     * @return return a map with the structure id in key and the list of workflows that the user has in the structure
     */
    Future<Map<String, List<String>>> getWorkflowListFromStructureScope(String userId, List<String> structureIdList);
}
