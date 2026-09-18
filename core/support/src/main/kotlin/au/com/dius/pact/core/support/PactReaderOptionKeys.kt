package au.com.dius.pact.core.support

/**
 * Keys used in the untyped options map that is passed to `PactReader.loadPact` and on to the HTTP clients that
 * fetch pact files.
 *
 * These are defined here (rather than as literals at each use site) because the map is populated in one module
 * (for instance the JUnit `PactBrokerLoader`) and consumed in another (`PactReader`), so a typo in either place
 * silently drops the setting.
 */
object PactReaderOptionKeys {
  /**
   * Authentication to use when fetching pacts. Either an [Auth] value or the legacy list form.
   */
  const val AUTHENTICATION = "authentication"

  /**
   * Boolean controlling whether TLS errors are ignored when fetching pacts.
   */
  const val INSECURE_TLS = "insecureTLS"

  /**
   * Additional HTTP headers (`Map<String, String>`) to send with every request made while fetching pacts.
   */
  const val CUSTOM_HEADERS = "customHeaders"
}
