import http from "axios";
import {idiom as lang, toasts} from 'entcore';
import {Mix} from "entcore-toolkit";

const fieldToI18nMap = new Map<string, any[]>([
    ["seconde", ["crre.second.class", 0]],
    ["premiere", ["crre.premiere.class", 1]],
    ["terminale", ["crre.terminal.class", 2]],
    ["secondepro", ["crre.second.pro.class", 3]],
    ["premierepro", ["crre.premiere.pro.class", 4]],
    ["terminalepro", ["crre.terminal.pro.class", 5]],
    ["secondetechno", ["crre.second.tech.class", 6]],
    ["premieretechno", ["crre.premiere.tech.class", 7]],
    ["terminaletechno", ["crre.terminal.tech.class", 8]],
    ["cap1", ["crre.first.cap.class", 9]],
    ["cap2", ["crre.second.cap.class", 10]],
    ["cap3", ["crre.third.cap.class", 11]],
    ["bma1", ["crre.first.bma.class", 12]],
    ["bma2", ["crre.second.bma.class", 13]]
]);

export interface StudentInfo {
    title: string
    value: number
    position: number
}

export class Student {
    id_structure: string;
    seconde: number;
    premiere: number;
    terminale: number;
    secondepro: number;
    premierepro: number;
    terminalepro: number;
    secondetechno: number;
    premieretechno: number;
    terminaletechno: number;
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
                return {title: lang.translate(fieldToI18nMap.get(key)[0]), value: this[key], position: fieldToI18nMap.get(key)[1]};
            });
    }

    get studentInfo(): Array<StudentInfo> {
        return this._studentInfo;
    }

    set studentInfo(value: Array<StudentInfo>) {
        this._studentInfo = value;
    }
}



