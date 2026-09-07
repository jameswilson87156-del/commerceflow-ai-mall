export * from 'vue/dist/vue.runtime.esm-bundler.js'

// Newer uni-app H5 builds also consume this lifecycle primitive from their
// own Vue runtime. It is intentionally not part of Vue's public entry.
export { injectHook } from '@dcloudio/uni-h5-vue'

// Uni's current alpha build imports this internal flag from Vue's public entry.
export const isInSSRComponentSetup = false
