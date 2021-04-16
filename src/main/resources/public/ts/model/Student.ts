import http from "axios";
import { toasts } from 'entcore';
import {Mix} from "entcore-toolkit";

export class Student {
    id_structure: string;
    seconde: number;
    premiere: number;
    terminale: number;
    total: number;
    pro: boolean;

    constructor() {}

    async updateAmount(id_structure: string, seconde: number, premiere: number, terminale: number, pro: boolean, previousTotal: number):Promise<void>{
        try {
            let url = `/crre/structure/amount/update?seconde=${seconde}&premiere=${premiere}&terminale=${terminale}`;
            url += `&id_structure=${id_structure}&pro=${pro}&previousTotal=${previousTotal}`;
            await http.put( url);
        }
        catch {
            toasts.warning('crre.structure.update.err');
        }
    }

    async getAmount(id_structure: string):Promise<void>{
        try {
            let {data} = await http.get(`/crre/structure/amount?id_structure=${id_structure}`);
            Mix.extend(this, Mix.castAs(Student, data));
        }
        catch {
            toasts.warning('crre.structure.amount.err');
        }
    }
}



