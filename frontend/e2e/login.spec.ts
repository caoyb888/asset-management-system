import { test, expect } from '@playwright/test'

/**
 * 吸收实践: E2E自动化测试 - 登录核心流程
 */
test.describe('登录流程', () => {
  test('管理员成功登录', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'admin123')
    await page.click('[data-testid="login-btn"]')
    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page.locator('.user-info')).toBeVisible()
  })

  test('错误密码提示', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'wrong')
    await page.click('[data-testid="login-btn"]')
    await expect(page.locator('.el-message--error')).toBeVisible()
  })
})
