import http from "axios";
import { toasts } from 'entcore';
import {Mix} from "entcore-toolkit";

export class Student {
    id_structure: string;
    seconde: number;
    premiere: number;
    terminale: number;
    secondepro: number;
    premierepro: number;
    terminalepro: number;
    cap1: number;
    cap2: number;
    cap3: number;
    bma1: number;
    bma2: number;
    total: number;
    total_april: number;
    pro: boolean;
    general: boolean;

    constructor() {
        this.seconde = this.premiere = this.terminale = this.secondepro = this.premierepro = this.terminalepro =
            this.cap1 = this.cap2 = this.cap3 = this.bma1 = this.bma2 = this.total = this.total_april = 0;
    }

    async updateAmount(id_structure: string, students: Student, previousTotal: number):Promise<void>{
        try {
            let url = `/crre/structure/amount/update?id_structure=${id_structure}&previousTotal=${previousTotal}`;
            await http.put(url, students);
        }
        catch (e) {
            toasts.warning('crre.structure.update.err');
        }
    }

    async getAmount(id_structure: string):Promise<void>{
        try {
            let {data} = await http.get(`/crre/structure/amount?id_structure=${id_structure}`);
            Mix.extend(this, Mix.castAs(Student, data));
        }
        catch (e) {
            toasts.warning('crre.structure.amount.err');
        }
    }
}



