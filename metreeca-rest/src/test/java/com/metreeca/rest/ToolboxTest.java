/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.rest.Toolbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class ToolboxTest {

    @Test void testReplacesToolsWithPlugins() {

        final Toolbox toolbox=new Toolbox();

        final Supplier<Object> target=() -> "target";
        final Supplier<Object> plugin=() -> "plugin";

        toolbox.set(target, plugin);

        assertThat(toolbox.get(target))
                .isEqualTo(plugin.get());

    }

    @Test void testReleaseAutoCloseableResources() {

        final Toolbox toolbox=new Toolbox();

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

        final Resource resource=toolbox.get(service);

        toolbox.clear();

        assertThat(resource.isClosed())
                .isTrue();

    }

    @Test void testReleaseDependenciesAfterResource() {

        final Toolbox toolbox=new Toolbox();

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

        toolbox.get(x); // load the terminal service with its dependencies
        toolbox.clear(); // release resources

        assertThat(released)
                .as("dependencies released after relying resources")
                .containsExactly(x, y, z);
    }

    @Test void testPreventToolBindingIfAlreadyInUse() {

        final Toolbox toolbox=new Toolbox();
        final Supplier<Object> service=Object::new;

        assertThatThrownBy(() -> {

            toolbox.get(service);
            toolbox.set(service, Object::new);

        })
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void testTrapCircularDependencies() {

        final Toolbox toolbox=new Toolbox();
        final Object delegate=new Object();

        assertThatThrownBy

                (() -> toolbox.get(new Supplier<Object>() {
                    @Override public Object get() {
                        return toolbox.get(this);
                    }
                }))

                .isInstanceOf(IllegalStateException.class);

        assertThat

                (toolbox.get(new Supplier<Object>() {
                    @Override public Object get() {
                        return toolbox.get(this, () -> delegate);
                    }
                }))

                .isEqualTo(delegate);

    }


    @Test void testHandleExceptionsInFactories() {

        final Toolbox toolbox=new Toolbox();

        final Supplier<Object> service=() -> {
            throw new NoSuchElementException("missing resource");
        };

        assertThatThrownBy(() ->

                toolbox.get(service)
        )
                .isInstanceOf(NoSuchElementException.class);

    }

}
