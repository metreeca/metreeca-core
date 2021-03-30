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

// wikidata support functions for google sheets

const searches={};
const entities={};


function WDSEARCH(key) {
	return search(key).id;
}

function WDLABEL(qid) {
	return value(entity(qid).labels);
}

function WDDESCRIPTION(qid) {
	return value(entity(qid).descriptions);
}

function WDALIASES(qid) {
	return values(entity(qid).aliases);
}


function search(key) { // !!! check return status // !!! purge cache
	return searches[key]=(searches[key]=JSON.parse(UrlFetchApp.fetch(
		`https://www.wikidata.org/w/api.php/w/api.php`
		+`?action=wbsearchentities`
		+`&format=json`
		+`&search=${encodeURIComponent(key)}`
		+`&language=en`
		+`&strictlanguage=1`
		+`&type=item`
		+`&limit=1`
	).getContentText()).search[0] || {});
}

function entity(qid) { // !!! check return status // !!! purge cache
	return entities[qid]=(entities[qid]=JSON.parse(UrlFetchApp.fetch(
		`https://www.wikidata.org/w/api.php/w/api.php`
		+`?action=wbgetentities`
		+`&format=json`
		+`&ids=${qid}`
		+`&props=aliases%7Clabels%7Cdescriptions`
		+`&languages=en`
	).getContentText()).entities[qid]);
}

function value(v) {
	return ((v || {}).en || {}).value || "";
}

function values(v) {
	return ((v || {}).en || []).map(e => e.value).join(", ");
}
