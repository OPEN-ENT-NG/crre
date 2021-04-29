import {Mix, Selectable, Selection} from "entcore-toolkit";
import http from "axios";
import {toasts} from "entcore";
import {OrdersRegion} from "./OrderRegion";
import {Utils} from "./Utils";
import {Filters} from "./Filter";

declare let window: any;

export class Project implements Selectable {
    id: number;
    title: string;
    creation_date: string;
    selected: boolean;
    orders: OrdersRegion;
    total: string;
    amount: number;
    campaign_name: string;
    uai: string;
    structure_name: string;

    constructor() {
    }
}

export class Projects extends Selection<Project> {
    constructor() {
        super([]);
    }

    async get(old = false, start: string, end: string,filterRejectedSentOrders: boolean, pageNumber: number, idStructure: string) {
        try {
            const {startDate, endDate} = Utils.formatDate(start, end);
            const page: string = pageNumber ? `page=${pageNumber}&` : '';
            let url = `/crre/orderRegion/projects?${page}startDate=${startDate}&endDate=${endDate}&old=${old}&idStructure=${idStructure}`;
            url += `&filterRejectedSentOrders=${filterRejectedSentOrders}`;
            let { data } = await http.get(url);
            this.all =  Mix.castArrayAs(Project, data);
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async filter_order(old = false, query_name:string, filters: Filters, start: string, end: string, pageNumber: number, idStructure: string) {
        function prepareParams() {
            let params = "";
            const word = query_name;
            filters.all.forEach(function (f) {
                params += f.name + "=" + f.value + "&";
            });
            const {startDate, endDate} = Utils.formatDate(start, end);
            const page: string = `page=${pageNumber}`;
            params += page;
            return {params, word, startDate, endDate};
        }

        try {
            let {params, word, startDate, endDate} = prepareParams();
            if (!Utils.format.test(word)) {
                const wordParams =  (!!word) ? `&q=${word}` : ``
                let url = `/crre/ordersRegion/projects/search_filter`;
                url += `?startDate=${startDate}&endDate=${endDate}&old=${old}&idStructure=${idStructure}&${params}${wordParams}`;
                const {data} = await http.get(url);
                this.all = Mix.castArrayAs(Project, data);
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        }
    }

    exportCSV = (old = false) => {
        let selectedOrders = this.extractSelectedOrders();
        let params_id_order = Utils.formatKeyToParameter(selectedOrders.all, 'id');
        let params_id_equipment = Utils.formatKeyToParameter(selectedOrders.all, "equipment_key");
        let params_id_structure = Utils.formatKeyToParameter(selectedOrders.all, "id_structure");
        window.location = `/crre/region/orders/exports?${params_id_order}&${params_id_equipment}&${params_id_structure}&old=${old}`;
    }

    private extractSelectedOrders(select: boolean = false) {
        let selectedOrders = new OrdersRegion();
        let allOrders = new OrdersRegion();
        this.all.forEach(project => {
            project.orders.forEach(order => {
                allOrders.all.push(order);
                if(order.selected){selectedOrders.all.push(order);}
            });
        });
        return (selectedOrders.length == 0 && !select) ? allOrders : selectedOrders;
    }
}