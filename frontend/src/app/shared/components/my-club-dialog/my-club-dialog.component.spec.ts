import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MyClubDialogComponent } from './my-club-dialog.component';
import { AuthService } from '../../../core/services/auth.service';
import { UserResponse } from '../../../core/models/auth.models';

const API = 'http://localhost:8080/api';
const ME: UserResponse = {
  id: 5, username: 'ana', email: 'ana@x.es', displayName: 'Ana', role: 'STUDENT',
  club: { id: 1, name: 'Club Ávila' }, active: true, createdAt: '2026-10-13T10:00:00'
};
const CLUBS = [{ id: 1, name: 'Club Ávila' }, { id: 2, name: 'Club Badajoz' }];

/** Elegir el propio club desde el menú de usuario (RF-41). */
describe('MyClubDialogComponent', () => {
  let fixture: ComponentFixture<MyClubDialogComponent>;
  let http: HttpTestingController;
  let close: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    localStorage.setItem('fest_token', 't');
    localStorage.setItem('fest_user', JSON.stringify(ME));
    close = vi.fn();
    TestBed.configureTestingModule({
      imports: [MyClubDialogComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: MatDialogRef, useValue: { close } }]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyClubDialogComponent);
    await stable();
    http.expectOne(`${API}/clubs`).flush(CLUBS);
    await stable();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function saveButton(): HTMLButtonElement {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find(b => b.textContent?.includes('Guardar')) as HTMLButtonElement;
  }

  async function select(): Promise<MatSelectHarness> {
    return TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
  }

  it('parte del club actual', async () => {
    expect(await (await select()).getValueText()).toBe('Club Ávila');
  });

  it('cambiar de club lo guarda y actualiza la sesión', async () => {
    await (await select()).clickOptions({ text: 'Club Badajoz' });
    saveButton().click();

    const req = http.expectOne(`${API}/users/me/club`);
    expect(req.request.body).toEqual({ clubId: 2 });
    req.flush({ ...ME, club: CLUBS[1] });
    await stable();

    expect(TestBed.inject(AuthService).currentUser()?.club?.name).toBe('Club Badajoz');
    expect(JSON.parse(localStorage.getItem('fest_user')!).club.name).toBe('Club Badajoz');
    expect(close).toHaveBeenCalled();
  });

  it('«Sin club» deja el club', async () => {
    await (await select()).clickOptions({ text: 'Sin club' });
    saveButton().click();

    const req = http.expectOne(`${API}/users/me/club`);
    expect(req.request.body).toEqual({ clubId: null });
    req.flush({ ...ME, club: null });
    await stable();

    expect(TestBed.inject(AuthService).currentUser()?.club).toBeNull();
  });
});
