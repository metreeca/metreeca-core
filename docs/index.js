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

(function (window) {

	//// TOC Toggling //////////////////////////////////////////////////////////////////////////////////////////////////

	window.onhashchange=function () {
		window.document.getElementById("toggle").checked=false;
	};


	//// browser-specific css selectors (eg html[data-useragent*='MSIE 10.0']) /////////////////////////////////////////

	document.documentElement.setAttribute("data-useragent", navigator.userAgent);


	//// ;) ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	(function (a) {

		for (var i=0; i < a.length; ++i) {
			a[i]=255-a[i];
		}

		var b=String.fromCharCode.apply(String, a);
		var c=b.substr(0, 7);
		var d=b.substr(7);

		var _=document.querySelectorAll("main a");

		for (var i=0; i < _.length; ++i) {

			var e=(_[i].getAttribute("href") || "").match(/^#@([-\w]+)(.*)$/);

			if (e) {

				var f=e[1] || "";
				var g=e[2] || "";

				_[i].setAttribute("href", c+f+d+g);

				if (_[i].childNodes.length === 0) {
					_[i].appendChild(document.createTextNode(f+d));
				}

			}
		}

	})([146, 158, 150, 147, 139, 144, 197, 191, 146, 154, 139, 141, 154, 154, 156, 158, 209, 156, 144, 146]);

})(window);
