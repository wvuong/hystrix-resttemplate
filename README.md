# hystrix-resttemplate
Let's mash [Spring's RestTemplate](https://github.com/spring-projects/spring-framework) and [Netflix's Hystrix](https://github.com/Netflix/Hystrix) together!

This is a RestTemplate sublass that uses Hystrix underneath to execute requests as Hystrix commands.  The goal is to:
* Provide a RestTemplate drop-in replacement that leverages Hystrix for fault-tolerance and latency
* Provide another RestTemplate subclass that exposes more of the Hystrix API and functionality while maintaining a Spring-y feel

## Notes:
* HystrixRestTemplate, with the `DefaultResponseErrorHandler`, will count any HTTP responses with 400 or 500 series status codes as Hystrix command execution failures.  If a 400 or 500 series status code response should NOT be counted as a failure, implement a custom `ResponseErrorHandler` to either wrap the `HttpStatusCodeException` in a `com.netflix.hystrix.exception.HystrixBadRequestException` or not throw at all.
* Take care in configuring the underling `org.springframework.http.client.ClientHttpRequestFactory`'s timeouts and retries as these should be configured with consideration to Hystrix's command timeout configuration.
