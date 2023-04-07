import { Selectable, Mix, Selection } from 'entcore-toolkit';
import http from 'axios';
import {Log} from "./Log";
import {IFilter} from "./Filter";
import {workflowService} from "../services";
import { WorkflowNeo4jModel } from './WorkflowNeo4jModel';

export class Structure implements Selectable, IFilter {
    id: string;
    name: string;
    uai: string;
    city: string;
    academy: string;
    type:string;
    department:number;
    selected: boolean;
    search: string;
    inregroupment : boolean
    workflow: Array<WorkflowNeo4jModel>;

    constructor () {
       this.selected = false;
    }

    getValue(): string {
        return this.id;
    }

}

export class Structures  extends Selection<Structure> {
    inRegroupement: Structure[];

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let {data} = await http.get(`/crre/structures`);
        this.all = Mix.castArrayAs(Structure, data);
        this.inRegroupement = [];
        for(let i=0; i<this.all.length; i++) {
            let structure =  this.all[i];
            structure.toString = () => structure.name + " / " + structure.uai;
            structure.search = structure.name + structure.uai;
            if(structure.inregroupment)
                this.inRegroupement.push(structure)
        }
        await this.calculatesWorkflow();
    }

    async syncUserStructures (): Promise<void> {
        let { data } = await http.get('/crre/user/structures');
        this.all = Mix.castArrayAs(Structure, data);
        await this.calculatesWorkflow();
    }

    private async calculatesWorkflow(): Promise<void> {
        await workflowService.getWorkflowListFromStructureScope(this.all.map((structure: Structure) => structure.id))
            .then((result: Map<string, Array<WorkflowNeo4jModel>>) => {
                this.all.forEach((structure: Structure) => {
                    structure.workflow = result.get(structure.id);
                })
            })
            .catch(error => console.error(error));
    }
}