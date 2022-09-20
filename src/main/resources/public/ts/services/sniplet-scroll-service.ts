import {Observable, Subject} from "rxjs";

export class SnipletScrollService {
    private scrollSubject: Subject<boolean> = new Subject<boolean>();


    sendScroll(init?: boolean): void {
        this.scrollSubject.next(init);
    }

    getScrollSubject(): Observable<boolean> {
        return this.scrollSubject.asObservable();
    }
}