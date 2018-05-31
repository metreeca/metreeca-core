/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray;

import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;


public class TrayTest {

	@Test public void testReplacesToolsWithPlugins() {

		final Tray tray=new Tray();

		final Supplier<Object> target=() -> "target";
		final Supplier<Object> plugin=() -> "plugin";

		tray.set(target, plugin);

		assertEquals("tool plugin", plugin.get(), tray.get(target));
	}

	@Test public void testReleaseAutoCloseableResources() {

		final Tray tray=new Tray();

		final class Resource implements AutoCloseable {

			private boolean closed;

			private boolean isClosed() {
				return closed;
			}

			@Override public void close() {
				this.closed=true;
			}

		}

		final Supplier<Resource> tool=() -> new Resource();

		final Resource resource=tray.get(tool);

		tray.clear();

		assertTrue("resource released", resource.isClosed());

	}

	@Test public void testReleaseDependenciesAfterResource() {

		final Tray tray=new Tray();

		final Collection<Object> released=new ArrayList<>();

		final class Step implements Supplier<AutoCloseable>, AutoCloseable {

			private final Supplier<AutoCloseable> dependency;


			private Step(final Supplier<AutoCloseable> dependency) {
				this.dependency=dependency;
			}


			@Override public AutoCloseable get() {

				if ( dependency != null ) { tool(dependency); }

				return this;
			}

			@Override public void close() throws Exception {
				released.add(this);
			}

		}

		final Step z=new Step(null);
		final Step y=new Step(z);
		final Step x=new Step(y);

		tray.get(x); // load the terminal tool with its dependencies
		tray.clear(); // release resources

		assertEquals("dependencies released after relying resources", asList(x, y, z), released);
	}


	@Test(expected=IllegalStateException.class) public void testPreventToolBindingIfAlreadyInUse() {

		final Tray tray=new Tray();
		final Supplier<Object> tool=Object::new;

		tray.get(tool);
		tray.set(tool, Object::new);

	}

	@Test(expected=IllegalStateException.class) public void testTrapCircularDependencies() {

		final Tray tray=new Tray();

		final Map<String, Supplier<Object>> cycle=new HashMap<>(); // avoid cyclic dependencies in initializers

		cycle.put("x", () -> tool(cycle.get("y")));
		cycle.put("y", () -> tool(cycle.get("z")));
		cycle.put("z", () -> tool(cycle.get("x")));

		tray.get(cycle.get("x"));

	}


	@Test(expected=NoSuchElementException.class) public void testHandleExceptionsInFactories() {

		final Tray tray=new Tray();

		final Supplier<Object> tool=() -> {
			throw new NoSuchElementException("missing resource");
		};

		try { tray.get(tool); } catch ( final NoSuchElementException ignored ) {}

		tray.get(tool);

	}
}
