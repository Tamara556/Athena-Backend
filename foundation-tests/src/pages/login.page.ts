import { type Page, type Locator } from '@playwright/test';

/**
 * Page Object for the login page (route `/login`, LoginComponent).
 * The primary submit label is dynamic, so the submit control is located
 * structurally within the form that owns the credential inputs.
 */
export class LoginPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly loginInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly createAccountLink: Locator;
  readonly errorAlert: Locator;
  readonly successAlert: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Welcome back' });
    this.loginInput = page.locator('#login');
    this.passwordInput = page.locator('#password');
    this.submitButton = page.locator('form:has(#password) button[type="submit"]');
    this.createAccountLink = page.getByRole('link', { name: 'Create account' });
    this.errorAlert = page.locator('.alert.error');
    this.successAlert = page.locator('.alert.success');
  }

  /** Fill credentials and submit in one step. */
  async login(login: string, password: string): Promise<void> {
    await this.fillCredentials(login, password);
    await this.submit();
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
  }

  async fillCredentials(login: string, password: string): Promise<void> {
    await this.loginInput.fill(login);
    await this.passwordInput.fill(password);
  }

  async submit(): Promise<void> {
    await this.submitButton.waitFor({ state: 'visible' });
    await this.submitButton.click({ force: true });
  }
}
