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

	var metreeca=window.metreeca; // platform metadata gathered by the index.html layout


	//// Utilities /////////////////////////////////////////////////////////////////////////////////////////////////////

	function insensitive(x, y) { return x.toLowerCase().localeCompare(y.toLowerCase()); }

	function unique(item, i, array) { return !i || item !== array[i-1]; }


	function expand(s) { // add leading zeros to version numbers for lexicographic sorting
		return s.replace(/\b\d+/g, function ($0) { return ("000"+$0).substr($0.length)});
	}

	function compact(s) { // remove leading zeros from version numbers
		return s.replace(/\b0+(\d)/g, "$1");
	}


	//// Indexes ///////////////////////////////////////////////////////////////////////////////////////////////////////

	function versions() {
		return metreeca.documents

				.map(function (doc) { return doc.version })

				.map(expand)
				.sort()
				.filter(unique)
				.map(compact);
	}

	function modules(version) {
		return metreeca.documents

				.filter(function (doc) { return doc.version === version })
				.map(function (doc) { return doc.module; })

				.sort(insensitive)
				.filter(unique);
	}

	function tags(version) {
		return metreeca.documents

				.filter(function (doc) { return doc.version === version })
				.map(function (doc) { return doc.tags; })

				.reduce(function (flattened, tags) { return flattened.concat(tags) }, [])

				.sort(insensitive)
				.filter(unique);
	}

	function groups(version) {
		return metreeca.documents

				.filter(function (doc) { return doc.version === version })

				.sort(function (x, y) { return insensitive(x.title, y.title) })
				.reduce(function (groups, doc) {

					var bin=groups[doc.module]=(groups[doc.module] || []);

					bin.push(doc);

					return groups;

				}, {});
	}


	//// Active Filters ////////////////////////////////////////////////////////////////////////////////////////////////

	var filters={
		version: "",
		module: "",
		tag: ""
	};

	function update(filters) {

		var params={};

		for (var filter in filters) { // strip empty filters
			if ( filters.hasOwnProperty(filter) && filters[filter] ) {
				params[filter]=filters[filter];
			}
		}

		window.location.hash=$.param(params);
	}


	//// Facets ////////////////////////////////////////////////////////////////////////////////////////////////////////

	var $versions=$(window.document.querySelector("body > nav .versions")).on("change", function (e) {

		filters={ version: e.target.value }; // reset filters on version change

		update(filters);
	});

	var $modules=$(window.document.querySelector("body > nav .modules")).on("change", function (e) {

		if ( e.target.checked ) {
			filters.module=e.target.value;
		} else {
			delete filters.module;
		}

		update(filters);
	});

	var $tags=$(window.document.querySelector("body > nav .tags")).on("change", function (e) {

		if ( e.target.checked ) {
			filters.tag=e.target.value;
		} else {
			delete filters.tag;
		}

		update(filters);
	});


	//// Matches////////////////////////////////////////////////////////////////////////////////////////////////////////

	var $matches=$(window.document.querySelector("body > main > article"));


	//// Update ////////////////////////////////////////////////////////////////////////////////////////////////////////

	(window.onhashchange=function () {

		for (var match, query=/(\w+)=([^&=]+)/g; (match=query.exec(window.location.hash));) { // update filters from hash

			var filter=decodeURI(match[1]).replace(/\+/g, " ");
			var value=decodeURI(match[2]).replace(/\+/g, " ");

			filters[filter]=value;
		}

		var _version=filters.version || metreeca.latest; // active version

		var _versions=versions();
		var _modules=modules(_version);
		var _tags=tags(_version);
		var _groups=groups(_version);

		if ( filters.version && _versions.indexOf(filters.version) < 0
				|| filters.module && _modules.indexOf(filters.module) < 0
				|| filters.tag && _tags.indexOf(filters.tag) < 0 ) {

			update(filters={}) // bad filter value >> reset facets

		} else {

			$versions.empty().append(_versions.map(function (version) {
				return $("<option/>")
						.attr("value", version)
						.prop("selected", version === _version)
						.text("Version "+version+(version === metreeca.latest ? " (Latest)" : ""));
			}));


			$modules.empty().append(_modules.map(function (module) {
				return $("<label/>")
						.append($("<input/>")
								.attr("type", "checkbox")
								.attr("value", module)
								.prop("checked", module === filters.module))
						.append($("<span/>")
								.text(module));
			}));


			$tags.empty().append(_tags.map(function (tag) {
				return $("<label/>")
						.append($("<input/>")
								.attr("type", "checkbox")
								.attr("value", tag)
								.prop("checked", tag === filters.tag))
						.append($("<span/>")
								.text(tag));
			}));

			$matches.empty().append($("<dl/>").append(
					Object.keys(_groups)

							.sort(insensitive)
							.map(function (module) {

								var matches=_groups[module]

										.filter(function (doc) {
											return (!filters.version || doc.version === filters.version)
													&& (!filters.module || doc.module === filters.module)
													&& (!filters.tag || doc.tags.indexOf(filters.tag) >= 0);
										});

								return matches.length ? [

									$("<dt/>").text(module),
									$("<dd/>").append($("<ul/>").append(matches.map(function (match) {
										return $("<li/>").append($("<a/>")
												.attr("href", match.path)
												.text(match.title))
									})))

								].reduce($.merge) : [];
							})
			));

		}

	})(); // update on page load

})(window);
