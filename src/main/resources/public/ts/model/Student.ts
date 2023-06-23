import http from "axios";
import {idiom as lang, toasts} from 'entcore';
import {Mix} from "entcore-toolkit";

const fieldToI18nMap = new Map<string, string>([
    ["seconde", "crre.second.class"],
    ["premiere", "crre.premiere.class"],
    ["terminale", "crre.terminal.class"],
    ["secondepro", "crre.second.pro.class"],
    ["premierepro", "crre.premiere.pro.class"],
    ["terminalepro", "crre.terminal.pro.class"],
    ["cap1", "crre.first.cap.class"],
    ["cap2", "crre.second.cap.class"],
    ["cap3", "crre.third.cap.class"],
    ["bma1", "crre.first.bma.class"],
    ["bma2", "crre.second.bma.class"]
]);

export interface StudentInfo {
    title: string
    value: number
}

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

    // Calculated value
    private _studentInfo: Array<StudentInfo>

    constructor() {
        this.seconde = this.premiere = this.terminale = this.secondepro = this.premierepro = this.terminalepro =
            this.cap1 = this.cap2 = this.cap3 = this.bma1 = this.bma2 = this.total = this.total_april = 0;
        this.calculateStudentInformation();
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
            let {data} = await http.get(`/crre/structure/amount?idStructure=${id_structure}`);
            Mix.extend(this, Mix.castAs(Student, data));

            this.calculateStudentInformation();
        }
        catch (e) {
            toasts.warning('crre.structure.amount.err');
        }
    }

    private calculateStudentInformation(): void {
        this._studentInfo = Object.keys(this).filter((key: string) => typeof this[key] === 'number' && this[key] > 0 && fieldToI18nMap.has(key))
            .map((key: string) => {
                return {title: lang.translate(fieldToI18nMap.get(key)), value: this[key]};
            });
    }

    get studentInfo(): Array<StudentInfo> {
        return this._studentInfo;
    }

    set studentInfo(value: Array<StudentInfo>) {
        this._studentInfo = value;
    }
}



