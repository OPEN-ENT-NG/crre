import {Mix, Selectable, Selection} from "entcore-toolkit";
import http from "axios";
import {toasts} from "entcore";

declare let window: any;

export class Quote implements Selectable {
    id: number;
    title: string;
    creation_date: string;
    owner_name: string;
    owner_id: string;
    quotation: any;
    selected: boolean;
    nb_structures: number;

    constructor() {
    }

    async generateCSV() {
        try {
            window.location = `/crre/quote/csv/${this.id}`;
        } catch (e) {
            toasts.warning('crre.quote.generate.csv.error');
        }
    };
}

export class Quotes extends Selection<Quote> {
    constructor() {
        super([]);
    }

    async get(page?:number) {
        try {
            const pageParams = (page) ? `?page=${page}` : ``;
            let {data} = await http.get(`/crre/quote${pageParams}`);
            return this.setQuotes(data);
        } catch (e) {
            toasts.warning('crre.quote.list.error');
        }
    };

    async search(name:string, page?:number) {
        try {
            const pageParams = (page) ? `?page=${page}` : ``;
            let {data} = await http.get(`/crre/quote/search?q=${name}${pageParams}`);
            return this.setQuotes(data);
        } catch (e) {
            toasts.warning('crre.quote.list.error');
        }
    };

    private setQuotes(data) {
        data.map(quote => {
            let date = new Date(quote.creation_date);
            quote.creation_date = date.toLocaleDateString().replace(/\//g, "-") + " - " +
                date.toLocaleTimeString('fr-FR');
        });
        this.all = this.all.concat(Mix.castArrayAs(Quote, data));
        return data.length > 0;
    }
}