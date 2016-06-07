package org.springframework.test.context.junit5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.gen5.api.Assertions;
import org.junit.gen5.api.Test;
import org.junit.gen5.api.extension.AfterAllCallback;
import org.junit.gen5.api.extension.BeforeAllCallback;
import org.junit.gen5.api.extension.ContainerExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.Closeable;
import java.io.IOException;

@SpringExtendWith(
		{
				SpringExtensibleExtensionTests.TestResourceController.class,
				SpringExtensibleExtensionTests.TestResourceValidation.class
		}
)
@ContextConfiguration(classes = SpringExtensibleExtensionTests.LocalTestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class SpringExtensibleExtensionTests {
	private static final Logger LOG = LogManager.getLogger(SpringExtensibleExtensionTests.class);

	@Autowired
	private TestResource testResource;

	@Test
	void testResourceShouldHaveBeenOpened() throws Exception {
		Assertions.assertTrue(testResource.isOpen(), "TestResource should have been closed.");
	}

	static class TestResourceController implements BeforeAllCallback, AfterAllCallback {
		@Autowired
		private TestResource testResource;

		@Override
		public void beforeAll(ContainerExtensionContext context) throws Exception {
			testResource.open();
		}

		@Override
		public void afterAll(ContainerExtensionContext context) throws Exception {
			testResource.close();
		}
	}

	static class TestResourceValidation implements AfterAllCallback {
		@Autowired
		private TestResource testResource;

		@Override
		public void afterAll(ContainerExtensionContext context) throws Exception {
			Assertions.assertFalse(testResource.isOpen(), "TestResource should have been closed.");
		}
	}

	@Configuration
	static class LocalTestConfig {
		@Bean
		public TestResource testResource() {
			return new TestResource();
		}

		@Bean
		public TestResourceController testResourceController() {
			return new TestResourceController();
		}

		@Bean
		public TestResourceValidation testResourceValidation() {
			return new TestResourceValidation();
		}
	}

	private static class TestResource implements Closeable {
		private boolean open = false;

		boolean isOpen() {
			return open;
		}

		void open() throws IOException {
			open = true;
			LOG.info("Opened TestResource.");
		}
		@Override
		public void close() throws IOException {
			open = false;
			LOG.info("Closed TestResource.");
		}
	}
}
