import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClubService } from './club.service';

const API = 'http://localhost:8080/api';

/** Contrato HTTP de los clubes (RF-38 a RF-42). */
describe('ClubService', () => {
  let clubs: ClubService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    clubs = TestBed.inject(ClubService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function expectCall(method: string, url: string, body?: unknown): void {
    const req = http.expectOne(r => r.urlWithParams === url);
    expect(req.request.method).toBe(method);
    if (body !== undefined) expect(req.request.body).toEqual(body);
    req.flush(null);
  }

  it('listar, crear, renombrar y borrar', () => {
    clubs.list().subscribe();
    expectCall('GET', `${API}/clubs`);
    clubs.create('Club Badajoz').subscribe();
    expectCall('POST', `${API}/clubs`, { name: 'Club Badajoz' });
    clubs.rename(3, 'Otro').subscribe();
    expectCall('PATCH', `${API}/clubs/3`, { name: 'Otro' });
    clubs.delete(3).subscribe();
    expectCall('DELETE', `${API}/clubs/3`);
  });

  it('miembros, candidatos y el propio club', () => {
    clubs.members(3).subscribe();
    expectCall('GET', `${API}/clubs/3/members`);
    clubs.candidates(3, 'ana').subscribe();
    expectCall('GET', `${API}/clubs/3/candidates?q=ana`);
    clubs.addMember(3, 9).subscribe();
    expectCall('PUT', `${API}/clubs/3/members/9`);
    clubs.removeMember(3, 9).subscribe();
    expectCall('DELETE', `${API}/clubs/3/members/9`);
    clubs.chooseMine(null).subscribe();
    expectCall('PUT', `${API}/users/me/club`, { clubId: null });
  });
});
