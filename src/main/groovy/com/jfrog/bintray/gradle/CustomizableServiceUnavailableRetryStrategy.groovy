package com.jfrog.bintray.gradle

import org.apache.http.HttpResponse;
import org.apache.http.annotation.Immutable;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Exactly the same as DefaultServiceUnavailableRetryStrategy, except it takes the response code to retry on
 * as a parameter.
 */
@Immutable
public class CustomizableServiceUnavailableRetryStrategy implements ServiceUnavailableRetryStrategy {
	private int maxRetries;
	private long retryInterval;
	private int responseCode

	public CustomizableServiceUnavailableRetryStrategy(int responseCode, int maxRetries, int retryInterval) {
		if(maxRetries < 1) {
			throw new IllegalArgumentException("MaxRetries must be greater than 1");
		} else if(retryInterval < 1) {
			throw new IllegalArgumentException("Retry interval must be greater than 1");
		} else {
			this.maxRetries = maxRetries;
			this.retryInterval = (long)retryInterval;
			this.responseCode = responseCode;
		}
	}

	public CustomizableServiceUnavailableRetryStrategy() {
		this(503, 1, 1000);
	}

	public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
		return executionCount <= this.maxRetries && response.getStatusLine().getStatusCode() == this.responseCode;
	}

	public long getRetryInterval() {
		return this.retryInterval;
	}
}

