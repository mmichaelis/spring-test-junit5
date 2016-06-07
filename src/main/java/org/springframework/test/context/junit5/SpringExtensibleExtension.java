package org.springframework.test.context.junit5;

import org.junit.gen5.api.extension.*;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

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
		List<Extension> result = new ArrayList<Extension>(extensionClasses.size());
		extensionClasses.stream().map(new Function<Class<? extends Extension>, Extension>() {
			@Override
			public Extension apply(Class<? extends Extension> extensionClass) {
				return applicationContext.getBean(extensionClass);
			}
		}).forEachOrdered(new Consumer<Extension>() {
			@Override
			public void accept(Extension extension) {
				result.add(extension);
			}
		});
		return result;
	}

	private List<Class<? extends Extension>> getExtensionClasses(Class<?> testClass) {
		SpringExtendWith[] annotations = testClass.getAnnotationsByType(SpringExtendWith.class);
		List<Class<? extends Extension>> extensionClasses = new ArrayList<Class<? extends Extension>>();
		Arrays.stream(annotations).forEachOrdered(new Consumer<SpringExtendWith>() {
			@Override
			public void accept(SpringExtendWith springExtendWith) {
				extensionClasses.addAll(Arrays.asList(springExtendWith.value()));
			}
		});
		return extensionClasses;
	}

}
