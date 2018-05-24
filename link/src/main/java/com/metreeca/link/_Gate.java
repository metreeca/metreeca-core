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

package com.metreeca.link;

import com.metreeca.link._gates.BasicGate;
import com.metreeca.tray.Tool;

import java.util.function.BiConsumer;


public interface _Gate {

	public static Tool<_Gate> Tool=BasicGate::new; // !!! configurable


	public void authorize(Tool.Loader tools, _Request request, _Response response, BiConsumer<_Request, _Response> sink);

	public void authenticate(Tool.Loader tools, _Request request, _Response response, BiConsumer<_Request, _Response> sink);

}