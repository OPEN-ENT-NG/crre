import {moment} from "entcore";

export class ExportOrderFilter {
    startDate;
    endDate;
    searchingText: string;
    idsUser: Array<string>;
    idsCampaign: Array<number>;
    status: Array<string>;
    idsOrder: Array<number>;
    idsStructure: Array<string>;


    constructor() {
        this.startDate = moment().add(-6, 'months')._d;
        this.endDate = moment()._d;
        this.searchingText = "";
        this.idsUser = [];
        this.idsCampaign = [];
        this.idsOrder = [];
        this.status = [];
        this.idsStructure = [];
    }

}