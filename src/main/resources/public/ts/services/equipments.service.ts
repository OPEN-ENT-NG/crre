import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';
import {Catalog, ICatalogResponse} from "../model/Catalog";


export interface IEquipmentsService {
    getFilters(): Promise<Catalog>;
}

export const equipmentsService: IEquipmentsService = {
    getFilters: async (): Promise<Catalog> => {
        return http.get(`/crre/equipments/catalog/filters`)
            .then((res: AxiosResponse) => new Catalog().build(res.data));
    }
};

export const EquipmentsService = ng.service('EquipmentsService', (): IEquipmentsService => equipmentsService);