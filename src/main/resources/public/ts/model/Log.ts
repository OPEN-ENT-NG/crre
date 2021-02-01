import http from 'axios';
import { toasts } from 'entcore';
import { Mix } from 'entcore-toolkit';

export class Log {
    id: number;
    date: string;
    action: string;
    context: string;
    value: any;
    id_user: string;
    username: string;
    item: string;
    selected: boolean;
}

export class Logs {
    all: Log[];
    numberOfPages: number;

    constructor () {
        this.all = [];
    }

    async loadPage (pageNumber: number = 1) {
        try {
            let { data } = await http.get(`/crre/logs?page=${--pageNumber}`);
            this.all = Mix.castArrayAs(Log, data.logs);
            this.numberOfPages = Math.floor(data.number_logs / 100) + 1;
            this.all.map((log) => log.selected = false);
        } catch (e) {
            toasts.warning('crre.logs.sync.err');
        }
    }

    export () {
        location.replace(`/crre/logs/export`);
    }

    reset () {
        this.all = [];
    }
}