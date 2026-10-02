import { type Page, type Locator } from '@playwright/test';

/**
 * Page Object for the AI onboarding flow (route `/onboarding`, OnboardingComponent).
 * The flow is a linear signal machine: goal → busy (AI loader) → questions → /roadmap.
 * There is no back button or numeric progress bar; steps are not browser-history routes.
 */
export class OnboardingPage {
  readonly page: Page;
  readonly greeting: Locator;
  readonly goalTitle: Locator;
  readonly goalInput: Locator;
  readonly goalSubmit: Locator;
  readonly questions: Locator;
  readonly generateButton: Locator;
  readonly startFromZeroButton: Locator;
  readonly error: Locator;
  readonly busyLoader: Locator;
  readonly busyText: Locator;

  constructor(page: Page) {
    this.page = page;
    this.greeting = page.locator('.ob-eyebrow');
    this.goalTitle = page.locator('.ob-title');
    this.goalInput = page.locator('textarea.ob-input');
    this.goalSubmit = page.getByRole('button', { name: 'Design my path' });
    this.questions = page.locator('.ob-questions textarea');
    this.generateButton = page.getByRole('button', { name: 'Generate my roadmap' });
    this.startFromZeroButton = page.getByRole('button', { name: /Start all from 0/ });
    this.error = page.locator('.ob-error');
    this.busyLoader = page.locator('.ath-loader');
    this.busyText = page.locator('.ath-sub');
  }

  async goto(): Promise<void> {
    await this.page.goto('/onboarding');
  }

  async enterGoal(text: string): Promise<void> {
    await this.goalInput.fill(text);
  }

  async submitGoal(): Promise<void> {
    await this.goalSubmit.waitFor({ state: 'visible' });
    await this.goalSubmit.click({ force: true });
  }

  async answer(index: number, text: string): Promise<void> {
    await this.questions.nth(index).fill(text);
  }

  /** Fills every rendered question with the same answer. */
  async answerAll(text: string): Promise<void> {
    const count = await this.questions.count();
    for (let i = 0; i < count; i++) {
      await this.questions.nth(i).fill(text);
    }
  }

  async generate(): Promise<void> {
    await this.generateButton.waitFor({ state: 'visible' });
    await this.generateButton.click({ force: true });
  }

  async startFromZero(): Promise<void> {
    await this.startFromZeroButton.waitFor({ state: 'visible' });
    await this.startFromZeroButton.click({ force: true });
  }
}
