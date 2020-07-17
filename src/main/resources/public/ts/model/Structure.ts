import {_} from "entcore";
import { Selectable, Mix, Selection } from 'entcore-toolkit';
import http from 'axios';

export class Structure implements Selectable {
    id: string;
    name: string;
    uai: string;
    city: string;
    academy: string;
    type:string;
    department:number;
    selected: boolean;

    constructor (name?: string, uai?: string, city?: string, department?: number) {
       if (name) this.name = name;
       if (uai) this.uai = uai;
       if (city) this.city = city;
       if(department) this.department = department;
       this.selected = false;
    }

    toJson () {
        return {
            id: this.id,
            name: this.name,
            uai: this.uai,
            city: this.city
        };
    }

}

export class Structures  extends Selection<Structure> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let {data} = await http.get(`/lystore/structures`);
        this.all = Mix.castArrayAs(Structure, data);
    }
    async getStructureType() : Promise<void> {
        let {data} = await http.get(`/lystore/structures/type`);
        this.all.map((structure)=>{
            let type = _.findWhere(data, {id: structure.id});
            structure.type = type ? type.type : 'LYC';
        })
    }

    async syncUserStructures (): Promise<void> {
        let { data } = await http.get('/lystore/user/structures');
        this.all = Mix.castArrayAs(Structure, data);
    }

}