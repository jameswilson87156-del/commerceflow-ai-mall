import { createBrowserOidcClient } from '../../../shared/oidc'

export const mobileOidc = createBrowserOidcClient('commerceflow-mobile')
export const {
  isOidcAuthEnabled,
  isOidcConfigured,
  prepareOidcSession,
  getAccessToken,
  getAuthorizationHeader,
  isSignedIn,
  startOidcLogin,
  clearOidcSession,
  authChangeEvent
} = mobileOidc
