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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;


public class TrayTest {

	@Test public void testReplacesToolsWithPlugins() {

		final Tray tray=Tray.tray();

		final Tool<Object> target=tools -> "target";
		final Tool<Object> plugin=tools -> "plugin";

		tray.set(target, plugin);

		assertEquals("tool plugin", plugin.create(tray), tray.get(target));
	}

	@Test public void testReleaseAutoCloseableResources() {

		final Tray tray=Tray.tray();

		final class Resource implements AutoCloseable {

			private boolean closed;

			private boolean isClosed() {
				return closed;
			}

			@Override public void close() {
				this.closed=true;
			}

		}

		final Tool<Resource> tool=tools -> new Resource();

		final Resource resource=tray.get(tool);

		tray.clear();

		assertTrue("resource released", resource.isClosed());

	}

	@Test public void testReleaseDependenciesAfterResource() {

		final Tray tray=Tray.tray();

		final Collection<Object> released=new ArrayList<>();

		final class Step implements Tool<AutoCloseable>, AutoCloseable {

			private final Tool<AutoCloseable> dependency;


			private Step(final Tool<AutoCloseable> dependency) {
				this.dependency=dependency;
			}


			@Override public AutoCloseable create(final Loader tools) {

				if ( dependency != null ) { tools.get(dependency); }

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

		final Tray tray=Tray.tray();
		final Tool<Object> tool=tools1 -> new Object();

		tray.get(tool);
		tray.set(tool, tools -> new Object());

	}

	@Test(expected=IllegalStateException.class) public void testTrapCircularDependencies() {

		final Tray tray=Tray.tray();

		final Map<String, Tool<Object>> cycle=new HashMap<>(); // avoid cyclic dependencies in initializers

		final Tool<Object> supplier2=tools -> tools.get(cycle.get("y"));
		cycle.put("x", supplier2);
		final Tool<Object> supplier1=tools -> tools.get(cycle.get("z"));
		cycle.put("y", supplier1);
		final Tool<Object> supplier=tools -> tools.get(cycle.get("x"));
		cycle.put("z", supplier);

		tray.get(cycle.get("x"));

	}


	@Test(expected=NoSuchElementException.class) public void testHandleExceptionsInFactories() {

		final Tool.Loader tray=Tray.tray();

		final Tool<Object> tool=tools -> {
			throw new NoSuchElementException("missing resource");
		};

		try { tray.get(tool); } catch ( final NoSuchElementException ignored ) {}

		tray.get(tool);

	}
}
