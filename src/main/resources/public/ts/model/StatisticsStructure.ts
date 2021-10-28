import http from 'axios';
import {toasts} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {Filters} from "./Filter";

export class StatisticsStructure {
    name: string;
    uai: string;
    catalog: string;
    public: string;
    id_structure: string;
    licences: object;
    orders: number;
    ressources: number;
}

export class StatisticsStructures extends StatisticsStructure {
    all: StatisticsStructure[];

    constructor() {
        super();
    }

    async get(filters: Filters, query?: String) {
        try {
            let params = "";
            filters.all.forEach(function (f) {
                params += f.name + "=" + f.value + "&";
            });

            if (!!query) {
                params += "query=" + query + "&";
            }
            let {data} = await http.get(`/crre/region/statistics/structures?${params}`);
            this.all = Mix.castArrayAs(StatisticsStructure, data);
        } catch (e) {
            toasts.warning('crre.logs.sync.err');
        }
    }

}