package com.willvuong.hystrix.resttemplate;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

/**
 * A RestTemplate extension that uses Hystrix underneath.
 * <p>
 * Note: HystrixRestTemplate, with the {@link DefaultResponseErrorHandler}, will count any HTTP responses with
 * 400 or 500 series status codes as Hystrix command execution failures.  If a 400 or 500 series status code response
 * should NOT be counted as a failure, implement a custom {@link ResponseErrorHandler} to either wrap the {@link HttpStatusCodeException}
 * in a {@link com.netflix.hystrix.exception.HystrixBadRequestException} or not throw at all.
 * <p>
 * Note: Take care in configuring the underling {@link org.springframework.http.client.ClientHttpRequestFactory}'s timeouts
 * and retries as these should be configured with consideration to Hystrix's command timeout configuration.
 *
 * Created by wvuong@chariotsolutions.com on 8/16/16.
 */
public class HystrixRestTemplate extends RestTemplate {

    @Override
    protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        return doExecute(url, method, requestCallback, responseExtractor, null, null);
    }

    protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor, HystrixCommandOptions options, Supplier<T> fallback) throws RestClientException {
        Assert.notNull(url, "'url' must not be null");
        Assert.notNull(method, "'method' must not be null");

        HystrixCommand.Setter setter = options != null && options.getSetter() != null ?
                options.getSetter() :
                defaultSetterFor(url, method);

        HystrixCommand<T> hystrixCommand = new HystrixCommand<T>(setter) {
            /*
                This is the original RestTemplate.doExecute() that has been moved inside of an anonymous HystrixCommand for execution.
             */
            @Override
            protected T run() throws Exception {
                ClientHttpResponse response = null;
                try {
                    ClientHttpRequest request = createRequest(url, method);
                    if (requestCallback != null) {
                        requestCallback.doWithRequest(request);
                    }

                    response = request.execute();
                    handleResponse(url, method, response);

                    if (responseExtractor != null) {
                        return responseExtractor.extractData(response);

                    } else {
                        return null;
                    }

                } catch (IOException ex) {
                    throw new ResourceAccessException("I/O error on " + method.name() +
                            " request for \"" + url + "\": " + ex.getMessage(), ex);

                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }

            @Override
            protected T getFallback() {
                if (fallback != null) {
                    return fallback.get();
                }

                return super.getFallback();
            }
        };

        return hystrixCommand.execute();
    }

    private HystrixCommand.Setter defaultSetterFor(URI url, HttpMethod method) {
        return HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(url.getHost()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(url.getPath() + "|" + method.name()));
    }

    public static class HystrixCommandOptions {

        private final String cacheKey;
        private final HystrixCommand.Setter setter;

        private HystrixCommandOptions(Builder builder) {
            this.cacheKey = builder.cacheKey;
            this.setter = builder.setter;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public HystrixCommand.Setter getSetter() {
            return setter;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        static class Builder<T> {
            private String cacheKey = null;
            private HystrixCommand.Setter setter = null;

            public Builder setCacheKey(String cacheKey) {
                this.cacheKey = cacheKey;
                return this;
            }

            public Builder setSetter(HystrixCommand.Setter setter) {
                this.setter = setter;
                return this;
            }

            public HystrixCommandOptions build() {
                return new HystrixCommandOptions(this);
            }
        }
    }
}