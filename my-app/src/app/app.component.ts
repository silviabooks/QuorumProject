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
  private endpoint = 'http://localhost:8080/QuorumProject-war/gestione/log';
  data: any = {};

  constructor(private _http: Http) {
    this.getAllLogs();
  }

  private getAllLogs() {
    return this._http.get('http://localhost:8080/QuorumProject-war/gestione/log/get')
                .map((res: Response) => res.json())
                 .subscribe(data => {
                        this.data = data;
                        console.log(this.data);
                });
  }
}
