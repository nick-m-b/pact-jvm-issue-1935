package au.com.dius.pact.core.support

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider
import org.apache.hc.client5.http.impl.classic.HttpRequestRetryExec
import org.apache.hc.client5.http.impl.classic.MainClientExec
import org.apache.hc.client5.http.protocol.RequestDefaultHeaders
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.Method
import spock.lang.Issue
import spock.lang.Specification

class HttpClientSpec extends Specification {

  /**
   * The default headers are only reachable by walking the client's exec chain to the interceptor that applies them.
   */
  private static defaultHeadersOf(client) {
    def defaultHeaders = null
    def execChain = client.execChain
    while (defaultHeaders == null && execChain != null) {
      if (execChain.handler instanceof MainClientExec) {
        def interceptor = execChain.handler.httpProcessor.requestInterceptors.find {
          it instanceof RequestDefaultHeaders
        }
        defaultHeaders = interceptor.defaultHeaders
      } else {
        execChain = execChain.next
      }
    }
    defaultHeaders
  }

  def 'when creating a new http client, add any authentication as default headers'() {
    given:
    URI uri = new URI('http://localhost')
    def authentication = ['bearer', '1234abcd']

    when:
    def result = HttpClient.INSTANCE.newHttpClient(authentication, uri, 1, 1, false)
    def defaultHeaders = defaultHeadersOf(result.component1())

    then:
    defaultHeaders[0].name == 'Authorization'
    defaultHeaders[0].value == 'Bearer 1234abcd'
  }

  @Issue('#1935')
  def 'when creating a http client from the options, adds every custom header alongside the authentication'() {
    given:
    URI uri = new URI('http://localhost')
    def options = [
      authentication: ['bearer', '1234abcd'],
      customHeaders: [
        'X-Pact-Broker-Client-Id': 'expected-id',
        'X-Pact-Broker-Client-Secret': 'expected-secret'
      ]
    ]

    when:
    def result = HttpClient.INSTANCE.newHttpClientFromOptions(uri, options)
    def defaultHeaders = defaultHeadersOf(result.component1()).collectEntries { [it.name, it.value] }

    then:
    defaultHeaders['X-Pact-Broker-Client-Id'] == 'expected-id'
    defaultHeaders['X-Pact-Broker-Client-Secret'] == 'expected-secret'
    defaultHeaders['Authorization'] == 'Bearer 1234abcd'
  }

  @Issue('#1935')
  def 'when creating a http client from the options, sets up authentication from the options'() {
    given:
    URI uri = new URI('http://localhost')
    def authScope = new AuthScope(uri.host, uri.port)
    def options = [authentication: ['basic', 'user', 'pwd']]

    when:
    def client = HttpClient.INSTANCE.newHttpClientFromOptions(uri, options)
    def creds = client.second.getCredentials(authScope, null)

    then:
    creds.principal.username == 'user'
    creds.password == 'pwd'.toCharArray()
  }

  @Issue('#1935')
  def 'when creating a http client from the options, adds no headers of its own if there are no options'() {
    when:
    def result = HttpClient.INSTANCE.newHttpClientFromOptions(new URI('http://localhost'), [:])

    then:
    defaultHeadersOf(result.component1())*.name == ['User-Agent']
  }

  def 'http client should retry any requests for any method'(Method method) {
    def uri = new URI('http://localhost')
    def request = Mock(HttpRequest)
    request.method >> method
    def client = HttpClient.INSTANCE.newHttpClient(null, uri, 1, 1, false).component1()
    def retryStrategy = null
    def execChain = client.execChain
    while (retryStrategy == null && execChain != null) {
      if (execChain.handler instanceof HttpRequestRetryExec) {
        retryStrategy = execChain.handler.retryStrategy
      } else {
        execChain = execChain.next
      }
    }

    expect:
    retryStrategy.handleAsIdempotent(request) == true

    where:
    method << Method.values()
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'if authentication is set, sets up the http client with auth'() {
    given:
    URI uri = new URI('http://localhost')
    def authentication = ['basic', 'user', 'pwd']
    def authScope = new AuthScope(uri.host, uri.port)

    when:
    def client = HttpClient.INSTANCE.newHttpClient(authentication, uri, 1, 1, false)
    def creds = client.second.getCredentials(authScope, null)

    then:
    client.second instanceof SystemDefaultCredentialsProvider
    creds.principal.username == 'user'
    creds.password == 'pwd'.toCharArray()
  }
}
