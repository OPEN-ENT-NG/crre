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
    search: string;

    constructor () {
       this.selected = false;
    }

}

export class Structures  extends Selection<Structure> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let {data} = await http.get(`/crre/structures`);
        this.all = Mix.castArrayAs(Structure, data);
        this.all.forEach((item) => {
            item.toString = () => item.name + " / " + item.uai;
            item.search = item.name + item.uai;
        });
    }

    async syncUserStructures (): Promise<void> {
        let { data } = await http.get('/crre/user/structures');
        this.all = Mix.castArrayAs(Structure, data);
    }

}