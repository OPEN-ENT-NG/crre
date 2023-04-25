import {Mix, Selectable, Selection} from "entcore-toolkit";
import http from "axios";
import {moment, toasts} from "entcore";
import {OrdersRegion} from "./OrderRegion";
import {Utils} from "./Utils";
import {Filters} from "./Filter";
import {ProjectFilter} from "./ProjectFilter";
import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";
import {ORDER_BY_PROJECT_FIELD_ENUM} from "../enum/order-by-project-field-enum";

declare let window: any;

export interface IProjectPayload {
    startDate: String;
    endDate: String;
    page?: Number;
    status?: Array<String>;
    idsStructure?: Array<String>;
    idsCampaign?: Array<Number>;
    distributors?: Array<String>;
    editors?: Array<String>;
    itemTypes?: Array<String>;
    catalogs?: Array<String>;
    structureTypes?: Array<String>;
    renew?: Array<String>;
    searchingText?: String;
    idsProject?: Array<Number>;
    orderBy?: ORDER_BY_PROJECT_FIELD_ENUM;
    orderDesc?: boolean;
}



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
    expanded: boolean;
    status: string;

    constructor() {
    }
}

export class Projects extends Selection<Project> {
    constructor() {
        super([]);
    }

    async get(old = false, start: string, end: string, filterRejectedSentOrders: boolean, pageNumber: number, idStructure: string) {
        try {
            const {startDate, endDate} = Utils.formatDate(start, end);
            const page: string = pageNumber ? `page=${pageNumber}&` : '';
            let url = `/crre/orderRegion/projects?${page}startDate=${startDate}&endDate=${endDate}&old=${old}&idStructure=${idStructure}`;
            url += `&filterRejectedSentOrders=${filterRejectedSentOrders}`;
            let {data} = await http.get(url);
            this.all = Mix.castArrayAs(Project, data);
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async search(filter: ProjectFilter): Promise<Project[]> {
        if (filter.queryName != null && filter.queryName.trim() !== "") {
            if (Utils.format.test(filter.queryName)) {
                toasts.warning('crre.equipment.special');
                return;
            }
        }

        let url = `/crre/ordersRegion/projects`;
        const {data} = await http.post(url, this.toJSON(filter));
        this.all = Mix.castArrayAs(Project, data)
        return this.all;
    }

    toJSON(filter: ProjectFilter): IProjectPayload {

        let payload: IProjectPayload = {
            startDate: moment(filter.startDate).format('YYYY-MM-DD').toString(),
            endDate: moment(filter.endDate).format('YYYY-MM-DD').toString()
        };

        if (filter.page != null) payload.page = filter.page;
        if (!!filter.queryName) payload.searchingText = filter.queryName;
        if (filter.statusFilterList.length > 0) payload.status = filter.statusFilterList.map(status => ORDER_STATUS_ENUM[status.orderStatusEnum]);
        if (filter.structureList.length > 0) payload.idsStructure = filter.structureList.map(structure => structure.id);
        if (filter.campaignList.length > 0) payload.idsCampaign = filter.campaignList.map(campaign => campaign.id);
        if (filter.distributorList.length > 0) payload.distributors = filter.distributorList.map(distributor => distributor.name);
        if (filter.editorList.length > 0) payload.editors = filter.editorList.map(editor => editor.name);
        if (filter.schoolType.length > 0) payload.structureTypes = filter.schoolType.map(schoolType => schoolType.name);
        if (filter.catalogList.length > 0) payload.catalogs = filter.catalogList.map(catalog => catalog.name);
        if (filter.renew.length > 0) payload.renew = filter.renew.map(renew => renew.name);
        if (filter.orderDesc != null && filter.orderBy != null) {
            payload.orderBy = filter.orderBy;
            payload.orderDesc = filter.orderDesc;
        }

        return payload;
    }

    /**
     * @deprecated Use {@link search}
     */
    async filter_order(old = false, query_name: string, filters: Filters, start: string, end: string, pageNumber: number, idStructure: string) {
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
                const wordParams = (!!word) ? `&q=${word}` : ``
                let url = `/crre/ordersRegion/projects/search_filter`;
                url += `?startDate=${startDate}&endDate=${endDate}&old=${old}&${params}${wordParams}`;
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

    public exportCSV = async (all?: boolean) => {
        let selectedOrders;
        if (all) {
            selectedOrders = this.extractAllOrders();
        } else {
            selectedOrders = this.extractSelectedOrders();
        }
        let params_id_equipment = new Set();
        let params_id_structure = new Set();
        let params_id_order = selectedOrders.all.map(order => order.id);
        selectedOrders.all.forEach(order => params_id_equipment.add(order.equipment_key));
        selectedOrders.all.forEach(order => params_id_structure.add(order.id_structure));

        let data = {
            idsStructures: params_id_structure,
            idsEquipments: params_id_equipment,
            idsOrders: params_id_order
        };
        const response = await http.post(`/crre/region/orders/exports`, data);
        if(response.status == 200) {
            if (params_id_order.length > 1000) {
                toasts.confirm('crre.export.order.region.success');
            } else {
                let blob = new Blob([response.data], {type: ' type: "text/csv;charset=UTF-8"'});
                let link = document.createElement('a');
                link.href = (window as any).URL.createObjectURL(blob);
                link.download = "orders.csv"
                document.body.appendChild(link);
                link.click();
            }
        } else {
           toasts.warning('crre.export.order.region.error')
        }
    }

    extractAllOrders() {
        let allOrders = new OrdersRegion();
        this.all.forEach(project => {
            project.orders.forEach(order => {
                allOrders.all.push(order);
            });
        });
        return allOrders;
    }

    extractSelectedOrders(select: boolean = false) {
        let selectedOrders = new OrdersRegion();
        let allOrders = new OrdersRegion();
        this.all.forEach(project => {
            project.orders.forEach(order => {
                allOrders.all.push(order);
                if (order.selected) {
                    selectedOrders.all.push(order);
                }
            });
        });
        return (selectedOrders.length == 0 && !select) ? allOrders : selectedOrders;
    }

    public hasSelectedOrders() {
        let isSelected = false;
        this.all.forEach(project => {
            project.orders.forEach(order => {
                if (order.selected) {
                    isSelected = true;
                }
            });
        });
        return isSelected
    }
}