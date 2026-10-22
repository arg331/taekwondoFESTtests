import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSliderHarness } from '@angular/material/slider/testing';
import { GradeRangeComponent } from './grade-range.component';

/** Barra de grados (RF-43): 11 posiciones, marca en cada cinturón y medio cinturón, nombre solo en los enteros. */
describe('GradeRangeComponent', () => {
  let fixture: ComponentFixture<GradeRangeComponent>;
  let el: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({ imports: [GradeRangeComponent] });
    fixture = TestBed.createComponent(GradeRangeComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('marca los 11 grados como una regla y nombra solo los cinturones enteros', () => {
    expect(el.querySelectorAll('.tick').length).toBe(11);
    expect(el.querySelectorAll('.tick.whole').length).toBe(6);
    expect(Array.from(el.querySelectorAll('.tick-label')).map(l => l.textContent?.trim()))
      .toEqual(['Blanco', 'Amarillo', 'Verde', 'Azul', 'Rojo', 'Negro']);
  });

  it('por defecto abarca de blanco a negro', () => {
    expect(fixture.componentInstance.range()).toEqual([0, 10]);
  });

  it('al soltar un extremo actualiza el rango', async () => {
    const slider = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSliderHarness);

    await (await slider.getStartThumb()).setValue(3);
    await (await slider.getEndThumb()).setValue(8);

    expect(fixture.componentInstance.range()).toEqual([3, 8]);
  });

  it('el valor se lee con el nombre del grado, también el de los medios cinturones', () => {
    expect(fixture.componentInstance.gradeLabel(3)).toBe('Amarillo-verde');
  });
});
