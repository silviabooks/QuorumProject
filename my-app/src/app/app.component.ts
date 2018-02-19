import { Component } from '@angular/core';
import { Http, Response, Headers } from '@angular/http';
import 'rxjs/add/operator/map';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'QuorumProject';
  private endpoint = 'http://localhost:8080/QuorumProject-war/gestione/log/';
  data: any = {};
  resultTitle = '';

  constructor(private _http: Http) {
    // this.getAllLogs();
    // this.getLastEvent();
  }

  public getAllLogs() {
    return this._http.get(this.endpoint + 'get')
                .map((res: Response) => res.json())
                 .subscribe(data => {
                        this.data = data;
                        this.resultTitle = 'All logs';
                        console.log(this.data);
                });
  }

  public getLastEvent() {
    return this._http.get(this.endpoint + 'getLast')
                .map((res: Response) => res.json())
                 .subscribe(data => {
                        this.data = data;
                        this.resultTitle = 'Last event';
                        console.log(this.data);
                });
  }
}
