import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'

afterEach(() => vi.unstubAllGlobals())

describe('application navigation', () => {
  it('opens the real AI customer service workbench from the sidebar', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(JSON.stringify([]), { status: 200 }))))
    const wrapper = mount(App)
    const aiButton = wrapper.findAll('button').find((button) => button.text().includes('AI 客服'))
    expect(aiButton).toBeDefined()
    await aiButton?.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('AI 商品客服')
    expect(wrapper.find('[data-testid="ai-catalog-empty"]').exists()).toBe(true)
  })
})
