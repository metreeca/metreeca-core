---
title: 		How To Create a Custom Data Hub
excerpt:	Configuration guidelines for extending the base linked data hub with custom request handlers and specialized shared tool
tags:		How-To
module:     "Metreeca Linked Data Hub"
version:    "0.45"
---

- maven project
	- overlay metreeca base

- add custom static apps under src/main/webapp/apps/
- add custom resources under src/main/webapp/WEB-INF/

- customize shared tool
	- implement com.metreeca.link.Toolbox
		- replace tool using Tray.set() method
			- acquire dependent resources only within replacing Tool.acquire() method and never in Toolbox.load() (to avoid acquiring a resource from an tool eventually replaced by a subsequently loaded Toolbox. trap in Vault with one-off resource acquisition?)
	- add toolbox class to META-INF/services/com.metreeca.link.Toolbox
		- if multiple jars define custom tool, the loading order is undefined
			- conflict may arise in tool replacements (trap in Vault with one-off tool replacement?)

- customize request handlers
	- implement com.metreeca.link.Service
		- bind a handler to a resource path pattern using tray.get(Index.Tool).insert()
		- append a handler to the request pre-processing chain using tray.get(Server.Tool).head()
		- append a handler to the request post-processing chain using tray.get(Server.Tool).tail()
			- pre/post-processing handlers must check Response.getStatus() != 0 (dedicated boolean getter?) to verify if a previous handler already defined a response code and act accordingly (explain…)
	- add service class to META-INF/services/com.metreeca.link.Service
		- if multiple jars define custom handlers, the loading order is undefined
			- conflict may arise in path pattern bindings (trap in Index with one-off path pattern binding? interactions with ui-drive handler insertion/deletion?)
			- the execution order for pre/post-processing handlers loaded by services from different jars is undefined
