import { type Page, type Locator, expect } from '@playwright/test';
import { type TestUser } from '../helpers/auth-helper';

/**
 * Page Object for the registration page (route `/register`, RegisterComponent).
 * Anchored to stable ids in
 * Athena-Frontend/src/app/pages/register/register.component.html.
 */
export class RegisterPage {
  readonly page: Page;
  readonly firstName: Locator;
  readonly lastName: Locator;
  readonly email: Locator;
  readonly username: Locator;
  readonly password: Locator;
  readonly confirm: Locator;
  readonly terms: Locator;
  readonly termsBox: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;
  readonly successAlert: Locator;
  readonly emailMsg: Locator;
  readonly confirmMsg: Locator;

  constructor(page: Page) {
    this.page = page;
    this.firstName = page.locator('#firstName');
    this.lastName = page.locator('#lastName');
    this.email = page.locator('#email');
    this.username = page.locator('#username');
    this.password = page.locator('#password');
    this.confirm = page.locator('#confirm');
    this.terms = page.locator('#terms');
    // The real click target: the styled box in the terms label toggles the checkbox.
    this.termsBox = page.locator('label.terms .box');
    this.submitButton = page.locator('form button[type="submit"]');
    this.errorAlert = page.locator('.alert.error');
    this.successAlert = page.locator('.alert.success');
    this.emailMsg = page.locator('#emailMsg');
    this.confirmMsg = page.locator('#confirmMsg');
  }

  async goto(): Promise<void> {
    await this.page.goto('/register');
  }

  /** Fills every field (confirm mirrors password unless overridden). */
  async fillForm(user: TestUser, confirmPassword?: string): Promise<void> {
    await this.firstName.fill(user.firstName);
    await this.lastName.fill(user.lastName);
    await this.email.fill(user.email);
    await this.username.fill(user.username);
    await this.password.fill(user.password);
    await this.confirm.fill(confirmPassword ?? user.password);
  }

  async acceptTerms(): Promise<void> {
    // The checkbox is visually hidden; users toggle it via the styled box in the
    // label. Clicking the box (native label behaviour) checks the input + fires change.
    await this.termsBox.click();
    await expect(this.terms).toBeChecked();
  }

  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /** Fill valid form, accept terms, submit. */
  async register(user: TestUser): Promise<void> {
    await this.fillForm(user);
    await this.acceptTerms();
    await this.submit();
  }
}
