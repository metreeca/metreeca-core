/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class ContextTest {

    @Test void testReplacesToolsWithPlugins() {

        final Context context=new Context();

        final Supplier<Object> target=() -> "target";
        final Supplier<Object> plugin=() -> "plugin";

        context.set(target, plugin);

        assertThat(context.get(target))
                .isEqualTo(plugin.get());

    }

    @Test void testReleaseAutoCloseableResources() {

        final Context context=new Context();

        final class Resource implements AutoCloseable {

            private boolean closed;

            private boolean isClosed() {
                return closed;
            }

            @Override public void close() {
                this.closed=true;
            }

        }

        final Supplier<Resource> service=() -> new Resource();

        final Resource resource=context.get(service);

        context.clear();

        assertThat(resource.isClosed())
                .isTrue();

    }

    @Test void testReleaseDependenciesAfterResource() {

        final Context context=new Context();

        final Collection<Object> released=new ArrayList<>();

        final class Step implements Supplier<AutoCloseable>, AutoCloseable {

            private final Supplier<AutoCloseable> dependency;


            private Step(final Supplier<AutoCloseable> dependency) {
                this.dependency=dependency;
            }


            @Override public AutoCloseable get() {

                if ( dependency != null ) { service(dependency); }

                return this;
            }

            @Override public void close() {
                released.add(this);
            }

        }

        final Step z=new Step(null);
        final Step y=new Step(z);
        final Step x=new Step(y);

        context.get(x); // load the terminal service with its dependencies
        context.clear(); // release resources

        assertThat(released)
                .as("dependencies released after relying resources")
                .containsExactly(x, y, z);
    }

    @Test void testPreventToolBindingIfAlreadyInUse() {

        final Context context=new Context();
        final Supplier<Object> service=Object::new;

        assertThatThrownBy(() -> {

            context.get(service);
            context.set(service, Object::new);

        })
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void testTrapCircularDependencies() {

        final Context context=new Context();
        final Object delegate=new Object();

        assertThatThrownBy

                (() -> context.get(new Supplier<Object>() {
                    @Override public Object get() {
                        return context.get(this);
                    }
                }))

                .isInstanceOf(IllegalStateException.class);

        assertThat

                (context.get(new Supplier<Object>() {
                    @Override public Object get() {
                        return context.get(this, () -> delegate);
                    }
                }))

                .isEqualTo(delegate);

    }


    @Test void testHandleExceptionsInFactories() {

        final Context context=new Context();

        final Supplier<Object> service=() -> {
            throw new NoSuchElementException("missing resource");
        };

        assertThatThrownBy(() ->

                context.get(service)
        )
                .isInstanceOf(NoSuchElementException.class);

    }

}
