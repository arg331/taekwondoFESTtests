import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CursoArbitrajeResponse } from '../models/scraper.models';

@Injectable({ providedIn: 'root' })
export class ScraperService {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/scraper`;

  getCursosArbitraje(): Observable<CursoArbitrajeResponse[]> {
    return this.http.get<CursoArbitrajeResponse[]>(`${this.api}/arbitraje`);
  }
}
