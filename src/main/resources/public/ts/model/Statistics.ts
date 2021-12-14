import http from 'axios';
import {toasts} from 'entcore';
import {Mix} from "entcore-toolkit";
import {Filters} from "./Filter";
declare let window: any;


export class Statistics {

    allNumericRessources: object;
    allPaperRessources: object;
    allRessources: object;
    ressources: number;
    orders: number;
    licences: object;
    structuresMoreOneOrder: object;
    structures: object;
    allStructures: object;

    async get (filters?: Filters) {
        try {
            let params = "";
            if(!!filters) {
                filters.all.forEach(function (f) {
                    params += f.name + "=" + f.value + "&";
                });
            }
            let { data } = await http.get(`/crre/region/statistics?${params}`);
            Mix.extend(this, Mix.castAs(Statistics, data[0]));
        } catch (e) {
            toasts.warning('crre.logs.sync.err');
        }
    }

    async exportCSV(filters?: Filters) {
        let params = "";
        if(!!filters) {
            filters.all.forEach(function (f) {
                params += f.name + "=" + f.value + "&";
            });
        }
        window.location = `/crre/stats/exports?${params}`;
    };


}