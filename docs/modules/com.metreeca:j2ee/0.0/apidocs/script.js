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

function show(type) {
	count=0;
	for (var key in methods) {
		var row=document.getElementById(key);
		if ( (methods[key]&type) != 0 ) {
			row.style.display='';
			row.className=(count++%2) ? rowColor : altColor;
		}
		else {
			row.style.display='none';
		}
	}
	updateTabs(type);
}

function updateTabs(type) {
	for (var value in tabs) {
		var sNode=document.getElementById(tabs[value][0]);
		var spanNode=sNode.firstChild;
		if ( value == type ) {
			sNode.className=activeTableTab;
			spanNode.innerHTML=tabs[value][1];
		}
		else {
			sNode.className=tableTab;
			spanNode.innerHTML="<a href=\"javascript:show("+value+");\">"+tabs[value][1]+"</a>";
		}
	}
}
