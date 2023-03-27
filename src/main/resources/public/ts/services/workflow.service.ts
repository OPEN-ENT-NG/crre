import http, {AxiosResponse} from "axios";
import {ng} from "entcore";

export interface IWorkflowService {
    getWorkflowListFromStructureScope(structureIdList: Array<string>): Promise<Array<string>>
}

class IListWorkflowPayload {
    id_structure_list: Array<string>
}

export const workflowService: IWorkflowService = {
    getWorkflowListFromStructureScope(structureIdList: Array<string>): Promise<Array<string>> {
        let data: IListWorkflowPayload = {id_structure_list: structureIdList}
        return http.post(`/crre/user/workflow`, data)
            .then((res: AxiosResponse) => res.data);
    }
}

export const WorkflowService = ng.service('WorkflowService', (): IWorkflowService => workflowService);