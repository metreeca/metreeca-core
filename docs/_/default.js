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

(function (window) {

	//// Header Toggling ///////////////////////////////////////////////////////////////////////////////////////////////

	var body=window.document.body;
	var header=window.document.querySelector("body > nav > header");

	var mark=0; // the previous scroll top
	var toggle; // header toggling callback (null if not yet scheduled)

	body.onscroll=function () {
		if ( !toggle && body.clientWidth < 480 ) {
			window.requestAnimationFrame(toggle=function () {
				try {

					var scroll=body.scrollTop;
					var height=body.clientHeight;

					var delta=scroll-mark;
					var threshold=50;

					if ( delta > threshold && (scroll+height) < body.scrollHeight ) { // downward (unless bouncing at bottom)

						header.style.top=-(header.clientHeight+1)+"px";
						mark=scroll;

					} else if ( -delta > threshold && scroll > 0 ) { // upward (unless bouncing at top)

						header.style.top=0;
						mark=scroll;

					}

				} finally { toggle=null; }
			});
		}
	};


	//// TOC Toggling //////////////////////////////////////////////////////////////////////////////////////////////////

	window.onhashchange=window.onresize=function () {
		window.document.getElementById("toggle").checked=false;
	};


	//// browser-specific css selectors (eg html[data-useragent*='MSIE 10.0']) /////////////////////////////////////////

	document.documentElement.setAttribute('data-useragent', navigator.userAgent);


	//// ;) ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	(function (a) {

		for (var i=0; i < a.length; ++i) { a[i]=255-a[i]; }

		var b=String.fromCharCode.apply(String, a);
		var c=b.substr(0, 7);
		var d=b.substr(7);

		var _=document.querySelectorAll("main a");

		for (var i=0; i < _.length; ++i) {

			var e=(_[i].getAttribute("href") || "").match(/^#@([-\w]+)(.*)$/);

			if ( e ) {

				var f=e[1] || "";
				var g=e[2] || "";

				_[i].setAttribute('href', c+f+d+g);

				if ( _[i].childNodes.length === 0 ) {
					_[i].appendChild(document.createTextNode(f+d));
				}

			}
		}

	})([146, 158, 150, 147, 139, 144, 197, 191, 146, 154, 139, 141, 154, 154, 156, 158, 209, 156, 144, 146]);

})(window);
