package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.injection.spi.InjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

public class Main {

	@ApplicationScoped
	public static class A {

		@Resource(name = "world")
		private String value;

		public String value() {
			return "hello " + value;
		}

	}

	@ApplicationScoped
	@Specializes
	public static class B extends A {

	}

	public static class FakeResourceInjectionServices implements ResourceInjectionServices {

		@Override
		public void cleanup() {
		}

		@Override
		public ResourceReferenceFactory<Object> registerResourceInjectionPoint(InjectionPoint injectionPoint) {
			return new ResourceReferenceFactory<Object>() {
				@Override
				public ResourceReference<Object> createResource() {
					return new SimpleResourceReference<Object>("world");
				}
			};
		}

		@Override
		public ResourceReferenceFactory<Object> registerResourceInjectionPoint(String jndiName, String mappedName) {
			return new ResourceReferenceFactory<Object>() {
				@Override
				public ResourceReference<Object> createResource() {
					return new SimpleResourceReference<Object>("world");
				}
			};
		}

	}

	public static class Ext implements Extension {

		void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager bm) {
			event.addAnnotatedType(bm.createAnnotatedType(B.class), B.class.getName());
		}

	}
	
	@Test
	public void discovered() {
		Weld weld = new Weld();

		weld.addBeanClasses(A.class, B.class);
		weld.addServices(new FakeResourceInjectionServices());

		test(weld);
	}
	
	@Test
	public void extension() {
		Weld weld = new Weld();

		weld.addBeanClasses(A.class);
		weld.addExtension(new Ext());
		
		weld.addServices(new FakeResourceInjectionServices());

		test(weld);
	}

	private void test(Weld weld) {
		WeldContainer initialize = weld.initialize();

		try {
			assertEquals(
			initialize.instance().select(A.class).get().value(),"hello world");
		} finally {
			initialize.close();
		}
	}
}
