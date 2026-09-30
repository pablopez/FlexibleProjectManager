import { expect, test } from '@playwright/test'

test('fresh setup, login and authenticated application route', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveURL(/\/setup$/)

  const setup = page.getByRole('form', { name: 'First-run setup' })
  await setup.locator('input').nth(0).fill('E2E Organization')
  await setup.locator('input').nth(1).fill('E2E Installation')
  await setup.locator('input').nth(2).fill('e2e@example.test')
  await setup.locator('input').nth(3).fill('E2E Administrator')
  await setup.locator('input').nth(4).fill('password123')
  await setup.getByRole('button', { name: /initialize/i }).click()

  await expect(page).toHaveURL(/\/login$/)
  const login = page.getByRole('form', { name: 'Login' })
  await login.locator('input[type="email"]').fill('e2e@example.test')
  await login.locator('input[type="password"]').fill('password123')
  await login.getByRole('button', { name: /sign in/i }).click()

  await expect(page).toHaveURL(/\/app\/dashboard$/)
  await page.goto('/app/license')
  await expect(page).toHaveURL(/\/app\/license$/)
  await page.goto('/app/projects')
  await expect(page).toHaveURL(/\/app\/projects$/)
})
