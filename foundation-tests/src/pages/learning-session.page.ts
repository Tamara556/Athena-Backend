import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Page Object for `/learning/current` and `/learning/:id` (DailyLearningSessionComponent).
 */
export class LearningSessionPage {
  readonly page: Page;
  readonly loader: Locator;
  readonly errorState: Locator;
  readonly emptyState: Locator;
  readonly title: Locator;
  readonly statusText: Locator;
  readonly startCta: Locator;
  readonly readingStage: Locator;
  readonly watchingStage: Locator;
  readonly practiceStage: Locator;
  readonly quizStage: Locator;
  readonly markReadCheckbox: Locator;
  readonly markReadingComplete: Locator;
  readonly markWatchingComplete: Locator;
  readonly markPracticeComplete: Locator;
  readonly quizNext: Locator;
  readonly finishSession: Locator;
  readonly completionOverlay: Locator;
  readonly returnToRoadmap: Locator;
  readonly backToRoadmap: Locator;

  constructor(page: Page) {
    this.page = page;
    this.loader = page.locator('app-athena-loader');
    this.errorState = page.locator('app-empty-state').filter({ hasText: 'Something went wrong' });
    this.emptyState = page.locator('app-empty-state').filter({ hasText: 'No session yet' });
    this.title = page.locator('#topicTitle');
    this.statusText = page.locator('.ring-status');
    this.startCta = page.getByRole('button', { name: 'Start Learning' });
    this.readingStage = page.locator('.stage[aria-label="Reading stage"]');
    this.watchingStage = page.locator('.stage[aria-label="Watching stage"]');
    this.practiceStage = page.locator('.stage[aria-label="Practice stage"]');
    this.quizStage = page.locator('.stage[aria-label="Quiz stage"]');
    this.markReadCheckbox = page.locator('.stage.active label.checkbox input[type="checkbox"]');
    this.markReadingComplete = page.getByRole('button', { name: /Mark reading complete/ });
    this.markWatchingComplete = page.getByRole('button', { name: /Mark watching complete/ });
    this.markPracticeComplete = page.getByRole('button', { name: /Mark practice complete/ });
    this.quizNext = page.getByRole('button', { name: /Submit quiz|Next/ });
    this.finishSession = page.getByRole('button', { name: /Finish today's session/ });
    this.completionOverlay = page.locator('.done-overlay');
    this.returnToRoadmap = page.getByRole('button', { name: 'Return to Roadmap' });
    this.backToRoadmap = page.getByRole('link', { name: 'Roadmap' }).first();
  }

  async gotoCurrent(): Promise<void> {
    await this.page.goto('/learning/current');
  }

  async gotoById(id: string): Promise<void> {
    await this.page.goto(`/learning/${id}`);
  }

  quizOption(label: string): Locator {
    return this.page.locator('button.opt', { hasText: label });
  }

  /** Click the label so Angular's `(change)="toggle"` runs (native force-check can miss it). */
  async markCurrentReading(): Promise<void> {
    await this.readingStage.locator('label.checkbox').first().click({ force: true });
    await expect(this.markReadingComplete).toBeEnabled();
  }

  /**
   * Walks the four real stages (read → watch → practice → quiz) and finishes
   * the session. Stage switches are driven by the component's own 420ms timer.
   */
  async completeAllStages(): Promise<void> {
    await expect(this.title).toBeVisible();
    if (await this.startCta.isVisible().catch(() => false)) {
      await this.startCta.click({ force: true });
    }
    await this.markCurrentReading();
    await expect(this.markReadingComplete).toBeEnabled();
    await this.markReadingComplete.click({ force: true });
    await expect(this.watchingStage).toHaveClass(/active/);
    await this.markWatchingComplete.click({ force: true });
    await expect(this.practiceStage).toHaveClass(/active/);
    await this.markPracticeComplete.click({ force: true });
    await expect(this.quizStage).toHaveClass(/active/);
    const opt = this.quizStage.locator('button.opt').first();
    await expect(opt).toBeVisible();
    await opt.click({ force: true });
    await expect(this.quizNext).toBeVisible();
    await this.quizNext.click({ force: true });
    await expect(this.finishSession).toBeVisible();
    await this.finishSession.click({ force: true });
  }
}
