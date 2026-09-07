import { createBrowserOidcClient } from '../../shared/oidc'

export const adminOidc = createBrowserOidcClient('commerceflow-admin')
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
} = adminOidc
