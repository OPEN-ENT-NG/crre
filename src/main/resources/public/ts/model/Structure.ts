import { Selectable, Mix, Selection } from 'entcore-toolkit';
import http from 'axios';
import {Log} from "./Log";

export class Structure implements Selectable {
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

    constructor () {
       this.selected = false;
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
    }

    async syncUserStructures (): Promise<void> {
        let { data } = await http.get('/crre/user/structures');
        this.all = Mix.castArrayAs(Structure, data);
    }

}