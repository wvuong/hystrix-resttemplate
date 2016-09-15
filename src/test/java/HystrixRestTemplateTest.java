import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.willvuong.hystrix.resttemplate.HystrixRestTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Created by will on 9/6/16.
 */
public class HystrixRestTemplateTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixRestTemplateTest.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Rule
    public HystrixRequestContextRule hystrixRequestContextRule = new HystrixRequestContextRule();

    @Before
    public void before() {
        stubFor(get(urlEqualTo("/ping/a"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>")));

        stubFor(get(urlEqualTo("/ping/b"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withStatusMessage("!!!")));
    }

    @After
    public void after() {
        LOGGER.info(HystrixRequestLog.getCurrentRequest().getExecutedCommandsAsString());
    }

    @Test
    public void testSampleUsage() {
        HystrixRestTemplate restTemplate = new HystrixRestTemplate();

        String result = restTemplate.getForObject("http://localhost:8080/ping/a", String.class);
        System.out.println(result);
    }

    @Test(expected = HystrixRuntimeException.class)
    public void testErroredStatusCode() {
        HystrixRestTemplate restTemplate = new HystrixRestTemplate();

        restTemplate.getForObject("http://localhost:8080/ping/b", String.class);
    }

    private static class HystrixRequestContextRule extends ExternalResource {

        private HystrixRequestContext hystrixRequestContext;

        @Override
        protected void before() throws Throwable {
            hystrixRequestContext = HystrixRequestContext.initializeContext();
        }

        @Override
        protected void after() {
            hystrixRequestContext.close();
        }

        public HystrixRequestContext getHystrixRequestContext() {
            return hystrixRequestContext;
        }
    }
}
