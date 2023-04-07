import http, {AxiosResponse} from "axios";
import {ng} from "entcore";
import {IWorkflowNeo4jModel, WorkflowNeo4jModel} from "../model/WorkflowNeo4jModel";

export interface IWorkflowService {
    getWorkflowListFromStructureScope(structureIdList: Array<string>): Promise<Map<string, Array<WorkflowNeo4jModel>>>
}

class IListWorkflowPayload {
    id_structure_list: Array<string>
}

export const workflowService: IWorkflowService = {
    getWorkflowListFromStructureScope(structureIdList: Array<string>): Promise<Map<string, Array<WorkflowNeo4jModel>>> {
        let data: IListWorkflowPayload = {id_structure_list: structureIdList}
        return http.post(`/crre/user/workflow`, data)
            .then((res: AxiosResponse) => {
                let result: Map<string, Array<WorkflowNeo4jModel>> = new Map();
                for (const key of Object.keys(res.data)) {
                    result.set(key, res.data[key].map((el: IWorkflowNeo4jModel) => new WorkflowNeo4jModel(el)));
                }
                return result;
            });
    }
}

export const WorkflowService = ng.service('WorkflowService', (): IWorkflowService => workflowService);