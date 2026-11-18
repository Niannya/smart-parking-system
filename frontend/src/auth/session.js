import { readonly, ref } from 'vue'
import { getCurrentAccount, loginAccount, logoutAccount } from '../api/auth'

const account = ref(null)
export const currentUser = readonly(account)

let initialized = false
let restoreRequest = null

function setAccount(user) {
  account.value = user
  initialized = true
}

export function clearSession() {
  setAccount(null)
}

export async function restoreSession() {
  if (initialized) return account.value
  if (!restoreRequest) {
    restoreRequest = getCurrentAccount()
      .then((user) => {
        setAccount(user)
        return user
      })
      .catch((error) => {
        if (error.status !== 401) throw error
        clearSession()
        return null
      })
      .finally(() => { restoreRequest = null })
  }
  return restoreRequest
}

export async function signIn(credentials) {
  const user = await loginAccount(credentials)
  setAccount(user)
  return user
}

export async function signOut() {
  await logoutAccount()
  clearSession()
}
