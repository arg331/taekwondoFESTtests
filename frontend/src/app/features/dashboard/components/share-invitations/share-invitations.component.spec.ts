import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ShareInvitationsComponent } from './share-invitations.component';
import { InvitationResponse } from '../../../../core/models/share.models';

const API = 'http://localhost:8080/api';

const INVITATIONS: InvitationResponse[] = [
  { id: 3, examId: 7, examTitle: 'Examen de Árbitros 2026', invitedBy: 'Ana Pérez', createdAt: '2026-10-21T10:00:00' },
  { id: 4, examId: 8, examTitle: 'Examen tipo C', invitedBy: 'Luis Gil', createdAt: '2026-10-21T09:00:00' }
];

/** Invitaciones a exámenes compartidos en el panel del profesor (RF-51). */
describe('ShareInvitationsComponent', () => {
  let fixture: ComponentFixture<ShareInvitationsComponent>;
  let http: HttpTestingController;
  let el: HTMLElement;

  async function render(invitations: InvitationResponse[] = INVITATIONS): Promise<void> {
    TestBed.configureTestingModule({
      imports: [ShareInvitationsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ShareInvitationsComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne(`${API}/shares/invitations`).flush(invitations);
    await stable();
  }

  async function stable(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function items(): HTMLElement[] {
    return Array.from(el.querySelectorAll('.invitation'));
  }

  function button(text: string, root: ParentNode): HTMLButtonElement {
    return Array.from(root.querySelectorAll('button')).find(b => b.textContent?.includes(text)) as HTMLButtonElement;
  }

  afterEach(() => http.verify());

  it('dice quién quiere compartir qué examen, con «Aceptar» y «Rechazar»', async () => {
    await render();

    expect(items().map(i => i.querySelector('.invitation-text')?.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      'Ana Pérez quiere compartir contigo el examen: Examen de Árbitros 2026',
      'Luis Gil quiere compartir contigo el examen: Examen tipo C'
    ]);
    expect(button('Aceptar', items()[0])).toBeTruthy();
    expect(button('Rechazar', items()[0])).toBeTruthy();
  });

  it('al aceptar desaparece y lleva al examen en «Exámenes»', async () => {
    await render();

    button('Aceptar', items()[0]).click();
    const req = http.expectOne(`${API}/shares/invitations/3/accept`);
    expect(req.request.method).toBe('POST');
    req.flush({});
    await stable();

    expect(items().length).toBe(1);
    const link = el.querySelector('.accepted-note a') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/exams?select=7');
  });

  it('al rechazar desaparece', async () => {
    await render();

    button('Rechazar', items()[1]).click();
    const req = http.expectOne(`${API}/shares/invitations/4`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    await stable();

    expect(items().map(i => i.querySelector('.invitation-text')?.textContent)).toHaveLength(1);
  });

  it('sin invitaciones no muestra nada', async () => {
    await render([]);

    expect(el.querySelector('.invitations')).toBeNull();
  });
});
