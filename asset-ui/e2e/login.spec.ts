import { test, expect } from '@playwright/test'

test.describe('登录流程', () => {
  test('管理员正常登录', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'admin123')
    await page.click('[data-testid="login-btn"]')
    await expect(page).toHaveURL(/dashboard/)
    await expect(page.locator('.user-name')).toContainText('超级管理员')
  })

  test('密码错误提示', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'wrongpass')
    await page.click('[data-testid="login-btn"]')
    await expect(page.locator('.el-message--error')).toBeVisible()
  })
})
