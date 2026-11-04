import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8180",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "demo",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "demo-client"
});

const redirectUri = import.meta.env.VITE_KEYCLOAK_REDIRECT_URI
  || `${window.location.origin}/`;

let initialized = false;

export async function initializeAuth() {
  if (initialized) return keycloak.authenticated;

  const initialization = keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    checkLoginIframe: false,
    silentCheckSsoFallback: false,
    redirectUri
  });
  const timeout = new Promise((_, reject) => {
    window.setTimeout(
      () => reject(new Error(
        "Keycloak did not respond within 8 seconds")),
      8000
    );
  });
  const authenticated = await Promise.race([initialization, timeout]);
  initialized = true;
  return authenticated;
}

export function login() {
  return keycloak.login({
    redirectUri
  });
}

export function logout() {
  return keycloak.logout({
    redirectUri
  });
}

export function getRedirectUri() {
  return redirectUri;
}

export async function getAccessToken() {
  if (!keycloak.authenticated) return null;
  await keycloak.updateToken(30);
  return keycloak.token;
}

export function getUserProfile() {
  const roles = keycloak.tokenParsed?.realm_access?.roles || [];
  return {
    username: keycloak.tokenParsed?.preferred_username || "Administrator",
    name: keycloak.tokenParsed?.name || keycloak.tokenParsed?.preferred_username || "Administrator",
    isAdmin: roles.includes("ADMIN") || roles.includes("admin"),
  };
}
