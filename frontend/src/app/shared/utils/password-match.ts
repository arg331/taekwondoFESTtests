import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validador de grupo: la repetición tiene que coincidir con la contraseña.
 * El error se marca también en el campo de repetición para que su mat-error se vea allí.
 */
export function passwordMatchValidator(passwordKey: string, confirmKey: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const password = group.get(passwordKey);
    const confirm = group.get(confirmKey);
    if (!password || !confirm) return null;

    if (password.value !== confirm.value) {
      confirm.setErrors({ ...confirm.errors, passwordMismatch: true });
      return { passwordMismatch: true };
    }
    if (confirm.hasError('passwordMismatch')) {
      const { passwordMismatch: _, ...rest } = confirm.errors ?? {};
      confirm.setErrors(Object.keys(rest).length ? rest : null);
    }
    return null;
  };
}
