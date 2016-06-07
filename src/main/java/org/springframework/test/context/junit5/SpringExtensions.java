package org.springframework.test.context.junit5;

import org.junit.gen5.api.extension.ExtendWith;
import org.junit.gen5.commons.meta.API;

import java.lang.annotation.*;

import static org.junit.gen5.commons.meta.API.Usage.Experimental;

@ExtendWith(SpringExtension.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(Experimental)
public @interface SpringExtensions {
	SpringExtendWith[] value();
}
