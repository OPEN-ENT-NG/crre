import http from "axios";
import {idiom as lang, moment, notify} from "entcore";
import {Mix, Selectable, Selection} from "entcore-toolkit";
import {Utils} from "./Utils";

export class Export implements Selectable {
    selected: boolean;
    filename: string;
    fileId: string;
    _id: string;
    typeObject: string;
    extension:string
    status: STATUS;
    created?;
    object_name: string;
    constructor(){
        this.status = STATUS.WAITING;
    }
}

export class Exports extends Selection<Export> {
    async getExports() {
        try {
            let {data} = await http.get(`/crre/exports`);
            let response = data.map( exportResponse => {
                let exportEdit = {
                    ...exportResponse};
                switch(exportResponse.status) {
                    case STATUS.WAITING:
                        exportEdit.classStatus =  "disableRow";
                        exportEdit.tooltip = lang.translate("crre.export.waiting");
                        break;
                    case STATUS.SUCCESS:
                        exportEdit.classStatus =  "successRow";
                        exportEdit.tooltip = lang.translate("crre.export.success");
                        break;
                    default:
                        exportEdit.classStatus = "errorRow";
                        exportEdit.tooltip = lang.translate("crre.export.error");
                }
                exportEdit.created = moment(exportResponse.created).format("YYYY-MM-DD HH:mm:ss");
                if(!exportEdit.typeObject)
                    exportEdit.typeObject = "INSTRUCTION";
                if(!exportEdit.extension)
                    exportEdit.extension = "xlsx"
                return exportEdit;
            });
            this.all = Mix.castArrayAs(Export, response);
        } catch (e) {
            notify.error('crre.instruction.create.err');
            throw e;
        }
    }

    async delete(idsExports: Array<number>, idsFiles: Array<number>):Promise<void> {
        try {
            const bodySend = {
                idsExport: idsExports,
                idsFiles: idsFiles,
            };
            await http.delete('/crre/exports', { data: bodySend });
        } catch (e) {
            throw notify.error('crre.export.delete.err');
        }
    }
}

export enum STATUS  {
    WAITING = "WAITING",
    SUCCESS = "SUCCESS",
    ERROR = "ERROR",
}