package org.springframework.test.context.junit5;

import org.junit.gen5.api.extension.*;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.junit.gen5.commons.util.AnnotationUtils.findRepeatableAnnotations;

public class SpringExtensibleExtension extends SpringExtension {
	/**
	 * Cache of {@code TestContextManagers} keyed by test class.
	 */
	private final Map<Class<?>, List<Extension>> extensionCache = new ConcurrentHashMap<>(64);

	@Override
	public void beforeAll(ContainerExtensionContext context) throws Exception {
		super.beforeAll(context);
		Iterable<Extension> extensions = getExtensions(context);
		for (Extension extension : extensions) {
			if (extension instanceof BeforeAllCallback) {
				BeforeAllCallback callback = (BeforeAllCallback) extension;
				// Let possible exceptions bubble up. It makes no sense to continue execution of further
				// callbacks as they might fail because of previous failures. Or one callback signalled to
				// ignore the test. Thus again it makes no sense to continue.
				callback.beforeAll(context);
			}
		}
	}

	@Override
	public void afterAll(ContainerExtensionContext context) throws Exception {
		super.afterAll(context);
		Iterable<Extension> extensions = getExtensions(context);
		for (Extension extension : extensions) {
			if (extension instanceof AfterAllCallback) {
				AfterAllCallback callback = (AfterAllCallback) extension;
				// Break on first exception
				callback.afterAll(context);
			}
		}
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		super.postProcessTestInstance(testInstance, context);
		Iterable<Extension> extensions = getExtensions(context);
		for (Extension extension : extensions) {
			if (extension instanceof TestInstancePostProcessor) {
				TestInstancePostProcessor callback = (TestInstancePostProcessor) extension;
				// Break on first exception
				callback.postProcessTestInstance(testInstance, context);
			}
		}
	}

	@Override
	public void beforeEach(TestExtensionContext context) throws Exception {
		super.beforeEach(context);
		Iterable<Extension> extensions = getExtensions(context);
		for (Extension extension : extensions) {
			if (extension instanceof BeforeEachCallback) {
				BeforeEachCallback callback = (BeforeEachCallback) extension;
				// Break on first exception
				callback.beforeEach(context);
			}
		}
	}

	@Override
	public void afterEach(TestExtensionContext context) throws Exception {
		super.afterEach(context);
		Iterable<Extension> extensions = getExtensions(context);
		for (Extension extension : extensions) {
			if (extension instanceof AfterEachCallback) {
				AfterEachCallback callback = (AfterEachCallback) extension;
				// Break on first exception
				callback.afterEach(context);
			}
		}
	}

	private Iterable<Extension> getExtensions(ExtensionContext context) {
		Optional<Class<?>> testClassOptional = context.getTestClass();
		List<Extension> extensions;
		if (testClassOptional.isPresent()) {
			Class<?> testClass = testClassOptional.get();
			extensions = extensionCache.computeIfAbsent(testClass, SpringExtensibleExtension.this::getExtensionBeans);
		} else {
			extensions = Collections.emptyList();
		}
		return extensions;
	}

	private List<Extension> getExtensionBeans(Class<?> testClass) {
		List<Class<? extends Extension>> extensionClasses = getExtensionClasses(testClass);
		return getExtensionBeans(testClass, extensionClasses);
	}

	private List<Extension> getExtensionBeans(Class<?> testClass, List<Class<? extends Extension>> extensionClasses) {
		ApplicationContext applicationContext = getApplicationContext(testClass);
		return extensionClasses
				.stream()
				.map((Function<Class<? extends Extension>, Extension>) applicationContext::getBean)
				.collect(Collectors.toList());
	}

	private List<Class<? extends Extension>> getExtensionClasses(Class<?> testClass) {
		return findRepeatableAnnotations(testClass, SpringExtendWith.class).stream()
				.map(SpringExtendWith::value)
				.flatMap(Arrays::stream)
				.collect(toList());
	}

}
